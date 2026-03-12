import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiFrontPage extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    unitInfo: { type: Object },
    interfaces: { type: Array },
    bitrates: { type: Array },
    features: { type: Object },
    loading: { type: Boolean },
  };

  constructor() {
    super();
    this.unitInfo = null;
    this.interfaces = [];
    this.bitrates = [];
    this.features = {};
    this.loading = true;
    this._interval = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
    this._interval = setInterval(() => this._refreshData(), 10000);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._interval) clearInterval(this._interval);
  }

  async _loadData() {
    this.loading = true;
    await this._refreshData();
    this.loading = false;
  }

  async _refreshData() {
    try {
      const [unitInfo, interfaces, bitrates] = await Promise.all([
        api.getUnitInfo(),
        api.getInterfaces(),
        api.getBitrates(),
      ]);
      this.unitInfo = unitInfo;
      this.interfaces = interfaces || [];
      this.bitrates = bitrates || [];

      // Load feature flags
      const featureNames = ['hls_output', 'cloud', 'portal'];
      const featureResults = await Promise.allSettled(
        featureNames.map(f => api.getFeature(f))
      );
      const features = {};
      featureNames.forEach((name, i) => {
        if (featureResults[i].status === 'fulfilled') {
          features[name] = featureResults[i].value;
        }
      });
      this.features = features;
    } catch { /* ignore refresh errors */ }
  }

  _getStatusColor(status) {
    if (!status) return 'bg-slate-400';
    const s = String(status).toLowerCase();
    if (s === 'active' || s === 'online' || s === 'locked') return 'bg-green-500';
    if (s === 'error' || s === 'failed' || s === 'offline') return 'bg-red-500';
    if (s === 'scanning' || s === 'warning') return 'bg-yellow-500';
    return 'bg-slate-400';
  }

  _getBitrateForPos(pos) {
    const entry = this.bitrates.find(b => b.interface_pos === pos);
    return entry ? entry.bitrate : null;
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        </div>
      `;
    }

    return html`
      <div class="space-y-6">
        <!-- Header -->
        <div>
          <h1 class="text-2xl font-bold text-slate-800">Dashboard</h1>
          <p class="text-slate-500 text-sm">System overview and status</p>
        </div>

        <!-- Unit Info + Features Row -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- Unit Info Card -->
          ${this.unitInfo ? html`
            <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h2 class="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
                <span class="text-blue-500">ℹ️</span> Unit Information
              </h2>
              <div class="grid grid-cols-2 gap-4">
                ${this._infoItem('Serial', this.unitInfo.serial)}
                ${this._infoItem('Version', this.unitInfo.version)}
                ${this._infoItem('Hostname', this.unitInfo.hostname)}
                ${this._infoItem('Cloud', this.unitInfo.cloud ? 'Enabled' : 'Disabled')}
              </div>
            </div>
          ` : ''}

          <!-- Features Card -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
            <h2 class="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <span class="text-blue-500">🏷️</span> Feature Flags
            </h2>
            <div class="grid grid-cols-2 gap-3">
              ${Object.entries(this.features).map(([name, data]) => html`
                <div class="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                  <span class="text-sm font-medium text-slate-700 capitalize">${name.replace(/_/g, ' ')}</span>
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                    ${data?.enabled ? 'bg-green-100 text-green-800' : 'bg-slate-200 text-slate-600'}">
                    ${data?.enabled ? 'On' : 'Off'}
                  </span>
                </div>
              `)}
              ${Object.keys(this.features).length === 0 ? html`
                <p class="text-slate-400 text-sm col-span-2">No feature data available</p>
              ` : ''}
            </div>
          </div>
        </div>

        <!-- Interfaces Overview -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h2 class="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
            <span class="text-blue-500">🔌</span> Interfaces Overview
          </h2>
          ${this.interfaces.length === 0 ? html`
            <p class="text-slate-400 text-sm">No interfaces found</p>
          ` : html`
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              ${this.interfaces.map(iface => this._renderInterfaceCard(iface))}
            </div>
          `}
        </div>

        <!-- Bitrates -->
        ${this.bitrates.length > 0 ? html`
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
            <h2 class="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <span class="text-blue-500">📈</span> Current Bitrates
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              ${this.bitrates.map(b => html`
                <div class="p-4 bg-slate-50 rounded-lg border border-slate-200">
                  <div class="text-xs text-slate-500 mb-1">Interface ${b.interface_pos}</div>
                  <div class="text-2xl font-bold text-slate-800">
                    ${typeof b.bitrate === 'number' ? `${(b.bitrate / 1000000).toFixed(2)} Mbps` : b.bitrate || 'N/A'}
                  </div>
                </div>
              `)}
            </div>
          </div>
        ` : ''}
      </div>
    `;
  }

  _infoItem(label, value) {
    return html`
      <div class="p-3 bg-slate-50 rounded-lg">
        <div class="text-xs text-slate-500">${label}</div>
        <div class="text-sm font-semibold text-slate-800 font-mono">${value || 'N/A'}</div>
      </div>
    `;
  }

  _renderInterfaceCard(iface) {
    const bitrate = this._getBitrateForPos(iface.position);
    return html`
      <div class="p-4 bg-slate-50 rounded-lg border border-slate-200 hover:border-blue-300 hover:shadow-sm transition-all">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm font-semibold text-slate-800">${iface.name || `Interface ${iface.position}`}</span>
          <span class="h-2.5 w-2.5 rounded-full ${iface.active ? 'bg-green-500' : 'bg-red-500'}"></span>
        </div>
        <div class="space-y-1 text-xs text-slate-500">
          <div class="flex justify-between"><span>Position:</span><span class="text-slate-700 font-mono">${iface.position}</span></div>
          <div class="flex justify-between"><span>Type:</span><span class="text-slate-700">${iface.type || 'N/A'}</span></div>
          <div class="flex justify-between">
            <span>Status:</span>
            <span class="inline-flex items-center gap-1">
              <span class="h-1.5 w-1.5 rounded-full ${this._getStatusColor(iface.status)}"></span>
              <span class="text-slate-700">${iface.status || 'Unknown'}</span>
            </span>
          </div>
          ${bitrate != null ? html`
            <div class="flex justify-between"><span>Bitrate:</span><span class="text-slate-700 font-mono">${typeof bitrate === 'number' ? `${(bitrate / 1000000).toFixed(2)} Mbps` : bitrate}</span></div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-front-page', IxuiFrontPage);
