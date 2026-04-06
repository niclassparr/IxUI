# Feature Parity Gap Report

## Scope

This report compares the legacy GWT application against the current FastAPI + Lit application.

Compared sources:

- legacy client and server under `GWT_APP/src/se/ixanon/ixui/...`
- new client under `app/frontend/src/...`
- new backend under `app/backend/...`

## Missing Pages

### User-facing page shells

- No legacy page is completely absent in the new app.
- The page mapping is:
  - `dashboard` -> `front-page`
  - `interfaces` -> `interfaces`
  - `interface-edit` -> `interface-edit`
  - `interface-status` -> `interface-status`
  - `interface-log` -> `interface-log`
  - `layout` -> `routes`
  - `settings` -> `settings`
  - `network` -> `network`
  - `commands` -> `commands`
  - `update` -> `update`
  - `cloud` -> `cloud`
  - `hls-wizard` -> `hls-wizard`
  - `force-content` -> `forced-content`

### Exact route-token parity

- Exact legacy tokens missing in the new app:
  - `dashboard` renamed to `front-page`
  - `layout` renamed to `routes`
  - `force-content` renamed to `forced-content`

These are route-name changes, not missing functional pages.

## Global And Cross-Page Gaps

### Missing application-shell functionality

- Missing push-config workflow:
  - legacy shell polls `isConfigChanged(sessionKey)` every 5 seconds and exposes a push-config banner/button
  - new app has no equivalent UI, no config-changed polling, and no `pushConfig` backend endpoint
- Missing permission augmentation step:
  - legacy shell calls `checkPersmission(...)` after session validation
  - new app validates session only and does not reproduce this step
- Missing authenticated download/upload shell flows:
  - legacy shell and dialogs drive backup download, restore upload/validation, and PDF export
  - new app has no equivalent UI flows

### Mock-only runtime data in the new app

- `get_network_status()` and `get_network_status2()` return static fixture data
- `get_streamer_status()` returns random mock values
- `get_tuner_status()` returns random mock values
- `get_interface_log()` returns static in-memory log strings
- `get_update_packages()` and `update_packages()` are mock-only package flows
- `interface_scan()` is a success stub and does not reproduce the legacy scan pipeline

These pages exist, but their runtime behavior is not equivalent to the legacy app.

## Page Parity Matrix

| Legacy page | New page | Status | Key gaps |
| --- | --- | --- | --- |
| `dashboard` | `front-page` | renamed but equivalent | no major missing legacy behavior; new page is broader than legacy |
| `interfaces` | `interfaces` | partial | missing type filter menu, missing 5-second polling, missing multiband marker behavior, missing exact config-link gating |
| `interface-edit` | `interface-edit` | partial | missing multiband type switching, missing EMM workflow, missing scan timestamp, missing service editing and save-services flow, missing infoch path, missing several type-specific forms |
| `interface-status` | `interface-status` | partial | missing start/stop/log shortcuts on page, missing CI menu workflow, missing per-type status tables, missing real backend metrics |
| `interface-log` | `interface-log` | partial | UI exists, but backend log source is mock/static rather than live streamer log |
| `layout` | `routes` | partial | missing editable route table, HLS wizard entry from page, bitrate usage summary, duplicate detection, save workflow |
| `settings` | `settings` | partial | missing grouped/settings-specific UI, missing date-time dialog, missing modulator dialog, missing password-change dialog, missing toggle-driven sections |
| `network` | `network` | partial | missing editable network config, missing IP status dialog, missing save/apply/reboot workflow, missing real network reachability checks |
| `commands` | `commands` | partial | missing backup, restore, PDF export, power off, restart network, update interfaces, reset software, start all interfaces, stop all interfaces, software-update launcher |
| `update` | `update` | partial | missing `check-sw` discovery step, missing real installer behavior, missing file-backed result flow |
| `cloud` | `cloud` | partial | missing connect/disconnect actions, missing 5-second polling, missing legacy cloud fields and enable/online semantics |
| `hls-wizard` | `hls-wizard` | behavior drift | frontend loads interfaces instead of services, missing scan/filter/capacity workflow, save payload shape does not match legacy intent |
| `force-content` | `forced-content` | partial | missing media/network/mode/signal/volume fields, missing control dialog, missing live status polling, missing override-status workflow |

## Detailed Missing Functionality By Page

