import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

const SERVICE_TYPE_OPTIONS = ['RADIO', 'TV_HD', 'TV_SD'];

class IxuiInterfaceEdit extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    position: { type: String },
    interfaceType: { type: String },
    interfaceInfo: { type: Object },
    unitInfo: { type: Object },
    config: { type: Object },
    services: { type: Array },
    interfaceTypes: { type: Array },
    emmData: { type: Object },
    scanTime: { type: String },
    loading: { type: Boolean },
    saving: { type: Boolean },
    savingServices: { type: Boolean },
    applying: { type: Boolean },
    scanning: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.position = '';
    this.interfaceType = '';
    this.interfaceInfo = null;
    this.unitInfo = null;
    this.config = null;
    this.services = [];
    this.interfaceTypes = [];
    this.emmData = null;
    this.scanTime = null;
    this.loading = true;
    this.saving = false;
    this.savingServices = false;
    this.applying = false;
    this.scanning = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
  }

  async _loadData() {
    this.loading = true;
    try {
      const configRequest = this._usesInfochFlow()
        ? api.getInterfaceInfoch(this.position)
        : api.getInterfaceConfig(this.position, this.interfaceType);

      const [interfaceInfo, unitInfo, config, services, scanTime, interfaceTypes] = await Promise.allSettled([
        api.getInterface(this.position),
        api.getUnitInfo(),
        configRequest,
        api.getServices(this.position),
        api.getInterfaceScanTime(this.position),
        api.getInterfaceTypes(),
      ]);

      this.interfaceInfo = interfaceInfo.status === 'fulfilled' ? interfaceInfo.value : null;
      this.unitInfo = unitInfo.status === 'fulfilled' ? unitInfo.value : null;
      this.config = this._normalizeConfig(config.status === 'fulfilled' ? config.value : {});
      this.services = this._normalizeServices(services.status === 'fulfilled' ? (services.value || []) : []);
      this.scanTime = scanTime.status === 'fulfilled' ? (scanTime.value?.scan_time || null) : null;
      this.interfaceTypes = interfaceTypes.status === 'fulfilled' ? (interfaceTypes.value || []) : [];

      if (this._supportsEmm()) {
        try {
          this.emmData = await api.getCurrentEmmList(this.position, this.interfaceType === 'dsc');
          if (!this.config?.emm && typeof this.emmData?.selected === 'number') {
            this.config = { ...this.config, emm: this.emmData.selected };
          }
        } catch {
          this.emmData = null;
        }
      } else {
        this.emmData = null;
      }
    } catch {
      this.interfaceInfo = null;
      this.unitInfo = null;
      this.config = {};
      this.services = [];
      this.interfaceTypes = [];
      this.emmData = null;
      this.scanTime = null;
    }
    this.loading = false;
  }

  _normalizeConfig(config) {
    const normalized = { ...(config || {}) };
    if (normalized.del != null && normalized.del_sys == null) {
      normalized.del_sys = normalized.del;
    }
    if (normalized.interface_name == null && this.interfaceInfo?.name) {
      normalized.interface_name = this.interfaceInfo.name;
    }
    if (normalized.interface_active == null && this.interfaceInfo) {
      normalized.interface_active = Boolean(this.interfaceInfo.active);
    }
    return normalized;
  }

  _serializeConfig() {
    const payload = { ...(this.config || {}) };
    payload.interface_pos = this.position;
    if (payload.del_sys != null) {
      payload.del = payload.del_sys;
      delete payload.del_sys;
    }
    return payload;
  }

  _normalizeServices(services) {
    const normalized = (services || []).map((service, index) => ({
      ...service,
      interface_pos: service.interface_pos || this.position,
      id: service.id ?? service.sid ?? index + 1,
      sid: service.sid ?? service.id ?? index + 1,
      type: this._normalizeServiceType(service.type || service.service_type),
      enabled: service.enabled !== false,
      lang: service.lang || 'eng',
      all_langs: Array.isArray(service.all_langs) && service.all_langs.length
        ? service.all_langs
        : [service.lang || 'eng'],
      radio_url: service.radio_url || service.webradio_url || '',
      hls_url: service.hls_url || '',
      webradio_url: service.webradio_url || service.radio_url || '',
      show_pres: Boolean(service.show_pres),
      prefered_lcn: service.prefered_lcn ?? index + 1,
      key: service.key || `${this.position}-${service.sid ?? service.id ?? index + 1}`,
    }));

    if (!this._usesExclusiveServiceSelection()) {
      return normalized;
    }

    let selectedIndex = normalized.findIndex((service) => service.enabled !== false);
    if (selectedIndex < 0) {
      return normalized;
    }

    return normalized.map((service, index) => ({
      ...service,
      enabled: index === selectedIndex,
    }));
  }

  _normalizeServiceType(value) {
    const normalized = String(value || '').trim().toUpperCase().replace('-', '_');
    if (normalized === 'TV') {
      return 'TV_SD';
    }
    return SERVICE_TYPE_OPTIONS.includes(normalized) ? normalized : 'TV_SD';
  }

  _type() {
    return (this.interfaceType || '').toLowerCase();
  }

  _isCloudUnit() {
    return Boolean(this.unitInfo?.cloud);
  }

  _showsServiceWorkflow() {
    return !(this._isWebradio() && !this._isCloudUnit());
  }

  _supportsScan() {
    const type = this._type();
    if (type === 'webradio' && !this._isCloudUnit()) {
      return false;
    }
    return !type.includes('hdmi') && !type.includes('asi');
  }

  _supportsEmm() {
    const type = this._type();
    return ['dvbs', 'dvbs2', 'dvbt', 'dvbt2', 'dvbc', 'dvbudp', 'dsc'].includes(type);
  }

  _usesInfochFlow() {
    return this._type() === 'infoch';
  }

  _isHlsOutput() {
    return this._type() === 'hls2ip';
  }

  _isWebradio() {
    return this._type() === 'webradio';
  }

  _isInfostreamer() {
    return this._type() === 'infostreamer';
  }

  _isHdmiType() {
    return this._type() === 'dvbhdmi' || this._type() === 'hdmi2ip';
  }

  _supportsApply() {
    return !this._usesInfochFlow();
  }

  _usesUrlColumn() {
    return this._isHlsOutput() || this._isWebradio() || this._usesInfochFlow();
  }

  _usesExclusiveServiceSelection() {
    return this._isHlsOutput() || this._isWebradio() || this._usesInfochFlow();
  }

  _onFieldChange(field, value) {
    this.config = { ...this.config, [field]: value };
  }

  _onServiceChange(index, field, value) {
    if (field === 'enabled' && this._usesExclusiveServiceSelection()) {
      this.services = this.services.map((service, serviceIndex) => ({
        ...service,
        enabled: serviceIndex === index,
      }));
      return;
    }

    this.services = this.services.map((service, serviceIndex) => (
      serviceIndex === index ? { ...service, [field]: value } : service
    ));
  }

  async _save() {
    this.saving = true;
    try {
      const response = this._usesInfochFlow()
        ? await api.setInterfaceInfoch(this.position, this._serializeConfig())
        : await api.setInterfaceConfig(this.position, this.interfaceType, this._serializeConfig());
      if (!response?.success) {
        throw new Error(response?.error || 'Failed to save configuration');
      }
      this._showNotification('Configuration saved successfully', 'success');
      if (this._supportsEmm()) {
        this.emmData = await api.getCurrentEmmList(this.position, this.interfaceType === 'dsc');
      }
    } catch (error) {
      this._showNotification(error.message || 'Failed to save configuration', 'error');
    }
    this.saving = false;
  }

  async _saveServices() {
    this.savingServices = true;
    try {
      const payload = this.services.map((service, index) => ({
        ...service,
        interface_pos: this.position,
        sid: Number(service.sid || index + 1),
        prefered_lcn: Number(service.prefered_lcn || index + 1),
        radio_url: service.radio_url || service.webradio_url || '',
        webradio_url: service.webradio_url || service.radio_url || '',
        hls_url: service.hls_url || '',
      }));
      const response = await api.saveServices(this.position, payload);
      if (!response?.success) {
        throw new Error(response?.error || 'Failed to save services');
      }
      this._showNotification('Services saved successfully', 'success');
      if (this._supportsEmm()) {
        this.emmData = await api.getCurrentEmmList(this.position, this.interfaceType === 'dsc');
      }
    } catch (error) {
      this._showNotification(error.message || 'Failed to save services', 'error');
    }
    this.savingServices = false;
  }

  async _apply() {
    if (!this._supportsApply()) {
      return;
    }

    this.applying = true;
    try {
      const saveResponse = await api.setInterfaceConfig(this.position, this.interfaceType, this._serializeConfig());
      if (!saveResponse?.success) {
        throw new Error(saveResponse?.error || 'Failed to save configuration');
      }
      const applyResponse = await api.applyInterface(this.position, this.interfaceType);
      if (!applyResponse?.success) {
        throw new Error(applyResponse?.error || 'Failed to apply configuration');
      }
      this._showNotification('Configuration applied to runtime', 'success');
    } catch (error) {
      this._showNotification(error.message || 'Failed to apply configuration', 'error');
    }
    this.applying = false;
  }

  async _startScan() {
    this.scanning = true;
    try {
      if (this._usesInfochFlow()) {
        const infochResponse = await api.setInterfaceInfoch(this.position, this._serializeConfig());
        if (!infochResponse?.success) {
          throw new Error(infochResponse?.error || 'Failed to save info channel settings');
        }
      } else {
        const saveResponse = await api.setInterfaceConfig(this.position, this.interfaceType, this._serializeConfig());
        if (!saveResponse?.success) {
          throw new Error(saveResponse?.error || 'Failed to save configuration');
        }
        const applyResponse = await api.applyInterface(this.position, this.interfaceType);
        if (!applyResponse?.success) {
          throw new Error(applyResponse?.error || 'Failed to apply configuration');
        }
      }
      const scanResponse = await api.startScan(this.position);
      if (!scanResponse?.success) {
        throw new Error(scanResponse?.error || 'Failed to start scan');
      }
      const [services, scanTime] = await Promise.allSettled([
        api.getInterfaceScanResult(this.position),
        api.getInterfaceScanTime(this.position),
      ]);
      this.services = services.status === 'fulfilled' ? this._normalizeServices(services.value || []) : this.services;
      this.scanTime = scanTime.status === 'fulfilled' ? (scanTime.value?.scan_time || null) : this.scanTime;
      this._showNotification('Scan completed successfully', 'success');
    } catch (error) {
      this._showNotification(error.message || 'Failed to start scan', 'error');
    }
    this.scanning = false;
  }

  async _changeMultiBandType(e) {
    const newType = e.target.value;
    if (!newType || newType === this.interfaceType) {
      return;
    }

    this.loading = true;
    try {
      const response = await api.updateInterfaceMultibandType(this.position, newType);
      if (!response.success) {
        this._showNotification(response.error || 'Failed to switch interface type', 'error');
        this.loading = false;
        return;
      }
      window.location.hash = `#interface-edit/${this.position}/${newType}`;
      return;
    } catch {
      this._showNotification('Failed to switch multiband interface type', 'error');
    }
    this.loading = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
  }

  _goBack() {
    window.location.hash = '#interfaces';
  }

  _getTypeFields() {
    const t = this._type();
    if (t === 'infoch') {
      return [];
    }
    if (t.includes('dvb-s') || t.includes('dvbs')) {
      return [
        { key: 'freq', label: 'Frequency (MHz)', type: 'number' },
        { key: 'pol', label: 'Polarization', type: 'select', options: ['H', 'V'] },
        { key: 'symb', label: 'Symbol Rate', type: 'number' },
        { key: 'del_sys', label: 'Delivery System', type: 'text' },
        { key: 'satno', label: 'Satellite Number', type: 'number' },
        { key: 'lnb_type', label: 'LNB Type', type: 'text' },
      ];
    }
    if (t.includes('dvb-t') || t.includes('dvbt')) {
      return [
        { key: 'freq', label: 'Frequency (MHz)', type: 'number' },
        { key: 'bw', label: 'Bandwidth', type: 'number' },
        { key: 'del_sys', label: 'Delivery System', type: 'text' },
      ];
    }
    if (t.includes('dvb-c') || t.includes('dvbc')) {
      return [
        { key: 'freq', label: 'Frequency (MHz)', type: 'number' },
        { key: 'symb', label: 'Symbol Rate', type: 'number' },
        { key: 'del_sys', label: 'Delivery System', type: 'text' },
        { key: 'constellation', label: 'Constellation', type: 'text' },
      ];
    }
    if (t === 'infostreamer' || t === 'istr') {
      return [
        { key: 'pres_url', label: 'Presentation URL', type: 'text' },
      ];
    }
    if (t === 'dvbhdmi' || t === 'hdmi2ip') {
      return [
        { key: 'hdmi_format', label: 'HDMI Format', type: 'text' },
      ];
    }
    if (t === 'hls2ip') {
      return [
        { key: 'max_bitrate', label: 'Max Bitrate', type: 'number' },
      ];
    }
    if (t === 'webradio') {
      return [
        { key: 'gain', label: 'Gain', type: 'number' },
        ...(!this._isCloudUnit() ? [{ key: 'webradio_url', label: 'Webradio URL', type: 'text' }] : []),
      ];
    }
    if (t === 'ip' || t === 'dvbudp') {
      return [
        { key: 'in_ip', label: 'Input IP', type: 'text' },
        { key: 'in_port', label: 'Input Port', type: 'number' },
        { key: 'max_bitrate', label: 'Max Bitrate', type: 'number' },
      ];
    }
    return Object.keys(this.config || {})
      .filter(k => !['interface_name', 'interface_active', 'interface_pos', 'id', 'del'].includes(k))
      .map(k => ({ key: k, label: k, type: 'text' }));
  }

  _renderEmmField() {
    if (!this._supportsEmm() || !this.emmData?.entries?.length) {
      return html``;
    }

    const currentValue = Number(this.config?.emm || 0);
    return html`
      <div>
        <label class="block text-sm font-medium text-slate-700 mb-1">EMM Slot</label>
        <select
          .value=${String(currentValue)}
          @change=${(e) => this._onFieldChange('emm', Number(e.target.value))}
          class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
        >
          ${this.emmData.entries.map(entry => html`
            <option
              value=${entry.value}
              ?disabled=${entry.in_use && Number(entry.value) !== currentValue}
            >${entry.label}${entry.in_use && Number(entry.value) !== currentValue ? ' (in use)' : ''}</option>
          `)}
        </select>
      </div>
    `;
  }

  _renderField(field) {
    const value = this.config?.[field.key] ?? '';
    if (field.type === 'select') {
      return html`
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">${field.label}</label>
          <select
            .value=${String(value)}
            @change=${(e) => this._onFieldChange(field.key, e.target.value)}
            class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
          >
            ${(field.options || []).map(opt => html`
              <option value=${opt} ?selected=${String(value) === String(opt)}>${opt}</option>
            `)}
          </select>
        </div>
      `;
    }
    return html`
      <div>
        <label class="block text-sm font-medium text-slate-700 mb-1">${field.label}</label>
        <input
          type=${field.type === 'number' ? 'number' : 'text'}
          .value=${String(value)}
          @input=${(e) => this._onFieldChange(field.key, field.type === 'number' ? Number(e.target.value) : e.target.value)}
          class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition font-mono"
        />
      </div>
    `;
  }

  _serviceUrlValue(service) {
    if (this._isHlsOutput()) {
      return service.hls_url || '';
    }
    if (this._isWebradio() || this._usesInfochFlow()) {
      return service.webradio_url || service.radio_url || '';
    }
    return '';
  }

  _serviceLanguageOptions(service) {
    const values = Array.isArray(service?.all_langs) ? service.all_langs.filter(Boolean) : [];
    if (service?.lang && !values.includes(service.lang)) {
      values.unshift(service.lang);
    }
    return values.length ? values : ['eng'];
  }

  _renderServicesSection() {
    if (!this._showsServiceWorkflow()) {
      return html``;
    }

    if (this.services.length === 0) {
      return html`
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-slate-700">Services (0)</h2>
          </div>
          <div class="p-8 text-center text-slate-400">No services found</div>
        </div>
      `;
    }

    return html`
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
          <h2 class="text-sm font-semibold text-slate-700">Services (${this.services.length})</h2>
          <button @click=${this._saveServices}
            class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
            ?disabled=${this.savingServices || this.services.length === 0}
          >${this.savingServices ? 'Saving...' : 'Save Services'}</button>
        </div>
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-slate-50 border-b border-slate-200">
              <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
              <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Enabled</th>
              ${this._isInfostreamer() ? html`
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Radio URL</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Show Presentation</th>
              ` : html`
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">SID</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Language</th>
              `}
              ${this._usesUrlColumn() ? html`
                <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">URL</th>
              ` : ''}
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            ${this.services.map((svc, index) => html`
              <tr class="hover:bg-blue-50/50 transition-colors">
                <td class="px-6 py-3 text-slate-800">${svc.name || svc.service_name || ''}</td>
                <td class="px-6 py-3 text-slate-600">
                  <label class="inline-flex items-center gap-2 text-sm font-medium text-slate-700">
                    <input
                      type="checkbox"
                      .checked=${svc.enabled !== false}
                      @change=${(e) => this._onServiceChange(index, 'enabled', e.target.checked)}
                    />
                    Enabled
                  </label>
                </td>
                ${this._isInfostreamer() ? html`
                  <td class="px-6 py-3 text-slate-600">
                    <input
                      type="text"
                      .value=${String(svc.radio_url || '')}
                      @input=${(e) => this._onServiceChange(index, 'radio_url', e.target.value)}
                      class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    />
                  </td>
                  <td class="px-6 py-3 text-slate-600">
                    <label class="inline-flex items-center gap-2 text-sm font-medium text-slate-700">
                      <input
                        type="checkbox"
                        .checked=${Boolean(svc.show_pres)}
                        @change=${(e) => this._onServiceChange(index, 'show_pres', e.target.checked)}
                      />
                      Show
                    </label>
                  </td>
                ` : html`
                  <td class="px-6 py-3 font-mono text-slate-600">${svc.sid || svc.id || ''}</td>
                  <td class="px-6 py-3 text-slate-600">
                    <select
                      .value=${this._normalizeServiceType(svc.type || svc.service_type)}
                      @change=${(e) => this._onServiceChange(index, 'type', e.target.value)}
                      class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    >
                      ${SERVICE_TYPE_OPTIONS.map((option) => html`
                        <option value=${option}>${option}</option>
                      `)}
                    </select>
                  </td>
                  <td class="px-6 py-3 text-slate-600">
                    <select
                      .value=${String(svc.lang || 'eng')}
                      @change=${(e) => this._onServiceChange(index, 'lang', e.target.value)}
                      class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    >
                      ${this._serviceLanguageOptions(svc).map(option => html`
                        <option value=${option}>${option}</option>
                      `)}
                    </select>
                  </td>
                `}
                ${this._usesUrlColumn() ? html`
                  <td class="px-6 py-3 font-mono text-slate-600">${this._serviceUrlValue(svc)}</td>
                ` : ''}
              </tr>
            `)}
          </tbody>
        </table>
      </div>
    `;
  }

  _renderWebradioInfo() {
    if (!this._isWebradio() || this._isCloudUnit()) {
      return html``;
    }

    return html`
      <div class="rounded-xl border border-sky-200 bg-sky-50 px-5 py-4 text-sm text-sky-900">
        <div class="font-semibold">Supported formats for radio services</div>
        <div class="mt-3 space-y-3">
          <p><span class="font-semibold">1. HLS, M3U8</span><br />ex: http://as-hls-ww-live.akamaized.net/pool_904/live/ww/bbc_radio_one/bbc_radio_one.isml/bbc_radio_one-audio%3d48000.norewind.m3u8</p>
          <p><span class="font-semibold">2. M3U</span><br />ex: https://sverigesradio.se/topsy/direkt/132-mp3.m3u</p>
          <p><span class="font-semibold">3. Audiostream (mp3 or aac)</span><br />ex: https://wr05-ice.stream.khz.se/wr05_aac</p>
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

    const fields = this._getTypeFields();

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <button @click=${this._goBack}
              class="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">← Back</button>
            <div>
              <h1 class="text-2xl font-bold text-slate-800">Edit Interface ${this.position}</h1>
              <p class="text-slate-500 text-sm">Type: <span class="font-mono">${this.interfaceType || 'Unknown'}</span></p>
            </div>
          </div>
          <div class="flex gap-3">
            ${this._supportsApply() ? html`
              <button @click=${this._apply}
                class="px-4 py-2 text-sm bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition disabled:opacity-50"
                ?disabled=${this.applying}
              >${this.applying ? 'Applying...' : '🛠 Apply'}</button>
            ` : ''}
            <button @click=${this._startScan}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition disabled:opacity-50"
              ?disabled=${this.scanning || !this._supportsScan()}
            >${this.scanning ? 'Scanning...' : '🔍 Scan'}</button>
            <button @click=${this._save}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : '💾 Save'}</button>
          </div>
        </div>

        <div class="grid grid-cols-1 xl:grid-cols-3 gap-4">
          <div class="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">
            <span class="font-medium text-slate-700">Runtime Status:</span>
            <span class="ml-2 font-mono">${this.interfaceInfo?.status || 'unknown'}</span>
          </div>
          ${this._supportsScan() ? html`
            <div class="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">
              <span class="font-medium text-slate-700">Last Scan:</span>
              <span class="ml-2 font-mono">${this.scanTime || 'Never'}</span>
            </div>
          ` : ''}
          ${this._showsServiceWorkflow() ? html`
            <div class="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">
              <span class="font-medium text-slate-700">Services:</span>
              <span class="ml-2 font-mono">${this.services.length}</span>
            </div>
          ` : ''}
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        ${this._renderWebradioInfo()}

        <!-- Config Form -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
            <h2 class="text-sm font-semibold text-slate-700">Configuration</h2>
          </div>
          <div class="p-6 space-y-5">
            ${this.interfaceInfo?.multi_band ? html`
              <div>
                <label class="block text-sm font-medium text-slate-700 mb-1">Interface Type</label>
                <select
                  .value=${String(this.interfaceType || '')}
                  @change=${this._changeMultiBandType}
                  class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                >
                  ${this.interfaceTypes.map(type => html`
                    <option value=${type}>${type}</option>
                  `)}
                </select>
              </div>
            ` : ''}

            <!-- Name field -->
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-1">Interface Name</label>
              <input
                type="text"
                .value=${this.config?.interface_name ?? ''}
                @input=${(e) => this._onFieldChange('interface_name', e.target.value)}
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
              />
            </div>

            <!-- Active toggle -->
            <div class="flex items-center gap-3">
              <label class="text-sm font-medium text-slate-700">Active</label>
              <button
                @click=${() => this._onFieldChange('interface_active', !this.config?.interface_active)}
                class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${this.config?.interface_active ? 'bg-blue-600' : 'bg-slate-300'}"
              >
                <span class="inline-block h-4 w-4 rounded-full bg-white transition-transform ${this.config?.interface_active ? 'translate-x-6' : 'translate-x-1'}"></span>
              </button>
            </div>

            <!-- Type-specific fields -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              ${fields.map(f => this._renderField(f))}
              ${this._renderEmmField()}
            </div>
          </div>

          <div class="px-6 py-4 bg-slate-50 border-t border-slate-200 flex justify-end">
            <button @click=${this._save}
              class="px-6 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : 'Save Changes'}</button>
          </div>
        </div>

        ${this._renderServicesSection()}
      </div>
    `;
  }
}

customElements.define('ixui-interface-edit', IxuiInterfaceEdit);
