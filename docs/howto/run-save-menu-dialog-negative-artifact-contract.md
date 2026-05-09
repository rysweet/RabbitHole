# Run the Save Menu Dialog Negative Artifact Contract

Use this guide to verify that the Save proof artifact validator rejects bad
evidence explicitly. This is a negative contract for the evidence-validation
seam, not the positive rendered Save write/readback proof.

## Prerequisites

Run commands from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

No display is required. The contract does not launch Alice, run Maven, or use
Xvfb.

Confirm the tweedle-lang submodule is initialized and the scenario catalog is
valid:

```bash
git submodule update --init tweedle-lang
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The validator must report the `save-negative-artifact-contract` workflow among
the registered scenarios. If it does not, the scenario YAML or schema enum is
out of sync.

## Run the negative contract

```bash
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

The contract passes only when each missing-context or invalid evidence case
exits non-zero and the validator prints an explicit diagnostic. It exercises:

1. Missing validator context options.
2. Missing canonical artifact.
3. Non-canonical artifact filename.
4. Symlinked artifact.
5. Malformed JSON artifact.
6. Non-object JSON artifact.
7. Stale artifact.
8. Future-dated artifact beyond the 300-second validator skew allowance.
9. Scenario, workflow, and run ID identity mismatches.
10. Proven-looking artifact missing required fields.
11. Internally inconsistent `status: "proven"` artifact.
12. Blocked artifact with a known blocker kind.
13. Blocked artifact with an unknown blocker kind.

## Run through the Amplihack CLI wrapper

Reviewers can install the wrapper from a branch or commit and execute the same
checked-out contract:

```bash
uvx --from git+https://github.com/rysweet/RabbitHole.git@<branch-or-commit> \
  amplihack alice-qa save-negative-contract
```

Run it from the root, or a child directory, of the checkout under review. The
wrapper delegates to the checked-out
`qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh`
script and accepts no extra arguments. Its success proves only that invalid Save
proof artifacts fail closed with explicit diagnostics. Replace
`<branch-or-commit>` with the PR branch or commit being reviewed.

## Run the positive proof contract beside it

When reviewing this lane, run the existing positive contract as a separate check:

```bash
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-write-proof-contract.sh
```

The positive contract preserves the current Save write/readback proof behavior.
The negative contract preserves fail-closed validation of artifacts that must not
be accepted as proof.

## Direct validator example

To debug one artifact by hand, call the runner seam directly:

```bash
tmp_dir=$(mktemp -d)
mkdir -p "$tmp_dir/projects"
cp qa/outside-in/alice-desktop/tests/fixtures/save-proof-evidence/inconsistent-proven.json \
  "$tmp_dir/robot-save-menu-dialog-write-readback-proof.json"

qa/outside-in/alice-desktop/runners/run-scenario.sh validate-save-proof-evidence \
  "$tmp_dir/robot-save-menu-dialog-write-readback-proof.json" \
  --scenario alice-desktop-save-menu-dialog-write-proof \
  --workflow save-menu-dialog-write-proof \
  --run-id contract-run-1 \
  --started-at-epoch 4102444800
```

That example must fail because the fixture is intentionally inconsistent: it
claims `status: "proven"` while its write/readback fields cannot satisfy the
canonical Save proof artifact contract. The copy step preserves the required
canonical artifact filename so the validator reaches the consistency checks.

## Mini tutorial: trace a rejected artifact

1. Copy one fixture to a temporary canonical artifact name.
2. Run `validate-save-proof-evidence` with the expected scenario, workflow, run ID, and command start epoch.
3. Confirm the command exits non-zero.
4. Read stderr and match the diagnostic to the evidence class, such as missing validator context, wrong-name, symlink, non-object, stale, future-dated, identity mismatch, blocked, missing flags, or inconsistent proven evidence.
5. Keep the result as validator coverage only. Do not convert the rejected artifact into Save proof evidence.

## Review result

Accept the negative contract only when failures are fail-closed and diagnostic.
Do not cite a passing negative contract as desktop Save completion. It proves
only that invalid Save proof artifacts cannot be silently accepted.

Keep review wording narrow. This contract does not prove full Save behavior,
Save As behavior, visible rendering correctness, grading, lesson completion,
learner assessment, broad UI automation, or native dialog automation.

For the field-level artifact contract, see
[Save Proof Evidence](../reference/save-proof-evidence.md). For the full
negative contract specification, see
[Save Menu Dialog Negative Artifact Contract](../reference/save-menu-dialog-negative-artifact-contract.md).

## Verify scenario registration

After any edits to the scenario YAML or workflow enum, confirm all five sync
surfaces agree:

```bash
# Scenario catalog validation (includes schema enum check)
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

# Workflow contract test (exact-match enum enforcement)
bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh

# Schema contract test (argv and workflow allowlists)
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

All three must pass before the negative contract result is trustworthy.

## Related documentation

- [Save Menu Dialog Negative Artifact Contract](../reference/save-menu-dialog-negative-artifact-contract.md) — full reference and registered scenario details
- [Save Proof Evidence](../reference/save-proof-evidence.md) — canonical artifact field contract
- [Save Menu Dialog Write/Readback Proof](../reference/save-menu-dialog-write-proof.md) — the positive scenario this contract guards
- [Alice desktop outside-in QA reference](../reference/alice-desktop-outside-in-qa.md) — scenario catalog and workflow list
- [Tutorial: Trace PR #430 No-Op Finalization](../tutorials/pr430-save-negative-no-op-finalization.md) — end-to-end guided finalization
