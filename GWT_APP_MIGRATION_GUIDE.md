# Complete Guide: Migrating a Legacy GWT Application to Any Modern Framework

This guide is based on real migration work done on a production-style GWT app (`IxUI`) with PostgreSQL, external command integrations, socket integrations, and legacy servlet/RPC behavior.

The guide is intentionally framework-agnostic so you can migrate to:
- Java stack (Spring Boot + React/Vue)
- JavaScript/TypeScript stack (Node/Nest + React/Vue/Svelte)
- Python stack (FastAPI/Django + React/Vue)
- .NET stack (ASP.NET Core + Blazor/React/Angular)
- Any equivalent architecture

## 1. Core Principle: Migrate Behavior, Not Just Code

If your goal is operational parity, the source of truth is runtime behavior, not class names or UI screenshots.

Treat migration as these parallel efforts:
1. Contract migration: requests/responses/events.
2. Data behavior migration: reads, writes, side effects, ordering, transactions.
3. Integration migration: commands, files, sockets, async jobs.
4. Workflow migration: the operator sequence and UI semantics.
5. Operational migration: deploy, rollback, monitoring, hypercare.

## 2. Pre-Migration Discovery (Mandatory)

## 2.1 Inventory all entry points

Document every endpoint and transport:
- GWT RPC services (`*Service`, `*ServiceAsync`)
- Servlets (`JsonServlet`, PDF download servlet, upload endpoints)
- Direct file operations
- Background jobs/timers
- Socket integrations
- Command-line execution paths

Deliverable: `operation inventory` table.

Suggested columns:
- Operation name
- Source files
- Triggering UI flow
- Input shape
- Output shape
- DB tables touched
- External dependencies touched
- Error behavior
- Security checks

## 2.2 Inventory DB behavior

For each operation, capture:
- SQL read/write pattern
- Transaction boundary
- Ordering assumptions
- Null/default semantics
- Legacy quirks (for example string booleans, sentinel values, text flags)

Deliverable: `DB behavior matrix`.

## 2.3 Inventory integration behavior

Capture all non-DB dependencies:
- External commands and exact arguments
- Expected exit code semantics
- stdout/stderr usage
- File paths (for example `/tmp/...`)
- Socket protocol contract
- Retry and timeout behavior

Deliverable: `integration contract document`.

## 2.4 Capture golden fixtures

Collect before migration:
- Real request payloads
- Real responses (success and failure)
- DB before/after snapshots for mutating operations
- Command logs
- File artifacts (JSON backup, PDF output)

Deliverable: `golden fixture pack` for parity tests.

## 3. Choose Migration Strategy and Scope

Pick one explicit strategy and do not mix silently:
1. Big-bang cutover
2. Strangler/parallel-run
3. Module-by-module migration

Define compatibility level:
1. Exact parity (recommended for operational systems)
2. Controlled modernization with signed deviations

Define schema policy:
1. No schema changes in phase 1 (safest)
2. Controlled schema evolution with migration scripts and rollback plan

## 4. Target Architecture (Framework-Agnostic Blueprint)

Use these layers regardless of language:
1. Contracts layer: DTOs, request/response schemas.
2. Application layer: business workflows, orchestration.
3. Infrastructure layer: DB, file system, commands, sockets.
4. Presentation layer: Web UI and API endpoints.
5. Cross-cutting: config, logging, auth, validation, feature flags.

## 4.1 Required abstractions

Create explicit interfaces for non-pure logic:
- `Session/Auth service`
- `Settings/config service`
- `Interfaces/status service`
- `Routes/services service`
- `Command runner`
- `Socket/streamer client`
- `Backup/PDF service`

This makes parity testing and target-framework replacement possible.

## 4.2 Compatibility adapter layer

Before redesigning API semantics, provide a compatibility API that mirrors legacy operation behavior.

Example adapter pattern:
- Legacy operation: `getNetworkStatus2(sessionKey)`
- New endpoint: `/api/legacy/settings/get-network-status2`
- New internals can be modern, external behavior stays legacy.

## 5. Security During Migration

Legacy GWT apps often contain insecure patterns that operators still depend on behavior-wise.

Preserve behavior, harden internals:
1. Replace concatenated SQL with parameterized SQL.
2. Keep response shapes stable while improving internals.
3. Add command execution allowlists.
4. Apply path sanitization for file uploads/downloads.
5. Add anti-forgery where applicable.
6. Add session validation gates to download endpoints.

Do not introduce user-visible behavior drift without explicit acceptance.

## 6. Detailed Migration Phases

## Phase A: Baseline and parity specification

Tasks:
1. Build operation parity matrix from source service files.
2. Map each operation to UI flow (presenter/view routes).
3. Capture integration contracts (commands/files/sockets).
4. Freeze fixture set.

Exit criteria:
1. 100% operation coverage in inventory.
2. 100% mutating operations have DB delta expectations.
3. All external integration flows documented.

## Phase B: Skeleton and platform setup

