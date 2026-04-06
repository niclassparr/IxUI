import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

const SECTION_CARD_CLASS = 'rounded-xl border border-slate-200 bg-white p-5 shadow-sm';
const TEXT_INPUT_CLASS = 'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500';
const SELECT_CLASS = TEXT_INPUT_CLASS;
const PRIMARY_BUTTON_CLASS = 'rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50';
const SECONDARY_BUTTON_CLASS = 'rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50';

const FREQUENCY_OPTIONS = Array.from({ length: 94 }, (_, index) => {
  const mhz = 114 + index * 8;
  return { value: String(mhz * 1000000), label: `${mhz} MHz` };
});

const SYMBOL_RATE_OPTIONS = [
  { value: '6900000', label: '6900000' },
  { value: '6875000', label: '6875000' },
];

const QAM_OPTIONS = ['QAM-64', 'QAM-128', 'QAM-256'];
const ATTENUATION_OPTIONS = Array.from({ length: 31 }, (_, index) => String(index));
const AUDIO_FORMAT_OPTIONS = ['passthrough', 'aac', 'mp2'];
const TOGGLE_OPTIONS = [
  { value: 'true', label: 'Enabled' },
  { value: 'false', label: 'Disabled' },
];

const BASE_SETTING_KEYS = [
  'dvbc_enable',
  'dvbc_freq',
  'dvbc_symb',
  'dvbc_qam',
  'dvbc_attenuation',
  'dvbc_netid',
  'dvbc_orgnetid',
  'dvbc_netname',
  'dvbc_net2_enable',
  'dvbc_net2_freq',
  'dvbc_net2_symb',
  'dvbc_net2_qam',
  'dvbc_net2_attenuation',
  'dvbc_net2_netid',
  'dvbc_net2_orgnetid',
  'dvbc_net2_netname',
  'ip_enable',
  'ip_startaddr',
  'nw_multicastdev',
  'ip_ttl',
  'ip_tos',
  'dsc_services',
  'dsc_bitrate',
  'bitrate_tvsd',
  'bitrate_tvhd',
  'bitrate_radio',
];

const HLS_SETTING_KEYS = [
  'hls_enable',
  'hls_server_ip',
  'hls_inport',
  'hls_outport',
  'hls_services',
  'hls_playback_prefix',
  'hls_max_bitrate',
  'hls_ba_enable',
  'hls_ba_user',
  'hls_ba_passwd',
  'remux_enable',
  'remux_audio_format',
  'remux_audio_offset',
  'remux_muxrate',
];

const PORTAL_SETTING_KEYS = ['portal_enable', 'portal_server_ip', 'portal_url'];
const CLOUD_SETTING_KEYS = ['ixcloud_enable', 'ixcloud_validate_url'];
const FORCED_CONTENT_SETTING_KEYS = ['forced_content_enable'];

function indexSettings(settings = []) {
  return Object.fromEntries(settings.map((setting) => [setting.name, setting]));
}

