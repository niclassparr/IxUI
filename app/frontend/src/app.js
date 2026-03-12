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
    this.currentPage = 'front-page';
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
    const hash = window.location.hash.replace('#', '') || 'front-page';
    this.currentPage = hash;
  }

  _onLoginSuccess() {
    this.loggedIn = true;
    window.location.hash = '#front-page';
  }

  async _onLogout() {
    await api.logout();
    this.loggedIn = false;
    window.location.hash = '';
  }

  _onNavigate(e) {
    this.currentPage = e.detail.page;
    window.location.hash = `#${e.detail.page}`;
  }

  _renderPage() {
    switch (this.currentPage) {
      case 'interfaces':
        return html`<ixui-interfaces></ixui-interfaces>`;
      case 'routes':
        return html`<ixui-routes></ixui-routes>`;
      case 'settings':
        return html`<ixui-settings></ixui-settings>`;
      case 'network':
        return html`<ixui-network></ixui-network>`;
      case 'commands':
        return html`<ixui-commands></ixui-commands>`;
      case 'front-page':
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
          .activePage=${this.currentPage}
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
