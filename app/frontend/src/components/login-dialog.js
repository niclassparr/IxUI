import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiLogin extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    username: { type: String },
    password: { type: String },
    error: { type: String },
    loading: { type: Boolean },
  };

  constructor() {
    super();
    this.username = 'admin';
    this.password = 'admin';
    this.error = '';
    this.loading = false;
  }

  async _handleLogin(e) {
    e.preventDefault();
    this.error = '';
    this.loading = true;

    try {
      const result = await api.login(this.username, this.password);
      if (result.success) {
        this.dispatchEvent(new CustomEvent('login-success', { bubbles: true, composed: true }));
      } else {
        this.error = result.error || 'Login failed. Please check your credentials.';
      }
    } catch (err) {
      this.error = 'Unable to connect to server.';
    } finally {
      this.loading = false;
    }
  }

  _onKeydown(e) {
    if (e.key === 'Enter') this._handleLogin(e);
  }

  render() {
    return html`
      <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
        <div class="w-full max-w-md">
          <div class="bg-white rounded-2xl shadow-2xl p-8">
            <!-- Logo -->
            <div class="flex justify-center mb-6">
              <img src="/static/images/logo.png" alt="IxUI" class="h-16" />
            </div>

            <h2 class="text-2xl font-bold text-center text-slate-800 mb-2">Welcome Back</h2>
            <p class="text-center text-slate-500 mb-6 text-sm">Sign in to your IxUI dashboard</p>

            <!-- Error message -->
            ${this.error ? html`
              <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm flex items-center gap-2">
                <span>⚠</span>
                <span>${this.error}</span>
              </div>
            ` : ''}

            <!-- Form -->
            <form @submit=${this._handleLogin} class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-slate-700 mb-1">Username</label>
                <input
                  type="text"
                  .value=${this.username}
                  @input=${(e) => this.username = e.target.value}
                  @keydown=${this._onKeydown}
                  class="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition text-slate-800 bg-slate-50"
                  placeholder="Enter username"
                  ?disabled=${this.loading}
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-slate-700 mb-1">Password</label>
                <input
                  type="password"
                  .value=${this.password}
                  @input=${(e) => this.password = e.target.value}
                  @keydown=${this._onKeydown}
                  class="w-full px-4 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition text-slate-800 bg-slate-50"
                  placeholder="Enter password"
                  ?disabled=${this.loading}
                />
              </div>

              <button
                type="submit"
                class="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md hover:shadow-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                ?disabled=${this.loading}
              >
                ${this.loading ? html`
                  <span class="inline-flex items-center gap-2">
                    <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                    </svg>
                    Signing in...
                  </span>
                ` : 'Sign In'}
              </button>
            </form>
          </div>

          <p class="text-center text-slate-500 text-xs mt-4">IxUI Management Console</p>
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-login', IxuiLogin);
