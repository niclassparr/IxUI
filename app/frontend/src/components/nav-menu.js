import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiNav extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    activePage: { type: String },
    unitInfo: { type: Object },
  };

  constructor() {
    super();
    this.activePage = 'front-page';
    this.unitInfo = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadUnitInfo();
  }

  async _loadUnitInfo() {
    try {
      this.unitInfo = await api.getUnitInfo();
    } catch { /* ignore */ }
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
      { id: 'front-page', label: 'Dashboard', icon: '📊' },
      { type: 'separator' },
      { id: 'interfaces', label: 'Interfaces', icon: '🔌' },
      { id: 'routes', label: 'Routes', icon: '🔀' },
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
      items.push({ id: 'forced-content', label: 'Forced Content', icon: '📌' });
    }
    if (this.unitInfo?.software_update) {
      items.push({ id: 'update', label: 'Software Update', icon: '🔄' });
    }

    return items;
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
