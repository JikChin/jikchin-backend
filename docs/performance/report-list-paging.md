# 관리자 신고 목록 커서 페이징 성능 개선

`GET /api/admin/reports?status=PENDING`은 관리자가 처리할 신고를 오래된 순으로 돌려준다. 개선 전에는 해당 상태의 신고 **전부**를 한 번에 돌려줬고, 정렬은 `created_at ASC`인데 인덱스는 `status` 단일 컬럼(`idx_reports_status`, 값이 3개뿐)이었다. 신고 100만 건(PENDING 10만) 기준 응답 하나가 21MB다.

개선은 두 가지이며 기여를 분리해서 측정한다.

1. **커서 페이징**: `?status=PENDING&cursor=<마지막 신고 id>&size=20`. `(created_at, id)` 키셋이므로 OFFSET과 달리 페이지가 깊어져도 이미 있던 행을 건너뛰지 않는다. 단, `created_at`은 애플리케이션이 저장 직전에 찍으므로, 커서보다 오래된 `created_at`으로 만들어졌지만 커밋이 늦어진 신고는 그 페이징 세션에서는 보이지 않고 다음 첫 페이지 로드에서 나타난다. 커서 신고의 상태는 검사하지 않는다. 관리자가 페이지 마지막 신고를 처리해 PENDING이 아니게 돼도 다음 페이지가 이어져야 하기 때문이다.
2. **복합 인덱스** `(status, created_at)`: status 구간을 created_at 순으로 걸어 size건에서 멈춘다. InnoDB 보조 인덱스에는 PK가 붙으므로 `id` 동률 처리도 인덱스 안에서 끝난다.

## 측정 대상

| 구분 | API / 쿼리 | 의도 |
| --- | --- | --- |
| 목록 (개선 전) | `GET /api/admin/reports?status=PENDING` | PENDING 전체(10만 건) 응답 비용 (기준점) |
| 목록 (개선 전, 상태 없음) | `GET /api/admin/reports` | 100만 건 전체 응답 (참고) |
| 첫 페이지 | `GET …/reports?status=PENDING&size=20` | `ORDER BY created_at, id LIMIT 21` |
| 깊은 페이지 | `GET …/reports?status=PENDING&size=20&cursor=<95% 지점>` | 키셋 조건이 붙어도 비용이 커지지 않는지 |
| SQL | `benchmark/sql/explain-report-list.sql` | 전체·첫 페이지·깊은 페이지 EXPLAIN |

## 사전 조건

1. 로컬 전용 MySQL 데이터베이스를 사용한다(`docs/performance/event-index-k6.md`와 같은 환경). 운영 데이터에는 대량 시드·인덱스 변경을 실행하지 않는다.
2. `docker compose up -d mysql` 후 애플리케이션을 실행해 JPA 테이블을 만들고, `benchmark/http/k6-auth.http`의 회원가입을 1회 실행한다(`admin@jikchin.com`).
3. `benchmark/sql/promote-k6-admin.sql`로 벤치 계정을 `ROLE_ADMIN`으로 올린다. `/api/admin/**`은 관리자 토큰이 필요하며, k6는 실행마다 새로 로그인한다.
4. `benchmark/sql/seed-reports-large.sql`을 실행한다. 시드가 참조할 메이트 글 1건을 만든 뒤 신고 **100만 건**을 넣는다. 상태 비율 PENDING 10% / RESOLVED 70% / REJECTED 20%, `created_at`은 오래된 것부터 1초 간격이다. 마지막 SELECT로 상태별 건수를 확인한다.
5. 개선 전 조건은 페이징 이전 코드의 jar로, 개선 후 조건은 페이징 코드의 jar로 실행한다. 한 조건 안에서는 서버를 재시작하지 않는다.

## 실행 순서

결과 파일은 `benchmark/results/report-list-1m/<조건>-<회차>.json`. 각 조건은 워밍업 30초 1회 후 60초 × 3회. 요청량은 리뷰 실험과 같은 **2 rps**이며, 상태 없는 전체 목록(230MB·4초)만 요청이 겹치지 않도록 0.2 rps로 잰다.