### Interfaces

- Missing type-filter menu populated from `getInterfaceTypes()`.
- Missing automatic 5-second refresh of interface status.
- Missing legacy row semantics such as multiband `M` prefix and error-row styling.
- New page expands rows inline, but does not reproduce legacy menu/filter/status-list behavior exactly.

### Interface Edit

- Missing multiband type-switch workflow using `updateInterfaceMultibandType(...)`.
- Missing EMM list loading and EMM selection via `getCurrentEmmList(...)`.
- Missing scan-date display from `getInterfaceScanTime(...)`.
- Missing `Save Services` UI and service-table editing.
- Missing service field editing for:
  - type
  - language
  - EPG
  - radio URL
  - show presentation
  - HLS/webradio/infoch URL handling
- Missing info channel-specific load/save flow via `getInterfaceInfoch(...)` and `setInterfaceInfoch(...)`.
- Missing several legacy interface-type forms or exact field handling for:
  - `dsc`
  - `infostreamer`
  - `dvbhdmi`
  - `hdmi2ip`
  - `hls2ip`
  - `webradio`
- Scan flow is behaviorally incomplete:
  - legacy saves config, applies it, waits for scan dialog readiness, then loads scan results
  - new page only calls `startScan()` and reloads after a fixed timeout

### Interface Status

- Missing `Start` button behavior using `interfaceCommand(..., "stream")`.
- Missing `Stop` button behavior using `interfaceCommand(..., "stop")`.
- Missing on-page `Log` action.
- Missing DSC `CI Menu` button and menu answer flow.
- Missing interface summary panel semantics from the legacy view.
- Missing per-type streamer tables for:
  - normal DVB and modulator interfaces
  - descramblers
  - HLS2IP
  - Webradio
  - Info channel
- Missing mux-load and descrambler-usage displays.
- New backend status data is mocked/random, not real streamer/tuner state.

### Interface Log

- UI parity is close, but backend parity is not:
  - legacy log comes from live streamer-side log retrieval
  - new log comes from `_INTERFACE_LOGS` mock data or fallback text

### Layout / Routes

- Missing editable route fields:
  - output/service name
  - LCN
  - descrambler
  - modulator net 1
  - modulator net 2
  - out SID
  - out IP
  - EPG
  - HLS enable checkbox
- Missing feature-dependent columns based on `dvbc`, `dvbc_net2`, `ip`, `hls`, `portal`.
- Missing config-link navigation from route row back to `interface-edit`.
- Missing HLS wizard button in the page header.
- Missing bitrate/capacity usage summary table.
- Missing duplicate SID/IP validation and warning dialog.
- Missing save action even though backend `update_routes()` exists.

### Settings

- Missing grouped settings layout.
- Missing feature-gated sections for DVBC, DVBC_NET2, IP, HLS, Portal, Cloud, Forced Content.
- Missing typed controls like toggles and dropdowns for legacy setting groups.
- Missing HLS auth toggle logic and remux toggle logic.
- Missing dialogs and their workflows:
  - date/time settings
  - modulator network settings
  - change password
- Missing supporting backend operations:
  - `saveDateTime(...)`
  - `getModulators(...)`
  - `saveModulatorsConfig(...)`
  - `updatePw(...)`
  - `updateSettingsNew(...)`

### Network

- Missing editable network configuration UI.
- Missing device-level `bootproto`, `onboot`, `ipaddr`, `netmask` editing.
- Missing multicast route selector.
- Missing `IP Status` dialog.
- Missing save-confirm-apply flow.
- Missing network apply command (`runCommand("wnet")`).
- New network data does not perform legacy gateway/DNS/public-IP reachability checks.

### Commands

- Missing `Backup` download workflow.
- Missing `Restore` upload, validation, and restore workflow.
- Missing `Document` PDF export workflow.
- Missing `Power off`.
- Missing `Restart network`.
- Missing `Update interfaces`.
- Missing `Reset Software`.
- Missing `Start all interfaces`.
- Missing `Stop all interfaces`.
- Missing `Software Update` launcher from the Commands page.
- Current command whitelist is different and much smaller than legacy command behavior.

### Update

- Missing initial `check-sw` discovery step.
- Missing disabled-state path driven by a dedicated backend call.
- Missing long-running modal/progress behavior around update checks and installs.
- Missing real package-manager command behavior and file-backed result collection.

