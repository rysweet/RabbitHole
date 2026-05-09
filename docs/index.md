# Alice Modernization Documentation

Alice modernization documentation describes durable behavior, repo-owned
contracts, contributor workflows, and compatibility characterization for this
repository.

## Project save and export characterization

- [Project Save and Export Operations](./reference/project-save-export-operations.md) - Reference for the `core/ide` Save, Save As, Export operation behavior, and characterization seams.
- [Save Menu Dialog Write/Readback Proof](./reference/save-menu-dialog-write-proof.md) - Implemented contract for the `save-menu-dialog-write-proof` QA scenario that runs the rendered File-menu Save, controlled Swing chooser, `.a3p` write, readback, and marker proof path without workflow timeout wiring.
- [Save Proof Evidence](./reference/save-proof-evidence.md) - Canonical JSON artifact contract, fail-closed validation rules, and executable blocker semantics for the rendered Save proof path.
- [Robot Save Menu Dialog Write/Readback Proof](./reference/robot-save-menu-dialog-write-readback-proof.md) - Reference for the Robot File-menu Save activation, Swing chooser control, `.a3p` write, readback, marker, and blocker artifact contract.
- [Project IO Corpus Characterization](./reference/project-io-corpus-characterization.md) - Reference for generated `.a3p`, `.a3w`, and `.a3c` archive characterization in `core/story-api-migration`.
- [JSON `.a3c` Constructor Assignment Characterization](./reference/json-a3c-constructor-assignment-characterization.md) - Narrow feature contract for a generated JSON type archive whose constructor assigns a decoded field.
- [ProjectMigrationManager Migration Characterization](./reference/project-migration-manager-characterization.md) - Reference for generated XML-string characterization around versioned Alice project text migrations.
- [Exported NetBeans Ant Project Behavior](./reference/exported-netbeans-ant-project-behavior.md) - Reference for exported-project launcher evidence, deterministic display no-go behavior, the bounded no-Sims exported Ant/NetBeans build proof, and Ant runtime metadata.
- [Generated Story API and AST Source Characterization](./reference/generated-story-api-listener-source-characterization.md) - Reference for AST/source-code-generator snippets, generated NetBeans project source, headless Story API listener seams, compilation, and no-GUI boundaries.
- [Characterize Source-Code-Generator Behavior](./howto/characterize-source-code-generator.md) - How to add or review focused AST, generated NetBeans source, Story API listener, and launcher evidence characterization.
- [Finalize a source-code-generator pull request](./howto/finalize-source-code-generator.md) - How to refresh current-head GitHub evidence, verify review and required-check state, preserve focused `core/ast` scope, and produce a no-timeout no-op or focused-fix finalization.
- [Characterize Project Save and Export Operations](./howto/characterize-project-save-export-operations.md) - How to add or review compatibility tests for save/export operations.
- [Finalize exported NetBeans Ant smoke recovery](./howto/finalize-exported-netbeans-ant-smoke-recovery.md) - How to collect current-head, diff-scope, QA scenario, focused Ant smoke, quality-audit, docs-impact, GitHub Actions, and PR description evidence for a bounded recovery handoff.
- [Run the Save Menu Dialog Write/Readback Proof](./howto/run-save-menu-dialog-write-proof.md) - How to run the focused Robot Save menu/dialog/write/readback QA scenario with Xvfb when needed.
- [Run the Robot Save Menu Dialog Write/Readback Proof](./howto/run-robot-save-menu-dialog-write-readback-proof.md) - Guide for running the focused Robot Save menu/dialog/write/readback proof and reviewing the canonical artifact contract.
- [Characterize Project IO Corpus Behavior](./howto/characterize-project-io-corpus.md) - How to add deterministic LFS-free IO corpus characterization around Alice archive readers and writers.
- [Tutorial: Add a Save Operation Characterization Test](./tutorials/save-operation-characterization-test.md) - A guided example for the first direct Save operation characterization test.
- [Tutorial: Trace the Robot Save Menu Dialog Write/Readback Proof](./tutorials/trace-robot-save-menu-dialog-write-readback-proof.md) - Guided review of Robot menu activation, chooser approval, `.a3p` write, readback, marker evidence, and non-claims.
- [Tutorial: Add a Project IO Corpus Characterization](./tutorials/project-io-corpus-characterization.md) - A guided example for protecting generated `.a3p` archive behavior.
- [Tutorial: Add a ProjectMigrationManager Migration Characterization](./tutorials/project-migration-manager-characterization.md) - A guided example for protecting ordered text migration behavior without binary fixtures.
- [Tutorial: Trace Source-Code-Generator Characterization](./tutorials/trace-source-code-generator-characterization.md) - Guided review from core AST snippets to generated NetBeans source, Story API listener seams, launcher evidence, and bounded non-claims.

