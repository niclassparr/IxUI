# Feature Parity Implementation Backlog

## Purpose

This backlog turns the parity audit into an execution order.

The order is based on two rules:

1. Fix shared backend and integration primitives before page-specific UI work.
2. Fix the gaps that block multiple pages before fixing isolated page polish.

## Priority Legend

- `P0`: critical parity blocker or shared dependency for multiple pages
- `P1`: high-value page parity work that depends on P0 items
- `P2`: specialized feature work after shared flows are in place
- `P3`: cleanup, regression coverage, and final drift removal

## Recommended Execution Order

1. `BL-01` Safe command and file integration layer
2. `BL-02` Streamer/runtime adapter layer
3. `BL-03` Shell parity and feature contract normalization
4. `BL-04` Backup, restore, and PDF workflows
5. `BL-05` Interface runtime parity
6. `BL-06` Layout/Routes editor parity
7. `BL-07` HLS wizard rewrite
8. `BL-08` Settings parity and supporting dialogs
9. `BL-09` Network parity
10. `BL-10` Commands page parity
11. `BL-11` Update parity
12. `BL-12` Force Content parity
13. `BL-13` Cloud parity
14. `BL-14` Remove mock runtime paths and add parity regression coverage

This order is not the same as page order. It is the order that minimizes rework.

## Backlog

### BL-01 Safe Command And File Integration Layer

- Priority: `P0`
- Depends on: none
- Why first:
  - unlocks backup, restore, PDF generation, network apply, software update, cloud connect/disconnect, push-config, and most of the Commands page
  - reduces the current gap where command behavior is hardcoded and incomplete
- Scope:
  - implement a backend command runner abstraction for legacy `ixuiconf` style operations
  - support explicit allowlisted commands instead of the current four-command whitelist only
  - add file/result helpers for `/tmp/ixui_backup.json`, `/tmp/ixui_update_sw.txt`, `/tmp/ixuiconf.log`
  - decide whether to keep the exact temp-file behavior or wrap it behind a compatibility service that preserves outputs
- Legacy operations covered:
  - `runCommand`
  - `runCommand2`
  - `runUpdateCommand`
  - `pushConfig`
  - command-backed backup/restore/apply/update flows
- New code areas likely touched:
  - `app/backend/services/`
  - `app/backend/routers/system.py`
  - possibly new dedicated routers for legacy command/export flows
- Done when:
  - backend has a safe allowlisted command layer that can execute all legacy operational commands needed by the UI
  - command-backed workflows can return structured success/error output instead of mock responses
  - no page is blocked by missing command primitives

### BL-02 Streamer And Runtime Adapter Layer

- Priority: `P0`
- Depends on: none
- Why first:
  - unlocks Interfaces, Interface Edit, Interface Status, Interface Log, HLS wizard, and `update interfaces`
  - replaces the highest-risk mock endpoints
- Scope:
  - implement a backend adapter for the legacy streamer/socket behavior
  - reproduce runtime operations now missing in FastAPI
  - replace random/static mock status and log endpoints with real data sources
- Legacy operations covered:
  - `interfaceSet`
  - `interfaceScanResult`
  - `interfaceUpdate`
  - `interfaceCommand`
  - `getInterfaceScanTime`
  - `getCurrentEmmList`
  - `updateInterfaceMultibandType`
  - `getInterfaceInfoch`
  - `setInterfaceInfoch`
  - `getTunerStatus`
  - `getStreamerStatus`
  - `interfaceLog`
  - `getSessionValue` if needed for progress/state tracking
- New code areas likely touched:
  - `app/backend/services/data_service.py`
  - new integration service under `app/backend/services/`
  - `app/backend/routers/interfaces.py`
- Done when:
  - FastAPI exposes the missing runtime interface endpoints
  - tuner, streamer, scan result, and log data are real or compatibility-backed rather than random/static
  - the backend can support legacy scan/apply/control flows end to end

### BL-03 Shell Parity And Feature Contract Normalization

