#!/bin/bash
set -euo pipefail
exec > >(tee /var/log/user-data.log | logger -t user-data -s 2>/dev/console) 2>&1
umask 077

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-631718647278}"
ECR_REPOSITORY="${ECR_REPOSITORY:-jikchin/app}"
IMAGE_TAG="${IMAGE_TAG:-2e1de7f}"
REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE="${REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
export AWS_DEFAULT_REGION="$AWS_REGION" AWS_PAGER=""

retry() {
  local attempt
  for attempt in {1..5}; do
    if "$@"; then return 0; fi
    if [ "$attempt" -eq 5 ]; then return 1; fi
    sleep 5
  done
}

[ "$(uname -m)" = aarch64 ] || { echo 'ARM64 AMI가 필요합니다.'; exit 1; }
retry dnf install -y docker
command -v aws >/dev/null
systemctl enable --now docker
retry docker info >/dev/null

# 새 EC2 전용: 기존 서비스 컨테이너를 강제로 교체하지 않습니다.
if docker container inspect my-app >/dev/null 2>&1; then
  echo 'my-app이 이미 존재합니다. 새 EC2에서 실행하세요.'
  exit 1
fi

install -d -m 700 /etc/jikchin
TEMP_FILE=$(mktemp /etc/jikchin/app.env.XXXXXX)
trap 'rm -f "$TEMP_FILE"' EXIT
for KEY in JWT_SECRET SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD; do
  VALUE=$(retry aws ssm get-parameter \
    --region "$AWS_REGION" --name "/jikchin/prod/${KEY}" \
    --with-decryption --query Parameter.Value --output text)
  if [[ -z "$VALUE" || "$VALUE" == *$'\n'* || "$VALUE" == *$'\r'* ]]; then
    echo "파라미터가 비어 있거나 여러 줄입니다: $KEY"
    exit 1
  fi
  printf '%s=%s\n' "$KEY" "$VALUE" >> "$TEMP_FILE"
done
unset VALUE
mv "$TEMP_FILE" /etc/jikchin/app.env

login_ecr() {
  aws ecr get-login-password --region "$AWS_REGION" \
    | docker login --username AWS --password-stdin "$REGISTRY"
}
retry login_ecr
retry docker pull "$IMAGE"
docker run -d --name my-app --restart unless-stopped --stop-timeout 60 \
  -p 8080:8080 --env-file /etc/jikchin/app.env \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_SHUTDOWN=graceful \
  -e SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=45s \
  -e JAVA_TOOL_OPTIONS='-XX:MaxRAMPercentage=75.0' \
  --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 \
  "$IMAGE"
# 이미지에 포함된 curl로 실제 HTTP 200 응답을 확인합니다.
for attempt in {1..60}; do
  STATUS=$(docker exec my-app curl -s -o /dev/null -w '%{http_code}' \
    --max-time 3 http://127.0.0.1:8080/actuator/health 2>/dev/null || true)
  if [ "$STATUS" = 200 ]; then
    echo 'Spring 상태 검사 성공. 트래픽 연결 여부는 ALB가 판단합니다.'
    exit 0
  fi
  sleep 5
done

echo 'Spring 상태 검사 실패. /actuator/health가 HTTP 200을 반환하지 않았습니다.'
docker stop --time 60 my-app >/dev/null || true
exit 1
