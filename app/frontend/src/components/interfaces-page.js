import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiInterfaces extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    interfaces: { type: Array },
    interfaceTypes: { type: Array },
    activeFilter: { type: String },
    loading: { type: Boolean },
    expandedPos: { type: String },
    selectedConfig: { type: Object },
    services: { type: Array },
    scanTime: { type: String },
    scanning: { type: Boolean },
    applying: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.interfaces = [];
    this.interfaceTypes = [];
    this.activeFilter = 'All';
    this.loading = true;
    this.expandedPos = null;
    this.selectedConfig = null;
    this.services = [];
    this.scanTime = null;
    this.scanning = false;
    this.applying = false;
    this.notification = null;
    this._refreshInterval = null;
    this._notificationTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadPage();
    this._refreshInterval = window.setInterval(() => {
      this._refreshInterfaces(false);
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

  async _loadPage() {
    this.loading = true;
    try {
      const [types] = await Promise.all([
        this._loadInterfaceTypes(),
        this._refreshInterfaces(false),
      ]);
      this.interfaceTypes = types;
    } finally {
      this.loading = false;
    }
  }

  async _loadInterfaceTypes() {
    try {
      const types = await api.getInterfaceTypes();
      return Array.isArray(types) ? types : [];
    } catch {
      return [];
    }
  }

  async _refreshInterfaces(showSpinner = false) {
    if (showSpinner) {
      this.loading = true;
    }

    try {
      this.interfaces = await api.getInterfaces() || [];
      this._ensureActiveFilter();

      if (this.expandedPos) {
        const expandedInterface = this.interfaces.find((iface) => iface.position === this.expandedPos);
        if (expandedInterface) {
          await this._loadExpandedDetails(expandedInterface, false);
        } else {
          this.expandedPos = null;
          this.selectedConfig = null;
          this.services = [];
          this.scanTime = null;
        }
      }
    } catch {
      this.interfaces = [];
    }

    if (showSpinner) {
      this.loading = false;
    }
  }

  _translateType(type) {
    switch ((type || '').toLowerCase()) {
      case 'dvbudp':
        return 'udp';
      case 'dvbs':
        return 'satellite';
      case 'dvbt':
        return 'terrestrial';
      case 'dvbc':
        return 'cable';
      case 'infostreamer':
        return 'infotv';
      case 'mod':
        return 'modulator';
      case 'dsc':
        return 'descrambler';
      case 'hdmi2ip':
      case 'dvbhdmi':
        return 'hdmi';
      case 'hls2ip':
        return 'hls';
      case 'webradio':
        return 'webradio';
      case 'infoch':
        return 'infochannel';
      default:
        return type || 'unknown';
    }
  }

  _filterOptions() {
    const labels = (this.interfaceTypes || []).map((type) => this._translateType(type));
    return ['All', ...new Set(labels)];
  }

  _ensureActiveFilter() {
    if (!this._filterOptions().includes(this.activeFilter)) {
      this.activeFilter = 'All';
    }
  }

  _filteredInterfaces() {
    if (this.activeFilter === 'All') {
      return this.interfaces;
    }
    return this.interfaces.filter((iface) => this._translateType(iface.type) === this.activeFilter);
  }

  _supportsConfig(interfaceType) {
    return [
      'dvbudp',
      'dvbs',
      'dvbt',
      'dvbc',
      'dsc',
      'infostreamer',
      'dvbhdmi',
      'hdmi2ip',
      'hls2ip',
      'webradio',
      'infoch',
    ].includes((interfaceType || '').toLowerCase());
  }

  _usesInfochFlow(interfaceType) {
    return (interfaceType || '').toLowerCase() === 'infoch';
  }

  _supportsApply(interfaceType) {
    return this._supportsConfig(interfaceType) && !this._usesInfochFlow(interfaceType);
  }

  _supportsScan(interfaceType) {
    const type = (interfaceType || '').toLowerCase();
    return this._supportsConfig(type) && !type.includes('hdmi') && !type.includes('asi');
  }

  async _loadExpandedDetails(iface, resetState = true) {
    if (resetState) {
      this.selectedConfig = null;
      this.services = [];
      this.scanTime = null;
    }

    try {
      const configPromise = this._supportsConfig(iface.type)
        ? (this._usesInfochFlow(iface.type)
          ? api.getInterfaceInfoch(iface.position)
          : api.getInterfaceConfig(iface.position, iface.type))
        : Promise.resolve(null);

      const [config, services, scanTime] = await Promise.allSettled([
        configPromise,
        api.getServices(iface.position),
        api.getInterfaceScanTime(iface.position),
      ]);

      this.selectedConfig = config.status === 'fulfilled' ? config.value : null;
      this.services = services.status === 'fulfilled' ? (services.value || []) : [];
      this.scanTime = scanTime.status === 'fulfilled' ? (scanTime.value?.scan_time || null) : null;
    } catch {
      this.selectedConfig = null;
      this.services = [];
      this.scanTime = null;
    }
  }

  async _toggleExpand(iface) {
    if (this.expandedPos === iface.position) {
      this.expandedPos = null;
      this.selectedConfig = null;
      this.services = [];
      this.scanTime = null;
      return;
    }

    this.expandedPos = iface.position;
    await this._loadExpandedDetails(iface);
  }

  async _startScan(pos) {
    this.scanning = true;
    try {
      await api.startScan(pos);
      this._showNotification('Scan started successfully', 'success');
      if (this.expandedPos === pos) {
        const [services, scanTime] = await Promise.allSettled([
          api.getInterfaceScanResult(pos),
          api.getInterfaceScanTime(pos),
        ]);
        this.services = services.status === 'fulfilled' ? (services.value || []) : this.services;
        this.scanTime = scanTime.status === 'fulfilled' ? (scanTime.value?.scan_time || null) : this.scanTime;
      }
    } catch (err) {
      this._showNotification('Failed to start scan', 'error');
    }
    this.scanning = false;
  }

  async _applyInterface(iface) {
    this.applying = true;
    try {
      await api.applyInterface(iface.position, iface.type);
      this._showNotification('Interface applied successfully', 'success');
      await this._loadInterfaces();
    } catch {
      this._showNotification('Failed to apply interface configuration', 'error');
    }
    this.applying = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
    }
    this._notificationTimer = window.setTimeout(() => { this.notification = null; }, 3000);
  }

  _setFilter(filter) {
    this.activeFilter = filter;
  }

  _rowClasses(iface) {
    const classes = ['hover:bg-blue-50/50', 'cursor-pointer', 'transition-colors'];
    if (this.expandedPos === iface.position) {
      classes.push('bg-blue-50');
    }
    if (String(iface.status || '').toLowerCase() === 'error') {
      classes.push('bg-red-50/80');
    }
    return classes.join(' ');
  }

  _showsEmmIndicator(iface) {
    return Boolean(iface?.emm) && String(iface?.type || '').toLowerCase() !== 'dsc';
  }

  _goToInterfaceStatus(event, iface) {
    event.stopPropagation();
    window.location.hash = `#interface-status/${iface.position}/${iface.type}`;
  }

  _goToInterfaceConfig(event, iface) {
    event.stopPropagation();
    window.location.hash = `#interface-edit/${iface.position}/${iface.type}`;
  }

  _goToInterfaceLog(event, iface) {
    event.stopPropagation();
    window.location.hash = `#interface-log/${iface.position}`;
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
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Interfaces</h1>
            <p class="text-slate-500 text-sm">Manage and monitor interfaces · auto-refreshes every 5s</p>
          </div>
          <button
            @click=${this._loadPage}
            class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
          >↻ Refresh</button>
        </div>

        <div class="flex flex-wrap gap-2">
          ${this._filterOptions().map((filter) => html`
            <button
              @click=${() => this._setFilter(filter)}
              class="rounded-full px-3 py-1.5 text-sm font-medium transition ${this.activeFilter === filter
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'}"
            >${filter}</button>
          `)}
        </div>

        <!-- Notification -->
        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Table -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200">
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Position</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                <th class="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">Config</th>
                <th class="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">Log</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              ${this._filteredInterfaces().map(iface => html`
                <tr
                  @click=${() => this._toggleExpand(iface)}
                  class=${this._rowClasses(iface)}
                >
                  <td class="px-6 py-4 text-sm font-mono text-slate-700">
                    <button
                      @click=${(event) => this._goToInterfaceStatus(event, iface)}
                      class="inline-flex items-center gap-2 text-sm font-mono text-blue-700 transition hover:text-blue-900 hover:underline"
                    >
                      <span>${iface.position}</span>
                      ${iface.active ? html`
                        <span
                          class="inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-[10px] text-emerald-700"
                          title="Active"
                        >▶</span>
                      ` : ''}
                      ${this._showsEmmIndicator(iface) ? html`
                        <img
                          src="/static/images/emm.png"
                          alt="EMM"
                          title="EMM enabled"
                          class="h-4 w-4 shrink-0"
                        />
                      ` : ''}
                    </button>
                  </td>
                  <td class="px-6 py-4">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      ${iface.multi_band ? 'M / ' : ''}${this._translateType(iface.type)}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm font-medium text-slate-800">${iface.name || '—'}</td>
                  <td class="px-6 py-4 text-sm text-slate-600">${iface.status || 'Unknown'}</td>
                  <td class="px-6 py-4 text-center">
                    ${this._supportsConfig(iface.type) ? html`
                      <button
                        @click=${(event) => this._goToInterfaceConfig(event, iface)}
                        class="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-blue-400 hover:text-blue-800"
                      >Config</button>
                    ` : html`<span class="text-sm text-slate-300">-</span>`}
                  </td>
                  <td class="px-6 py-4 text-center">
                    <button
                      @click=${(event) => this._goToInterfaceLog(event, iface)}
                      class="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:border-blue-400 hover:text-blue-800"
                    >Log</button>
                  </td>
                </tr>

                <!-- Expanded Detail Panel -->
                ${this.expandedPos === iface.position ? html`
                  <tr>
                    <td colspan="6" class="px-6 py-6 bg-slate-50 border-t border-slate-200">
                      <div class="space-y-6">
                        <!-- Action buttons -->
                        <div class="flex flex-wrap gap-3">
                          ${this._supportsConfig(iface.type) ? html`
                            <button
                              @click=${(e) => { e.stopPropagation(); window.location.hash = `#interface-edit/${iface.position}/${iface.type}`; }}
                              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                            >✏️ Edit Config</button>
                          ` : ''}
                          <button
                            @click=${(e) => { e.stopPropagation(); window.location.hash = `#interface-status/${iface.position}/${iface.type}`; }}
                            class="px-4 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
                          >📡 Status</button>
                          <button
                            @click=${(e) => { e.stopPropagation(); window.location.hash = `#interface-log/${iface.position}`; }}
                            class="px-4 py-2 text-sm bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition"
                          >📋 Log</button>
                          ${this._supportsApply(iface.type) ? html`
                            <button
                              @click=${(e) => { e.stopPropagation(); this._applyInterface(iface); }}
                              class="px-4 py-2 text-sm bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition disabled:opacity-50"
                              ?disabled=${this.applying}
                            >${this.applying ? 'Applying...' : '🛠 Apply'}</button>
                          ` : ''}
                          ${this._supportsScan(iface.type) ? html`
                            <button
                              @click=${(e) => { e.stopPropagation(); this._startScan(iface.position); }}
                              class="px-4 py-2 text-sm bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition disabled:opacity-50"
                              ?disabled=${this.scanning}
                            >${this.scanning ? 'Scanning...' : '🔍 Start Scan'}</button>
                          ` : ''}
                        </div>

                        ${this.scanTime ? html`
                          <div class="rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">
                            <span class="font-medium text-slate-700">Last Scan:</span>
                            <span class="ml-2 font-mono">${this.scanTime}</span>
                          </div>
                        ` : ''}

                        <!-- Config Details -->
                        ${this.selectedConfig ? html`
                          <div>
                            <h4 class="text-sm font-semibold text-slate-700 mb-3">Configuration</h4>
                            <div class="bg-white rounded-lg border border-slate-200 p-4">
                              <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                                ${Object.entries(this.selectedConfig).map(([key, val]) => html`
                                  <div class="text-sm">
                                    <span class="text-slate-500">${key}:</span>
                                    <span class="ml-1 font-mono text-slate-800">${typeof val === 'object' ? JSON.stringify(val) : String(val)}</span>
                                  </div>
                                `)}
                              </div>
                            </div>
                          </div>
                        ` : ''}

                        <!-- Services -->
                        ${this.services.length > 0 ? html`
                          <div>
                            <h4 class="text-sm font-semibold text-slate-700 mb-3">Services (${this.services.length})</h4>
                            <div class="bg-white rounded-lg border border-slate-200 overflow-hidden">
                              <table class="w-full text-sm">
                                <thead>
                                  <tr class="bg-slate-100">
                                    <th class="px-4 py-2 text-left text-xs font-semibold text-slate-600">Name</th>
                                    <th class="px-4 py-2 text-left text-xs font-semibold text-slate-600">Type</th>
                                    <th class="px-4 py-2 text-left text-xs font-semibold text-slate-600">ID</th>
                                  </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-100">
                                  ${this.services.map(svc => html`
                                    <tr class="hover:bg-slate-50">
                                      <td class="px-4 py-2 text-slate-800">${svc.name || svc.service_name || '—'}</td>
                                      <td class="px-4 py-2 text-slate-600">${svc.type || svc.service_type || '—'}</td>
                                      <td class="px-4 py-2 font-mono text-slate-600">${svc.id || svc.service_id || '—'}</td>
                                    </tr>
                                  `)}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        ` : html`
                          <p class="text-sm text-slate-400">No services found for this interface</p>
                        `}
                      </div>
                    </td>
                  </tr>
                ` : ''}
              `)}
            </tbody>
          </table>

          ${this._filteredInterfaces().length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No interfaces found</div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-interfaces', IxuiInterfaces);
