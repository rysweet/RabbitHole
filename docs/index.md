# Alice Modernization Documentation

Alice modernization documentation describes durable behavior, repo-owned
contracts, contributor workflows, and compatibility characterization for this
repository.

## Project save and export characterization

- [Project Save and Export Operations](./reference/project-save-export-operations.md) - Reference for the `core/ide` Save, Save As, and Export operation behavior and planned characterization scope.
- [Characterize Project Save and Export Operations](./howto/characterize-project-save-export-operations.md) - How to build the first compatibility-test layer for the save/export operations.
- [Tutorial: Add a Save Operation Characterization Test](./tutorials/save-operation-characterization-test.md) - A guided example for the first direct Save operation characterization test.

## QA and acceptance testing

- [Run Alice desktop outside-in QA](./howto/alice-desktop-outside-in-qa.md) - validate, list, and collect reviewable evidence for user-like desktop acceptance scenarios.
- [Alice desktop outside-in QA tutorial](./tutorials/alice-desktop-outside-in-qa.md) - collect launch evidence and complete a manual workflow evidence checklist.
- [Alice desktop outside-in QA reference](./reference/alice-desktop-outside-in-qa.md) - scenario schema, runner commands, configuration, and evidence artifacts.
- [Expand coverage ratchets](./howto/expand-coverage-ratchets.md) - measure no-Sims coverage, choose safe module floors, and document protected hotspot decisions.
- [Coverage ratchet and hotspot review tutorial](./tutorials/coverage-ratchet-and-hotspot-review.md) - guided ratchet expansion example with conservative thresholds and a hotspot skip/refactor decision.
- [Coverage reporting reference](./reference/coverage-reporting.md) - aggregate and module JaCoCo reporting, CLI options, CI ratchet gates, configuration, and path toward 70% line coverage.

## Formal specification lane

The formal-spec lane documents Alice project archive and backup-recovery
behavior as acceptance contracts, a small TLA+ recovery model, and focused JUnit
characterization tests.

- [Formal spec lane concepts](./concepts/formal-spec-lane.md) - Why the lane
  exists and how the artifacts fit together.
- [Use the formal spec artifacts](./howto/use-formal-spec-artifacts.md) - How to
  apply the Gherkin and TLA+ contracts while changing save, load, export, or
  backup recovery behavior.
- [Formal spec contracts reference](./reference/formal-spec-contracts.md) -
  Artifact inventory, archive contracts, recovery model details, configuration,
  and executable validation boundaries.
- [Trace save, load, export, and recovery behavior](./tutorials/trace-save-load-recovery.md) -
  A guided walkthrough from acceptance scenario to model rule to focused test.
