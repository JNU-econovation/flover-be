import { buildSummary } from './summary.js';
import {
  completePloggingWrite,
  config,
  getMonthlyStats,
  getMyInfo,
  getMyPloggingStats,
  getNearbyToilets,
  getNearbyTrashBins,
  getRouteRecommendation,
  getWeeklyStats,
  listPloggingSessions,
  thinkTime,
} from './common.js';

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  stages: [
    { duration: '2m', target: 5 },
    { duration: '3m', target: 10 },
    { duration: '3m', target: 20 },
    { duration: '3m', target: 30 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    checks: ['rate>0.95'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{type:read}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{type:route}': ['p(95)<5000', 'p(99)<10000'],
    'http_req_duration{type:write}': ['p(95)<2500'],
  },
};

export default function () {
  const roll = Math.random();

  if (roll < 0.35) {
    getNearbyTrashBins();
    thinkTime(0.2);
    getNearbyToilets();
  } else if (roll < 0.55) {
    getMyInfo();
    getMyPloggingStats();
  } else if (roll < 0.75) {
    listPloggingSessions();
    getMonthlyStats();
    getWeeklyStats();
  } else if (roll < 0.92) {
    getRouteRecommendation();
  } else if (config.includeWrite) {
    completePloggingWrite();
  } else {
    getNearbyTrashBins();
  }

  thinkTime();
}

export function handleSummary(data) {
  return buildSummary(data, 'load');
}