### Cloud

- Missing `Connect` action.
- Missing `Disconnect` action.
- Missing 5-second polling.
- Missing legacy cloud table fields and semantics:
  - `ixcloud_enable`
  - `ixcloud_online`
  - `ixcloud_validate_date`
  - `ixcloud_validate_message`
  - `ixcloud_beaconid`
- New backend returns stripped `ixcloud_*` keys, but the new UI expects a different data contract (`status`, `cloud_id`, `last_sync`, `endpoint`, `registration_url`).

### HLS Wizard

- Highest-risk page-level behavior drift.
- Legacy wizard works on channel/service selection after scan results and route reconciliation.
- New page loads HLS interfaces and treats them like selectable services.
- Missing scan flow.
- Missing filter tags.
- Missing max-HLS-capacity enforcement.
- Missing legacy route-based preselection logic.
- Missing usage/capacity summary.
- Save payload is not aligned with the service-centric legacy workflow.

### Force Content

- Missing fields per forced-content item:
  - DVB-C networks
  - media name/file selection
  - operation mode
  - signal type
  - volume
- Missing exact four-card layout semantics.
- Missing `Force Content Control` dialog.
- Missing 1-second live polling of enabled forced-content status.
- Missing override-status writeback via `saveForcedContentOverrideStatus(...)`.
- New page exposes `override_index`, but DB-backed save only updates `enable` and `name`, so this field does not actually persist in the current Python backend.

## Missing Or Drifted Backend Operations

### Missing entirely from the FastAPI backend

- `checkPersmission`
- `getInterfaceScanTime`
- `interfaceSet`
- `interfaceScanResult`
- `interfaceUpdate`
- `interfaceCommand`
- `pushConfig`
- `isConfigChanged`
- `getMaxBitrates`
- `getNetworkSettings`
- `savePDF`
- `runUpdateCommand`
- `updateInterfaceMultibandType`
- `getSessionValue`
- `runCommand2`
- `saveDateTime`
- `updateSettingsNew`
- `getModulators`
- `saveModulatorsConfig`
- `getEnabledForcedContents`
- `saveForcedContentOverrideStatus`
- `updatePw`
- `setInterfaceInfoch`
- `getInterfaceInfoch`
- `getCurrentEmmList`

### Present but behavior-drifted

- `runCommand`
  - legacy supports operational `ixuiconf` commands and command-dialog workflows
  - new backend only allows `reboot`, `restart-services`, `factory-reset`, `update-epg`
- `getJsonInfo`
  - legacy validates uploaded backup JSON metadata for restore
  - new backend returns aggregated unit/interfaces/routes/settings JSON and does not support restore validation
- `getCloudDetails`
  - legacy returns `ixcloud_*` semantics used by the cloud page
  - new backend strips prefixes and returns a different shape
- `getForcedContents` and `saveForcedContents`
  - legacy model includes network/media/mode/signal/volume and live override control
  - new model is reduced to `id`, `name`, `enabled`, `override_index`
- `getHlsInterfaces` and `saveHlsWizardServices`
  - backend method exists, but the current frontend drives it with the wrong entity type and omits the legacy scan/filter/selection workflow
- `getNetworkStatus2`
  - legacy performs gateway/DNS/public-IP reachability checks
  - new backend returns static `online` fixture entries
- `getUpdatePackages`, `updatePackages`, `getUpdateResult`
  - new backend is mock-only and does not reproduce the legacy check/install/result command flow
- `getTunerStatus`, `getStreamerStatus`, `getInterfaceLog`
  - new backend is mock-only and does not reproduce live streamer/tuner data

## Highest-Priority Gaps

### Severity: critical

- HLS wizard behavior drift is severe enough that the current page is not equivalent to the legacy workflow.
- Backup, restore, and PDF export workflows are missing from the new UI and backend surface.
- Push-config state/action is missing from the new app shell.
- Interface scan/apply/result flow is incomplete because `interfaceSet` and `interfaceScanResult` do not exist.

### Severity: high

- Routes page is largely read-only compared to the legacy route editor.
- Commands page omits most operational actions.
- Network page omits almost all editable functionality and its apply workflow.
- Settings page omits several important sub-workflows and supporting endpoints.
- Force Content page omits the majority of the domain model and the live control dialog.
