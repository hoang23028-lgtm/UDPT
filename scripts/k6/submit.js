/**
 * k6 load script — POST submit quiz (distributed / idempotency demo).
 *
 * Chạy (từ thư mục repo):
 *   k6 run -e QUIZ_ID=<id> -e TOKEN=<jwt> -e ANSWERS_JSON='{"<questionId>":0}' scripts/k6/submit.js
 *
 * Biến môi trường:
 *   BASE_URL  (optional) mặc định http://localhost
 *   QUIZ_ID   (required)
 *   TOKEN     (required) Bearer JWT
 *   ANSWERS_JSON (required) object JSON string, ví dụ '{"123":0,"456":1}'
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-vus',
      vus: 50,
      duration: '2m',
    },
  },
};

const base = __ENV.BASE_URL || 'http://localhost';

export default function () {
  const quizId = __ENV.QUIZ_ID;
  const token = __ENV.TOKEN;
  const answersJson = __ENV.ANSWERS_JSON;
  if (!quizId || !token || !answersJson) {
    throw new Error('Thiếu biến môi trường: QUIZ_ID, TOKEN, ANSWERS_JSON');
  }

  const idem = `${__VU}-${__ITER}-${Date.now()}`;
  const url = `${base}/api/quizzes/${quizId}/submit`;
  const body = JSON.stringify({ answers: JSON.parse(answersJson) });

  const res = http.post(url, body, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      'Idempotency-Key': idem,
    },
    timeout: '30s',
  });

  check(res, {
    'status ok': (r) => [200, 201, 409].includes(r.status),
  });

  sleep(0.1);
}
