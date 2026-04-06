import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiUpdate extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    softwareUpdateEnabled: { type: Boolean },
    stage: { type: String },
    packages: { type: Array },
    selectedPackages: { type: Object },
    updateResult: { type: String },
    loading: { type: Boolean },
    checking: { type: Boolean },
    installing: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.softwareUpdateEnabled = false;
    this.stage = 'init';
    this.packages = [];
    this.selectedPackages = new Set();
    this.updateResult = '';
    this.loading = true;
    this.checking = false;
    this.installing = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadInit();
  }

  async _loadInit() {
    this.loading = true;
    try {
      const info = await api.getUnitInfo();
      this.softwareUpdateEnabled = Boolean(info?.software_update);
    } catch {
      this.softwareUpdateEnabled = false;
    }
    this.stage = 'init';
    this.packages = [];
    this.updateResult = '';
    this.selectedPackages = new Set();
    this.loading = false;
  }

  async _checkForUpdates() {
    if (!this.softwareUpdateEnabled) {
      return;
    }

    this.checking = true;
    this.notification = null;
    try {
      const response = await api.checkUpdatePackages();
      if (!response?.success) {
        this._showNotification(response?.error || 'Failed to check for updates.', 'error');
        this.checking = false;
        return;
      }

      const packages = await api.getUpdatePackages();
      this.packages = packages || [];
      this.selectedPackages = new Set();

      if (this.packages.length === 0) {
        this._showNotification('No packages needs to be updated.', 'warning');
      } else {
        this.stage = 'table';
      }
    } catch {
      this._showNotification('Failed to check for updates.', 'error');
    }
    this.checking = false;
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
      this._showNotification('No packages selected.', 'warning');
      return;
    }
    this.installing = true;
    try {
      await api.installUpdatePackages(
        this.packages.map((pkg) => ({
          ...pkg,
          update: this.selectedPackages.has(pkg.name),
        }))
      );
      const result = await api.getUpdateResult();
      this.updateResult = result?.result || '';
      this.stage = 'result';
    } catch {
      this._showNotification('Failed to start installation.', 'error');
    }
    this.installing = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
  }

  _notificationClasses() {
    if (!this.notification) {
      return '';
    }
    switch (this.notification.type) {
      case 'success':
        return 'bg-green-50 text-green-800 border border-green-200';
      case 'warning':
        return 'bg-amber-50 text-amber-800 border border-amber-200';
      case 'error':
      default:
        return 'bg-red-50 text-red-800 border border-red-200';
    }
  }

  _renderInitView() {
    if (!this.softwareUpdateEnabled) {
      return html`
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-slate-600">
          The update software function is disabled.
        </div>
      `;
    }

    return html`
      <div class="space-y-4">
        <div class="rounded-xl border border-blue-200 bg-blue-50 px-4 py-4 text-sm text-blue-900">
          Check for available software updates. This might take several minutes.
        </div>
        <button
          @click=${this._checkForUpdates}
          class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
          ?disabled=${this.checking}
        >${this.checking ? 'Checking...' : 'OK'}</button>
      </div>
    `;
  }

  _renderTableView() {
    return html`
      <div class="space-y-4">
        <div class="rounded-xl border border-blue-200 bg-blue-50 px-4 py-4 text-sm text-blue-900">
          Update selected packages. This might take several minutes.
        </div>
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200">
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Update</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Version</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              ${this.packages.map(pkg => html`
                <tr class="hover:bg-blue-50/50 transition-colors cursor-pointer" @click=${() => this._togglePackage(pkg.name)}>
                  <td class="px-6 py-4">
                    <input type="checkbox"
                      .checked=${this.selectedPackages.has(pkg.name)}
                      @change=${() => this._togglePackage(pkg.name)}
                      @click=${(event) => event.stopPropagation()}
                      class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    />
                  </td>
                  <td class="px-6 py-4 text-sm font-medium text-slate-800">${pkg.name}</td>
                  <td class="px-6 py-4 text-sm font-mono text-slate-600">${pkg.version || '—'}</td>
                </tr>
              `)}
            </tbody>
          </table>
        </div>
        <button @click=${this._install}
          class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
          ?disabled=${this.installing}
        >${this.installing ? 'Updating...' : 'Update'}</button>
      </div>
    `;
  }

  _renderResultView() {
    return html`
      <div class="space-y-4">
        <pre class="bg-white rounded-xl shadow-sm border border-slate-200 p-4 text-sm font-mono text-slate-700 whitespace-pre-wrap overflow-auto" style="max-height: 420px;">${this.updateResult}</pre>
        <button @click=${this._loadInit}
          class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
        >Back</button>
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

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Software Update</h1>
            <p class="text-slate-500 text-sm">Legacy check, select, install, and result workflow</p>
          </div>
          <button @click=${this._loadInit}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium ${this._notificationClasses()}">
            ${this.notification.message}
          </div>
        ` : ''}

        ${this.stage === 'init' ? this._renderInitView() : ''}
        ${this.stage === 'table' ? this._renderTableView() : ''}
        ${this.stage === 'result' ? this._renderResultView() : ''}
      </div>
    `;
  }
}

customElements.define('ixui-update', IxuiUpdate);
