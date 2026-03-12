import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiCloud extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    cloudDetails: { type: Object },
    loading: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.cloudDetails = null;
    this.loading = true;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadCloudDetails();
  }

  async _loadCloudDetails() {
    this.loading = true;
    try {
      this.cloudDetails = await api.getCloudDetails();
    } catch {
      this.cloudDetails = null;
      this._showNotification('Failed to load cloud details', 'error');
    }
    this.loading = false;
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

    const status = this.cloudDetails?.status || 'Unknown';
    const isConnected = status.toLowerCase() === 'connected';

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Cloud</h1>
            <p class="text-slate-500 text-sm">Cloud connection management</p>
          </div>
          <button @click=${this._loadCloudDetails}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        ${!this.cloudDetails ? html`
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-center text-slate-400">
            No cloud information available
          </div>
        ` : html`
          <!-- Status Card -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
              <h2 class="text-sm font-semibold text-slate-700">Connection Status</h2>
            </div>
            <div class="p-6">
              <div class="flex items-center justify-center mb-6">
                <span class="inline-flex items-center gap-3 px-6 py-3 rounded-full text-lg font-bold
                  ${isConnected ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                  <span class="h-4 w-4 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}"></span>
                  ${status}
                </span>
              </div>

              <div class="divide-y divide-slate-100">
                <div class="flex items-center justify-between py-4 px-2">
                  <span class="text-sm font-medium text-slate-600">Cloud ID</span>
                  <span class="text-sm font-mono text-slate-800">${this.cloudDetails.cloud_id || '—'}</span>
                </div>
                <div class="flex items-center justify-between py-4 px-2">
                  <span class="text-sm font-medium text-slate-600">Last Sync</span>
                  <span class="text-sm font-mono text-slate-800">${this.cloudDetails.last_sync || '—'}</span>
                </div>
                <div class="flex items-center justify-between py-4 px-2">
                  <span class="text-sm font-medium text-slate-600">Endpoint</span>
                  <span class="text-sm font-mono text-slate-800 break-all">${this.cloudDetails.endpoint || '—'}</span>
                </div>
                <div class="flex items-center justify-between py-4 px-2">
                  <span class="text-sm font-medium text-slate-600">Registration URL</span>
                  <span class="text-sm font-mono text-slate-800 break-all">${this.cloudDetails.registration_url || '—'}</span>
                </div>
              </div>
            </div>
          </div>
        `}
      </div>
    `;
  }
}

customElements.define('ixui-cloud', IxuiCloud);
