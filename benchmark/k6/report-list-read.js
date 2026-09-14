import http from 'k6/http';
import {check} from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const status = __ENV.STATUS;
const size = __ENV.SIZE;
const cursor = __ENV.CURSOR;
const email = __ENV.EMAIL || 'admin@jikchin.com';
const password = __ENV.PASSWORD || 'password1234';

export const options = {
  scenarios: {
    read_admin_reports: {
      executor: 'constant-arrival-rate',
      // k6의 rate는 정수만 받는다. 1 rps 미만은 TIME_UNIT을 늘려 표현한다 (RATE=1, TIME_UNIT=5s → 0.2 rps).
      rate: Number(__ENV.RATE || 2),
      timeUnit: __ENV.TIME_UNIT || '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 5),
      maxVUs: Number(__ENV.MAX_VUS || 20),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

// /api/admin/** 은 ROLE_ADMIN 토큰이 필요하다 (benchmark/sql/promote-k6-admin.sql). 로그인은 한 번만.
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

// SIZE·CURSOR를 주지 않으면 status만 붙인다. 페이징 이전 코드(전체 목록)에도 같은 스크립트를 쓰기 위해서다.
function buildUrl() {
  const params = [];
  if (status) params.push(`status=${status}`);
  if (size) params.push(`size=${size}`);
  if (cursor) params.push(`cursor=${cursor}`);
  const query = params.length > 0 ? `?${params.join('&')}` : '';
  return `${baseUrl}/api/admin/reports${query}`;
}

export default function (data) {
  const response = http.get(buildUrl(), {
    headers: {Authorization: `Bearer ${data.accessToken}`},
    responseType: 'text',
  });
  check(response, {
    'HTTP 200': (result) => result.status === 200,
    'returns success response': (result) => result.json('resultType') === 'SUCCESS',
  });
}
