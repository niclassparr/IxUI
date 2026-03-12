import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiForcedContent extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    entries: { type: Array },
    loading: { type: Boolean },
    saving: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.entries = [];
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
      const data = await api.getForcedContents();
      if (data && typeof data === 'object' && !Array.isArray(data)) {
        this.entries = Object.entries(data).map(([key, val]) => ({
          key,
          id: val.id,
          name: val.name,
          enabled: val.enabled ?? false,
          override_index: val.override_index ?? 0,
        }));
      } else {
        this.entries = [];
      }
    } catch {
      this.entries = [];
    }
    this.loading = false;
  }

  _toggleEnabled(index) {
    const updated = [...this.entries];
    updated[index] = { ...updated[index], enabled: !updated[index].enabled };
    this.entries = updated;
  }

  _onOverrideChange(index, value) {
    const updated = [...this.entries];
    updated[index] = { ...updated[index], override_index: Number(value) || 0 };
    this.entries = updated;
  }

  async _save() {
    this.saving = true;
    try {
      const payload = this.entries.map(entry => ({
        id: entry.id,
        name: entry.name,
        enabled: entry.enabled,
        override_index: entry.override_index,
      }));
      await api.saveForcedContents(payload);
      this._showNotification('Forced contents saved successfully', 'success');
    } catch {
      this._showNotification('Failed to save forced contents', 'error');
    }
    this.saving = false;
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

    return html`
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Forced Content</h1>
            <p class="text-slate-500 text-sm">Manage forced content entries</p>
          </div>
          <div class="flex gap-3">
            <button @click=${this._loadData}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
            <button @click=${this._save}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : '💾 Save Changes'}</button>
          </div>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Entries Table -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          ${this.entries.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No forced content entries</div>
          ` : html`
            <table class="w-full">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Name</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">ID</th>
                  <th class="px-6 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">Enabled</th>
                  <th class="px-6 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Override Index</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                ${this.entries.map((entry, index) => html`
                  <tr class="hover:bg-blue-50/50 transition-colors">
                    <td class="px-6 py-4 text-sm font-medium text-slate-800">${entry.name || '—'}</td>
                    <td class="px-6 py-4 text-sm font-mono text-slate-600">${entry.id ?? '—'}</td>
                    <td class="px-6 py-4 text-center">
                      <button
                        @click=${() => this._toggleEnabled(index)}
                        class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${entry.enabled ? 'bg-blue-600' : 'bg-slate-300'}"
                      >
                        <span class="inline-block h-4 w-4 rounded-full bg-white transition-transform ${entry.enabled ? 'translate-x-6' : 'translate-x-1'}"></span>
                      </button>
                    </td>
                    <td class="px-6 py-4">
                      <input
                        type="number"
                        .value=${String(entry.override_index)}
                        @input=${(e) => this._onOverrideChange(index, e.target.value)}
                        class="w-24 px-3 py-1.5 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition font-mono"
                      />
                    </td>
                  </tr>
                `)}
              </tbody>
            </table>

            <div class="px-6 py-4 bg-slate-50 border-t border-slate-200 flex justify-end">
              <button @click=${this._save}
                class="px-6 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                ?disabled=${this.saving}
              >${this.saving ? 'Saving...' : 'Save Changes'}</button>
            </div>
          `}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-forced-content', IxuiForcedContent);
