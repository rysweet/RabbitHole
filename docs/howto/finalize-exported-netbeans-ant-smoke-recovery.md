# Finalize exported NetBeans Ant smoke recovery

Use this guide to recover or finalize an exported NetBeans Ant smoke change when
the implementation already exists on a review branch and the remaining work is
readiness, review, and finalization evidence.

For the behavior contract, see [Exported NetBeans Ant Project
Behavior](../reference/exported-netbeans-ant-project-behavior.md). For the QA
runner contract, see [Alice desktop outside-in QA
reference](../reference/alice-desktop-outside-in-qa.md).

## Contents

- [Before you start](#before-you-start)
- [Check recovery readiness](#check-recovery-readiness)
- [Review scenario and runner contracts](#review-scenario-and-runner-contracts)
- [Run the focused exported Ant smoke](#run-the-focused-exported-ant-smoke)
- [Finalize the recovery](#finalize-the-recovery)
- [Keep the claim narrow](#keep-the-claim-narrow)
- [Troubleshooting](#troubleshooting)

## Before you start

Run commands from the repository root. Use the existing review branch; do not
merge the pull request manually as part of recovery.

Initialize the Tweedle grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Do not wrap the validation commands in external timeout wrappers. The checked-in
scenario metadata and test code own their bounded execution behavior.

## Check recovery readiness

Confirm the branch and head under review are the expected recovery inputs:

```bash
git --no-pager status --short --branch
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
```

The branch check is readiness evidence only. A clean source tree is acceptable
when all current-head checks pass and no implementation change is required. Treat
an unknown previous owner exit as recovery metadata, not as a blocker.

## Review scenario and runner contracts

Validate the scenario catalog and the shell contract tests that keep schema,
workflow allowlists, argv allowlists, and gated command behavior synchronized:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh

NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-gated-command-contract.sh

NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-workflow-contract.sh
```

These checks prove the exported project smoke remains a strict
`gated-command-smoke` scenario with an allowlisted Maven argv. They do not run a
full desktop UI automation path.

## Run the focused exported Ant smoke

Run the current bounded Maven evidence path:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl netbeans -am \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest \
  test
```

Accept the smoke only when the focused test completes successfully and the Ant
logs contain the expected probe evidence:

```text
ANT_RUN_PROBE_OK
ANT_RESOURCE_PROBE_OK
ANT_RUNTIME_CONFIGURATION_PROBE_OK
ANT_TEST_MAIN_PROBE_OK
```

No Ant log may contain:

```text
Java Result:
```

The accepted evidence is limited to exported NetBeans Ant behavior: generated
classes, jar packaging, manifest contents, generated resource packaging, Ant
`jar`, `run`, `run-test-with-main`, `clean`, and runtime metadata propagation up
to the headless probe boundary.

## Finalize the recovery

Use this finalization evidence set in the pull request handoff or recovery note:

| Evidence | Required result |
| --- | --- |
| Branch/head readiness | The checked-out branch and commit match the recovery target. |
| Worktree review | No unrelated repository changes are mixed into the recovery. |
| Tweedle grammar | `tweedle-lang/Grammar` exists. |
| Scenario validation | `validate-scenarios.sh` exits successfully. |
| Schema contract | `test-schema-contract.sh` exits successfully. |
| Gated command contract | `test-gated-command-contract.sh` exits successfully. |
| Workflow contract | `test-workflow-contract.sh` exits successfully. |
| Focused Ant smoke | `Alice3ProjectTemplateAntSmokeTest` exits successfully. |
| Claim boundary | The handoff cites only bounded exported NetBeans Ant smoke evidence and excludes full UI automation, rendering correctness, grading, creative assessment, and lesson completion. |

When no implementation change is required, the final handoff must include a
current-head no-op justification. Keep it executable and specific:

```text
No-op justification: current head <commit> on <branch> already satisfies the
exported NetBeans Ant smoke readiness, scenario contract, gated command contract,
workflow contract, and focused Alice3ProjectTemplateAntSmokeTest evidence path;
no repository changes were required.
```

If any check fails, patch only the failing boundary and rerun the same evidence
path. Do not broaden the claim or substitute unrelated full-reactor, rendering,
grading, or manual workflow evidence for the failed check.

## Keep the claim narrow

Cite this recovery only as silver-thread exported NetBeans Ant smoke evidence.
It proves the checked-in exported Ant project template can compile generated
Alice source, package a jar, run headless probes through the exported runtime
classpath, load generated resources, and clean Ant build outputs.

Do not cite this evidence as proof of:

- manual PR merge safety
- installer validation
- full GUI export journey completion
- full UI automation
- visible rendering correctness
- grading or creative assessment
- lesson completion

Those claims require separate executable evidence and separate documentation.

## Troubleshooting

| Symptom | Use this blocker | Next step |
| --- | --- | --- |
| `tweedle-lang/Grammar` is missing. | `tweedle-grammar-submodule-missing` | Run `git submodule update --init tweedle-lang` and confirm the grammar directory exists. |
| Scenario validation rejects the exported project smoke. | `scenario-contract-mismatch` | Fix the scenario, schema, validator, and contract tests together so workflow and argv allowlists match. |
| The gated command contract fails. | `gated-command-contract-mismatch` | Keep `ALICE_QA_RUN_GATED_SMOKES=1` as the only path that executes the focused Maven smoke through the runner. |
| Maven cannot find the NetBeans project template zip. | `project-template-zip-missing` | Re-run the focused NetBeans Maven command so test resources are processed, then inspect the NetBeans test resources phase. |
| An Ant target exits nonzero. | `ant-target-failed` | Preserve the command log and name the failing target: `jar`, `run`, `run-test-with-main`, or `clean`. |
| Required jar, class, manifest, or resource output is absent. | `generated-jar-output-missing` | Preserve the assertion message and generated project listing. |
