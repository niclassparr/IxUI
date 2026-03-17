import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiNetwork extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    settings: { type: Array },
    networkStatus: { type: Array },
    statusDetails: { type: Object },
    loading: { type: Boolean },
    saving: { type: Boolean },
    loadingStatusDetails: { type: Boolean },
    showStatusDialog: { type: Boolean },
    confirmSave: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.settings = [];
    this.networkStatus = [];
    this.statusDetails = {};
    this.loading = true;
    this.saving = false;
    this.loadingStatusDetails = false;
    this.showStatusDialog = false;
    this.confirmSave = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
  }

  async _loadData() {
    this.loading = true;
    try {
      const [settingsResult, statusResult] = await Promise.allSettled([
        api.getSettings(),
        api.getNetworkStatus(),
      ]);

      const status = statusResult.status === 'fulfilled' ? (statusResult.value || []) : [];
      const settings = settingsResult.status === 'fulfilled' ? (settingsResult.value || []) : [];
      this.networkStatus = status;
      this.settings = this._normalizeSettings(settings, status);
    } catch {
      this.networkStatus = [];
      this.settings = [];
    }
    this.loading = false;
  }

  _normalizeSettings(settings, status) {
    if (settings.some(setting => String(setting.name || '').startsWith('nw_'))) {
      return settings;
    }

    const byName = new Map(settings.map(setting => [setting.name, setting]));
    const valueOf = (name, fallback = '') => byName.get(name)?.value ?? fallback;
    const eth0 = status[0] || {};
    const eth1 = status[1] || {};

    return [
      { id: byName.get('hostname')?.id ?? 1, name: 'nw_hostname', value: valueOf('hostname', '209990') },
      { id: byName.get('gateway')?.id ?? 2, name: 'nw_gateway', value: valueOf('gateway', '192.168.0.1') },
      { id: 3, name: 'nw_multicastdev', value: 'eth0' },
      { id: byName.get('dns')?.id ?? 4, name: 'nw_dns1', value: valueOf('dns', '8.8.4.4') },
      { id: 5, name: 'nw_dns2', value: '' },
      { id: 6, name: 'nw_eth0_bootproto', value: 'static' },
      { id: 7, name: 'nw_eth0_onboot', value: 'yes' },
      { id: byName.get('ip')?.id ?? 8, name: 'nw_eth0_ipaddr', value: valueOf('ip', eth0.ip || '') },
      { id: byName.get('netmask')?.id ?? 9, name: 'nw_eth0_netmask', value: valueOf('netmask', '255.255.255.0') },
      { id: 10, name: 'nw_eth0_mac', value: eth0.mac || '' },
      { id: 11, name: 'nw_eth1_bootproto', value: 'static' },
      { id: 12, name: 'nw_eth1_onboot', value: 'yes' },
      { id: 13, name: 'nw_eth1_ipaddr', value: eth1.ip || '' },
      { id: 14, name: 'nw_eth1_netmask', value: '255.255.255.0' },
      { id: 15, name: 'nw_eth1_mac', value: eth1.mac || '' },
    ];
  }

  _settingsMap() {
    return new Map(this.settings.map(setting => [setting.name, setting]));
  }

  _getSettingValue(name, fallback = '') {
    return this._settingsMap().get(name)?.value ?? fallback;
  }

  _setSettingValue(name, value) {
    const existingIndex = this.settings.findIndex(setting => setting.name === name);
    const updated = [...this.settings];
    if (existingIndex >= 0) {
      updated[existingIndex] = { ...updated[existingIndex], value };
    } else {
      updated.push({ id: 0, name, value });
    }
    this.settings = updated;
  }

  _networkEntries() {
    if (Array.isArray(this.networkStatus) && this.networkStatus.length > 0) {
      return this.networkStatus.map((entry, index) => ({
        type: `eth${index}`,
        ip: entry?.ip || '',
        mac: entry?.mac || '',
      }));
    }

    return this._deviceRows().map(row => ({
      type: row.type,
      ip: row.ip,
      mac: row.mac,
    }));
  }

  _deviceRows() {
    const rows = [];
    for (let index = 0; index <= 9; index += 1) {
      const type = `eth${index}`;
      const hasRow = this.settings.some(setting => setting.name.startsWith(`nw_${type}_`));
      if (!hasRow) {
        if (rows.length > 0) break;
        continue;
      }

      rows.push({
        index,
        type,
        protocol: this._getSettingValue(`nw_${type}_bootproto`, 'static'),
        onboot: this._getSettingValue(`nw_${type}_onboot`, 'yes'),
        ip: this._getSettingValue(`nw_${type}_ipaddr`, ''),
        netmask: this._getSettingValue(`nw_${type}_netmask`, ''),
        mac: this._getSettingValue(`nw_${type}_mac`, ''),
      });
    }
    return rows;
  }

  _multicastOptions() {
    const options = new Set(this._deviceRows().map(row => row.type));
    if (options.size === 0) {
      options.add('eth0');
    }
    return [...options];
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => {
      if (this.notification?.message === message) {
        this.notification = null;
      }
    }, 4000);
  }

  _goBack() {
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    window.location.hash = '#front-page';
  }

  _statusBadgeClasses(status) {
    const normalized = String(status || '').toLowerCase();
    if (normalized === 'up' || normalized === 'active' || normalized === 'online' || normalized === 'connected') {
      return 'bg-green-100 text-green-800';
    }
    if (normalized === 'down' || normalized === 'inactive' || normalized === 'offline' || normalized === 'disconnected') {
      return 'bg-red-100 text-red-800';
    }
    return 'bg-slate-100 text-slate-700';
  }

  async _openStatusDialog() {
    this.showStatusDialog = true;
    this.loadingStatusDetails = true;
    try {
      this.statusDetails = await api.getNetworkStatus2() || {};
    } catch {
      this.statusDetails = {};
      this._showNotification('Failed to load IP status', 'error');
    }
    this.loadingStatusDetails = false;
  }

  _closeStatusDialog() {
    this.showStatusDialog = false;
  }

  async _save() {
    this.confirmSave = false;
    this.saving = true;
    try {
      await api.updateSettings(this.settings);
      await api.runCommand('wnet');
      this._showNotification('Network settings saved successfully', 'success');
      await this._loadData();
    } catch (err) {
      this._showNotification(`Failed to save network settings: ${err.message}`, 'error');
    }
    this.saving = false;
  }

  _renderCommonSettingRow(label, content) {
    return html`
      <div class="grid grid-cols-[18rem,minmax(0,1fr)] border border-slate-200 bg-white">
        <div class="px-4 py-3 text-lg text-slate-700 text-center border-r border-slate-200">${label}</div>
        <div class="px-2 py-1">${content}</div>
      </div>
    `;
  }

  _renderStatusDialog() {
    if (!this.showStatusDialog) {
      return '';
    }

    const rows = [
      { label: 'Gateway', key: 'gateway' },
      { label: 'DNS1', key: 'dns1' },
      { label: 'DNS2', key: 'dns2' },
      { label: 'Public IP', key: 'public' },
    ];

    return html`
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/45 px-4">
        <div class="w-full max-w-2xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200">
            <h2 class="text-2xl font-semibold text-slate-800">Network Status</h2>
          </div>

          <div class="p-6">
            ${this.loadingStatusDetails ? html`
              <div class="flex items-center justify-center py-10">
                <div class="animate-spin h-8 w-8 border-4 border-sky-500 border-t-transparent rounded-full"></div>
              </div>
            ` : html`
              <div class="overflow-x-auto">
                <table class="w-full text-left">
                  <thead>
                    <tr class="bg-sky-600 text-white">
                      <th class="px-4 py-2 font-medium">Type</th>
                      <th class="px-4 py-2 font-medium">IP</th>
                      <th class="px-4 py-2 font-medium">Status</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-200">
                    ${rows.map(row => {
                      const status = this.statusDetails?.[row.key] || {};
                      return html`
                        <tr>
                          <td class="px-4 py-3 text-slate-800">${row.label}</td>
                          <td class="px-4 py-3 font-mono text-slate-700">${status.ip || '—'}</td>
                          <td class="px-4 py-3">
                            <span class="inline-flex rounded-full px-3 py-1 text-sm font-medium ${this._statusBadgeClasses(status.status)}">
                              ${status.status || 'unknown'}
                            </span>
                          </td>
                        </tr>
                      `;
                    })}
                  </tbody>
                </table>
              </div>
            `}
          </div>

          <div class="px-6 py-4 border-t border-slate-200 bg-slate-50 flex justify-end">
            <button
              @click=${this._closeStatusDialog}
              class="px-5 py-2 text-sm bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
            >Close</button>
          </div>
        </div>
      </div>
    `;
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        </div>
      `;
    }

    const deviceRows = this._deviceRows();
    const networkEntries = this._networkEntries();

    return html`
      <div class="space-y-8">
        <div class="space-y-3">
          <h1 class="text-5xl font-light text-slate-800">🌐 Network</h1>
          <button
            @click=${this._goBack}
            class="inline-flex items-center gap-2 text-2xl text-slate-900 hover:text-sky-700 transition"
          >
            <span aria-hidden="true">‹</span>
            <span>Back</span>
          </button>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <div class="grid gap-10 xl:grid-cols-[1.05fr,0.95fr] xl:items-start">
          <section class="space-y-4">
            <h2 class="text-3xl font-semibold text-slate-800">Common Settings:</h2>
            <div class="space-y-4">
              ${this._renderCommonSettingRow('Hostname', html`
                <input
                  type="text"
                  .value=${this._getSettingValue('nw_hostname')}
                  @input=${(e) => this._setSettingValue('nw_hostname', e.target.value)}
                  class="w-full border-0 px-2 py-2 text-xl text-slate-800 outline-none"
                />
              `)}
              ${this._renderCommonSettingRow('Default Gateway', html`
                <input
                  type="text"
                  .value=${this._getSettingValue('nw_gateway')}
                  @input=${(e) => this._setSettingValue('nw_gateway', e.target.value)}
                  class="w-full border-0 px-2 py-2 text-xl text-slate-800 outline-none"
                />
              `)}
              ${this._renderCommonSettingRow('Multicast Route', html`
                <select
                  .value=${this._getSettingValue('nw_multicastdev', this._multicastOptions()[0] || 'eth0')}
                  @change=${(e) => this._setSettingValue('nw_multicastdev', e.target.value)}
                  class="w-full bg-white px-2 py-2 text-xl text-slate-800 outline-none"
                >
                  ${this._multicastOptions().map(option => html`<option value=${option}>${option}</option>`)}
                </select>
              `)}
              ${this._renderCommonSettingRow('DNS1', html`
                <input
                  type="text"
                  .value=${this._getSettingValue('nw_dns1')}
                  @input=${(e) => this._setSettingValue('nw_dns1', e.target.value)}
                  class="w-full border-0 px-2 py-2 text-xl text-slate-800 outline-none"
                />
              `)}
              ${this._renderCommonSettingRow('DNS2', html`
                <input
                  type="text"
                  .value=${this._getSettingValue('nw_dns2')}
                  @input=${(e) => this._setSettingValue('nw_dns2', e.target.value)}
                  class="w-full border-0 px-2 py-2 text-xl text-slate-800 outline-none"
                />
              `)}
            </div>
          </section>

          <section class="space-y-6">
            <h2 class="text-3xl font-semibold text-slate-800">Network Status:</h2>
            <div class="overflow-hidden">
              <table class="w-full text-left">
                <thead>
                  <tr class="bg-sky-600 text-white">
                    <th class="px-3 py-2 text-xl font-medium">Type</th>
                    <th class="px-3 py-2 text-xl font-medium">Mac</th>
                    <th class="px-3 py-2 text-xl font-medium">IP</th>
                  </tr>
                </thead>
                <tbody>
                  ${networkEntries.map(entry => html`
                    <tr>
                      <td class="px-3 py-2 text-2xl text-slate-900">${entry.type}</td>
                      <td class="px-3 py-2 text-2xl text-slate-900">${entry.mac || '—'}</td>
                      <td class="px-3 py-2 text-2xl text-slate-900">${entry.ip || '—'}</td>
                    </tr>
                  `)}
                </tbody>
              </table>
            </div>
            <button
              @click=${this._openStatusDialog}
              class="px-12 py-6 text-4xl bg-sky-500 text-white hover:bg-sky-600 transition"
            >IP Status</button>
          </section>
        </div>

        <section class="space-y-4">
          <h2 class="text-3xl font-semibold text-slate-800">Device Settings:</h2>
          <div class="overflow-x-auto">
            <table class="w-full text-left border-separate border-spacing-y-2">
              <thead>
                <tr class="bg-sky-600 text-white">
                  <th class="px-2 py-2 text-xl font-medium">Type</th>
                  <th class="px-2 py-2 text-xl font-medium">Protocol</th>
                  <th class="px-2 py-2 text-xl font-medium">Onboot</th>
                  <th class="px-2 py-2 text-xl font-medium">IP</th>
                  <th class="px-2 py-2 text-xl font-medium">Netmask</th>
                  <th class="px-2 py-2 text-xl font-medium">Mac</th>
                </tr>
              </thead>
              <tbody>
                ${deviceRows.map(row => html`
                  <tr>
                    <td class="px-2 py-2 text-2xl text-slate-900 align-top">${row.type}</td>
                    <td class="px-2 py-2 align-top">
                      <select
                        .value=${row.protocol}
                        @change=${(e) => this._setSettingValue(`nw_${row.type}_bootproto`, e.target.value)}
                        class="w-full border border-slate-200 bg-slate-50 px-3 py-2 text-2xl text-slate-900"
                      >
                        <option value="static">static</option>
                        <option value="dhcp">dhcp</option>
                      </select>
                    </td>
                    <td class="px-2 py-2 align-top">
                      <select
                        .value=${row.onboot}
                        @change=${(e) => this._setSettingValue(`nw_${row.type}_onboot`, e.target.value)}
                        class="w-full border border-slate-200 bg-slate-50 px-3 py-2 text-2xl text-slate-900"
                      >
                        <option value="yes">yes</option>
                        <option value="no">no</option>
                      </select>
                    </td>
                    <td class="px-2 py-2 align-top">
                      <input
                        type="text"
                        .value=${row.ip}
                        @input=${(e) => this._setSettingValue(`nw_${row.type}_ipaddr`, e.target.value)}
                        class="w-full border border-slate-200 bg-slate-50 px-3 py-2 text-2xl text-slate-900"
                      />
                    </td>
                    <td class="px-2 py-2 align-top">
                      <input
                        type="text"
                        .value=${row.netmask}
                        @input=${(e) => this._setSettingValue(`nw_${row.type}_netmask`, e.target.value)}
                        class="w-full border border-slate-200 bg-slate-50 px-3 py-2 text-2xl text-slate-900"
                      />
                    </td>
                    <td class="px-2 py-2 text-2xl text-slate-900 align-top">${row.mac || '—'}</td>
                  </tr>
                `)}
              </tbody>
            </table>
          </div>
        </section>

        <div class="space-y-3">
          ${this.confirmSave ? html`
            <div class="max-w-xl rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              <p class="font-medium">Apply these network changes now?</p>
              <p class="mt-1">This reloads the network configuration.</p>
              <div class="mt-3 flex gap-2">
                <button
                  @click=${this._save}
                  class="px-4 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition"
                >Yes, Save</button>
                <button
                  @click=${() => { this.confirmSave = false; }}
                  class="px-4 py-2 bg-white border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition"
                >Cancel</button>
              </div>
            </div>
          ` : ''}

          <button
            @click=${() => { this.confirmSave = true; }}
            class="px-12 py-6 text-4xl bg-sky-500 text-white hover:bg-sky-600 transition disabled:opacity-50"
            ?disabled=${this.saving}
          >${this.saving ? 'Saving...' : 'Save'}</button>
        </div>

        ${this._renderStatusDialog()}
      </div>
    `;
  }
}

customElements.define('ixui-network', IxuiNetwork);
