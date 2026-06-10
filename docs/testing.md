# Testing

RabbitHole uses a mix of unit tests, characterization tests, and focused
desktop proof tests.

The planned reflection-sweep migration makes broad sweeps non-initializing
smoke-load checks only. Constructor, method, lifecycle, state, data, factory,
and render behaviors belong in explicit JUnit contracts. See
[Reflection Sweep Contracts](concepts/reflection-sweep-contracts.md),
[Replace Reflection Sweeps with Explicit Contracts](howto/replace-reflection-sweeps.md),
and [Reflection Smoke Support](reference/reflection-smoke-support.md).

Deterministic-wait helpers replace fixed sleeps in tests that observe
asynchronous work, UI state, generated launchers, process output, or worker
threads. See
[Replace test sleeps with deterministic waits](./howto/replace-test-sleeps.md)
for usage examples and
[Deterministic test wait reference](./reference/deterministic-test-waits.md)
for helper contracts and the sleep inventory disposition.

## Run the main test lanes

Validate the documented Getting Started path:

```bash
./scripts/validate-getting-started.sh
python3 alice_qa.py getting-started validate --headless
```

Run everything:

```bash
mvn test
```

Run the no-Sims, headless-friendly install lane:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean install
```

The headed Ubuntu GUI validation lane runs under Xvfb:

```bash
xvfb_run="${{ steps.setup-xvfb.outputs.xvfb-run }}"
RABBITHOLE_LAUNCH_TIMEOUT_SECONDS=60 \
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds "${RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS:-7200}" \
  --expect success \
  --xvfb-run "${xvfb_run}" \
  -- \
  scripts/validate-getting-started.sh --gui
```

Run the golden Alice project corpus validator:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=GoldenProjectCorpusValidatorTest \
  test
```

Run the RabbitHole baseline parity harness:

```bash
git submodule update --init tweedle-lang
mvn -pl netbeans -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=RabbitHoleBaselineParityTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Run the generated-source validation ownership lanes after changes that affect
compiler/story source shape or NetBeans project generation.

```bash
git submodule update --init tweedle-lang
mvn -pl core/ast -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=SourceCodeGeneratorTest,JavaCodeGeneratorExtendedTest,JavaCodeGeneratorDelegationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=StoryApiGeneratedSourceTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
mvn -pl core/ide -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=SilverThreadStudentProgramCodegenTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
mvn -pl netbeans -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=ProjectCodeGeneratorGeneratedSourceTest,ProjectCodeGeneratorStoryApiGeneratedSourceTest,ProjectCodeGeneratorTest,ProjectCodeGeneratorStandaloneProjectTest,RabbitHoleBaselineParityTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Run the dual-baseline replay harness in CI-safe fallback mode:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=DualBaselineReplayHarnessTest \
  test
```

Run the strict text migration JSON drift check:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=TextMigrationJsonGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

This lane regenerates text migration JSON from the legacy registries in memory,
compares it with
`core/story-api-migration/src/main/resources/migrations/text-migrations.json`
using exact UTF-8 string comparison, and fails non-zero on drift. This strict
command is read-only. Do not add the write property to this command; use the
regeneration command in
[Verify Text Migration Registry Parity](./howto/verify-text-migration-parity.md).

Run the full text migration registry parity lane:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=TextMigrationRegistryTest,TextMigrationJsonLoaderTest,TextMigrationJsonGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Both text migration lanes require the Tweedle grammar submodule. If Maven reports
missing generated Tweedle parser classes, run:

```bash
git submodule update --init tweedle-lang
```

Then confirm `tweedle-lang/Grammar` exists.

Run the same harness against a local preserved baseline checkout:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=DualBaselineReplayHarnessTest \
  -Drabbithole.baseline.checkout=/absolute/path/to/preserved-alice-baseline \
  test
```

Run Checkstyle separately:

```bash
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml
```

Run coverage:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmaven.test.failure.ignore=true -Dmdep.skip=true -Pcoverage verify
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --evidence-manifest coverage-evidence-manifest.json \
  --target-aggregate-line-percent 70.0 \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent core/scenegraph=10.0 \
  --min-module-line-percent netbeans=25.0
