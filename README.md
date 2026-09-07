# jikchin-backend
직친(JikChin)- 스포츠 직관 매이트 매칭 서비스


## 로컬 JWT 설정

1. `.env.example`을 프로젝트 루트의 `.env`로 복사합니다.
2. `openssl rand -base64 32`로 키를 생성하고 `.env`의 `JWT_SECRET`에 입력합니다.
3. 프로젝트 루트를 실행 작업 디렉터리로 설정하여 애플리케이션을 실행합니다.

`.env`는 `KEY=value` 형식으로 작성하며 값에 따옴표나 `export`를 붙이지 않습니다.
`application.yaml`이 `.env`를 properties 형식으로 읽어 JWT 설정에 적용합니다.
`.env`는 Git에서 제외되며, 공유용 `.env.example`에는 실제 비밀키를 넣지 않습니다.

- `JWT_SECRET`: Base64로 인코딩한 최소 32바이트 키 (필수, 기본값 없음)
- `JWT_ISSUER`: 발급자 (기본값 `jikchin-backend`)
- `JWT_ACCESS_TOKEN_EXPIRATION`: 액세스 토큰 유효기간 (기본값 `15m`)
- `JWT_REFRESH_TOKEN_EXPIRATION`: 리프레시 토큰 유효기간 (기본값 `14d`)

배포 환경에서는 `.env` 없이 동일한 이름의 환경변수로 설정할 수 있습니다.
테스트는 `src/test/resources/application.yaml`의 별도 테스트 설정을 사용합니다.


## 로컬 MySQL 연결

`.env.example`의 `MYSQL_*` 값을 `.env`에 설정합니다. 예시 비밀번호는 로컬 개발용입니다.
Docker Compose와 Spring이 같은 `.env`의 DB 이름, 사용자, 비밀번호를 사용합니다.

1. 프로젝트 루트에서 `docker compose up -d mysql`을 실행합니다.
2. IntelliJ의 실행 설정에서 Working directory를 프로젝트 루트로 지정합니다.
3. Spring 애플리케이션을 실행합니다.

호스트 접속 포트는 `3306`입니다. 포트를 변경할 때는 Docker Compose의 호스트 포트와 `.env`의 `MYSQL_PORT`를 동일하게 맞춥니다.
컨테이너 내부 MySQL 포트는 `3306`입니다.
기존 MySQL 볼륨이 있으면 사용자와 비밀번호는 최초 초기화 값을 유지하므로 `.env`에도 해당 값을 사용해야 합니다.

로컬 `.env`의 `JPA_DDL_AUTO=update`는 실행 시 엔티티에 맞춰 테이블을 생성·갱신합니다.
이 값을 지정하지 않으면 기본값은 `none`이며, 운영에서는 별도 마이그레이션으로 스키마를 관리합니다.
