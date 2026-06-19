---
title: Verify CI GUI, Eatme, and Xvfb Readiness
description: How to verify GUI-capable CI dependency resolution, Eatme wrappers, and outside-in Xvfb evidence tracking.
last_updated: 2026-06-10
review_schedule: quarterly
owner: maintainers
doc_type: howto
related:
  - ../reference/ci-gui-eatme-xvfb-validation.md
---

# Verify CI GUI, Eatme, and Xvfb Readiness

Use this guide when changing CI dependency resolution, Eatme tooling, Xvfb setup,
or Alice desktop outside-in scenario evidence semantics.

## Prerequisites

Run commands from the repository root.

```bash
git submodule update --init tweedle-lang
export NODE_OPTIONS=--max-old-space-size=32768
```

For GUI validation on Ubuntu, install Xvfb if it is not already installed:

```bash
sudo apt-get update
sudo apt-get install -y --no-install-recommends xvfb
```

## Verify GUI-capable CI dependency resolution

Build the NetBeans package without Sims assets:

```bash
mvn --settings .github/maven/jogamp-ci-settings.xml \
  -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip \
  -pl netbeans -am clean package -DskipTests
```

Run the GUI Getting Started validation under the shared Xvfb harness:

```bash
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds 1800 \
  --expect success \
  -- \
  env MAVEN_SETTINGS_PATH=.github/maven/jogamp-ci-settings.xml \
    scripts/validate-getting-started.sh --gui
```

These commands exercise the same dependency surfaces used by the
`headed-ubuntu-xvfb` and `package-netbeans` GitHub Actions jobs. Also keep the
Maven resolution logs as evidence that JOGL and GlueGen resolved through
`.github/maven/jogamp-ci-settings.xml`, which mirrors the `jogamp.org`
repository ID to the approved SciJava HTTPS repository in CI. Passing headless
validation alone is not enough to verify GL-capable dependency resolution.

## Verify Eatme wrappers

Package Alice before running wrappers:

```bash
mvn -DincludeSims=false -Dinstall4j.skip clean package -DskipTests
```

Create an object-placement proof from a checked-in starter project:

```bash
rm -rf qa/outside-in/alice-desktop/evidence/eatme-local
mkdir -p qa/outside-in/alice-desktop/evidence/eatme-local/place

tools/eatme-place-object \
  --project core/resources/src/application/resources/starter-projects/magicMinimum.a3p \
  --object alice-gallery://animals/bunny \
  --evidence-dir qa/outside-in/alice-desktop/evidence/eatme-local/place \
  --json
```

Inspect the result JSON and evidence files:

```bash
python3 -m json.tool \
  qa/outside-in/alice-desktop/evidence/eatme-local/place/placement.json

test -s qa/outside-in/alice-desktop/evidence/eatme-local/place/placed-project.a3p
test -s qa/outside-in/alice-desktop/evidence/eatme-local/place/scene.diff.json
```

The checked-in starter project is sufficient for object placement. The other
Eatme wrappers require a fixture project that already contains the named
zero-argument scene method; the repository does not currently ship a stable
selector fixture for manual CLI use. For selector wrappers, the maintained
runnable verification is the focused Java test suite:

```bash
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am test \
  -Dtest='Eatme*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

If edit-procedure behavior is in scope, run that Maven command under Xvfb so
display-gated tests execute instead of being skipped by headless assumptions:

```bash
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds 1800 \
  --expect success \
  -- \
  mvn -DincludeSims=false -Dinstall4j.skip \
    -pl core/ide -am test \
    -Dtest='Eatme*Test' \
    -Dsurefire.failIfNoSpecifiedTests=false
```

If you have a local Eatme fixture with `scene.eatmeFirstLesson`, verify the edit
handoff with an explicit fixture path:

```bash
LESSON_FIXTURE=/absolute/path/to/project-with-scene-eatmeFirstLesson.a3p
test -f "$LESSON_FIXTURE"

mkdir -p qa/outside-in/alice-desktop/evidence/eatme-local/edit

tools/eatme-edit-procedure \
  --project "$LESSON_FIXTURE" \
  --procedure-selector scene.eatmeFirstLesson \
  --edit-spec "append-comment:eatme edit proof" \
  --evidence-dir qa/outside-in/alice-desktop/evidence/eatme-local/edit \
  --json
```

If you have a local Eatme fixture with `scene.eatmeFirstLessonStep`, verify
save, reopen, and run evidence with the same explicit fixture guard:

```bash
LESSON_STEP_FIXTURE=/absolute/path/to/project-with-scene-eatmeFirstLessonStep.a3p
test -f "$LESSON_STEP_FIXTURE"

