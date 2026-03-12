import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiSettings extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    settings: { type: Array },
    loading: { type: Boolean },
    saving: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.settings = [];
    this.loading = true;
    this.saving = false;
    this.notification = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadSettings();
  }

  async _loadSettings() {
    this.loading = true;
    try {
      this.settings = await api.getSettings() || [];
    } catch { this.settings = []; }
    this.loading = false;
  }

  _onValueChange(index, newValue) {
    const updated = [...this.settings];
    updated[index] = { ...updated[index], value: newValue };
    this.settings = updated;
  }

  async _save() {
    this.saving = true;
    try {
      await api.updateSettings(this.settings);
      this._showNotification('Settings saved successfully', 'success');
    } catch (err) {
      this._showNotification('Failed to save settings', 'error');
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
            <h1 class="text-2xl font-bold text-slate-800">Settings</h1>
            <p class="text-slate-500 text-sm">Manage system configuration</p>
          </div>
          <div class="flex gap-3">
            <button
              @click=${this._loadSettings}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
            >↻ Refresh</button>
            <button
              @click=${this._save}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : '💾 Save Changes'}</button>
          </div>
        </div>

        <!-- Notification -->
        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Settings Form -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          ${this.settings.length === 0 ? html`
            <div class="p-8 text-center text-slate-400">No settings available</div>
          ` : html`
            <div class="divide-y divide-slate-100">
              ${this.settings.map((setting, index) => html`
                <div class="flex items-center gap-4 px-6 py-4 hover:bg-slate-50/50 transition">
                  <div class="flex-1 min-w-0">
                    <label class="block text-sm font-medium text-slate-700 mb-0.5">${setting.name}</label>
                    ${setting.id != null ? html`<span class="text-xs text-slate-400">ID: ${setting.id}</span>` : ''}
                  </div>
                  <div class="w-80">
                    <input
                      type="text"
                      .value=${setting.value ?? ''}
                      @input=${(e) => this._onValueChange(index, e.target.value)}
                      class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition font-mono"
                    />
                  </div>
                </div>
              `)}
            </div>

            <!-- Bottom Save button -->
            <div class="px-6 py-4 bg-slate-50 border-t border-slate-200 flex justify-end">
              <button
                @click=${this._save}
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

customElements.define('ixui-settings', IxuiSettings);