- Priority: `P0`
- Depends on: `BL-01` for push-config execution
- Why early:
  - unlocks correct nav visibility and global application behavior
  - prevents each page from reimplementing feature logic inconsistently
- Scope:
  - normalize the feature contract used by shell and pages
  - implement the missing permission/session augmentation behavior
  - add global config-changed polling and push-config endpoint/UI
  - align cloud/feature fields between backend and frontend contracts
- Legacy operations covered:
  - `checkPersmission`
  - `isConfigChanged`
  - `pushConfig`
  - feature-gated menu behavior from `UnitInfo` + session state
- New code areas likely touched:
  - `app/frontend/src/app.js`
  - `app/frontend/src/components/nav-menu.js`
  - `app/frontend/src/services/api.js`
  - `app/backend/routers/auth.py`
  - `app/backend/routers/system.py`
  - `app/backend/models.py`
- Done when:
  - nav visibility and page reachability follow legacy feature rules
  - the app shell shows config-changed state and can push config
  - backend and frontend use a stable, explicit feature/state contract

### BL-04 Backup, Restore, And PDF Workflows

- Priority: `P0`
- Depends on: `BL-01`, `BL-03`
- Why early:
  - these are entirely missing workflows
  - they are operationally important and user-visible
- Scope:
  - implement authenticated backup download
  - implement restore upload, uploaded-file validation, and restore execution
  - implement PDF export
  - decide whether to mirror legacy servlet URLs or provide SPA-compatible replacements with equivalent behavior
- Legacy operations covered:
  - `JsonServlet.doGet`
  - `JsonServlet.doPost`
  - `getJsonInfo`
  - `savePDF`
  - `DownloadPdfServlet.doGet`
- Frontend work required:
  - add Backup, Restore, and Document actions to the Commands page
  - add restore dialog or equivalent upload/validate/apply flow
- Done when:
  - a user can download a backup JSON file
  - a user can upload a backup JSON file, validate it, and restore from it
  - a user can download the installation PDF

### BL-05 Interface Runtime Parity

- Priority: `P0`
- Depends on: `BL-02`, `BL-03`
- Why before page-specific polish:
  - it closes the deepest gaps across `interfaces`, `interface-edit`, `interface-status`, and `interface-log`
- Scope:
  - restore complete interface scan/apply/result flow
  - restore multiband switching and infoch flows
  - restore EMM selection support
  - restore real log/status/control behavior
- Pages unlocked:
  - `interfaces`
  - `interface-edit`
  - `interface-status`
  - `interface-log`
- Backend work checklist:
  - `getInterfaceScanTime`
  - `interfaceSet`
  - `interfaceScanResult`
  - `interfaceCommand`
  - `updateInterfaceMultibandType`
  - `getCurrentEmmList`
  - `getInterfaceInfoch`
  - `setInterfaceInfoch`
- Frontend work checklist:
  - add missing start/stop/log/scan semantics where required
  - add multiband selector behavior
  - add scan date display
  - add EMM field lifecycle
  - add service-edit and save-services behavior to interface edit
  - add per-type status UI to interface status
- Done when:
  - the four interface-related pages can perform the same backend workflows as the legacy app
  - no interface page still depends on mock tuner/streamer/log data

### BL-06 Layout / Routes Editor Parity

- Priority: `P1`
- Depends on: `BL-02`, `BL-05`
- Why here:
  - Routes depends on real interface/runtime data and on feature gating
  - HLS wizard depends on proper route semantics
- Scope:
  - convert the new Routes page from read-only to editable
  - add feature-dependent columns and route field editing
  - add duplicate SID/IP detection
  - add usage summary table and max bitrate support
  - add HLS wizard entry from the page header
- Legacy operations covered:
  - `getMaxBitrates`
  - `getBitrates`
  - `getRoutes`
  - `updateRoutes`
- Frontend work checklist:
  - editable fields for LCN, descrambler, modulators, out SID, out IP, EPG, HLS toggle, output name
  - config-link navigation back to `interface-edit`
  - usage summary view
  - duplicate highlighting and warning flow
