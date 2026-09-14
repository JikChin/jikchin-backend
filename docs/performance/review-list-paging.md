# 받은 리뷰 목록 커서 페이징 성능 개선

`GET /api/members/{memberId}/reviews`는 회원이 받은 리뷰를 최신순으로 돌려준다. 개선 전에는 **받은 리뷰 전부**를 한 번에 돌려줬고, 정렬은 `created_at DESC`인데 인덱스는 `reviewee_id` 단일 컬럼뿐이었다. 리뷰가 100만 건인 회원에게는 응답 하나가 159MB였다.

개선은 두 가지를 한 번에 한다. 어느 쪽이 얼마를 기여하는지 분리해서 측정한다.

1. **커서 페이징**: `?cursor=<마지막 리뷰 id>&size=20`으로 size건씩 이어 읽는다. OFFSET이 아니라 `(created_at, id)` 키셋이므로 페이지가 깊어져도 이미 있던 행을 건너뛰지 않는다. 단, `created_at`은 애플리케이션이 저장 직전에 찍으므로 커서 근처의 `created_at`으로 만들어졌지만 커밋이 늦어진 리뷰는 그 페이징 세션에서 보이지 않고 다음 첫 페이지 로드에서 나타난다.
2. **복합 인덱스** `(reviewee_id, created_at)`: 정렬 방향으로 인덱스를 역방향 스캔해 size건에서 멈춘다. InnoDB 보조 인덱스에는 PK(`id`)가 붙어 있으므로 `id` 동률 처리도 인덱스 안에서 끝난다.

## 측정 대상

| 구분 | API / 쿼리 | 의도 |
| --- | --- | --- |
| 목록 (개선 전) | `GET /api/members/{memberId}/reviews` | 전체 목록 응답 비용 (기준점) |
| 목록 첫 페이지 | `GET …/reviews?size=20` | `ORDER BY created_at DESC, id DESC LIMIT 21` |
| 목록 깊은 페이지 | `GET …/reviews?size=20&cursor=500000` | 키셋 조건이 붙어도 비용이 커지지 않는지 |
| SQL | `benchmark/sql/explain-review-list.sql` | 첫 페이지·깊은 페이지의 EXPLAIN |

## 사전 조건

1. `docs/performance/review-stats-ladder.md`의 사전 조건과 같은 데이터(피리뷰어 1명에게 100만 건, 계정 `admin@jikchin.com`)를 쓴다.
2. 통계 사다리의 실험 인덱스 `idx_reviews_reviewee_score`는 `benchmark/sql/without-review-score-index.sql`로 제거해 둔다. 같은 테이블이라 서로의 측정에 끼어든다.
3. 개선 전 조건은 페이징 이전 코드의 jar로, 개선 후 조건은 페이징 코드의 jar로 실행한다. 한 조건 안에서는 서버를 재시작하지 않는다.

## 실행 순서

결과 파일은 `benchmark/results/review-list-1m/<조건>-<회차>.json`. 각 조건은 워밍업 30초 1회 후 60초 × 3회.

```bash
# 개선 전 (전체 목록). 요청 하나가 약 4초·159MB라 겹치면 1GB 힙이 버티지 못하므로 0.2 rps(5초에 1건)로 잰다. k6의 rate는 정수만 받아 TIME_UNIT으로 표현한다.
k6 run -e MEMBER_ID=1 -e RATE=1 -e TIME_UNIT=5s -e DURATION=60s \
  --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
  --summary-export=benchmark/results/review-list-1m/full-list-1.json \
  benchmark/k6/review-list-read.js

# 개선 후. 첫 페이지와 깊은 페이지를 각각, 인덱스 없이/있이 잰다. 통계 사다리와 같은 2 rps.
docker exec -i jikchin-mysql mysql -ujikchin_local -p"$MYSQL_PASSWORD" jikchin_benchmark < benchmark/sql/without-review-created-index.sql
k6 run -e MEMBER_ID=1 -e SIZE=20 -e RATE=2 -e DURATION=60s ... --summary-export=.../cursor-no-index-1.json benchmark/k6/review-list-read.js
k6 run -e MEMBER_ID=1 -e SIZE=20 -e CURSOR=500000 -e RATE=2 ... --summary-export=.../cursor-no-index-deep-1.json benchmark/k6/review-list-read.js
docker exec -i jikchin-mysql mysql -ujikchin_local -p"$MYSQL_PASSWORD" jikchin_benchmark < benchmark/sql/with-review-created-index.sql
k6 run -e MEMBER_ID=1 -e SIZE=20 -e RATE=2 ... --summary-export=.../cursor-with-index-1.json benchmark/k6/review-list-read.js
k6 run -e MEMBER_ID=1 -e SIZE=20 -e CURSOR=500000 -e RATE=2 ... --summary-export=.../cursor-with-index-deep-1.json benchmark/k6/review-list-read.js
```

