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
  const headers = { ...(options.headers || {}) };
  const isFormData = options.body instanceof FormData;
  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(url, {
    headers,
    ...options,
  });
  if (res.status === 204) return null;
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed: ${res.status}`);
  }
  return res.json();
}

function buildApiUrl(path, includeSession = false) {
  const url = new URL(`${API_BASE}${path}`, window.location.origin);
  if (includeSession) {
    const key = getSessionKey();
    if (key) {
      url.searchParams.set('session_key', key);
    }
  }
  return url.toString();
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

function getInterfaceTypes() {
  return request('/interfaces/types/all');
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

function getInterfaceInfoch(pos) {
  return request(`/interfaces/${pos}/infoch`);
}

function setInterfaceInfoch(pos, config) {
  return request(`/interfaces/${pos}/infoch`, {
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

function getInterfaceScanTime(pos) {
  return request(`/interfaces/${pos}/scan-time`);
}

function getInterfaceScanResult(pos) {
  return request(`/interfaces/${pos}/scan-result`);
}

function applyInterface(pos, interfaceType) {
  return request(`/interfaces/${pos}/apply`, {
    method: 'POST',
    body: JSON.stringify({ interface_type: interfaceType }),
  });
}

function runInterfaceCommand(pos, command) {
  return request(`/interfaces/${pos}/command`, {
    method: 'POST',
    body: JSON.stringify({ command }),
  });
}

function getCurrentEmmList(pos, isDsc = false) {
  return request(`/interfaces/${pos}/emm?is_dsc=${encodeURIComponent(String(isDsc))}`);
}

function updateInterfaceMultibandType(pos, interfaceType) {
  return request(`/interfaces/${pos}/multiband/${encodeURIComponent(interfaceType)}`, {
    method: 'PUT',
  });
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

function getDateTimeSettings() {
  return request('/settings/datetime');
}

function updateDateTimeSettings(payload) {
  return request('/settings/datetime', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

function getModulators() {
  return request('/settings/modulators');
}

function updateModulators(modulators) {
  return request('/settings/modulators', {
    method: 'PUT',
    body: JSON.stringify(modulators),
  });
}

function changePassword(oldPassword, newPassword) {
  return request('/settings/password', {
    method: 'POST',
    body: JSON.stringify({
      session_key: getSessionKey(),
      old_password: oldPassword,
      new_password: newPassword,
    }),
  });
}

function getNetworkSettings() {
  return request('/settings/network');
}

function updateNetworkSettings(settings, applyChanges = true) {
  return request('/settings/network', {
    method: 'PUT',
    body: JSON.stringify({
      settings,
      apply_changes: applyChanges,
    }),
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

function getConfigStatus() {
  return request(`/system/config-status?session_key=${encodeURIComponent(getSessionKey() || '')}`);
}

function pushConfig() {
  return request(`/system/push-config?session_key=${encodeURIComponent(getSessionKey() || '')}`, {
    method: 'POST',
  });
}

function downloadBackup() {
  window.open(buildApiUrl('/system/backup/download', true), '_self');
}

async function uploadBackup(file) {
  const body = new FormData();
  body.append('file', file);
  return request(`/system/backup/upload?session_key=${encodeURIComponent(getSessionKey() || '')}`, {
    method: 'POST',
    body,
  });
}

function getStagedBackupInfo() {
  return request(`/system/backup/staged-info?session_key=${encodeURIComponent(getSessionKey() || '')}`);
}

function restoreBackup() {
  return request(`/system/backup/restore?session_key=${encodeURIComponent(getSessionKey() || '')}`, {
    method: 'POST',
  });
}

function downloadDocumentPdf() {
  window.open(buildApiUrl('/system/document/pdf', true), '_self');
}

function getJsonInfo() {
  return request('/system/json-info');
}

function getFeature(name) {
  return request(`/system/feature/${name}`);
}

function getInterfaceLog(pos) {
  return request(`/interfaces/${pos}/log`);
}

function getCloudDetails() {
  return request('/system/cloud-details');
}

function getUpdatePackages() {
  return request('/system/update-packages');
}

function checkUpdatePackages() {
  return request('/system/update/check', {
    method: 'POST',
  });
}

function installUpdatePackages(packages) {
  return request('/system/update-packages', {
    method: 'POST',
    body: JSON.stringify(packages),
  });
}

function getUpdateResult() {
  return request('/system/update-result');
}

function getForcedContents() {
  return request('/system/forced-contents');
}

function saveForcedContents(contents) {
  return request('/system/forced-contents', {
    method: 'PUT',
    body: JSON.stringify(contents),
  });
}

function getEnabledForcedContents() {
  return request('/system/forced-contents/enabled');
}

function saveForcedContentOverrideStatus(id, overrideIndex) {
  return request(`/system/forced-contents/${id}/override`, {
    method: 'POST',
    body: JSON.stringify({ override_index: overrideIndex }),
  });
}

function getHlsInterfaces() {
  return request('/interfaces/hls/list');
}

function getHlsWizardState() {
  return request('/interfaces/hls/wizard');
}

function scanHlsWizard() {
  return request('/interfaces/hls/scan', {
    method: 'POST',
  });
}

function saveHlsWizardServices(services) {
  return request('/interfaces/hls/services', {
    method: 'PUT',
    body: JSON.stringify(services),
  });
}

function getMedia() {
  return request('/system/media');
}

export {
  login,
  logout,
  validateSession,
  getSessionKey,
  isLoggedIn,
  getInterfaces,
  getInterfaceTypes,
  getInterface,
  getInterfaceConfig,
  setInterfaceConfig,
  getInterfaceInfoch,
  setInterfaceInfoch,
  getServices,
  saveServices,
  startScan,
  getInterfaceScanTime,
  getInterfaceScanResult,
  applyInterface,
  runInterfaceCommand,
  getCurrentEmmList,
  updateInterfaceMultibandType,
  getInterfaceStatus,
  getStreamerStatus,
  getTunerStatus,
  getRoutes,
  updateRoutes,
  getBitrates,
  getSettings,
  updateSettings,
  getDateTimeSettings,
  updateDateTimeSettings,
  getModulators,
  updateModulators,
  changePassword,
  getNetworkSettings,
  updateNetworkSettings,
  getNetworkStatus,
  getNetworkStatus2,
  getUnitInfo,
  runCommand,
  getConfigStatus,
  pushConfig,
  downloadBackup,
  uploadBackup,
  getStagedBackupInfo,
  restoreBackup,
  downloadDocumentPdf,
  getJsonInfo,
  getFeature,
  getInterfaceLog,
  getCloudDetails,
  getUpdatePackages,
  checkUpdatePackages,
  installUpdatePackages,
  getUpdateResult,
  getForcedContents,
  saveForcedContents,
  getEnabledForcedContents,
  saveForcedContentOverrideStatus,
  getHlsInterfaces,
  getHlsWizardState,
  scanHlsWizard,
  saveHlsWizardServices,
  getMedia,
};
