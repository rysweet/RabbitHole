# Alice Modernization Scorecard

The Alice modernization scorecard is the repo-owned reference view of current
modernization evidence. It reports what is measured, what is ratcheted, and
what remains blocked without treating the long-term 70% line coverage target
as current reality.

## Generation

Refresh this checked-in scorecard from the repository root:

```sh
python3 scripts/generate-modernization-scorecard.py \
  --output docs/reference/modernization-scorecard.md
```

Reviewers can reproduce this scorecard from an Alice modernization checkout
with the repository script, then compare the checked-in scorecard with the
generated output.

See the [Modernization scorecard generator reference](./modernization-scorecard-generator.md)
for the durable CLI contract, output-path safety behavior, examples, and
review workflow.

## Coverage ratchets

Coverage ratchets are executable CI floors, not the long-term target. They are
parsed from `.github/workflows/alice-coverage-ci.yml`, which runs the no-Sims
coverage summary with Git LFS disabled.

| Scope | Current CI floor | Source |
| --- | ---: | --- |
| Aggregate reactor | 8.0% | `--min-aggregate-line-percent 8.0` |
| `core/ast` | 18.0% | `--min-module-line-percent core/ast=18.0` |
| `core/model-loading` | 10.0% | `--min-module-line-percent core/model-loading=10.0` |
| `core/story-api-migration` | 75.0% | `--min-module-line-percent core/story-api-migration=75.0` |
| `core/tweedle` | 50.0% | `--min-module-line-percent core/tweedle=50.0` |
| `netbeans` | 25.0% | `--min-module-line-percent netbeans=25.0` |

When these workflow values change, the generated scorecard changes with them.
Do not manually maintain a second ratchet table.

## Aggregate coverage state

Aggregate coverage is measured only when this file exists in the inspected
checkout:

```text
coverage-report/target/site/jacoco-aggregate/jacoco.csv
```

| Measurement | State | Meaning |
| --- | --- | --- |
| Aggregate JaCoCo CSV | Missing | The no-Sims Maven coverage lane has not produced aggregate coverage data in this checkout. |
| Aggregate line coverage | Not measured | The scorecard cannot report a current aggregate percent without the CSV. |
| Aggregate CI ratchet | 8.0% | The CI floor is still reported because it comes from the workflow. |

Missing aggregate coverage is a blocker for claiming coverage progress, but it
is not a scorecard generation failure.

## Module coverage state

Module coverage is measured from `target/site/jacoco/jacoco.csv` files under
module directories. A module with a CI ratchet is listed even when its local
CSV is missing, because losing a ratcheted module report is itself a gap.

| Module | CI floor | Expected CSV | State |
| --- | ---: | --- | --- |
| `core/ast` | 18.0% | `core/ast/target/site/jacoco/jacoco.csv` | Missing measurement |
| `core/model-loading` | 10.0% | `core/model-loading/target/site/jacoco/jacoco.csv` | Missing measurement |
| `core/story-api-migration` | 75.0% | `core/story-api-migration/target/site/jacoco/jacoco.csv` | Missing measurement |
| `core/tweedle` | 50.0% | `core/tweedle/target/site/jacoco/jacoco.csv` | Missing measurement |
| `netbeans` | 25.0% | `netbeans/target/site/jacoco/jacoco.csv` | Missing measurement |

Rows with no line totals are ignored rather than converted to false zero
coverage. This matches the coverage summary contract.

## 70% target status

The 70% line coverage goal is the modernization mission target. It is not the
current CI ratchet and is not considered met unless measured aggregate JaCoCo
coverage is at least `70.0%`.

| Target | State | Evidence |
| --- | --- | --- |
| 70.0% aggregate line coverage | Not claimable | Aggregate JaCoCo CSV is missing, so the scorecard cannot claim the 70% target from current measured data. |

## Production hotspots over 500 lines

A production hotspot is a tracked Java file with more than 500 physical lines
using this deterministic filter:

1. Start from `git ls-files '*.java'`.
2. Include paths containing `/src/main/java/`.
3. Exclude paths containing `/target/`, `/build/`, `/generated/`, `/generated-sources/`, `/drinkme/`, or `/src/main/java/test/`.
4. Include only files with line count greater than 500.
5. Sort by descending line count, then by path.

Current scorecard state for this checkout: 51 production-root Java hotspots over 500 lines.

