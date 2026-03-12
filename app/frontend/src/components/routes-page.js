import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiRoutes extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    routes: { type: Array },
    loading: { type: Boolean },
    sortField: { type: String },
    sortDir: { type: String },
  };

  constructor() {
    super();
    this.routes = [];
    this.loading = true;
    this.sortField = 'service_name';
    this.sortDir = 'asc';
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadRoutes();
  }

  async _loadRoutes() {
    this.loading = true;
    try {
      this.routes = await api.getRoutes() || [];
    } catch { this.routes = []; }
    this.loading = false;
  }

  _toggleSort(field) {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
    }
    this.requestUpdate();
  }

  _sortedRoutes() {
    const sorted = [...this.routes];
    sorted.sort((a, b) => {
      const aVal = a[this.sortField] ?? '';
      const bVal = b[this.sortField] ?? '';
      const cmp = typeof aVal === 'number' && typeof bVal === 'number'
        ? aVal - bVal
        : String(aVal).localeCompare(String(bVal));
      return this.sortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }

  _sortIcon(field) {
    if (this.sortField !== field) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        </div>
      `;
    }

    const columns = [
      { key: 'service_name', label: 'Service Name' },
      { key: 'interface_pos', label: 'Interface' },
      { key: 'interface_type', label: 'Type' },
      { key: 'lcn', label: 'LCN' },
      { key: 'out_sid', label: 'Out SID' },
      { key: 'out_ip', label: 'Out IP' },
      { key: 'hls_enable', label: 'HLS' },
      { key: 'output_name', label: 'Output Name' },
    ];

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Routes</h1>
            <p class="text-slate-500 text-sm">${this.routes.length} route${this.routes.length !== 1 ? 's' : ''} configured</p>
          </div>
          <button
            @click=${this._loadRoutes}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
          >↻ Refresh</button>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  ${columns.map(col => html`
                    <th
                      @click=${() => this._toggleSort(col.key)}
                      class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider cursor-pointer hover:bg-slate-100 select-none transition"
                    >
                      <span class="inline-flex items-center gap-1">
                        ${col.label}
                        <span class="text-slate-400">${this._sortIcon(col.key)}</span>
                      </span>
                    </th>
                  `)}
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                ${this._sortedRoutes().map(route => html`
                  <tr class="hover:bg-blue-50/50 transition-colors">
                    <td class="px-5 py-3 text-sm font-medium text-slate-800">${route.service_name || '—'}</td>
                    <td class="px-5 py-3 text-sm font-mono text-slate-600">${route.interface_pos ?? '—'}</td>
                    <td class="px-5 py-3 text-sm text-slate-600">${route.interface_type || '—'}</td>
                    <td class="px-5 py-3 text-sm font-mono text-slate-600">${route.lcn ?? '—'}</td>
                    <td class="px-5 py-3 text-sm font-mono text-slate-600">${route.out_sid ?? '—'}</td>
                    <td class="px-5 py-3 text-sm font-mono text-slate-600">${route.out_ip || '—'}</td>
                    <td class="px-5 py-3">
                      ${route.hls_enable ? html`
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Enabled</span>
                      ` : html`
                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-500">Disabled</span>
                      `}
                    </td>
                    <td class="px-5 py-3 text-sm text-slate-600">${route.output_name || '—'}</td>
                  </tr>
                `)}
              </tbody>
            </table>
          </div>

          ${this.routes.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No routes configured</div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-routes', IxuiRoutes);