```bash
# 개선 전
k6 run -e STATUS=PENDING -e RATE=2 -e DURATION=60s \
  --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
  --summary-export=benchmark/results/report-list-1m/full-pending-1.json \
  benchmark/k6/report-list-read.js
k6 run -e RATE=1 -e TIME_UNIT=5s -e DURATION=60s ... --summary-export=.../full-all-1.json benchmark/k6/report-list-read.js

# 개선 후: 인덱스 없이 → 있이, 각각 첫 페이지와 깊은 페이지
docker exec -i jikchin-mysql mysql -ujikchin_local -p"$MYSQL_PASSWORD" jikchin_benchmark < benchmark/sql/without-report-status-created-index.sql
k6 run -e STATUS=PENDING -e SIZE=20 -e RATE=2 ... --summary-export=.../cursor-no-index-1.json benchmark/k6/report-list-read.js
k6 run -e STATUS=PENDING -e SIZE=20 -e CURSOR=<deep> -e RATE=2 ... --summary-export=.../cursor-no-index-deep-1.json benchmark/k6/report-list-read.js
docker exec -i jikchin-mysql mysql -ujikchin_local -p"$MYSQL_PASSWORD" jikchin_benchmark < benchmark/sql/with-report-status-created-index.sql
k6 run ... --summary-export=.../cursor-with-index-1.json ...
k6 run ... -e CURSOR=<deep> --summary-export=.../cursor-with-index-deep-1.json ...
```

깊은 페이지 커서는 PENDING 신고를 `created_at` 순으로 세었을 때 **95,000번째**(PENDING 10만 건의 95% 지점) id다. `explain-report-list.sql`이 같은 방법으로 구한다.

## 측정 결과 (2026-09-14)

### 개선 전: 상태별 전체 목록

`EXPLAIN`(status=PENDING): `key = idx_reports_status`, 추정 rows 204,124(실제 100,000), `Using index condition; Using filesort`. `EXPLAIN ANALYZE` 108ms(인덱스 조회 89ms + 정렬). 값이 3개뿐인 `idx_reports_status`도 PENDING이 전체의 10%라 필터로는 쓰이지만, 정렬은 해결하지 못한다.

API p50은 약 340ms, 응답 20.9MB(60초에 2.5GB). DB 0.1초보다 엔티티 10만 개 적재와 JSON 직렬화가 크다. 상태 없이 전체를 부르면 230MB·p50 3.7초다.

### 페이징만 (인덱스 없음), EXPLAIN 기준

`LIMIT 21`이 붙어도 `Using filesort`. `EXPLAIN ANALYZE` 첫 페이지 94.6ms, 깊은 페이지 97.8ms(10만 행 조회 → 키셋 필터 4,999행 → 정렬). 응답은 수 KB로 줄지만 DB는 status 구간 전체를 읽고 정렬한다.

API p50은 첫 페이지 약 109ms, 깊은 페이지 약 134ms. 응답이 21MB에서 5KB로 줄어 직렬화 비용은 사라졌고, 남은 100ms는 10만 행 조회와 정렬이다. 깊은 페이지는 키셋 필터가 추가되어 약간 더 느리다.

### 페이징 + 인덱스 `(status, created_at)`

`EXPLAIN` 첫 페이지: `key = idx_reports_status_created`, filesort 소멸, `Limit 21` 실제 rows 21, 0.026ms. 깊은 페이지: `type = range`, 키셋 조건이 인덱스 구간 `(status = 'PENDING' AND created_at = c AND id > cursor) OR (status = 'PENDING' AND created_at > c)`로 변환돼 실제 rows 21, 0.23ms. 개선 전 전체 목록 쿼리도 이 인덱스로 정렬 없이 읽게 된다(151ms, 10만 행).

API p50은 첫 페이지 약 12ms, 깊은 페이지 약 14ms. PENDING 95% 지점의 깊은 페이지가 첫 페이지와 같은 비용이다.

### 정리

| 조건 | 첫 페이지 p50 | 깊은 페이지 p50 | 응답 크기 | DB가 읽는 행 |
| --- | ---: | ---: | ---: | ---: |
| 개선 전 PENDING 전체 | 340ms | — | 20.9 MB | 100,000 + 정렬 |
| 개선 전 상태 없음 전체 | 3,750ms | — | 230 MB | 1,000,000 + 정렬 |
| 페이징만 | 109ms | 134ms | 5 KB | 100,000 + 정렬 |
| 페이징 + 인덱스 | 12ms | 14ms | 5 KB | 21 |

`idx_reports_status`는 "죽은 인덱스"라기보다 **반쪽 인덱스**였다. PENDING이 10%라 필터로는 쓰이지만 `created_at` 정렬을 못 받쳐 매 요청 10만 행을 정렬했다. `(status, created_at)`이 그 정렬을 없애며, 왼쪽 접두사가 같으므로 `idx_reports_status`는 중복이다. 엔티티에서 선언을 제거했고(`ddl-auto: update`는 기존 인덱스를 지우지 않으므로 새 환경에서만 안 만들어짐), 기존 환경은 `ALTER TABLE reports DROP INDEX idx_reports_status;` 한 번으로 정리한다.