## QA and acceptance testing

- [Run Alice desktop outside-in QA](./howto/alice-desktop-outside-in-qa.md) - validate, list, and collect reviewable evidence for user-like desktop acceptance scenarios.
- [Open Africa Full through Select Project with AT-SPI](./howto/open-africa-full-through-select-project-atspi.md) - run and review the target-specific Select Project evidence path for the committed starter project.
- [Recover PR #437 after DIRTY merge state](./howto/recover-pr437-dirty-select-project.md) - dirty-repair operator flow for focused Select Project recovery, validation, edit-and-push reporting, and finalization refresh.
- [Alice desktop outside-in QA tutorial](./tutorials/alice-desktop-outside-in-qa.md) - collect launch evidence and complete a manual workflow evidence checklist.
- [Alice desktop outside-in QA reference](./reference/alice-desktop-outside-in-qa.md) - scenario schema, runner commands, configuration, and evidence artifacts.
- [Learner-world assessment boundary](./reference/learner-world-assessment-boundary.md) - reference for the manual instructor/student setup/open/save evidence boundary, generated checklist wording, unsupported assessment claims, and `define-reviewed-assessment-contract` next boundary.
- [Select Project Africa Full AT-SPI evidence reference](./reference/select-project-africa-full-atspi-evidence.md) - target starter metadata, runner environment, evidence statuses, blocker contract, and post-open gating.
- [PR #437 dirty recovery reference](./reference/pr437-dirty-recovery.md) - dirty-repair mode, metadata contract, validation matrix, report shape, no-op prohibition, and push safety rules.
- [Post-open runtime/display accessibility evidence](./reference/post-open-runtime-display-accessibility-evidence.md) - usage, configuration, artifact API, examples, claim boundaries, and world-canvas pixel target readiness contract.
- [Visible rendering evidence nonclaim contract](./reference/visible-rendering-evidence-nonclaim-contract.md) - executable QA contract for keeping render artifacts, screenshots, generated files, and sampled pixels from becoming visible correctness claims without a separate visual-correctness observation contract.
- [Alice Desktop Silver-Thread Status Report](./reference/silver-thread-status-report.md) - fail-closed shell QA report that aggregates bounded launch, starter change, object placement, procedure edit, run-window/render-affordance, and optional Save/reopen evidence.
- [First-Lesson Live Procedure Target Action Seam](./reference/first-lesson-live-procedure-target-observation.md) - read-only live desktop shard contract for opening the first-lesson starter through Select Project and producing edit-ready-or-named-blocker evidence.
- [Run the First-Lesson Live Procedure Target Action Seam](./howto/run-first-lesson-live-procedure-target-action-seam.md) - how to collect and review the read-only first-lesson procedure/code-editor action-seam evidence.
- [First-Lesson Procedure Tab Code-Editor Backing](./reference/first-lesson-procedure-tab-code-editor-backing.md) - reference for proving `scene.eatmeFirstLesson` tab selection lands on the expected `CodeComposite` and `CodeEditor.getCode()` model.
- [Run the First-Lesson Procedure Tab Code-Editor Backing Proof](./howto/run-first-lesson-procedure-tab-code-editor-backing.md) - validation command and review checklist for the focused `ProcedureTabSelectionTest` backing seam without claiming edit, Save, rendering, assessment, or completion.
- [First-Lesson Code-Editor Action Proof](./reference/first-lesson-code-editor-action-proof.md) - Java proof that the selected `scene.eatmeFirstLesson` code-editor backing seam accepts one deterministic `append-comment` action with target-only marker evidence and negative checks.
- [Run the First-Lesson Code-Editor Action Proof](./howto/run-first-lesson-code-editor-action-proof.md) - command and review checklist for the timeout-free focused action proof.
- [First-Lesson Procedure/Edit Seam](./reference/first-lesson-procedure-edit-seam.md) - narrow executable proof that chains deterministic object placement into AST-level procedure editing.
- [Run the First-Lesson Procedure/Edit Handoff Proof](./howto/run-first-lesson-procedure-edit-handoff.md) - how to run the focused Maven proof and QA command smoke for the procedure/edit handoff.
- [Tutorial: Trace the First-Lesson Procedure Tab Code-Editor Backing Seam](./tutorials/trace-first-lesson-procedure-tab-code-editor-backing.md) - guided review of the selected procedure, selected `CodeComposite`, and backing code-editor model assertions.
- [Tutorial: Trace the First-Lesson Code-Editor Action Proof](./tutorials/trace-first-lesson-code-editor-action-proof.md) - guided review of the target selection, backing identity, deterministic edit action, target-only marker evidence, and non-claims.
- [Tutorial: Trace the First-Lesson Procedure/Edit Seam](./tutorials/trace-first-lesson-procedure-edit-seam.md) - guided review of asserted placement evidence, procedure-edit artifacts, and strict evidence boundaries.
- [Desktop procedure edit and Save automation](./reference/desktop-procedure-edit-and-save-automation.md) - checked-in hook points, next tests, and unproven limits for procedure tab selection and project Save automation.
- [Gadugi exported launcher evidence scenario](./reference/gadugi-exported-launcher-evidence.md) - Gadugi CLI scenario contract, configuration, commands, and conservative boundaries for exported launcher evidence checks.
- [Headless-safe desktop action characterization](./reference/headless-safe-desktop-action-characterization.md) - JavaFX/Swing headless startup contract, Croquet action-flow seams, validation commands, and compatibility rules.
- [Characterize headless-safe desktop actions](./howto/characterize-headless-safe-desktop-actions.md) - how to add or review desktop action characterization without display-dependent tests.
- [Tutorial: Trace a Desktop Action Journey](./tutorials/desktop-action-journey-characterization.md) - guided walkthrough from outside-in menu/action smoke evidence to headless-safe Save action tests.
- [Expand coverage ratchets](./howto/expand-coverage-ratchets.md) - measure no-Sims coverage, choose safe module floors, and document protected hotspot decisions.
- [Coverage ratchet and hotspot review tutorial](./tutorials/coverage-ratchet-and-hotspot-review.md) - guided ratchet expansion example with conservative thresholds and a hotspot skip/refactor decision.
- [Coverage reporting reference](./reference/coverage-reporting.md) - aggregate and module JaCoCo reporting, CLI options, CI ratchet gates, configuration, and path toward 70% line coverage.
- [CI efficiency and no-op validation skips](./reference/ci-efficiency.md) - conservative docs-only CI skip rules, event-aware Maven gates, and preserved validation surfaces.

## Modernization evidence and scorecards

- [Alice modernization scorecard](./reference/modernization-scorecard.md) - generated, reproducible scorecard snapshot for modernization evidence.
- [Modernization scorecard generator reference](./reference/modernization-scorecard-generator.md) - CLI contract, evidence inputs, output-path safety, examples, and review workflow.
- [Modernization corpus manifest](./reference/modernization-corpus-manifest.md) - representative, LFS-independent corpus evidence manifest and validation contract.
- [Maintain the modernization corpus manifest](./howto/maintain-modernization-corpus-manifest.md) - how to update representative corpus evidence without adding binary payloads or Git LFS objects.
- [Tutorial: Add a modernization corpus manifest entry](./tutorials/add-modernization-corpus-manifest-entry.md) - guided example for documenting a new generated fixture shape and refreshing scorecard evidence.
- [Characterize ModelResourceExporter behavior](./howto/characterize-model-resource-exporter.md) - how to add focused, behavior-backed model resource exporter coverage before protected hotspot work.
- [Model resource exporter reference](./reference/model-resource-exporter.md) - XML, generated Java, thumbnail, and protected-hotspot contracts for model-loading resource export.
- [Tutorial: Characterize ModelResourceExporter bounding-box state](./tutorials/model-resource-exporter-bounding-box-state.md) - guided example for protecting the intentional stateful XML bounding-box behavior.
- [Decode coverage characterization](./reference/decode-coverage-characterization.md) - build contract, API behavior, examples, and tutorial guidance for Tweedle, literal arithmetic field initializers, player archive, type archive boundaries, and resource decode tests.
- [Archive/Player Boundary](./reference/archive-player-boundary.md) - fail-closed legacy JSON `.a3w` player readback when manifest-declared image resources are recoverable but the manifest-named program Tweedle type is unsupported.
- [Zero-argument this-method call decode reference](./reference/zero-argument-this-method-call-decode.md) - narrow Tweedle decoder contract for explicit same-type `this.method()` calls with no arguments and the argument-bearing explicit `this.method(label: value, ...)` fail-fast boundary.
- [Simple if-statement decode reference](./reference/simple-if-statement-decode.md) - Tweedle decoder contract for simple `if (condition) { ... }` bodies with supported conditions and an explicit conditional-body allowlist including zero-argument `this.method();` calls.
- [Player archive unsupported Tweedle diagnostics](./reference/player-archive-unsupported-tweedle-diagnostics.md) - narrow JSON `.a3w` archive contract for surfacing unsupported argument-bearing explicit `this` call reasons while keeping literal arithmetic field-initializer support scoped.
- [Validate the Archive/Player Boundary](./howto/validate-archive-player-boundary.md) - how to run and review the focused resource-recovery fail-closed evidence without broad player, rendering, Save, grading, Sims, installer, or lesson-completion claims.
- [Characterize zero-argument this-method call decode](./howto/characterize-zero-argument-this-method-call-decode.md) - how to review focused positive and negative tests for the implemented call slice and named argument-bearing boundary.
- [Characterize simple if-statement decode](./howto/characterize-simple-if-statement-decode.md) - how to add or review focused positive and negative tests for simple-if bodies that preserve supported conditions and the conditional-body allowlist.
- [Characterize player archive unsupported Tweedle diagnostics](./howto/characterize-player-archive-unsupported-tweedle-diagnostics.md) - how to add generated `.a3w` characterization for archive-level unsupported decode reason reporting.
- [Tutorial: Add Archive/Player Boundary Characterization](./tutorials/archive-player-boundary-characterization.md) - guided example for a generated JSON `.a3w` image-resource archive that must fail closed when the manifest-named program type is unsupported.
- [Tutorial: Add zero-argument this-method call decode coverage](./tutorials/zero-argument-this-method-call-decode.md) - guided example for adding decoded `MethodInvocation` shape coverage and unsupported-neighbor assertions without broadening decoder claims.
- [Tutorial: Trace simple if-statement decode](./tutorials/simple-if-statement-decode.md) - guided example for asserting the decoded `ConditionalStatement` shape, allowlisted body statements, and unsupported neighboring conditional-body cases.
- [Tutorial: Trace a player archive unsupported this-call diagnostic](./tutorials/player-archive-unsupported-this-call-diagnostic.md) - guided example for checking fail-closed `.a3w` diagnostics around `this.helper(value: 1)` and `caller.this.helper` context.

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
