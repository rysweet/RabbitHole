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

Run the no-Sims, headless-friendly lane:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean test
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
| Headless | `./scripts/validate-getting-started.sh` or `./scripts/validate-getting-started.sh --headless` | CI and local shells without a display | Git checkout and `tweedle-lang/Grammar` are present, the no-Sims Maven test command passes, and the no-Sims launch probe reaches the expected GUI-required message. |
| GUI | `./scripts/validate-getting-started.sh --gui` | Local desktop with real Java AWT display support | The no-Sims Alice desktop launch starts far enough to prove the documented GUI launch path is usable on that platform. |
| All | `./scripts/validate-getting-started.sh --all` | Local validation before sharing setup changes | Headless validation passes; GUI validation runs when supported and reports a clear skip or blocker when unsupported without failing the command. |

The headless lane runs this Maven command:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean test
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
