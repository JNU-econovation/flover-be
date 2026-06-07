import http from 'k6/http';
import { check, sleep } from 'k6';
import { buildSummary } from './summary.js';
import { config } from './common.js';

if ((__ENV.RUN_HEAVY_API || 'false').toLowerCase() !== 'true') {
  throw new Error('AI analysis test is disabled. Set RUN_HEAVY_API=true to call the external AI API.');
}

const imageName = 'sample-trash-image.jpg';
const imageContentType = 'image/jpeg';
const imageBytes = open('../data/sample-trash-image.jpg', 'b');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  vus: 1,
  duration: '30s',
  thresholds: {
    checks: ['rate>0.95'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{type:heavy}': ['p(95)<3000'],
  },
};

export default function () {
  const params = {
    tags: { api: 'plogging_analyze', priority: 'P1', type: 'heavy' },
  };
  const body = {
    image: http.file(imageBytes, imageName, imageContentType),
  };
  const res = http.post(
    `${config.baseUrl}/api/plogging/analyze?latitude=37.5665&longitude=126.9780`,
    body,
    params
  );

  check(res, {
    'AI analyze status is 200': (r) => r.status === 200,
  });

  sleep(10);
}

export function handleSummary(data) {
  return buildSummary(data, 'ai-analysis-smoke');
}
