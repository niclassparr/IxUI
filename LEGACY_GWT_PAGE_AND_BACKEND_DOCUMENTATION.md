# Legacy GWT Page And Backend Documentation

## Scope

This document describes the legacy GWT application page by page and notes what the backend does for each workflow.

Primary evidence:

- `GWT_APP/src/se/ixanon/ixui/client/AppController.java`
- `GWT_APP/src/se/ixanon/ixui/client/item/menu/Menu.java`
- `GWT_APP/src/se/ixanon/ixui/client/presenter/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/view/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/item/dialog/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/item/table/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/IxuiService.java`
- `GWT_APP/src/se/ixanon/ixui/client/IxuiServiceAsync.java`
- `GWT_APP/src/se/ixanon/ixui/server/IxuiServiceImpl.java`
- `GWT_APP/src/se/ixanon/ixui/server/JsonServlet.java`
- `GWT_APP/src/se/ixanon/ixui/server/DownloadPdfServlet.java`
- `GWT_APP/src/se/ixanon/ixui/server/FileUploadListener.java`

## Global Shell Behavior

### Startup and routing

- `AppController` validates the current session with `validateSession(sessionKey, username)`.
- If the session is valid it calls `checkPersmission(sessionKey, currentSessionKey)` and then rebuilds the menu and resolves the page token to a presenter.
- If the session is invalid it shows `LoginDialog`, clears menu/main content, and resets the application shell.
- Route tokens handled by `AppController` are: `dashboard`, `interfaces`, `interface-edit`, `interface-status`, `interface-log`, `layout`, `settings`, `network`, `commands`, `update`, `cloud`, `hls-wizard`, `force-content`.

### Menu and non-page behavior

- `Menu` loads `UnitInfo` and shows hostname, serial, and version.
- Menu entries are always: `Network`, `Settings`, `Interfaces`, `Layout`, `Commands`, `Logout`.
- Menu entries are conditional: `Cloud` if unit cloud is enabled, `Force Content` if forced-content is enabled.
- `Menu` polls `isConfigChanged(sessionKey)` every 5 seconds.
- When `config_changed` is true the menu shows a push-config banner and flashing push button.
- The push-config banner is a global workflow, not tied to a page.
- This global shell behavior is part of legacy parity and is not optional.

## Page Inventory

### 1. Dashboard

- Route token: `dashboard`
- Reachability: default landing page fired by `AppController.go()`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/FrontPagePresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/FrontPageView.java`
- UI behavior:
  - shows a simple welcome page
  - displays `Welcome` or permission-related text depending on presenter input
  - no editable controls
- Backend behavior:
  - no page-specific RPC calls after session validation
  - all backend work happens in shell startup before the page is shown

### 2. Interfaces

- Route token: `interfaces`
- Reachability: direct menu entry from `Menu`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/InterfacesPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/InterfacesView.java`
- UI behavior:
  - page header `Interfaces`
  - type filter menu built from interface types
  - table columns: position, type, name, status, config, log
  - multiband interfaces are marked with `M`
  - some interface types expose config links, others do not
  - error rows are styled distinctly
  - page polls every 5 seconds
- Navigation from page:
  - position link -> `interface-status`
  - config link -> `interface-edit`
  - log link -> `interface-log`
- Backend behavior:
  - `getInterfaceTypes()` loads the filter menu
  - `getInterfaces(sessionKey, true)` loads the table
  - each refresh repeats `getInterfaces`
  - backend `getInterfaces` reads `interfaces` and related config state, and may derive status per interface

### 3. Interface Edit

