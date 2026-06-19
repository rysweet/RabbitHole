---
title: CI GUI, Eatme, and Xvfb Validation Reference
description: Reference for RabbitHole CI dependency resolution, Eatme CLI tooling, and outside-in Xvfb evidence semantics.
review_schedule: quarterly
owner: maintainers
doc_type: reference
---

# CI GUI, Eatme, and Xvfb Validation Reference

This reference describes the intended finished-state contracts for
GUI-capable CI lanes, Eatme integration tooling, and Alice desktop outside-in
evidence tracking.

## Scope

These contracts are CI, tooling, and QA contracts. They do not change Alice 3
runtime behavior or classroom-facing project semantics.

| Area | Contract |
| --- | --- |
| JogAmp dependency resolution | GUI and NetBeans lanes resolve JOGL and GlueGen without depending on a single transient `jogamp.org` availability point. |
| Eatme tooling | Repository-owned wrappers launch the Java Eatme entry points with the packaged Alice classpath and write bounded JSON evidence. |
| Xvfb evidence tracking | Outside-in scenarios distinguish real execution, gated skips, prepare-only skips, blocked evidence, and manual evidence requirements. |

## Follow-up evidence matrix

| Follow-up area | Evidence to collect | What may be claimed | What must not be claimed |
| --- | --- | --- | --- |
| JogAmp CI mitigation | Maven logs from `headed-ubuntu-xvfb` and `package-netbeans` showing JOGL/GlueGen resolution through `.github/maven/jogamp-ci-settings.xml`, plus no TLS/checksum bypasses. | CI dependency resolution for GL-capable lanes mirrors `jogamp.org` to an approved HTTPS repository instead of treating `jogamp.org` as the only availability point. | Do not claim visible rendering correctness, classroom behavior changes, or full desktop validation from dependency-resolution evidence alone. |
| Eatme wrappers/API | Wrapper package precondition, focused `Eatme*Test` results, JSON stdout, and bounded evidence artifacts for the wrapper under test. | The named Eatme seam produced its scoped proof artifact for the selected project/method/object. | Do not claim full first-lesson completion, grading, creative assessment, broad UI automation, or rendering correctness. |
| Xvfb scenario evidence semantics | Scenario validation, runner contract tests, `status.txt`, `command.log` when executed, and blocker/checklist artifacts when not executed. | The runner correctly distinguishes executed success, failed execution, gated skips, blocked evidence, and manual evidence requirements. | Do not report `gated-not-run`, `blocked`, or `manual-evidence-required` as passing GUI execution. |

## Maven dependency resolution target

RabbitHole uses JOGL and GlueGen for GL-capable desktop paths:

| Dependency | Maven coordinate |
| --- | --- |
| JOGL | `org.jogamp.jogl:jogl-all:${jogl.version}` |
| GlueGen runtime | `org.jogamp.gluegen:gluegen-rt:${gluegen.version}` |

The dependency-resolution mitigation must follow these rules:

1. Public artifacts resolve through Maven Central whenever available.
2. The explicit JogAmp repository ID may remain in the project POM for local
   compatibility, but CI mirrors that ID through
   `.github/maven/jogamp-ci-settings.xml`.
3. Any Alice-owned repository, mirror, or cache used by CI is scoped to
   Alice-owned, Alice-wrapped, or explicitly approved third-party artifacts.
4. CI never disables TLS, checksum validation, or Maven artifact verification.
5. Headless validation keeps the same Maven dependency graph; the mitigation
   only improves repository availability for lanes that need GL-capable classes.

The root `pom.xml` remains the source of truth for JOGL and GlueGen versions.
Workflow files use the CI-scoped settings file for GL-capable lanes, but they do
not change source dependencies or hide runtime artifacts behind CI-only
profiles.

## GUI-capable CI jobs

| Workflow | Job | Purpose |
| --- | --- | --- |
| `.github/workflows/alice-test-ci.yml` | `headed-ubuntu-xvfb` | Runs Getting Started GUI validation under Xvfb. |
| `.github/workflows/alice-netbeans-package-ci.yml` | `package-netbeans` | Builds and checks the NetBeans package with default open assets. |

Both jobs initialize the Tweedle grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
```

The Xvfb job uses the shared setup action:

```yaml
- name: Set up Xvfb
  id: setup-xvfb
  uses: ./.github/actions/setup-xvfb
