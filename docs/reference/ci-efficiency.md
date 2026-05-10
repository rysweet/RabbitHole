# CI efficiency and no-op validation skips

RabbitHole CI preserves the same required validation surfaces while avoiding
expensive Maven work for pull requests that change only documentation or license
metadata. The optimization is implemented inside each required workflow, so the
workflow and job status still complete normally and branch protection is not
bypassed.

The no-op path is intentionally narrow. Any uncertainty, mixed code and
documentation change, build configuration change, workflow change, source change,
test change, QA change, script change, or unknown path runs the original Maven
validation.

## Current checks

Pull requests to `develop` start these required checks in parallel:

| Workflow | Job | Preserved validation surface |
| --- | --- | --- |
| `Alice Checkstyle CI` | `build` | Maven Checkstyle validation. |
| `Alice Test CI` | `test` | No-Sims Maven test baseline. |
| `Alice NetBeans Package CI` | `package-netbeans` | NetBeans package build plus package artifact verification. |
| `Alice Coverage Reports` | `coverage` | No-Sims aggregate and per-module coverage reports, coverage gates, and uploaded evidence artifacts. |
| `GitGuardian Security Checks` | External required check | Repository-owned CI does not modify or replace this security check. |

The repository-owned workflows keep their existing names, job names, triggers,
stale pull request cancellation, and required status semantics. Pull request runs
for the same pull request cancel older in-progress runs; unrelated pull requests
do not cancel each other even when their source branches share a name. `develop`
push runs are kept for history.

## Measured bottleneck baseline

The optimization targets the measured pull request bottlenecks instead of adding
cosmetic workflow churn. The times below are the measured pre-optimization
baseline; the commands shown are the optimized equivalents now used for the same
validation surfaces.

