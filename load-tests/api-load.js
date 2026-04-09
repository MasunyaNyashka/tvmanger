import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const authBaseUrl = __ENV.AUTH_BASE_URL || 'http://localhost:18081';
const tariffBaseUrl = __ENV.TARIFF_BASE_URL || 'http://localhost:18082';
const orderBaseUrl = __ENV.ORDER_BASE_URL || 'http://localhost:18083';
const serviceRequestBaseUrl = __ENV.SERVICE_REQUEST_BASE_URL || 'http://localhost:18084';

const businessErrors = new Counter('business_errors');
const successfulFlows = new Rate('successful_flows');

export const options = {
  scenarios: {
    api_smoke_load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
    successful_flows: ['rate>0.80'],
  },
};

export default function () {
  const suffix = `${__VU}_${__ITER}_${Date.now()}`;
  const username = `load_${suffix}`;
  const password = 'secret123';

  const registerResponse = http.post(
    `${authBaseUrl}/auth/register`,
    JSON.stringify({
      username,
      password,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'auth_register' },
    }
  );

  const registerOk = check(registerResponse, {
    'register status is 200': (r) => r.status === 200,
    'register returns token': (r) => hasJsonField(r, 'token'),
  });

  if (!registerOk) {
    businessErrors.add(1);
    successfulFlows.add(false);
    return;
  }

  const loginResponse = http.post(
    `${authBaseUrl}/auth/login`,
    JSON.stringify({
      username,
      password,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'auth_login' },
    }
  );

  const loginOk = check(loginResponse, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => hasJsonField(r, 'token'),
  });

  if (!loginOk) {
    businessErrors.add(1);
    successfulFlows.add(false);
    return;
  }

  const token = loginResponse.json('token');

  const tariffsResponse = http.get(`${tariffBaseUrl}/tariffs`, {
    tags: { endpoint: 'tariffs_list' },
  });

  const tariffsOk = check(tariffsResponse, {
    'tariffs status is 200': (r) => r.status === 200,
    'tariffs returns array': (r) => Array.isArray(safeJson(r)),
  });

  if (!tariffsOk) {
    businessErrors.add(1);
    successfulFlows.add(false);
    return;
  }

  const tariffs = safeJson(tariffsResponse);
  const tariffId =
    Array.isArray(tariffs) && tariffs.length > 0
      ? tariffs[0].id
      : '00000000-0000-0000-0000-000000000000';

  const orderResponse = http.post(
    `${orderBaseUrl}/orders`,
    JSON.stringify({
      tariffId,
      fullName: 'Ivan Ivanov',
      address: 'Moscow, Lenina 1',
      phone: '+79991234567',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      tags: { endpoint: 'orders_create' },
    }
  );

  const orderOk = check(orderResponse, {
    'order status is 200': (r) => r.status === 200,
    'order created in submitted status': (r) => jsonFieldEquals(r, 'status', 'SUBMITTED'),
  });

  if (!orderOk) {
    businessErrors.add(1);
    successfulFlows.add(false);
    return;
  }

  const serviceRequestResponse = http.post(
    `${serviceRequestBaseUrl}/service-requests`,
    JSON.stringify({
      type: 'REPAIR',
      tariffId,
      address: 'Moscow, Arbat 10',
      phone: '+79991234567',
      details: 'No signal',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      tags: { endpoint: 'service_requests_create' },
    }
  );

  const serviceRequestOk = check(serviceRequestResponse, {
    'service request status is 200': (r) => r.status === 200,
    'service request created in submitted status': (r) => jsonFieldEquals(r, 'status', 'SUBMITTED'),
  });

  if (!serviceRequestOk) {
    businessErrors.add(1);
    successfulFlows.add(false);
    return;
  }

  successfulFlows.add(true);
  sleep(1);
}

function safeJson(response) {
  try {
    return response.json();
  } catch (e) {
    return null;
  }
}

function hasJsonField(response, field) {
  const data = safeJson(response);
  return data !== null && data[field] !== undefined && data[field] !== null && data[field] !== '';
}

function jsonFieldEquals(response, field, expected) {
  const data = safeJson(response);
  return data !== null && data[field] === expected;
}
