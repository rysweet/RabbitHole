---
title: CodeQL SAST Workflow Reference
description: Reference for the CodeQL static analysis workflow (.github/workflows/codeql.yml) including triggers, permissions, steps, inputs, and configuration.
last_updated: 2026-07-24
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: reference
related:
  - ../howto/run-codeql-sast-analysis.md
---

# CodeQL SAST Workflow Reference

The CodeQL workflow performs static application security testing (SAST) on the
Java/Kotlin reactor and uploads findings to GitHub code scanning.

- **File:** `.github/workflows/codeql.yml`
- **Workflow name:** `CodeQL`
- **Job:** `analyze` — *Analyze (java-kotlin)*
- **Runner:** `ubuntu-latest`
- **Timeout:** `120` minutes

## Contents

- [Triggers](#triggers)
- [Permissions](#permissions)
- [Concurrency](#concurrency)
- [Steps](#steps)
- [Action versions](#action-versions)
- [Configuration inputs](#configuration-inputs)
- [Guardrails](#guardrails)

## Triggers

```yaml
on:
  push:
    branches: [develop]
  pull_request:
    branches: [develop]
  schedule:
    - cron: '17 7 * * 1'
```

| Event | Filter | Purpose |
| --- | --- | --- |
| `push` | `develop` | Scan every merged/pushed change on the base branch |
| `pull_request` | `develop` | Scan proposed changes before merge (read-only token on forks) |
| `schedule` | `17 7 * * 1` | Weekly baseline scan (Mondays 07:17 UTC) to pick up new queries and advisories |

## Permissions

Top-level permissions default to read-only; the `analyze` job elevates only the
scopes CodeQL requires.

```yaml
permissions:
  contents: read            # top level

jobs:
  analyze:
    permissions:
      security-events: write # upload code scanning results
      contents: read         # check out source
      actions: read          # read workflow run metadata
```

The workflow deliberately uses `pull_request` rather than `pull_request_target`,
so pull requests originating from forks receive a read-only `GITHUB_TOKEN`.

## Concurrency

```yaml
concurrency:
  group: codeql-${{ github.event_name == 'pull_request' && github.event.pull_request.number || github.run_id }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}
```

Pull-request runs are grouped by PR number and superseded by newer pushes to the
same PR. Non-PR runs (`push` to `develop`, `schedule`) use the unique run ID as
the group key and are never cancelled.

## Steps

| # | Name | Action / command |
| --- | --- | --- |
| 1 | Check out source | `actions/checkout@v4` (`submodules: false`, `fetch-depth: 1`) |
| 2 | Initialize Tweedle grammar submodule | `git submodule update --init tweedle-lang` |
| 3 | Set up JDK 21 | `actions/setup-java@v4` (`distribution: temurin`, `java-version: '21'`, `cache: maven`) |
| 4 | Initialize CodeQL | `github/codeql-action/init@v3` (`languages: java-kotlin`, `build-mode: manual`) |
| 5 | Build (Maven) | fast install (see below) |
| 6 | Perform CodeQL Analysis | `github/codeql-action/analyze@v3` (`category: /language:java-kotlin`) |

### Build (Maven) command

```bash
mvn --settings .github/maven/jogamp-ci-settings.xml -U \
  -DskipTests -Dcheckstyle.skip \
  -Dlicense.skipAggregateDownloadLicenses=true \
  -Dinstall4j.skip -Djava.awt.headless=true -q install
```

| Flag | Effect |
| --- | --- |
| `--settings .github/maven/jogamp-ci-settings.xml` | Resolve `org.jogamp.*` (JOGL/GlueGen) via the CI-approved SciJava mirror |
| `-U` | Force a check for updated releases/snapshots |
| `-DskipTests` | Compile without running tests (extraction only needs compiled bytecode) |
| `-Dcheckstyle.skip` | Skip Checkstyle |
| `-Dlicense.skipAggregateDownloadLicenses=true` | Skip aggregate license download |
| `-Dinstall4j.skip` | Skip install4j packaging |
| `-Djava.awt.headless=true` | Run headless (no display) |
| `-q install` | Quiet install lifecycle |

The JDK comes from the `Set up JDK 21` step (`actions/setup-java` exports
`JAVA_HOME` for Temurin 21), so the build command does not hardcode a JDK path.

> **Consistency note:** The `--settings .github/maven/jogamp-ci-settings.xml -U`
> arguments match the repository's other CI Maven jobs
> ([`alice-test-ci.yml`](../../.github/workflows/alice-test-ci.yml),
> [`alice-coverage-ci.yml`](../../.github/workflows/alice-coverage-ci.yml)), which
> route `org.jogamp.*` (JOGL/GlueGen) resolution through the CI-approved SciJava
> mirror. The build fails to resolve those dependencies without them.

## Action versions

| Action | Pin |
| --- | --- |
| `actions/checkout` | `@v4` |
| `actions/setup-java` | `@v4` |
| `github/codeql-action/init` | `@v3` |
| `github/codeql-action/analyze` | `@v3` |

Pins use major-version tags to mirror the rest of the repository's workflows.
Full commit-SHA pinning is an optional defense-in-depth hardening step and, if
adopted, should be applied repo-wide.

## Configuration inputs

Key CodeQL inputs and how to change them:

| Setting | Location | Notes |
| --- | --- | --- |
| Analyzed language | `init.languages: java-kotlin` | Single combined Java/Kotlin analysis |
| Build mode | `init.build-mode: manual` | CodeQL observes the real Maven build |
| Result category | `analyze.category: /language:java-kotlin` | Groups results in code scanning |
| JDK | `setup-java.java-version: '21'` | Temurin 21; keep in sync with the reactor's target |
| Schedule | `schedule.cron: '17 7 * * 1'` | Adjust cadence here |
| Job timeout | `timeout-minutes: 120` | Raise only if the build legitimately exceeds it |

## Guardrails

- The Tweedle grammar submodule **must** be initialized before the Maven build,
  or extraction fails with missing generated parser classes.
- The workflow does not modify source, `pom.xml`, or `org.alice` `-SNAPSHOT`
  reactor versions.
- Do not switch to `pull_request_target`; it would grant fork PRs a writable
  token.
- The Maven build flags mirror the project's fast CI build; keep them
  synchronized if that build changes. Like the other CI jobs, this step passes
  `--settings .github/maven/jogamp-ci-settings.xml -U` so JOGL/GlueGen
  (`org.jogamp.*`) resolution goes through the CI-approved mirror.

## Related

- How-to: [Run CodeQL SAST Analysis](../howto/run-codeql-sast-analysis.md)
