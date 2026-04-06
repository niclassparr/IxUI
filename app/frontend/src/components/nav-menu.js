import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiNav extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    activePage: { type: String },
    unitInfo: { type: Object },
    configChanged: { type: Boolean },
    pushingConfig: { type: Boolean },
    shellMessage: { type: Object },
  };

  constructor() {
    super();
    this.activePage = 'dashboard';
    this.unitInfo = null;
    this.configChanged = false;
    this.pushingConfig = false;
    this.shellMessage = null;
    this._configStatusTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadShellContext();
    this._configStatusTimer = window.setInterval(() => {
      this._refreshConfigStatus();
    }, 5000);
  }

  disconnectedCallback() {
    if (this._configStatusTimer) {
      window.clearInterval(this._configStatusTimer);
      this._configStatusTimer = null;
    }
    super.disconnectedCallback();
  }

  async _loadShellContext() {
    await Promise.allSettled([
      this._loadUnitInfo(),
      this._refreshConfigStatus(),
    ]);
  }

  async _loadUnitInfo() {
    try {
      this.unitInfo = await api.getUnitInfo();
    } catch { /* ignore */ }
  }

  async _refreshConfigStatus() {
    try {
      const status = await api.getConfigStatus();
      this.configChanged = Boolean(status.config_changed);
      if (!this.configChanged && this.shellMessage?.type === 'error') {
        this.shellMessage = null;
      }
    } catch {
      this.configChanged = false;
    }
  }

  async _pushConfig() {
    this.pushingConfig = true;
    try {
      const response = await api.pushConfig();
      if (response.success) {
        this.configChanged = false;
        this.shellMessage = { type: 'success', text: 'Configuration pushed successfully.' };
      } else {
        this.shellMessage = { type: 'error', text: response.error || 'Failed to push configuration.' };
      }
    } catch (error) {
      this.shellMessage = { type: 'error', text: error.message || 'Failed to push configuration.' };
    }
    await this._refreshConfigStatus();
    this.pushingConfig = false;
  }

  _navigate(page) {
    this.dispatchEvent(new CustomEvent('navigate', {
      detail: { page },
      bubbles: true,
      composed: true,
    }));
  }

  _logout() {
    this.dispatchEvent(new CustomEvent('logout', { bubbles: true, composed: true }));
  }

  _navItems() {
    const items = [
      { id: 'dashboard', label: 'Dashboard', icon: '📊' },
      { type: 'separator' },
      { id: 'interfaces', label: 'Interfaces', icon: '🔌' },
      { id: 'layout', label: 'Layout', icon: '🔀' },
      { type: 'separator' },
      { id: 'settings', label: 'Settings', icon: '⚙️' },
      { id: 'network', label: 'Network', icon: '🌐' },
      { id: 'commands', label: 'Commands', icon: '⚡' },
      { type: 'separator' },
    ];

    if (this.unitInfo?.cloud) {
      items.push({ id: 'cloud', label: 'Cloud', icon: '☁️' });
    }
    if (this.unitInfo?.hls_output) {
      items.push({ id: 'hls-wizard', label: 'HLS Wizard', icon: '📺' });
    }
    if (this.unitInfo?.forced_content) {
      items.push({ id: 'force-content', label: 'Forced Content', icon: '📌' });
    }
    if (this.unitInfo?.software_update) {
      items.push({ id: 'update', label: 'Software Update', icon: '🔄' });
    }

    return items;
  }

  _renderConfigBanner() {
    if (!this.configChanged && !this.shellMessage) {
      return html``;
    }

    return html`
      <div class="px-4 py-3 border-b border-slate-700 bg-slate-900/40 space-y-2">
        ${this.configChanged ? html`
          <div class="rounded-xl border border-amber-400/50 bg-amber-300/10 p-3">
            <div class="text-[11px] uppercase tracking-[0.18em] text-amber-200 font-semibold">Pending Configuration</div>
            <p class="mt-2 text-sm text-slate-100">Changes are waiting to be pushed to the runtime.</p>
            <button
              @click=${this._pushConfig}
              class="mt-3 w-full rounded-lg bg-amber-500 px-3 py-2 text-sm font-semibold text-slate-900 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:opacity-60"
              ?disabled=${this.pushingConfig}
            >${this.pushingConfig ? 'Pushing...' : 'Push Config'}</button>
          </div>
        ` : ''}

        ${this.shellMessage ? html`
          <div class="rounded-lg px-3 py-2 text-xs ${this.shellMessage.type === 'error'
            ? 'bg-red-500/15 text-red-200 border border-red-400/30'
            : 'bg-emerald-500/15 text-emerald-200 border border-emerald-400/30'}">
            ${this.shellMessage.text}
          </div>
        ` : ''}
      </div>
    `;
  }

  render() {
    return html`
      <aside class="fixed left-0 top-0 bottom-0 w-64 bg-slate-800 text-white flex flex-col z-40">
        <!-- Logo -->
        <div class="p-5 border-b border-slate-700">
          <div class="flex items-center gap-3">
            <img src="/static/images/logo.png" alt="IxUI" class="h-10" />
            <div>
              <h1 class="text-lg font-bold text-white">IxUI</h1>
              <p class="text-xs text-slate-400">Management Console</p>
            </div>
          </div>
        </div>

        <!-- Unit Info -->
        ${this.unitInfo ? html`
          <div class="px-5 py-3 border-b border-slate-700 bg-slate-750">
            <div class="text-xs text-slate-400 space-y-1">
              ${this.unitInfo.serial ? html`<div class="flex justify-between"><span>Serial:</span><span class="text-slate-300 font-mono">${this.unitInfo.serial}</span></div>` : ''}
              ${this.unitInfo.version ? html`<div class="flex justify-between"><span>Version:</span><span class="text-slate-300 font-mono">${this.unitInfo.version}</span></div>` : ''}
              ${this.unitInfo.hostname ? html`<div class="flex justify-between"><span>Host:</span><span class="text-slate-300 font-mono">${this.unitInfo.hostname}</span></div>` : ''}
            </div>
          </div>
        ` : ''}

        ${this._renderConfigBanner()}

        <!-- Navigation -->
        <nav class="flex-1 py-4 overflow-y-auto">
          <ul class="space-y-1 px-3">
            ${this._navItems().map(item => item.type === 'separator' ? html`
              <li class="my-2 border-t border-slate-700"></li>
            ` : html`
              <li>
                <button
                  @click=${() => this._navigate(item.id)}
                  class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150
                    ${this.activePage === item.id
                      ? 'bg-blue-600 text-white shadow-md'
                      : 'text-slate-300 hover:bg-slate-700 hover:text-white'}"
                >
                  <span class="text-lg">${item.icon}</span>
                  <span>${item.label}</span>
                </button>
              </li>
            `)}
          </ul>
        </nav>

        <!-- Logout -->
        <div class="p-3 border-t border-slate-700">
          <button
            @click=${this._logout}
            class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:bg-red-600/20 hover:text-red-400 transition-all duration-150"
          >
            <span class="text-lg">🚪</span>
            <span>Logout</span>
          </button>
        </div>
      </aside>
    `;
  }
}

customElements.define('ixui-nav', IxuiNav);
