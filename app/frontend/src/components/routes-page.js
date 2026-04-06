import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiRoutes extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    routes: { type: Array },
    interfaces: { type: Array },
    features: { type: Object },
    settings: { type: Object },
    loading: { type: Boolean },
    saving: { type: Boolean },
    sortField: { type: String },
    sortDir: { type: String },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.routes = [];
    this.interfaces = [];
    this.features = {};
    this.settings = {};
    this.loading = true;
    this.saving = false;
    this.sortField = 'service_output';
    this.sortDir = 'asc';
    this.notification = null;
    this._initialSnapshot = '[]';
    this._notificationTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadRoutes();
  }

  disconnectedCallback() {
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
      this._notificationTimer = null;
    }
    super.disconnectedCallback();
  }

  async _loadRoutes() {
    this.loading = true;
    try {
      const [routes, interfaces, settings] = await Promise.allSettled([
        api.getRoutes(),
        api.getInterfaces(),
        api.getSettings(),
      ]);

      this.routes = routes.status === 'fulfilled' ? (routes.value || []) : [];
      this.interfaces = interfaces.status === 'fulfilled' ? (interfaces.value || []) : [];
      this.settings = this._indexSettings(settings.status === 'fulfilled' ? (settings.value || []) : []);
      this.features = {
        dvbc: this._settingEnabled('dvbc_enable'),
        dvbc_net2: this._settingEnabled('dvbc_net2_enable'),
        ip: this._settingEnabled('ip_enable'),
        hls: this._settingEnabled('hls_enable'),
        portal: this._settingEnabled('portal_enable'),
      };
      this._initialSnapshot = JSON.stringify(this.routes);
    } catch {
      this.routes = [];
      this.interfaces = [];
      this.settings = {};
      this.features = {};
    }
    this.loading = false;
  }

  _indexSettings(settings) {
    return Object.fromEntries((settings || []).map((setting) => [setting.name, setting]));
  }

  _featureEnabled(name) {
    return Boolean(this.features?.[name]);
  }

  _settingValue(name, fallback = '') {
    return this.settings?.[name]?.value ?? fallback;
  }

  _settingEnabled(name) {
    return String(this._settingValue(name, 'false')).toLowerCase() === 'true';
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
    }
    this._notificationTimer = window.setTimeout(() => {
      this.notification = null;
    }, 4000);
  }

  _columns() {
    const columns = [
      { key: 'service_output', label: 'Service' },
      { key: 'interface_pos', label: 'Interface' },
      { key: 'lcn', label: 'LCN' },
      { key: 'descrambler_pos', label: 'Descrambler' },
    ];

    if (this._featureEnabled('dvbc')) {
      columns.push({ key: 'modulator_pos', label: 'Mod Net 1' });
    }
    if (this._featureEnabled('dvbc_net2')) {
      columns.push({ key: 'modulator_pos_net2', label: 'Mod Net 2' });
    }
    if (this._featureEnabled('dvbc') || this._featureEnabled('dvbc_net2')) {
      columns.push({ key: 'out_sid', label: 'Out SID' });
    }
    if (this._featureEnabled('ip')) {
      columns.push({ key: 'out_ip', label: 'Out IP' });
    }

    columns.push({ key: 'epg_url', label: 'Epg' });

    if (this._featureEnabled('hls')) {
      columns.push({ key: 'hls_enable', label: 'Hls' });
    }

    return columns;
  }

  _descramblerOptions() {
    return [
      'None',
      ...this.interfaces
        .filter((iface) => iface.type === 'dsc')
        .map((iface) => iface.position)
        .sort((left, right) => left.localeCompare(right, undefined, { numeric: true })),
    ];
  }

  _modulatorOptions(networkNum) {
    return [
      'None',
      ...this.interfaces
        .filter((iface) => iface.type === 'mod' && Number(iface.network_num || 0) === Number(networkNum))
        .map((iface) => iface.position)
        .sort((left, right) => left.localeCompare(right, undefined, { numeric: true })),
    ];
  }

  _sortValue(route, field) {
    if (field === 'service_output') {
      return route.output_name || route.service_name || '';
    }
    return route[field] ?? '';
  }

  _toggleSort(field) {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
    }
    this.requestUpdate();
  }

  _sortedRoutes() {
    const sorted = this.routes.map((route, index) => ({ route, index }));
    sorted.sort((a, b) => {
      const aVal = this._sortValue(a.route, this.sortField);
      const bVal = this._sortValue(b.route, this.sortField);
      const cmp = typeof aVal === 'number' && typeof bVal === 'number'
        ? aVal - bVal
        : String(aVal).localeCompare(String(bVal));
      return this.sortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }

  _sortIcon(field) {
    if (this.sortField !== field) return '↕';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  _onFieldChange(index, field, value) {
    this.routes = this.routes.map((route, routeIndex) => (
      routeIndex === index ? { ...route, [field]: value } : route
    ));
  }

  _isDirty() {
    return JSON.stringify(this.routes) !== this._initialSnapshot;
  }

  _duplicateValues(field) {
    const counts = new Map();
    for (const route of this.routes) {
      const rawValue = route[field];
      const value = rawValue == null ? '' : String(rawValue).trim();
      if (!value) {
        continue;
      }
      counts.set(value, (counts.get(value) || 0) + 1);
    }
    return new Set([...counts.entries()].filter(([, count]) => count > 1).map(([value]) => value));
  }

  _duplicateWarningCount() {
    const sidCount = (this._featureEnabled('dvbc') || this._featureEnabled('dvbc_net2'))
      ? this._duplicateValues('out_sid').size
      : 0;
    const ipCount = this._featureEnabled('ip') ? this._duplicateValues('out_ip').size : 0;
    return sidCount + ipCount;
  }

  _duplicateWarnings() {
    const warnings = [];

    if (this._featureEnabled('dvbc') || this._featureEnabled('dvbc_net2')) {
      for (const value of [...this._duplicateValues('out_sid')].sort((left, right) => left.localeCompare(right, undefined, { numeric: true }))) {
        warnings.push(`Duplicate service with SID: ${value}`);
      }
    }

    if (this._featureEnabled('ip')) {
      for (const value of [...this._duplicateValues('out_ip')].sort((left, right) => left.localeCompare(right))) {
        warnings.push(`Duplicate service with IP: ${value}`);
      }
    }

    return warnings;
  }

  _maxModulatorBitrate() {
    const qam = this._settingValue('dvbc_qam', '');
    if (qam === 'QAM-256') return 51000000;
    if (qam === 'QAM-128') return 44000000;
    if (qam === 'QAM-64') return 38000000;
    return 0;
  }

  _maxDescramblerBitrate() {
    return Number(this._settingValue('dsc_bitrate', 0) || 0);
  }

  _maxHlsServices() {
    return Number(this._settingValue('hls_services', 0) || 0);
  }

  _serviceBitrate(serviceType) {
    const normalized = String(serviceType || '').replace(/_/g, '').toLowerCase();
    if (normalized.includes('radio')) {
      return Number(this._settingValue('bitrate_radio', 0) || 0);
    }
    if (normalized.includes('hd')) {
      return Number(this._settingValue('bitrate_tvhd', 0) || 0);
    }
    return Number(this._settingValue('bitrate_tvsd', 0) || 0);
  }

  _usageSummary() {
    const usage = new Map();
    const addUsage = (type, interfacePos, bitrate, maxValue, usageMode) => {
      const key = `${type}:${interfacePos}`;
      const current = usage.get(key) || {
        type,
        interface_pos: interfacePos,
        count_services: 0,
        count_bitrate: 0,
        max_value: maxValue,
        usage_mode: usageMode,
      };
      current.count_services += 1;
      current.count_bitrate += bitrate;
      current.max_value = maxValue;
      usage.set(key, current);
    };

    for (const route of this.routes) {
      const serviceBitrate = this._serviceBitrate(route.service_type);

      if (route.descrambler_pos && route.descrambler_pos !== 'None') {
        addUsage('Descrambler', route.descrambler_pos, serviceBitrate, this._maxDescramblerBitrate(), 'bitrate');
      }
      if (this._featureEnabled('dvbc') && route.modulator_pos && route.modulator_pos !== 'None') {
        addUsage('Modulator', route.modulator_pos, serviceBitrate, this._maxModulatorBitrate(), 'bitrate');
      }
      if (this._featureEnabled('dvbc_net2') && route.modulator_pos_net2 && route.modulator_pos_net2 !== 'None') {
        addUsage('Modulator', route.modulator_pos_net2, serviceBitrate, this._maxModulatorBitrate(), 'bitrate');
      }
    }

    if (this._featureEnabled('hls')) {
      usage.set('Hls:-', {
        type: 'Hls',
        interface_pos: '-',
        count_services: this._hlsSelectedCount(),
        count_bitrate: 0,
        max_value: this._maxHlsServices(),
        usage_mode: 'services',
      });
    }

    return [...usage.values()].sort((left, right) => {
      const typeCompare = String(left.type).localeCompare(String(right.type));
      if (typeCompare !== 0) {
        return typeCompare;
      }
      return String(left.interface_pos).localeCompare(String(right.interface_pos), undefined, { numeric: true });
    });
  }

  _hasHlsWizard() {
    return (this.interfaces || []).some(iface => ['dvbs', 'dvbt', 'ip', 'hls', 'hls2ip'].includes(iface.type));
  }

  _hlsSelectedCount() {
    return this.routes.filter((route) => Boolean(route.hls_enable)).length;
  }

  _isHlsCheckboxDisabled(route) {
    if (!this._featureEnabled('hls')) {
      return true;
    }
    const maxHls = this._maxHlsServices();
    if (!maxHls) {
      return false;
    }
    return !route.hls_enable && this._hlsSelectedCount() >= maxHls;
  }

  _formatBitrate(value) {
    const bitrate = Number(value || 0);
    return `${(bitrate / 1000000).toFixed(2)} Mbps`;
  }

  _usagePercent(row) {
    if (!row.max_value) {
      return 0;
    }
    const numerator = row.usage_mode === 'services' ? row.count_services : row.count_bitrate;
    return Math.max(0, Math.min(100, Math.round((numerator / row.max_value) * 100)));
  }

  _usageBarClass(percent) {
    if (percent >= 90) return 'bg-red-500';
    if (percent >= 70) return 'bg-amber-500';
    return 'bg-green-500';
  }

  _renderUsageBar(row) {
    const percent = this._usagePercent(row);
    const valueLabel = row.usage_mode === 'services'
      ? `${row.count_services}/${row.max_value || 0}`
      : `${this._formatBitrate(row.count_bitrate)} / ${this._formatBitrate(row.max_value || 0)}`;

    return html`
      <div class="min-w-[180px]">
        <div class="mb-1 flex items-center justify-between gap-3 text-xs text-slate-500">
          <span>${valueLabel}</span>
          <span>${percent}%</span>
        </div>
        <div class="h-2 w-full overflow-hidden rounded-full bg-slate-200">
          <div class=${`h-full rounded-full ${this._usageBarClass(percent)}`} style=${`width: ${percent}%`}></div>
        </div>
      </div>
    `;
  }

  _duplicateCellClasses(field, value) {
    const duplicates = this._duplicateValues(field);
    const normalized = value == null ? '' : String(value).trim();
    return duplicates.has(normalized)
      ? 'border-red-300 bg-red-50 text-red-900'
      : 'border-slate-300 bg-white text-slate-800';
  }

  async _saveRoutes() {
    const duplicateWarnings = this._duplicateWarnings();
    if (duplicateWarnings.length > 0) {
      const confirmed = window.confirm([
        'The following warnings has been detected:',
        '',
        ...duplicateWarnings,
        '',
        'Save anyway?',
      ].join('\n'));
      if (!confirmed) {
        return;
      }
    }

    this.saving = true;
    try {
      const payload = this.routes.map(route => ({
        ...route,
        lcn: Number(route.lcn || 0),
        out_sid: Number(route.out_sid || 0),
        hls_enable: Boolean(route.hls_enable),
      }));
      const response = await api.updateRoutes(payload);
      if (!response.success) {
        this._showNotification(response.error || 'Failed to save layout', 'error');
        this.saving = false;
        return;
      }
      this._initialSnapshot = JSON.stringify(this.routes);
      this._showNotification('Layout saved successfully', 'success');
      await this._loadRoutes();
    } catch (error) {
      this._showNotification(error.message || 'Failed to save layout', 'error');
    }
    this.saving = false;
  }

  _renderSelect(index, field, value, options, widthClass = 'w-28') {
    return html`
      <select
        .value=${String(value || 'None')}
        @change=${(e) => this._onFieldChange(index, field, e.target.value)}
        class=${`${widthClass} rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20`}
      >
        ${options.map((option) => html`
          <option value=${option}>${option}</option>
        `)}
      </select>
    `;
  }

  _renderServiceEditor(route, index) {
    return html`
      <div class="min-w-[220px] space-y-2">
        <input
          type="text"
          .value=${String(route.output_name || '')}
          @input=${(e) => this._onFieldChange(index, 'output_name', e.target.value)}
          class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
        />
        <div class="flex items-center gap-2 text-xs text-slate-500">
          <span>${route.service_name || '—'}</span>
          ${route.scrambled ? html`
            <span class="rounded-full bg-amber-100 px-2 py-0.5 font-semibold text-amber-800">Scrambled</span>
          ` : ''}
        </div>
      </div>
    `;
  }

  _renderInterfaceLink(route) {
    return html`
      <button
        @click=${() => { window.location.hash = `#interface-edit/${route.interface_pos}/${route.interface_type}`; }}
        class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-left text-sm font-medium text-blue-700 transition hover:border-blue-400 hover:text-blue-900 hover:underline"
      >${route.interface_pos || '—'}</button>
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

    const columns = this._columns();
    const duplicateWarnings = this._duplicateWarnings();
    const usageSummary = this._usageSummary();

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Layout</h1>
            <p class="text-slate-500 text-sm">${this.routes.length} route${this.routes.length !== 1 ? 's' : ''} configured</p>
          </div>
          <div class="flex flex-wrap gap-3">
            ${this._hasHlsWizard() ? html`
              <button
                @click=${() => { window.location.hash = '#hls-wizard'; }}
                class="px-4 py-2 text-sm bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition"
              >HLS Wizard</button>
            ` : ''}
            <button
              @click=${this._loadRoutes}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
            >↻ Refresh</button>
            <button
              @click=${this._saveRoutes}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving || !this._isDirty()}
            >${this.saving ? 'Saving...' : 'Save Layout'}</button>
          </div>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium ${this.notification.type === 'success'
            ? 'bg-green-50 text-green-800 border border-green-200'
            : 'bg-red-50 text-red-800 border border-red-200'}">
            ${this.notification.message}
          </div>
        ` : ''}

        ${duplicateWarnings.length > 0 ? html`
          <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            <div class="font-medium">Duplicate routing values detected.</div>
            <div class="mt-1">Resolve them if unintended. Saving still requires an explicit confirmation, matching the legacy Layout workflow.</div>
            <div class="mt-2 space-y-1">
              ${duplicateWarnings.map((warning) => html`<div>${warning}</div>`)}
            </div>
          </div>
        ` : ''}

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
            <h2 class="text-sm font-semibold text-slate-700">Usage Summary</h2>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  <th class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Type</th>
                  <th class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Interface</th>
                  <th class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Services</th>
                  <th class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Calculated Bitrate</th>
                  <th class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Calculated Usage</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                ${usageSummary.map(row => html`
                  <tr>
                    <td class="px-5 py-3 text-slate-700">${row.type}</td>
                    <td class="px-5 py-3 font-mono text-slate-700">${row.interface_pos}</td>
                    <td class="px-5 py-3 text-slate-600">${row.count_services}</td>
                    <td class="px-5 py-3 font-mono text-slate-600">${row.type === 'Hls' ? '-' : this._formatBitrate(row.count_bitrate)}</td>
                    <td class="px-5 py-3">${this._renderUsageBar(row)}</td>
                  </tr>
                `)}
              </tbody>
            </table>
          </div>
          ${usageSummary.length === 0 ? html`
            <div class="p-6 text-center text-sm text-slate-400">No usage data available</div>
          ` : ''}
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  ${columns.map(col => html`
                    <th
                      @click=${() => this._toggleSort(col.key)}
                      class="px-5 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider cursor-pointer hover:bg-slate-100 select-none transition"
                    >
                      <span class="inline-flex items-center gap-1">
                        ${col.label}
                        <span class="text-slate-400">${this._sortIcon(col.key)}</span>
                      </span>
                    </th>
                  `)}
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                ${this._sortedRoutes().map(({ route, index }) => html`
                  <tr class="hover:bg-blue-50/50 transition-colors">
                    ${this._columns().map((column) => {
                      if (column.key === 'service_output') {
                        return html`<td class="px-5 py-3 text-sm text-slate-800">${this._renderServiceEditor(route, index)}</td>`;
                      }
                      if (column.key === 'interface_pos') {
                        return html`<td class="px-5 py-3 text-sm text-slate-600">${this._renderInterfaceLink(route)}</td>`;
                      }
                      if (column.key === 'lcn') {
                        return html`
                          <td class="px-5 py-3 text-sm font-mono text-slate-600">
                            <input
                              type="number"
                              .value=${String(route.lcn ?? '')}
                              @input=${(e) => this._onFieldChange(index, 'lcn', Number(e.target.value))}
                              class="w-24 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                            />
                          </td>
                        `;
                      }
                      if (column.key === 'descrambler_pos') {
                        return html`<td class="px-5 py-3 text-sm text-slate-600">${this._renderSelect(index, 'descrambler_pos', route.descrambler_pos, this._descramblerOptions())}</td>`;
                      }
                      if (column.key === 'modulator_pos') {
                        return html`<td class="px-5 py-3 text-sm text-slate-600">${this._renderSelect(index, 'modulator_pos', route.modulator_pos, this._modulatorOptions(1))}</td>`;
                      }
                      if (column.key === 'modulator_pos_net2') {
                        return html`<td class="px-5 py-3 text-sm text-slate-600">${this._renderSelect(index, 'modulator_pos_net2', route.modulator_pos_net2, this._modulatorOptions(2))}</td>`;
                      }
                      if (column.key === 'out_sid') {
                        return html`
                          <td class="px-5 py-3 text-sm font-mono text-slate-600">
                            <input
                              type="number"
                              .value=${String(route.out_sid ?? '')}
                              @input=${(e) => this._onFieldChange(index, 'out_sid', Number(e.target.value))}
                              class="w-28 rounded-lg border px-3 py-2 text-sm font-mono focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 ${this._duplicateCellClasses('out_sid', route.out_sid)}"
                            />
                          </td>
                        `;
                      }
                      if (column.key === 'out_ip') {
                        return html`
                          <td class="px-5 py-3 text-sm font-mono text-slate-600">
                            <input
                              type="text"
                              .value=${String(route.out_ip || '')}
                              @input=${(e) => this._onFieldChange(index, 'out_ip', e.target.value)}
                              class="w-44 rounded-lg border px-3 py-2 text-sm font-mono focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 ${this._duplicateCellClasses('out_ip', route.out_ip)}"
                            />
                          </td>
                        `;
                      }
                      if (column.key === 'epg_url') {
                        return html`
                          <td class="px-5 py-3 text-sm text-slate-600">
                            <input
                              type="text"
                              .value=${String(route.epg_url || '')}
                              @input=${(e) => this._onFieldChange(index, 'epg_url', e.target.value)}
                              class="w-40 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                            />
                          </td>
                        `;
                      }
                      if (column.key === 'hls_enable') {
                        return html`
                          <td class="px-5 py-3">
                            <label class="inline-flex items-center gap-2 text-sm font-medium text-slate-700">
                              <input
                                type="checkbox"
                                .checked=${Boolean(route.hls_enable)}
                                @change=${(e) => this._onFieldChange(index, 'hls_enable', e.target.checked)}
                                ?disabled=${this._isHlsCheckboxDisabled(route)}
                              />
                              Enabled
                            </label>
                          </td>
                        `;
                      }
                      return html`<td class="px-5 py-3 text-sm text-slate-600">—</td>`;
                    })}
                  </tr>
                `)}
              </tbody>
            </table>
          </div>

          ${this.routes.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No routes configured</div>
          ` : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-routes', IxuiRoutes);
