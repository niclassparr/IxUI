import { LitElement, html } from 'lit';
import * as api from '../services/api.js';

class IxuiCommands extends LitElement {
  createRenderRoot() { return this; }

  static properties = {
    executing: { type: String },
    confirmCommand: { type: String },
    notification: { type: Object },
  };

  constructor() {
    super();
    this.executing = null;
    this.confirmCommand = null;
    this.notification = null;
  }

  _commands() {
    return [
      {
        id: 'reboot',
        label: 'Reboot System',
        description: 'Restart the entire system. All services will be interrupted.',
        icon: '🔄',
        color: 'red',
        confirm: true,
      },
      {
        id: 'restart-services',
        label: 'Restart Services',
        description: 'Restart all running services without a full system reboot.',
        icon: '🔃',
        color: 'yellow',
        confirm: true,
      },
      {
        id: 'factory-reset',
        label: 'Factory Reset',
        description: 'Reset all settings to factory defaults. This cannot be undone!',
        icon: '⚠️',
        color: 'red',
        confirm: true,
      },
      {
        id: 'update-epg',
        label: 'Update EPG',
        description: 'Trigger an Electronic Program Guide update.',
        icon: '📡',
        color: 'blue',
        confirm: false,
      },
    ];
  }

  _getButtonClasses(color) {
    switch (color) {
      case 'red':
        return 'bg-red-600 hover:bg-red-700 text-white';
      case 'yellow':
        return 'bg-amber-500 hover:bg-amber-600 text-white';
      case 'blue':
      default:
        return 'bg-blue-600 hover:bg-blue-700 text-white';
    }
  }

  _getBorderClasses(color) {
    switch (color) {
      case 'red': return 'border-red-200 bg-red-50/30';
      case 'yellow': return 'border-amber-200 bg-amber-50/30';
      case 'blue':
      default: return 'border-blue-200 bg-blue-50/30';
    }
  }

  _requestCommand(cmd) {
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
      await api.runCommand(command);
      this._showNotification(`Command "${command}" executed successfully`, 'success');
    } catch (err) {
      this._showNotification(`Command "${command}" failed: ${err.message}`, 'error');
    }
    this.executing = null;
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
          <p class="text-slate-500 text-sm">Execute system commands</p>
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
