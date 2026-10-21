import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { Buffer } from 'node:buffer';
import axios from 'axios';
import { clearSession, getSession, saveSession } from '../src/services/session.js';

let sequence = 0;
async function setup(adapter) {
  const values = new Map();
  const redirects = [];
  const alerts = [];
  globalThis.localStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key),
  };
  globalThis.window = { location: { replace: (url) => redirects.push(url) }, alert: (message) => alerts.push(message) };
  saveSession({ accountId: 1, accountRole: 2, accessToken: 'old-access', refreshToken: 'refresh', expiresIn: 0 });
  let source = await readFile(new URL('../src/services/api.js', import.meta.url), 'utf8');
  source = source.replace("from 'axios'", `from '${import.meta.resolve('axios')}'`)
    .replace("from './session'", `from '${new URL('../src/services/session.js', import.meta.url).href}'`)
    .replace('import.meta.env.VITE_API_BASE_URL', 'undefined');
  const previous = axios.defaults.adapter;
  axios.defaults.adapter = adapter;
  try {
    const { default: api } = await import(`data:text/javascript;base64,${Buffer.from(source + `\n// instance ${sequence++}`).toString('base64')}`);
    return { api, values, redirects, alerts };
  } finally {
    axios.defaults.adapter = previous;
  }
}

function response(config, data = {}, status = 200) {
  return { config, data, status, headers: {}, statusText: String(status) };
}

function reject(config, status) {
  throw new axios.AxiosError('Rejected', 'ERR_BAD_REQUEST', config, null, response(config, { message: 'Denied' }, status));
}

const refreshed = { accessToken: 'new-access', refreshToken: 'refresh', expiresIn: 1800 };

test('expired access retains the session while a refresh token is available', async () => {
  await setup(async (config) => response(config));
  assert.equal(getSession().token, 'old-access');
  assert.equal(getSession().refreshToken, 'refresh');
});

test('concurrent 401s share one refresh and both retry using the new token', async () => {
  let refreshes = 0;
  const { api, redirects } = await setup(async (config) => {
    if (config.url === '/auth/refresh') {
      refreshes++;
      assert.equal(config.headers.Authorization, undefined);
      assert.equal(JSON.parse(config.data).refreshToken, 'refresh');
      await new Promise((resolve) => setTimeout(resolve, 10));
      return response(config, refreshed);
    }
    if (config.headers.Authorization === 'Bearer old-access') reject(config, 401);
    assert.equal(config.headers.Authorization, 'Bearer new-access');
    return response(config);
  });
  await Promise.all([api.get('/news'), api.get('/tags')]);
  assert.equal(refreshes, 1);
  assert.equal(getSession().user.accountId, 1);
  assert.deepEqual(redirects, []);
});

test('403 displays permission error and keeps the session without refreshing', async () => {
  let calls = 0;
  const { api, redirects, alerts } = await setup(async (config) => { calls++; reject(config, 403); });
  await assert.rejects(api.get('/users'));
  assert.equal(calls, 1);
  assert.equal(getSession().refreshToken, 'refresh');
  assert.deepEqual(redirects, []);
  assert.deepEqual(alerts, ['Denied']);
});

test('invalid refresh clears only authentication keys and redirects to login', async () => {
  const { api, redirects, values } = await setup(async (config) => reject(config, 401));
  values.set('theme', 'dark');
  await assert.rejects(api.get('/news'));
  assert.equal(getSession(), null);
  assert.equal(values.get('theme'), 'dark');
  assert.deepEqual(redirects, ['/login']);
});

test('network failure during refresh preserves the session for a later retry', async () => {
  const { api, redirects } = await setup(async (config) => {
    if (config.url === '/auth/refresh') throw new axios.AxiosError('Offline', 'ERR_NETWORK', config);
    reject(config, 401);
  });
  await assert.rejects(api.get('/news'));
  assert.equal(getSession().refreshToken, 'refresh');
  assert.deepEqual(redirects, []);
});

test('retried 401 stops after one refresh', async () => {
  let refreshes = 0;
  const { api, redirects } = await setup(async (config) => {
    if (config.url === '/auth/refresh') { refreshes++; return response(config, refreshed); }
    reject(config, 401);
  });
  await assert.rejects(api.get('/news'));
  assert.equal(refreshes, 1);
  assert.deepEqual(redirects, ['/login']);
});

test('login failure never starts refresh or clears an existing session', async () => {
  let calls = 0;
  const { api, redirects } = await setup(async (config) => { calls++; reject(config, 401); });
  await assert.rejects(api.post('/auth/login', {}));
  assert.equal(calls, 1);
  assert.equal(getSession().token, 'old-access');
  assert.deepEqual(redirects, []);
});

test('a refresh response cannot restore a session after logout', async () => {
  const { api, redirects } = await setup(async (config) => {
    if (config.url === '/auth/refresh') { clearSession(); return response(config, refreshed); }
    reject(config, 401);
  });
  await assert.rejects(api.get('/news'));
  assert.equal(getSession(), null);
  assert.deepEqual(redirects, []);
});

test('an old refresh failure cannot clear a newly logged-in session', async () => {
  const { api, redirects } = await setup(async (config) => {
    if (config.url === '/auth/refresh') {
      saveSession({ accountId: 2, accessToken: 'another-access', refreshToken: 'another-refresh', expiresIn: 1800 });
    }
    reject(config, 401);
  });
  await assert.rejects(api.get('/news'));
  assert.equal(getSession().user.accountId, 2);
  assert.deepEqual(redirects, []);
});
