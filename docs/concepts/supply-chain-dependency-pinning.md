---
title: Supply-Chain Dependency Pinning
description: Why every external dependency source in RabbitHole is pinned to an exact, immutable identifier and how the trust boundaries are drawn.
last_updated: 2026-07-23
review_schedule: quarterly
owner: modernization
doc_type: concept
related:
  - ../reference/dependency-pinning-audit.md
  - ../howto/pin-and-audit-dependencies.md
---

# Supply-Chain Dependency Pinning

RabbitHole builds are **reproducible and tamper-evident**. Every external input
that participates in a build — Maven artifacts, Maven plugins, GitHub Actions,
and the Tweedle grammar submodule — resolves to an exact, immutable identifier.
No build step depends on a mutable pointer that a third party can re-target after
review.

This concept explains *why* pinning exists, *what* counts as an external input,
and *where* the trust boundaries are drawn. For the concrete inventory and the
current pinned values, see the
[Dependency Pinning Audit](../reference/dependency-pinning-audit.md). For the
step-by-step procedure to add or update a pin, see
[Pin and Audit Dependencies](../howto/pin-and-audit-dependencies.md).

## The threat we are closing

A mutable dependency reference — a Maven version range, `LATEST`, `RELEASE`, or a
GitHub Action tag such as `@v4` — points at *whatever the upstream owner has that
tag resolve to today*. The bytes a maintainer reviewed are not guaranteed to be
the bytes that execute on the next run. An attacker who gains push access to an
upstream tag, or who publishes a higher version into a range window, can inject
code into RabbitHole's build and CI without any change landing in this
repository.

This is the class of attack catalogued as **OWASP CICD-SEC-3 (Dependency Chain
Abuse)** and **CICD-SEC-4 (Poisoned Pipeline Execution)**. The protected asset is
**build integrity**: the guarantee that the reviewed input is the executed input.

Pinning to an immutable identifier closes the window. A 40-character commit SHA
or an exact artifact version names one specific, content-addressed set of bytes.
Re-targeting a tag no longer changes what RabbitHole runs.

## What counts as an external input

RabbitHole draws its build-time trust boundary around five surfaces:

| Surface | Immutable identifier | Pinned by |
| --- | --- | --- |
| Maven dependencies (`<dependency>`) | exact `<version>` | centralized `<dependencyManagement>` |
| Maven plugins (`<plugin>`) | exact `<version>` | centralized `<pluginManagement>` |
| GitHub Actions (`uses:`) | full 40-char commit SHA | `# vX.Y.Z` trailing comment |
| Tweedle grammar submodule | gitlink commit SHA | `.gitmodules` + recorded gitlink |
| Local composite actions (`./.github/...`) | in-repo path | version-controlled with the repo |

Anything outside this boundary that can influence a build is treated as
untrusted until it is pinned.

## Trust boundaries and what is intentionally *not* pinned

Not every version-like string is an external input, and pinning the wrong thing
breaks the reactor build. The following are deliberately excluded:

- **`org.alice` reactor modules** carry `-SNAPSHOT` versions. These are
  *in-repo* modules resolved from the local reactor, not downloaded from a remote
  repository. Their version is inherited from the parent POM and must stay a
  `SNAPSHOT`. Adding a fixed `<version>` to a reactor sibling breaks module
  resolution.
- **Maven Enforcer `requireMavenVersion`** uses a range such as `[3.9.9,4)`.
  This is a *constraint on the developer's Maven install*, not a dependency
  coordinate. It is not fetched and is left as a range on purpose.
- **m2e lifecycle-mapping filters** use a range such as `[0.1,)`. This is IDE
  metadata consumed by Eclipse tooling, not a build dependency.
- **NetBeans platform identifiers** such as `RELEASE180` are module-system
  identifiers, not Maven `RELEASE` version keywords, and are not pinned.
- **Local composite actions** (`uses: ./.github/actions/setup-xvfb`) are stored
  in this repository and reviewed with every change. They need no SHA because the
  in-repo path already resolves to the reviewed commit. The composite action is
  `run:`-only and declares no external `uses:`, so it introduces no additional
  trust surface.

## Immutability guarantees per surface

**Maven** — A pinned dependency or plugin names an exact version in Maven Central
(or a declared repository). Maven Central artifacts are immutable once published;
a coordinate plus version resolves to fixed bytes. Ranges, `LATEST`, and
`RELEASE` are prohibited because they re-resolve over time.

**GitHub Actions** — A tag such as `v4` is a mutable Git ref. A 40-character
commit SHA is content-addressed and immutable. RabbitHole pins the SHA and
records the human-readable version in a trailing comment so reviewers keep
semantic context:

```yaml
uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
```

The comment is documentation; the SHA is the security control.

**Tweedle grammar submodule** — A submodule records a gitlink to one specific
upstream commit. `git submodule status tweedle-lang` shows the pinned SHA. The
submodule contributes grammar and data files only (no build manifest and no
transitive dependencies), so pinning the commit fully determines its
contribution to the build.

## Relationship to reproducible builds

Pinning is a prerequisite for reproducibility, not the whole of it. Because every
external input is immutable, two checkouts of the same RabbitHole commit resolve
the same artifacts, the same action bytes, and the same grammar, on any machine
with the documented JDK and local Maven cache. This is what lets the
[Testing](../testing.md) suite produce a stable result (the audit's JDK 21
validation run recorded 58272 run / 2236 skipped / 0 failures / 0 errors) rather
than a moving target.

## Keeping pins current without losing immutability

Pinning trades "always newest" for "always reviewed". A pinned SHA is a
*point-in-time* snapshot: it stays correct until someone deliberately bumps it.
The practice that keeps pins from going stale today is:

- Treat every pin change as a normal reviewed change on the `develop` base
  branch, so a bump is only ever the reviewed input.
- When a reviewer spots a non-SHA `uses:` reference or a Maven range in a diff,
  pin it before merge.

See [Pin and Audit Dependencies](../howto/pin-and-audit-dependencies.md) for the
exact commands.

### Planned hardening (not yet implemented)

The following controls would automate the above but are **not** part of the
current audit state. They are tracked as follow-up work, not delivered
guarantees:

- **Dependabot for the `github-actions` ecosystem**, so SHA bumps arrive as
  reviewable pull requests that preserve immutability.
- **Explicit top-level `permissions: contents: read`** on each workflow, to
  minimize the token scope available to any pinned action.
- **A CI lint that rejects non-SHA `uses: …@vN` references**, turning the
  "pin before merge" convention into an enforced gate.

Until these land, pin currency depends on the manual review discipline above.
