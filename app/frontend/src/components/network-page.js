import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiNetwork extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    networkStatus: { type: Object },
    loading: { type: Boolean },
  };

  constructor() {
    super();
    this.networkStatus = null;
    this.loading = true;
    this._interval = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadStatus();
    this._interval = setInterval(() => this._loadStatus(), 15000);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._interval) clearInterval(this._interval);
  }

  async _loadStatus() {
    try {
      this.networkStatus = await api.getNetworkStatus2();
    } catch {
      // Fallback to v1 endpoint
      try {
        const data = await api.getNetworkStatus();
        this.networkStatus = data;
      } catch { /* ignore */ }
    }
    this.loading = false;
  }

  _getStatusColor(status) {
    if (!status) return 'bg-slate-400';
    const s = String(status).toLowerCase();
    if (s === 'up' || s === 'active' || s === 'online' || s === 'connected') return 'bg-green-500';
    if (s === 'down' || s === 'inactive' || s === 'offline' || s === 'disconnected') return 'bg-red-500';
    return 'bg-yellow-500';
  }

  _getStatusBadge(status) {
    if (!status) return 'bg-slate-100 text-slate-600';
    const s = String(status).toLowerCase();
    if (s === 'up' || s === 'active' || s === 'online' || s === 'connected') return 'bg-green-100 text-green-800';
    if (s === 'down' || s === 'inactive' || s === 'offline' || s === 'disconnected') return 'bg-red-100 text-red-800';
    return 'bg-yellow-100 text-yellow-800';
  }

  _renderEntries() {
    if (!this.networkStatus) return [];

    // Handle object format {ip: {ip, mac, status}} from network-status2
    if (!Array.isArray(this.networkStatus) && typeof this.networkStatus === 'object') {
      return Object.entries(this.networkStatus).map(([key, val]) => ({
        name: key,
        ip: val?.ip || key,
        mac: val?.mac || 'N/A',
        status: val?.status || 'Unknown',
      }));
    }

    // Handle array format from network-status
    if (Array.isArray(this.networkStatus)) {
      return this.networkStatus.map((entry, i) => ({
        name: `Interface ${i + 1}`,
        ip: entry.ip || 'N/A',
        mac: entry.mac || 'N/A',
        status: entry.status || 'Unknown',
      }));
    }

    return [];
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        </div>
      `;
    }

    const entries = this._renderEntries();

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Network</h1>
            <p class="text-slate-500 text-sm">Network interface status — auto-refreshes every 15s</p>
          </div>
          <button
            @click=${() => { this.loading = true; this._loadStatus(); }}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
          >↻ Refresh</button>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200">
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Interface</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">IP Address</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">MAC Address</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              ${entries.map(entry => html`
                <tr class="hover:bg-blue-50/50 transition-colors">
                  <td class="px-6 py-4 text-sm font-medium text-slate-800">${entry.name}</td>
                  <td class="px-6 py-4 text-sm font-mono text-slate-600">${entry.ip}</td>
                  <td class="px-6 py-4 text-sm font-mono text-slate-600">${entry.mac}</td>
                  <td class="px-6 py-4">
                    <span class="inline-flex items-center gap-2">
                      <span class="h-2.5 w-2.5 rounded-full ${this._getStatusColor(entry.status)}"></span>
                      <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${this._getStatusBadge(entry.status)}">
                        ${entry.status}
                      </span>
                    </span>
                  </td>
                </tr>
              `)}
            </tbody>
          </table>

          ${entries.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No network information available</div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-network', IxuiNetwork);
