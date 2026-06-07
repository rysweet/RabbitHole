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
- [Verify Text Migration Registry Parity](./howto/verify-text-migration-parity.md)
- [RabbitHole baseline parity](./rabbithole-baseline-parity.md)
- [Dual-baseline replay harness](./dual-baseline-replay-harness.md)
- [Contributing](./contributing.md)
- [Repository hygiene](./repository-hygiene.md)
- [Concepts](#concepts)
- [Architecture Atlas](#architecture-atlas)

## What this site covers

- how to clone, build, test, validate, and package Alice 3
- how the Maven modules fit together
- how characterization tests protect refactors
- how save, export, golden corpus, migration, desktop proof, and QA contracts work
- how the text-only RabbitHole baseline parity snapshots catch generated-output drift
- how the dual-baseline replay harness compares RabbitHole against a local preserved Alice baseline when configured

## Documentation map

### Start here

- [Getting started](./getting-started.md)
- [Architecture](./architecture.md)
- [Testing](./testing.md)
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

### Reference

- [Modernization Scorecard Generator](./reference/modernization-scorecard-generator.md)
- [Modernization Corpus Manifest](./reference/modernization-corpus-manifest.md)
- [Text Migration Registry Parity](./reference/text-migration-registry-parity.md)

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
