import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

const CARD_CLASS = 'rounded-xl border border-slate-200 bg-white p-5 shadow-sm';
const INPUT_CLASS = 'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500';
const PRIMARY_BUTTON_CLASS = 'rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50';
const SECONDARY_BUTTON_CLASS = 'rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50';

function indexSettings(settings = []) {
  return Object.fromEntries(settings.map((setting) => [setting.name, setting]));
}

class IxuiNetwork extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    settings: { type: Object },
    liveStatus: { type: Array },
    reachability: { type: Object },
    loading: { type: Boolean },
    saving: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.settings = {};
    this.liveStatus = [];
    this.reachability = {};
    this.loading = true;
    this.saving = false;
    this.notification = null;
    this._interval = null;
    this._notificationTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadPage();
    this._interval = setInterval(() => this._loadStatus(), 15000);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._interval) {
      clearInterval(this._interval);
    }
    if (this._notificationTimer) {
      clearTimeout(this._notificationTimer);
    }
  }

  async _loadPage() {
    this.loading = true;
    try {
      const [settings] = await Promise.all([api.getNetworkSettings(), this._loadStatus()]);
      this.settings = indexSettings(settings || []);
    } catch {
      this.settings = {};
      this._showNotification('Failed to load network settings.', 'error');
    } finally {
      this.loading = false;
    }
  }

  async _loadStatus() {
    try {
      const [liveStatus, reachability] = await Promise.all([
        api.getNetworkStatus(),
        api.getNetworkStatus2(),
      ]);
      this.liveStatus = liveStatus || [];
      this.reachability = reachability || {};
    } catch {
      this.liveStatus = [];
      this.reachability = {};
    }
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    if (this._notificationTimer) {
      clearTimeout(this._notificationTimer);
    }
    this._notificationTimer = setTimeout(() => {
      this.notification = null;
    }, 4000);
  }

  _settingValue(name, fallback = '') {
    return this.settings[name]?.value ?? fallback;
  }

  _updateSetting(name, value) {
    const current = this.settings[name] || { id: 0, name, value: '' };
    this.settings = {
      ...this.settings,
      [name]: { ...current, value: value ?? '' },
    };
  }

  _deviceRows() {
    const rows = [];
    let index = 0;
    while (true) {
      const onbootKey = `nw_eth${index}_onboot`;
      if (!(onbootKey in this.settings)) {
        break;
      }
      rows.push({
        index,
        interface: `eth${index}`,
        bootproto: this._settingValue(`nw_eth${index}_bootproto`, 'static'),
        onboot: this._settingValue(onbootKey, 'yes'),
        ipaddr: this._settingValue(`nw_eth${index}_ipaddr`, ''),
        netmask: this._settingValue(`nw_eth${index}_netmask`, ''),
        mac: this._settingValue(`nw_eth${index}_mac`, ''),
      });
      index += 1;
    }
    return rows;
  }

  _statusBadge(status) {
    const value = String(status || 'unknown').toLowerCase();
    if (value === 'online' || value === 'up' || value === 'active') {
      return 'bg-green-100 text-green-800';
    }
    if (value === 'offline' || value === 'down' || value === 'inactive') {
      return 'bg-red-100 text-red-800';
    }
    return 'bg-slate-100 text-slate-700';
  }

  _deviceUsesStaticConfig(row) {
    return String(row.bootproto || 'static').toLowerCase() === 'static'
      && String(row.onboot || 'yes').toLowerCase() === 'yes';
  }

  _deviceInputClass(disabled = false) {
    return `${INPUT_CLASS} ${disabled ? 'cursor-not-allowed bg-slate-100 text-slate-400' : ''}`;
  }

  async _saveNetworkSettings() {
    if (!window.confirm('Save and apply the current network settings now?')) {
      return;
    }

    this.saving = true;
    try {
      const payload = Object.values(this.settings).filter((setting) => setting.name.startsWith('nw_'));
      const response = await api.updateNetworkSettings(payload, true);
      if (!response?.success) {
        this._showNotification(response?.error || 'Failed to save network settings.', 'error');
        return;
      }
      this._showNotification('Network settings saved and applied.', 'success');
      await this._loadStatus();
    } catch {
      this._showNotification('Failed to save network settings.', 'error');
    } finally {
      this.saving = false;
    }
  }

  _renderNotification() {
    if (!this.notification) {
      return '';
    }
    const tone = this.notification.type === 'success'
      ? 'border-green-200 bg-green-50 text-green-800'
      : 'border-red-200 bg-red-50 text-red-800';
    return html`
      <div class=${`rounded-lg border px-4 py-3 text-sm font-medium ${tone}`}>
        ${this.notification.message}
      </div>
    `;
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent"></div>
        </div>
      `;
    }

    const rows = this._deviceRows();
    const reachabilityEntries = Object.entries(this.reachability || {});

    return html`
      <div class="space-y-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Network</h1>
            <p class="text-sm text-slate-500">Editable common and device network settings with live status and reachability.</p>
          </div>
          <div class="flex gap-3">
            <button class=${SECONDARY_BUTTON_CLASS} @click=${this._loadStatus}>Refresh Status</button>
            <button class=${PRIMARY_BUTTON_CLASS} @click=${this._saveNetworkSettings} ?disabled=${this.saving}>
              ${this.saving ? 'Saving...' : 'Save and Apply'}
            </button>
          </div>
        </div>

        ${this._renderNotification()}

        <div class="grid gap-6 xl:grid-cols-3">
          <section class=${CARD_CLASS}>
            <div class="mb-4">
              <h2 class="text-lg font-semibold text-slate-800">Common Settings</h2>
              <p class="text-sm text-slate-500">Hostname, gateway, DNS, and multicast route.</p>
            </div>
            <div class="space-y-4">
              <label class="block space-y-1">
                <span class="text-sm font-medium text-slate-700">Hostname</span>
                <input type="text" class=${INPUT_CLASS} .value=${this._settingValue('nw_hostname')} @input=${(event) => this._updateSetting('nw_hostname', event.target.value)} />
              </label>
              <label class="block space-y-1">
                <span class="text-sm font-medium text-slate-700">Default Gateway</span>
                <input type="text" class=${INPUT_CLASS} .value=${this._settingValue('nw_gateway')} @input=${(event) => this._updateSetting('nw_gateway', event.target.value)} />
              </label>
              <label class="block space-y-1">
                <span class="text-sm font-medium text-slate-700">Multicast Route</span>
                <input type="text" class=${INPUT_CLASS} .value=${this._settingValue('nw_multicastdev')} @input=${(event) => this._updateSetting('nw_multicastdev', event.target.value)} />
              </label>
              <label class="block space-y-1">
                <span class="text-sm font-medium text-slate-700">DNS 1</span>
                <input type="text" class=${INPUT_CLASS} .value=${this._settingValue('nw_dns1')} @input=${(event) => this._updateSetting('nw_dns1', event.target.value)} />
              </label>
              <label class="block space-y-1">
                <span class="text-sm font-medium text-slate-700">DNS 2</span>
                <input type="text" class=${INPUT_CLASS} .value=${this._settingValue('nw_dns2')} @input=${(event) => this._updateSetting('nw_dns2', event.target.value)} />
              </label>
            </div>
          </section>

          <section class=${CARD_CLASS}>
            <div class="mb-4 flex items-center justify-between gap-4">
              <div>
                <h2 class="text-lg font-semibold text-slate-800">Interface Status</h2>
                <p class="text-sm text-slate-500">Auto-refreshes every 15 seconds.</p>
              </div>
            </div>
            ${this.liveStatus.length === 0 ? html`
              <div class="rounded-lg border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">
                No live interface status available.
              </div>
            ` : html`
              <div class="space-y-3">
                ${this.liveStatus.map((entry, index) => html`
                  <div class="rounded-lg border border-slate-200 px-4 py-3">
                    <div class="flex items-center justify-between gap-3">
                      <div class="text-sm font-semibold text-slate-800">eth${index}</div>
                      <span class=${`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${this._statusBadge(entry.status)}`}>
                        ${entry.status || 'unknown'}
                      </span>
                    </div>
                    <div class="mt-1 text-sm text-slate-600">IP: <span class="font-mono">${entry.ip || '-'}</span></div>
                    <div class="text-sm text-slate-600">MAC: <span class="font-mono">${entry.mac || '-'}</span></div>
                  </div>
                `)}
              </div>
            `}
          </section>

          <section class=${CARD_CLASS}>
            <div class="mb-4">
              <h2 class="text-lg font-semibold text-slate-800">IP Status</h2>
              <p class="text-sm text-slate-500">Gateway, DNS, and public reachability summary.</p>
            </div>
            ${reachabilityEntries.length === 0 ? html`
              <div class="rounded-lg border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">
                No reachability data available.
              </div>
            ` : html`
              <div class="space-y-3">
                ${reachabilityEntries.map(([name, status]) => html`
                  <div class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3">
                    <div>
                      <div class="text-sm font-semibold uppercase tracking-wide text-slate-700">${name}</div>
                      <div class="font-mono text-sm text-slate-600">${status?.ip || '-'}</div>
                    </div>
                    <span class=${`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${this._statusBadge(status?.status)}`}>
                      ${status?.status || 'unknown'}
                    </span>
                  </div>
                `)}
              </div>
            `}
          </section>
        </div>

        <section class=${CARD_CLASS}>
          <div class="mb-4">
            <h2 class="text-lg font-semibold text-slate-800">Device Settings</h2>
            <p class="text-sm text-slate-500">Per-interface boot protocol, onboot behavior, IP address, and netmask.</p>
          </div>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-slate-200">
              <thead>
                <tr class="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                  <th class="px-4 py-3">Interface</th>
                  <th class="px-4 py-3">Protocol</th>
                  <th class="px-4 py-3">Onboot</th>
                  <th class="px-4 py-3">IP</th>
                  <th class="px-4 py-3">Netmask</th>
                  <th class="px-4 py-3">MAC</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                ${rows.map((row) => html`
                  <tr>
                    <td class="px-4 py-3 text-sm font-medium text-slate-800">${row.interface}</td>
                    <td class="px-4 py-3">
                      <select class=${INPUT_CLASS} .value=${row.bootproto} @change=${(event) => this._updateSetting(`nw_eth${row.index}_bootproto`, event.target.value)}>
                        <option value="static">static</option>
                        <option value="dhcp">dhcp</option>
                      </select>
                    </td>
                    <td class="px-4 py-3">
                      <select class=${INPUT_CLASS} .value=${row.onboot} @change=${(event) => this._updateSetting(`nw_eth${row.index}_onboot`, event.target.value)}>
                        <option value="yes">yes</option>
                        <option value="no">no</option>
                      </select>
                    </td>
                    <td class="px-4 py-3">
                      <input
                        type="text"
                        class=${this._deviceInputClass(!this._deviceUsesStaticConfig(row))}
                        .value=${row.ipaddr}
                        ?disabled=${!this._deviceUsesStaticConfig(row)}
                        @input=${(event) => this._updateSetting(`nw_eth${row.index}_ipaddr`, event.target.value)}
                      />
                    </td>
                    <td class="px-4 py-3">
                      <input
                        type="text"
                        class=${this._deviceInputClass(!this._deviceUsesStaticConfig(row))}
                        .value=${row.netmask}
                        ?disabled=${!this._deviceUsesStaticConfig(row)}
                        @input=${(event) => this._updateSetting(`nw_eth${row.index}_netmask`, event.target.value)}
                      />
                    </td>
                    <td class="px-4 py-3 text-sm font-mono text-slate-600">${row.mac || '-'}</td>
                  </tr>
                `)}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    `;
  }
}

customElements.define('ixui-network', IxuiNetwork);
