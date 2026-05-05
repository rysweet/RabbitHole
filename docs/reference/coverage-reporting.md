# Coverage reporting and ratchets

Alice coverage reporting measures the no-Sims Maven reactor, publishes an
aggregate Markdown summary, and enforces conservative aggregate and module-level
line coverage ratchets in CI.

The ratchets are deliberately lower than the long-term 70% coverage mission
target. Each floor represents coverage that already exists with a safety margin,
so CI fails only when coverage regresses below a known-supported level.

## Commands

Run the same coverage command used by CI from the repository root:

```sh
mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent netbeans=25.0
```

Initialize the Tweedle grammar submodule before broad Maven validation in a
fresh checkout or worktree:

```sh
git submodule update --init tweedle-lang
```

## Report locations

The Maven `coverage` profile writes JaCoCo reports to these locations:

| Report | Location |
| --- | --- |
| Aggregate no-Sims report | `coverage-report/target/site/jacoco-aggregate/` |
| Aggregate CSV consumed by the summary script | `coverage-report/target/site/jacoco-aggregate/jacoco.csv` |
| Per-module HTML reports | `<module>/target/site/jacoco/index.html` |
| Per-module CSV reports | `<module>/target/site/jacoco/jacoco.csv` |

`scripts/summarize-jacoco-coverage.py` prints the Markdown summary to standard
output. With `--output coverage-summary.md`, it also writes the same Markdown to
`coverage-summary.md`. In GitHub Actions, the workflow appends the summary to
the job summary and uploads the reports as the `coverage-no-sims-reports`
artifact.

## CLI reference

```text
python3 scripts/summarize-jacoco-coverage.py [options]
```

| Option | Required | Description |
| --- | --- | --- |
| `--root PATH` | No | Reads JaCoCo reports under `PATH`. Defaults to the current working directory. |
| `--output PATH` | No | Writes the Markdown coverage summary to `PATH` in addition to standard output. |
| `--min-aggregate-line-percent PERCENT` | No | Fails the command when aggregate line coverage is below `PERCENT` or when the aggregate JaCoCo CSV is missing. `PERCENT` must be a number from `0` through `100`. |
| `--min-module-line-percent MODULE=PERCENT` | No | Adds a module-level line coverage floor. May be repeated for different modules. The command fails when `MODULE` has no JaCoCo CSV or when its line coverage is below `PERCENT`. |

`MODULE` is the repository-relative module path used in the generated coverage
summary, such as `core/story-api-migration` or `netbeans`. Aggregate and module
percentages must be numbers from `0` through `100`, and each module may be
configured only once per command.

Examples:

```sh
python3 scripts/summarize-jacoco-coverage.py --output coverage-summary.md
```

```sh
python3 scripts/summarize-jacoco-coverage.py \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/tweedle=50.0
```

```sh
python3 scripts/summarize-jacoco-coverage.py \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent netbeans=25.0
```

### Output contract

After arguments parse successfully, the summary script prints Markdown to
standard output. When `--output` is provided, it writes the same Markdown to that
file. When `GITHUB_STEP_SUMMARY` is set by GitHub Actions, it appends the same
Markdown to the job summary.

The summary has these sections:

| Section | When it appears | Contents |
| --- | --- | --- |
| `Aggregate no-Sims coverage` | Always | Aggregate line coverage table, or a message that the aggregate CSV is missing. |
| `Per-module reports with JaCoCo CSV output` | Always | One row for each module JaCoCo CSV with non-empty line data. |
| `Aggregate coverage gate` | When `--min-aggregate-line-percent` is provided | Required percent, actual percent when available, and `PASS` or `FAIL`. |
| `Module coverage gates` | When at least one `--min-module-line-percent` is provided | Required percent, actual percent or `missing`, and `PASS` or `FAIL` for each configured module. |

Exit status is part of the contract:

| Exit status | Meaning |
| ---: | --- |
| `0` | The summary was generated and every requested aggregate and module gate passed. |
| `2` | A requested coverage gate failed, a configured report was missing, a threshold argument was malformed, or the same module threshold was configured more than once. |

Argument parsing errors, including duplicate module floors, exit before the
Markdown summary is written. Coverage gate failures exit after writing the
summary so CI can upload the diagnostics.

Missing reports fail only when a gate depends on them. Running the script with
no threshold arguments still produces a best-effort inventory of available
reports so contributors can inspect raw coverage before choosing ratchets.

## Gate behavior

The summary script treats ratchet gates as enforceable contracts:

| Gate | Pass condition | Failure condition |
| --- | --- | --- |
| Aggregate floor | Aggregate no-Sims line coverage is at or above `--min-aggregate-line-percent`. | The aggregate CSV is missing or aggregate line coverage is below the floor. |
| Module floor | The configured module has a JaCoCo CSV and line coverage is at or above its configured floor. | The configured module report is missing or line coverage is below the floor. |
| Aggregate threshold syntax | The aggregate floor parses as a numeric value from `0` through `100`. | The aggregate floor is malformed or outside `0` through `100`, and `argparse` exits before writing the summary. |
| Module threshold syntax | Every module floor is written as `MODULE=PERCENT`, with a numeric percent from `0` through `100`. | A module floor omits `=`, has an empty module name, uses a non-numeric percent value, or falls outside `0` through `100`. |
| Duplicate module floors | Each module appears in at most one configured module floor. | The same module path is passed more than once. |

