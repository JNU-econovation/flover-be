import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const locations = new SharedArray('locations', () => JSON.parse(open('../data/locations.json')));

export const config = {
  baseUrl: (__ENV.BASE_URL || '').replace(/\/$/, ''),
  accessToken: __ENV.ACCESS_TOKEN || '',
  includeWrite: (__ENV.INCLUDE_WRITE || 'false').toLowerCase() === 'true',
  testDataPrefix: __ENV.TEST_DATA_PREFIX || 'k6-test',
  testUserEmail: __ENV.TEST_USER_EMAIL || '',
  testUserPassword: __ENV.TEST_USER_PASSWORD || '',
  sleepSeconds: Number(__ENV.SLEEP_SECONDS || '1'),
  statsYear: Number(__ENV.STATS_YEAR || new Date().getFullYear()),
  statsMonth: Number(__ENV.STATS_MONTH || (new Date().getMonth() + 1)),
  weekStartDate: __ENV.WEEK_START_DATE || '2026-06-01',
  ploggingSessionId: __ENV.PLOGGING_SESSION_ID || '',
  routeTimeMinutes: Number(__ENV.ROUTE_TIME_MINUTES || '30'),
  routeMode: __ENV.ROUTE_MODE || 'PLOGGING',
  contentType: __ENV.IMAGE_CONTENT_TYPE || 'image/png',
};

if (!config.baseUrl) {
  throw new Error('BASE_URL is required. Example: $env:BASE_URL = "https://api.example.com"');
}

let missingTokenWarned = false;

function randomLocation() {
  return locations[Math.floor(Math.random() * locations.length)];
}

function randomSuffix() {
  return `${Date.now()}-${__VU}-${__ITER}`;
}

function query(params) {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
}

function url(path, params = {}) {
  const qs = query(params);
  return `${config.baseUrl}${path}${qs ? `?${qs}` : ''}`;
}

function commonHeaders(extra = {}) {
  return {
    ...extra,
  };
}

function authHeaders(extra = {}) {
  return commonHeaders({
    Authorization: `Bearer ${config.accessToken}`,
    ...extra,
  });
}

function requestParams(tags, headers = {}) {
  return {
    headers,
    tags,
  };
}

function parseJson(res) {
  try {
    return res.json();
  } catch (_) {
    return null;
  }
}

function hasAccessToken(apiName) {
  if (config.accessToken) {
    return true;
  }

  if (!missingTokenWarned) {
    console.warn(`ACCESS_TOKEN is missing. Skipping authenticated k6 requests such as ${apiName}.`);
    missingTokenWarned = true;
  }
  return false;
}

export function thinkTime(multiplier = 1) {
  sleep(Math.max(config.sleepSeconds * multiplier, 0.1));
}

export function getNearbyTrashBins() {
  const loc = randomLocation();
  const res = http.get(
    url('/api/facilities/trash-bins', { lat: loc.lat, lng: loc.lng }),
    requestParams({ api: 'facility_trash_bins', priority: 'P0', type: 'read' })
  );
  const body = parseJson(res);

  check(res, {
    'trash bins status is 200': (r) => r.status === 200,
    'trash bins response is array': () => Array.isArray(body),
  });
}

export function getNearbyToilets() {
  const loc = randomLocation();
  const res = http.get(
    url('/api/facilities/toilets', { lat: loc.lat, lng: loc.lng }),
    requestParams({ api: 'facility_toilets', priority: 'P0', type: 'read' })
  );
  const body = parseJson(res);

  check(res, {
    'toilets status is 200': (r) => r.status === 200,
    'toilets response is array': () => Array.isArray(body),
  });
}

export function getRouteRecommendation() {
  const loc = randomLocation();
  const res = http.get(
    url('/api/v1/routes', {
      lat: loc.lat,
      lon: loc.lng,
      time: config.routeTimeMinutes,
      mode: config.routeMode,
    }),
    requestParams({ api: 'route_recommendation', priority: 'P0', type: 'route' })
  );
  const body = parseJson(res);

  check(res, {
    'route status is 200': (r) => r.status === 200,
    'route response has routes array': () => body !== null && Array.isArray(body.routes),
  });
}

export function getMyInfo() {
  if (!hasAccessToken('getMyInfo')) {
    return;
  }

  const res = http.get(
    url('/api/users/me'),
    requestParams({ api: 'user_me', priority: 'P0', type: 'read' }, authHeaders())
  );
  const body = parseJson(res);

  check(res, {
    'my info status is 200': (r) => r.status === 200,
    'my info has nickname': () => body !== null && Object.prototype.hasOwnProperty.call(body, 'nickname'),
  });
}

export function getMyPloggingStats() {
  if (!hasAccessToken('getMyPloggingStats')) {
    return;
  }

  const res = http.get(
    url('/api/users/me/plogging-stats'),
    requestParams({ api: 'user_plogging_stats', priority: 'P0', type: 'read' }, authHeaders())
  );
  const body = parseJson(res);

  check(res, {
    'my plogging stats status is 200': (r) => r.status === 200,
    'my plogging stats has count': () => body !== null && Object.prototype.hasOwnProperty.call(body, 'totalPloggingCount'),
  });
}

