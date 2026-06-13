import importlib.util
import sys
import textwrap
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "inventory-forbidden-patterns.py"


@lru_cache(maxsize=1)
def load_inventory():
    spec = importlib.util.spec_from_file_location("inventory_forbidden_patterns", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class ForbiddenPatternInventoryTest(unittest.TestCase):
    def test_production_throwable_and_print_stack_trace_are_high_confidence(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/vm/ExceptionFormatter.java",
            textwrap.dedent(
                """\
                class ExceptionFormatter {
                  void format() {
                    try {
                      run();
                    } catch (Throwable t) {
                      t.printStackTrace();
                    }
                  }
                }
                """
            ),
        )

        high_confidence = {
            (finding.pattern, finding.severity, finding.owner_module, finding.disposition)
            for finding in findings
        }
        self.assertIn(
            ("broad-throwable-catch", "high", "core/ide", "candidate"),
            high_confidence,
        )
        self.assertIn(
            ("print-stack-trace", "high", "core/ide", "candidate"),
            high_confidence,
        )
        self.assertIn("vm-exception-formatting", findings[0].focus_areas)

    def test_log_and_continue_catch_blocks_are_separate_candidates(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/stageide/sceneeditor/SceneEditor.java",
            textwrap.dedent(
                """\
                class SceneEditor {
                  void open() {
                    try {
                      run();
                    } catch (Exception ex) {
                      logger.warning("continuing without scene", ex);
                      repairUi();
                    }
                  }
                }
                """
            ),
        )

        log_findings = [finding for finding in findings if finding.pattern == "log-and-continue"]
        self.assertEqual(1, len(log_findings))
        self.assertEqual("high", log_findings[0].severity)
        self.assertEqual(("scene-editor",), log_findings[0].focus_areas)

    def test_capital_logger_severe_catch_blocks_are_log_and_continue_candidates(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/story-api/src/main/java/org/lgna/story/resourceutilities/ResourceClassLoader.java",
            textwrap.dedent(
                """\
                class ResourceClassLoader {
                  void load() {
                    try {
                      readResource();
                    } catch (Exception ex) {
                      Logger.severe("resource missing", ex);
                    }
                  }
                }
                """
            ),
        )

        log_findings = [finding for finding in findings if finding.pattern == "log-and-continue"]
        self.assertEqual(1, len(log_findings))
        self.assertEqual("core/story-api", log_findings[0].owner_module)

    def test_conditional_flow_change_does_not_hide_log_and_continue_path(self) -> None:
        inventory = load_inventory()

        java_path = "core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java"
        braced_findings = inventory.scan_text(
            java_path,
            textwrap.dedent(
                """\
                class VirtualMachine {
                  void runBraced() {
                    try {
                      execute();
                    } catch (Exception ex) {
                      if (isForRunning) {
                        throw ex;
                      }
                      Logger.warning("continuing after failed statement", ex);
                    }
                  }
                }
                """
            ),
        )
        unbraced_findings = inventory.scan_text(
            java_path,
            textwrap.dedent(
                """\
                class VirtualMachine {
                  void runUnbraced() {
                    try {
                      execute();
                    } catch (Exception ex) {
                      if (isForRunning)
                        throw ex;
                      Logger.warning("continuing after failed statement", ex);
                    }
                  }
                }
                """
            ),
        )

        for findings in (braced_findings, unbraced_findings):
            with self.subTest(shape=findings[0].text if findings else "missing"):
                log_findings = [
                    finding for finding in findings if finding.pattern == "log-and-continue"
                ]
                self.assertEqual(1, len(log_findings))
                self.assertEqual("high", log_findings[0].severity)

    def test_unconditional_flow_change_suppresses_log_and_continue_path(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java",
            textwrap.dedent(
                """\
                class VirtualMachine {
                  void run() {
                    try {
                      execute();
                    } catch (Exception ex) {
                      Logger.warning("aborting after failed statement", ex);
                      throw ex;
                    }
                  }
                }
                """
            ),
        )

        self.assertEqual(
            [],
            [finding for finding in findings if finding.pattern == "log-and-continue"],
        )

    def test_full_conditional_flow_change_suppresses_log_and_continue_path(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/RendererNativeLibraryLoader.java",
            textwrap.dedent(
                """\
                class RendererNativeLibraryLoader {
                  boolean loadLibrary(boolean isIgnoringError) {
                    try {
                      load();
                    } catch (UnsatisfiedLinkError ule) {
                      if (isIgnoringError) {
                        return false;
                      } else {
                        System.err.println("jogl");
                        throw ule;
                      }
                    }
                    return true;
                  }
                }
                """
            ),
        )

        self.assertEqual(
            [],
            [finding for finding in findings if finding.pattern == "log-and-continue"],
        )

    def test_same_line_flow_change_suppresses_log_and_continue_path(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            "class ProjectLoader { void run() { try { load(); } catch (Exception ex) { Logger.warning(ex); return; } } }\n",
        )

        self.assertEqual(
            [],
            [finding for finding in findings if finding.pattern == "log-and-continue"],
        )

    def test_same_line_system_exit_suppresses_log_and_continue_path(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            "class ProjectLoader { void run() { try { load(); } catch (Exception ex) { Logger.warning(ex); System.exit(1); } } }\n",
        )

        self.assertEqual(
            [],
            [finding for finding in findings if finding.pattern == "log-and-continue"],
        )

    def test_nested_conditional_then_log_remains_log_and_continue_candidate(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            textwrap.dedent(
                """\
                class ProjectLoader {
                  void run() {
                    try {
                      load();
                    } catch (Exception ex) {
                      if (recoverable) {
                        if (headless) {
                          return;
                        } else {
                          throw ex;
                        }
                      }
                      Logger.warning(ex);
                      recover();
                    }
                  }
                }
                """
            ),
        )

        log_findings = [finding for finding in findings if finding.pattern == "log-and-continue"]
        self.assertEqual(1, len(log_findings))

    def test_log_then_full_conditional_exit_suppresses_log_and_continue_path(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            textwrap.dedent(
                """\
                class ProjectLoader {
                  void run() {
                    try {
                      load();
                    } catch (Exception ex) {
                      Logger.warning(ex);
                      if (recoverable) {
                        return;
                      } else {
                        throw ex;
                      }
                    }
                  }
                }
                """
            ),
        )

        self.assertEqual(
            [],
            [finding for finding in findings if finding.pattern == "log-and-continue"],
        )

    def test_line_comment_markers_inside_strings_are_preserved(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            'class ProjectLoader { void run() { try { load(); } catch (Exception ex) { Logger.warning("http://example.invalid/return"); return; } } }\n',
        )

        self.assertEqual(
            [],
            [finding for finding in findings if finding.pattern == "log-and-continue"],
        )

    def test_forbidden_patterns_inside_strings_are_ignored(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            'class ProjectLoader { String example = "catch (Throwable t) { t.printStackTrace(); }"; }\n',
        )

        self.assertEqual([], findings)

    def test_block_commented_java_examples_are_ignored(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            textwrap.dedent(
                """\
                class ProjectLoader {
                  /*
                   * try {
                   *   load();
                   * } catch (Throwable t) {
                   *   Logger.warning("example", t);
                   * }
                   */
                }
                """
            ),
        )

        self.assertEqual([], findings)

    def test_trailing_line_commented_java_examples_are_ignored(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            "class ProjectLoader { int n = 0; // catch (Throwable t) { t.printStackTrace(); } }\n",
        )

        self.assertEqual([], findings)

    def test_adjacent_catch_clauses_report_the_logging_catch_line(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            "\n\nclass ProjectLoader { void run() { try { load(); } catch (IOException ioe) { return; } catch (RuntimeException re) { Logger.warning(re); repair(); } } }\n",
        )

        log_findings = [finding for finding in findings if finding.pattern == "log-and-continue"]
        self.assertEqual(1, len(log_findings))
        self.assertIn("RuntimeException", log_findings[0].text)
        self.assertEqual(3, log_findings[0].line)

    def test_multiline_adjacent_catches_report_later_logging_catch(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            textwrap.dedent(
                """\
                class ProjectLoader {
                  void run() {
                    try {
                      load();
                    } catch (IOException ioe) {
                      return;
                    } catch (RuntimeException re) {
                      Logger.warning(re);
                      repair();
                    }
                  }
                }
                """
            ),
        )

        log_findings = [finding for finding in findings if finding.pattern == "log-and-continue"]
        self.assertEqual(1, len(log_findings))
        self.assertIn("RuntimeException", log_findings[0].text)
        self.assertEqual(7, log_findings[0].line)

    def test_same_line_adjacent_catch_collects_later_catch_body(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            textwrap.dedent(
                """\
                class ProjectLoader {
                  void run() {
                    try {
                      load();
                    } catch (IOException ioe) { return; } catch (RuntimeException re) {
                      Logger.warning(re);
                      repair();
                    }
                  }
                }
                """
            ),
        )

        log_findings = [finding for finding in findings if finding.pattern == "log-and-continue"]
        self.assertEqual(1, len(log_findings))
        self.assertIn("RuntimeException", log_findings[0].text)
        self.assertEqual(5, log_findings[0].line)

    def test_todo_markers_in_block_comments_remain_inventory_candidates(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            "class ProjectLoader { /* TODO remove legacy branch after characterization */ }\n",
        )

        todo_findings = [finding for finding in findings if finding.pattern == "todo-hack-marker"]
        self.assertEqual(1, len(todo_findings))
        self.assertEqual("medium", todo_findings[0].severity)

    def test_palette_todo_placeholders_are_false_positives(self) -> None:
        inventory = load_inventory()

        findings = [
            *inventory.scan_text(
                "netbeans/src/main/resources/org/alice/netbeans/palette/items/resources/Bundle.properties",
                "HINT_html-IFSTATEMENT = <html><pre>//TODO: Code goes here.</pre></html>\n",
            ),
            *inventory.scan_text(
                "netbeans/src/main/resources/org/alice/netbeans/palette/items/resources/Bundle_es.properties",
                "HINT_html-IFSTATEMENT = <html><pre>//TODO: El código va aquí.</pre></html>\n",
            ),
            *inventory.scan_text(
                "netbeans/src/main/resources/org/alice/netbeans/palette/items/resources/Bundle_en_CA.properties",
                "HINT_html-IFSTATEMENT = <html><pre>//|TODO: Code goes here.</pre></html>\n",
            ),
            *inventory.scan_text(
                "netbeans/src/main/resources/org/alice/netbeans/palette/items/resources/Bundle_zh_CN.properties",
                "HINT_html-IFSTATEMENT = <html><pre>//TODO：代码在这里。</pre></html>\n",
            ),
        ]

        self.assertEqual(4, len(findings))
        self.assertEqual({"todo-hack-marker"}, {finding.pattern for finding in findings})
        self.assertEqual({"false-positive"}, {finding.disposition for finding in findings})
        self.assertEqual({"none"}, {finding.severity for finding in findings})

    def test_real_todo_and_hack_markers_are_candidates(self) -> None:
        inventory = load_inventory()

        findings = inventory.scan_text(
            "core/ide/src/main/java/org/alice/ide/ProjectLoader.java",
            textwrap.dedent(
                """\
                class ProjectLoader {
                  // TODO remove stale migration branch after characterization.
                  // HACK preserve legacy fallback until project loading is split.
                }
                """
            ),
        )

        self.assertEqual(
            [("todo-hack-marker", "medium", "candidate")] * 2,
            [
                (finding.pattern, finding.severity, finding.disposition)
                for finding in findings
            ],
        )

    def test_markdown_output_separates_candidates_from_false_positives(self) -> None:
        inventory = load_inventory()
        findings = [
            *inventory.scan_text(
                "core/story-api-migration/src/main/java/org/lgna/project/migration/MigrationRegistry.java",
                "class MigrationRegistry { void x() { try { run(); } catch (Throwable t) { } } }\n",
            ),
            *inventory.scan_text(
                "netbeans/src/main/resources/org/alice/netbeans/palette/items/resources/Bundle.properties",
                "HINT_html-IFSTATEMENT = <html><pre>//TODO: Code goes here.</pre></html>\n",
            ),
            *inventory.scan_text(
                "docs/sceneeditor-example.md",
                "Example text only: catch (Throwable t) { sceneEditor.recover(); }\n",
            ),
        ]

        markdown = inventory.render_markdown(findings, limit=10)
        summary = inventory.inventory_document(findings)["summary"]

        self.assertEqual({"high": 1}, summary["bySeverity"])
        self.assertEqual({"broad-throwable-catch": 1}, summary["byPattern"])
        self.assertEqual(
            {"core/story-api-migration": 1},
            summary["byOwnerModule"],
        )
        self.assertEqual({"migration-registry": 1}, summary["byFocusArea"])
        self.assertNotIn("none", summary["bySeverity"])
        self.assertNotIn("netbeans", summary["byOwnerModule"])
        self.assertNotIn("scene-editor", summary["byFocusArea"])
        self.assertIn("## Candidate findings by severity", markdown)
        self.assertIn("## Candidate findings by owner module", markdown)
        self.assertIn("## High-confidence follow-up candidates", markdown)
        self.assertIn("## False positives", markdown)
        self.assertIn("core/story-api-migration", markdown)
        self.assertIn("NetBeans palette user-code placeholder", markdown)

    def test_default_root_resolves_from_repository_subdirectories(self) -> None:
        inventory = load_inventory()

        self.assertEqual(REPO_ROOT, inventory.resolve_repo_root(REPO_ROOT / "scripts"))


if __name__ == "__main__":
    unittest.main()
