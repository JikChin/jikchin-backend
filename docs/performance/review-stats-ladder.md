# 리뷰 별점 통계 성능 개선 사다리

`GET /api/members/{memberId}/reviews/stats`는 한 회원이 받은 리뷰를 `GROUP BY score`로 집계한다. 이 쿼리를 소재로 **인덱스 → 읽기 전용 집계 테이블 → 마이크로 배치 → Redis 캐시** 순서로 올라가며, 각 단계에서 **얻는 성능과 치르는 희생을 같은 데이터·서버·요청량으로 측정**한다.

목록 조회가 아니라 집계를 소재로 삼은 이유: `COUNT(*)`는 그룹의 모든 행을 읽어야 한다. 인덱스는 행당 비용을 줄이지만 O(N) 자체는 바꾸지 못하므로, 다음 단계로 올라갈 이유가 실제 측정으로 드러난다.

## 측정 대상

| 구분 | API / 쿼리 | 의도 |
| --- | --- | --- |
| 읽기 | `GET /api/members/{memberId}/reviews/stats` | `reviewee_id = ?` 뒤 `GROUP BY score` 집계 비용 |
| 읽기 SQL | `SELECT score, COUNT(*) FROM reviews WHERE reviewee_id = ? GROUP BY score` | `benchmark/sql/explain-review-stats.sql` |
| 쓰기 | `POST /api/reviews` | 보조 인덱스·집계 테이블 갱신이 리뷰 작성에 주는 비용 (1단계부터) |

## 사전 조건

1. 로컬 전용 MySQL 데이터베이스를 사용한다. 운영 데이터에는 인덱스 제거·대량 시드를 실행하지 않는다.
2. `docker compose up -d mysql` 후 애플리케이션을 실행해 JPA 테이블을 생성한다.
3. `benchmark/http/k6-auth.http`의 회원가입을 1회 실행한다. 이 계정(`admin@jikchin.com`)이 리뷰를 받는 **피리뷰어**이며, k6 로그인 계정이기도 하다.
4. `benchmark/sql/bootstrap-review-fixture.sql`을 실행하고 출력된 `reviewee_id`를 기록한다. 시드 리뷰가 참조할 메이트 글 1건을 만든다.
5. `benchmark/sql/seed-reviews-large.sql`을 실행한다. 피리뷰어 한 명에게 **100만 건**을 몰아넣는다. 로컬 MySQL에서 수 분이 걸리며, 마지막 `SELECT`의 `total_reviews`가 1,000,000인지 확인한다.
6. 쓰기 테스트용으로 작성자 계정 `k6-writer@jikchin.com`(비밀번호 `password1234`)을 가입시키고 `benchmark/sql/bootstrap-review-write-fixture.sql`을 실행한다. `POST /api/reviews`는 (메이트 글, 작성자, 피리뷰어) 조합당 1건만 허용하므로, 두 계정이 ACTIVE 멤버로 들어간 메이트 글 3,000건을 시드하고 요청마다 다른 글에 쓴다. 출력된 `post_id_start`를 기록한다.
7. 한 조건의 워밍업부터 3회 측정 사이에는 서버를 재시작하지 않는다. `Review` 엔티티가 선언한 인덱스(`idx_reviews_reviewee`, `fk_reviews_reviewer`)는 모든 조건에서 유지되므로 조건 사이의 재시작은 실험 조건을 바꾸지 않는다.

## 실행 순서

각 조건은 워밍업 30초 1회 후, 측정 60초를 3회 수행한다. 결과 파일은 `benchmark/results/review-stats-1m/<조건>-<회차>.json`으로 저장한다.

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e MEMBER_ID=<reviewee_id> \
  -e RATE=2 \
  -e DURATION=60s \
  --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
  --summary-export=benchmark/results/review-stats-1m/baseline-1.json \
  benchmark/k6/review-stats-read.js
```

`RATE=2`로 고정한 이유: 0단계는 요청 하나가 100만 행을 읽어 약 430ms가 걸린다. 1~2 rps에서는 p95가 안정적이지만 5 rps에서는 p95가 4초를 넘고 VU가 대기에 묶인다. 모든 단계를 포화되지 않는 같은 요청량으로 측정해야 단계 간 차이가 큐 대기가 아니라 쿼리 비용의 차이로 읽힌다. `--summary-trend-stats`는 k6 기본 요약에 없는 p99를 결과 파일에 남기기 위한 것이다.

쓰기 테스트는 매 실행 전에 `benchmark/sql/clear-k6-reviews.sql`로 직전 실행이 쓴 리뷰를 지워 같은 메이트 글을 다시 쓸 수 있게 한다.

```bash
docker exec -i jikchin-mysql mysql -ujikchin_local -p"$MYSQL_PASSWORD" jikchin_benchmark < benchmark/sql/clear-k6-reviews.sql
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e REVIEWEE_ID=<reviewee_id> \
  -e POST_ID_START=<post_id_start> \
  -e RATE=2 \
  -e DURATION=60s \
  --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
  --summary-export=benchmark/results/review-stats-1m/baseline-write-1.json \
  benchmark/k6/review-write.js