- Done when:
  - the new Routes page can edit and save route configuration with the same domain concepts as legacy
  - route usage/capacity is visible and validated

### BL-07 HLS Wizard Rewrite

- Priority: `P0`
- Depends on: `BL-05`, `BL-06`
- Why explicitly separate:
  - this page is not merely incomplete; it is behaviorally wrong in the current app
- Scope:
  - rewrite the page to be service-centric rather than interface-centric
  - restore scan-driven service discovery, filtering, capacity enforcement, and selected-service save behavior
  - align frontend payloads with the existing backend intent or adjust backend contract to match legacy exactly
- Legacy behavior to restore:
  - scan step
  - tag filter menu
  - available vs selected channel tables
  - maximum HLS service enforcement
  - usage summary
  - route-based preselection logic
- Backend work checklist:
  - validate and possibly adjust `get_hls_interfaces()` and `save_hls_wizard_services()` contracts
  - add any missing max-capacity and route helper endpoints needed by the UI
- Done when:
  - the new HLS wizard selects services, not interfaces
  - the page can reproduce the legacy scan/select/save workflow without behavior drift

### BL-08 Settings Parity And Supporting Dialogs

- Priority: `P1`
- Depends on: `BL-01`, `BL-03`
- Why here:
  - a large amount of missing behavior is in dialogs and supporting endpoints, not just the main form
- Scope:
  - replace the flat name-value editor with grouped, feature-aware settings sections
  - add dialog-backed subflows for date/time, modulator network settings, and password change
  - restore toggle-driven section visibility
- Legacy operations covered:
  - `saveDateTime`
  - `getModulators`
  - `saveModulatorsConfig`
  - `updatePw`
  - `updateSettingsNew` where needed by settings-specific flows
- Frontend work checklist:
  - group settings by legacy domains
  - typed controls instead of raw key/value inputs
  - HLS auth toggle behavior
  - remux toggle behavior
  - modal flows for date/time, modulator settings, and password change
- Done when:
  - the Settings page exposes the same grouped settings domains and subflows as legacy

### BL-09 Network Parity

- Priority: `P1`
- Depends on: `BL-01`, `BL-08`
- Why after settings primitives:
  - network settings reuse name-value settings infrastructure and command execution
- Scope:
  - add editable network settings UI
  - add IP status/reachability dialog
  - add save-confirm-apply behavior
  - replace static network status with real status checks
- Legacy operations covered:
  - `getNetworkSettings`
  - `updateSettingsNew`
  - `getNetworkStatus`
  - `getNetworkStatus2`
  - `runCommand("wnet")`
- Frontend work checklist:
  - editable common settings fields
  - editable per-interface rows for bootproto, onboot, ipaddr, netmask
  - IP status dialog showing gateway, DNS1, DNS2, public IP
  - save confirmation before apply
- Done when:
  - the Network page can edit and apply network settings and show real reachability diagnostics

### BL-10 Commands Page Parity

- Priority: `P1`
- Depends on: `BL-01`, `BL-04`, `BL-05`, `BL-09`
- Why after prerequisites:
  - the Commands page is mostly an entry point into other operational workflows
- Scope:
  - expand the Commands page to the full legacy command set
  - wire each action to the corresponding backend flow instead of a small hardcoded whitelist
- Legacy commands/pages to restore:
  - backup
  - restore
  - document
  - power off
  - reboot
  - restart network
  - update interfaces
  - reset software
  - start all interfaces
  - stop all interfaces
  - software update launcher
- Done when:
  - every legacy Commands page action exists in the new page and routes to a real backend operation

### BL-11 Update Parity

- Priority: `P2`
- Depends on: `BL-01`, `BL-10`
- Why after Commands:
  - legacy update is launched from Commands and depends on command/result plumbing
- Scope:
  - implement the legacy check/install/result sequence
  - replace the current mock package behavior with real discovery and result handling
- Legacy operations covered:
  - `isSoftwareUpdate`
  - `runUpdateCommand("check-sw")`
  - `getUpdatePackages`
  - `updatePackages`
  - `getUpdateResult`
