import http from 'k6/http';
import {check} from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const accessToken = __ENV.ADMIN_ACCESS_TOKEN;
const sportId = Number(__ENV.SPORT_ID);
const venueId = Number(__ENV.VENUE_ID);
const homeTeamId = Number(__ENV.HOME_TEAM_ID);
const awayTeamId = Number(__ENV.AWAY_TEAM_ID);

if (!accessToken || !sportId || !venueId || !homeTeamId || !awayTeamId) {
  throw new Error(
    'ADMIN_ACCESS_TOKEN, SPORT_ID, VENUE_ID, HOME_TEAM_ID, AWAY_TEAM_ID 환경 변수가 필요합니다.',
  );
}

export const options = {
  scenarios: {
    create_events: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 20),
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

function futureLocalDateTime() {
  const future = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000 + (__VU * 100000 + __ITER) * 1000);
  return future.toISOString().slice(0, 19);
}

export default function () {
  const payload = JSON.stringify({
    sportId,
    venueId,
    homeTeamId,
    awayTeamId,
    leagueName: `k6 benchmark ${__VU}-${__ITER}`,
    startsAt: futureLocalDateTime(),
  });
  const response = http.post(`${baseUrl}/api/admin/events`, payload, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  });
  check(response, {
    'HTTP 200': (result) => result.status === 200,
    'returns success response': (result) => result.json('resultType') === 'SUCCESS',
  });
}
