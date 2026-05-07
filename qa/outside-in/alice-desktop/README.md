# Alice desktop outside-in QA

This lane defines executable acceptance coverage for Alice desktop workflows without changing product modules. It keeps scenario intent, execution wrappers, and evidence requirements in one repo-owned QA area.

For user-facing instructions, see [Run Alice desktop outside-in QA](../../../docs/howto/alice-desktop-outside-in-qa.md). For the complete scenario schema and runner interface, see the [Alice desktop outside-in QA reference](../../../docs/reference/alice-desktop-outside-in-qa.md).

## What belongs here

| Area | Owns | Does not own |
| --- | --- | --- |
| `scenarios/` | User-like workflows, expected outcomes, evidence requirements, automation mode | Java implementation details or brittle internal UI assumptions |
| `gadugi/` | Gadugi-compatible CLI scenarios for agentic QA tools, including exported launcher evidence contract checks | The custom Alice scenario schema or full desktop/rendering claims |
| `schema/` | Scenario structure and allowed field values | Business logic |
| `runners/` | Thin wrappers around existing Maven/Alice commands | New build systems, hidden dependencies, or product behavior changes |
| `evidence/` | Local run artifacts produced by the runner | Source-controlled product assets |

Generated evidence is ignored by Git. Commit only scenario definitions, schema changes, runner changes, and the evidence `.gitignore`.

## Scenario model

Each scenario uses the same fields:

- `id`
- `title`
- `workflow`
- `automationMode`
- `preconditions`
- `userActions`
- `expectedOutcomes`
- `evidence.required`
- `fallback`

Allowed `automationMode` values are:

| Mode | Meaning |
| --- | --- |
| `xvfb-real-alice` | Attempts to run the real Alice desktop under Xvfb and captures logs/screenshots. |
| `manual-evidence-required` | Produces an executable checklist with required evidence, but does not automate GUI interaction or mark the scenario complete. |
| `gated-command-smoke` | Produces status/checklist evidence by default; executes the configured CLI smoke only when `ALICE_QA_RUN_GATED_SMOKES=1`, including focused launch-adjacent menu/action plumbing tests. |

Do not use Playwright here unless Alice later exposes a browser/web UI.

Scenario YAML intentionally uses a strict subset: simple mappings, nested mappings, scalar values, and scalar lists. Do not use anchors, aliases, tags, multiline scalars, flow-style collections, or tabs for indentation. The JSON Schema is the published contract; the dependency-free validator must stay in parity with it.

## Commands

Run all commands from the repository root.

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --list
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json
qa/outside-in/alice-desktop/runners/run-scenario.sh list
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
qa/outside-in/alice-desktop/runners/run-scenario.sh run qa/outside-in/alice-desktop/scenarios/launch.yaml
```

`run-scenario.sh run` accepts either a scenario ID or a direct `.yaml` file inside the active scenario catalog. Use `--evidence-dir <dir>` to write evidence outside the repository, `--timeout-seconds <seconds>` to override argv-backed launch timeout, and `--prepare-only` to intentionally prepare gated smoke evidence without executing the gated command.

The Gadugi exported launcher evidence scenario is a separate CLI scenario under
`gadugi/`, not a custom Alice scenario under `scenarios/`. Validate and run it
with `gadugi-test` installed on `PATH`:

```bash
NODE_OPTIONS=--max-old-space-size=32768 gadugi-test validate \
  -f qa/outside-in/alice-desktop/gadugi/exported-launcher-evidence.yaml

NODE_OPTIONS=--max-old-space-size=32768 gadugi-test run \
  -d qa/outside-in/alice-desktop/gadugi \
  -s exported-launcher-evidence \
  --timeout 300000
```

That Gadugi scenario delegates to the exported-project smoke runner in
prepare-only mode by default and writes evidence under
`qa/outside-in/alice-desktop/evidence/gadugi-exported-launcher`.
`ALICE_QA_RUN_GATED_SMOKES=1` only applies when the underlying Alice runner is
invoked without `--prepare-only`. The Gadugi lane validates launcher evidence
wiring and JavaFX handoff/no-go checks only; it does not prove visible
rendering, save behavior, grading, creative assessment, or full lesson
completion.

For branch-installable outside-in checks, run the thin `amplihack` wrapper from a checkout of the branch:

```bash
uvx --from git+https://github.com/rysweet/alice3-modernization.git@feat/alice-qa-outside-in amplihack alice-qa list
uvx --from git+https://github.com/rysweet/alice3-modernization.git@feat/alice-qa-outside-in amplihack alice-qa run alice-desktop-save-load --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The wrapper delegates to the same repo-owned runners and intentionally requires an Alice checkout as the current working tree.