상태 없이 전체를 부르는 경로(`GET /api/admin/reports?size=20`)는 `(status, created_at)`으로 정렬을 못 받아 100만 행 전체 스캔 + filesort(약 175ms)가 남았다. `created_at` 단독 인덱스 `idx_reports_created_at`을 추가해 이 경로도 인덱스 순서로 21행만 읽게 했다(`EXPLAIN ANALYZE` 첫 페이지 0.08ms, 키셋 깊은 페이지 0.04ms. k6 측정은 하지 않았다).

## 판정 방법

| 항목 | 기대 | 확인할 값 |
| --- | --- | --- |
| 응답 크기 | 21MB → 수 KB | k6 `data_received` |
| 페이징만 (인덱스 없음) | 응답은 작아지지만 DB는 여전히 status 구간 10만 행 정렬 | `EXPLAIN` `Using filesort` |
| 페이징 + 인덱스 | 21행만 읽음 | `EXPLAIN` filesort 소멸, `key = idx_reports_status_created` |
| 깊은 페이지 | 인덱스가 있으면 첫 페이지와 같은 비용 | 첫 페이지와 깊은 페이지 p50 차이 |
| 오류율 | 모든 조건 1% 미만 | `http_req_failed` |

## 결과 기록 양식

| 조건 | 실행 | p50 / p95 / p99 (ms) | RPS | 실패율 | dropped | 응답 크기 | EXPLAIN Extra |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 개선 전 PENDING 전체 | 1 | 348.1 / 425.7 / 513.3 | 2.01 | 0% | 0 | 20.9 MB/건 | Using index condition; Using filesort |
| 개선 전 PENDING 전체 | 2 | 341.8 / 393.3 / 481.9 | 2.02 | 0% | 0 | 20.9 MB/건 | Using index condition; Using filesort |
| 개선 전 PENDING 전체 | 3 | 338.0 / 356.1 / 364.0 | 2.02 | 0% | 0 | 20.9 MB/건 | Using index condition; Using filesort |
| 개선 전 상태 없음 전체 (0.2 rps) | 1 | 3841.8 / 4416.7 / 4994.5 | 0.22 | 0% | 0 | 230 MB/건 | Using filesort |
| 개선 전 상태 없음 전체 (0.2 rps) | 2 | 3710.1 / 3900.2 / 4027.5 | 0.22 | 0% | 0 | 230 MB/건 | Using filesort |
| 개선 전 상태 없음 전체 (0.2 rps) | 3 | 3746.6 / 4089.1 / 4154.3 | 0.22 | 0% | 0 | 230 MB/건 | Using filesort |
| 페이징, 인덱스 없음, 첫 페이지 | 1 | 110.9 / 115.2 / 123.5 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition; Using filesort |
| 페이징, 인덱스 없음, 첫 페이지 | 2 | 108.9 / 111.6 / 114.7 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition; Using filesort |
| 페이징, 인덱스 없음, 첫 페이지 | 3 | 107.8 / 110.1 / 110.5 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition; Using filesort |
| 페이징, 인덱스 없음, 깊은 페이지 | 1 | 135.0 / 138.3 / 149.9 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition; Using where; Using filesort |
| 페이징, 인덱스 없음, 깊은 페이지 | 2 | 134.3 / 137.9 / 143.3 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition; Using where; Using filesort |
| 페이징, 인덱스 없음, 깊은 페이지 | 3 | 133.8 / 137.0 / 145.7 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition; Using where; Using filesort |
| 페이징 + 인덱스, 첫 페이지 | 1 | 10.9 / 12.9 / 20.7 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition |
| 페이징 + 인덱스, 첫 페이지 | 2 | 12.2 / 13.5 / 16.5 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition |
| 페이징 + 인덱스, 첫 페이지 | 3 | 12.1 / 14.8 / 16.3 | 2.03 | 0% | 0 | 5 KB/건 | Using index condition |
| 페이징 + 인덱스, 깊은 페이지 | 1 | 13.6 / 17.1 / 38.8 | 2.03 | 0% | 0 | 5 KB/건 | range; Using index condition |
| 페이징 + 인덱스, 깊은 페이지 | 2 | 14.2 / 20.4 / 29.9 | 2.03 | 0% | 0 | 5 KB/건 | range; Using index condition |
| 페이징 + 인덱스, 깊은 페이지 | 3 | 14.2 / 17.7 / 29.8 | 2.03 | 0% | 0 | 5 KB/건 | range; Using index condition |