class IxuiSettings extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    settings: { type: Object },
    unitInfo: { type: Object },
    dateTimeForm: { type: Object },
    timezones: { type: Array },
    modulators: { type: Array },
    passwordForm: { type: Object },
    loading: { type: Boolean },
    saving: { type: Boolean },
    dateTimeSaving: { type: Boolean },
    modulatorSaving: { type: Boolean },
    passwordSaving: { type: Boolean },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.settings = {};
    this.unitInfo = {};
    this.dateTimeForm = { timezone: 'UTC', ntpEnabled: true, date: '', time: '' };
    this.timezones = [];
    this.modulators = [];
    this.passwordForm = { oldPassword: '', newPassword: '', verifyPassword: '' };
    this.loading = true;
    this.saving = false;
    this.dateTimeSaving = false;
    this.modulatorSaving = false;
    this.passwordSaving = false;
    this.notification = null;
    this._notificationTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadPage();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._notificationTimer) {
      clearTimeout(this._notificationTimer);
    }
  }

  async _loadPage() {
    this.loading = true;
    try {
      const [settings, unitInfo, dateTimeState, modulators] = await Promise.all([
        api.getSettings(),
        api.getUnitInfo(),
        api.getDateTimeSettings(),
        api.getModulators(),
      ]);

      this.settings = indexSettings(settings || []);
      this.unitInfo = unitInfo || {};
      this.timezones = dateTimeState?.timezones || [];
      this.dateTimeForm = {
        timezone: dateTimeState?.timezone || 'UTC',
        ntpEnabled: dateTimeState?.ntp_enabled ?? true,
        date: dateTimeState?.current_date || '',
        time: dateTimeState?.current_time || '',
      };
      this.modulators = modulators || [];
    } catch {
      this.settings = {};
      this.unitInfo = {};
      this.timezones = [];
      this.modulators = [];
      this._showNotification('Failed to load settings data.', 'error');
    } finally {
      this.loading = false;
    }
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    if (this._notificationTimer) {
      clearTimeout(this._notificationTimer);
    }
    this._notificationTimer = setTimeout(() => {
      this.notification = null;
    }, 4000);
  }

  _settingValue(name, fallback = '') {
    return this.settings[name]?.value ?? fallback;
  }

  _settingEnabled(name) {
    return String(this._settingValue(name, 'false')).toLowerCase() === 'true';
  }

  _updateSetting(name, value) {
    const current = this.settings[name] || { id: 0, name, value: '' };
    this.settings = {
      ...this.settings,
      [name]: { ...current, value: value ?? '' },
    };
  }

  _updateDateTimeField(field, value) {
    this.dateTimeForm = { ...this.dateTimeForm, [field]: value };
  }

  _updatePasswordField(field, value) {
    this.passwordForm = { ...this.passwordForm, [field]: value };
  }

  _updateModulator(index, networkNum) {
    const next = [...this.modulators];
    next[index] = { ...next[index], network_num: Number(networkNum) };
    this.modulators = next;
  }

  _networkDevices() {
    const devices = Object.keys(this.settings)
      .map((name) => {
        const match = name.match(/^nw_(eth\d+)_onboot$/);
        return match ? match[1] : null;
      })
      .filter(Boolean)
      .sort((left, right) => left.localeCompare(right, undefined, { numeric: true }));
    return devices.length > 0 ? devices : ['eth0'];
  }

  _multicastRouteParts() {
    const value = this._settingValue('nw_multicastdev', this._networkDevices()[0] || 'eth0');
    const [device, vlan = ''] = String(value).split('.', 2);
    return {
      device: device || this._networkDevices()[0] || 'eth0',
      vlan,
    };
  }

  _updateMulticastRoute(field, value) {
    const current = this._multicastRouteParts();
    const next = { ...current, [field]: value };
    const combined = next.vlan ? `${next.device}.${next.vlan}` : next.device;
    this._updateSetting('nw_multicastdev', combined);
  }

  _showHlsSection() {
    return this.unitInfo?.hls_output || 'hls_enable' in this.settings;
  }

  _showPortalSection() {
    return this.unitInfo?.portal || 'portal_enable' in this.settings;
  }

  _showCloudSection() {
    return this.unitInfo?.cloud || 'ixcloud_enable' in this.settings;
  }

  _showForcedContentSection() {
    return this.unitInfo?.forced_content || 'forced_content_enable' in this.settings;
  }

  _normalizeOptions(options) {
    return (options || []).map((option) => (
      typeof option === 'string' ? { value: option, label: option } : option
    ));
  }

  _renderField(field) {
    if (field.when && !field.when(this)) {
      return '';
    }

    const value = this._settingValue(field.key, field.defaultValue || '');
    const options = typeof field.options === 'function' ? field.options(this) : field.options;
    const normalizedOptions = this._normalizeOptions(options);

    if (field.type === 'toggle') {
      return html`
        <label class="flex items-center justify-between gap-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
          <div>
            <div class="text-sm font-medium text-slate-800">${field.label}</div>
            ${field.description ? html`<div class="text-xs text-slate-500">${field.description}</div>` : ''}
          </div>
          <input
            type="checkbox"
            class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
            .checked=${String(value).toLowerCase() === 'true'}
            @change=${(event) => this._updateSetting(field.key, event.target.checked ? 'true' : 'false')}
          />
        </label>
      `;
    }

    if (field.type === 'select') {
      return html`
        <label class="block space-y-1">
          <span class="text-sm font-medium text-slate-700">${field.label}</span>
          <select
            class=${SELECT_CLASS}
            .value=${value}
            @change=${(event) => this._updateSetting(field.key, event.target.value)}
          >
            ${normalizedOptions.map((option) => html`
              <option value=${option.value}>${option.label}</option>
            `)}
          </select>
        </label>
      `;
    }

    return html`
      <label class="block space-y-1">
        <span class="text-sm font-medium text-slate-700">${field.label}</span>
        <input
          type=${field.type || 'text'}
          class=${TEXT_INPUT_CLASS}
          .value=${value}
          @input=${(event) => this._updateSetting(field.key, event.target.value)}
        />
      </label>
    `;
  }

  _collectSettingsPayload() {
    const keys = new Set(BASE_SETTING_KEYS);
    if (this._showHlsSection()) {
      HLS_SETTING_KEYS.forEach((key) => keys.add(key));
    }
    if (this._showPortalSection()) {
      PORTAL_SETTING_KEYS.forEach((key) => keys.add(key));
    }
    if (this._showCloudSection()) {
      CLOUD_SETTING_KEYS.forEach((key) => keys.add(key));
    }
    if (this._showForcedContentSection()) {
      FORCED_CONTENT_SETTING_KEYS.forEach((key) => keys.add(key));
    }

    return [...keys].map((key) => this.settings[key] || { id: 0, name: key, value: '' });
  }

  async _saveSettings() {
    this.saving = true;
    try {
      await api.updateSettings(this._collectSettingsPayload());
      this._showNotification('Settings saved successfully.', 'success');
    } catch {
      this._showNotification('Failed to save settings.', 'error');
    } finally {
      this.saving = false;
    }
  }

  async _saveDateTime() {
    this.dateTimeSaving = true;
    try {
      await api.updateDateTimeSettings({
        timezone: this.dateTimeForm.timezone,
        ntp_enabled: this.dateTimeForm.ntpEnabled,
        date: this.dateTimeForm.ntpEnabled ? null : this.dateTimeForm.date,
        time: this.dateTimeForm.ntpEnabled ? null : this.dateTimeForm.time,
      });
      this._showNotification('Date and time settings saved.', 'success');
      const state = await api.getDateTimeSettings();
      this.timezones = state?.timezones || this.timezones;
      this.dateTimeForm = {
        timezone: state?.timezone || this.dateTimeForm.timezone,
        ntpEnabled: state?.ntp_enabled ?? this.dateTimeForm.ntpEnabled,
        date: state?.current_date || this.dateTimeForm.date,
        time: state?.current_time || this.dateTimeForm.time,
      };
    } catch {
      this._showNotification('Failed to save date and time settings.', 'error');
    } finally {
      this.dateTimeSaving = false;
    }
  }

  async _saveModulators() {
    this.modulatorSaving = true;
    try {
      await api.updateModulators(this.modulators);
      this._showNotification('Modulator assignments saved.', 'success');
    } catch {
      this._showNotification('Failed to save modulator assignments.', 'error');
    } finally {
      this.modulatorSaving = false;
    }
  }

  async _savePassword() {
    const { oldPassword, newPassword, verifyPassword } = this.passwordForm;
    if (!oldPassword || !newPassword || !verifyPassword) {
      this._showNotification('Please fill in all password fields.', 'error');
      return;
    }
    if (newPassword !== verifyPassword) {
      this._showNotification('New passwords do not match.', 'error');
      return;
    }

    this.passwordSaving = true;
    try {
      const response = await api.changePassword(oldPassword, newPassword);
      if (!response?.success) {
        this._showNotification(response?.error || 'Failed to change password.', 'error');
        return;
      }
      this.passwordForm = { oldPassword: '', newPassword: '', verifyPassword: '' };
      this._showNotification('Password changed successfully.', 'success');
    } catch (error) {
      this._showNotification(error?.message || 'Failed to change password.', 'error');
    } finally {
      this.passwordSaving = false;
    }
  }

  _renderNotification() {
    if (!this.notification) {
      return '';
    }
    const tone = this.notification.type === 'success'
      ? 'border-green-200 bg-green-50 text-green-800'
      : 'border-red-200 bg-red-50 text-red-800';
    return html`
      <div class=${`rounded-lg border px-4 py-3 text-sm font-medium ${tone}`}>
        ${this.notification.message}
      </div>
    `;
  }

  _renderSection(title, description, fields) {
    return html`
      <section class=${SECTION_CARD_CLASS}>
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-slate-800">${title}</h2>
          <p class="text-sm text-slate-500">${description}</p>
        </div>
        <div class="grid gap-4 md:grid-cols-2">
          ${fields.map((field) => this._renderField(field))}
        </div>
      </section>
    `;
  }

  _renderDateTimeCard() {
    return html`
      <section class=${SECTION_CARD_CLASS}>
        <div class="mb-4 flex items-start justify-between gap-4">
          <div>
            <h2 class="text-lg font-semibold text-slate-800">Date and Time</h2>
            <p class="text-sm text-slate-500">Update timezone and clock mode. A timezone change may require a streamer restart.</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            Current unit time: <span class="font-medium text-slate-800">${this.dateTimeForm.timezone}</span>
            <span class="ml-2 font-mono text-slate-700">${this.dateTimeForm.date} ${this.dateTimeForm.time}</span>
          </div>
          <label class="block space-y-1">
            <span class="text-sm font-medium text-slate-700">Timezone</span>
            <select
              class=${SELECT_CLASS}
              .value=${this.dateTimeForm.timezone}
              @change=${(event) => this._updateDateTimeField('timezone', event.target.value)}
            >
              ${this.timezones.map((timezone) => html`
                <option value=${timezone}>${timezone}</option>
              `)}
            </select>
          </label>
          <label class="block space-y-1">
            <span class="text-sm font-medium text-slate-700">Clock Mode</span>
            <select
              class=${SELECT_CLASS}
              .value=${this.dateTimeForm.ntpEnabled ? 'ntp' : 'local'}
              @change=${(event) => this._updateDateTimeField('ntpEnabled', event.target.value === 'ntp')}
            >
              <option value="ntp">NTP</option>
              <option value="local">Local</option>
            </select>
          </label>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="block space-y-1">
              <span class="text-sm font-medium text-slate-700">Date</span>
              <input
                type="date"
                class=${TEXT_INPUT_CLASS}
                .value=${this.dateTimeForm.date}
                ?disabled=${this.dateTimeForm.ntpEnabled}
                @input=${(event) => this._updateDateTimeField('date', event.target.value)}
              />
            </label>
            <label class="block space-y-1">
              <span class="text-sm font-medium text-slate-700">Time</span>
              <input
                type="time"
                class=${TEXT_INPUT_CLASS}
                .value=${this.dateTimeForm.time}
                ?disabled=${this.dateTimeForm.ntpEnabled}
                @input=${(event) => this._updateDateTimeField('time', event.target.value)}
              />
            </label>
          </div>
          <button
            class=${PRIMARY_BUTTON_CLASS}
            @click=${this._saveDateTime}
            ?disabled=${this.dateTimeSaving}
          >${this.dateTimeSaving ? 'Saving...' : 'Save Date and Time'}</button>
        </div>
      </section>
    `;
  }

  _renderModulatorsCard() {
    return html`
      <section class=${SECTION_CARD_CLASS}>
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-slate-800">Modulator Net Settings</h2>
          <p class="text-sm text-slate-500">Assign each modulator output to none, Net 1, or Net 2.</p>
        </div>
        ${this.modulators.length === 0 ? html`
          <div class="rounded-lg border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">
            No modulator interfaces were found.
          </div>
        ` : html`
          <div class="space-y-3">
            ${this.modulators.map((assignment, index) => html`
              <div class="grid gap-3 md:grid-cols-[1fr,160px] md:items-center">
                <div class="text-sm font-medium text-slate-700">${assignment.interface_pos}</div>
                <select
                  class=${SELECT_CLASS}
                  .value=${String(assignment.network_num ?? 0)}
                  @change=${(event) => this._updateModulator(index, event.target.value)}
                >
                  <option value="0">None</option>
                  <option value="1">Net 1</option>
                  <option value="2">Net 2</option>
                </select>
              </div>
            `)}
            <button
              class=${PRIMARY_BUTTON_CLASS}
              @click=${this._saveModulators}
              ?disabled=${this.modulatorSaving}
            >${this.modulatorSaving ? 'Saving...' : 'Save Modulator Settings'}</button>
          </div>
        `}
      </section>
    `;
  }

  _renderPasswordCard() {
    return html`
      <section class=${SECTION_CARD_CLASS}>
        <div class="mb-4">
          <h2 class="text-lg font-semibold text-slate-800">Change Password</h2>
          <p class="text-sm text-slate-500">Update the password for the currently logged-in user.</p>
        </div>
        <div class="space-y-4">
          <label class="block space-y-1">
            <span class="text-sm font-medium text-slate-700">Old Password</span>
            <input
              type="password"
              class=${TEXT_INPUT_CLASS}
              .value=${this.passwordForm.oldPassword}
              @input=${(event) => this._updatePasswordField('oldPassword', event.target.value)}
            />
          </label>
          <label class="block space-y-1">
            <span class="text-sm font-medium text-slate-700">New Password</span>
            <input
              type="password"
              class=${TEXT_INPUT_CLASS}
              .value=${this.passwordForm.newPassword}
              @input=${(event) => this._updatePasswordField('newPassword', event.target.value)}
            />
          </label>
          <label class="block space-y-1">
            <span class="text-sm font-medium text-slate-700">Verify New Password</span>
            <input
              type="password"
              class=${TEXT_INPUT_CLASS}
              .value=${this.passwordForm.verifyPassword}
              @input=${(event) => this._updatePasswordField('verifyPassword', event.target.value)}
            />
          </label>
          <button
            class=${PRIMARY_BUTTON_CLASS}
            @click=${this._savePassword}
            ?disabled=${this.passwordSaving}
          >${this.passwordSaving ? 'Saving...' : 'Change Password'}</button>
        </div>
      </section>
    `;
  }

  render() {
    if (this.loading) {
      return html`
        <div class="flex items-center justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent"></div>
        </div>
      `;
    }

    const multicast = this._multicastRouteParts();

    const dvbcNet1Fields = [
      { key: 'dvbc_enable', label: 'Enabled', type: 'toggle' },
      { key: 'dvbc_freq', label: 'Start Frequency', type: 'select', options: FREQUENCY_OPTIONS },
      { key: 'dvbc_symb', label: 'Symbol Rate', type: 'select', options: SYMBOL_RATE_OPTIONS },
      { key: 'dvbc_qam', label: 'QAM Constellation', type: 'select', options: QAM_OPTIONS },
      { key: 'dvbc_attenuation', label: 'Attenuation (dB)', type: 'select', options: ATTENUATION_OPTIONS },
      { key: 'dvbc_netid', label: 'Network ID' },
      { key: 'dvbc_orgnetid', label: 'Original Network ID' },
      { key: 'dvbc_netname', label: 'Network Name' },
    ];

    const dvbcNet2Fields = [
      { key: 'dvbc_net2_enable', label: 'Enabled', type: 'toggle' },
      { key: 'dvbc_net2_freq', label: 'Start Frequency', type: 'select', options: FREQUENCY_OPTIONS },
      { key: 'dvbc_net2_symb', label: 'Symbol Rate', type: 'select', options: SYMBOL_RATE_OPTIONS },
      { key: 'dvbc_net2_qam', label: 'QAM Constellation', type: 'select', options: QAM_OPTIONS },
      { key: 'dvbc_net2_attenuation', label: 'Attenuation (dB)', type: 'select', options: ATTENUATION_OPTIONS },
      { key: 'dvbc_net2_netid', label: 'Network ID' },
      { key: 'dvbc_net2_orgnetid', label: 'Original Network ID' },
      { key: 'dvbc_net2_netname', label: 'Network Name' },
    ];

    const descramblerFields = [
      { key: 'dsc_services', label: 'Max Services' },
      { key: 'dsc_bitrate', label: 'Max Bitrate (bps)' },
      { key: 'bitrate_tvsd', label: 'TV SD Estimate (bps)' },
      { key: 'bitrate_tvhd', label: 'TV HD Estimate (bps)' },
      { key: 'bitrate_radio', label: 'Radio Estimate (bps)' },
    ];

    const hlsFields = [
      { key: 'hls_enable', label: 'Enabled', type: 'toggle' },
      { key: 'hls_server_ip', label: 'Server IP' },
      { key: 'hls_inport', label: 'Inport' },
      { key: 'hls_outport', label: 'Outport' },
      { key: 'hls_services', label: 'Max Services' },
      { key: 'hls_playback_prefix', label: 'Playback Prefix' },
      { key: 'hls_max_bitrate', label: 'Max Bitrate (bps)' },
      { key: 'hls_ba_enable', label: 'Basic Auth Enabled', type: 'toggle' },
      { key: 'hls_ba_user', label: 'Basic Auth User', when: (component) => component._settingEnabled('hls_ba_enable') },
      { key: 'hls_ba_passwd', label: 'Basic Auth Password', when: (component) => component._settingEnabled('hls_ba_enable') },
      { key: 'remux_enable', label: 'Remux Enabled', type: 'toggle' },
      { key: 'remux_audio_format', label: 'Remux Audio Format', type: 'select', options: AUDIO_FORMAT_OPTIONS, when: (component) => component._settingEnabled('remux_enable') },
      { key: 'remux_audio_offset', label: 'Audio Offset (ms)', when: (component) => component._settingEnabled('remux_enable') },
      { key: 'remux_muxrate', label: 'Multiplexer Bitrate (bps)', when: (component) => component._settingEnabled('remux_enable') },
    ];

    const portalFields = [
      { key: 'portal_enable', label: 'Enabled', type: 'toggle' },
      { key: 'portal_server_ip', label: 'Server IP' },
      { key: 'portal_url', label: 'Portal URL' },
    ];

    const cloudFields = [
      { key: 'ixcloud_enable', label: 'Enabled', type: 'toggle' },
      { key: 'ixcloud_validate_url', label: 'Cloud URL' },
    ];

    const forcedContentFields = [
      { key: 'forced_content_enable', label: 'Enabled', type: 'toggle' },
    ];

    return html`
      <div class="space-y-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 class="text-2xl font-bold text-slate-800">Settings</h1>
            <p class="text-sm text-slate-500">Grouped legacy settings, date and time, password, and modulator flows.</p>
          </div>
          <div class="flex gap-3">
            <button class=${SECONDARY_BUTTON_CLASS} @click=${this._loadPage}>Refresh</button>
            <button class=${PRIMARY_BUTTON_CLASS} @click=${this._saveSettings} ?disabled=${this.saving}>
              ${this.saving ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </div>

        ${this._renderNotification()}

        <div class="grid gap-6 xl:grid-cols-3">
          ${this._renderDateTimeCard()}
          ${this._renderModulatorsCard()}
          ${this._renderPasswordCard()}
        </div>

        <section class=${SECTION_CARD_CLASS}>
          <div class="mb-4">
            <h2 class="text-lg font-semibold text-slate-800">IP Output</h2>
            <p class="text-sm text-slate-500">Configure multicast output, TTL/TOS, and the routed interface.</p>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            ${this._renderField({ key: 'ip_enable', label: 'Enabled', type: 'toggle' })}
            ${this._renderField({ key: 'ip_startaddr', label: 'Multicast Start Address' })}
            <label class="block space-y-1">
              <span class="text-sm font-medium text-slate-700">Network Interface</span>
              <select
                class=${SELECT_CLASS}
                .value=${multicast.device}
                @change=${(event) => this._updateMulticastRoute('device', event.target.value)}
              >
                ${this._networkDevices().map((device) => html`
                  <option value=${device}>${device}</option>
                `)}
              </select>
            </label>
            <label class="block space-y-1">
              <span class="text-sm font-medium text-slate-700">VLAN ID (Optional)</span>
              <input
                type="text"
                class=${TEXT_INPUT_CLASS}
                .value=${multicast.vlan}
                @input=${(event) => this._updateMulticastRoute('vlan', event.target.value)}
              />
            </label>
            ${this._renderField({ key: 'ip_ttl', label: 'Time To Live (TTL)' })}
            ${this._renderField({ key: 'ip_tos', label: 'Type Of Service (TOS)' })}
          </div>
        </section>

        <div class="grid gap-6 xl:grid-cols-2">
          ${this._renderSection('DVB-C Net 1', 'Primary DVB-C output parameters.', dvbcNet1Fields)}
          ${this._renderSection('DVB-C Net 2', 'Secondary DVB-C output parameters.', dvbcNet2Fields)}
          ${this._renderSection('Descrambler and Bitrates', 'Limits and bitrate estimation used by layout planning.', descramblerFields)}
          ${this._showHlsSection() ? this._renderSection('HLS Output and Remux', 'HLS transport, basic auth, and remux compatibility controls.', hlsFields) : ''}
          ${this._showPortalSection() ? this._renderSection('Portal', 'Portal publication settings.', portalFields) : ''}
          ${this._showCloudSection() ? this._renderSection('Cloud', 'Cloud validation endpoint and enablement.', cloudFields) : ''}
          ${this._showForcedContentSection() ? this._renderSection('Forced Content', 'Enable or disable forced content support.', forcedContentFields) : ''}
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-settings', IxuiSettings);