```

The action installs `xvfb` and exposes an absolute `xvfb-run` path through the
`xvfb-run` output.

## Xvfb GUI harness

Use `scripts/validate-gui-with-xvfb.sh` for bounded GUI validation:

```bash
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds 1800 \
  --expect success \
  -- \
  env MAVEN_SETTINGS_PATH=.github/maven/jogamp-ci-settings.xml \
    scripts/validate-getting-started.sh --gui
```

### Options

| Option | Default | Description |
| --- | --- | --- |
| `--timeout-seconds N` | `RABBITHOLE_LAUNCH_TIMEOUT_SECONDS` or `60` | Maximum wrapped command runtime. |
| `--expect success` | `success` | Requires the wrapped command to exit `0`. |
| `--expect failure` | `success` | Requires the wrapped command to exit non-zero. |
| `--xvfb-run PATH` | `xvfb-run` on `PATH` | Uses a specific executable, normally the action output in CI. |

The harness always runs:

```text
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac"
```

See [JavaFX Xvfb Launcher Reference](./javafx-xvfb-launcher.md) for the shared
launcher prefix used by Java tests and packaged-launcher checks.

## Outside-in runner configuration

The Alice desktop outside-in runner is:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh
```

### Commands

| Command | Description |
| --- | --- |
| `run-scenario.sh list` | Lists supported scenarios. |
| `run-scenario.sh validate` | Validates scenario YAML contracts. |
| `run-scenario.sh run <scenario>` | Runs or prepares one scenario. |

### Environment variables

| Variable | Description |
| --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Overrides the directory containing scenario YAML files. |
| `ALICE_QA_DISPLAY` | Reuses a specific X display, such as `:99`. |
| `ALICE_QA_SCREEN` | Xvfb screen geometry. Default: `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Overrides GUI readiness wait before screenshot capture. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` | Prepares isolated first-run license acceptance state for controlled QA launches. |
| `ALICE_QA_RUN_GATED_SMOKES=1` | Executes `gated-command-smoke` scenarios. |
| `ALICE_QA_DISABLE_XVFB=1` | Forces the Xvfb-unavailable fallback for contract tests. |
| `NODE_OPTIONS` | Defaults to `--max-old-space-size=32768` inside gated command execution when unset. |

## Scenario outcome semantics

`status.txt` is the canonical per-run outcome summary.

| Mode or condition | Exit code | `status.txt` outcome | Meaning |
| --- | ---: | --- | --- |
| `manual-evidence-required` | `0` with `--prepare-only` | `manual-evidence-required` | Checklist generated; a human must attach and review evidence. |
| Gated smoke without `ALICE_QA_RUN_GATED_SMOKES=1` | `3` | `gated-not-run` with `skipMode=missing-gate`, `executionStatus=not-run`, and `executionClaim=no-gui-execution` | Scenario intentionally did not execute because the gate was unset. |
| Gated smoke with `--prepare-only` | `0` | `gated-not-run` with `skipMode=prepare-only`, `executionStatus=not-run`, and `executionClaim=no-gui-execution` | Scenario intentionally prepared checklist evidence without execution. |
| Gated smoke with gate enabled and command exit `0` | `0` | `passed` with `executionStatus=executed` and `executionClaim=gated-command-executed` | The allowed command ran and `command.log` captured output. |
| Gated smoke with gate enabled and command failure | command exit | `failed` with `executionStatus=executed` and `executionClaim=gated-command-executed` | The allowed command ran and failed; inspect `command.log`. |
| `xvfb-real-alice` evidence observed | `0` | `passed` | Alice launched under Xvfb and required bounded evidence was observed. |
| `xvfb-real-alice` evidence blocked | non-zero | `blocked` or failed precondition | Required bounded evidence was not observed; inspect the named blocker artifact. |

Do not treat `gated-not-run` as executed validation. It is evidence that the
runner recorded an intentional skip or missing gate.

## Eatme CLI launcher contract

Eatme wrappers live in `tools/` and all use the same launcher shape:

```bash
java \
  -ea \
  -Dorg.alice.ide.rootDirectory=./core/resources/target/distribution \
  -Dedu.cmu.cs.dennisc.java.util.logging.Logger.Level=WARNING \
  -cp "alice-ide/target/*:alice-ide/target/lib/*" \
  <org.alice.tools.EntryPoint> \
  "$@"
```

Before running any wrapper, package Alice so `alice-ide/target/lib/` exists:

