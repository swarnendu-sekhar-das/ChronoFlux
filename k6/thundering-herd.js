import http from 'k6/http';
import { check, sleep } from 'k6';

// This load test simulates a "thundering herd" scenario where thousands of game clients
// all request the active events endpoint at the exact same time when a push notification
// or in-game timer triggers.

export const options = {
  // Ramp up to 5000 users over 5 seconds, stay there for 20 seconds, ramp down over 5.
  stages: [
    { duration: '5s', target: 5000 },
    { duration: '20s', target: 5000 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    // 95% of requests must complete below 50ms (achievable via Redis)
    http_req_duration: ['p(95)<50'],
    // Less than 1% error rate
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // Hit the Edge API which is load balanced via NGINX and heavily cached via Redis
  const res = http.get('http://localhost/api/v1/events/active');
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response is json': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
  });

  // Small sleep to emulate client processing time before their next background poll
  sleep(1);
}