| Check | Current optimized work step | Measured baseline before this optimization |
| --- | --- | ---: |
| `Alice Coverage Reports` | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -Pcoverage verify` | about 13 minutes for the job, including about 12.15 minutes in Maven |
| `Alice Test CI` | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip clean test` | about 5-6 minutes |
| `Alice NetBeans Package CI` | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -pl netbeans -am package -DskipTests` | about 5-6 minutes |
| `Alice Checkstyle CI` | `mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml` | about 1 minute |

Coverage is the primary wall-clock bottleneck because the checks already run in
parallel. NetBeans package and test validation are the secondary bottlenecks.

## Change-scope classifier

Each repository-owned workflow has an internal `change-scope` step. The step
runs only for `pull_request` events, computes the changed file list from the
exact pull request base and head SHAs, and emits these outputs:

| Output | Meaning |
| --- | --- |
| `maven-required` | `true` when the workflow must run its required Maven command; `false` only for clearly non-impacting pull request changes. |
| `reason` | Short classifier reason printed in the workflow log and used by the no-op step. |

Non-`pull_request` events do not use the no-op path or depend on
`change-scope` outputs. Their Maven steps are gated with the event-aware
condition:

```text
github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true'
```

This keeps pushes to `develop` on the full validation path even though
`change-scope` does not run for those events. Pull request diff failures,
base or head SHAs that remain unfetchable after bounded retries, malformed
paths, empty or unavailable file lists, and classifier uncertainty all emit
`maven-required=true`.

The classifier must compare the exact base and head SHAs from the pull request
event instead of relying on the default checkout depth or branch names. It
retries the exact-SHA fetch up to three times with short backoff to tolerate
transient GitHub service or network failures. If either SHA still cannot be
fetched or compared, the workflow fails closed by running Maven. For renames and
copies, the changed path set includes both the old path and the new path so a
source-to-doc rename, doc-to-source rename, or copied source file cannot be
misclassified as documentation-only.

The classifier treats changed paths as untrusted data. It reads paths
line-by-line, quotes variables, never evaluates path contents, and never executes
or sources changed files.

## No-op skip rules

The no-op path is allowed only when every changed path is known to be
non-validation-impacting documentation or license metadata.

Allowed no-op paths:

| Path pattern | Example |
| --- | --- |
| `docs/**` exactly, case-sensitive | `docs/reference/ci-efficiency.md` |
| Root-level `*.md` exactly, case-sensitive | `README.md`, `CONTRIBUTING.md` |
| Exact root license metadata names | `LICENSE`, `LICENSE.md`, `NOTICE`, `NOTICE.md` |

The allowlist is case-sensitive and exact. `docs/**` means paths under the
lowercase root `docs/` directory only; root Markdown means `*.md` files directly
at repository root only; license metadata means only the four root filenames
listed above.

All other paths require Maven validation. This includes, but is not limited to:

| Path kind | Examples |
| --- | --- |
| Source and resources | `core/**`, `netbeans/**`, `application/**` |
| Tests and QA | `tests/**`, `qa/**`, files named `*Test.java` |
| Build and dependency configuration | `pom.xml`, `**/pom.xml`, `.mvn/**`, Maven wrapper files |
| GitHub Actions and automation | `.github/**`, `scripts/**` |
| Project configuration | Checkstyle, coverage, packaging, schema, and tool configuration files |
| Unknown or malformed paths | Empty paths, absolute paths, `..` traversal, paths outside the repository |
| Mixed changes | Any pull request that changes both allowed documentation paths and validation-impacting paths |

The mixed-change rule is deliberate: documentation edits bundled with code,
build, test, QA, workflow, or script edits still run the full validation surface.

## Workflow behavior

For validation-impacting changes, each workflow runs its required Maven command
and dependent evidence steps. Test, coverage, and NetBeans package commands pass
`-Dcheckstyle.skip` because the same pull request still runs the dedicated
`Alice Checkstyle CI` required check. This removes repeated Checkstyle execution
from the three longer jobs without weakening the Checkstyle validation surface.

For docs-only or license-only pull requests, the workflow completes through a
lightweight no-op step. Checkout still runs with Git LFS and submodules disabled
so the classifier can inspect the pull request diff. Tweedle submodule
initialization, JDK setup, Maven cache restore through `setup-java`, `mvn -v`,
the expensive Maven validation command, and Maven-dependent evidence steps run
only when Maven validation is required.

The no-op step prints only the changed file list and the classifier reason. It
does not print environment dumps, token values, cache internals, secrets, or the
full GitHub event payload.

### Checkstyle

`Alice Checkstyle CI` runs:

```sh
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml
```

when the event-aware Maven gate is true:

```text
github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true'
```

For allowed no-op pull requests, the Maven Checkstyle step is skipped and the job
reports the no-op reason.

### Tests

`Alice Test CI` runs:

```sh
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip clean test
```

when the event-aware Maven gate is true:

```text
github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true'
```

For allowed no-op pull requests, the Maven test step is skipped and the job
reports the no-op reason.

### Coverage

`Alice Coverage Reports` runs:

```sh
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -Pcoverage verify
```

when the event-aware Maven gate is true:

```text
github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true'
```

The coverage summary, coverage gates, and `alice-coverage-evidence-no-sims`
artifact upload are Maven-dependent, so they keep `always()` behavior only inside
the Maven-required path:

```text
always() && (github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true')
```

That preserves coverage summary and artifact upload after failed coverage runs,
while preventing docs-only no-op runs from uploading stale or missing evidence.

When coverage is skipped for an allowed no-op pull request, the job succeeds
without claiming fresh coverage evidence for that pull request. The skip is valid
only because the classifier proved that no validation-impacting files changed.

### NetBeans package

`Alice NetBeans Package CI` runs:

```sh
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -pl netbeans -am package -DskipTests
```

when the event-aware Maven gate is true:

```text
github.event_name != 'pull_request' || steps.change-scope.outputs.maven-required == 'true'
```

The NetBeans package artifact verification step is Maven-dependent, so it also
runs only when the package build runs.

For allowed no-op pull requests, the job succeeds without producing package
artifacts and prints the classifier reason.

## Security contract

The no-op optimization does not add privileged workflow behavior.

- Workflows do not use `pull_request_target`.
- Workflows do not add new secrets, personal access tokens, write permissions, or
  third-party classifier actions.
- Repository-owned workflows explicitly set `permissions: contents: read`, which
  is sufficient to check out the repository and classify changes.
- Changed paths are treated as untrusted input and are never executed.
- Unknown conditions fail closed by running Maven.
- External required security checks, including GitGuardian, are not edited,
  replaced, inferred, or weakened by repository-owned CI changes.
- Workflow names and job names are preserved so required status checks keep the
  same semantics.

## Configuration

There is no user-facing configuration knob for the skip classifier. The
allowlist is intentionally hard-coded in the four workflow files so future
changes are reviewed with the same scrutiny as CI behavior changes.

When extending the allowlist, keep these rules:

1. Add only paths that cannot affect compiled code, tests, package output,
   coverage output, QA behavior, dependency resolution, workflow behavior, or
   required security evidence.
2. Keep the default fail-closed: a path is validation-impacting unless explicitly
   proven safe.
3. Update all four repository-owned workflows together to prevent classifier
   drift.
4. Update this document with the new path category and the safety rationale.
5. Keep exact-SHA fetch retry behavior consistent across all four workflows.
6. Validate representative docs-only, source/build/test/workflow, mixed-change,
   and unknown-path file sets before merging.

## Examples

### Docs-only pull request

Changed files:

```text
docs/reference/ci-efficiency.md
README.md
```

Classifier result:

```text
maven-required=false
reason=docs/license-only pull request changes
```

Expected behavior: the four repository-owned workflows keep their required
statuses, skip their Maven-dependent work, and log the no-op reason. External
required security checks still run according to their own configuration.

### License-only pull request

Changed files:

```text
LICENSE
NOTICE.md
```

Classifier result:

```text
maven-required=false
reason=docs/license-only pull request changes
```

Expected behavior: same as the docs-only pull request.

### Mixed documentation and source pull request

Changed files:

```text
docs/reference/coverage-reporting.md
core/story-api-migration/src/test/java/org/lgna/project/migration/StoryMigrationTest.java
```

Classifier result:

```text
maven-required=true
reason=validation-impacting, mixed, or unknown path change
```

Expected behavior: all original Maven commands and Maven-dependent evidence
steps run. The documentation change does not suppress validation because the pull
request also changes tests.

### Workflow-only pull request

Changed files:

```text
.github/workflows/alice-coverage-ci.yml
```

Classifier result:

```text
maven-required=true
reason=validation-impacting, mixed, or unknown path change
```

Expected behavior: Maven validation runs. Workflow changes are never treated as
docs-only because they can affect required status behavior.

### Unknown path or diff failure

Changed files:

```text
../outside-repo
```

or the workflow cannot compute the pull request diff.

Classifier result for the path shown above:

```text
maven-required=true
reason=invalid absolute or traversal path; validation required
```

If the workflow cannot compute the pull request diff, the reason instead names
the failed fetch or diff operation. In both cases, Maven validation runs. The
classifier never skips validation when it cannot prove the change is
non-impacting.

## Local validation checklist

Before changing the classifier or workflow gates, confirm these contracts:

1. Docs-only and license-only file sets produce `maven-required=false`.
2. Source, resource, test, QA, script, build, workflow, package, and coverage
   configuration file sets produce `maven-required=true`.
3. Mixed docs-plus-code file sets produce `maven-required=true`.
4. Empty, malformed, absolute, parent-traversal, unavailable, or unknown file
   sets produce `maven-required=true`.
5. Exact-SHA fetches are retried consistently and still fail closed to Maven
   after retry exhaustion.
6. The Maven commands shown in this document preserve their validation surfaces
   for validation-impacting changes.
7. Coverage thresholds, coverage reports, coverage evidence artifacts, NetBeans
   package verification, test execution, Checkstyle validation, and external
   security checks remain intact when validation is required.

Do not add timeout wrappers as a CI-time optimization. Existing workflow timeout
settings are not part of the no-op skip mechanism and should be changed only for
a directly justified reliability reason.