```bash
mvn -DincludeSims=false -Dinstall4j.skip clean package -DskipTests
```

If the package step has not populated `alice-ide/target/lib/`, wrappers exit
with code `2` and explain which package step is missing.

### Wrapper API

| Wrapper | Java entry point | Required arguments | Main artifacts |
| --- | --- | --- | --- |
| `tools/eatme-place-object` | `org.alice.tools.EatmePlaceObject` | `--project`, `--object`, `--evidence-dir`, `--json` | `placed-project.a3p`, `placement.json`, `scene.diff.json` |
| `tools/eatme-edit-procedure` | `org.alice.tools.EatmeEditProcedure` | `--project`, `--procedure-selector`, `--edit-spec`, `--evidence-dir`, `--json` | `edited-project.a3p`, `first-lesson-code-editor-action-proof.json` |
| `tools/eatme-run-world` | `org.alice.tools.EatmeRunWorld` | `--project`, `--run-selector`, `--evidence-dir`, `--json` | `world-run.json`, `runtime.log` |
| `tools/eatme-save-project` | `org.alice.tools.EatmeSaveProject` | `--project`, `--save-selector`, `--evidence-dir`, `--json` | `saved-project.a3p`, `project-save.json` |
| `tools/eatme-reopen-project` | `org.alice.tools.EatmeReopenProject` | `--saved-project`, `--reopen-selector`, `--evidence-dir`, `--json` | `reopened.a3p`, `reopen-evidence.json`, `reopened-state.json` |

Selector arguments use `scene.<methodName>` and the method name must match
`[A-Za-z_][A-Za-z0-9_]*`. Missing methods are validation errors, not successful
proofs.

`--object` currently supports repository-defined Alice gallery identifiers such
as:

```text
alice-gallery://animals/bunny
```

### Eatme exit codes

| Code | Meaning |
| ---: | --- |
| `0` | Success; JSON was written to stdout and evidence artifacts were written. |
| `2` | Argument, selector, project, package, or validation error. |
| `3` | Unexpected runtime failure from the Java entry point. |

## Eatme evidence boundaries

Eatme evidence is intentionally narrow.

| Evidence | Proves | Does not prove |
| --- | --- | --- |
| Object placement | A supported object identifier was added to the project and persisted to `placed-project.a3p`. | Visible rendering correctness, broad UI automation, grading, or lesson completion. |
| Procedure edit | The selected scene method was targeted and the requested edit spec was applied to that method. | Full first-lesson completion, creative assessment, or unrelated UI workflows. |
| World run | The selected scene method was invoked by the Eatme runtime proof path and runtime logs were captured. | Full desktop playback, rendering correctness, or physical user interaction. |
| Save | The selected scene method survived a headless project persistence write. | Save As coverage, native dialog coverage, or all save variants. |
| Reopen | The saved project can be read again and the selected scene method survives the round trip. | Full project interaction, rendering correctness, or UI reopen workflows. |

## Validation commands

Run targeted checks when changing these contracts:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh
bash qa/outside-in/alice-desktop/tests/test-run-execution-gap-contract.sh
bash qa/outside-in/alice-desktop/tests/test-visible-rendering-evidence-contract.sh
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am test \
  -Dtest='Eatme*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Run the focused Eatme test command under Xvfb when changes affect
display-gated edit-procedure behavior; otherwise those tests may be skipped by
headless JUnit assumptions.

Run GUI-capable validation when changing Xvfb, GL dependency resolution, or
NetBeans package behavior:

```bash
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds 1800 \
  --expect success \
  -- \
  env MAVEN_SETTINGS_PATH=.github/maven/jogamp-ci-settings.xml \
    scripts/validate-getting-started.sh --gui

mvn --settings .github/maven/jogamp-ci-settings.xml \
  -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip \
  -pl netbeans -am clean package -DskipTests
```

## Security and integrity rules

- Use HTTPS repositories only.
- Do not disable Maven checksum or certificate validation.
- Do not add credentials or tokens to workflow files, Maven settings, wrapper
  scripts, scenario files, or evidence examples.
- Keep wrapper arguments quoted and pass commands as argument arrays.
- Reject absolute paths or `..` traversal where evidence artifact paths are
  expected to be repository-local or evidence-directory-local.
- Do not promote skipped, blocked, or manual checklist evidence into successful
  execution claims.
