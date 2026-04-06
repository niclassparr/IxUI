import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

const NETWORK_OPTIONS = [
  { value: 0, label: 'None' },
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: 'Both' },
];

const OPERATION_MODE_OPTIONS = [
  { value: 0, label: 'Continuity' },
  { value: 1, label: 'Single' },
];

const SIGNAL_TYPE_OPTIONS = [
  { value: 0, label: 'Normally Open' },
  { value: 1, label: 'Normally Closed' },
];

const OVERRIDE_OPTIONS = [
  { value: 0, label: 'None' },
  { value: 1, label: 'On' },
  { value: 2, label: 'Off' },
];

const VOLUME_OPTIONS = [
  { value: -1, label: 'None' },
  ...Array.from({ length: 21 }, (_, index) => ({ value: index * 5, label: `${index * 5}` })),
];

class IxuiForcedContent extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    entries: { type: Array },
    liveEntries: { type: Array },
    media: { type: Array },
    featureEnabled: { type: Boolean },
    loading: { type: Boolean },
    saving: { type: Boolean },
    controlOpen: { type: Boolean },
    controlEntries: { type: Array },
    controlLoading: { type: Boolean },
    overrideBusyId: { type: Number },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.entries = [];
    this.liveEntries = [];
    this.media = [];
    this.featureEnabled = false;
    this.loading = true;
    this.saving = false;
    this.controlOpen = false;
    this.controlEntries = [];
    this.controlLoading = false;
    this.overrideBusyId = null;
    this.notification = null;
    this._liveTimer = null;
    this._notificationTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
  }

  disconnectedCallback() {
    this._stopLiveRefresh();
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
      this._notificationTimer = null;
    }
    super.disconnectedCallback();
  }

  async _loadData() {
    this.loading = true;
    try {
      const [feature, media, data] = await Promise.all([
        api.getFeature('forced_content'),
        api.getMedia(),
        api.getForcedContents(),
      ]);
      this.featureEnabled = Boolean(feature?.enabled);
      this.media = (media || []).map((item) => ({
        title: item.title || item.name || item.internal_filename || 'Untitled',
        internal_filename: item.internal_filename || item.url || item.name || '',
      }));
      this.entries = this._normalizeEntries(data);
      await this._refreshLiveEntries();
      this._syncLiveRefresh();
    } catch {
      this.entries = [];
      this.liveEntries = [];
      this.controlEntries = [];
      this.media = [];
      this.featureEnabled = false;
      this._stopLiveRefresh();
      this._showNotification('Failed to load force content data', 'error');
    }
    this.loading = false;
  }

  _syncLiveRefresh() {
    if (!this.featureEnabled) {
      this._stopLiveRefresh();
      return;
    }
    if (this._liveTimer) {
      return;
    }
    this._liveTimer = window.setInterval(() => {
      this._refreshLiveEntries(false);
    }, 1000);
  }

  _stopLiveRefresh() {
    if (this._liveTimer) {
      window.clearInterval(this._liveTimer);
      this._liveTimer = null;
    }
  }

  _defaultEntry(id) {
    return {
      id,
      name: '',
      enabled: false,
      networks: 0,
      ts_filename: 'None',
      operation_mode: 0,
      signal_type: 0,
      override_index: 0,
      signal_status: 0,
      com_status: true,
      volume: -1,
    };
  }

  _normalizeEntries(data) {
    const map = new Map();
    if (data && typeof data === 'object' && !Array.isArray(data)) {
      Object.values(data).forEach((entry) => {
        map.set(Number(entry.id), {
          ...this._defaultEntry(Number(entry.id)),
          ...entry,
          id: Number(entry.id),
          networks: Number(entry.networks ?? 0),
          operation_mode: Number(entry.operation_mode ?? 0),
          signal_type: Number(entry.signal_type ?? 0),
          override_index: Number(entry.override_index ?? 0),
          signal_status: Number(entry.signal_status ?? 0),
          volume: Number(entry.volume ?? -1),
          ts_filename: entry.ts_filename || 'None',
        });
      });
    }

    const entries = [];
    for (let id = 1; id <= 4; id += 1) {
      entries.push(map.get(id) || this._defaultEntry(id));
    }
    return entries;
  }

  _updateEntry(index, field, value) {
    this.entries = this.entries.map((entry, entryIndex) => (
      entryIndex === index ? { ...entry, [field]: value } : entry
    ));
  }

  _mediaOptions() {
    return [
      { title: 'None', internal_filename: 'None' },
      ...this.media,
    ];
  }

  _toggleEnabled(index) {
    this._updateEntry(index, 'enabled', !this.entries[index].enabled);
  }

  async _save() {
    this.saving = true;
    try {
      const payload = this.entries.map(entry => ({
        id: entry.id,
        name: entry.name,
        enabled: entry.enabled,
        networks: Number(entry.networks || 0),
        ts_filename: entry.ts_filename || 'None',
        operation_mode: Number(entry.operation_mode || 0),
        signal_type: Number(entry.signal_type || 0),
        override_index: entry.override_index,
        signal_status: entry.signal_status,
        com_status: entry.com_status,
        volume: Number(entry.volume ?? -1),
      }));
      await api.saveForcedContents(payload);
      this._showNotification('Force content settings saved successfully', 'success');
      await this._loadData();
    } catch {
      this._showNotification('Failed to save force content settings', 'error');
    }
    this.saving = false;
  }

  async _openControl() {
    this.controlOpen = true;
    this.controlEntries = this.liveEntries;
    await this._refreshLiveEntries(true);
  }

  _closeControl() {
    this.controlOpen = false;
    this.controlLoading = false;
  }

  async _refreshLiveEntries(showSpinner = false) {
    if (!this.featureEnabled) {
      this.liveEntries = [];
      this.controlEntries = [];
      this.controlLoading = false;
      return;
    }

    if (showSpinner && this.controlOpen) {
      this.controlLoading = true;
    }
    try {
      const liveEntries = await api.getEnabledForcedContents();
      this.liveEntries = liveEntries || [];
      this.controlEntries = this.liveEntries;
    } catch {
      this.liveEntries = [];
      this.controlEntries = [];
      if (showSpinner || this.controlOpen) {
        this._showNotification('Failed to load force content control status', 'error');
      }
    }
    if (showSpinner && this.controlOpen) {
      this.controlLoading = false;
    }
  }

  async _changeOverride(id, overrideIndex) {
    this.overrideBusyId = id;
    try {
      const response = await api.saveForcedContentOverrideStatus(id, Number(overrideIndex));
      if (!response?.success) {
        this._showNotification(response?.error || 'Failed to save override state', 'error');
        this.overrideBusyId = null;
        return;
      }
      this.entries = this.entries.map((entry) => (
        entry.id === id ? { ...entry, override_index: Number(overrideIndex) } : entry
      ));
      await this._refreshLiveEntries(false);
    } catch {
      this._showNotification('Failed to save override state', 'error');
    }
    this.overrideBusyId = null;
  }

  _runtimeEntry(entry) {
    const liveEntry = this.liveEntries.find((candidate) => candidate.id === entry.id);
    if (!liveEntry) {
      return entry;
    }
    return {
      ...entry,
      signal_status: liveEntry.signal_status,
      com_status: liveEntry.com_status,
      override_index: liveEntry.override_index,
    };
  }

  _overrideLabel(overrideIndex) {
    return OVERRIDE_OPTIONS.find((option) => option.value === Number(overrideIndex))?.label || 'None';
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    if (this._notificationTimer) {
      window.clearTimeout(this._notificationTimer);
    }
    this._notificationTimer = window.setTimeout(() => { this.notification = null; }, 3000);
  }

  _signalStatusLabel(status) {
    switch (Number(status || 0)) {
      case 1:
        return 'On';
      case 2:
        return 'Off';
      default:
        return 'None';
    }
  }

  _signalStatusClasses(status) {
    switch (Number(status || 0)) {
      case 1:
        return 'bg-green-100 text-green-800';
      case 2:
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-slate-100 text-slate-700';
    }
  }

  _renderSelect(label, value, options, onChange) {
    return html`
      <label class="block space-y-1">
        <span class="text-sm font-medium text-slate-700">${label}</span>
        <select
          class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
          .value=${String(value)}
          @change=${(event) => onChange(event.target.value)}
        >
          ${options.map((option) => html`
            <option value=${String(option.value ?? option.internal_filename)}>${option.label ?? option.title}</option>
          `)}
        </select>
      </label>
    `;
  }

  _renderControlOverlay() {
    if (!this.controlOpen) {
      return html``;
    }

    return html`
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-6">
        <div class="w-full max-w-4xl rounded-2xl border border-slate-200 bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">Force Content Control</h2>
              <p class="text-sm text-slate-500">Live status refreshes every second for enabled entries.</p>
            </div>
            <button
              @click=${this._closeControl}
              class="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            >Close</button>
          </div>

          <div class="max-h-[70vh] overflow-auto p-6">
            ${this.controlLoading ? html`
              <div class="flex items-center justify-center py-12">
                <div class="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent"></div>
              </div>
            ` : this.controlEntries.length === 0 ? html`
              <div class="rounded-lg border border-dashed border-slate-300 px-4 py-12 text-center text-sm text-slate-500">
                No enabled force content entries.
              </div>
            ` : html`
              <div class="grid gap-4 md:grid-cols-2">
                ${this.controlEntries.map((entry) => html`
                  <div class="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-4">
                    <div class="flex items-start justify-between gap-4">
                      <div>
                        <h3 class="text-base font-semibold text-slate-800">${entry.name || `Force Content #${entry.id}`}</h3>
                        ${!entry.com_status ? html`
                          <div class="mt-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
                            Warning - Communication Error
                          </div>
                        ` : ''}
                      </div>
                      <span class=${`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${this._signalStatusClasses(entry.signal_status)}`}>
                        Signal ${this._signalStatusLabel(entry.signal_status)}
                      </span>
                    </div>

                    ${this._renderSelect(
                      'Override',
                      entry.override_index ?? 0,
                      OVERRIDE_OPTIONS,
                      (value) => this._changeOverride(entry.id, value)
                    )}

                    ${this.overrideBusyId === entry.id ? html`
                      <div class="text-sm text-slate-500">Saving override...</div>
                    ` : ''}
                  </div>
                `)}
              </div>
            `}
          </div>
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
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Force Content</h1>
            <p class="text-slate-500 text-sm">Four fixed force content cards plus a live override control workflow</p>
          </div>
          <div class="flex gap-3">
            <button @click=${this._openControl}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition"
              ?disabled=${!this.featureEnabled}
            >Force Content Control</button>
            <button @click=${this._loadData}
              class="px-4 py-2 text-sm bg-white border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-700 transition">↻ Refresh</button>
            <button @click=${this._save}
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
              ?disabled=${this.saving}
            >${this.saving ? 'Saving...' : 'Save'}</button>
          </div>
        </div>

        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        ${!this.featureEnabled ? html`
          <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-slate-600">
            The force content function is disabled.
          </div>
        ` : html`
          <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            Live signal status refreshes every second for enabled entries.
          </div>
          <div class="grid gap-6 xl:grid-cols-2">
            ${this.entries.map((entry, index) => {
              const runtimeEntry = this._runtimeEntry(entry);
              return html`
              <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm space-y-4">
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <h2 class="text-lg font-semibold text-slate-800">Force Content #${entry.id}</h2>
                    <p class="text-sm text-slate-500">Signal and media override profile</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      <span class=${`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${entry.enabled ? 'bg-blue-100 text-blue-800' : 'bg-slate-100 text-slate-700'}`}>
                        ${entry.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                      <span class=${`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${this._signalStatusClasses(runtimeEntry.signal_status)}`}>
                        Signal ${this._signalStatusLabel(runtimeEntry.signal_status)}
                      </span>
                      <span class=${`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${runtimeEntry.com_status ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
                        ${runtimeEntry.com_status ? 'Communication OK' : 'Communication Error'}
                      </span>
                    </div>
                  </div>
                  <button
                    @click=${() => this._toggleEnabled(index)}
                    class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${entry.enabled ? 'bg-blue-600' : 'bg-slate-300'}"
                  >
                    <span class="inline-block h-4 w-4 rounded-full bg-white transition-transform ${entry.enabled ? 'translate-x-6' : 'translate-x-1'}"></span>
                  </button>
                </div>

                <label class="block space-y-1">
                  <span class="text-sm font-medium text-slate-700">Name</span>
                  <input
                    type="text"
                    .value=${entry.name || ''}
                    @input=${(event) => this._updateEntry(index, 'name', event.target.value)}
                    class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
                  />
                </label>

                <div class="grid gap-3 md:grid-cols-3">
                  <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                    <div class="text-xs font-semibold uppercase tracking-wide text-slate-500">Override</div>
                    <div class="mt-1 font-medium text-slate-800">${this._overrideLabel(runtimeEntry.override_index)}</div>
                  </div>
                  <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                    <div class="text-xs font-semibold uppercase tracking-wide text-slate-500">Signal</div>
                    <div class="mt-1 font-medium text-slate-800">${this._signalStatusLabel(runtimeEntry.signal_status)}</div>
                  </div>
                  <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                    <div class="text-xs font-semibold uppercase tracking-wide text-slate-500">Communication</div>
                    <div class="mt-1 font-medium text-slate-800">${runtimeEntry.com_status ? 'OK' : 'Error'}</div>
                  </div>
                </div>

                <div class="grid gap-4 md:grid-cols-2">
                  ${this._renderSelect('DVB-C Networks', entry.networks ?? 0, NETWORK_OPTIONS, (value) => this._updateEntry(index, 'networks', Number(value)))}
                  ${this._renderSelect('Media Name', entry.ts_filename || 'None', this._mediaOptions(), (value) => this._updateEntry(index, 'ts_filename', value))}
                  ${this._renderSelect('Operation Mode', entry.operation_mode ?? 0, OPERATION_MODE_OPTIONS, (value) => this._updateEntry(index, 'operation_mode', Number(value)))}
                  ${this._renderSelect('Signal Type', entry.signal_type ?? 0, SIGNAL_TYPE_OPTIONS, (value) => this._updateEntry(index, 'signal_type', Number(value)))}
                  ${this._renderSelect('Volume %', entry.volume ?? -1, VOLUME_OPTIONS, (value) => this._updateEntry(index, 'volume', Number(value)))}
                </div>
              </div>
            `;})}
          </div>
        `}

        ${this._renderControlOverlay()}
      </div>
    `;
  }
}

customElements.define('ixui-forced-content', IxuiForcedContent);