- Route token: `interface-edit`
- Reachability: indirect, from `Interfaces` and `Routes`
- Route parameters:
  - `INTERFACE_POS`
  - `INTERFACE_TYPE`
  - `MULTIBAND`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/InterfaceEditPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/InterfaceEditView.java`
- UI behavior:
  - header `Config: {pos} - {translated type}`
  - optional multiband selector for type switching
  - config form changes by interface type
  - supported type-specific fields include:
    - `dvbs`: frequency, polarization, symbol rate, delivery method, satellite number, LNB type
    - `dvbt`: frequency, bandwidth, delivery method
    - `dvbc`: frequency, symbol rate, constellation, delivery method
    - `dvbudp`: address, port
    - `infostreamer`: presentation URL
    - `dvbhdmi` and `hdmi2ip`: format
    - `hls2ip`: max bitrate
    - `webradio`: gain and webradio URL depending on cloud mode
    - `infoch`: simplified interface name and active state
  - shows `Interface Active` toggle for most interface types
  - shows EMM selector for DVB and DVBUDP style interfaces
  - shows last scan time when available
  - has `Save` and `Save Services` buttons
  - may show `Scan` button for DVB, DVBUDP, HLS2IP, infoch, and some webradio flows
  - service table fields vary by type:
    - normal interfaces: enabled, sid, type, language, epg
    - infostreamer: radio URL, show presentation
    - hls2ip/webradio/infoch: URL column
- Backend behavior:
  - `getConfig(sessionKey, interface_pos, interface_type)` loads the config form
  - `getServices(sessionKey, interface_pos)` loads services
  - `getInterfaceScanTime(sessionKey, interface_pos)` loads scan timestamp
  - `getCurrentEmmList(interface_pos, isDsc)` populates EMM options
  - `setConfig(sessionKey, config, interface_type)` saves interface config
  - `saveServices(services, interface_type, interface_pos)` saves service edits
  - scan flow:
    - save config with `setConfig`
    - call `interfaceSet(sessionKey, interface_pos, interface_type)`
    - show `ScanDialog`
    - after scan-ready event, call `interfaceScanResult(interface_pos)`
  - multiband flow:
    - `updateInterfaceMultibandType(interface_pos, new_type)`
    - then route back into `interface-edit` with updated type
  - info channel flow:
    - `getInterfaceInfoch(interface_pos)`
    - `setInterfaceInfoch(config, isScan)`
  - backend side effects include updates to type-specific config tables, service persistence, scan state, EMM allocation, and `config_changed`

### 4. Interface Status

- Route token: `interface-status`
- Reachability: indirect from `Interfaces`
- Route parameters:
  - `INTERFACE_POS`
  - `INTERFACE_TYPE`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/InterfaceStatusPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/InterfaceStatusView.java`
- UI behavior:
  - header `Status: {pos}`
  - action buttons: `Start`, `Stop`, `Log`
  - `CI Menu` button for `dsc`
  - interface status panel shows name and status
  - tuner status panel varies by interface type
  - streamer status panel varies by interface type
  - clears tuner/streamer detail when interface status is idle
  - polls every 5 seconds
  - DSC interfaces show CI text, CA/EMM information, and interactive CI menu items
  - HLS/Webradio/Infoch use special streamer tables distinct from DVB and modulator displays
- Backend behavior:
  - `getInterface(interface_pos, true)` loads interface status base data
  - `interfaceTunerStatus(interface_pos, interface_type)` loads tuner or CA state
  - `interfaceStreamerStatus(interface_pos, interface_type)` loads per-service stream metrics
  - `interfaceCommand(interface_pos, command)` handles:
    - `stream`
    - `stop`
    - `mmi_open`
    - `mmi_close`
    - `mmi_answer {id}`
  - backend reaches streamer/socket operations and returns typed status objects

### 5. Interface Log

- Route token: `interface-log`
- Reachability: indirect from `Interfaces` or `Interface Status`
- Route parameters:
  - `INTERFACE_POS`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/InterfaceLogPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/InterfaceLogView.java`
- UI behavior:
  - header `Log: {interface_pos}`
  - text area showing current interface log
  - manual refresh button
- Backend behavior:
  - `interfaceLog(interface_pos)` returns current log text
  - backend retrieves log content from the streamer side

### 6. Layout

- Route token: `layout`
- Reachability: direct menu entry from `Menu`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/RoutesPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/RoutesView.java`
- UI behavior:
  - page header `Layout`
  - optional `HLS Wizard` button when HLS interfaces exist
  - editable routing table
  - columns are feature-dependent and can include:
    - service/output name
    - interface config link
    - LCN
    - descrambler
    - mod net 1
    - mod net 2
    - out SID
    - out IP
    - EPG
    - HLS checkbox
  - duplicate SID and duplicate IP detection with highlighted cells
  - reorderable columns and sortable rows
  - bitrate and usage summary table for descramblers, modulators, and HLS
  - save button
