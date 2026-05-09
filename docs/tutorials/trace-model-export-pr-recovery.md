# Tutorial: Trace a Model Export PR Recovery

This tutorial walks through the merge-ready recovery for a model export PR,
from QA scenario validation through quality audit to the final PR description
update.

## Goal

Trace one complete merge-ready recovery path for a PR that adds model export
boundary characterization. You will:

1. Inspect the QA scenario YAML and its four-file allowlist.
2. Validate the scenario catalog and schema contract.
3. Run the gated command smoke.
4. Trace three quality audit SEEK/VALIDATE/FIX cycles.
5. Review the merge-ready evidence template on the PR.

The trace uses the `model-export-boundary-smoke` workflow as the concrete
example, but the same pattern applies to any gated-command-smoke PR recovery.

## Before you start

Open a terminal at the repository root and confirm prerequisites:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
gh auth status
```

## Step 1: Read the QA scenario

Open the scenario YAML:

```bash
cat qa/outside-in/alice-desktop/scenarios/model-export-boundary-smoke.yaml
```

Confirm these fields:

| Field | Expected value |
| --- | --- |
| `id` | `alice-desktop-model-export-boundary-smoke` |
| `workflow` | `model-export-boundary-smoke` |
| `automationMode` | `gated-command-smoke` |
| `automation.argv[0]` | `mvn` |
| `automation.argv[8]` | `-Dtest=org.lgna.story.resourceutilities.ModelExportTest` |
| `automation.argv[9]` | `test` |
| `automation.timeoutSeconds` | `600` |

The scenario runs the focused `ModelExportTest` characterization through Maven.
It does not launch the Alice desktop, open a project, or perform visual checks.

## Step 2: Verify the four-file allowlist

The QA scenario requires matching entries in four files. Trace each one:

### Schema workflow enum

```bash
python3 -c "
import json, sys
schema = json.load(open('qa/outside-in/alice-desktop/schema/scenario.schema.json'))
workflows = schema['properties']['workflow']['enum']
assert 'model-export-boundary-smoke' in workflows, 'Missing workflow enum'
print('✅ workflow enum includes model-export-boundary-smoke')
"
```

### Schema argv oneOf

```bash
python3 -c "
import json
schema = json.load(open('qa/outside-in/alice-desktop/schema/scenario.schema.json'))
argv_items = schema['definitions']['automationArgv']['oneOf']
found = any(
    item.get('description','').startswith('ModelExportTest')
    for item in argv_items
)
assert found, 'Missing argv oneOf entry'
print('✅ argv oneOf includes ModelExportTest tuple')
"
```

### Validator workflow values

```bash
grep -q 'model-export-boundary-smoke' \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh \
  && echo '✅ validate-scenarios.sh includes workflow' \
  || echo '❌ Missing from validate-scenarios.sh'
```

### Runner allowed automation

```bash
grep -q 'ModelExportTest' \
  qa/outside-in/alice-desktop/runners/run-scenario.sh \
  && echo '✅ run-scenario.sh includes ModelExportTest clause' \
  || echo '❌ Missing from run-scenario.sh'
```

### Contract test expected argv

```bash
grep -q 'ModelExportTest' \
  qa/outside-in/alice-desktop/tests/test-schema-contract.sh \
  && echo '✅ test-schema-contract.sh includes expected argv' \
  || echo '❌ Missing from test-schema-contract.sh'
```

All five checks should print ✅. If any fails, the four-file allowlist is out
of sync. See
[Alice desktop outside-in QA reference](../reference/alice-desktop-outside-in-qa.md)
for the synchronization rules.

## Step 3: Validate the catalog

Run the catalog validator:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

Confirm the output includes `model-export-boundary-smoke` in the active
scenario list and the validator exits 0.

Run the schema contract test:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

Confirm the test exits 0 with no assertion failures.

## Step 4: Run the gated command smoke

Execute the scenario with the gate enabled:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-model-export-boundary-smoke \
  --evidence-dir /tmp/qa-evidence-model-export
```

Review the evidence:

```bash
cat /tmp/qa-evidence-model-export/alice-desktop-model-export-boundary-smoke/*/status.txt
```

Confirm `outcome=passed` and review the test count. The `ModelExportTest`
characterization includes 23 focused tests covering:

| Category | Tests |
| --- | --- |
| XML generation | Root attributes, resource attributes, bounding boxes, tags, deprecation |
| Generated Java | Enum constants, constructors, compilation, forced enum names |
| Enum naming | `createResourceEnumName` combinations for model and texture names |
| Bounding-box state | Stateful population during XML generation, union computation, subresource refresh |
| Thumbnails | Class thumbnail creation, missing subresource failure, registered thumbnail loss |
| File I/O | `createXMLFile` write/copy paths, output failure surfaces |

## Step 5: Trace the quality audit

A quality audit runs at least three SEEK/VALIDATE/FIX cycles against the PR
diff surface.

### Identify the diff surface

```bash
git --no-pager diff --name-only origin/main...HEAD | head -30
```

### Cycle 1: General sweep

The first cycle examines all changed files for bugs, security issues, and logic
errors. For a model export PR, the typical changed files are:

- `core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceExporter.java`
- `core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceThumbnailWriter.java`
- `core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java`
- `qa/outside-in/alice-desktop/scenarios/model-export-boundary-smoke.yaml`
- `qa/outside-in/alice-desktop/runners/*.sh`
- `qa/outside-in/alice-desktop/schema/scenario.schema.json`
- `qa/outside-in/alice-desktop/tests/test-schema-contract.sh`

For each finding, check whether the affected lines are in the PR diff or in
pre-existing code:

```bash
git --no-pager diff origin/main...HEAD -- <file> | head -40
```

### Cycle 2: Consistency check

The second cycle focuses on cross-file consistency:

- QA scenario YAML fields match the schema constraints.
- Validator, runner, and contract test allowlists are synchronized.
- Test assertions match the documented API contract.
- Documentation references match the actual file paths and method names.

### Cycle 3: Final clean pass

The third cycle re-examines the PR diff surface after any fixes from cycles 1
and 2. A clean final cycle means zero findings in the PR diff scope.
Pre-existing findings outside the diff are reported but do not block
merge-readiness.

## Step 6: Review the PR description

After the recovery script completes, check the PR description:

```bash
gh pr view 425 --json body -q '.body' | tail -30
```

Confirm the `## Merge-Ready Evidence` section includes:

- ✅ QA scenario passed with the test count.
- ✅ Quality audit cycles completed with the final cycle clean.
- Validation commands that a reviewer can copy and run.
- A timestamp.
- A No-op justification if no production code changes were needed.

## Step 7: Verify tests pass

Run the Python test suite to confirm the recovery and workflow tests:

```bash
python3 -m unittest \
  tests.test_merge_ready_pr_recovery_units \
  tests.test_merge_ready_pr_recovery_workflow
```

Run the focused model export characterization to confirm nothing regressed:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

## Key takeaways

| Concept | Why it matters |
| --- | --- |
| Four-file allowlist | Prevents argument injection and keeps QA scenarios executable through the runner allowlist. |
| Quality audit diff scoping | Focuses review effort on PR changes and avoids false positives from pre-existing code. |
| Evidence template | Gives reviewers a structured, verifiable summary without requiring them to rerun every check. |
| No merge | The script brings the PR to merge-ready but does not merge. The reviewer makes the final decision. |
