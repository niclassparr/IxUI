import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiInterfaceStatus extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    position: { type: String },
    interfaceType: { type: String },
    interfaceInfo: { type: Object },
    tunerStatus: { type: Object },
    streamerStatus: { type: Object },
    scanTime: { type: String },
    loading: { type: Boolean },
    busyCommand: { type: String },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.position = '';
    this.interfaceType = '';
    this.interfaceInfo = null;
    this.tunerStatus = null;
    this.streamerStatus = null;
    this.scanTime = null;
    this.loading = true;
    this.busyCommand = null;
    this.notification = null;
    this._refreshInterval = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._startAutoRefresh();
    this._loadStatus();
  }

  updated(changedProperties) {
    if ((changedProperties.has('position') || changedProperties.has('interfaceType')) && this.position) {
      this.loading = true;
      this._startAutoRefresh();
      this._loadStatus();
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._refreshInterval) {
      clearInterval(this._refreshInterval);
      this._refreshInterval = null;
    }
  }

  _startAutoRefresh() {
    if (this._refreshInterval) {
      clearInterval(this._refreshInterval);
    }
    if (!this.position) {
      this._refreshInterval = null;
      return;
    }
    this._refreshInterval = setInterval(() => this._loadStatus(), 5000);
  }

  async _loadStatus() {
    if (!this.position) {
      this.loading = false;
      return;
    }

    try {
      const [interfaceInfo, tuner, streamer, scanTime] = await Promise.allSettled([
        api.getInterface(this.position),
        api.getTunerStatus(this.position, this.interfaceType),
        api.getStreamerStatus(this.position, this.interfaceType),
        api.getInterfaceScanTime(this.position),
      ]);
      this.interfaceInfo = interfaceInfo.status === 'fulfilled' ? interfaceInfo.value : null;
      this.tunerStatus = tuner.status === 'fulfilled' ? tuner.value : null;
      this.streamerStatus = streamer.status === 'fulfilled' ? streamer.value : null;
      this.scanTime = scanTime.status === 'fulfilled' ? (scanTime.value?.scan_time || null) : null;
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

  async _runCommand(command) {
    this.busyCommand = command;
    try {
      const response = await api.runInterfaceCommand(this.position, command);
      if (!response?.success) {
        throw new Error(response?.error || `Command "${command}" failed`);
      }

      const message = this._commandSuccessMessage(command);
      if (message) {
        this._showNotification(message, 'success');
      }
      await this._loadStatus();
    } catch (error) {
      this._showNotification(error.message || `Command "${command}" failed`, 'error');
    }
    this.busyCommand = null;
  }

  _commandSuccessMessage(command) {
    if (command === 'stream') return 'Streamer started';
    if (command === 'stop') return 'Streamer stopped';
    if (command === 'restart') return 'Streamer restarted';
    if (command === 'mmi_open') return 'CI menu opened';
    if (command === 'mmi_close') return 'CI menu closed';
    if (command.startsWith('mmi_answer')) return 'CI menu action completed';
    return `Command "${command}" executed successfully`;
  }

  _type() {
    return (this.interfaceType || this.interfaceInfo?.type || '').toLowerCase();
  }

  _isDsc() {
    return this._type() === 'dsc';
  }

  _isInfoChannel() {
    return this._type() === 'infoch';
  }

  _isAdaptiveStream() {
    return this._type() === 'hls2ip';
  }

  _isWebradio() {
    return this._type() === 'webradio';
  }

  _isMuxType() {
    return this._type() === 'dsc' || this._type() === 'mod';
  }

  _showDiscontinuity() {
    return !['infostreamer', 'hdmi2ip'].includes(this._type());
  }

  _signalRaw() {
    return Number(this.tunerStatus?.signal_strength || 0);
  }

  _signalPercent() {
    const raw = this._signalRaw();
    if (raw <= 0) return 0;
    return Math.max(0, Math.min(100, raw > 100 ? Math.round(raw / 100) : raw));
  }

  _formatSignal() {
    const raw = this._signalRaw();
    if (!raw) return '0.00 dBuV';
    return `${(raw / 100).toFixed(2)} dBuV`;
  }

  _formatSnr() {
    const snr = Number(this.tunerStatus?.snr || 0);
    if (!snr) return '0.00 dB';
    if (snr >= 10000) {
      return `${((snr * 100) / 65535).toFixed(2)} %`;
    }
    return `${(snr / 100).toFixed(2)} dB`;
  }

  _formatBitrate(value) {
    const bitrate = Number(value || 0);
    return `${(bitrate / 1000000).toFixed(2)} Mbps`;
  }

  _formatBuffer(value) {
    const buffer = Number(value || 0);
    return `${buffer} ms`;
  }

  _camState() {
    const ciStatus = Number(this.tunerStatus?.ci_status || 0);
    if (ciStatus & 4) {
      return {
        label: 'Decrypting',
        badgeClass: 'bg-emerald-100 text-emerald-800 border border-emerald-200',
      };
    }
    if (ciStatus & 2) {
      return {
        label: 'CAM Detected',
        badgeClass: 'bg-amber-100 text-amber-800 border border-amber-200',
      };
    }
    return {
      label: 'No CAM',
      badgeClass: 'bg-slate-100 text-slate-700 border border-slate-200',
    };
  }

  _statusBadgeClass(status) {
    if (status === 'running' || status === 'locked' || status === 'connected') {
      return 'bg-emerald-100 text-emerald-800 border border-emerald-200';
    }
    if (status === 'powered off') {
      return 'bg-rose-100 text-rose-800 border border-rose-200';
    }
    if (status === 'disabled' || status === 'stopped' || status === 'idle') {
      return 'bg-slate-100 text-slate-700 border border-slate-200';
    }
    return 'bg-amber-100 text-amber-800 border border-amber-200';
  }

  _renderValueRow(label, value) {
    return html`
      <div class="flex items-center justify-between gap-4">
        <span class="text-sm font-medium text-slate-600">${label}</span>
        <span class="text-sm font-mono text-slate-800 text-right">${value}</span>
      </div>
    `;
  }

  _renderNotification() {
    if (!this.notification) {
      return html``;
    }

    return html`
      <div class="p-3 rounded-lg text-sm font-medium
        ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
        ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
        ${this.notification.message}
      </div>
    `;
  }

  _renderRuntimeCard() {
    const ciOpen = Boolean(this.tunerStatus?.ci_menu_open);
    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between gap-4">
          <div>
            <h2 class="text-sm font-semibold text-slate-700">Runtime Control</h2>
            <p class="mt-1 text-sm text-slate-500">Runtime status: <span class="font-mono text-slate-700">${this.interfaceInfo?.status || 'unknown'}</span></p>
          </div>
          <div class="text-sm text-slate-500">Last scan: <span class="font-mono text-slate-700">${this.scanTime || 'Never'}</span></div>
        </div>
        <div class="p-6 flex flex-wrap gap-3">
          <button
            @click=${() => this._runCommand('stream')}
            class="px-4 py-2 text-sm bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition disabled:opacity-50"
            ?disabled=${this.busyCommand != null}
          >${this.busyCommand === 'stream' ? 'Starting...' : 'Start'}</button>
          <button
            @click=${() => this._runCommand('stop')}
            class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 transition disabled:opacity-50"
            ?disabled=${this.busyCommand != null}
          >${this.busyCommand === 'stop' ? 'Stopping...' : 'Stop'}</button>
          <button
            @click=${() => this._runCommand('restart')}
            class="px-4 py-2 text-sm bg-amber-500 text-white rounded-lg hover:bg-amber-600 transition disabled:opacity-50"
            ?disabled=${this.busyCommand != null}
          >${this.busyCommand === 'restart' ? 'Restarting...' : 'Restart'}</button>
          ${this._isDsc() ? html`
            <button
              @click=${() => this._runCommand(ciOpen ? 'mmi_close' : 'mmi_open')}
              class="px-4 py-2 text-sm bg-sky-600 text-white rounded-lg hover:bg-sky-700 transition disabled:opacity-50"
              ?disabled=${this.busyCommand != null}
            >${this.busyCommand?.startsWith('mmi_') ? 'Updating CI...' : (ciOpen ? 'Close CI Menu' : 'Open CI Menu')}</button>
          ` : ''}
          <button
            @click=${() => { window.location.hash = `#interface-log/${this.position}`; }}
            class="px-4 py-2 text-sm bg-slate-700 text-white rounded-lg hover:bg-slate-800 transition"
          >View Log</button>
        </div>
      </div>
    `;
  }

  _renderInterfaceCard() {
    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 class="text-sm font-semibold text-slate-700">Interface</h2>
        </div>
        <div class="p-6 space-y-4">
          ${this._renderValueRow('Name', this.interfaceInfo?.name || '-')}
          ${this._renderValueRow('Type', this.interfaceType || this.interfaceInfo?.type || '-')}
          <div class="flex items-center justify-between gap-4">
            <span class="text-sm font-medium text-slate-600">Status</span>
            <span class="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${this._statusBadgeClass(this.interfaceInfo?.status || 'unknown')}">
              ${this.interfaceInfo?.status || 'unknown'}
            </span>
          </div>
          ${this._renderValueRow('Auto Refresh', '5 seconds')}
        </div>
      </div>
    `;
  }

  _renderTunerCard() {
    if (!this.tunerStatus) {
      return html`
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
            <h2 class="text-sm font-semibold text-slate-700">Tuner Status</h2>
          </div>
          <div class="p-8 text-center text-slate-400">No tuner status available</div>
        </div>
      `;
    }

    if (this._isDsc()) {
      const camState = this._camState();
      return html`
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
            <h2 class="text-sm font-semibold text-slate-700">CI Status</h2>
          </div>
          <div class="p-6 space-y-4">
            <div class="flex items-center justify-between gap-4">
              <span class="text-sm font-medium text-slate-600">CAM State</span>
              <span class="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${camState.badgeClass}">
                ${camState.label}
              </span>
            </div>
            ${this._renderValueRow('Receiving EMM', this.tunerStatus.ca_emm ? 'true' : 'false')}
            ${this._renderValueRow('CI Text', this.tunerStatus.ca_text || '-')}
            ${this._renderValueRow('CI Message', this.tunerStatus.ca_osd || '-')}
          </div>
        </div>
      `;
    }

    const signalStrength = this._signalPercent();
    const locked = Boolean(this.tunerStatus?.locked);
    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 class="text-sm font-semibold text-slate-700">Tuner Status</h2>
        </div>
        <div class="p-6 space-y-5">
          <div class="flex items-center justify-between gap-4">
            <span class="text-sm font-medium text-slate-700">Lock Status</span>
            <span class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold ${locked ? 'bg-green-100 text-green-800 border border-green-200' : 'bg-red-100 text-red-800 border border-red-200'}">
              <span class="h-2 w-2 rounded-full ${locked ? 'bg-green-500' : 'bg-red-500'}"></span>
              ${locked ? 'Locked' : 'Unlocked'}
            </span>
          </div>

          <div>
            <div class="flex items-center justify-between mb-1 gap-4">
              <span class="text-sm font-medium text-slate-700">Signal</span>
              <span class="text-sm font-mono text-slate-600">${this._formatSignal()}</span>
            </div>
            <div class="w-full h-3 bg-slate-200 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500 ${signalStrength > 70 ? 'bg-green-500' : signalStrength > 40 ? 'bg-yellow-500' : 'bg-red-500'}"
                style="width: ${signalStrength}%"
              ></div>
            </div>
          </div>

          ${this._renderValueRow('Frequency', this.tunerStatus.frequency ? String(this.tunerStatus.frequency) : '-')}
          ${this._renderValueRow('CNR', this._formatSnr())}
          ${this._renderValueRow('BER', this.tunerStatus.ber ?? '-')}
        </div>
      </div>
    `;
  }

  _renderStreamerSummaryCard() {
    const status = this.streamerStatus?.status || 'unknown';
    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 class="text-sm font-semibold text-slate-700">Streamer Status</h2>
        </div>
        <div class="p-6 space-y-4">
          <div class="flex items-center justify-between gap-4">
            <span class="text-sm font-medium text-slate-700">Status</span>
            <span class="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${this._statusBadgeClass(status)}">${status}</span>
          </div>
          ${this._renderValueRow('Total Bitrate', this._formatBitrate(this.streamerStatus?.bitrate || 0))}
          ${this._renderValueRow('PID', this.streamerStatus?.pid ?? '-')}
          ${this._isMuxType() ? this._renderValueRow('Multiplex Usage', `${this.streamerStatus?.mux_load || 0} (max ${this.streamerStatus?.max_mux_load || 0}) %`) : ''}
          ${this._isDsc() ? this._renderValueRow('Descrambler Usage', `${this.streamerStatus?.ca_services || 0} service(s) and ${this.streamerStatus?.ca_pids || 0} pids`) : ''}
        </div>
      </div>
    `;
  }

  _renderCiMenuCard() {
    if (!this._isDsc()) {
      return html``;
    }

    const isOpen = Boolean(this.tunerStatus?.ci_menu_open);
    const items = this.tunerStatus?.menu_items || [];
    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden xl:col-span-2">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between gap-4">
          <div>
            <h2 class="text-sm font-semibold text-slate-700">CI Menu</h2>
            <p class="mt-1 text-sm text-slate-500">${isOpen ? (this.tunerStatus?.menu_title || 'Common Interface') : 'The CI menu is currently closed.'}</p>
          </div>
          <span class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">DSC</span>
        </div>
        ${isOpen ? html`
          <div class="p-6 space-y-4">
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              ${this.tunerStatus?.ca_osd || 'No active CI message.'}
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              ${items.map(item => html`
                <button
                  @click=${() => this._runCommand(`mmi_answer ${item.id}`)}
                  class="w-full rounded-lg border border-slate-300 bg-white px-4 py-3 text-left text-sm font-medium text-slate-700 hover:border-sky-400 hover:text-sky-700 transition disabled:opacity-50"
                  ?disabled=${this.busyCommand != null}
                >${item.label}</button>
              `)}
            </div>
          </div>
        ` : html`
          <div class="p-8 text-center text-slate-400">Open the CI menu to interact with the descrambler module.</div>
        `}
      </div>
    `;
  }

  _renderNameCell(row) {
    const showScramble = !['mod', 'infostreamer', 'dvbhdmi', 'hdmi2ip'].includes(this._type());
    if (!showScramble) {
      return html`<span class="font-medium text-slate-800">${row.name || '-'}</span>`;
    }

    return html`
      <div class="flex items-center gap-3">
        <span class="font-medium text-slate-800">${row.name || '-'}</span>
        <span class="inline-flex rounded-full px-2 py-0.5 text-[11px] font-semibold ${row.scrambled ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-700'}">
          ${row.scrambled ? 'Scrambled' : 'Clear'}
        </span>
      </div>
    `;
  }

  _renderServiceTable() {
    const rows = this.streamerStatus?.services || [];
    if (!rows.length) {
      return html`
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden xl:col-span-2">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
            <h2 class="text-sm font-semibold text-slate-700">Service Status</h2>
          </div>
          <div class="p-8 text-center text-slate-400">No runtime service statistics are available for this interface.</div>
        </div>
      `;
    }

    const totalBitrate = rows.reduce((sum, row) => sum + Number(row.bitrate || 0), 0);

    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden xl:col-span-2">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between gap-4">
          <h2 class="text-sm font-semibold text-slate-700">Service Status</h2>
          <span class="text-sm text-slate-500">${rows.length} channel(s)</span>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-slate-200 text-sm">
            <thead class="bg-slate-50 text-slate-600">
              ${this._isInfoChannel() ? html`
                <tr>
                  <th class="px-4 py-3 text-left font-semibold">Name</th>
                  <th class="px-4 py-3 text-left font-semibold">Bitrate</th>
                </tr>
              ` : this._isAdaptiveStream() ? html`
                <tr>
                  <th class="px-4 py-3 text-left font-semibold">Name</th>
                  <th class="px-4 py-3 text-left font-semibold">Download Bitrate</th>
                  <th class="px-4 py-3 text-left font-semibold">Selected Bitrate</th>
                  <th class="px-4 py-3 text-left font-semibold">Segment Counter</th>
                  <th class="px-4 py-3 text-left font-semibold">Stream Switches</th>
                  <th class="px-4 py-3 text-left font-semibold">Segments Missed</th>
                  <th class="px-4 py-3 text-left font-semibold">Bitrate</th>
                  <th class="px-4 py-3 text-left font-semibold">Buffer Level</th>
                </tr>
              ` : this._isWebradio() ? html`
                <tr>
                  <th class="px-4 py-3 text-left font-semibold">Name</th>
                  <th class="px-4 py-3 text-left font-semibold">Bitrate</th>
                  <th class="px-4 py-3 text-left font-semibold">Buffer Level</th>
                </tr>
              ` : html`
                <tr>
                  <th class="px-4 py-3 text-left font-semibold">Name</th>
                  <th class="px-4 py-3 text-left font-semibold">Bitrate</th>
                  ${this._showDiscontinuity() ? html`<th class="px-4 py-3 text-left font-semibold">Discontinuity</th>` : ''}
                  ${this._isMuxType() ? html`<th class="px-4 py-3 text-left font-semibold">Mux Load</th>` : ''}
                </tr>
              `}
            </thead>
            <tbody class="divide-y divide-slate-100 bg-white text-slate-700">
              ${rows.map(row => html`
                ${this._isInfoChannel() ? html`
                  <tr>
                    <td class="px-4 py-3">${row.name || '-'}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBitrate(row.bitrate)}</td>
                  </tr>
                ` : this._isAdaptiveStream() ? html`
                  <tr>
                    <td class="px-4 py-3">${row.name || '-'}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBitrate(row.download_bitrate)}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBitrate(row.selected_bitrate)}</td>
                    <td class="px-4 py-3 font-mono">${row.segment_counter ?? 0}</td>
                    <td class="px-4 py-3 font-mono">${row.num_stream_switches ?? 0}</td>
                    <td class="px-4 py-3 font-mono">${row.num_segments_missed ?? 0}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBitrate(row.bitrate)}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBuffer(row.buffer_level)}</td>
                  </tr>
                ` : this._isWebradio() ? html`
                  <tr>
                    <td class="px-4 py-3">${row.name || '-'}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBitrate(row.bitrate)}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBuffer(row.buffer_level)}</td>
                  </tr>
                ` : html`
                  <tr>
                    <td class="px-4 py-3">${this._renderNameCell(row)}</td>
                    <td class="px-4 py-3 font-mono">${this._formatBitrate(row.bitrate)}</td>
                    ${this._showDiscontinuity() ? html`<td class="px-4 py-3 font-mono">${row.discontinuity ?? 0}</td>` : ''}
                    ${this._isMuxType() ? html`<td class="px-4 py-3 font-mono">${row.mux_load || 0} (max ${row.max_mux_load || 0}) %</td>` : ''}
                  </tr>
                `}
              `)}
            </tbody>
            <tfoot class="bg-slate-50 text-slate-600">
              <tr>
                <td class="px-4 py-3 font-semibold">${rows.length} channel(s)</td>
                <td class="px-4 py-3 font-mono font-semibold">${this._formatBitrate(totalBitrate)}</td>
                ${this._showDiscontinuity() && !this._isAdaptiveStream() && !this._isWebradio() && !this._isInfoChannel() ? html`<td class="px-4 py-3"></td>` : ''}
                ${this._isMuxType() ? html`<td class="px-4 py-3"></td>` : ''}
              </tr>
            </tfoot>
          </table>
        </div>
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
          <div class="flex items-center gap-4">
            <button @click=${this._goBack}
              class="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">Back</button>
            <div>
              <h1 class="text-2xl font-bold text-slate-800">Interface Status ${this.position}</h1>
              <p class="text-slate-500 text-sm">Type: <span class="font-mono">${this.interfaceType || 'Unknown'}</span> · Auto-refreshes every 5s</p>
            </div>
          </div>
          <button @click=${this._refresh}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">Refresh</button>
        </div>

        ${this._renderNotification()}

        <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
          ${this._renderRuntimeCard()}
          ${this._renderInterfaceCard()}
          ${this._renderTunerCard()}
          ${this._renderStreamerSummaryCard()}
          ${this._renderCiMenuCard()}
          ${this._renderServiceTable()}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-interface-status', IxuiInterfaceStatus);
