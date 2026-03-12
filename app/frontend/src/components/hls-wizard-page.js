import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiHlsWizard extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    availableServices: { type: Array },
    selectedServices: { type: Array },
    availableSelection: { type: Object },
    selectedSelection: { type: Object },
    loading: { type: Boolean },
    saving: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.availableServices = [];
    this.selectedServices = [];
    this.availableSelection = new Set();
    this.selectedSelection = new Set();
    this.loading = true;
    this.saving = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
  }

  async _loadData() {
    this.loading = true;
    try {
      const interfaces = await api.getHlsInterfaces() || [];
      const selected = interfaces.filter(s => s.active);
      const available = interfaces.filter(s => !s.active);
      this.selectedServices = selected;
      this.availableServices = available;
    } catch {
      this.availableServices = [];
      this.selectedServices = [];
    }
    this.availableSelection = new Set();
    this.selectedSelection = new Set();
    this.loading = false;
  }

  _toggleAvailable(index) {
    const next = new Set(this.availableSelection);
    if (next.has(index)) { next.delete(index); } else { next.add(index); }
    this.availableSelection = next;
  }

  _toggleSelected(index) {
    const next = new Set(this.selectedSelection);
    if (next.has(index)) { next.delete(index); } else { next.add(index); }
    this.selectedSelection = next;
  }

  _moveRight() {
    if (this.availableSelection.size === 0) return;
    const toMove = [...this.availableSelection].sort((a, b) => b - a);
    const avail = [...this.availableServices];
    const sel = [...this.selectedServices];
    for (const idx of toMove) {
      const [item] = avail.splice(idx, 1);
      sel.push({ ...item, active: true });
    }
    this.availableServices = avail;
    this.selectedServices = sel;
    this.availableSelection = new Set();
  }

  _moveLeft() {
    if (this.selectedSelection.size === 0) return;
    const toMove = [...this.selectedSelection].sort((a, b) => b - a);
    const sel = [...this.selectedServices];
    const avail = [...this.availableServices];
    for (const idx of toMove) {
      const [item] = sel.splice(idx, 1);
      avail.push({ ...item, active: false });
    }
    this.selectedServices = sel;
    this.availableServices = avail;
    this.selectedSelection = new Set();
  }

  async _save() {
    this.saving = true;
    try {
      await api.saveHlsWizardServices(this.selectedServices);
      this._showNotification('HLS services saved successfully', 'success');
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
    window.location.hash = '#interfaces';
  }

  _renderServiceList(services, selectionSet, toggleFn, emptyText) {
    if (services.length === 0) {
      return html`<div class="p-6 text-center text-slate-400 text-sm">${emptyText}</div>`;
    }
    return html`
      <div class="divide-y divide-slate-100 overflow-auto" style="max-height: 500px;">
        ${services.map((svc, i) => html`
          <div
            @click=${() => toggleFn(i)}
            class="px-4 py-3 flex items-center gap-3 cursor-pointer transition-colors
              ${selectionSet.has(i) ? 'bg-blue-50 border-l-4 border-blue-500' : 'hover:bg-slate-50 border-l-4 border-transparent'}"
          >
            <input type="checkbox" .checked=${selectionSet.has(i)}
              @change=${() => toggleFn(i)} @click=${(e) => e.stopPropagation()}
              class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500" />
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium text-slate-800 truncate">${svc.name || '—'}</div>
              <div class="text-xs text-slate-500">
                ${svc.position ? `Pos: ${svc.position}` : ''}
                ${svc.type ? ` · ${svc.type}` : ''}
              </div>
            </div>
            ${svc.status ? html`
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium
                ${svc.status === 'running' ? 'bg-green-100 text-green-800' : 'bg-slate-100 text-slate-600'}">
                ${svc.status}
              </span>
            ` : ''}
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

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <button @click=${this._goBack}
              class="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">← Back</button>
            <div>
              <h1 class="text-2xl font-bold text-slate-800">HLS Wizard</h1>
              <p class="text-slate-500 text-sm">Configure HLS output services</p>
            </div>
          </div>
          <button @click=${this._save}
            class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
            ?disabled=${this.saving}
          >${this.saving ? 'Saving...' : '💾 Save'}</button>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Two-panel layout -->
        <div class="grid grid-cols-1 lg:grid-cols-[1fr_auto_1fr] gap-4 items-start">
          <!-- Available Services -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-200 bg-slate-50">
              <h2 class="text-sm font-semibold text-slate-700">Available Services (${this.availableServices.length})</h2>
            </div>
            ${this._renderServiceList(this.availableServices, this.availableSelection, (i) => this._toggleAvailable(i), 'No available services')}
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
            ${this._renderServiceList(this.selectedServices, this.selectedSelection, (i) => this._toggleSelected(i), 'No services selected')}
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-hls-wizard', IxuiHlsWizard);
