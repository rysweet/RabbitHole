# Alice Modernization Documentation

Alice modernization documentation describes durable behavior, repo-owned
contracts, contributor workflows, and compatibility characterization for this
repository.

## Project save and export characterization

- [Project Save and Export Operations](./reference/project-save-export-operations.md) - Reference for the `core/ide` Save, Save As, Export operation behavior, and characterization seams.
- [Save Menu Dialog Write Proof](./reference/save-menu-dialog-write-proof.md) - Reference for the bounded Save menu item, Swing `JFileChooser`, and `.a3p` write proof shard.
- [Project IO Corpus Characterization](./reference/project-io-corpus-characterization.md) - Reference for generated `.a3p`, `.a3w`, and `.a3c` archive characterization in `core/story-api-migration`.
- [JSON `.a3c` Constructor Assignment Characterization](./reference/json-a3c-constructor-assignment-characterization.md) - Narrow feature contract for a generated JSON type archive whose constructor assigns a decoded field.
- [ProjectMigrationManager Migration Characterization](./reference/project-migration-manager-characterization.md) - Reference for generated XML-string characterization around versioned Alice project text migrations.
- [Exported NetBeans Ant Project Behavior](./reference/exported-netbeans-ant-project-behavior.md) - Reference for exported launcher evidence, deterministic display no-go behavior, Ant `run` metadata, and no-Sims characterization.
- [Generated Story API Listener Source Characterization](./reference/generated-story-api-listener-source-characterization.md) - Reference for the headless generated-source evidence lane for synthetic listener registration calls, compilation, and no-GUI boundaries.
- [Characterize Project Save and Export Operations](./howto/characterize-project-save-export-operations.md) - How to add or review compatibility tests for save/export operations.
- [Run the Save Menu Dialog Write Proof](./howto/run-save-menu-dialog-write-proof.md) - How to run the focused desktop-safe Save menu/dialog/write proof with Xvfb when needed.
- [Characterize Project IO Corpus Behavior](./howto/characterize-project-io-corpus.md) - How to add deterministic LFS-free IO corpus characterization around Alice archive readers and writers.
- [Tutorial: Add a Save Operation Characterization Test](./tutorials/save-operation-characterization-test.md) - A guided example for the first direct Save operation characterization test.
- [Tutorial: Add a Project IO Corpus Characterization](./tutorials/project-io-corpus-characterization.md) - A guided example for protecting generated `.a3p` archive behavior.
- [Tutorial: Add a ProjectMigrationManager Migration Characterization](./tutorials/project-migration-manager-characterization.md) - A guided example for protecting ordered text migration behavior without binary fixtures.

## QA and acceptance testing

- [Run Alice desktop outside-in QA](./howto/alice-desktop-outside-in-qa.md) - validate, list, and collect reviewable evidence for user-like desktop acceptance scenarios.
- [Open Africa Full through Select Project with AT-SPI](./howto/open-africa-full-through-select-project-atspi.md) - run and review the target-specific Select Project evidence path for the committed starter project.
- [Alice desktop outside-in QA tutorial](./tutorials/alice-desktop-outside-in-qa.md) - collect launch evidence and complete a manual workflow evidence checklist.
- [Alice desktop outside-in QA reference](./reference/alice-desktop-outside-in-qa.md) - scenario schema, runner commands, configuration, and evidence artifacts.
- [Select Project Africa Full AT-SPI evidence reference](./reference/select-project-africa-full-atspi-evidence.md) - target starter metadata, runner environment, evidence statuses, blocker contract, and post-open gating.
- [Post-open runtime/display accessibility evidence](./reference/post-open-runtime-display-accessibility-evidence.md) - usage, configuration, artifact API, examples, and claim boundaries for the narrow runtime/display evidence lane.
- [First-Lesson Procedure/Edit Seam](./reference/first-lesson-procedure-edit-seam.md) - narrow executable proof that chains deterministic object placement into AST-level procedure editing.
- [Run the First-Lesson Procedure/Edit Handoff Proof](./howto/run-first-lesson-procedure-edit-handoff.md) - how to run the focused Maven proof and QA command smoke for the procedure/edit handoff.
- [Tutorial: Trace the First-Lesson Procedure/Edit Seam](./tutorials/trace-first-lesson-procedure-edit-seam.md) - guided review of asserted placement evidence, procedure-edit artifacts, and strict evidence boundaries.
- [Desktop procedure edit and Save automation](./reference/desktop-procedure-edit-and-save-automation.md) - checked-in hook points, next tests, and unproven limits for procedure tab selection and project Save automation.
- [Gadugi exported launcher evidence scenario](./reference/gadugi-exported-launcher-evidence.md) - Gadugi CLI scenario contract, configuration, commands, and conservative boundaries for exported launcher evidence checks.
- [Headless-safe desktop action characterization](./reference/headless-safe-desktop-action-characterization.md) - JavaFX/Swing headless startup contract, Croquet action-flow seams, validation commands, and compatibility rules.
- [Characterize headless-safe desktop actions](./howto/characterize-headless-safe-desktop-actions.md) - how to add or review desktop action characterization without display-dependent tests.
- [Tutorial: Trace a Desktop Action Journey](./tutorials/desktop-action-journey-characterization.md) - guided walkthrough from outside-in menu/action smoke evidence to headless-safe Save action tests.
- [Expand coverage ratchets](./howto/expand-coverage-ratchets.md) - measure no-Sims coverage, choose safe module floors, and document protected hotspot decisions.
- [Coverage ratchet and hotspot review tutorial](./tutorials/coverage-ratchet-and-hotspot-review.md) - guided ratchet expansion example with conservative thresholds and a hotspot skip/refactor decision.
- [Coverage reporting reference](./reference/coverage-reporting.md) - aggregate and module JaCoCo reporting, CLI options, CI ratchet gates, configuration, and path toward 70% line coverage.
- [CI efficiency notes](./reference/ci-efficiency.md) - current pull request check timing, parallelism status, and safe next targets.

