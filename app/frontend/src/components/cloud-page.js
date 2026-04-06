import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiCloud extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    cloudDetails: { type: Object },
    loading: { type: Boolean },
    runningCommand: { type: String },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.cloudDetails = null;
    this.loading = true;
    this.runningCommand = null;
    this.notification = null;
    this._refreshInterval = null;
    this._notificationTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadCloudDetails();
    this._refreshInterval = window.setInterval(() => {
      this._loadCloudDetails(false);
    }, 5000);
  }

  disconnectedCallback() {
    if (this._refreshInterval) {
      window.clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
      this._notificationTimer = null;
    }
    super.disconnectedCallback();
  }

  async _loadCloudDetails(showSpinner = true) {
    this.loading = showSpinner;
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
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
    }
    this._notificationTimer = window.setTimeout(() => { this.notification = null; }, 3000);
  }

  _cloudEnabled() {
    return String(this.cloudDetails?.ixcloud_enable || 'false').toLowerCase() === 'true';
  }

  _cloudOnline() {
    return String(this.cloudDetails?.ixcloud_online || 'false').toLowerCase() === 'true';
  }

  async _runCloudCommand(command) {
    this.runningCommand = command;
    try {
      const response = await api.runCommand(command);
      if (!response?.success) {
        this._showNotification(response?.error || 'Cloud command failed', 'error');
        this.runningCommand = null;
        return;
      }
      this._showNotification(
        command === 'ixcloud-connect' ? 'Cloud connect command completed' : 'Cloud disconnect command completed',
        'success'
      );
      await this._loadCloudDetails(false);
    } catch (error) {
      this._showNotification(error?.message || 'Cloud command failed', 'error');
    }
    this.runningCommand = null;
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        </div>
      `;
    }

    const cloudEnabled = this._cloudEnabled();
    const isConnected = this._cloudOnline();
    const status = isConnected ? 'Online' : 'Offline';

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Cloud</h1>
            <p class="text-slate-500 text-sm">Legacy cloud status view with 5 second polling and connect or disconnect actions</p>
          </div>
          <div class="flex gap-3">
            ${cloudEnabled ? html`
              <button
                @click=${() => this._runCloudCommand('ixcloud-connect')}
                class="px-4 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 transition disabled:opacity-50"
                ?disabled=${this.runningCommand != null}
              >${this.runningCommand === 'ixcloud-connect' ? 'Connecting...' : 'Connect'}</button>
              <button
                @click=${() => this._runCloudCommand('ixcloud-disconnect')}
                class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 transition disabled:opacity-50"
                ?disabled=${this.runningCommand != null}
              >${this.runningCommand === 'ixcloud-disconnect' ? 'Disconnecting...' : 'Disconnect'}</button>
            ` : ''}
            <button @click=${this._loadCloudDetails}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
          </div>
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
        ` : !cloudEnabled ? html`
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-slate-600">
            The cloud function is disabled.
          </div>
        ` : html`
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
                  <span class="text-sm font-medium text-slate-600">Date</span>
                  <span class="text-sm font-mono text-slate-800">${this.cloudDetails.ixcloud_validate_date || '—'}</span>
                </div>
                <div class="flex items-center justify-between py-4 px-2">
                  <span class="text-sm font-medium text-slate-600">Message</span>
                  <span class="text-sm text-slate-800 text-right max-w-xl">${this.cloudDetails.ixcloud_validate_message || '—'}</span>
                </div>
                <div class="flex items-center justify-between py-4 px-2">
                  <span class="text-sm font-medium text-slate-600">BeaconId</span>
                  <span class="text-sm font-mono text-slate-800 break-all">${this.cloudDetails.ixcloud_beaconid || '—'}</span>
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