깊은 페이지 커서 `500000`은 리뷰 id다. 시드의 `created_at`은 id 순서와 무관하게 흩어져 있고, 이 리뷰는 최신순으로 **950,859번째**(100만 건의 95% 지점)다. OFFSET 방식이라면 95만 행을 건너뛰어야 하는 위치다.

## 측정 결과 (2026-09-14)

### 개선 전: 전체 목록

`EXPLAIN`: `key = idx_reviews_reviewee`, `Using filesort`. `EXPLAIN ANALYZE` 616ms(인덱스 조회 424ms + 정렬). API p50은 약 2.9초, 응답 159MB. DB 0.6초보다 엔티티 100만 개 적재와 JSON 직렬화(약 2.2초)가 더 크다. 2 rps로 겹치면 1GB 힙이 버티지 못해 0.2 rps로 쟀고, 그 조건에서도 `p(95) < 500ms` 임계값은 실패했다.

### 페이징만 (인덱스 없음)

`LIMIT 21`이 붙어도 `EXPLAIN`은 여전히 `Using filesort`다. `EXPLAIN ANALYZE` 첫 페이지 448ms, 깊은 페이지 486ms(100만 행 조회 후 `created_at < cursor` 필터로 49,141행 남기고 정렬). 응답 크기는 수 KB로 줄지만 DB는 여전히 그룹 전체를 읽고 정렬한다.

API p50은 첫 페이지 약 440ms, 깊은 페이지 약 1,050ms. 응답이 159MB에서 4KB로 줄어 직렬화 비용은 사라졌지만 DB 정렬이 남아 통계 0단계와 같은 수준이다. 깊은 페이지가 더 느린 것은 키셋 조건이 붙은 100만 행 필터 + 정렬이 첫 페이지의 단순 정렬보다 비싸기 때문이며, 커서가 깊을수록 비용이 늘어나는 구조다.

### 페이징 + 인덱스 `(reviewee_id, created_at)`

`EXPLAIN` 첫 페이지: `idx_reviews_reviewee_created` 역방향 인덱스 조회, `Limit 21`, 실제 rows 21, 0.02ms. 깊은 페이지: `type = range`, `Using index condition; Backward index scan`, 키셋 조건이 인덱스 구간 `(reviewee_id = 1 AND created_at < cursor) OR (… = cursor AND id < 500000)`으로 그대로 변환돼 실제 rows 21, 0.05ms. filesort는 두 경우 모두 사라졌다.

API p50은 첫 페이지 약 12ms, 깊은 페이지 약 14ms. 95% 지점의 깊은 페이지가 첫 페이지와 같은 비용이며, 남은 시간은 JWT 검증·회원 확인·커서 리뷰 PK 조회 등 고정 비용이다.

### 정리

| 조건 | 첫 페이지 p50 | 깊은 페이지 p50 | 응답 크기 | DB가 읽는 행 |
| --- | ---: | ---: | ---: | ---: |
| 개선 전 전체 목록 | 2,900ms | — | 159 MB | 1,000,000 + 정렬 |
| 페이징만 | 440ms | 1,050ms | 4 KB | 1,000,000 + 정렬 |
| 페이징 + 인덱스 | 12ms | 14ms | 4 KB | 21 |

페이징은 응답 크기(네트워크·직렬화)를, 인덱스는 DB 정렬을 각각 없앤다. 둘 중 하나만으로는 부족하다. 커서 방식이라 페이지 깊이에 따른 비용 증가가 없다는 점은 인덱스가 있을 때만 성립한다.

