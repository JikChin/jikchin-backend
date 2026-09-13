# 비관적 락 MySQL 정합성 검증

## 실행

```bash
docker compose -f compose.concurrency.yml up -d --wait --wait-timeout 120
./gradlew mysqlConcurrencyTest
```

- 컨테이너: `jikchin-concurrency-mysql-1`
- MySQL: 8.4 이미지 (2026-09-11 실행 버전 8.4.11)
- 접속: `127.0.0.1:13306`, DB `mate_lock_test`
- 테스트 전용 사용자: `mate_test` / 비밀번호: `local-test-only`
- 격리 수준: 기본 `REPEATABLE-READ`, InnoDB 락 대기 제한 10초
- 테스트 커넥션 풀: 최대 20개
- 저장소: 컨테이너 전용 tmpfs. 컨테이너 중지 시 데이터가 사라짐.
- 테스트는 전용 DB 스키마를 생성·삭제한다. 기존 프로젝트 DB와 분리되어 있다.
- `mysqlConcurrencyTest`는 해당 동시성 테스트만 실행하고 매번 다시 수행한다.
- 일반 `./gradlew test`는 이 테스트에 H2를 사용한다.

테스트 리포트: `build/reports/tests/mysqlConcurrencyTest/index.html`

컨테이너가 필요 없으면 다음 명령으로 종료한다.

```bash
docker compose -f compose.concurrency.yml down
```

## 2026-09-11 수정 전: 18회 중 9회 통과, 9회 실패

운영 코드는 origin/dev 8ccf9db와 동일한 상태에서 검증했다. 6개 시나리오별 동시 요청 20개,
각 3회 반복. HTTP 요청이 아닌 Spring 서비스 프록시를 통한 호출이며 각 호출은 별도 트랜잭션이다.
작업 종료 후 새 조회로 DB 상태를 확인한다. 예상한 업무 거절과 DB 예외를 구별한다.

| 시나리오 | 통과 | 결과 |
|---|---:|---|
| 서로 다른 사용자 동시 신청 | 3/3 | 20건 접수, 인원은 작성자 1명 유지 |
| 마지막 한 자리에 20명 동시 승인 | 3/3 | 1명 승인, 19명 정원 마감으로 거절 |
| 충분한 정원에서 20명 동시 승인 | 3/3 | 20명 승인, 작성자 포함 21명 일치 |
| 동일 사용자 중복 신청 | 0/3 | DB 유일 제약이 중복을 막지만, 예상 업무 예외 대신 DataIntegrityViolationException 발생 |
| 동일 신청 중복 승인 | 0/3 | DB 유일 제약이 중복 멤버를 막지만, 예상 업무 예외 대신 DataIntegrityViolationException 발생 |
| 동일 신청 승인·거절 경쟁 | 0/3 | 활성 멤버가 존재하지만 ACCEPTED 신청이 없는 불일치. 각 반복에서 성공 호출도 1개가 아닌 11개 관측 |

신청과 멤버 중복 여부, ACCEPTED 신청자와 실제 활성 멤버의 일치(작성자 제외),
current_members와 실제 인원수 일치, 정원 초과 및 마감 상태를 검사한다.
예상 밖 DB 예외가 있어도 모든 요청 결과를 수집한 후 상태 검증을 수행한다.

## 원인 분석

현재 apply/accept/reject는 트랜잭션에서 먼저 회원을 일반 조회하고, 모집글을 FOR UPDATE로 조회한다.
MySQL REPEATABLE READ에서는 첫 일반 조회 시점의 스냅샷을 이후 일반 조회도 사용한다.
따라서 모집글 락을 기다렸다가 획득해도, 뒤의 일반 신청 조회와 중복 검사는
앞선 트랜잭션의 커밋을 보지 못할 수 있다. 승인 이후에도 PENDING으로 판단하여
거절 상태를 덮어쓰는 현상과 유일 제약 위반이 이 코드 흐름에 부합한다.

근거: https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html

수정 전 검증에서는 운영 코드나 격리 수준을 변경하지 않았다.

## 2026-09-11 수정 후: 18회 모두 통과

MateApplicationService의 apply/accept/reject에만 다음 설정을 적용했다.

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
```

모집글의 PESSIMISTIC_WRITE는 유지한다. 같은 모집글의 변경을 순차 처리하면서,
락 획득 후 실행하는 일반 조회가 앞선 요청의 커밋 결과를 읽도록 한다.
DB 서버의 기본 REPEATABLE-READ나 다른 서비스의 격리 수준은 변경하지 않았다.

동일한 6개 시나리오를 각 3회 반복하여 모두 통과했다. 중복 신청과 중복 승인은
예상한 업무 예외로 거절되고, 승인·거절 경쟁에서 하나만 성공하며 신청 상태와 참여자가 일치한다.
정원 초과 및 인원 증가 누락 방지도 유지된다.

현재 세 메서드는 컨트롤러에서 호출되어 새 트랜잭션을 시작한다. 향후 기존 트랜잭션 안에서
호출하는 구조로 바꾸면 기본 REQUIRED 전파에 의해 외부 트랜잭션의 격리 수준을 따르므로
그 호출 경로도 검증해야 한다.

이번 실행은 서비스 수준 정합성 검증이며 Grafana 연동이나 HTTP 성능 벤치마크는 포함하지 않는다.
