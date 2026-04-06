# IxUI Feature Parity Audit Plan

## Goal

Produce an exact gap report between the legacy GWT application and the new FastAPI + Lit application.

The audit must answer three questions for every legacy workflow:

1. What exists in the GWT UI, exactly?
2. What happens in the legacy backend when that UI is used?
3. Does the new app implement the same functionality, behavior, and side effects?

## Confirmed Scope

### Legacy page tokens

- `dashboard`
- `interfaces`
- `interface-edit`
- `interface-status`
- `interface-log`
- `layout`
- `settings`
- `network`
- `commands`
- `update`
- `cloud`
- `hls-wizard`
- `force-content`

### New app page tokens

- `front-page`
- `interfaces`
- `interface-edit`
- `interface-status`
- `interface-log`
- `routes`
- `settings`
- `network`
- `commands`
- `update`
- `cloud`
- `hls-wizard`
- `forced-content`

### Direct page mapping to audit

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

## Important Audit Rules

1. Do not treat matching page names as parity.
2. Do not treat a matching endpoint name as parity.
3. Track reachability separately from implementation.
4. Track feature-gating separately from missing functionality.
5. Trace every user action down to backend side effects.
6. Mark anything unverified as `unknown`, not `implemented`.

## Evidence Sources

### Legacy UI

- `GWT_APP/src/se/ixanon/ixui/client/AppController.java`
- `GWT_APP/src/se/ixanon/ixui/client/item/menu/Menu.java`
- `GWT_APP/src/se/ixanon/ixui/client/presenter/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/view/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/item/**/*.java`
- `GWT_APP/src/se/ixanon/ixui/client/event/**/*.java`

### Legacy backend

- `GWT_APP/src/se/ixanon/ixui/client/IxuiService.java`
- `GWT_APP/src/se/ixanon/ixui/server/IxuiServiceImpl.java`
- `GWT_APP/src/se/ixanon/ixui/server/JsonServlet.java`
- `GWT_APP/src/se/ixanon/ixui/server/DownloadPdfServlet.java`
- `GWT_APP/src/se/ixanon/ixui/server/FileUploadListener.java`
- `GWT_APP/src/se/ixanon/ixui/server/ContextManager.java`
- `GWT_APP/src/se/ixanon/ixui/server/StreamerManager.java`
- `GWT_APP/src/se/ixanon/ixui/server/singleton/*.java`

### New UI

- `app/frontend/src/app.js`
- `app/frontend/src/components/*.js`
- `app/frontend/src/services/api.js`

### New backend

- `app/backend/main.py`
- `app/backend/routers/*.py`
- `app/backend/services/*.py`
- `app/backend/models.py`
- `app/ixui_73_260312.sql`

## Execution Plan

### Phase 1: Legacy navigation and page inventory

For each legacy route token:

- identify how the page is reached: menu, redirect, dialog, history event, or feature gate
- identify whether the page is always visible, conditionally visible, or indirectly reachable
- record route parameters such as interface position, interface type, and multiband state

Deliverable: a complete `legacy page map`.

### Phase 2: Legacy page-by-page functional specification

For each legacy page, document:

- page purpose
- visible sections and controls
- tables, fields, buttons, dialogs, and toggles
- initial data load sequence
- refresh timers and polling behavior
- validation rules
- save flows and confirmation flows
- navigation flows to child pages
- error and empty-state behavior
- feature-gated blocks
- exact RPC calls triggered by page load and each user action

This phase must read presenter, view, and dialog/item classes together. Presenter-only review is not enough.

Deliverable: `legacy page specification`.

### Phase 3: Legacy backend behavior inventory

For every RPC method, servlet, and file-backed flow used by the legacy UI, document:

- triggering page and user action
- server entry point
- downstream helper classes used
- tables read and written
- command execution
- file reads and writes
- network or socket calls
- session and permission checks
- returned payload shape
- side effects and follow-up operations

This phase must include non-page flows such as backup, restore, PDF generation, uploads, JSON diagnostics, and update/install commands.

Deliverable: `legacy backend operation inventory`.

### Phase 4: New app capability inventory

For each mapped page in the FastAPI + Lit app, document:

- route and reachability
- rendered sections and controls
- API calls on load and on user actions
- current validation and save behavior
- current feature-gating rules
- backend router and service methods used
- actual persistence and side effects
- fixture-only behavior versus real DB-backed behavior

Deliverable: `new app capability specification`.

### Phase 5: Page and operation parity comparison

Compare legacy versus new app at two levels:

1. Page level
2. Backend operation level

For each comparison item, assign exactly one status:

- `implemented`
- `partial`
- `missing`
- `renamed but equivalent`
- `behavior drift`
- `unknown`

Every non-implemented item must include exact evidence and the missing detail.

Deliverable: `parity matrix`.

### Phase 6: Final missing-feature report

Produce the final report in three sections:

1. Missing pages
2. Missing functionality on existing pages
3. Missing backend operations and side effects

Each finding should include:

- legacy page or operation name
- new app counterpart, if any
- status
- exact missing behavior
- evidence files
- severity
- recommended implementation order

Deliverable: `missing feature report`.

## Required Report Columns

### Page specification columns

- page token
- menu visibility
- feature flag or permission gate
- source presenter/view/dialog files
- load-time RPC calls
- action-triggered RPC calls
- timers/polling
- child navigation
- backend methods reached
- notes

### Backend operation columns

- operation name
- legacy entry point
- triggered from page
- helper classes
- DB tables
- external commands
- files touched
- network/socket behavior
- response shape
- new app counterpart
- parity status

### Gap report columns

- category
- legacy feature
- new app counterpart
- status
- exact gap
- evidence
- severity

## Initial Risks Already Visible

These are not yet the final findings, but they are high-probability gap areas that must be checked early:

- legacy backend surface is much larger than the current FastAPI router surface
- legacy app has indirect flows for software update and possibly HLS-specific workflows
- legacy menu includes push-config changed-state behavior that may not exist in the new nav
- backup, restore, upload, PDF, and JSON/export paths require servlet-level review, not only RPC review
- commands page likely includes more operational actions than the new app currently exposes
- settings and interface-edit pages are likely to hide significant feature-specific fields behind type and feature flags
- forced-content behavior includes enable state and override behavior, not just CRUD
- interface workflows include scanning, scan result retrieval, update flows, status polling, streamer/tuner status, and possible apply/push steps

## Recommended Work Order

1. Document legacy page behavior page by page.
2. Build the legacy backend operation inventory from those pages.
3. Document the new page behavior page by page.
4. Build the parity matrix.
5. Write the final missing-feature report.

## Definition of Done

The audit is complete only when:

1. every legacy page token is documented
2. every legacy UI action is traced to backend behavior
3. every legacy backend operation used by the UI has a parity status
4. every new page has been matched against its legacy counterpart
5. all missing pages and missing features are listed with evidence