| File | Lines |
| --- | ---: |
| `core/story-api-migration/src/main/java/org/lgna/project/migration/ProjectMigrationManager.java` | 5914 |
| `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java` | 1842 |
| `core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceExporter.java` | 1505 |
| `core/story-api/src/main/java/org/lgna/ik/core/enforcer/TightPositionalIkEnforcer.java` | 1328 |
| `core/ide/src/main/java/org/alice/stageide/sceneeditor/StorytellingSceneEditor.java` | 1259 |
| `core/scenegraph/src/main/java/edu/cmu/cs/dennisc/scenegraph/io/ASG.java` | 1241 |
| `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/Graphics2D.java` | 1225 |
| `core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java` | 1193 |
| `core/model-loading/src/main/java/org/lgna/story/resourceutilities/JointedModelColladaExporter.java` | 1181 |
| `core/croquet/src/main/java/org/lgna/croquet/AbstractComposite.java` | 1113 |
| `core/story-api/src/main/java/Jama/Matrix.java` | 1055 |
| `core/story-api/src/main/java/org/alice/interact/DragAdapter.java` | 1039 |
| `core/story-api/src/main/java/org/lgna/story/implementation/AbstractTransformableImp.java` | 977 |
| `core/ast/src/main/java/org/alice/serialization/tweedle/Encoder.java` | 959 |
| `core/story-api/src/main/java/Jama/EigenvalueDecomposition.java` | 956 |
| `core/story-api/src/main/java/org/lgna/story/implementation/JointedModelImp.java` | 955 |
| `core/tweedle/src/main/java/org/alice/tweedle/run/VirtualMachine.java` | 938 |
| `core/story-api/src/main/java/org/lgna/story/implementation/alice/AliceResourceUtilities.java` | 916 |
| `core-nonfree/ide-nonfree/src/main/java/org/alice/stageide/personresource/IngredientsComposite.java` | 789 |
| `core/story-api/src/main/java/org/lgna/story/implementation/EntityImp.java` | 786 |
| `core/ide/src/main/java/org/alice/media/audio/FloatSampleBuffer.java` | 780 |
| `core/ide/src/main/java/org/alice/ide/ProjectApplication.java` | 761 |
| `core/ast/src/main/java/org/lgna/project/ast/SourceCodeGenerator.java` | 749 |
| `core/ide/src/main/java/org/alice/stageide/sceneeditor/viewmanager/CameraMarkerTracker.java` | 731 |
| `core/story-api/src/main/java/org/lgna/story/implementation/GroundImp.java` | 727 |
| `core/model-loading/src/main/java/org/lgna/story/resourceutilities/JointedModelGltfExporter.java` | 724 |
| `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/adapters/GlrSkeletonVisual.java` | 712 |
| `core/model-loading/src/main/java/org/lgna/story/resourceutilities/JointedModelColladaImporter.java` | 688 |
| `core/story-api/src/main/java/org/alice/interact/handle/ManipulationHandle3D.java` | 664 |
| `core/ast/src/main/java/org/lgna/project/ast/AstUtilities.java` | 649 |
| `core/ast/src/main/java/org/lgna/project/ast/JavaCodeGenerator.java` | 643 |
| `core/croquet/src/main/java/org/lgna/croquet/views/FolderTabbedPane.java` | 643 |
| `core/story-api-migration/src/main/java/org/lgna/project/io/JsonProjectIo.java` | 642 |
| `core/ide/src/main/java/org/alice/ide/clipboard/icons/ClipboardIcon.java` | 619 |
| `core/ide/src/main/java/org/alice/ide/ast/declaration/DeclarationLikeSubstanceComposite.java` | 617 |
| `core/story-api/src/main/java/org/lgna/story/resourceutilities/StorytellingResources.java` | 611 |
| `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/RenderContext.java` | 603 |
| `core/ast/src/main/java/org/lgna/project/ast/JavaType.java` | 600 |
| `core/ide/src/main/java/org/alice/stageide/StoryApiConfigurationManager.java` | 587 |
| `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/RenderTargetImp.java` | 584 |
| `core/ide/src/main/java/org/alice/ide/croquet/models/html/HtmlEncoder.java` | 580 |
| `core/croquet/src/main/java/org/lgna/croquet/views/AwtComponentView.java` | 575 |
| `core/ide/src/main/java/org/alice/stageide/properties/uicontroller/ModelSizePropertyController.java` | 561 |
| `core/story-api/src/main/java/org/lgna/ik/core/solver/Solver.java` | 557 |
| `core/story-api/src/main/java/Jama/SingularValueDecomposition.java` | 553 |
| `core/tweedle/src/main/java/org/alice/tweedle/unlinked/TweedleUnlinkedParser.java` | 548 |
| `core/ide/src/main/java/org/alice/ide/IDE.java` | 539 |
| `core/ide/src/main/java/org/alice/stageide/sceneeditor/interact/GlobalDragAdapter.java` | 534 |
| `core-nonfree/story-api-nonfree/src/main/java/edu/cmu/cs/dennisc/nebulous/Model.java` | 521 |
| `core/image-editor/src/main/java/org/alice/imageeditor/croquet/ImageEditorFrame.java` | 512 |
| `core/story-api/src/main/java/org/lgna/story/resourceutilities/ModelResourceInfo.java` | 510 |

Hotspot rows are not automatic refactor instructions. A hotspot is a blocker
only when its size prevents safe characterization, review, or focused
modernization work. Production refactors still require behavior
characterization before code moves.

## QA journey automation gaps

Journey status comes from:

```sh
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json
```

The validator returns the normalized scenario catalog. The scorecard groups
scenarios by `automationMode` and treats manual evidence and gated command
smokes as remaining evidence gaps.

