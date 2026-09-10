# Event 인덱스 k6 비교

`events`의 복합 인덱스 `(sport_id, starts_at)`가 실제 API 읽기와 경기 생성 쓰기에 주는 영향을 확인한다. 같은 데이터·서버·요청량에서 **보조 인덱스 없음**과 **복합 인덱스만 존재**를 비교한다.

## 측정 대상

| 구분 | API | 의도 |
| --- | --- | --- |
| 읽기 | `GET /api/events?sportId={id}&from={ISO_LOCAL_DATE_TIME}&to={ISO_LOCAL_DATE_TIME}&size=20` | `sport_id = ?`와 기간 조건 뒤 `starts_at ASC` 정렬을 복합 인덱스로 처리하는지 확인 |
| 쓰기 | `POST /api/admin/events` | 동일한 INSERT에서 보조 인덱스 유지 비용을 확인 |

읽기는 20건으로 제한한다. 전체 이벤트를 반환하면 DB 인덱스보다 JSON 직렬화와 네트워크 전송 시간이 결과를 지배하기 때문이다.

## 사전 조건

1. 로컬 전용 MySQL 데이터베이스를 사용한다. 운영 데이터에는 인덱스 제거·대량 시드를 실행하지 않는다.
2. 애플리케이션을 먼저 실행해 JPA 테이블을 생성한다.
3. `benchmark/sql/bootstrap-event-catalog.sql`을 실행하고 출력된 ID를 기록한다. 네 종목과 각 종목의 홈·원정팀을 생성한다.
4. `benchmark/sql/seed-events.sql` 상단의 `venue_id`를 수정한 뒤 실행한다. 기본값은 10만 건이며 네 종목에 균등 분산된다.
5. 서버를 재시작하지 않는다. `ddl-auto: update` 환경에서 재시작하면 Hibernate가 제거한 인덱스를 다시 만들 수 있다.

## 실행 순서

각 시나리오는 워밍업 30초 1회 후, 측정 60초를 3회 수행한다. 각 실행 뒤에는 결과 파일에 p50/p95/p99, 실패율, 처리량을 기록한다.

### 1. 보조 인덱스 없음

`benchmark/sql/without-secondary-index.sql`을 실행한 뒤 아래 읽기 테스트를 수행한다.

```bash
k6 run -e SPORT_ID=1 -e FROM=2026-10-01T00:00:00 -e TO=2026-12-01T00:00:00 -e RATE=100 benchmark/k6/event-read.js
```

관리자 액세스 토큰과 카탈로그 ID를 넣어 쓰기 테스트도 실행한다.

```bash
k6 run -e ADMIN_ACCESS_TOKEN=... -e SPORT_ID=1 -e VENUE_ID=1 -e HOME_TEAM_ID=1 -e AWAY_TEAM_ID=2 -e RATE=20 benchmark/k6/event-write.js
```

### 2. 복합 인덱스만 존재

`benchmark/sql/with-composite-index.sql`을 실행한 뒤 위와 **같은 환경 변수와 요청량**으로 읽기·쓰기 테스트를 반복한다.

각 상태에서 `benchmark/sql/explain-read.sql`도 실행한다. 복합 인덱스 상태에서는 `idx_events_sport_starts_at`을 사용하고, filesort와 대량 스캔이 없어야 한다.

## 판정 방법

| 항목 | 복합 인덱스 기대 효과 | 확인할 값 |
| --- | --- | --- |
| 읽기 p95 | 감소 | `http_req_duration` p95 |
| 읽기 DB 계획 | 탐색 범위 감소 | `EXPLAIN ANALYZE`의 실제 rows와 실행 시간 |
| 쓰기 p95·처리량 | 약간 악화할 수 있음 | `http_req_duration` p95, `http_reqs` |
| 오류율 | 두 조건 모두 1% 미만 | `http_req_failed` |

쓰기는 인덱스 추가만큼 B-tree 갱신이 필요해 느려질 수 있다. 읽기 p95가 의미 있게 줄고, 쓰기 비용이 서비스 허용 범위에 있으면 복합 인덱스를 유지한다. 결과가 비슷하면 현재 데이터가 작거나 기간 조건의 선택도가 낮다는 뜻이므로, 이벤트 수를 늘리거나 기간 범위를 좁혀 다시 측정한다.

## 결과 기록 양식

| 상태 | 실행 | 읽기 p50/p95/p99 (ms) | 읽기 RPS | 쓰기 p50/p95/p99 (ms) | 쓰기 RPS | 실패율 | EXPLAIN 실제 rows |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 보조 인덱스 없음 | 1 | | | | | | |
| 보조 인덱스 없음 | 2 | | | | | | |
| 보조 인덱스 없음 | 3 | | | | | | |
| 복합 인덱스만 | 1 | | | | | | |
| 복합 인덱스만 | 2 | | | | | | |
| 복합 인덱스만 | 3 | | | | | | |