```

## Getting Started validation lanes

`scripts/validate-getting-started.sh` is the executable contract for the
out-of-the-box setup instructions in [Getting started](getting-started.md). It
validates the current checkout instead of creating a nested clone: GitHub
Actions supplies the fresh checkout, and local users can run the script
immediately after cloning.

`tests/test_getting_started_validation_contract.py` protects the documented
command surface from drift by checking the validator flags, submodule failure
guidance, no-Sims launch command, and GUI skip/block behavior described here.
Those contract tests also assert the shared Xvfb action, reusable Xvfb harness,
separate headed job, bounded startup timeout, and preserved headless lane.
`python3 alice_qa.py getting-started validate` is the wrapper entry point for
running the same validator from the checkout under review.

| Lane | Command | Intended environment | Success condition |
| --- | --- | --- | --- |
| Headless | `./scripts/validate-getting-started.sh` or `./scripts/validate-getting-started.sh --headless` | CI and local shells without a display | Git checkout and `tweedle-lang/Grammar` are present, the no-Sims Maven install command passes with `java.awt.headless=true`, and the no-Sims launch probe reaches the expected GUI-required message. |
| Headed Ubuntu Xvfb | `RABBITHOLE_LAUNCH_TIMEOUT_SECONDS=60 scripts/validate-gui-with-xvfb.sh --timeout-seconds "${RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS:-7200}" --expect success --xvfb-run "${xvfb_run}" -- scripts/validate-getting-started.sh --gui`, where `xvfb_run` is the shared action output | Ubuntu CI runner without a physical display | The no-Sims build/install passes under Xvfb with `java.awt.headless=false` and tests skipped, and the Alice desktop launch starts far enough under Xvfb to prove the documented display-dependent GUI launch path without hanging. |
| Local GUI | `./scripts/validate-getting-started.sh --gui` | Local desktop with real Java AWT display support | The no-Sims Alice desktop launch starts far enough to prove the documented GUI launch path is usable on that platform. |
| All | `./scripts/validate-getting-started.sh --all` | Local validation before sharing setup changes | Headless validation passes; GUI validation runs when supported and reports a clear skip or blocker when unsupported without failing the command. |

The headless lane runs this Maven command:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean install
```

The launch probe uses this documented no-Sims launch command:

```bash
cd alice-ide
mvn -DincludeSims=false exec:java -Dalice-ide
```

The headless validator adds `-Djava.awt.headless=true` to that launch probe so
the command proves the GUI boundary without opening the IDE on local desktops.

The headed Ubuntu Xvfb lane is intentionally separate. It uses the shared Xvfb
setup action to install Xvfb from Ubuntu apt repositories and expose an absolute
`xvfb-run` path to workflow steps. Running `--headless` under Xvfb is not a GUI
validation because Java still runs with `java.awt.headless=true`; the headed
lane must use `-Djava.awt.headless=false` and `--gui` so display-dependent
behavior is actually exercised. The reusable Xvfb harness owns the standard
`xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac"` invocation. The
`RABBITHOLE_LAUNCH_TIMEOUT_SECONDS` setting bounds the Alice startup probe, and
`RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS` is the larger whole-command
deadline for the harness.

### Skip, fail, and block behavior

| Situation | `--headless` | `--gui` | `--all` |
| --- | --- | --- | --- |
| Missing Git checkout metadata | Fail | Fail | Fail |
| Missing `tweedle-lang` or `tweedle-lang/Grammar` | Fail with `git submodule update --init tweedle-lang` guidance | Fail with the same guidance | Fail with the same guidance |
| No desktop display | Pass only if the launch probe reports `Alice desktop launch requires a graphical environment.` | Exit non-zero because GUI was requested explicitly | Skip the GUI lane and exit successfully after headless validation passes |
| macOS Apple Silicon desktop GUI launch | Headless lane remains valid | Exit non-zero as a known platform blocker | Report blocked and exit successfully after headless validation passes |
| Unknown validator flag | Fail | Fail | Fail |

CI integration keeps the headless lane for CLI/docs-safe launch behavior with
`java.awt.headless=true` and adds a separate headed Ubuntu Xvfb lane for
GUI/display-dependent behavior without a physical display. Local users should
run `--gui` only when their desktop session supports Java GUI launch.

## Golden Alice project corpus validator

`GoldenProjectCorpusValidatorTest` is the executable archive contract for
representative Alice project and player-export shapes. It lives in
`core/story-api-migration/src/test/java/org/lgna/project/io/` because it
characterizes the same save, reopen, export, manifest, and ZIP-entry behavior
implemented by `IoUtilities`, `ProjectIo`, `XmlProjectIo`, and `JsonProjectIo`.

The corpus is text-only. The checked-in manifest is:

```text
core/story-api-migration/src/test/resources/golden-project-corpus/corpus.properties
```

