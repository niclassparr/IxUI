import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiCommands extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    unitInfo: { type: Object },
    executing: { type: String },
    confirmCommand: { type: String },
    notification: { type: Object },
    restoreOpen: { type: Boolean },
    restoreBusy: { type: Boolean },
    stagedBackupInfo: { type: Object },
  };

  constructor() {
    super();
    this.unitInfo = null;
    this.executing = null;
    this.confirmCommand = null;
    this.notification = null;
    this.restoreOpen = false;
    this.restoreBusy = false;
    this.stagedBackupInfo = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadContext();
  }

  async _loadContext() {
    try {
      this.unitInfo = await api.getUnitInfo();
    } catch {
      this.unitInfo = null;
    }

    try {
      this.stagedBackupInfo = await api.getStagedBackupInfo();
    } catch {
      this.stagedBackupInfo = null;
    }
  }

  _commands() {
    const commands = [
      {
        id: 'backup',
        label: 'Backup',
        description: 'Generate and download a system backup file.',
        icon: '☁️',
        color: 'blue',
        action: 'download-backup',
      },
      {
        id: 'restore',
        label: 'Restore',
        description: 'Upload a backup file, validate it, and restore the current system state.',
        icon: '📤',
        color: 'blue',
        action: 'restore',
      },
      {
        id: 'document',
        label: 'Document',
        description: 'Generate a downloadable installation document as PDF.',
        icon: '📄',
        color: 'blue',
        action: 'download-document',
      },
      {
        id: 'poweroff',
        label: 'Power Off',
        description: 'Shut down the streamer.',
        icon: '⏻',
        color: 'red',
        action: 'command',
        confirm: true,
      },
      {
        id: 'reboot',
        label: 'Reboot',
        description: 'Restart the streamer. Active services will be interrupted.',
        icon: '🔄',
        color: 'red',
        action: 'command',
        confirm: true,
      },
      {
        id: 'netrestart',
        label: 'Restart Network',
        description: 'Restart the network stack with the current network configuration.',
        icon: '🌐',
        color: 'yellow',
        action: 'command',
        confirm: true,
      },
      {
        id: 'update-interfaces',
        label: 'Update Interfaces',
        description: 'Refresh the interface inventory and pick up added or removed interfaces.',
        icon: '🧩',
        color: 'yellow',
        action: 'command',
        confirm: true,
      },
      {
        id: 'reset',
        label: 'Reset Software',
        description: 'Reset the IxUI software configuration to factory defaults.',
        icon: '⚠️',
        color: 'red',
        action: 'command',
        confirm: true,
      },
      {
        id: 'allstart',
        label: 'Start All Interfaces',
        description: 'Start all configured interfaces.',
        icon: '▶️',
        color: 'green',
        action: 'command',
        confirm: true,
      },
      {
        id: 'allstop',
        label: 'Stop All Interfaces',
        description: 'Stop all configured interfaces.',
        icon: '⏹️',
        color: 'red',
        action: 'command',
        confirm: true,
      },
    ];

    if (this.unitInfo?.software_update) {
      commands.push({
        id: 'software-update',
        label: 'Software Update',
        description: 'Check for available software updates and install selected packages.',
        icon: '📦',
        color: 'blue',
        action: 'navigate-update',
      });
    }

    return commands;
  }

  _getButtonClasses(color) {
    switch (color) {
      case 'red':
        return 'bg-red-600 hover:bg-red-700 text-white';
      case 'yellow':
        return 'bg-amber-500 hover:bg-amber-600 text-white';
      case 'green':
        return 'bg-green-600 hover:bg-green-700 text-white';
      case 'blue':
      default:
        return 'bg-blue-600 hover:bg-blue-700 text-white';
    }
  }

  _getBorderClasses(color) {
    switch (color) {
      case 'red': return 'border-red-200 bg-red-50/30';
      case 'yellow': return 'border-amber-200 bg-amber-50/30';
      case 'green': return 'border-green-200 bg-green-50/30';
      case 'blue':
      default: return 'border-blue-200 bg-blue-50/30';
    }
  }

  _requestCommand(cmd) {
    if (cmd.action === 'download-backup') {
      api.downloadBackup();
      this._showNotification('Backup download started', 'success');
      return;
    }

    if (cmd.action === 'download-document') {
      api.downloadDocumentPdf();
      this._showNotification('PDF document download started', 'success');
      return;
    }

    if (cmd.action === 'restore') {
      this.restoreOpen = !this.restoreOpen;
      return;
    }

    if (cmd.action === 'navigate-update') {
      window.location.hash = '#update';
      return;
    }

    if (cmd.confirm) {
      this.confirmCommand = cmd.id;
    } else {
      this._executeCommand(cmd.id);
    }
  }

  _cancelConfirm() {
    this.confirmCommand = null;
  }

  async _executeCommand(command) {
    this.confirmCommand = null;
    this.executing = command;
    try {
      const response = await api.runCommand(command);
      if (!response?.success) {
        this._showNotification(`Command "${command}" failed: ${response?.error || 'Unknown error'}`, 'error');
        this.executing = null;
        return;
      }

      const messageByCommand = {
        poweroff: 'System powered off successfully',
        reboot: 'System rebooted successfully',
        netrestart: 'Network restart command completed',
        'update-interfaces': 'Interface inventory updated successfully',
        reset: 'Software reset completed successfully',
        allstart: 'All active interfaces started successfully',
        allstop: 'All interfaces stopped successfully',
      };
      this._showNotification(messageByCommand[command] || `Command "${command}" executed successfully`, 'success');
      await this._loadContext();
    } catch (err) {
      this._showNotification(`Command "${command}" failed: ${err.message}`, 'error');
    }
    this.executing = null;
  }

  async _onRestoreFileChange(e) {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }

    this.restoreBusy = true;
    try {
      this.stagedBackupInfo = await api.uploadBackup(file);
      this._showNotification('Backup uploaded and validated successfully', 'success');
    } catch (err) {
      this.stagedBackupInfo = null;
      this._showNotification(`Failed to upload backup: ${err.message}`, 'error');
    }
    this.restoreBusy = false;
  }

  async _applyRestore() {
    this.restoreBusy = true;
    try {
      await api.restoreBackup();
      this._showNotification('Backup restored successfully', 'success');
      this.restoreOpen = false;
    } catch (err) {
      this._showNotification(`Restore failed: ${err.message}`, 'error');
    }
    this.restoreBusy = false;
  }

  _renderRestorePanel() {
    if (!this.restoreOpen) {
      return html``;
    }

    return html`
      <div class="mt-4 p-4 bg-white border border-slate-200 rounded-xl space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-2">Backup File</label>
          <input
            type="file"
            accept=".json,application/json"
            @change=${this._onRestoreFileChange}
            class="block w-full text-sm text-slate-700 file:mr-4 file:px-4 file:py-2 file:border-0 file:rounded-lg file:bg-blue-600 file:text-white hover:file:bg-blue-700"
          />
        </div>

        ${this.stagedBackupInfo ? html`
          <div class="text-sm text-slate-700 space-y-1">
            <div><span class="font-medium">Serial:</span> ${this.stagedBackupInfo.serial || '—'}</div>
            <div><span class="font-medium">Backup Date:</span> ${this.stagedBackupInfo.backup_date || '—'}</div>
            <div><span class="font-medium">Interfaces:</span> ${this.stagedBackupInfo.interface_count ?? 0}</div>
            <div><span class="font-medium">Routes:</span> ${this.stagedBackupInfo.route_count ?? 0}</div>
            <div><span class="font-medium">Settings:</span> ${this.stagedBackupInfo.setting_count ?? 0}</div>
          </div>
        ` : html`
          <p class="text-sm text-slate-500">Upload a backup file to validate it before restore.</p>
        `}

        <div class="flex gap-2">
          <button
            @click=${this._applyRestore}
            class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 transition disabled:opacity-50"
            ?disabled=${this.restoreBusy || !this.stagedBackupInfo?.valid}
          >${this.restoreBusy ? 'Restoring...' : 'Apply Restore'}</button>
          <button
            @click=${() => { this.restoreOpen = false; }}
            class="px-4 py-2 text-sm bg-white border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition"
          >Close</button>
        </div>
      </div>
    `;
  }

  _showNotification(message, type = 'info') {
    this.notification = { message, type };
    setTimeout(() => { this.notification = null; }, 5000);
  }

  render() {
    return html`
      <div class="space-y-6">
        <div>
          <h1 class="text-2xl font-bold text-slate-800">Commands</h1>
          <p class="text-slate-500 text-sm">Operational tools, export workflows, and system commands</p>
        </div>

        <!-- Notification -->
        ${this.notification ? html`
          <div class="p-3 rounded-lg text-sm font-medium
            ${this.notification.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' : ''}
            ${this.notification.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : ''}">
            ${this.notification.message}
          </div>
        ` : ''}

        <!-- Command Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          ${this._commands().map(cmd => html`
            <div class="bg-white rounded-xl shadow-sm border ${this._getBorderClasses(cmd.color)} p-6">
              <div class="flex items-start gap-4">
                <span class="text-3xl">${cmd.icon}</span>
                <div class="flex-1">
                  <h3 class="text-lg font-semibold text-slate-800">${cmd.label}</h3>
                  <p class="text-sm text-slate-500 mt-1 mb-4">${cmd.description}</p>

                  ${this.confirmCommand === cmd.id ? html`
                    <!-- Confirmation -->
                    <div class="p-3 bg-red-50 border border-red-200 rounded-lg mb-3">
                      <p class="text-sm text-red-800 font-medium mb-2">Are you sure you want to execute this command?</p>
                      <div class="flex gap-2">
                        <button
                          @click=${() => this._executeCommand(cmd.id)}
                          class="px-3 py-1.5 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
                        >Yes, Execute</button>
                        <button
                          @click=${this._cancelConfirm}
                          class="px-3 py-1.5 text-sm bg-white border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition"
                        >Cancel</button>
                      </div>
                    </div>
                  ` : html`
                    <button
                      @click=${() => this._requestCommand(cmd)}
                      class="px-4 py-2 text-sm rounded-lg font-medium transition disabled:opacity-50 disabled:cursor-not-allowed ${this._getButtonClasses(cmd.color)}"
                      ?disabled=${this.executing != null}
                    >
                      ${this.executing === cmd.id ? html`
                        <span class="inline-flex items-center gap-2">
                          <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                          </svg>
                          Executing...
                        </span>
                      ` : `Execute`}
                    </button>
                  `}

                  ${cmd.id === 'restore' ? this._renderRestorePanel() : ''}
                </div>
              </div>
            </div>
          `)}
        </div>

        <!-- Warning -->
        <div class="p-4 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-800">
          <span class="font-semibold">⚠ Warning:</span> Some commands may cause service interruptions. Use with caution.
        </div>
      </div>
    `;
  }
}

customElements.define('ixui-commands', IxuiCommands);
