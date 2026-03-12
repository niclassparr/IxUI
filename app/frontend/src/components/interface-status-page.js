import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiInterfaceStatus extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    position: { type: String },
    interfaceType: { type: String },
    tunerStatus: { type: Object },
    streamerStatus: { type: Object },
    loading: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.position = '';
    this.interfaceType = '';
    this.tunerStatus = null;
    this.streamerStatus = null;
    this.loading = true;
    this.notification = null;
    this._refreshInterval = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadStatus();
    this._refreshInterval = setInterval(() => this._loadStatus(), 5000);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._refreshInterval) {
      clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }
  }

  async _loadStatus() {
    try {
      const [tuner, streamer] = await Promise.allSettled([
        api.getTunerStatus(this.position, this.interfaceType),
        api.getStreamerStatus(this.position, this.interfaceType),
      ]);
      this.tunerStatus = tuner.status === 'fulfilled' ? tuner.value : null;
      this.streamerStatus = streamer.status === 'fulfilled' ? streamer.value : null;
    } catch { /* ignore */ }
    this.loading = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
  }

  _goBack() {
    window.location.hash = '#interfaces';
  }

  _refresh() {
    this.loading = true;
    this._loadStatus();
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        </div>
      `;
    }

    const signalStrength = this.tunerStatus?.signal_strength ?? 0;
    const locked = this.tunerStatus?.locked ?? false;

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <button @click=${this._goBack}
              class="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">← Back</button>
            <div>
              <h1 class="text-2xl font-bold text-slate-800">Interface Status ${this.position}</h1>
              <p class="text-slate-500 text-sm">Type: <span class="font-mono">${this.interfaceType || 'Unknown'}</span> · Auto-refreshes every 5s</p>
            </div>
          </div>
          <button @click=${this._refresh}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Tuner Status -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
              <h2 class="text-sm font-semibold text-slate-700">Tuner Status</h2>
            </div>
            ${this.tunerStatus ? html`
              <div class="p-6 space-y-5">
                <!-- Locked indicator -->
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-slate-700">Lock Status</span>
                  <span class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold
                    ${locked ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                    <span class="h-2 w-2 rounded-full ${locked ? 'bg-green-500' : 'bg-red-500'}"></span>
                    ${locked ? 'Locked' : 'Unlocked'}
                  </span>
                </div>

                <!-- Signal Strength Bar -->
                <div>
                  <div class="flex items-center justify-between mb-1">
                    <span class="text-sm font-medium text-slate-700">Signal Strength</span>
                    <span class="text-sm font-mono text-slate-600">${signalStrength}%</span>
                  </div>
                  <div class="w-full h-3 bg-slate-200 rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all duration-500
                        ${signalStrength > 70 ? 'bg-green-500' : signalStrength > 40 ? 'bg-yellow-500' : 'bg-red-500'}"
                      style="width: ${Math.min(100, Math.max(0, signalStrength))}%"
                    ></div>
                  </div>
                </div>

                <!-- SNR -->
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-slate-700">SNR</span>
                  <span class="text-sm font-mono text-slate-600">${this.tunerStatus.snr ?? '—'}</span>
                </div>

                <!-- BER -->
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-slate-700">BER</span>
                  <span class="text-sm font-mono text-slate-600">${this.tunerStatus.ber ?? '—'}</span>
                </div>
              </div>
            ` : html`
              <div class="p-8 text-center text-slate-400">No tuner status available</div>
            `}
          </div>

          <!-- Streamer Status -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
              <h2 class="text-sm font-semibold text-slate-700">Streamer Status</h2>
            </div>
            ${this.streamerStatus ? html`
              <div class="p-6 space-y-5">
                <!-- Status badge -->
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-slate-700">Status</span>
                  <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold
                    ${this.streamerStatus.status === 'running' ? 'bg-green-100 text-green-800' :
                      this.streamerStatus.status === 'stopped' ? 'bg-slate-100 text-slate-800' :
                      'bg-yellow-100 text-yellow-800'}">
                    ${this.streamerStatus.status || 'Unknown'}
                  </span>
                </div>

                <!-- Bitrate -->
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-slate-700">Bitrate</span>
                  <span class="text-sm font-mono text-slate-600">${this.streamerStatus.bitrate ?? '—'}</span>
                </div>

                <!-- PID -->
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium text-slate-700">PID</span>
                  <span class="text-sm font-mono text-slate-600">${this.streamerStatus.pid ?? '—'}</span>
                </div>
              </div>
            ` : html`
              <div class="p-8 text-center text-slate-400">No streamer status available</div>
            `}
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-interface-status', IxuiInterfaceStatus);