The test reads that manifest, generates deterministic temporary `.a3p` and
`.a3w` archives, validates each archive with `ZipFile`, and deletes the generated
payloads with the test temporary directory. Do not check in generated `.a3p`,
`.a3w`, image, audio, or other binary fixture payloads for this corpus.

### What it proves

Each manifest case describes one archive shape. The validator applies the
expectations that match the case type:

| Case type | Extension | Validation contract |
| --- | --- | --- |
| Editable project | `.a3p` | Generate the project, save it, reopen it with `IoUtilities.readProject`, save the reopened project to a second archive, reopen that second archive, export it to `.a3w`, and verify the expected project name, camera type, resource metadata, manifest file type, and archive entries. |
| Player export | `.a3w` | Generate a project, export it to the player archive, and verify the expected manifest metadata, Tweedle source entries, resource entries, readable ZIP entries, and absence of editable-only entries such as `programType.xml`. Player export cases are ZIP/player-shape characterization only; they do not require editable save/reopen through `IoUtilities.readProject`. |

For both types, ZIP entry validation rejects unsafe entry names before checking
content. Invalid entries include empty names, absolute paths, `..` traversal,
Windows drive-letter paths, and backslash-separated paths.

### Manifest format

`corpus.properties` is a JDK `Properties` file. It uses one comma-separated
`cases` key and one `case.<id>.*` namespace per case.

```properties
cases=editable-minimal,editable-with-image-resource,player-export-minimal,player-export-with-image-resource

case.editable-minimal.archiveType=a3p
case.editable-minimal.projectName=GoldenEditableMinimal
case.editable-minimal.cameraType=WindowCamera
case.editable-minimal.resources=none
case.editable-minimal.expectedEntries=version.txt,manifest.json,programType.xml
case.editable-minimal.absentEntries=resources.xml
case.editable-minimal.roundTripEditable=true
case.editable-minimal.exportAfterReopen=true

case.editable-with-image-resource.archiveType=a3p
case.editable-with-image-resource.projectName=GoldenEditableWithImageResource
case.editable-with-image-resource.cameraType=WindowCamera
case.editable-with-image-resource.resources=image:golden-texture.png
case.editable-with-image-resource.expectedEntries=version.txt,manifest.json,programType.xml,resources.xml,resources/golden-texture.png
case.editable-with-image-resource.roundTripEditable=true
case.editable-with-image-resource.exportAfterReopen=true

case.player-export-minimal.archiveType=a3w
case.player-export-minimal.projectName=GoldenPlayerExportMinimal
case.player-export-minimal.cameraType=VRHeadset
case.player-export-minimal.resources=none
case.player-export-minimal.expectedEntries=version.txt,manifest.json,src/GoldenPlayerExportMinimal.twe
case.player-export-minimal.absentEntries=programType.xml
case.player-export-minimal.roundTripEditable=false

case.player-export-with-image-resource.archiveType=a3w
case.player-export-with-image-resource.projectName=GoldenPlayerExportWithImageResource
case.player-export-with-image-resource.cameraType=WindowCamera
case.player-export-with-image-resource.resources=image:golden-export-texture.png
case.player-export-with-image-resource.expectedEntries=version.txt,manifest.json,src/GoldenPlayerExportWithImageResource.twe,resources/golden-export-texture.png
case.player-export-with-image-resource.absentEntries=programType.xml
case.player-export-with-image-resource.roundTripEditable=false
```

Supported fields:

| Field | Required | Values |
| --- | --- | --- |
| `cases` | Yes | Comma-separated case IDs. IDs must be unique and use safe filename characters: letters, numbers, `.`, `_`, and `-`. |
| `case.<id>.archiveType` | Yes | `a3p` for editable projects or `a3w` for player exports. |
| `case.<id>.projectName` | Yes | Simple ASCII Alice type name used for generated program type and manifest assertions. It must match `[A-Za-z_][A-Za-z0-9_]*` and must not be a Java reserved word. |
| `case.<id>.cameraType` | Yes | `WindowCamera` or `VRHeadset`. |
| `case.<id>.resources` | Yes | `none` or a comma-separated list of generated resources such as `image:golden-texture.png`. Resource payloads are generated in the test temporary directory. |
| `case.<id>.expectedEntries` | Yes | Comma-separated archive entries that must exist and be readable. Use forward slashes. |
| `case.<id>.absentEntries` | No | Comma-separated archive entries that must not exist. |
| `case.<id>.roundTripEditable` | Yes | `true` only for archive shapes that are expected to reopen as editable projects and save again as `.a3p`. |
| `case.<id>.exportAfterReopen` | No | `true` when an editable case must also prove export to `.a3w` after reopen. |

