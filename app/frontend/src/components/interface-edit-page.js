import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiInterfaceEdit extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    position: { type: String },
    interfaceType: { type: String },
    config: { type: Object },
    services: { type: Array },
    loading: { type: Boolean },
    saving: { type: Boolean },
    scanning: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.position = '';
    this.interfaceType = '';
    this.config = null;
    this.services = [];
    this.loading = true;
    this.saving = false;
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
      const [config, services] = await Promise.allSettled([
        api.getInterfaceConfig(this.position, this.interfaceType),
        api.getServices(this.position),
      ]);
      this.config = config.status === 'fulfilled' ? config.value : {};
      this.services = services.status === 'fulfilled' ? (services.value || []) : [];
    } catch {
      this.config = {};
      this.services = [];
    }
    this.loading = false;
  }

  _onFieldChange(field, value) {
    this.config = { ...this.config, [field]: value };
  }

  async _save() {
    this.saving = true;
    try {
      await api.setInterfaceConfig(this.position, this.interfaceType, this.config);
      this._showNotification('Configuration saved successfully', 'success');
    } catch {
      this._showNotification('Failed to save configuration', 'error');
    }
    this.saving = false;
  }

  async _startScan() {
    this.scanning = true;
    try {
      await api.startScan(this.position);
      this._showNotification('Scan started successfully', 'success');
      setTimeout(() => this._loadData(), 2000);
    } catch {
      this._showNotification('Failed to start scan', 'error');
    }
    this.scanning = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
  }

  _goBack() {
    window.location.hash = '#interfaces';
  }

  _getTypeFields() {
    const t = (this.interfaceType || '').toLowerCase();
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
        { key: 'bw', label: 'Bandwidth', type: 'text' },
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
    if (t.includes('ip')) {
      return [
        { key: 'in_ip', label: 'Input IP', type: 'text' },
        { key: 'in_port', label: 'Input Port', type: 'number' },
        { key: 'max_bitrate', label: 'Max Bitrate', type: 'number' },
      ];
    }
    return Object.keys(this.config || {})
      .filter(k => k !== 'name' && k !== 'active')
      .map(k => ({ key: k, label: k, type: 'text' }));
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
            <button @click=${this._startScan}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition disabled:opacity-50"
              ?disabled=${this.scanning}
            >${this.scanning ? 'Scanning...' : '🔍 Scan'}</button>
            <button @click=${this._save}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : '💾 Save'}</button>
          </div>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Config Form -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
            <h2 class="text-sm font-semibold text-slate-700">Configuration</h2>
          </div>
          <div class="p-6 space-y-5">
            <!-- Name field -->
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-1">Interface Name</label>
              <input
                type="text"
                .value=${this.config?.name ?? ''}
                @input=${(e) => this._onFieldChange('name', e.target.value)}
                class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
              />
            </div>

            <!-- Active toggle -->
            <div class="flex items-center gap-3">
              <label class="text-sm font-medium text-slate-700">Active</label>
              <button
                @click=${() => this._onFieldChange('active', !this.config?.active)}
                class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${this.config?.active ? 'bg-blue-600' : 'bg-slate-300'}"
              >
                <span class="inline-block h-4 w-4 rounded-full bg-white transition-transform ${this.config?.active ? 'translate-x-6' : 'translate-x-1'}"></span>
              </button>
            </div>

            <!-- Type-specific fields -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              ${fields.map(f => this._renderField(f))}
            </div>
          </div>

          <div class="px-6 py-4 bg-slate-50 border-t border-slate-200 flex justify-end">
            <button @click=${this._save}
              class="px-6 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : 'Save Changes'}</button>
          </div>
        </div>

        <!-- Services Table -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-slate-700">Services (${this.services.length})</h2>
          </div>
          ${this.services.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No services found</div>
          ` : html`
            <table class="w-full text-sm">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Type</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">ID</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                ${this.services.map(svc => html`
                  <tr class="hover:bg-blue-50/50 transition-colors">
                    <td class="px-6 py-3 text-slate-800">${svc.name || svc.service_name || '—'}</td>
                    <td class="px-6 py-3 text-slate-600">${svc.type || svc.service_type || '—'}</td>
                    <td class="px-6 py-3 font-mono text-slate-600">${svc.id || svc.service_id || '—'}</td>
                  </tr>
                `)}
              </tbody>
            </table>
          `}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-interface-edit', IxuiInterfaceEdit);