Tasks:
1. Create target app skeleton.
2. Add layered folders/modules.
3. Add config model for DB, commands, sockets, feature flags.
4. Add health checks and startup diagnostics.

Exit criteria:
1. App boots cleanly in local and test environments.
2. Config keys exist for all dependencies.

## Phase C: Contract and model port

Tasks:
1. Port shared models and enums.
2. Preserve null/default semantics.
3. Build serialization tests against fixtures.

Exit criteria:
1. Contract parity tests pass.
2. No shape drift in payloads.

## Phase D: Data and business parity

Tasks:
1. Implement service/repository logic operation by operation.
2. Preserve transaction boundaries and side effects.
3. Add deterministic unit tests per workflow.

Exit criteria:
1. All operations implemented.
2. DB delta tests pass.

## Phase E: External integration parity

Tasks:
1. Implement command runner abstraction (OS-aware).
2. Implement socket client abstraction.
3. Preserve timeout/exit code semantics.
4. Recreate file artifact behavior.

Exit criteria:
1. Integration tests for success/failure/timeout pass.
2. Runtime prerequisites validated.

## Phase F: UI/workflow parity

Tasks:
1. Port navigation shell and history behavior.
2. Port module pages in business-critical order.
3. Recreate dialogs, list edit flows, refresh/polling behavior.
4. Add parity-friendly feature flags.
5. Apply visual modernization without changing operational workflow.

Exit criteria:
1. Module-level UAT passes.
2. No critical workflow drift.

## Phase G: Non-UI endpoints, release readiness, cutover

Tasks:
1. Port backup JSON upload/download.
2. Port PDF generation/export behavior.
3. Run full parity and security suites.
4. Run staging rehearsal with production-like data.
5. Produce cutover, rollback, hypercare runbooks.

Exit criteria:
1. Parity evidence signed off.
2. Cutover rehearsal successful.
3. Rollback verified.

## 7. Testing Strategy You Should Reuse

Use a test pyramid specialized for migration.

## 7.1 Operation parity tests

For each operation test:
- Input forwarding correctness
- Output shape/value parity
- Error code/text parity

## 7.2 DB delta parity tests

For mutating workflows:
1. Seed DB state.
2. Execute operation.
3. Assert expected row-level changes only.
4. Assert unchanged unrelated rows/tables.

## 7.3 Endpoint compatibility tests

Spin in-memory app host and inject fake services:
- Validate endpoint routes and payload mapping.
- Validate unauthorized behavior and session checks.
- Validate alias endpoints if legacy had multiple names.

## 7.4 UI interaction tests

Use component/browser tests for module pages:
- Click flows
- Form editing and save
- Error and validation visibility
- Polling/refresh behavior

## 7.5 Integration contract tests

Mock external command/socket dependencies where needed and run selected real integration tests in staging.

## 8. Data and Environment Migration Checklist

## 8.1 Database

1. Take backup and verify restore.
2. Create reproducible local/staging DB bootstrap.
3. Validate table count and critical row counts.
4. Validate representative business rows.

## 8.2 Credentials and auth

1. Verify legacy credential assumptions (for example `admin/password`).
2. Implement upgrade path if moving to hashed passwords.
3. Add session expiry policy and observability.

## 8.3 External dependencies

1. Verify command binaries are present and executable.
2. Verify temp/log paths and permissions.
3. Verify socket endpoints reachable.

## 8.4 Runtime ports and process management

1. Reserve known dev/test ports.
2. Detect and kill stale listeners before build/test runs.
3. Ensure CI does not run while local process locks binaries.

## 9. UI Migration Guidance (From GWT Patterns)

Map legacy patterns to modern equivalents:
- GWT presenter -> page/container component
- GWT view -> reusable visual component
- GWT event bus -> local state + service callbacks or modern event/state library
- GWT async callbacks -> async/await with explicit loading/error state

Preserve operator workflow semantics:
1. Same field meanings and defaults.
2. Same action ordering.
3. Same confirmation/error behavior for destructive actions.
4. Same quick access to status and logs.

Modernization rules:
1. Improve layout/visual language.
2. Do not hide critical controls behind extra clicks.
3. Keep power-user workflows fast.

## 10. Common Pitfalls and How to Avoid Them

1. Missing render mode/interactivity setup in root app host
- Symptom: button clicks appear to do nothing.
- Fix: ensure interactive render mode is enabled in app root/routes, not only server registration.

2. Wrong default credentials in migrated UI
- Symptom: login appears broken.
- Fix: verify actual DB credentials and show explicit login result message.

3. Silent UI failures from swallowed errors
- Symptom: action does nothing visually.
- Fix: always display operation status and last response.

4. Build/test failures due to locked app binary
- Symptom: repeated copy retry and MSB3021/MSB3027 errors.
- Fix: stop running app process before build/test in local environments.

5. Type collisions during porting
- Symptom: ambiguous type compile errors (`Route`, `Config`, etc.).
- Fix: use explicit aliases in code.

