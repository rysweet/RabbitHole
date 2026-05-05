# Expand coverage ratchets

Use this guide to raise Alice coverage gates safely after characterization tests
increase durable coverage.

## Prerequisites

Start from current `develop` before measuring coverage:

```sh
git fetch origin develop
git switch develop
git pull --ff-only origin develop
git submodule update --init tweedle-lang
```

If you work from a feature branch, create it after the branch is current with
`origin/develop`. Do not duplicate coverage or refactor changes already merged to
`develop`.

## 1. Measure coverage

Run the CI-equivalent no-Sims coverage command:

```sh
mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --evidence-manifest coverage-evidence-manifest.json \
  --target-aggregate-line-percent 70.0 \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent netbeans=25.0
```

Open `coverage-summary.md` and identify modules whose measured line coverage is
comfortably above an existing floor or above a proposed new floor.

## 2. Choose safe thresholds

Prefer module ratchets over aggregate ratchets while aggregate coverage remains
low. Set each floor below measured coverage with enough margin for harmless
coverage drift.

Good ratchet choices:

| Measured module line coverage | Safe floor | Why |
| ---: | ---: | --- |
| 81.72% | 75.0% | The floor is high enough to prevent meaningful regression and still leaves several points of margin. |
| 28.54% | 25.0% | The floor protects current progress without failing on tiny report changes. |
| 10.24% aggregate | 8.0% | The aggregate gate remains honest because a 10.0% gate would have too little margin. |

Avoid these changes:

| Change | Problem |
| --- | --- |
| Setting a floor to the exact measured value | Minor report drift can break unrelated pull requests. |
| Raising every module to 70% immediately | The 70% target is directional, not current measured reality. |
| Removing production packages from coverage | The ratchet stops measuring real modernization progress. |
| Adding tests without assertions just to raise coverage | The gate becomes a metric game instead of compatibility protection. |

## 3. Update the CI command

Edit `.github/workflows/alice-coverage-ci.yml` and add or raise
`--min-module-line-percent MODULE=PERCENT` arguments.

Example:

```yaml
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --evidence-manifest coverage-evidence-manifest.json \
  --target-aggregate-line-percent 70.0 \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent netbeans=25.0
```

Use repository-relative module paths exactly as they appear in
`coverage-summary.md`. Keep aggregate and module floors in the `0` through `100`
range; the summary script rejects threshold values outside that range.

## 4. Validate the ratchet locally

Run the summary script against the generated JaCoCo reports with the proposed
floors:

```sh
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --evidence-manifest coverage-evidence-manifest.json \
  --target-aggregate-line-percent 70.0 \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent netbeans=25.0
```

The command must pass without malformed threshold, duplicate module, or
missing-module failures. If a module report is missing, either fix the coverage
generation for that module or do not configure a floor for it.

Open `coverage-summary.md` for the human-readable result and
`coverage-evidence-manifest.json` for the deterministic inventory of aggregate
JaCoCo state, module JaCoCo state, diagnostic artifact paths, configured gate
results, and the non-gating 70% aggregate target status. Do not describe the
70% target as met unless the aggregate JaCoCo CSV exists and reports aggregate
line coverage of at least `70.0%`. Module-level reports can justify module
ratchets, but they cannot substitute for aggregate target evidence.

## 5. Review protected hotspots

Before changing production code, check whether the ratchet work includes an
oversized hotspot that is already protected by characterization tests.

Use this checklist:

| Question | Required answer |
| --- | --- |
| Is the candidate still present on current `develop`? | Yes. |
| Is it different from already-merged refactors such as `JsonModelIo`? | Yes. |
| Do existing tests cover the exact behavior that would move? | Yes. |
| Can the change be described as extraction, naming, or small flow simplification? | Yes. |
| Does it preserve serialization, save/load behavior, export behavior, UI routing, and file formats? | Yes. |

If any answer is not clearly yes, skip the refactor. Do not add new tests merely
to justify a production refactor in this lane.

When the candidate is `ModelResourceExporter`, use the
[model resource exporter reference](../reference/model-resource-exporter.md) as
the behavior contract. The safe refactor boundary is extraction into
package-private Java, XML, or thumbnail helpers while preserving the exporter
API, generated XML, generated Java, thumbnail paths, and checked error handling.

## 6. Document the decision

Update the pull request description and any repo-owned tracking issue or
investigation artifact with:

- measured aggregate coverage;
- measured coverage for each ratcheted module;
- the `coverage-summary.md` and `coverage-evidence-manifest.json` artifact paths
  reviewers should inspect;
- the floor chosen for each module;
- the safety margin rationale;
- the hotspot candidate reviewed;
- the refactor performed or the reason it was skipped;
- validation results for coverage and affected tests, plus checkstyle when
  production code changes.

This record makes the ratchet auditable and prevents later lanes from
rediscovering the same hotspot decision.
