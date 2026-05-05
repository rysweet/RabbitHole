# Modernization scorecard generator reference

The modernization scorecard generator produces the checked-in
[`modernization-scorecard.md`](./modernization-scorecard.md) evidence snapshot.
The snapshot is generated output; this page is the durable hand-authored
reference for the generator's command-line contract, evidence sources, review
workflow, and output-path safety behavior.

Use the generated scorecard as an evidence index. It reports what is measured,
what CI ratchets today, and what remains blocked. It must not claim that the
long-term 70% line coverage target is met unless current aggregate JaCoCo data
proves it.

## Evidence sources

The generator has no configuration file. Its inputs are repository state and
optional local evidence reports.

| Evidence source | Required to generate | Effect |
| --- | --- | --- |
| `.github/workflows/alice-coverage-ci.yml` | Yes | Supplies aggregate and module CI ratchet thresholds. |
| `qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json` | Yes | Supplies desktop QA journey metadata. |
| `git ls-files '*.java'` | Yes | Supplies tracked Java files for hotspot detection. |
| `coverage-report/target/site/jacoco-aggregate/jacoco.csv` | No | Supplies current aggregate line coverage when present. |
| `<module>/target/site/jacoco/jacoco.csv` | No | Supplies current module line coverage when present. |
| `docs/reference/modernization-corpus-manifest.json` | No | Supplies representative corpus coverage metadata without requiring Git LFS payloads. |

Missing optional evidence is reported as missing or blocked evidence rather
than converted to false zero coverage. The generator does not run `git lfs
pull`, inspect binary corpus payloads, or modify production code.

The corpus manifest is representative evidence only. It is not full historical
archive coverage. Manifest entries must use non-empty repository-relative
`path` values, non-empty `description` values, and non-empty
`generatedFixtureExpectations` lists that describe the expected generated
fixture behavior rather than pointing to checked-in binary payloads.

## CLI contract

```text
python3 scripts/generate-modernization-scorecard.py [options]
```

| Option | Required | Description |
| --- | --- | --- |
| `--output PATH` | No | Writes the generated Markdown scorecard to `PATH`. Use `docs/reference/modernization-scorecard.md` when refreshing the checked-in snapshot. |
| `--root PATH` | No | Reads scorecard inputs from another Alice modernization checkout or worktree. Defaults to the current working directory. |

With no `--output`, the generator prints the Markdown scorecard to standard
output. With `--root`, relative output paths are resolved inside the inspected
checkout, so reviewers can refresh that checkout's checked-in scorecard without
changing their shell working directory.

The supported automation interface is the command-line script. There is no
stable Python package API for downstream callers. Tests may import script
internals for characterization, but external automation should invoke the CLI
and consume the generated Markdown file or standard output.

| Contract | Behavior |
| --- | --- |
| Successful generation | Exit `0`; write Markdown to `--output` or standard output. |
| Invalid arguments | Exit with argparse failure before writing output. |
| Invalid repository evidence | Exit non-zero with a concise error message and no traceback. |
| Missing optional evidence | Generate the scorecard and mark the evidence as missing or blocked. |

## Output-path safety

`--output` is always constrained to the resolved `--root`.

| Output form | Accepted when | Rejected when |
| --- | --- | --- |
| Relative path | The resolved path stays inside `--root`. | The path escapes `--root` with traversal such as `../..`. |
| Absolute path | The absolute path is inside `--root`. | The absolute path points outside `--root`. |

Rejected output paths fail during argument handling before the scorecard is
rendered or written. This prevents a review command for one checkout from
overwriting files elsewhere on the machine.

## Output contract

The generator writes plain Markdown. The generated scorecard currently uses this
section order:

1. `Alice Modernization Scorecard`
2. `Generation`
3. `Coverage ratchets`
4. `Aggregate coverage state`
5. `Module coverage state`
6. `70% target status`
7. `Production hotspots over 500 lines`
8. `QA journey automation gaps`
9. `Corpus gaps`
10. `Remaining blockers`
11. `Interpretation notes`

Generated output ends with one newline, omits timestamps, and omits host-specific
absolute paths.

## Examples

Refresh the checked-in scorecard for the current branch:

```sh
python3 scripts/generate-modernization-scorecard.py \
  --output docs/reference/modernization-scorecard.md
```

Preview the scorecard without modifying files:

```sh
python3 scripts/generate-modernization-scorecard.py
```

Compare the current branch with another worktree:

```sh
python3 scripts/generate-modernization-scorecard.py \
  --root ../alice-modernization-worktree
```

Generate a review artifact under the inspected checkout:

```sh
python3 scripts/generate-modernization-scorecard.py \
  --root ../alice-modernization-worktree \
  --output review-artifacts/alice-modernization-scorecard.md
```

Attempting to write outside the inspected checkout fails:

```sh
python3 scripts/generate-modernization-scorecard.py \
  --root ../alice-modernization-worktree \
  --output ../../scorecard.md
```

Use standard output when attaching a scorecard to another local command:

```sh
python3 scripts/generate-modernization-scorecard.py \
  --root ../alice-modernization-worktree > /tmp/alice-scorecard-preview.md
```

## Review workflow

Use this workflow when reviewing a branch that changes coverage, QA scenarios,
corpus evidence, or modernization documentation.

1. Check out the Alice modernization branch under review.
2. If the checkout is fresh and broad Maven validation is needed, initialize the
   Tweedle grammar submodule:

   ```sh
   git submodule update --init tweedle-lang
   ```

3. Run any coverage or QA commands needed to produce local evidence. The
   scorecard can run without optional reports, but it will mark missing evidence
   as blocked or unmeasured.
4. Regenerate the checked-in scorecard:

   ```sh
   python3 scripts/generate-modernization-scorecard.py \
     --output docs/reference/modernization-scorecard.md
   ```

5. Review the diff. Expected changes are limited to the evidence that changed:
   ratchet values, measured coverage availability, hotspot counts, QA journey
   gaps, corpus status, or blocker text.
6. If the branch does not intentionally change modernization evidence, the
   regenerated scorecard should match the checked-in file.

Review instructions must name the Alice modernization checkout and the
repository-owned generator command. They must not require any helper command
outside this repository.

## Runtime notes

The generator is Python-stdlib-only. If local automation around the scorecard
uses Node, preserve the repository preference:

```sh
export NODE_OPTIONS=--max-old-space-size=32768
```

That Node memory setting is only for local automation that already uses Node.
The scorecard generator itself is invoked with Python and has no Node runtime
dependency.