6. Parity drift from over-modernizing too early
- Symptom: app looks nicer but operations differ.
- Fix: complete parity first, then redesign with guardrails.

## 11. Cutover, Rollback, Hypercare

## 11.1 Cutover preparation

1. Freeze source changes.
2. Final rehearsal with production-like data.
3. Prepare operational smoke checklist.
4. Pre-stage rollback artifacts and scripts.

## 11.2 Cutover execution (big-bang example)

1. Stop legacy writes.
2. Final data sync/verification.
3. Deploy new app.
4. Execute smoke tests for critical modules:
- login/session
- interfaces status
- routes/services update
- commands/update
- backup/json/pdf

## 11.3 Rollback criteria

Define objective rollback triggers:
- critical workflow failure not resolved within SLA window
- data corruption risk
- integration outage impact threshold

## 11.4 Hypercare

Track for first 24-72 hours:
- auth/session failures
- command execution failures
- external integration timeouts
- key module error rates
- operator-reported workflow regressions

## 12. Framework-Specific Mapping Cheat Sheet

Use this to adapt the same migration plan to your chosen stack.

## Java target (Spring Boot + React/Vue)

- Compatibility API: Spring MVC controllers
- Services: Spring services + repositories
- DB: JDBC/JPA/MyBatis (use parameterized queries)
- UI: React/Vue pages mapped from GWT modules
- Tests: JUnit + MockMvc + Playwright/Cypress

## JavaScript/TypeScript target (Node/Nest + React/Vue)

- Compatibility API: Nest/Express routes
- Services: injectable classes
- DB: Prisma/Knex/TypeORM with transactions
- UI: React/Vue SPA with operator workflows
- Tests: Jest + supertest + Playwright/Cypress

## Python target (FastAPI/Django + React/Vue)

- Compatibility API: FastAPI routers or Django views
- Services: domain service modules
- DB: SQLAlchemy/Django ORM with explicit transactions
- UI: React/Vue frontend, preserve workflow behavior
- Tests: pytest + httpx + Playwright

## .NET target (ASP.NET Core + Blazor/React/Angular)

- Compatibility API: minimal APIs/controllers
- Services: DI interfaces + infrastructure adapters
- DB: Npgsql/Dapper/EF Core with parameterized SQL
- UI: Blazor or SPA preserving flow parity
- Tests: xUnit + WebApplicationFactory + bUnit/Playwright

## 13. Recommended Migration Artifacts (Create These Files)

1. `PARITY_MATRIX.md`
2. `DB_BEHAVIOR_MATRIX.md`
3. `INTEGRATION_CONTRACTS.md`
4. `UI_WORKFLOW_CHECKLIST.md`
5. `MIGRATION_STATUS.md`
6. `CUTOVER_PLAYBOOK.md`
7. `ROLLBACK_PLAYBOOK.md`
8. `HYPERCARE_RUNBOOK.md`

## 14. Definition of Done for Migration

A migration is done only when all of these are true:
1. Functional parity achieved or signed deviations documented.
2. Security posture improved (parameterized SQL, guarded commands, validated uploads).
3. Integration behavior preserved under normal and failure paths.
4. Operator UAT passes for every module.
5. Automated tests provide regression coverage for contracts, DB deltas, and UI interactions.
6. Cutover and rollback tested and documented.

## 15. Reusable Execution Checklist

## Discovery
- [ ] Operation inventory complete
- [ ] DB behavior matrix complete
- [ ] Integration contract complete
- [ ] Golden fixtures captured

## Implementation
- [ ] Compatibility API scaffolded
- [ ] Business services ported
- [ ] DB logic ported with transactions
- [ ] Command/socket abstractions implemented
- [ ] UI module workflows ported

## Verification
- [ ] Contract tests green
- [ ] DB delta tests green
- [ ] Integration tests green
- [ ] UI interaction tests green
- [ ] Security checks green

## Release
- [ ] Staging rehearsal done
- [ ] Cutover checklist approved
- [ ] Rollback tested
- [ ] Hypercare staffing and metrics ready

---

## Appendix A: Practical Tips From This Migration

1. Keep a dedicated `/api/legacy/*` namespace during migration.
2. Add deterministic test doubles for endpoint tests to decouple DB state.
3. Keep local DB in Docker with scripted initialization for repeatability.
4. Track parity per module and endpoint, not only as one global status.
5. Modernize UI visuals gradually after workflow parity has test coverage.

## Appendix B: Suggested Starter Timeline

For medium-size legacy GWT apps:
1. Discovery and parity spec: 2-4 weeks
2. Architecture and skeleton: 1 week
3. Contracts and data model port: 1-2 weeks
4. Business/data parity: 4-8 weeks
5. Integration parity: 2-4 weeks
6. UI/workflow parity: 3-6 weeks
7. Verification/cutover prep: 2-3 weeks

Adjust based on module complexity, team size, and integration depth.