export function listPloggingSessions() {
  if (!hasAccessToken('listPloggingSessions')) {
    return;
  }

  const res = http.get(
    url('/api/plogging-sessions', { page: 0, size: 20 }),
    requestParams({ api: 'plogging_session_list', priority: 'P0', type: 'read' }, authHeaders())
  );
  const body = parseJson(res);

  check(res, {
    'session list status is 200': (r) => r.status === 200,
    'session list has content': () => body !== null && Array.isArray(body.content),
  });
}

export function getMonthlyStats() {
  if (!hasAccessToken('getMonthlyStats')) {
    return;
  }

  const res = http.get(
    url('/api/plogging-sessions/monthly', {
      year: config.statsYear,
      month: config.statsMonth,
    }),
    requestParams({ api: 'plogging_monthly_stats', priority: 'P0', type: 'read' }, authHeaders())
  );
  const body = parseJson(res);

  check(res, {
    'monthly stats status is 200': (r) => r.status === 200,
    'monthly stats has total count': () => body !== null && Object.prototype.hasOwnProperty.call(body, 'totalPloggingCount'),
  });
}

export function getWeeklyStats() {
  if (!hasAccessToken('getWeeklyStats')) {
    return;
  }

  const res = http.get(
    url('/api/plogging-sessions/weekly', { startDate: config.weekStartDate }),
    requestParams({ api: 'plogging_weekly_stats', priority: 'P0', type: 'read' }, authHeaders())
  );
  const body = parseJson(res);

  check(res, {
    'weekly stats status is 200': (r) => r.status === 200,
    'weekly stats has daily stats': () => body !== null && Array.isArray(body.dailyStats),
  });
}

export function getPloggingSessionDetail() {
  if (!config.ploggingSessionId || !hasAccessToken('getPloggingSessionDetail')) {
    return;
  }

  const res = http.get(
    url(`/api/plogging-sessions/${config.ploggingSessionId}`),
    requestParams({ api: 'plogging_session_detail', priority: 'P1', type: 'read' }, authHeaders())
  );
  const body = parseJson(res);

  check(res, {
    'session detail status is 200': (r) => r.status === 200,
    'session detail has id': () => body !== null && Object.prototype.hasOwnProperty.call(body, 'ploggingSessionId'),
  });
}

export function getUploadUrlsSmoke() {
  if (!hasAccessToken('getUploadUrlsSmoke')) {
    return;
  }

  const endpoints = [
    {
      name: 'profile_image_upload_url',
      path: '/api/users/me/profile-image/upload-url',
      priority: 'P1',
    },
    {
      name: 'map_image_upload_url',
      path: '/api/plogging-sessions/map-image/upload-url',
      priority: 'P1',
    },
    {
      name: 'photo_upload_url',
      path: '/api/plogging-sessions/photo/upload-url',
      priority: 'P1',
    },
  ];

  for (const endpoint of endpoints) {
    const res = http.get(
      url(endpoint.path, { contentType: config.contentType }),
      requestParams({ api: endpoint.name, priority: endpoint.priority, type: 'presigned_url' }, authHeaders())
    );
    const body = parseJson(res);

    check(res, {
      [`${endpoint.name} status is 200`]: (r) => r.status === 200,
      [`${endpoint.name} has upload url`]: () => body !== null && Object.prototype.hasOwnProperty.call(body, 'uploadUrl'),
    });
  }
}

export function completePloggingWrite() {
  if (!config.includeWrite || !hasAccessToken('completePloggingWrite')) {
    return;
  }

  const loc = randomLocation();
  const suffix = randomSuffix();
  const now = new Date();
  const startedAt = new Date(now.getTime() - 10 * 60 * 1000).toISOString().slice(0, 19);
  const finishedAt = now.toISOString().slice(0, 19);

  const payload = {
    mode: 'FREE',
    startedAt,
    finishedAt,
    distanceMeters: 1200,
    stepCount: 1800,
    caloriesBurned: 70,
    ploggingSeconds: 600,
    restSeconds: 0,
    placeName: `${config.testDataPrefix}-place-${suffix}`,
    startLatitude: loc.lat,
    startLongitude: loc.lng,
    endLatitude: loc.lat + 0.002,
    endLongitude: loc.lng + 0.002,
    routePoints: [
      { latitude: loc.lat, longitude: loc.lng },
      { latitude: loc.lat + 0.001, longitude: loc.lng + 0.001 },
      { latitude: loc.lat + 0.002, longitude: loc.lng + 0.002 },
    ],
    mapImageUrl: `https://example.com/${config.testDataPrefix}/map-${suffix}.png`,
    photoUrls: [`https://example.com/${config.testDataPrefix}/photo-${suffix}.jpg`],
  };

  const res = http.post(
    url('/api/plogging-sessions/complete'),
    JSON.stringify(payload),
    requestParams(
      { api: 'plogging_complete_write', priority: 'P0', type: 'write' },
      authHeaders({ 'Content-Type': 'application/json' })
    )
  );
  const body = parseJson(res);

  check(res, {
    'complete plogging status is 200': (r) => r.status === 200,
    'complete plogging has session id': () => body !== null && Object.prototype.hasOwnProperty.call(body, 'ploggingSessionId'),
  });
}