`idx_reviews_reviewee`는 새 인덱스의 왼쪽 접두사와 중복이라 엔티티 선언에서 제거했다. `ddl-auto: update`는 기존 인덱스를 지우지 않으므로 기존 환경은 `ALTER TABLE reviews DROP INDEX idx_reviews_reviewee;` 한 번으로 정리한다. 실험 SQL(`with-/without-*.sql`)은 변인 통제를 위해 이 인덱스를 양쪽 조건에 유지한다.

## 판정 방법

| 항목 | 기대 | 확인할 값 |
| --- | --- | --- |
| 응답 크기 | 159MB → 수 KB | k6 `data_received` |
| 페이징만 (인덱스 없음) | 응답은 작아지지만 DB는 여전히 100만 행 정렬 | `EXPLAIN` `Using filesort`, p50 수백 ms |
| 페이징 + 인덱스 | 21행만 읽음 | `EXPLAIN` `Backward index scan`, filesort 소멸, p50 수 ms |
| 깊은 페이지 | 인덱스가 있으면 첫 페이지와 같은 비용 | 첫 페이지와 깊은 페이지 p50 차이 |
| 오류율 | 모든 조건 1% 미만 | `http_req_failed` |

## 결과 기록 양식

| 조건 | 실행 | p50 / p95 / p99 (ms) | RPS | 실패율 | dropped | 응답 크기 | EXPLAIN Extra |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 개선 전 전체 목록 (0.2 rps) | 1 | 2795 / 3023 / 3024 | 0.22 | 0% | 0 | 159 MB/건 | Using filesort |
| 개선 전 전체 목록 (0.2 rps) | 2 | 2879 / 3162 / 3329 | 0.22 | 0% | 0 | 159 MB/건 | Using filesort |
| 개선 전 전체 목록 (0.2 rps) | 3 | 2958 / 3138 / 3140 | 0.22 | 0% | 0 | 159 MB/건 | Using filesort |
| 페이징, 인덱스 없음, 첫 페이지 | 1 | 439.8 / 444.5 / 450.6 | 2.02 | 0% | 0 | 4 KB/건 | Using filesort |
| 페이징, 인덱스 없음, 첫 페이지 | 2 | 440.4 / 519.1 / 699.5 | 2.02 | 0% | 0 | 4 KB/건 | Using filesort |
| 페이징, 인덱스 없음, 첫 페이지 | 3 | 437.2 / 440.7 / 470.8 | 2.01 | 0% | 0 | 4 KB/건 | Using filesort |
| 페이징, 인덱스 없음, 깊은 페이지 | 1 | 1056.5 / 1092.7 / 1098.9 | 2.00 | 0% | 0 | 4 KB/건 | Using where; Using filesort |
| 페이징, 인덱스 없음, 깊은 페이지 | 2 | 1050.2 / 1086.8 / 1116.2 | 2.00 | 0% | 0 | 4 KB/건 | Using where; Using filesort |
| 페이징, 인덱스 없음, 깊은 페이지 | 3 | 1050.4 / 1092.3 / 1112.8 | 2.00 | 0% | 0 | 4 KB/건 | Using where; Using filesort |
| 페이징 + 인덱스, 첫 페이지 | 1 | 11.8 / 13.5 / 20.0 | 2.01 | 0% | 0 | 4 KB/건 | Backward index scan |
| 페이징 + 인덱스, 첫 페이지 | 2 | 12.2 / 13.8 / 16.6 | 2.01 | 0% | 0 | 4 KB/건 | Backward index scan |
| 페이징 + 인덱스, 첫 페이지 | 3 | 12.3 / 15.0 / 18.5 | 2.03 | 0% | 0 | 4 KB/건 | Backward index scan |
| 페이징 + 인덱스, 깊은 페이지 | 1 | 13.1 / 15.5 / 15.9 | 2.03 | 0% | 0 | 4 KB/건 | Using index condition; Backward index scan |
| 페이징 + 인덱스, 깊은 페이지 | 2 | 14.2 / 16.4 / 40.2 | 2.03 | 0% | 0 | 4 KB/건 | Using index condition; Backward index scan |
| 페이징 + 인덱스, 깊은 페이지 | 3 | 13.8 / 21.1 / 36.3 | 2.03 | 0% | 0 | 4 KB/건 | Using index condition; Backward index scan |
