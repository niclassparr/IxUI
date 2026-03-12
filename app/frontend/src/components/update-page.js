import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiUpdate extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    packages: { type: Array },
    selectedPackages: { type: Object },
    updateResult: { type: String },
    loading: { type: Boolean },
    installing: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.packages = [];
    this.selectedPackages = new Set();
    this.updateResult = '';
    this.loading = true;
    this.installing = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
  }

  async _loadData() {
    this.loading = true;
    try {
      const [pkgs, result] = await Promise.allSettled([
        api.getUpdatePackages(),
        api.getUpdateResult(),
      ]);
      this.packages = pkgs.status === 'fulfilled' ? (pkgs.value || []) : [];
      this.updateResult = result.status === 'fulfilled' ? (result.value?.result || '') : '';
    } catch {
      this.packages = [];
      this.updateResult = '';
    }
    this.selectedPackages = new Set();
    this.loading = false;
  }

  _togglePackage(name) {
    const next = new Set(this.selectedPackages);
    if (next.has(name)) {
      next.delete(name);
    } else {
      next.add(name);
    }
    this.selectedPackages = next;
  }

  _toggleAll() {
    if (this.selectedPackages.size === this.packages.length) {
      this.selectedPackages = new Set();
    } else {
      this.selectedPackages = new Set(this.packages.map(p => p.name));
    }
  }

  async _install() {
    if (this.selectedPackages.size === 0) {
      this._showNotification('No packages selected', 'error');
      return;
    }
    this.installing = true;
    try {
      await api.installUpdatePackages(
        [...this.selectedPackages].map(name => {
          const pkg = this.packages.find(p => p.name === name);
          return { name, version: pkg?.version || null, installed: pkg?.installed || false };
        })
      );
      this._showNotification('Installation started successfully', 'success');
      setTimeout(() => this._loadData(), 3000);
    } catch {
      this._showNotification('Failed to start installation', 'error');
    }
    this.installing = false;
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
            <h1 class="text-2xl font-bold text-slate-800">Software Update</h1>
            <p class="text-slate-500 text-sm">Manage system packages and updates</p>
          </div>
          <div class="flex gap-3">
            <button @click=${this._loadData}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
            <button @click=${this._install}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.installing || this.selectedPackages.size === 0}
            >${this.installing ? 'Installing...' : `📦 Install Selected (${this.selectedPackages.size})`}</button>
          </div>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Packages Table -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200">
                <th class="px-6 py-3 text-left">
                  <input type="checkbox"
                    .checked=${this.packages.length > 0 && this.selectedPackages.size === this.packages.length}
                    @change=${this._toggleAll}
                    class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                  />
                </th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Package</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Version</th>
                <th class="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              ${this.packages.map(pkg => html`
                <tr class="hover:bg-blue-50/50 transition-colors cursor-pointer" @click=${() => this._togglePackage(pkg.name)}>
                  <td class="px-6 py-4">
                    <input type="checkbox"
                      .checked=${this.selectedPackages.has(pkg.name)}
                      @change=${() => this._togglePackage(pkg.name)}
                      @click=${(e) => e.stopPropagation()}
                      class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    />
                  </td>
                  <td class="px-6 py-4 text-sm font-medium text-slate-800">${pkg.name}</td>
                  <td class="px-6 py-4 text-sm font-mono text-slate-600">${pkg.version || '—'}</td>
                  <td class="px-6 py-4 text-center">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                      ${pkg.installed ? 'bg-green-100 text-green-800' : 'bg-slate-100 text-slate-800'}">
                      ${pkg.installed ? 'Installed' : 'Available'}
                    </span>
                  </td>
                </tr>
              `)}
            </tbody>
          </table>

          ${this.packages.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No packages available</div>
          ` : ''}
        </div>

        <!-- Last Update Result -->
        ${this.updateResult ? html`
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
              <h2 class="text-sm font-semibold text-slate-700">Last Update Result</h2>
            </div>
            <pre class="p-4 text-sm font-mono text-slate-700 whitespace-pre-wrap overflow-auto" style="max-height: 300px;">${this.updateResult}</pre>
          </div>
        ` : ''}
      </div>
    `;
  }
}

customElements.define('ixui-update', IxuiUpdate);