- Backend behavior:
  - `getInterfaceTypes()` to determine HLS wizard availability
  - `getEnabledType(...)` for `dvbc`, `dvbc_net2`, `ip`, `hls`, `portal`
  - `getMaxBitrates("mod")`, `getMaxBitrates("dsc")`, `getMaxBitrates("hls")`
  - `getInterfaces(sessionKey, true)` to populate descrambler/modulator lists
  - `getRoutes(sessionKey)` to populate table rows
  - `getBitrates(sessionKey)` to build usage summaries
  - `updateRoutes(sessionKey, routes)` saves edits

### 7. Settings

- Route token: `settings`
- Reachability: direct menu entry from `Menu`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/SettingsPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/SettingsView.java`
  - dialogs:
    - `GWT_APP/src/se/ixanon/ixui/client/item/dialog/DateTimeDialog.java`
    - `GWT_APP/src/se/ixanon/ixui/client/item/dialog/ModulatorSettingsDialog.java`
    - `GWT_APP/src/se/ixanon/ixui/client/item/dialog/PwDialog.java`
- UI behavior:
  - grouped settings page, not a flat key-value editor
  - feature-gated sections for DVBC, DVBC_NET2, IP, HLS, Portal, Cloud, Forced Content
  - toggle-dependent field visibility for HLS auth and remux sections
  - modal launchers for date/time, modulator network settings, and password change
  - save button
- Backend behavior:
  - `getUnitInfo()` determines which feature sections to render
  - `getSettings(sessionKey, true)` loads structured settings into the view
  - `updateSettings(sessionKey, settings_map)` saves settings
  - supporting dialog workflows use:
    - `saveDateTime(...)`
    - `getModulators(sessionKey)`
    - `saveModulatorsConfig(...)`
    - `updatePw(username, old_password, new_password)`

### 8. Network

- Route token: `network`
- Reachability: direct menu entry from `Menu`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/NetworkPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/NetworkView.java`
  - dialog: `GWT_APP/src/se/ixanon/ixui/client/item/dialog/NetworkStatusDialog.java`
- UI behavior:
  - editable common settings: hostname, default gateway, multicast route, DNS1, DNS2
  - editable device table for `eth0`..`ethN` with protocol, onboot, IP, netmask, MAC
  - status table showing current MAC/IP per interface
  - `IP Status` button opens dialog with reachability checks for gateway, DNS1, DNS2, and public IP
  - `Save` requires confirmation because reboot/network restart is needed
- Backend behavior:
  - `getNetworkStatus(sessionKey)` loads current interface MAC/IP values
  - `getNetworkSettings()` loads `nw_*` name-value settings
  - `updateSettingsNew(sessionKey, settings)` saves network settings
  - `runCommand("wnet", null)` applies network changes
  - `getNetworkStatus2(sessionKey)` powers the `IP Status` dialog and performs reachability checks

### 9. Commands

- Route token: `commands`
- Reachability: direct menu entry from `Menu`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/CommandsPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/CommandsView.java`
  - dialogs:
    - `GWT_APP/src/se/ixanon/ixui/client/item/dialog/CommandDialog.java`
    - `GWT_APP/src/se/ixanon/ixui/client/item/dialog/RestoreDialog.java`
- UI behavior:
  - actions available from the page:
    - backup
    - restore
    - document (PDF export)
    - power off
    - reboot
    - restart network
    - update interfaces
    - reset software
    - start all interfaces
    - stop all interfaces
    - software update if enabled
  - backup and PDF open authenticated download URLs
  - restore opens upload/validate/restore dialog
  - command actions use confirmation dialogs where appropriate
- Backend behavior:
  - `getUnitInfo()` gates software update button visibility
  - backup download uses `JsonServlet.doGet` and `runCommand("backup")`
  - restore upload uses `JsonServlet.doPost`, then `getJsonInfo()`, then `runCommand("restore")`
  - PDF download uses `DownloadPdfServlet.doGet`
  - operational commands use `runCommand(...)`
  - update interfaces uses `interfaceUpdate()`
  - software update entry routes to the `update` page

### 10. Update

- Route token: `update`
- Reachability: indirect from `Commands` when software update is enabled
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/UpdatePresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/UpdateView.java`
- UI behavior:
  - disabled-state message if software update is unavailable
  - initial `OK` action starts package discovery
  - package table with checkboxes after discovery
  - `Update` button installs selected packages
  - result view shows raw update output in `<pre>`