The validator fails fast on duplicate case IDs, missing required fields,
unsupported archive types, unsupported resource declarations, unsafe project
names, empty expected-entry lists, or unsafe ZIP-entry paths.

### Local commands

Run the focused corpus test when changing project archive save, read, export, or
resource handling:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=GoldenProjectCorpusValidatorTest \
  test
```

Run the full `story-api-migration` test lane:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  test
```

### CI behavior

The validator is named `GoldenProjectCorpusValidatorTest`, so Maven Surefire
picks it up with the existing `core/story-api-migration` test command and
reactor test or verify commands when those lanes run. There is no separate CI
workflow for the corpus, and docs-only pull requests may skip Maven by
change-scope rules. A CI failure in this test means a project/archive behavior
changed and the manifest expectations or the IO implementation need to be
reviewed together.

## RabbitHole baseline parity harness

`RabbitHoleBaselineParityTest` is the text-only baseline lane for generated
NetBeans package output and representative Alice `.a3p`/`.a3w` archive shape.
It lives in `netbeans/src/test/java/org/alice/netbeans/project/` because it
exercises `ProjectCodeGenerator` alongside `IoUtilities.writeProject()` and
`IoUtilities.exportProject()`. Source file presence and hashes are allowed only
as package parity evidence; pure Java imports, declarations, and snippet syntax
live in the focused generated-source lanes described in
[Generated Source Validation](reference/generated-source-validation.md).

The checked-in snapshots are:

```text
netbeans/src/test/resources/org/alice/netbeans/project/parity/
```

Normal runs never mutate snapshots. Update them only after reviewing an
intentional behavior change:

```bash
git submodule update --init tweedle-lang
mvn -pl netbeans -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=RabbitHoleBaselineParityTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Drabbithole.baseline.updateSnapshots=true \
  -Drabbithole.baseline.snapshotDir=netbeans/src/test/resources/org/alice/netbeans/project/parity \
  test
```

See [RabbitHole baseline parity](rabbithole-baseline-parity.md) for the
normalization rules, review workflow, and relationship to `eatme`.

## Dual-baseline replay harness

`DualBaselineReplayHarnessTest` is the headless compatibility lane for
comparing current RabbitHole project I/O summaries against a local preserved
Alice baseline checkout. It lives in
`core/story-api-migration/src/test/java/org/lgna/project/io/compat/` because it
characterizes project save, reopen, export, manifest, source, and resource
behavior around `IoUtilities` and the story API migration boundary.

The harness uses deterministic generated cases only. It never requires checked-in
`.a3p`, `.a3w`, `.a3c`, image, audio, or ZIP payloads. Temporary project,
archive, source, and resource files are generated under the test temporary
directory and deleted after the run.

### Modes

| Mode | How to enable | CI behavior | What it proves |
| --- | --- | --- | --- |
| Fallback | Leave `rabbithole.baseline.checkout` and `RABBITHOLE_BASELINE_CHECKOUT` unset. | Default. | RabbitHole summaries are deterministic, normalized, text-only, and safe to compare. |
| Strict baseline | Set `-Drabbithole.baseline.checkout=/path/to/baseline` or `RABBITHOLE_BASELINE_CHECKOUT=/path/to/baseline`. | Opt-in only; CI does not set it. | RabbitHole summaries exactly match the preserved baseline summaries for the same generated cases. |

The Maven system property takes precedence over the environment variable. Missing
configuration selects fallback mode. A configured but invalid baseline path fails
the test.

### Focused commands

Fallback mode:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=DualBaselineReplayHarnessTest \
  test
```

Strict mode with a Maven property:

```bash
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=DualBaselineReplayHarnessTest \
  -Drabbithole.baseline.checkout=/absolute/path/to/preserved-alice-baseline \
  test
```

Strict mode with an environment variable:

```bash
export RABBITHOLE_BASELINE_CHECKOUT=/absolute/path/to/preserved-alice-baseline
mvn -pl core/story-api-migration \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=DualBaselineReplayHarnessTest \
  test
