import http from 'k6/http';
import {check} from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const memberId = __ENV.MEMBER_ID;
const email = __ENV.EMAIL || 'admin@jikchin.com';
const password = __ENV.PASSWORD || 'password1234';

if (!memberId) {
  throw new Error('MEMBER_ID(통계를 조회할 회원 ID)가 필요합니다.');
}

export const options = {
  scenarios: {
    read_review_stats: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 100),
      timeUnit: '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 30),
      maxVUs: Number(__ENV.MAX_VUS || 100),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

// 리뷰 API는 인증이 필요하다. 로그인은 BCrypt 검증 비용이 커서 VU 루프 안에서 호출하면
// 측정이 로그인 비용에 묻히므로 setup()에서 한 번만 수행하고 토큰을 공유한다.
export function setup() {
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({email, password}),
    {headers: {'Content-Type': 'application/json'}},
  );
  if (response.status !== 200) {
    throw new Error(`로그인 실패: HTTP ${response.status} ${response.body}`);
  }
  return {accessToken: response.json('data.accessToken')};
}

export default function (data) {
  const response = http.get(`${baseUrl}/api/members/${memberId}/reviews/stats`, {
    headers: {Authorization: `Bearer ${data.accessToken}`},
  });
  check(response, {
    'HTTP 200': (result) => result.status === 200,
    'returns success response': (result) => result.json('resultType') === 'SUCCESS',
    'has total count': (result) => Number.isInteger(result.json('data.totalCount')),
  });
}
