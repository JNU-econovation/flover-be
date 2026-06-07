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
    { duration: '2m', target: 10 },
    { duration: '3m', target: 20 },
    { duration: '3m', target: 30 },
    { duration: '3m', target: 50 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    checks: ['rate>0.90'],
    http_req_failed: ['rate<0.05'],
    'http_req_duration{type:read}': ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{type:route}': ['p(95)<10000', 'p(99)<15000'],
    'http_req_duration{type:write}': ['p(95)<4000'],
  },
};

export default function () {
  const roll = Math.random();

  if (roll < 0.42) {
    getNearbyTrashBins();
  } else if (roll < 0.55) {
    getNearbyToilets();
  } else if (roll < 0.70) {
    getMyInfo();
    getMyPloggingStats();
  } else if (roll < 0.85) {
    listPloggingSessions();
    getMonthlyStats();
    getWeeklyStats();
  } else if (roll < 0.98) {
    getRouteRecommendation();
  } else if (config.includeWrite) {
    completePloggingWrite();
  } else {
    getNearbyTrashBins();
  }

  thinkTime(0.5);
}

export function handleSummary(data) {
  return buildSummary(data, 'stress');
}