```

See [Dual-baseline replay harness](dual-baseline-replay-harness.md) for the
summary format, baseline process contract, configuration reference,
test-scope API, and relationship to `eatme`.

### Adding a corpus case

1. Add a new ID to the `cases` list in
   `core/story-api-migration/src/test/resources/golden-project-corpus/corpus.properties`.
2. Add the matching `case.<id>.*` keys with explicit archive type, project name,
   camera type, generated resources, and expected entries.
3. Keep expected entries focused on stable archive contracts: `version.txt`,
   `manifest.json`, editable XML entries, generated `src/*.twe` exports, and
   generated `resources/*` entries.
4. Run the focused validator command before opening a pull request.

## What the coverage lane measures

The aggregate report is written to:

```text
coverage-report/target/site/jacoco-aggregate/index.html
```

The coverage workflow also uploads:

- `coverage-summary.md`
- `coverage-evidence-manifest.json`
- per-module `target/site/jacoco/` reports when they exist

## Test patterns used in this repository

### Headless guards

GUI tests must skip clearly when there is no real display. Use JUnit `Assume`
guards as the first line of the test instead of `if (...) return`.

Examples used in the repository:

```java
assumeFalse("requires a graphical display", GraphicsEnvironment.isHeadless());
```

```java
assumeTrue("Requires non-headless AWT display",
    SaveMenuDoClickProbe.isNonHeadlessAwtDisplayAvailable());
```

These guards make CI output honest: skipped means skipped, not silently passed.

### GL skip patterns

Most unit tests stay away from real OpenGL work. Instead, they test pure math,
scene data, serialization, and state changes in isolation.

Common patterns:

- keep `scenegraph` tests pure model tests with no rendering pipeline
- keep `story-api` tests headless-safe by avoiding `GlrRenderFactory`
- characterize non-GL seams around rendering code, then leave real GL behavior
  to focused integration or proof tests
- use stubs and artifact checks when you only need to prove contract shape

### Characterization-first refactoring

Modernization changes usually start with a characterization test. That protects
the current Alice 3 behavior before a large class is split or moved.

The text migration registry parity lane applies that rule to generated migration
JSON. See
[Verify Text Migration Registry Parity](howto/verify-text-migration-parity.md)
for the focused workflow and
[Text Migration Registry Parity](reference/text-migration-registry-parity.md)
for the JSON, loader, generator, and test contracts.

### Focused module validation

When you touch one area, prefer a targeted Maven command before you run the
whole reactor. For example:

```bash
mvn -pl core/ide -am test
mvn -pl core/scenegraph -am test
mvn -pl core/story-api -am test
```

## core/ide coverage test infrastructure

The `core/ide` module has ~960 test files in
`core/ide/src/test/java/org/alice/ide/coverage/` built on four support classes
(`HeadlessClassExerciseSupport`, `ClassLoadingSweepSupport`,
`SingleClassDefaultArgsSweepTestSupport`, `CompositeCreateViewSweepSupport`).
New coverage follows a three-tier approach. See
[docs/core-ide-coverage.md](core-ide-coverage.md) for the full guide.

### Quick reference

Run core/ide tests with coverage:

```bash
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false verify
```

Maintained Markdown examples use the resilient Xvfb prefix documented in the
[JavaFX Xvfb Launcher Reference](reference/javafx-xvfb-launcher.md).

Check the JaCoCo report:

```bash
cat core/ide/target/site/jacoco/jacoco.csv | python3 -c "
import csv, sys
r = csv.DictReader(sys.stdin)
missed = covered = 0
for row in r:
    missed += int(row['LINE_MISSED']); covered += int(row['LINE_COVERED'])
total = missed + covered
print(f'{covered}/{total} lines = {100*covered/total:.1f}%')
"
```

### Coverage tiers for adding new tests

| Tier | Pattern | Example | Typical yield |
|------|---------|---------|---------------|
| 1 — Sweep targets | Class names in `small-class-targets.txt` | Add `org.alice.ide.ast.export.TypeInfo` | ~20 lines/class |
| 2 — Headless package sweeps | `*HeadlessSweepTest.java` with explicit class name lists | `SceneeditorLogicHeadlessSweepTest` | ~80–200 lines/package |
| 3 — Targeted unit tests | `*Test.java` testing specific logic paths | `ExpressionCascadeManagerTest` | ~30–100 lines/class |

## Practical advice

- Use headless-safe tests for data models and serialization.
- Add display guards only when a real UI is required.
- Keep proof-artifact tests separate from UI-skip logic.
- Run the coverage lane after larger refactors so you catch dropped coverage
  before review.
- When adding coverage to `core/ide`, prefer Tier 2 headless sweeps for new
  packages and Tier 3 targeted tests for high-LOC logic classes. See
  [docs/core-ide-coverage.md](core-ide-coverage.md).