- Frontend work checklist:
  - disabled-state handling
  - initial discovery action
  - package selection UI
  - result output view
- Done when:
  - the Update page follows the same check-select-install-result lifecycle as legacy

### BL-12 Force Content Parity

- Priority: `P1`
- Depends on: `BL-03` and any media/backend integration needed
- Why separate:
  - the current page preserves only a small subset of the legacy domain model
- Scope:
  - expand the Forced Content data model, backend persistence, and UI fields
  - implement the live control dialog and override-status workflow
  - validate media-source requirements for real parity
- Legacy operations covered:
  - `getForcedContents`
  - `saveForcedContents`
  - `getEnabledForcedContents`
  - `saveForcedContentOverrideStatus`
  - `getMedia`
- Frontend work checklist:
  - four-card layout
  - fields for networks, media, operation mode, signal type, volume
  - control dialog with 1-second polling
  - live override changes
- Done when:
  - the new page can fully configure and control forced content the same way as legacy

### BL-13 Cloud Parity

- Priority: `P2`
- Depends on: `BL-01`, `BL-03`
- Scope:
  - align cloud data contract with legacy `ixcloud_*` semantics
  - add connect/disconnect actions and 5-second polling
  - ensure nav gating and page behavior stay consistent with legacy cloud enablement
- Legacy operations covered:
  - `getCloudDetails`
  - command-backed `ixcloud-connect`
  - command-backed `ixcloud-disconnect`
- Done when:
  - the Cloud page shows the same core fields and can connect/disconnect like the legacy page

### BL-14 Remove Mock Runtime Paths And Add Parity Regression Coverage

- Priority: `P3`
- Depends on: `BL-01` through `BL-13`
- Scope:
  - remove remaining mock/static/random runtime paths for parity-sensitive features
  - add regression coverage around the key parity workflows
  - create acceptance checklist coverage for every page in the gap report
- Minimum coverage targets:
  - shell login/session/nav/feature behavior
  - backup/download/upload/export flows
  - interface edit/status/log/scan flows
  - routes save flow
  - settings/network apply flows
  - commands/update/force content/cloud flows
- Done when:
  - critical pages no longer depend on placeholder data paths
  - there is a repeatable regression suite for the parity-critical workflows

## Dependency Summary

### Foundational blockers

- `BL-01` blocks `BL-03`, `BL-04`, `BL-08`, `BL-09`, `BL-10`, `BL-11`, `BL-13`
- `BL-02` blocks `BL-05`, `BL-06`, `BL-07`, and part of `BL-10`
- `BL-03` blocks global feature-gated UI parity and helps stabilize `BL-05`, `BL-08`, `BL-12`, `BL-13`

### Sequential chains that should stay in order

- `BL-01` -> `BL-04` -> `BL-10` -> `BL-11`
- `BL-02` -> `BL-05` -> `BL-06` -> `BL-07`
- `BL-01` -> `BL-08` -> `BL-09`

### Safe parallel work after foundations

- After `BL-01`, `BL-02`, and `BL-03` are complete, these can run in parallel with low coordination cost:
  - `BL-08` Settings parity
  - `BL-12` Force Content parity
  - `BL-13` Cloud parity
- `BL-09` Network parity can run in parallel with `BL-06` Routes parity once `BL-01` and `BL-08` are stable.

## Suggested Milestones

### Milestone 1: Operational Foundations

- `BL-01`
- `BL-02`
- `BL-03`

Result:

- the new app can support real command-backed and streamer-backed parity work

### Milestone 2: Critical Missing Workflows

- `BL-04`
- `BL-05`
- `BL-06`
- `BL-07`

Result:

- the biggest operational and runtime gaps are closed

### Milestone 3: Configuration Pages

- `BL-08`
- `BL-09`
- `BL-10`
- `BL-11`

Result:

- commands, settings, network, and update flows reach near-complete parity

### Milestone 4: Specialized Features And Hardening

- `BL-12`
- `BL-13`
- `BL-14`

Result:

- specialized domains are covered and remaining mock/drift paths are removed