The command exits successfully only when all configured gates pass. A missing
configured module is a failure because otherwise a module could silently stop
producing coverage data while appearing to preserve its ratchet.

## GitHub Actions workflow contract

`.github/workflows/alice-coverage-ci.yml` runs on pushes to `develop` and on
pull requests that target `develop`. It checks out source with recursive
submodules and with `lfs: false`, so the coverage lane does not add a Git LFS
checkout dependency.

The workflow has three ordered coverage steps:

1. Generate no-Sims aggregate and module reports with
   `mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify`.
2. Run `scripts/summarize-jacoco-coverage.py` with the aggregate floor and every
   configured module floor.
3. Upload `coverage-summary.md`, the aggregate report, module reports,
   `jacoco.exec` files, and Surefire reports as the
   `coverage-no-sims-reports` artifact.

The summarize and upload steps use `if: always()`. That keeps the Markdown
summary and diagnostic artifacts available when Maven tests fail, when coverage
falls below a floor, or when a required module report is missing.

## Current CI configuration

The Alice coverage workflow enforces these no-Sims line coverage floors. The
measured values are the reports used to choose the current floors, not hardcoded
expectations in the workflow:

| Scope | Measured line coverage | CI floor | Reason |
| --- | ---: | ---: | --- |
| Aggregate reactor | 10.24% | 8.0% | The measured value is not high enough to raise the aggregate gate without a brittle margin. |
| `core/ast` | 20.87% | 18.0% | Module coverage safely exceeds the ratchet. |
| `core/model-loading` | 12.47% | 10.0% | Module coverage safely exceeds the ratchet. |
| `core/story-api-migration` | 81.72% | 75.0% | Characterization coverage supports a high module floor with margin. |
| `core/tweedle` | 54.13% | 50.0% | Parser-related coverage supports a module floor with margin. |
| `netbeans` | 28.54% | 25.0% | IDE coverage supports a conservative module floor. |

These values live in `.github/workflows/alice-coverage-ci.yml`. Local validation
uses the same command so contributors can reproduce coverage failures before
opening a pull request.

## Ratchet policy

Coverage ratchets move only upward and only after measured coverage supports the
new floor with a conservative margin.

Use these rules when changing thresholds:

| Rule | Requirement |
| --- | --- |
| Measure first | Sync to `origin/develop`, run the no-Sims coverage command, and choose floors from current reports. |
| Keep margin | Set each floor below measured coverage. Do not round up to the exact observed value. |
| Prefer modules | Add or raise module floors for areas with durable characterization coverage before raising the aggregate floor. |
| Preserve aggregate gate | Do not remove or weaken the aggregate no-Sims floor. |
| Keep the 70% target directional | Treat 70% as the long-term modernization goal, not a blanket requirement for every module today. |
| Avoid artificial coverage | Do not raise gates by excluding production code or by adding tests that execute code without asserting behavior. |

Durable ratchets come from behavior-focused tests around modernization areas such
as save/load/export, Tweedle parsing, story migration, model loading, and IDE
service boundaries.

## Configuration examples

Use `--root` when summarizing reports generated outside the current directory:

```sh
python3 scripts/summarize-jacoco-coverage.py \
  --root /tmp/alice-coverage-worktree \
  --output /tmp/alice-coverage-worktree/coverage-summary.md \
  --min-aggregate-line-percent 8.0
```

Add a new module floor only after the module appears in `coverage-summary.md`
with durable measured coverage above the chosen floor:

```sh
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent netbeans=25.0 \
  --min-module-line-percent core/new-characterized-module=12.0
```

Do not configure the same module twice. The script rejects duplicates because
conflicting floors make the CI contract ambiguous.

## Protected hotspot refactor rule

Coverage ratchet work may include zero or one oversized hotspot refactor. The
refactor is allowed only when existing characterization tests already protect the
exact behavior being simplified.

The rule is intentionally strict:

| Check | Requirement |
| --- | --- |
| Already protected | Existing tests must cover the behavior affected by the extraction or simplification before the refactor starts. |
| Behavior preserving | The refactor must not change Alice project compatibility, generated artifacts, UI routing, or serialization formats. |
| One hotspot maximum | A ratchet expansion pull request includes at most one production hotspot refactor. |
| No duplicate work | Do not repeat refactors already merged to `develop`, including the completed `JsonModelIo` split. |
| Skip when uncertain | If protection is unclear, document the candidate and skip the production refactor. |

Common candidate areas include saving, reopening, editing, saving again,
reopening again, and exporting Alice projects, model resource export, and
application save/load orchestration, but size alone is not enough. The
controlling criterion is test protection for the exact behavior being moved.

For the model-loading resource export hotspot, see the
[Model resource exporter reference](./model-resource-exporter.md) for the
supported XML, Java generation, thumbnail, and error-handling contracts.

## Pull request documentation

A coverage ratchet pull request records:

- the measured aggregate coverage;
- each module floor added or raised;
- why the floor has enough margin;
- the protected hotspot candidate reviewed;
- whether a refactor was performed or skipped;
- local validation results for coverage and affected tests, plus checkstyle when
  production code changes.

The PR also links any relevant repo-owned tracking issue or investigation
artifact after it is updated with the same coverage and hotspot decisions.