The launch scenario first verifies the Alice exec root-directory property and
prepares the distribution root if it is missing:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests -pl core/resources process-resources
```

It then uses the Alice desktop Maven path with an explicit compile step before
`exec:java`, so `org.alice.stageide.EntryPoint` is present in
`alice-ide/target/classes` and the JVM sees
`org.alice.ide.rootDirectory=../core/resources/target/distribution` from
`alice-ide/pom.xml`:

```bash
cd alice-ide
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -DskipTests compile exec:java -Dalice-ide
```

Scenario automation stores executable steps as argv lists, not shell command strings. The validator and runner allow only the checked-in Alice QA argv set, including custom catalogs selected with `ALICE_QA_SCENARIO_DIR`.

The runner records evidence under `qa/outside-in/alice-desktop/evidence/<scenario-id>/<timestamp>/`. Successful Xvfb launch evidence includes `root-directory-prep.json`, an environment summary, Xvfb log, Alice launch log, screenshot (`screenshot.png` or `screenshot.xwd`), `x-window-inventory.json`, `application-root-error.json`, `license-dialog.json`, `license-acceptance.json`, `controlled-display-pixel-observation.json`, optional screenshot pixel stats, and status file. The root-directory prep artifact records whether `core/resources/target/distribution` was already present or prepared with Maven phase `process-resources`, and blocked cases name the exact missing property, distribution path, or Maven failure. The window inventory records visible X window title, class, process, and geometry after the readiness wait. When a Java window titled `Application Root Error` appears, `application-root-error.json` maps that exact blocker to the observed JVM `org.alice.ide.rootDirectory` condition, expected dialog text, and next invocation change; it does not infer text without that exact window. When a first-run License Agreement appears, `license-dialog.json` records the exact title, expected JEulaPane header/controls, preference class/package/key, and test-only bypass runner. `license-acceptance.json` records either the explicit opt-in blocker or the isolated `java.util.prefs.userRoot` state files under `.java/.userPrefs/` prepared when `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` is set. The controlled display artifact records the exact blocker when root-directory preparation, Xvfb, display allocation, screenshot capture, process lifetime, Alice-window detection, application-root detection, first-run license detection, or screenshot pixel analysis prevents pixel observation; it does not assert Alice rendering correctness. Early Xvfb fallback directories may contain only the diagnostics available before launch plus a manual fallback checklist. For manual scenarios, the runner creates a status file and structured checklist so the workflow is repeatable and reviewable; the scenario is complete only after a human performs the workflow and adds the required evidence artifacts plus `review-notes.txt`. For gated command smokes, an unset gate records `outcome=gated-not-run` and exits non-zero; pass `--prepare-only` for intentional preflight/checklist preparation, or set `ALICE_QA_RUN_GATED_SMOKES=1` only in a worktree prepared for the configured Maven or display-backed argv.

## Configuration

| Variable | Purpose |
| --- | --- |
| `ALICE_QA_SCENARIO_DIR` | Override the active scenario catalog. |
| `ALICE_QA_DISPLAY` | Reuse a specific X display for Xvfb runs. |
| `ALICE_QA_SCREEN` | Set Xvfb screen geometry. Defaults to `1280x900x24`. |
| `ALICE_QA_READY_WAIT_SECONDS` | Override launch readiness wait before screenshot capture. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS` | Set to `1` only for controlled QA launches that need to bypass first-run License Agreement dialogs with an isolated Java Preferences user root. |
| `ALICE_QA_RUN_GATED_SMOKES` | Execute gated command smoke scenarios when set to `1`; otherwise they write `gated-not-run` evidence and exit non-zero unless `--prepare-only` is requested. |
| `NODE_OPTIONS` | Optional for surrounding Node-based orchestrators. Use `--max-old-space-size=32768` when needed; this lane itself does not require Node. |

## Scenario authoring checklist

Before adding or changing a scenario:

1. Keep actions and outcomes observable from the user-visible Alice desktop.
2. Use one of the supported workflows: `launch`, `instructor-student-setup`, `scene-creation`, `run-debug`, `save-load`, `open-load-save`, `export`, `exported-project-smoke`, `netbeans-package-smoke`, `package-install-smoke`, `project-io-smoke`, `failure-path-smoke`, `future-ui-smoke`, `menu-action-smoke`, or `wizard-palette-completion-smoke`.
3. Use `xvfb-real-alice` only when the runner can execute the real Alice command and collect logs/screenshots.
4. Use `manual-evidence-required` when human Swing interaction is required.
5. Name concrete required artifacts in `evidence.required`; manual workflows also require `review-notes.txt` for acceptance.
6. Keep YAML to the supported simple mapping/list subset.
7. Run `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`.

## Baseline preconditions

- Java 21 is available.
- Maven 3.9.9 or later is available.
- The Tweedle grammar submodule is initialized:

```bash
git submodule update --init tweedle-lang
```

If Maven reports missing generated Tweedle parser classes, first check:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```
