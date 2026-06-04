# Alice 3 Modernization

Alice 3 is a teaching tool for building 3D stories, animations, and simple games.
RabbitHole keeps that classroom experience working while the codebase is modernized, tested, and broken into smaller pieces that are easier to change safely.

## Current status

- Coverage snapshot: **74%**
- Build system: **22 Maven modules** on **Java 21**
- Main validation lanes: **Checkstyle**, **headless no-Sims tests**, **Getting Started headless validation**, and **JaCoCo coverage**.

## Quick links

- [Getting started](./getting-started.md)
- [Architecture](./architecture.md)
- [Testing](./testing.md)
- [Contributing](./contributing.md)
- [Concepts](#concepts)
- [Architecture Atlas](#architecture-atlas)

## What this site covers

- how to clone, build, test, validate, and package Alice 3
- how the Maven modules fit together
- how characterization tests protect refactors
- how save, export, migration, desktop proof, and QA contracts work

## Documentation map

### Start here

- [Getting started](./getting-started.md)
- [Architecture](./architecture.md)
- [Testing](./testing.md)
- [Contributing](./contributing.md)

### Architecture deep dives

- [Singletons](./architecture/singletons.md)
- [Event Handler Thread Safety](./architecture/event-handler-thread-safety.md)

### Concepts

- [Formal Specification Lane](./concepts/formal-spec-lane.md)
- [Migration Hotspot Characterization](./concepts/migration-hotspot-characterization.md)

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
