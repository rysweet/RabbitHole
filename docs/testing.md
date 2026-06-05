# Testing

RabbitHole uses a mix of unit tests, characterization tests, and focused
desktop proof tests.

## Run the main test lanes

Validate the documented Getting Started path:

```bash
./scripts/validate-getting-started.sh
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> amplihack getting-started validate --headless
```

Run everything:

```bash
mvn test
```

Run the no-Sims, headless-friendly install lane:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean install
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

Run Checkstyle separately:

```bash
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml
```

Run coverage:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmdep.skip=true -Pcoverage verify
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
guidance, no-Sims launch command, and GUI skip/block semantics described here.
`amplihack getting-started validate` is the branch-installable wrapper entry
point for running the same validator with `uvx --from git+...@<branch-or-commit>`.

| Lane | Command | Intended environment | Success condition |
| --- | --- | --- | --- |
| Headless | `./scripts/validate-getting-started.sh` or `./scripts/validate-getting-started.sh --headless` | CI and local shells without a display | Git checkout and `tweedle-lang/Grammar` are present, the no-Sims Maven install command passes, and the no-Sims launch probe reaches the expected GUI-required message. |
| GUI | `./scripts/validate-getting-started.sh --gui` | Local desktop with real Java AWT display support | The no-Sims Alice desktop launch starts far enough to prove the documented GUI launch path is usable on that platform. |
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

### Skip, fail, and block behavior

| Situation | `--headless` | `--gui` | `--all` |
| --- | --- | --- | --- |
| Missing Git checkout metadata | Fail | Fail | Fail |
| Missing `tweedle-lang` or `tweedle-lang/Grammar` | Fail with `git submodule update --init tweedle-lang` guidance | Fail with the same guidance | Fail with the same guidance |
| No desktop display | Pass only if the launch probe reports `Alice desktop launch requires a graphical environment.` | Exit non-zero because GUI was requested explicitly | Skip the GUI lane and exit successfully after headless validation passes |
| macOS Apple Silicon desktop GUI launch | Headless lane remains valid | Exit non-zero as blocked by [#848](https://github.com/rysweet/RabbitHole/issues/848) | Report blocked and exit successfully after headless validation passes |
| Unknown validator flag | Fail | Fail | Fail |

CI integration calls the headless lane only. Local users should run `--gui`
only when their desktop session supports Java GUI launch.

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
xvfb-run mvn -pl core/ide -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false verify
```

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
