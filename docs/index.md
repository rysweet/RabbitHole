# Alice 3 Modernization

Alice 3 is a teaching tool for building 3D stories, animations, and simple games.
RabbitHole keeps that classroom experience working while the codebase is modernized, tested, and broken into smaller pieces that are easier to change safely.

## Validation overview

RabbitHole is validated with Maven, Checkstyle, headless no-Sims tests,
Getting Started headless validation, and coverage reporting. See
[Testing](./testing.md) for the maintained command list and local validation
expectations.

## Quick links

- [Getting started](./getting-started.md)
- [Architecture](./architecture.md)
- [Testing](./testing.md)
- [Verify CI GUI, Eatme, and Xvfb Readiness](./howto/verify-ci-gui-eatme-xvfb-readiness.md)
- [Eatme Object Transform Workflow](./tools-eatme-object-transform.md)
- [Eatme Reopen Project Tool](./tools-eatme-reopen-project.md)
- [Generated source validation](./reference/generated-source-validation.md)
- [Verify Text Migration Registry Parity](./howto/verify-text-migration-parity.md)
- [RabbitHole baseline parity](./rabbithole-baseline-parity.md)
- [Dual-baseline replay harness](./dual-baseline-replay-harness.md)
- [Contributing](./contributing.md)
- [Repository hygiene](./repository-hygiene.md)
- [UI Prompt Boundary](./concepts/ui-prompt-boundary.md)
- [Open Asset Import Pipeline API](./reference/open-asset-import-pipeline-api.md)
- [Use Open 3D Assets](./howto/use-open-3d-assets.md)
- [Verify the Default Open 3D Asset Workflow](./howto/verify-default-open-3d-asset-workflow.md)
- [Concepts](#concepts)
- [How-to guides](#how-to-guides)
- [Tutorials](#tutorials)
- [Reference](#reference)
- [Architecture Atlas](#architecture-atlas)

## What this site covers

- how to clone, build, test, validate, and package Alice 3
- how the Maven modules fit together
- how characterization tests protect refactors
- how save, export, golden corpus, migration, desktop proof, and QA contracts work
- how the text-only RabbitHole baseline parity snapshots catch generated-output drift
- how the dual-baseline replay harness compares RabbitHole against a local preserved Alice baseline when configured
- how reusable code requests UI prompts without opening Swing dialogs in headless contexts
- how GUI-capable CI lanes, Eatme wrappers, the deterministic object-transform workflow, and Xvfb outside-in evidence are verified
- how normalized open COLLADA assets flow through Java model loading to `SkeletonVisual`, `.glb`, `.a3r`, and `.a3t` output
- how every external build input is pinned to an exact, immutable identifier for reproducible, tamper-evident builds

## Documentation map

### Start here

- [Getting started](./getting-started.md)
- [Architecture](./architecture.md)
- [Testing](./testing.md)
- [Verify CI GUI, Eatme, and Xvfb Readiness](./howto/verify-ci-gui-eatme-xvfb-readiness.md)
- [Eatme Object Transform Workflow](./tools-eatme-object-transform.md)
- [Eatme Reopen Project Tool](./tools-eatme-reopen-project.md)
- [Generated source validation](./reference/generated-source-validation.md)
- [Verify Text Migration Registry Parity](./howto/verify-text-migration-parity.md)
- [RabbitHole baseline parity](./rabbithole-baseline-parity.md)
- [Dual-baseline replay harness](./dual-baseline-replay-harness.md)
- [Contributing](./contributing.md)
- [Repository hygiene](./repository-hygiene.md)

### Architecture deep dives

- [Singletons](./architecture/singletons.md)
- [Event Handler Thread Safety](./architecture/event-handler-thread-safety.md)

### Concepts

- [Formal Specification Lane](./concepts/formal-spec-lane.md)
- [Migration Hotspot Characterization](./concepts/migration-hotspot-characterization.md)
- [Process Termination Boundary](./concepts/process-termination-boundary.md)
- [UI Prompt Boundary](./concepts/ui-prompt-boundary.md)
- [Scoped Clipboard Operation Registries](./concepts/scoped-clipboard-operation-registries.md)
- [Reflection Sweep Contracts](./concepts/reflection-sweep-contracts.md)
- [Supply-Chain Dependency Pinning](./concepts/supply-chain-dependency-pinning.md)

### How-to guides

- [Extract a Class for a Lesson](./howto/extract-a-class-for-a-lesson.md)
- [Import an Open 3D Asset](./howto/import-open-3d-asset.md)
- [Use Open 3D Assets](./howto/use-open-3d-assets.md)
- [Verify the Default Open 3D Asset Workflow](./howto/verify-default-open-3d-asset-workflow.md)
- [Verify Text Migration Registry Parity](./howto/verify-text-migration-parity.md)
- [Request Process Termination Safely](./howto/request-process-termination.md)
- [Use the UI Prompt Boundary](./howto/use-ui-prompt-boundary.md)
- [Use a Scoped Clipboard Operation Registry](./howto/use-scoped-clipboard-operation-registry.md)
- [Replace Reflection Sweeps with Explicit Contracts](./howto/replace-reflection-sweeps.md)
- [Replace Test Sleeps with Deterministic Waits](./howto/replace-test-sleeps.md)
- [Verify Generated Source Validation](./howto/verify-generated-source-validation.md)
- [Verify CI GUI, Eatme, and Xvfb Readiness](./howto/verify-ci-gui-eatme-xvfb-readiness.md)
- [Verify PGP Signatures of Maven Artifacts](./howto/run-pgpverify.md)
- [Pin and Audit Dependencies](./howto/pin-and-audit-dependencies.md)

### Tutorials

- [Add a UI Prompt Boundary Adapter](./tutorials/add-ui-prompt-boundary-adapter.md)
- [Import a COLLADA Open Asset](./tutorials/import-collada-open-asset.md)

### Reference

- [GL Graphics2D Contract](./reference/gl-graphics2d-contract.md)
- [Forbidden Pattern Inventory](./reference/forbidden-pattern-inventory.md)
- [Generated Source Validation](./reference/generated-source-validation.md)
- [JavaFX Xvfb Launcher Reference](./reference/javafx-xvfb-launcher.md)
- [CI GUI, Eatme, and Xvfb Validation](./reference/ci-gui-eatme-xvfb-validation.md)
- [Eatme Object Transform Workflow](./tools-eatme-object-transform.md)
- [Eatme Reopen Project Tool](./tools-eatme-reopen-project.md)
- [Modernization Scorecard Generator](./reference/modernization-scorecard-generator.md)
- [Merge-ready Evidence Generator](./reference/merge-ready-evidence-generator.md)
- [Modernization Corpus Manifest](./reference/modernization-corpus-manifest.md)
- [Open Asset Import Pipeline API](./reference/open-asset-import-pipeline-api.md)
- [Default Open 3D Asset Workflow Test](./reference/default-open-3d-asset-workflow-test.md)
- [Release Management Checklist](./reference/release-management-checklist.md)
- [Text Migration Registry Parity](./reference/text-migration-registry-parity.md)
- [Process Termination API](./reference/process-termination-api.md)
- [UI Prompt Boundary API](./reference/ui-prompt-boundary-api.md)
- [Clipboard Operation Registry](./reference/clipboard-operation-registry.md)
- [Reflection Smoke Support](./reference/reflection-smoke-support.md)
- [System.exit Allowlist](./reference/system-exit-allowlist.md)
- [Deterministic Test Waits](./reference/deterministic-test-waits.md)
- [Hybrid Tweedle/XML Archive Format](./tweedle-hybrid-format.md)
- [Tweedle Decoder-Gap Backlog](./tweedle-decode-gaps.md)
- [Dependency Pinning Audit](./reference/dependency-pinning-audit.md)

### Architecture Atlas

- [Atlas Overview](./atlas/index.md)
- [Repository Surface](./atlas/repo-surface/README.md)
- [Symbol Bindings](./atlas/ast-lsp-bindings/README.md)
- [Compile Dependencies](./atlas/compile-deps/README.md)
- [Runtime Topology](./atlas/runtime-topology/README.md)
- [API Contracts](./atlas/api-contracts/README.md)
- [Data Flow](./atlas/data-flow/README.md)
- [Service Components](./atlas/service-components/README.md)
- [User Journeys](./atlas/user-journeys/README.md)
