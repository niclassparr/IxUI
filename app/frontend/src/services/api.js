const API_BASE = '/api';
const SESSION_KEY = 'ixui_session_key';

function getSessionKey() {
  return localStorage.getItem(SESSION_KEY);
}

function isLoggedIn() {
  return !!getSessionKey();
}

async function request(path, options = {}) {
  const url = `${API_BASE}${path}`;
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (res.status === 204) return null;
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed: ${res.status}`);
  }
  return res.json();
}

async function login(username, password) {
  const data = await request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  if (data.success && data.session_key) {
    localStorage.setItem(SESSION_KEY, data.session_key);
  }
  return data;
}

async function logout() {
  const key = getSessionKey();
  if (key) {
    try {
      await request(`/auth/logout?session_key=${encodeURIComponent(key)}`, { method: 'POST' });
    } catch (_) { /* ignore */ }
  }
  localStorage.removeItem(SESSION_KEY);
}

async function validateSession() {
  const key = getSessionKey();
  if (!key) return false;
  try {
    const data = await request(`/auth/validate?session_key=${encodeURIComponent(key)}`);
    return data?.valid === true;
  } catch {
    return false;
  }
}

// Interfaces
function getInterfaces() {
  return request('/interfaces/');
}

function getInterface(pos) {
  return request(`/interfaces/${pos}`);
}

function getInterfaceConfig(pos, type) {
  return request(`/interfaces/${pos}/config/${type}`);
}

function setInterfaceConfig(pos, type, config) {
  return request(`/interfaces/${pos}/config/${type}`, {
    method: 'PUT',
    body: JSON.stringify(config),
  });
}

function getServices(pos) {
  return request(`/interfaces/${pos}/services`);
}

function saveServices(pos, services) {
  return request(`/interfaces/${pos}/services`, {
    method: 'PUT',
    body: JSON.stringify(services),
  });
}

function startScan(pos) {
  return request(`/interfaces/${pos}/scan`, { method: 'POST' });
}

function getInterfaceStatus(pos) {
  return request(`/interfaces/${pos}/status`);
}

function getStreamerStatus(pos, type) {
  return request(`/interfaces/${pos}/streamer-status/${type}`);
}

function getTunerStatus(pos, type) {
  return request(`/interfaces/${pos}/tuner-status/${type}`);
}

// Routes
function getRoutes() {
  return request('/routes/');
}

function updateRoutes(routes) {
  return request('/routes/', {
    method: 'PUT',
    body: JSON.stringify(routes),
  });
}

function getBitrates() {
  return request('/routes/bitrates');
}

// Settings
function getSettings() {
  return request('/settings/');
}

function updateSettings(settings) {
  return request('/settings/', {
    method: 'PUT',
    body: JSON.stringify(settings),
  });
}

function getNetworkStatus() {
  return request('/settings/network-status');
}

function getNetworkStatus2() {
  return request('/settings/network-status2');
}

// System
function getUnitInfo() {
  return request('/system/unit-info');
}

function runCommand(command) {
  return request('/system/command', {
    method: 'POST',
    body: JSON.stringify({ command }),
  });
}

function getJsonInfo() {
  return request('/system/json-info');
}

function getFeature(name) {
  return request(`/system/feature/${name}`);
}

export {
  login,
  logout,
  validateSession,
  getSessionKey,
  isLoggedIn,
  getInterfaces,
  getInterface,
  getInterfaceConfig,
  setInterfaceConfig,
  getServices,
  saveServices,
  startScan,
  getInterfaceStatus,
  getStreamerStatus,
  getTunerStatus,
  getRoutes,
  updateRoutes,
  getBitrates,
  getSettings,
  updateSettings,
  getNetworkStatus,
  getNetworkStatus2,
  getUnitInfo,
  runCommand,
  getJsonInfo,
  getFeature,
};
