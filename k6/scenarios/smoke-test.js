import { group } from 'k6';
import { buildSummary } from './summary.js';
import {
  completePloggingWrite,
  getMonthlyStats,
  getMyInfo,
  getMyPloggingStats,
  getNearbyToilets,
  getNearbyTrashBins,
  getPloggingSessionDetail,
  getRouteRecommendation,
  getUploadUrlsSmoke,
  getWeeklyStats,
  listPloggingSessions,
  thinkTime,
} from './common.js';

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  vus: 1,
  duration: '1m',
  thresholds: {
    checks: ['rate>0.95'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{type:read}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{type:route}': ['p(95)<5000'],
    'http_req_duration{type:presigned_url}': ['p(95)<1500'],
    'http_req_duration{type:write}': ['p(95)<2000'],
  },
};

export default function () {
  group('P0 public read APIs', () => {
    getNearbyTrashBins();
    thinkTime(0.5);
    getNearbyToilets();
    thinkTime(0.5);
    getRouteRecommendation();
  });

  group('P0 authenticated read APIs', () => {
    getMyInfo();
    getMyPloggingStats();
    listPloggingSessions();
    getMonthlyStats();
    getWeeklyStats();
    getPloggingSessionDetail();
  });

  group('P1 presigned URL smoke only', () => {
    getUploadUrlsSmoke();
  });

  group('P0 write API opt-in', () => {
    completePloggingWrite();
  });

  thinkTime();
}

export function handleSummary(data) {
  return buildSummary(data, 'smoke');
}