## Modernization evidence and scorecards

- [Alice modernization scorecard](./reference/modernization-scorecard.md) - generated, reproducible scorecard snapshot for modernization evidence.
- [Modernization scorecard generator reference](./reference/modernization-scorecard-generator.md) - CLI contract, evidence inputs, output-path safety, examples, and review workflow.
- [Modernization corpus manifest](./reference/modernization-corpus-manifest.md) - representative, LFS-independent corpus evidence manifest and validation contract.
- [Maintain the modernization corpus manifest](./howto/maintain-modernization-corpus-manifest.md) - how to update representative corpus evidence without adding binary payloads or Git LFS objects.
- [Tutorial: Add a modernization corpus manifest entry](./tutorials/add-modernization-corpus-manifest-entry.md) - guided example for documenting a new generated fixture shape and refreshing scorecard evidence.
- [Characterize ModelResourceExporter behavior](./howto/characterize-model-resource-exporter.md) - how to add focused, behavior-backed model resource exporter coverage before protected hotspot work.
- [Model resource exporter reference](./reference/model-resource-exporter.md) - XML, generated Java, thumbnail, and protected-hotspot contracts for model-loading resource export.
- [Tutorial: Characterize ModelResourceExporter bounding-box state](./tutorials/model-resource-exporter-bounding-box-state.md) - guided example for protecting the intentional stateful XML bounding-box behavior.
- [Decode coverage characterization](./reference/decode-coverage-characterization.md) - build contract, API behavior, examples, and tutorial guidance for Tweedle, player/type archive, and resource decode tests.
- [Zero-argument this-method call decode reference](./reference/zero-argument-this-method-call-decode.md) - narrow Tweedle decoder contract for explicit same-type `this.method()` calls with no arguments and the argument-bearing explicit `this.method(label: value, ...)` fail-fast boundary.
- [Characterize zero-argument this-method call decode](./howto/characterize-zero-argument-this-method-call-decode.md) - how to review focused positive and negative tests for the implemented call slice and named argument-bearing boundary.
- [Tutorial: Add zero-argument this-method call decode coverage](./tutorials/zero-argument-this-method-call-decode.md) - guided example for adding decoded `MethodInvocation` shape coverage and unsupported-neighbor assertions without broadening decoder claims.

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
- [Project Load and Backup Recovery Characterization](./reference/project-backup-recovery-io.md) -
  Reference for saved temporary project loading, corrupt project rejection,
  file-loader QA smoke evidence, backup selection, all-backups failure dispatch,
  configuration, and focused `core/ide` validation.
- [Trace save, load, export, and recovery behavior](./tutorials/trace-save-load-recovery.md) -
  A guided walkthrough from acceptance scenario to model rule to focused test.
