# Tutorial: Expand a coverage ratchet and review one hotspot

This tutorial walks through a complete coverage-ratchet lane using the current
Alice no-Sims coverage gates.

## Goal

Measure current module coverage, choose conservative ratchet floors, and make an
explicit protected-hotspot decision without changing behavior.

At the end, the pull request has:

- a CI-enforced module ratchet update;
- a coverage summary generated from current JaCoCo reports;
- zero or one behavior-preserving hotspot refactor;
- issue updates that explain the coverage and hotspot decisions.

## 1. Start from develop

Use `origin/develop` as the source of truth:

```sh
git fetch origin develop
git switch -c feat/coverage-ratchet origin/develop
git submodule update --init tweedle-lang
```

This avoids recreating work that is already merged, including aggregate coverage
gates and completed refactors around saving, reopening, editing, saving again,
reopening again, and exporting Alice projects.

## 2. Generate coverage data

Run the no-Sims coverage reactor:

```sh
mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify
```

Then summarize the generated JaCoCo CSV files:

```sh
python3 scripts/summarize-jacoco-coverage.py --output coverage-summary.md
```

The summary lists aggregate line coverage and per-module line coverage. Use this
file as the measurement source for ratchet decisions.

## 3. Select module floors

Suppose the summary reports:

| Scope | Measured line coverage |
| --- | ---: |
| Aggregate reactor | 10.24% |
| `core/ast` | 20.87% |
| `core/model-loading` | 12.47% |
| `core/story-api-migration` | 81.72% |
| `core/tweedle` | 54.13% |
| `netbeans` | 28.54% |

Choose floors that sit below those measurements:

| Scope | Floor |
| --- | ---: |
| Aggregate reactor | 8.0% |
| `core/ast` | 18.0% |
| `core/model-loading` | 10.0% |
| `core/story-api-migration` | 75.0% |
| `core/tweedle` | 50.0% |
| `netbeans` | 25.0% |

The aggregate gate stays at 8.0% because 10.24% measured coverage does not leave
enough cushion for a 10.0% aggregate floor. The module floors are safer because
each has a wider gap between measured coverage and the enforced threshold.

## 4. Validate the proposed gates

Run the summary script with the proposed floors:

```sh
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent netbeans=25.0
```

The command passes only when every configured module report exists, every module
appears at most once, and every configured line coverage floor is met. Keep the
aggregate floor within `0` through `100` by CI policy; module floors are also
validated by the summary script.

## 5. Review one hotspot candidate

Pick at most one oversized production hotspot to review. Do not select a class
that has already been refactored on `develop`.

For each candidate, answer the protection questions:

| Candidate question | Example answer |
| --- | --- |
| What behavior changes if the extraction is wrong? | Project save/load JSON shape, exported resources, IDE save routing, or migration output. |
| Which existing characterization test fails for that behavior? | A focused save/load/export or migration test. |
| Is the refactor only moving or simplifying code? | Yes, no format or routing change. |
| Is the candidate distinct from already-merged work? | Yes, not the completed `JsonModelIo` refactor. |

If the tests do not clearly protect the exact behavior, skip the refactor and
record the reason. A skipped refactor is a valid outcome for this lane.

For the model export hotspot, compare the proposed change with the
[model resource exporter reference](../reference/model-resource-exporter.md).
A safe change keeps callers on `ModelResourceExporter`, keeps helper classes
package-private, and preserves XML output, Java enum output, thumbnail behavior,
and checked `IOException` reporting.

## 6. Keep the pull request focused

A cohesive ratchet-only pull request includes:

- the CI ratchet command update;
- coverage documentation updates;
- coverage validation results;
- no production refactor when no protected hotspot qualifies.

If one protected hotspot qualifies, keep the refactor small and behavior
preserving, and include the affected tests plus checkstyle in the validation
notes. Do not include a second hotspot in the same pull request.

## 7. Record the outcome

Update the pull request notes and any repo-owned tracking issue or investigation
artifact with language like:

```text
Measured no-Sims aggregate line coverage at 10.24%.
Kept aggregate floor at 8.0% because a 10.0% floor has insufficient margin.
Added module floors: core/ast 18.0%, core/model-loading 10.0%,
core/story-api-migration 75.0%, core/tweedle 50.0%, netbeans 25.0%.
Reviewed `ModelResourceExporter` as the protected hotspot and performed one
small behavior-preserving subresource tag helper extraction covered by existing
XML characterization tests.
Validated the coverage gates, affected test scope, and checkstyle.
```

The notes make the ratchet expansion reproducible and explain why the hotspot
decision is behavior-safe.
