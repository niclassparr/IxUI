import { LitElement, html } from 'lit';
import * as api from './services/api.js';
import './components/login-dialog.js';
import './components/nav-menu.js';
import './components/front-page.js';
import './components/interfaces-page.js';
import './components/routes-page.js';
import './components/settings-page.js';
import './components/network-page.js';
import './components/commands-page.js';
import './components/interface-edit-page.js';
import './components/interface-status-page.js';
import './components/interface-log-page.js';
import './components/cloud-page.js';
import './components/update-page.js';
import './components/hls-wizard-page.js';
import './components/forced-content-page.js';

const _PAGE_ALIASES = {
  '': 'dashboard',
  'front-page': 'dashboard',
  dashboard: 'dashboard',
  routes: 'layout',
  layout: 'layout',
  'forced-content': 'force-content',
  'force-content': 'force-content',
};

function _normalizePageToken(page) {
  return _PAGE_ALIASES[page] || page || 'dashboard';
}

function _navSectionForPage(page) {
  const normalizedPage = _normalizePageToken(page);
  if (['interface-edit', 'interface-status', 'interface-log'].includes(normalizedPage)) {
    return 'interfaces';
  }
  return normalizedPage;
}

class IxuiApp extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    loggedIn: { type: Boolean },
    currentPage: { type: String },
    checking: { type: Boolean },
  };

  constructor() {
    super();
    this.loggedIn = false;
    this.currentPage = 'dashboard';
    this.checking = true;
  }

  connectedCallback() {
    super.connectedCallback();
    window.addEventListener('hashchange', () => this._onHashChange());
    this._checkSession();
  }

  async _checkSession() {
    this.checking = true;
    if (api.isLoggedIn()) {
      const valid = await api.validateSession();
      this.loggedIn = valid;
      if (!valid) {
        await api.logout();
      }
    }
    this.checking = false;
    this._onHashChange();
    window.dispatchEvent(new CustomEvent('app-ready'));
  }

  _onHashChange() {
    const params = this._parseParams();
    const canonicalHash = this._canonicalizeHash(params);
    const currentHash = window.location.hash.replace('#', '');
    if (currentHash !== canonicalHash) {
      window.history.replaceState(
        null,
        '',
        `${window.location.pathname}${window.location.search}#${canonicalHash}`,
      );
    }
    this.currentPage = params.page;
  }

  _parseParams(hash = window.location.hash.replace('#', '')) {
    const parts = hash.split('/');
    return {
      page: _normalizePageToken(parts[0]),
      pos: parts[1],
      type: parts[2],
    };
  }

  _canonicalizeHash(params) {
    return [
      _normalizePageToken(params.page),
      params.pos,
      params.type,
    ].filter((part) => part != null && part !== '').join('/');
  }

  _onLoginSuccess() {
    this.loggedIn = true;
    window.location.hash = '#dashboard';
  }

  async _onLogout() {
    await api.logout();
    this.loggedIn = false;
    window.location.hash = '';
  }

  _onNavigate(e) {
    const page = _normalizePageToken(e.detail.page);
    this.currentPage = page;
    window.location.hash = `#${page}`;
  }

  _renderPage() {
    const params = this._parseParams();
    const page = params.page || 'dashboard';
    switch (page) {
      case 'dashboard':
        return html`<ixui-front-page></ixui-front-page>`;
      case 'interfaces':
        return html`<ixui-interfaces></ixui-interfaces>`;
      case 'layout':
        return html`<ixui-routes></ixui-routes>`;
      case 'settings':
        return html`<ixui-settings></ixui-settings>`;
      case 'network':
        return html`<ixui-network></ixui-network>`;
      case 'commands':
        return html`<ixui-commands></ixui-commands>`;
      case 'interface-edit':
        return html`<ixui-interface-edit .position=${params.pos} .interfaceType=${params.type}></ixui-interface-edit>`;
      case 'interface-status':
        return html`<ixui-interface-status .position=${params.pos} .interfaceType=${params.type}></ixui-interface-status>`;
      case 'interface-log':
        return html`<ixui-interface-log .position=${params.pos}></ixui-interface-log>`;
      case 'cloud':
        return html`<ixui-cloud></ixui-cloud>`;
      case 'update':
        return html`<ixui-update></ixui-update>`;
      case 'hls-wizard':
        return html`<ixui-hls-wizard></ixui-hls-wizard>`;
      case 'force-content':
        return html`<ixui-forced-content></ixui-forced-content>`;
      default:
        return html`<ixui-front-page></ixui-front-page>`;
    }
  }

  render() {
    if (this.checking) {
      return html``;
    }

    if (!this.loggedIn) {
      return html`<ixui-login @login-success=${this._onLoginSuccess}></ixui-login>`;
    }

    return html`
      <div class="flex min-h-screen">
        <ixui-nav
          .activePage=${_navSectionForPage(this.currentPage)}
          @navigate=${this._onNavigate}
          @logout=${this._onLogout}
        ></ixui-nav>
        <main class="flex-1 ml-64 bg-slate-100 min-h-screen">
          <div class="p-6 max-w-7xl mx-auto">
            ${this._renderPage()}
          </div>
        </main>
      </div>
    `;
  }
}

customElements.define('ixui-app', IxuiApp);
