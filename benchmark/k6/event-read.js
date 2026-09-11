import http from 'k6/http';
import {check} from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const sportId = __ENV.SPORT_ID;
const from = __ENV.FROM;
const to = __ENV.TO;
const size = Number(__ENV.SIZE || 20);

if (!sportId || !from || !to || !Number.isInteger(size) || size < 1) {
  throw new Error('SPORT_ID, FROM, TO가 필요하며 SIZE는 설정할 경우 1 이상의 정수여야 합니다.');
}

export const options = {
  scenarios: {
    read_events: {
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

export default function () {
  const url = `${baseUrl}/api/events?sportId=${sportId}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&size=${size}`;
  const response = http.get(url);
  check(response, {
    'HTTP 200': (result) => result.status === 200,
    'returns success response': (result) => result.json('resultType') === 'SUCCESS',
  });
}
