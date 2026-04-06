import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiHlsWizard extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    outputInterfaces: { type: Array },
    sourceInterfaces: { type: Array },
    availableServices: { type: Array },
    selectedServices: { type: Array },
    availableSelection: { type: Object },
    selectedSelection: { type: Object },
    activeFilters: { type: Object },
    maxServices: { type: Number },
    scannedAt: { type: String },
    warning: { type: String },
    loading: { type: Boolean },
    scanning: { type: Boolean },
    saving: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.outputInterfaces = [];
    this.sourceInterfaces = [];
    this.availableServices = [];
    this.selectedServices = [];
    this.availableSelection = new Set();
    this.selectedSelection = new Set();
    this.activeFilters = new Set();
    this.maxServices = 0;
    this.scannedAt = null;
    this.warning = null;
    this.loading = true;
    this.scanning = false;
    this.saving = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadState();
  }

  _serviceId(service) {
    return service.key || `${service.interface_pos}:${service.sid}:${service.name}`;
  }

  _sortServices(services) {
    return [...services].sort((left, right) => {
      const nameCompare = String(left.name || '').localeCompare(String(right.name || ''));
      if (nameCompare !== 0) {
        return nameCompare;
      }
      const posCompare = String(left.interface_pos || '').localeCompare(String(right.interface_pos || ''));
      if (posCompare !== 0) {
        return posCompare;
      }
      return Number(left.sid || 0) - Number(right.sid || 0);
    });
  }

  async _loadState(forceScan = false) {
    this.loading = !forceScan;
    this.scanning = forceScan;
    try {
      const state = forceScan
        ? await api.scanHlsWizard()
        : await api.getHlsWizardState();
      this.outputInterfaces = state.output_interfaces || [];
      this.sourceInterfaces = state.source_interfaces || [];
      this.maxServices = state.max_services || 0;
      this.scannedAt = state.scanned_at || null;
      this.warning = state.warning || null;
      this.availableServices = this._sortServices(state.available_services || []);
      this.selectedServices = this._sortServices(state.selected_services || []);
    } catch {
      this.outputInterfaces = [];
      this.sourceInterfaces = [];
      this.availableServices = [];
      this.selectedServices = [];
      this.maxServices = 0;
      this.scannedAt = null;
      this.warning = null;
    }
    this.availableSelection = new Set();
    this.selectedSelection = new Set();
    if (forceScan) {
      this.activeFilters = new Set();
    }
    this.loading = false;
    this.scanning = false;
  }

  _toggleAvailable(serviceId) {
    const next = new Set(this.availableSelection);
    if (next.has(serviceId)) { next.delete(serviceId); } else { next.add(serviceId); }
    this.availableSelection = next;
  }

  _toggleSelected(serviceId) {
    const next = new Set(this.selectedSelection);
    if (next.has(serviceId)) { next.delete(serviceId); } else { next.add(serviceId); }
    this.selectedSelection = next;
  }

  _toggleFilter(tag) {
    const next = new Set(this.activeFilters);
    if (next.has(tag)) {
      next.delete(tag);
    } else {
      next.add(tag);
    }
    this.activeFilters = next;
  }

  _clearFilters() {
    this.activeFilters = new Set();
  }

  _filters() {
    const filters = new Set();
    for (const service of [...this.availableServices, ...this.selectedServices]) {
      for (const filter of service.filters || []) {
        if (filter) {
          filters.add(filter);
        }
      }
    }
    return [...filters].sort((left, right) => left.localeCompare(right));
  }

  _filteredAvailableServices() {
    if (this.activeFilters.size === 0) {
      return this.availableServices;
    }

    const activeFilters = [...this.activeFilters].map(filter => filter.toLowerCase());
    return this.availableServices.filter(service => {
      const serviceFilters = (service.filters || []).map(filter => String(filter).toLowerCase());
      return activeFilters.every(activeFilter => (
        serviceFilters.some(serviceFilter => serviceFilter.includes(activeFilter))
      ));
    });
  }

  _moveRight() {
    if (this.availableSelection.size === 0) return;
    const visibleServices = this._filteredAvailableServices();
    const selectedIds = new Set(
      visibleServices
        .filter(service => this.availableSelection.has(this._serviceId(service)))
        .map(service => this._serviceId(service))
    );

    if ((this.selectedServices.length + selectedIds.size) > this.maxServices) {
      this._showNotification(`Too many channels selected. Limit is ${this.maxServices}.`, 'error');
      return;
    }

    const avail = [];
    const selectedToAdd = [];
    for (const service of this.availableServices) {
      if (selectedIds.has(this._serviceId(service))) {
        selectedToAdd.push(service);
      } else {
        avail.push(service);
      }
    }

    this.availableServices = this._sortServices(avail);
    this.selectedServices = this._sortServices([...this.selectedServices, ...selectedToAdd]);
    this.availableSelection = new Set();
  }

  _moveLeft() {
    if (this.selectedSelection.size === 0) return;
    const selectedIds = new Set(this.selectedSelection);
    const sel = [];
    const toAvailable = [];

    for (const service of this.selectedServices) {
      if (selectedIds.has(this._serviceId(service))) {
        toAvailable.push(service);
      } else {
        sel.push(service);
      }
    }

    this.selectedServices = this._sortServices(sel);
    this.availableServices = this._sortServices([...this.availableServices, ...toAvailable]);
    this.selectedSelection = new Set();
  }

  async _scan() {
    await this._loadState(true);
  }

  async _save() {
    if (this.selectedServices.length > this.maxServices) {
      this._showNotification(`Too many channels selected. Limit is ${this.maxServices}.`, 'error');
      return;
    }

    this.saving = true;
    try {
      const payload = [
        ...this.selectedServices.map(service => ({ ...service, enabled: true })),
        ...this.availableServices.map(service => ({ ...service, enabled: false })),
      ];
      await api.saveHlsWizardServices(payload);
      this._showNotification('HLS services saved successfully', 'success');
      window.location.hash = '#layout';
    } catch {
      this._showNotification('Failed to save HLS services', 'error');
    }
    this.saving = false;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 3000);
  }

  _goBack() {
    window.location.hash = '#layout';
  }

  _renderServiceList(services, selectionSet, toggleFn, emptyText) {
    if (services.length === 0) {
      return html`<div class="p-6 text-center text-slate-400 text-sm">${emptyText}</div>`;
    }
    return html`
      <div class="divide-y divide-slate-100 overflow-auto" style="max-height: 500px;">
        ${services.map((svc) => html`
          <div
            @click=${() => toggleFn(this._serviceId(svc))}
            class="px-4 py-3 flex items-center gap-3 cursor-pointer transition-colors
              ${selectionSet.has(this._serviceId(svc)) ? 'bg-blue-50 border-l-4 border-blue-500' : 'hover:bg-slate-50 border-l-4 border-transparent'}"
          >
            <input type="checkbox" .checked=${selectionSet.has(this._serviceId(svc))}
              @change=${() => toggleFn(this._serviceId(svc))} @click=${(e) => e.stopPropagation()}
              class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500" />
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium text-slate-800 truncate">${svc.name || '—'}</div>
              <div class="text-xs text-slate-500 flex flex-wrap gap-2">
                <span>Pos: ${svc.interface_pos || '—'}</span>
                <span>${svc.type || '—'}</span>
                <span>SID: ${svc.sid ?? '—'}</span>
              </div>
              ${(svc.filters || []).length > 0 ? html`
                <div class="mt-2 flex flex-wrap gap-1">
                  ${(svc.filters || []).slice(0, 5).map(filter => html`
                    <span class="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">${filter}</span>
                  `)}
                </div>
              ` : ''}
            </div>
            <div class="text-right text-xs text-slate-400 font-mono">${svc.hls_url || '—'}</div>
          </div>
        `)}
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

    const filters = this._filters();
    const filteredAvailable = this._filteredAvailableServices();
    const overCapacity = this.selectedServices.length > this.maxServices;

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <button @click=${this._goBack}
              class="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">← Back</button>
            <div>
              <h1 class="text-2xl font-bold text-slate-800">HLS Wizard</h1>
              <p class="text-slate-500 text-sm">Scan source services, choose HLS channels, and rewrite the HLS mapping</p>
            </div>
          </div>
          <div class="flex gap-3">
            <button @click=${this._scan}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition disabled:opacity-50"
              ?disabled=${this.scanning}
            >${this.scanning ? 'Scanning...' : '🔍 Scan'}</button>
            <button @click=${this._save}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving || this.maxServices === 0 || overCapacity}
            >${this.saving ? 'Saving...' : '💾 Save Services'}</button>
          </div>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        ${this.warning ? html`
          <div class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            ${this.warning}
          </div>
        ` : ''}

        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          <div class="rounded-xl border border-slate-200 bg-white px-4 py-3">
            <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">HLS Capacity</div>
            <div class="mt-2 text-2xl font-bold text-slate-800">${this.selectedServices.length}/${this.maxServices}</div>
            <div class="mt-1 text-sm ${overCapacity ? 'text-red-600' : 'text-slate-500'}">Selected services vs available HLS output slots</div>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white px-4 py-3">
            <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Source Interfaces</div>
            <div class="mt-2 text-2xl font-bold text-slate-800">${this.sourceInterfaces.length}</div>
            <div class="mt-1 text-sm text-slate-500">Active interfaces available for scanning</div>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white px-4 py-3">
            <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Available Services</div>
            <div class="mt-2 text-2xl font-bold text-slate-800">${this.availableServices.length}</div>
            <div class="mt-1 text-sm text-slate-500">Scanned services not currently selected</div>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white px-4 py-3">
            <div class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Last Scan</div>
            <div class="mt-2 text-sm font-mono text-slate-800">${this.scannedAt || 'Not scanned yet'}</div>
            <div class="mt-1 text-sm text-slate-500">Use Scan to refresh source services</div>
          </div>
        </div>

        <div class="rounded-xl border border-slate-200 bg-white p-4 space-y-4">
          <div class="flex flex-wrap gap-2">
            <button
              @click=${this._clearFilters}
              class="rounded-full px-3 py-1.5 text-xs font-semibold transition ${this.activeFilters.size === 0
                ? 'bg-blue-600 text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}"
            >All</button>
            ${filters.map(filter => html`
              <button
                @click=${() => this._toggleFilter(filter)}
                class="rounded-full px-3 py-1.5 text-xs font-semibold transition ${this.activeFilters.has(filter)
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}"
              >${filter}</button>
            `)}
          </div>

          <div class="flex flex-wrap gap-2 text-xs text-slate-500">
            ${this.outputInterfaces.map(interfaceItem => html`
              <span class="rounded-full bg-emerald-50 px-3 py-1.5 font-medium text-emerald-700">
                ${interfaceItem.position} · ${interfaceItem.name}
              </span>
            `)}
          </div>
        </div>

        <!-- Two-panel layout -->
        <div class="grid grid-cols-1 lg:grid-cols-[1fr_auto_1fr] gap-4 items-start">
          <!-- Available Services -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
              <h2 class="text-sm font-semibold text-slate-700">Available Services (${filteredAvailable.length})</h2>
            </div>
            ${this._renderServiceList(filteredAvailable, this.availableSelection, (serviceId) => this._toggleAvailable(serviceId), 'No available services')}
          </div>

          <!-- Arrow buttons -->
          <div class="flex lg:flex-col items-center justify-center gap-3 py-4">
            <button @click=${this._moveRight}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.availableSelection.size === 0}
            >→</button>
            <button @click=${this._moveLeft}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition disabled:opacity-50"
              ?disabled=${this.selectedSelection.size === 0}
            >←</button>
          </div>

          <!-- Selected HLS Services -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-blue-50">
              <h2 class="text-sm font-semibold text-blue-800">Selected HLS Services (${this.selectedServices.length})</h2>
            </div>
            ${this._renderServiceList(this.selectedServices, this.selectedSelection, (serviceId) => this._toggleSelected(serviceId), 'No services selected')}
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-hls-wizard', IxuiHlsWizard);
