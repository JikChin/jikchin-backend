import http from 'k6/http';
import {check} from 'k6';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const revieweeId = Number(__ENV.REVIEWEE_ID);
const postIdStart = Number(__ENV.POST_ID_START);
const email = __ENV.EMAIL || 'k6-writer@jikchin.com';
const password = __ENV.PASSWORD || 'password1234';

if (!revieweeId || !postIdStart) {
  throw new Error(
    'REVIEWEE_ID(리뷰를 받을 회원 ID), POST_ID_START(bootstrap-review-write-fixture.sql이 출력한 post_id_start)가 필요합니다.',
  );
}

export const options = {
  scenarios: {
    write_reviews: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 2),
      timeUnit: '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 10),
      maxVUs: Number(__ENV.MAX_VUS || 50),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

// 읽기 스크립트와 같은 이유로 로그인은 setup()에서 한 번만 한다.
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

// 같은 (메이트 글, 작성자, 피리뷰어) 조합은 한 번만 리뷰할 수 있으므로 요청마다 다른 글을 쓴다.
// iterationInTest는 모든 VU에 걸쳐 0부터 증가하는 전역 번호라 글이 겹치지 않는다.
export default function (data) {
  const iteration = exec.scenario.iterationInTest;
  const payload = JSON.stringify({
    matePostId: postIdStart + iteration,
    revieweeId,
    // 시드와 같은 분포: 5점 50%, 4점 30%, 3점 10%, 2점 5%, 1점 5%
    score: [5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 3, 3, 2, 1][iteration % 20],
    content: `k6-write-marker-${iteration}`,
  });
  const response = http.post(`${baseUrl}/api/reviews`, payload, {
    headers: {
      Authorization: `Bearer ${data.accessToken}`,
      'Content-Type': 'application/json',
    },
  });
  check(response, {
    'HTTP 201': (result) => result.status === 201,
    'returns success response': (result) => result.json('resultType') === 'SUCCESS',
  });
}