| Automation mode | Count | Scorecard category |
| --- | ---: | --- |
| `xvfb-real-alice` | 1 | Automated real Alice journey |
| `gated-command-smoke` | 8 | Gated command smoke coverage |
| `manual-evidence-required` | 6 | Manual evidence gap |

Manual evidence gaps:

| Scenario | Workflow |
| --- | --- |
| `alice-desktop-export` | `export` |
| `alice-desktop-instructor-student-setup` | `instructor-student-setup` |
| `alice-desktop-open-load-save` | `open-load-save` |
| `alice-desktop-run-debug` | `run-debug` |
| `alice-desktop-save-load` | `save-load` |
| `alice-desktop-scene-creation` | `scene-creation` |

The Run/debug journey has a narrower executable evidence slice even while the
scenario remains `manual-evidence-required` in the catalog. When the desktop Run
evidence lane is enabled, it writes `desktop-run-execution.json`,
`desktop-run-runtime.log`, and `desktop-run-render-affordance.json` as evidence
that VM statement execution was observed and the onscreen render target was
structurally attached into the Run view hierarchy. That evidence reduces
Run-progress ambiguity, but it does not claim screenshot validation, pixel
rendering correctness, coordinate-based visual validation, or rendering-engine
correctness.

Gated command smoke gaps:

| Scenario | Workflow |
| --- | --- |
| `alice-desktop-exported-project-smoke` | `exported-project-smoke` |
| `alice-desktop-failure-path-smoke` | `failure-path-smoke` |
| `alice-desktop-future-ui-smoke` | `future-ui-smoke` |
| `alice-desktop-menu-action-smoke` | `menu-action-smoke` |
| `alice-desktop-netbeans-package-smoke` | `netbeans-package-smoke` |
| `alice-desktop-package-install-smoke` | `package-install-smoke` |
| `alice-desktop-project-io-smoke` | `project-io-smoke` |
| `alice-desktop-wizard-palette-completion-smoke` | `wizard-palette-completion-smoke` |

Gated command smokes are automation coverage, but they remain evidence gaps
when `ALICE_QA_RUN_GATED_SMOKES=1` has not been used or when the required
artifacts are unavailable in the local environment.

## Corpus gaps

The scorecard looks for a checked-in, LFS-independent corpus manifest at:

```text
docs/reference/modernization-corpus-manifest.json
```

| Corpus signal | State | Meaning |
| --- | --- | --- |
| LFS-independent corpus manifest | Present | Found 3 representative checked-in corpus manifest entries. |
| Git LFS payloads | Not required | The scorecard does not fetch or inspect large binary project files. |

Corpus coverage is representative manifest evidence only; it is not full historical archive coverage and does not depend on local LFS payload availability.

## Remaining blockers

| Blocker | Current state | Required movement |
| --- | --- | --- |
| Aggregate coverage measurement | Missing aggregate JaCoCo CSV | Run the no-Sims coverage lane and regenerate the scorecard. |
| Ratcheted module measurements | Missing module JaCoCo CSVs for 5 ratcheted modules in this checkout | Run the no-Sims coverage lane and confirm each ratcheted module still emits a report. |
| 70% target evidence | Not claimable | Produce aggregate measured coverage at or above 70.0% before marking the target met. |
| Production hotspots | 51 files over 500 lines | Characterize behavior first; refactor only protected hotspots in focused changes. |
| Manual QA journeys | 6 scenarios require manual evidence | Add stable automation or collect accepted manual evidence for each workflow. Run/debug has a non-coordinate execution/render-affordance artifact boundary, but it remains manual for claims beyond VM execution and structural render-target attachment. |
| Gated QA smokes | 8 smokes are gated by local prerequisites | Run with `ALICE_QA_RUN_GATED_SMOKES=1` where prerequisites exist, or attach equivalent CI evidence. |
| Corpus manifest | Present | Keep manifest entries mapped to representative modernization journeys. |

## Interpretation notes

Use the scorecard as an evidence index, not as a single pass/fail badge.

Coverage ratchets answer "what regressions does CI prevent today?" The 70%
target status answers "can the project honestly claim the mission target
today?" Those are different questions. A low aggregate ratchet can be healthy
when it matches measured coverage with margin, and the 70% target must remain
not met or not claimable until current aggregate data proves otherwise.

Missing coverage reports are explicit blockers because they prevent
measurement. They are not interpreted as zero coverage and are not hidden
behind stale values.

Hotspots identify review and characterization risk. They do not authorize
production refactors by themselves. Follow the protected hotspot rule in
[Coverage reporting and ratchets](./coverage-reporting.md) before moving
production behavior.

Journey gaps are derived from the outside-in QA scenario catalog, not from
prose summaries. A manual scenario remains a gap until there is accepted
evidence or a stable automation mode. A gated smoke remains partial coverage
until it runs in an environment with the required prerequisites.

Corpus gaps are intentionally conservative. The scorecard can run without Git
LFS, so representative corpus coverage must be described by a small checked-in
manifest rather than inferred from local binary payloads.
