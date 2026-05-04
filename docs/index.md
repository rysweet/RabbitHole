# Alice Modernization Documentation

Alice modernization documentation describes durable behavior, repo-owned
contracts, contributor workflows, and compatibility characterization for this
repository.

## Project save and export characterization

- [Project Save and Export Operations](./reference/project-save-export-operations.md) - Reference for the `core/ide` Save, Save As, Export operation behavior, and characterization seams.
- [Characterize Project Save and Export Operations](./howto/characterize-project-save-export-operations.md) - How to add or review compatibility tests for save/export operations.
- [Tutorial: Add a Save Operation Characterization Test](./tutorials/save-operation-characterization-test.md) - A guided example for the first direct Save operation characterization test.

## QA and acceptance testing

- [Run Alice desktop outside-in QA](./howto/alice-desktop-outside-in-qa.md) - validate, list, and collect reviewable evidence for user-like desktop acceptance scenarios.
- [Alice desktop outside-in QA tutorial](./tutorials/alice-desktop-outside-in-qa.md) - collect launch evidence and complete a manual workflow evidence checklist.
- [Alice desktop outside-in QA reference](./reference/alice-desktop-outside-in-qa.md) - scenario schema, runner commands, configuration, and evidence artifacts.
- [Headless-safe desktop action characterization](./reference/headless-safe-desktop-action-characterization.md) - JavaFX/Swing headless startup contract, Croquet action-flow seams, validation commands, and compatibility rules.
- [Characterize headless-safe desktop actions](./howto/characterize-headless-safe-desktop-actions.md) - how to add or review desktop action characterization without display-dependent tests.
- [Tutorial: Trace a Desktop Action Journey](./tutorials/desktop-action-journey-characterization.md) - guided walkthrough from outside-in menu/action smoke evidence to headless-safe Save action tests.
- [Expand coverage ratchets](./howto/expand-coverage-ratchets.md) - measure no-Sims coverage, choose safe module floors, and document protected hotspot decisions.
- [Coverage ratchet and hotspot review tutorial](./tutorials/coverage-ratchet-and-hotspot-review.md) - guided ratchet expansion example with conservative thresholds and a hotspot skip/refactor decision.
- [Coverage reporting reference](./reference/coverage-reporting.md) - aggregate and module JaCoCo reporting, CLI options, CI ratchet gates, configuration, and path toward 70% line coverage.
- [Model resource exporter reference](./reference/model-resource-exporter.md) - XML, generated Java, thumbnail, and protected-hotspot contracts for model-loading resource export.
- [Decode coverage characterization](./reference/decode-coverage-characterization.md) - build contract, API behavior, examples, and tutorial guidance for Tweedle, player/type archive, and resource decode tests.

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
