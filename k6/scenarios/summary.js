function valueOf(data, metricName, valueName) {
  const metric = data.metrics[metricName];
  if (!metric || !metric.values) {
    return null;
  }
  return metric.values[valueName] ?? null;
}

function formatNumber(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 'n/a';
  }
  return Number(value).toFixed(digits);
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 'n/a';
  }
  return `${(Number(value) * 100).toFixed(2)}%`;
}

function pad2(value) {
  return String(value).padStart(2, '0');
}

function formatTimestamp(date) {
  const datePart = [
    date.getFullYear(),
    pad2(date.getMonth() + 1),
    pad2(date.getDate()),
  ].join('');
  const timePart = [
    pad2(date.getHours()),
    pad2(date.getMinutes()),
    pad2(date.getSeconds()),
  ].join('');
  return `${datePart}-${timePart}`;
}

function formatGeneratedAt(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())} local time`;
}

function thresholdRows(data) {
  return Object.entries(data.metrics)
    .filter(([, metric]) => metric.thresholds)
    .flatMap(([name, metric]) => Object.entries(metric.thresholds)
      .map(([threshold, result]) => `
        <tr>
          <td>${name}</td>
          <td>${threshold}</td>
          <td class="${result.ok ? 'pass' : 'fail'}">${result.ok ? 'PASS' : 'FAIL'}</td>
        </tr>
      `))
    .join('');
}

function metricRow(data, label, metricName) {
  return `
    <tr>
      <td>${label}</td>
      <td>${formatNumber(valueOf(data, metricName, 'avg'))} ms</td>
      <td>${formatNumber(valueOf(data, metricName, 'p(95)'))} ms</td>
      <td>${formatNumber(valueOf(data, metricName, 'p(99)'))} ms</td>
      <td>${formatNumber(valueOf(data, metricName, 'max'))} ms</td>
    </tr>
  `;
}

function renderHtml(data, testName, generatedAt) {
  const failedRate = valueOf(data, 'http_req_failed', 'rate');
  const requestRate = valueOf(data, 'http_reqs', 'rate');
  const requestCount = valueOf(data, 'http_reqs', 'count');
  const p95 = valueOf(data, 'http_req_duration', 'p(95)');
  const p99 = valueOf(data, 'http_req_duration', 'p(99)');

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>k6 ${testName} summary - ${generatedAt}</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 32px; color: #1f2937; }
    h1 { margin-bottom: 8px; }
    .generated-at { color: #6b7280; margin-top: 0; }
    table { border-collapse: collapse; width: 100%; margin-top: 16px; }
    th, td { border: 1px solid #d1d5db; padding: 8px 10px; text-align: left; }
    th { background: #f3f4f6; }
    .metric-grid { display: grid; grid-template-columns: repeat(5, minmax(120px, 1fr)); gap: 12px; margin-top: 20px; }
    .metric { border: 1px solid #d1d5db; border-radius: 6px; padding: 12px; }
    .label { color: #6b7280; font-size: 12px; }
    .value { font-size: 22px; margin-top: 6px; }
    .pass { color: #047857; font-weight: 700; }
    .fail { color: #b91c1c; font-weight: 700; }
  </style>
</head>
<body>
  <h1>k6 ${testName} summary - ${generatedAt}</h1>
  <p class="generated-at">Generated at: ${generatedAt}</p>
  <p>발표용 핵심 지표: 실패율, p95/p99 응답 시간, 처리량(TPS/RPS), threshold 통과 여부</p>
  <div class="metric-grid">
    <div class="metric"><div class="label">http_req_failed</div><div class="value">${formatPercent(failedRate)}</div></div>
    <div class="metric"><div class="label">http_req_duration p95</div><div class="value">${formatNumber(p95)} ms</div></div>
    <div class="metric"><div class="label">http_req_duration p99</div><div class="value">${formatNumber(p99)} ms</div></div>
    <div class="metric"><div class="label">TPS/RPS</div><div class="value">${formatNumber(requestRate)} req/s</div></div>
    <div class="metric"><div class="label">Total requests</div><div class="value">${formatNumber(requestCount, 0)}</div></div>
  </div>
  <h2>Latency by Type</h2>
  <table>
    <thead><tr><th>Type</th><th>Avg</th><th>p95</th><th>p99</th><th>Max</th></tr></thead>
    <tbody>
      ${metricRow(data, 'overall', 'http_req_duration')}
      ${metricRow(data, 'read', 'http_req_duration{type:read}')}
      ${metricRow(data, 'route', 'http_req_duration{type:route}')}
      ${metricRow(data, 'write', 'http_req_duration{type:write}')}
    </tbody>
  </table>
  <h2>Thresholds</h2>
  <table>
    <thead><tr><th>Metric</th><th>Threshold</th><th>Result</th></tr></thead>
    <tbody>${thresholdRows(data)}</tbody>
  </table>
</body>
</html>`;
}

function renderStdout(data, testName, timestamp) {
  const failedRate = valueOf(data, 'http_req_failed', 'rate');
  const requestRate = valueOf(data, 'http_reqs', 'rate');
  const p95 = valueOf(data, 'http_req_duration', 'p(95)');
  const p99 = valueOf(data, 'http_req_duration', 'p(99)');

  return [
    '',
    `k6 ${testName} summary`,
    `- http_req_failed: ${formatPercent(failedRate)}`,
    `- http_req_duration p95: ${formatNumber(p95)} ms`,
    `- http_req_duration p99: ${formatNumber(p99)} ms`,
    `- route p95/p99: ${formatNumber(valueOf(data, 'http_req_duration{type:route}', 'p(95)'))} / ${formatNumber(valueOf(data, 'http_req_duration{type:route}', 'p(99)'))} ms`,
    `- TPS/RPS(http_reqs rate): ${formatNumber(requestRate)} req/s`,
    `- JSON/HTML saved under k6/results/${testName}-summary-${timestamp}.*`,
    '',
  ].join('\n');
}

export function buildSummary(data, testName) {
  const generatedDate = new Date();
  const timestamp = formatTimestamp(generatedDate);
  const generatedAt = formatGeneratedAt(generatedDate);
  const basePath = `k6/results/${testName}-summary-${timestamp}`;

  return {
    [`${basePath}.json`]: JSON.stringify(data, null, 2),
    [`${basePath}.html`]: renderHtml(data, testName, generatedAt),
    stdout: renderStdout(data, testName, timestamp),
  };
}