```

액세스 토큰 만료는 15분이다. `DURATION`을 그 이상으로 늘리면 `setup()`에서 받은 토큰이 만료되므로 한 실행은 15분 미만으로 잡는다.

### 0단계. 베이스라인 (인덱스 없음)

`benchmark/sql/without-review-score-index.sql`을 실행한 뒤 읽기 테스트를 수행한다. 엔티티가 선언한 `idx_reviews_reviewee(reviewee_id)`만 존재한다.

`benchmark/sql/explain-review-stats.sql`에서 다음이 보여야 한다.

- `key = idx_reviews_reviewee`, `Extra`에 `Using temporary`
- 보조 인덱스에는 `score`가 없으므로 그룹의 모든 행에 대해 클러스터드 인덱스를 다시 읽는다.

측정 결과 (2026-09-14, MySQL 8.4.11 도커, 앱 `-Xmx1g`): `EXPLAIN ANALYZE` 실제 시간 488ms, 인덱스 lookup 415ms + 임시 테이블 집계. 옵티마이저 추정 rows는 497,075였으나 실제 rows는 1,000,000이다. 통계 API p50은 3회 모두 약 426ms로, 응답 시간의 대부분이 이 쿼리다.

쓰기(`POST /api/reviews`, 피리뷰어 1명에게 집중) p50도 3회 모두 약 442ms다. INSERT 자체가 아니라 작성 트랜잭션 끝의 `updateMannerScore`가 `AVG(score) WHERE reviewee_id = ?`로 같은 100만 행을 훑기 때문이다. 이 도메인은 읽기뿐 아니라 쓰기도 O(N)이며, 이 값이 1단계 이후 쓰기 비교의 기준이다.

### 1단계. 복합 인덱스 `(reviewee_id, score)`

`benchmark/sql/with-review-score-index.sql`을 실행한 뒤 **같은 환경 변수와 요청량**으로 읽기 테스트를 반복한다.

`explain-review-stats.sql`에서 다음이 보여야 한다.

- `key = idx_reviews_reviewee_score`, `Extra = Using index` (커버링), `Using temporary` 소멸
- `EXPLAIN ANALYZE`의 실제 rows는 0단계와 같다. 읽는 행 수는 그대로이고 행당 비용만 준 것이다.

**희생**: 리뷰 INSERT마다 B-tree 하나를 더 갱신한다. 쓰기 테스트로 p95·처리량 악화 폭을 기록한다. 또한 실험에서는 변인을 하나로 유지하려고 `idx_reviews_reviewee`를 남겨두지만, 운영에서는 복합 인덱스가 `reviewee_id` 단독 조회도 처리하므로 단일 인덱스는 중복이다.

측정 결과 (2026-09-14, 0단계와 같은 서버·데이터·2 rps): `EXPLAIN`은 `key = idx_reviews_reviewee_score`, `Extra = Using index`이고 `Using temporary`가 사라졌다. `EXPLAIN ANALYZE` 실제 시간은 인덱스 생성 직후 첫 실행 143ms, 이후 86~105ms(0단계 488ms)이며 실제 rows는 그대로 1,000,000이다. 통계 API p50은 3회 모두 약 111ms로 0단계 426ms의 약 1/4이다.

쓰기 p50은 442ms → 약 170ms로 **오히려 개선**됐다. 예상한 B-tree 갱신 비용(수 ms)보다 `updateMannerScore`의 `AVG(score) WHERE reviewee_id = ?`가 같은 복합 인덱스를 커버링으로 타서 얻은 이득(약 400ms → 85ms)이 훨씬 크기 때문이다. 즉 이 단계에서 "쓰기 처리량 희생"은 측정상 드러나지 않았고, 순수 INSERT 비용을 보려면 리뷰가 없는 피리뷰어에게 써야 한다. 남은 쓰기 170ms 중 대부분은 여전히 O(N)인 AVG 스캔이며, 이것이 2단계 집계 테이블이 흡수할 대상이다.

### 2단계. 읽기 전용 집계 테이블 (동기 갱신)

`review_stats(reviewee_id PK, total_count, score_sum, count_1 … count_5)`를 두고 리뷰 작성 트랜잭션 안에서 함께 갱신한다. 조회는 PK 1건, O(1).

**희생**: 같은 피리뷰어에게 동시에 들어오는 리뷰가 그 행의 배타 락에서 직렬화된다. 현재 `ReviewRepository.updateMannerScore`가 이미 같은 구조(리뷰마다 `members` 행 UPDATE)이므로, 쓰기 테스트에서 한 피리뷰어에게 리뷰를 집중시켜 락 대기를 드러낸다.

### 3단계. 마이크로 배치

동기 갱신을 떼고, 일정 주기로 변경분만 모아 집계 테이블을 갱신한다.

**희생**: 최종 일관성. 리뷰를 쓰고 통계에 반영되기까지 지연이 생긴다. 트레이드오프 축이 "읽기 대 쓰기"에서 "성능 대 정합성"으로 바뀌며, 허용 지연은 개발이 아니라 서비스가 정할 값이다.

### 4단계. Redis 캐시

집계 결과를 Redis에 두고 DB를 거치지 않는다.

**희생**: 인프라 1개 추가, 캐시 무효화 설계, 메모리 비용, 장애 시 DB와 불일치 가능성. 앞 단계로 충분하면 여기까지 오지 않는다.

## 판정 방법

| 항목 | 기대 | 확인할 값 |
| --- | --- | --- |
| 읽기 p95 | 단계마다 감소 | `http_req_duration` p95 |
| 읽기 DB 계획 | 1단계에서 `Using temporary` 소멸, `Using index` 등장 | `EXPLAIN` Extra |
| 읽기 실제 시간 | 1단계에서 감소하되 rows는 동일 | `EXPLAIN ANALYZE` |
| 쓰기 p95·처리량 | 1·2단계에서 악화, 3단계에서 회복 (1단계 실측은 `updateMannerScore` 덕에 개선, 1단계 절 참고) | `http_req_duration` p95, `http_reqs` |
| 오류율 | 모든 조건 1% 미만 | `http_req_failed` |

## 트레이드오프 기록 양식

| 단계 | 읽기 p50/p95/p99 (ms) | 읽기 RPS | 쓰기 p50/p95/p99 (ms) | 쓰기 RPS | EXPLAIN Extra | 희생한 것 |
| --- | ---: | ---: | ---: | ---: | --- | --- |
| 0. 베이스라인 | 426 / 437 / 459 | 2.02 | 442 / 449 / 456 | 2.02 | Using temporary | — |
| 1. 복합 인덱스 | 111 / 113 / 114 | 2.03 | 170 / 175 / 177 | 2.02 | Using index | 인덱스 1개 유지 비용(측정상 미미) |
| 2. 집계 테이블(동기) | | | | | | 쓰기 락 경합, 코드 복잡도 |
| 3. 마이크로 배치 | | | | | | 정합성 지연 |
| 4. Redis | | | | | | 운영 복잡도, 무효화 설계 |

## 결과 기록 양식

| 조건 | 실행 | 읽기 p50/p95/p99 (ms) | 읽기 RPS | 쓰기 p50/p95/p99 (ms) | 쓰기 RPS | 실패율 | dropped iterations | EXPLAIN 실제 시간 (ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 0. 베이스라인 | 1 | 426.1 / 432.6 / 434.6 | 2.02 | 443.7 / 448.3 / 450.0 | 2.01 | 0% | 0 | 488 |
| 0. 베이스라인 | 2 | 426.1 / 437.0 / 485.5 | 2.02 | 441.4 / 446.0 / 463.1 | 2.02 | 0% | 0 | 488 |
| 0. 베이스라인 | 3 | 426.3 / 440.8 / 455.4 | 2.02 | 441.8 / 453.1 / 455.2 | 2.02 | 0% | 0 | 488 |
| 1. 복합 인덱스 | 1 | 110.9 / 113.4 / 114.2 | 2.03 | 170.8 / 174.9 / 176.3 | 2.01 | 0% | 0 | 105 |
| 1. 복합 인덱스 | 2 | 111.5 / 113.0 / 113.6 | 2.03 | 169.9 / 173.9 / 176.0 | 2.02 | 0% | 0 | 86 |
| 1. 복합 인덱스 | 3 | 110.7 / 112.4 / 114.9 | 2.03 | 169.3 / 174.7 / 177.5 | 2.02 | 0% | 0 | 89 |
