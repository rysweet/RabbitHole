---
title: Run CodeQL SAST Analysis
description: How the CodeQL static application security testing (SAST) workflow scans the java-kotlin reactor and how to interpret, trigger, and maintain it.
last_updated: 2026-07-24
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: howto
related:
  - ../reference/codeql-sast-workflow.md
---

# Run CodeQL SAST Analysis

RabbitHole runs [CodeQL](https://codeql.github.com/) static application security
testing (SAST) against the Java/Kotlin sources on every change to `develop` and
on a weekly schedule. The workflow lives at
[`.github/workflows/codeql.yml`](../../.github/workflows/codeql.yml) and reports
findings to the repository **Security → Code scanning alerts** tab.

Use this guide when you need to understand why the scan runs, how to trigger it,
how to read its results, or how to keep it maintainable.

## What it scans

- **Language:** `java-kotlin` (a single CodeQL analysis that covers both the Java
  and Kotlin sources in the reactor).
- **Build mode:** `manual`. CodeQL observes the project's real Maven build rather
  than guessing how to compile it, so extracted results match what ships.
- **Scope:** the full reactor as compiled by the fast install build described
  below. Source, `pom.xml` files, and `org.alice` `-SNAPSHOT` reactor versions
  are never modified by the workflow.

## When it runs

The workflow triggers on three events:

| Trigger | Condition |
| --- | --- |
| `push` | Commits pushed to `develop` |
| `pull_request` | PRs targeting `develop` |
| `schedule` | Weekly cron `17 7 * * 1` (Mondays, 07:17 UTC) |

The weekly schedule catches newly published CodeQL queries and advisories even
when no code changes land.

Concurrent runs for the same pull request are collapsed: a new push to a PR
cancels the in-flight scan for that PR, while pushes to `develop` and scheduled
runs always complete.

## Permissions

The token follows least privilege. The workflow declares `contents: read` at the
top level, and the `analyze` job elevates only what CodeQL needs:

```yaml
permissions:
  security-events: write   # upload results to code scanning
  contents: read           # check out source
  actions: read            # read workflow run metadata
```

Because the workflow uses `pull_request` (never `pull_request_target`), pull
requests from forks build with a **read-only** token — untrusted fork code is
compiled but cannot write results or exfiltrate secrets.

## How the build works

CodeQL's `manual` build mode requires the workflow to compile the project itself.
The steps run in this order:

1. **Check out source** — `actions/checkout@v4` with `submodules: false`.
2. **Initialize the Tweedle grammar submodule** — required so the generated
   Tweedle parser classes exist before Maven runs:

   ```bash
   git submodule update --init tweedle-lang
   ```

3. **Set up JDK 21** — `actions/setup-java@v4`, Temurin distribution, with the
   Maven dependency cache enabled. This exports `JAVA_HOME` for the build, so the
   step below does not hardcode a JDK path.
4. **Initialize CodeQL** — `github/codeql-action/init@v3` with
   `languages: java-kotlin` and `build-mode: manual`.
5. **Build (Maven)** — a fast install that skips tests and non-essential
   plugins so the scan stays quick:

   ```bash
   mvn --settings .github/maven/jogamp-ci-settings.xml -U \
     -DskipTests -Dcheckstyle.skip \
     -Dlicense.skipAggregateDownloadLicenses=true \
     -Dinstall4j.skip -Djava.awt.headless=true -q install
   ```

6. **Perform CodeQL Analysis** — `github/codeql-action/analyze@v3` with
   `category: /language:java-kotlin`.

The Maven invocation mirrors the project's CI build flags (skip tests,
Checkstyle, license aggregation, and install4j) so CodeQL extraction compiles the
real project rather than a guessed build. It also routes dependency resolution
through `--settings .github/maven/jogamp-ci-settings.xml -U`, the same shared
settings file the other CI Maven jobs use to resolve the JOGL/GlueGen
(`org.jogamp.*`) artifacts via the CI-approved SciJava mirror. Without it, the
build fails to resolve those dependencies.

## Reproduce the build locally

You do not need to run CodeQL locally, but you can reproduce the exact compile
the workflow uses to debug extraction or build failures. From the repository
root:

```bash
git submodule update --init tweedle-lang
mvn --settings .github/maven/jogamp-ci-settings.xml -U \
  -DskipTests -Dcheckstyle.skip \
  -Dlicense.skipAggregateDownloadLicenses=true \
  -Dinstall4j.skip -Djava.awt.headless=true -q install
```

If Maven reports missing generated Tweedle parser classes, confirm the submodule
is present:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar && echo "grammar present"
```

The `--settings .github/maven/jogamp-ci-settings.xml` argument is what lets Maven
resolve the `org.jogamp.*` (JOGL/GlueGen) artifacts through the CI-approved
SciJava mirror; omit it and dependency resolution fails.

## Read the results

1. Open the repository's **Security** tab.
2. Select **Code scanning alerts**.
3. Filter by **Tool: CodeQL**. Each alert links to the offending source location,
   an explanation, and remediation guidance.

On pull requests, CodeQL findings introduced by the change also surface as PR
checks and inline annotations, so reviewers see new alerts before merge.

## Trigger a scan manually

The scan runs automatically on the events above. To force a fresh run without a
code change, either:

- Push an empty commit to a PR branch targeting `develop`, or
- Wait for the weekly scheduled run, or
- Re-run the latest **CodeQL** workflow from the **Actions** tab.

## Maintain the workflow

Keep the workflow easy to reason about:

- **Keep the build command in sync.** If the project's fast build flags change,
  update the `Build (Maven)` step so CodeQL keeps compiling the real project.
- **Preserve the submodule step.** Removing it breaks extraction with missing
  Tweedle parser classes.
- **Do not switch to `pull_request_target`.** That would grant fork PRs a
  writable token.
- **Action pinning.** Actions are pinned to `@v4`/`@v3` major tags to mirror the
  rest of the repository. For defense-in-depth you may pin to full commit SHAs;
  do this repo-wide rather than only here.
- **Timeout.** The job caps at `timeout-minutes: 120`; raise it only if the
  reactor grows enough to exceed it legitimately.

## Related

- Reference: [CodeQL SAST Workflow](../reference/codeql-sast-workflow.md)
- GitHub docs: [Configuring advanced setup for code scanning](https://docs.github.com/en/code-security/code-scanning/creating-an-advanced-setup-for-code-scanning/configuring-advanced-setup-for-code-scanning)
