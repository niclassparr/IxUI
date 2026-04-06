import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiInterfaceLog extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    position: { type: String },
    log: { type: String },
    loading: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.position = '';
    this.log = '';
    this.loading = true;
    this.notification = null;
    this._refreshInterval = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadLog();
    this._refreshInterval = window.setInterval(() => this._loadLog(false), 5000);
  }

  disconnectedCallback() {
    if (this._refreshInterval) {
      window.clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }
    super.disconnectedCallback();
  }

  async _loadLog(showSpinner = true) {
    this.loading = showSpinner;
    try {
      const data = await api.getInterfaceLog(this.position);
      this.log = data?.log ?? '';
    } catch {
      this.log = '';
      this._showNotification('Failed to load log', 'error');
    }
    this.loading = false;
    await this.updateComplete;
    this._scrollToBottom();
  }

  _scrollToBottom() {
    const pre = this.querySelector('#log-output');
    if (pre) {
      pre.scrollTop = pre.scrollHeight;
    }
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
  }

  _goBack() {
    window.location.hash = '#interfaces';
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
          <div class="flex items-center gap-4">
            <button @click=${this._goBack}
              class="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">← Back</button>
            <div>
              <h1 class="text-2xl font-bold text-slate-800">Interface Log ${this.position}</h1>
              <p class="text-slate-500 text-sm">View interface output log · auto-refreshes every 5s</p>
            </div>
          </div>
          <button @click=${this._loadLog}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Log Output -->
        <div class="bg-slate-900 rounded-xl shadow-sm border border-slate-700 overflow-hidden">
          <div class="px-4 py-3 border-b border-slate-700 bg-slate-800 flex items-center gap-2">
            <span class="h-3 w-3 rounded-full bg-red-500"></span>
            <span class="h-3 w-3 rounded-full bg-yellow-500"></span>
            <span class="h-3 w-3 rounded-full bg-green-500"></span>
            <span class="ml-3 text-xs text-slate-400 font-mono">log — ${this.position}</span>
          </div>
          <pre
            id="log-output"
            class="p-4 text-sm font-mono text-green-400 overflow-auto whitespace-pre-wrap wrap-break-word"
            style="max-height: 600px; min-height: 300px;"
          >${this.log || 'No log data available.'}</pre>
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-interface-log', IxuiInterfaceLog);
