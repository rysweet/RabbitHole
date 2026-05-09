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

## Run the negative contract

```bash
bash qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh
```

The contract passes only when each invalid evidence case exits non-zero and the
validator prints an explicit diagnostic. It exercises:

1. Missing canonical artifact.
2. Non-canonical artifact filename.
3. Symlinked artifact.
4. Malformed JSON artifact.
5. Stale artifact.
6. Future-dated artifact beyond the 300-second validator skew allowance.
7. Scenario, workflow, and run ID identity mismatches.
8. Proven-looking artifact missing required fields.
9. Internally inconsistent `status: "proven"` artifact.
10. Blocked artifact with a known blocker kind.
11. Blocked artifact with an unknown blocker kind.

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
4. Read stderr and match the diagnostic to the evidence class, such as stale, future-dated, identity mismatch, blocked, missing flags, or inconsistent proven evidence.
5. Keep the result as validator coverage only. Do not convert the rejected artifact into Save proof evidence.

## Review result

Accept the negative contract only when failures are fail-closed and diagnostic.
Do not cite a passing negative contract as desktop Save completion. It proves
only that invalid Save proof artifacts cannot be silently accepted.

For the field-level artifact contract, see
[Save Proof Evidence](../reference/save-proof-evidence.md). For the full
negative contract specification, see
[Save Menu Dialog Negative Artifact Contract](../reference/save-menu-dialog-negative-artifact-contract.md).