mkdir -p qa/outside-in/alice-desktop/evidence/eatme-local/save

tools/eatme-save-project \
  --project "$LESSON_STEP_FIXTURE" \
  --save-selector scene.eatmeFirstLessonStep \
  --evidence-dir qa/outside-in/alice-desktop/evidence/eatme-local/save \
  --json

mkdir -p qa/outside-in/alice-desktop/evidence/eatme-local/reopen

tools/eatme-reopen-project \
  --saved-project qa/outside-in/alice-desktop/evidence/eatme-local/save/saved-project.a3p \
  --reopen-selector scene.eatmeFirstLessonStep \
  --evidence-dir qa/outside-in/alice-desktop/evidence/eatme-local/reopen \
  --json

mkdir -p qa/outside-in/alice-desktop/evidence/eatme-local/run

tools/eatme-run-world \
  --project qa/outside-in/alice-desktop/evidence/eatme-local/reopen/reopened.a3p \
  --run-selector scene.eatmeFirstLessonStep \
  --evidence-dir qa/outside-in/alice-desktop/evidence/eatme-local/run \
  --json
```

## Verify Xvfb and outside-in evidence tracking

Validate every scenario definition:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Prepare a gated scenario without executing it:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh \
  run alice-desktop-netbeans-package-smoke \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/local-netbeans-package
```

The generated `status.txt` must contain:

```text
outcome=gated-not-run
gate=ALICE_QA_RUN_GATED_SMOKES
skipMode=prepare-only
executionStatus=not-run
executionClaim=no-gui-execution
```

Run a gated smoke for real:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh \
  run alice-desktop-project-io-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/local-project-io
```

The generated `status.txt` must contain `outcome=passed`,
`executionStatus=executed`, and `executionClaim=gated-command-executed`.
`command.log` must exist in the same run directory.

Run the runner contract tests after changing scenario mode semantics:

```bash
bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
bash qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh
bash qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
```

## Interpret evidence

| Evidence state | How to report it |
| --- | --- |
| `outcome=passed` with `command.log` | The gated command executed and exited successfully. |
| `outcome=gated-not-run`, `skipMode=missing-gate`, `executionStatus=not-run` | The scenario did not execute because `ALICE_QA_RUN_GATED_SMOKES=1` was absent. |
| `outcome=gated-not-run`, `skipMode=prepare-only`, `executionStatus=not-run` | The scenario intentionally generated checklist evidence only. |
| `outcome=manual-evidence-required` | A human must attach and review the required artifacts before claiming completion. |
| `outcome=blocked` | Required bounded evidence was not observed; use the named blocker artifact as the result. |

Do not describe a skipped, blocked, or manual checklist run as a passing GUI
execution.

## Follow-up closure evidence

| Follow-up area | Minimum evidence before closing | Safe closure wording |
| --- | --- | --- |
| JogAmp CI mitigation | GUI and NetBeans Maven logs showing JOGL/GlueGen resolution through an approved repository, mirror, or cache path that is not solely `jogamp.org`, with no TLS or checksum bypass. | "GL-capable CI dependency resolution no longer depends on `jogamp.org` as the only availability point." |
| Eatme wrappers/API | Package precondition plus `Eatme*Test` results and wrapper JSON/artifacts for the changed seam. | "The Eatme wrapper/API seam produces bounded evidence for the selected project operation." |
| Xvfb scenario evidence semantics | Scenario validation, runner contract tests, and representative `status.txt` evidence for executed and non-executed outcomes. | "Outside-in runner evidence distinguishes execution, skips, blockers, and manual evidence requirements." |

Do not use dependency-resolution, Eatme, or runner-status evidence to claim full
desktop rendering correctness or Alice runtime behavior changes.

## Troubleshooting

### `alice-ide/target/lib is missing`

Run the package step before invoking an Eatme wrapper:

```bash
mvn -DincludeSims=false -Dinstall4j.skip clean package -DskipTests
```

### `xvfb-run` is missing

Install Xvfb locally or use the GitHub Action setup step in CI:

```bash
sudo apt-get install -y --no-install-recommends xvfb
```

### A gated scenario exits `3`

The gate is unset. Either execute the scenario with:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-project-io-smoke
```

or intentionally prepare evidence without execution:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-project-io-smoke --prepare-only
```

### A selector is rejected

Eatme selectors must name an existing zero-argument scene method:

```text
scene.eatmeFirstLessonStep
```

Selectors outside `scene.<methodName>` or methods missing from the project are
validation failures.
