import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiInterfaces extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    interfaces: { type: Array },
    loading: { type: Boolean },
    expandedPos: { type: Number },
    selectedConfig: { type: Object },
    services: { type: Array },
    scanning: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.interfaces = [];
    this.loading = true;
    this.expandedPos = null;
    this.selectedConfig = null;
    this.services = [];
    this.scanning = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadInterfaces();
  }

  async _loadInterfaces() {
    this.loading = true;
    try {
      this.interfaces = await api.getInterfaces() || [];
    } catch { this.interfaces = []; }
    this.loading = false;
  }

  async _toggleExpand(iface) {
    if (this.expandedPos === iface.position) {
      this.expandedPos = null;
      this.selectedConfig = null;
      this.services = [];
      return;
    }

    this.expandedPos = iface.position;
    this.selectedConfig = null;
    this.services = [];

    try {
      const [config, services] = await Promise.allSettled([
        api.getInterfaceConfig(iface.position, iface.type),
        api.getServices(iface.position),
      ]);
      this.selectedConfig = config.status === 'fulfilled' ? config.value : null;
      this.services = services.status === 'fulfilled' ? (services.value || []) : [];
    } catch { /* ignore */ }
  }

  async _startScan(pos) {
    this.scanning = true;
    try {
      await api.startScan(pos);
      this._showNotification('Scan started successfully', 'success');
    } catch (err) {
      this._showNotification('Failed to start scan', 'error');
    }
    this.scanning = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
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
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Interfaces</h1>
            <p class="text-slate-500 text-sm">Manage and monitor interfaces</p>
          </div>
          <button
            @click=${this._loadInterfaces}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
          >↻ Refresh</button>
        </div>

        <!-- Notification -->
        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Table -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200">
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Position</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                <th class="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">Active</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              ${this.interfaces.map(iface => html`
                <tr
                  @click=${() => this._toggleExpand(iface)}
                  class="hover:bg-blue-50/50 cursor-pointer transition-colors ${this.expandedPos === iface.position ? 'bg-blue-50' : ''}"
                >
                  <td class="px-6 py-4 text-sm font-mono text-slate-700">${iface.position}</td>
                  <td class="px-6 py-4 text-sm font-medium text-slate-800">${iface.name || '—'}</td>
                  <td class="px-6 py-4">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      ${iface.type || 'N/A'}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm text-slate-600">${iface.status || 'Unknown'}</td>
                  <td class="px-6 py-4 text-center">
                    <span class="inline-block h-3 w-3 rounded-full ${iface.active ? 'bg-green-500' : 'bg-red-500'}"></span>
                  </td>
                </tr>

                <!-- Expanded Detail Panel -->
                ${this.expandedPos === iface.position ? html`
                  <tr>
                    <td colspan="5" class="px-6 py-6 bg-slate-50 border-t border-slate-200">
                      <div class="space-y-6">
                        <!-- Action buttons -->
                        <div class="flex gap-3">
                          <button
                            @click=${(e) => { e.stopPropagation(); this._startScan(iface.position); }}
                            class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                            ?disabled=${this.scanning}
                          >${this.scanning ? 'Scanning...' : '🔍 Start Scan'}</button>
                        </div>

                        <!-- Config Details -->
                        ${this.selectedConfig ? html`
                          <div>
                            <h4 class="text-sm font-semibold text-slate-700 mb-3">Configuration</h4>
                            <div class="bg-white rounded-lg border border-slate-200 p-4">
                              <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                                ${Object.entries(this.selectedConfig).map(([key, val]) => html`
                                  <div class="text-sm">
                                    <span class="text-slate-500">${key}:</span>
                                    <span class="ml-1 font-mono text-slate-800">${typeof val === 'object' ? JSON.stringify(val) : String(val)}</span>
                                  </div>
                                `)}
                              </div>
                            </div>
                          </div>
                        ` : ''}

                        <!-- Services -->
                        ${this.services.length > 0 ? html`
                          <div>
                            <h4 class="text-sm font-semibold text-slate-700 mb-3">Services (${this.services.length})</h4>
                            <div class="bg-white rounded-lg border border-slate-200 overflow-hidden">
                              <table class="w-full text-sm">
                                <thead>
                                  <tr class="bg-slate-100">
                                    <th class="px-4 py-2 text-left text-xs font-semibold text-slate-600">Name</th>
                                    <th class="px-4 py-2 text-left text-xs font-semibold text-slate-600">Type</th>
                                    <th class="px-4 py-2 text-left text-xs font-semibold text-slate-600">ID</th>
                                  </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-100">
                                  ${this.services.map(svc => html`
                                    <tr class="hover:bg-slate-50">
                                      <td class="px-4 py-2 text-slate-800">${svc.name || svc.service_name || '—'}</td>
                                      <td class="px-4 py-2 text-slate-600">${svc.type || svc.service_type || '—'}</td>
                                      <td class="px-4 py-2 font-mono text-slate-600">${svc.id || svc.service_id || '—'}</td>
                                    </tr>
                                  `)}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        ` : html`
                          <p class="text-sm text-slate-400">No services found for this interface</p>
                        `}
                      </div>
                    </td>
                  </tr>
                ` : ''}
              `)}
            </tbody>
          </table>

          ${this.interfaces.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No interfaces found</div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-interfaces', IxuiInterfaces);
