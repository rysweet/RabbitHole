---
title: Dependency Pinning Audit
description: Inventory of every external dependency source in RabbitHole and its pinned, immutable identifier, plus the allowlist of version-like strings that are intentionally not pinned.
last_updated: 2026-07-23
review_schedule: quarterly
owner: modernization
doc_type: reference
related:
  - ../concepts/supply-chain-dependency-pinning.md
  - ../howto/pin-and-audit-dependencies.md
---

# Dependency Pinning Audit

This reference is the authoritative inventory of external build inputs in
RabbitHole and the immutable identifier each is pinned to. It is the artifact of
the supply-chain security audit and the source of truth for reviewers verifying
that no mutable reference has slipped back in.

Conceptual background: [Supply-Chain Dependency Pinning](../concepts/supply-chain-dependency-pinning.md).
Procedure: [Pin and Audit Dependencies](../howto/pin-and-audit-dependencies.md).

## Audit scope

The audit covers every source that can inject bytes into a build or CI run:

1. Maven dependencies and plugins across all module POMs.
2. The Maven wrapper distribution.
3. GitHub Actions in `.github/workflows/*.yml`.
4. The local composite action `.github/actions/setup-xvfb`.
5. The `tweedle-lang` grammar submodule and its dependencies.

## Summary of findings

| Surface | Unpinned findings | Action taken |
| --- | --- | --- |
| Maven dependencies | none — all fixed via `<dependencyManagement>` | verified, no change |
| Maven plugins | none — all fixed via `<pluginManagement>` | verified, no change |
| Maven wrapper | not present (system Maven used) | documented, nothing to pin |
| GitHub Actions | 6 actions on mutable major tags (`@v4`, `@v5`, `@v3`) | pinned to 40-char SHAs |
| `setup-xvfb` composite | `run:`-only, no external `uses:` | verified, no change |
| `tweedle-lang` submodule | gitlink pinned; data-only, no deps | verified, no change |

The only actionable finding was the GitHub Actions surface. Everything else was
already pinned.

## GitHub Actions — pinned values

All six external actions are pinned to the full commit SHA that the resolved
version tag points to, with the human-readable version in a trailing comment.
The SHAs below are the values resolved at audit time; they are **point-in-time**
snapshots and stay valid until a pin is deliberately bumped (see
[verify a pin later](../howto/pin-and-audit-dependencies.md#verify-a-pin-later)
to detect drift).

| Action | Pinned SHA | Version |
| --- | --- | --- |
| `actions/checkout` | `11d5960a326750d5838078e36cf38b85af677262` | v4.4.0 |
| `actions/setup-java` | `c1e323688fd81a25caa38c78aa6df2d33d3e20d9` | v4.8.0 |
| `actions/setup-python` | `a26af69be951a213d495a4c3e4e4022e16d87065` | v5.6.0 |
| `actions/upload-artifact` | `ea165f8d65b6e75b540449e92b4886f43607fa02` | v4.6.2 |
| `actions/upload-pages-artifact` | `56afc609e74202658d3ffba0e8f6dda462b719fa` | v3.0.1 |
| `actions/deploy-pages` | `d6db90164ac5ed86f2b6aed7e0febac5b3c0c03e` | v4.0.5 |

### Where each pin appears

| Workflow | Actions pinned |
| --- | --- |
| `.github/workflows/alice-checkstyle-ci.yml` | checkout, setup-java |
| `.github/workflows/alice-coverage-ci.yml` | checkout, setup-java, upload-artifact |
| `.github/workflows/alice-netbeans-package-ci.yml` | checkout, setup-java |
| `.github/workflows/alice-test-ci.yml` | checkout (×2), setup-java (×2), `./.github/actions/setup-xvfb` |
| `.github/workflows/docs.yml` | checkout, setup-python, upload-pages-artifact, deploy-pages |

The reference `uses: ./.github/actions/setup-xvfb` is a local composite action
and is intentionally left as an in-repo path — see the allowlist below.

## Maven — verification result

All Maven `<dependency>` and `<plugin>` versions are fixed literals or fixed
properties, centralized in the root POM's `<dependencyManagement>` and
`<pluginManagement>`. A static scan of all module POMs found **no** version
ranges, `LATEST`, or `RELEASE` keywords in dependency or plugin coordinates. No
POM changes were required.

## Tweedle grammar submodule — verification result

`tweedle-lang` is pinned via its gitlink to a specific upstream commit
(`git submodule status tweedle-lang` reports commit `f6fc9e5`). The submodule
contains grammar and data only — no `pom.xml`, `build.gradle`, or other build
manifest — so it declares no transitive dependencies of its own. Pinning the
commit fully determines its contribution.

## Maven wrapper — verification result

The repository does not include `mvnw`, `mvnw.cmd`, or
`.mvn/wrapper/maven-wrapper.properties`. Builds use the system Maven install
(constrained by the Enforcer `requireMavenVersion` rule). There is no wrapper
distribution URL to pin.

## Allowlist — version-like strings that are intentionally not pinned

These strings look like versions but are **not** external dependency coordinates.
Reviewers should not "fix" them.

| String | Location | Why it is excluded |
| --- | --- | --- |
| `-SNAPSHOT` on `org.alice` modules | reactor module coordinates | in-repo reactor siblings; must stay SNAPSHOT, version inherited from parent |
| `[3.9.9,4)` | Enforcer `requireMavenVersion` | constraint on the developer's Maven install, not a fetched dependency |
| `[0.1,)` | m2e `lifecycle-mapping` filter | Eclipse IDE metadata, not a build dependency |
| `RELEASE180` | NetBeans module identifier | platform module id, not the Maven `RELEASE` keyword |
| `./.github/actions/setup-xvfb` | workflow `uses:` | local composite action, versioned in-repo; no SHA needed |

## How to re-run the audit

The audit is reproducible. See
[Pin and Audit Dependencies](../howto/pin-and-audit-dependencies.md) for the
commands that:

- scan all POMs for dynamic versions,
- list every `uses:` reference across workflows and composite actions,
- resolve a version tag to its commit SHA, and
- validate the build under JDK 21.

## Planned hardening — not yet implemented

The audit pinned every mutable reference, but three automation controls that
would keep pins honest over time are **out of scope for this audit** and remain
follow-up work. They are listed here so the "finished state" of the audit is not
mistaken for a fully automated one:

| Control | Status | Purpose |
| --- | --- | --- |
| Dependabot `github-actions` ecosystem | not configured | propose SHA bumps as reviewable PRs |
| Top-level `permissions: contents: read` on workflows | not applied | minimize action token scope |
| CI lint rejecting non-SHA `uses: …@vN` | not present | enforce "pin before merge" |

Until these are delivered, pin currency and pin enforcement depend on manual
review discipline.

## Verification baseline

After pinning, the build passes under JDK 21 with the documented flags, reaching
`BUILD SUCCESS` across all modules with **0 failures / 0 errors**. Because only
workflow YAML changed, the [Testing](../testing.md) suite result is unchanged
from the pre-audit run (this audit's JDK 21 validation recorded 58272 run /
2236 skipped).