- Backend behavior:
  - `isSoftwareUpdate()` gates the page
  - `runUpdateCommand("check-sw")` discovers available updates
  - `getUpdatePackages()` returns available packages
  - `updatePackages(packages)` installs selected packages
  - `getUpdateResult()` returns update output/result text

### 11. Cloud

- Route token: `cloud`
- Reachability: menu entry only when cloud is enabled
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/CloudPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/CloudView.java`
- UI behavior:
  - header `Cloud`
  - connection details table
  - when cloud is enabled, shows `Connect` and `Disconnect` buttons
  - polls every 5 seconds
  - displayed fields include status, validation date, validation message, and beacon ID
- Backend behavior:
  - `getCloudDetails(sessionKey)` loads status data from `ixcloud_*` settings
  - `runCommand("ixcloud-connect")` and `runCommand("ixcloud-disconnect")` are used by button dialogs

### 12. HLS Wizard

- Route token: `hls-wizard`
- Reachability: indirect from `Layout`
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/HlsWizardPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/HlsWizardView.java`
- UI behavior:
  - three-step wizard: scan, select services, save services
  - warning that previous HLS interface configuration will be erased
  - scan button
  - filter menu built from service tags
  - dual tables for available channels and selected channels
  - left/right arrow actions move services between tables
  - maximum HLS service count is enforced
  - usage summary table shows selected service count against capacity
  - `Save Services` button commits the new HLS mapping
- Backend behavior:
  - `getInterfacesHls(sessionKey)` determines HLS interface inventory
  - page also relies on existing `routes` to preselect matching services
  - scanning uses `interfaceScanResult(interface_pos)` after scan-ready
  - save action calls `saveHlsWizardServices(sessionKey, services)`
  - backend rewrites HLS interface/service/route mappings

### 13. Force Content

- Route token: `force-content`
- Reachability: menu entry only when forced content is enabled
- Primary client files:
  - `GWT_APP/src/se/ixanon/ixui/client/presenter/ForcedContentPresenter.java`
  - `GWT_APP/src/se/ixanon/ixui/client/view/ForcedContentView.java`
  - `GWT_APP/src/se/ixanon/ixui/client/item/table/ForcedContentItem.java`
  - `GWT_APP/src/se/ixanon/ixui/client/item/dialog/ForcedContentControlDialog.java`
- UI behavior:
  - page header `Force Content`
  - `Force Content Control` button opens a live control dialog
  - exactly four forced-content cards are rendered
  - each card contains:
    - enabled toggle
    - name
    - DVB-C networks selector
    - media selector from media library
    - operation mode
    - signal type
    - volume
  - save button
  - control dialog polls every second and shows live signal/com status for enabled entries
  - control dialog writes override status changes immediately
- Backend behavior:
  - `getEnabledType("forced_content")` gates page enablement
  - `getMedia()` loads media library items
  - `getForcedContents(sessionKey)` loads stored forced-content entries
  - `saveForcedContents(values)` saves all cards
  - control dialog uses:
    - `getEnabledForcedContents(sessionKey)` for live status polling
    - `saveForcedContentOverrideStatus(id, index)` for override changes

## Non-Page Backend Flows

### Auth and session

- `login(username, password)` authenticates against `users`
- `logout(sessionKey)` invalidates DB-backed session
- `validateSession(sessionKey, username)` validates a session against `user_sessions`
- `checkPersmission(sessionKey, sessionKeys)` augments session state with cloud flag

### Backup and restore

- `JsonServlet.doGet` validates session and username, runs backup command, then downloads `/tmp/ixui_backup.json` as `ixui_backup_{serial}_{date}.json`
- `JsonServlet.doPost` accepts multipart upload, writes uploaded JSON to `/tmp/ixui_backup.json`, and relies on `RestoreDialog` plus `getJsonInfo()` and `runCommand("restore")` to complete restore

### PDF export

- `DownloadPdfServlet.doGet` validates session and streams generated PDF `installation_{serial}_{date}.pdf`
- PDF generation is built from settings, interfaces, modulators, and channel rows via `PDFGenerator`

### Command and integration behavior

- many legacy workflows ultimately call `/usr/bin/ixuiconf` or the streamer socket/daemon layer through `StreamerManager`
- interface status, scan, CI menu, and logs are streamer-driven, not simple DB reads
- config save and push-config workflows update DB state and also trigger external command/application behavior
