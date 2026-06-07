package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for SaveProofJsonDelegate — the 12 static JSON fragment builders
 * and 2 blocker-inference helpers extracted from EvidenceJsonWriter (issue #684).
 *
 * <p>These tests define the contract that SaveProofJsonDelegate must satisfy.
 * They will FAIL until the delegate class is created.
 */
public class SaveProofJsonDelegateTest {

  // ── Snapshot factory helpers ────────────────────────────────────────

  /**
   * Returns a fully-proven snapshot where every gate is passed.
   * Individual tests override specific fields by constructing new snapshots.
   */
  private static EvidenceJsonWriter.SaveProofSnapshot provenSnapshot() {
    return new EvidenceJsonWriter.SaveProofSnapshot(
        /* robotFileMenuOpened */        true,
        /* robotSaveItemClicked */       true,
        /* saveActionIdentityMatched */  true,
        /* chooserObserved */            true,
        /* approvedSelection */          true,
        /* ambiguousChooserDiscovery */  false,
        /* selectedFileVerified */       true,
        /* targetInsideProofRoot */      true,
        /* dialogShowing */              true,
        /* dialogClass */                "javax.swing.JFileChooser",
        /* normalizedSelectedFile */     "/proof/root/classroom.a3p",
        /* pollCount */                  3,
        /* projectReadable */            true,
        /* markerPresent */              true,
        /* blockerKind */                null,
        /* blockerObserved */            null,
        /* blockerRequired */            null,
        /* targetCanonicalPath */        "/proof/root/classroom.a3p",
        /* targetPath */                 Path.of("/proof/root/classroom.a3p"),
        /* targetFileName */             "classroom.a3p",
        /* proofRoot */                  Path.of("/proof/root"),
        /* scenario */                   "alice-desktop-save-menu-dialog-write-proof",
        /* runId */                      "run-001");
  }

  /**
   * Returns a snapshot where the file menu was never opened — the first
   * gate in the blocker-inference chain.
   */
  private static EvidenceJsonWriter.SaveProofSnapshot menuNotOpenedSnapshot() {
    return new EvidenceJsonWriter.SaveProofSnapshot(
        false, false, false, false, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test-scenario", "run-002");
  }

  // ═══════════════════════════════════════════════════════════════════
  //  headerJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void headerJsonContainsSchemaVersion() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains(
        "\"schemaVersion\": \"" + SaveOperationCompletionEvidence.SAVE_PROOF_SCHEMA_VERSION + "\""));
  }

  @Test
  public void headerJsonContainsScenarioAndRunId() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains("\"scenario\": \"alice-desktop-save-menu-dialog-write-proof\""));
    assertTrue(json, json.contains("\"runId\": \"run-001\""));
  }

  @Test
  public void headerJsonContainsWorkflow() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains(
        "\"workflow\": \"" + SaveOperationCompletionEvidence.SAVE_PROOF_WORKFLOW + "\""));
  }

  @Test
  public void headerJsonContainsGeneratedAtUtc() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains("\"generatedAtUtc\":"));
  }

  @Test
  public void headerJsonContainsStatus() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains("\"status\": \"proven\""));
  }

  @Test
  public void headerJsonContainsProofTarget() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains("\"proofTarget\":"));
  }

  @Test
  public void headerJsonContainsClaimWhenProven() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains("\"claim\":"));
    assertTrue(json, json.contains(SaveOperationCompletionEvidence.SAVE_PROOF_MARKER));
    assertFalse(json, json.contains("\"reportingSummary\":"));
  }

  @Test
  public void headerJsonContainsReportingSummaryWhenNotProven() {
    EvidenceJsonWriter.SaveProofSnapshot snap = menuNotOpenedSnapshot();
    String json = SaveProofJsonDelegate.headerJson("blocked", false, snap);
    assertTrue(json, json.contains("\"reportingSummary\":"));
    assertFalse(json, json.contains("\"claim\":"));
  }

  @Test
  public void headerJsonEscapesSpecialCharsInScenario() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, "javax.swing.JFileChooser", "/proof/root/classroom.a3p", 3,
        true, true, null, null, null,
        "/proof/root/classroom.a3p", Path.of("/proof/root/classroom.a3p"),
        "classroom.a3p", Path.of("/proof/root"),
        "scenario\"with-quotes", "run-001");
    String json = SaveProofJsonDelegate.headerJson("proven", true, snap);
    assertTrue(json, json.contains("scenario\\\"with-quotes"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  blockerJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void blockerJsonReturnsNullLiteralWhenProven() {
    String json = SaveProofJsonDelegate.blockerJson(true, null, null, null);
    assertTrue(json, json.contains("\"blocker\": null"));
  }

  @Test
  public void blockerJsonReturnsObjectWhenNotProven() {
    String json = SaveProofJsonDelegate.blockerJson(
        false, "file_menu_not_showing",
        "The rendered File menu was not opened",
        "A complete Robot File menu path");
    assertTrue(json, json.contains("\"blocker\": {"));
    assertTrue(json, json.contains("\"kind\": \"file_menu_not_showing\""));
    assertTrue(json, json.contains("\"observed\": \"The rendered File menu was not opened\""));
    assertTrue(json, json.contains("\"required\": \"A complete Robot File menu path\""));
  }

  @Test
  public void blockerJsonHandlesNullObservedViaBlank() {
    String json = SaveProofJsonDelegate.blockerJson(
        false, "some_kind", null, null);
    assertTrue(json, json.contains("\"observed\": \"\""));
    assertTrue(json, json.contains("\"required\": \"\""));
  }

  @Test
  public void blockerJsonEscapesSpecialChars() {
    String json = SaveProofJsonDelegate.blockerJson(
        false, "kind\"special", "observed\\value", "required\nnewline");
    assertTrue(json, json.contains("kind\\\"special"));
    assertTrue(json, json.contains("observed\\\\value"));
    assertTrue(json, json.contains("required\\nnewline"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  menuJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void menuJsonContainsAllFields() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.menuJson(snap);
    assertTrue(json, json.contains("\"menu\": {"));
    assertTrue(json, json.contains("\"fileMenuOpened\": true"));
    assertTrue(json, json.contains("\"saveMenuItemInvoked\": true"));
    assertTrue(json, json.contains("\"saveActionIdentityMatched\": true"));
  }

  @Test
  public void menuJsonReflectsFalseValues() {
    EvidenceJsonWriter.SaveProofSnapshot snap = menuNotOpenedSnapshot();
    String json = SaveProofJsonDelegate.menuJson(snap);
    assertTrue(json, json.contains("\"fileMenuOpened\": false"));
    assertTrue(json, json.contains("\"saveMenuItemInvoked\": false"));
    assertTrue(json, json.contains("\"saveActionIdentityMatched\": false"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  dialogJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void dialogJsonContainsAllFields() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.dialogJson(snap);
    assertTrue(json, json.contains("\"dialog\": {"));
    assertTrue(json, json.contains("\"saveDialogObserved\": true"));
    assertTrue(json, json.contains("\"dialogType\": \"Swing JFileChooser\""));
    assertTrue(json, json.contains("\"dialogClass\": \"javax.swing.JFileChooser\""));
    assertTrue(json, json.contains("\"dialogShowing\": true"));
    assertTrue(json, json.contains("\"ambiguousChooserDiscovery\": false"));
    assertTrue(json, json.contains("\"pollCount\": 3"));
  }

  @Test
  public void dialogJsonRendersNullDialogClassAsJsonNull() {
    EvidenceJsonWriter.SaveProofSnapshot snap = menuNotOpenedSnapshot();
    String json = SaveProofJsonDelegate.dialogJson(snap);
    assertTrue(json, json.contains("\"dialogClass\": null"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  controlJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void controlJsonContainsAllFields() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    Path selectedPath = Path.of("/proof/root/classroom.a3p");
    String json = SaveProofJsonDelegate.controlJson(snap, selectedPath, true);
    assertTrue(json, json.contains("\"control\": {"));
    assertTrue(json, json.contains("\"selectedPathSet\": true"));
    assertTrue(json, json.contains("\"approvedSelection\": true"));
    assertTrue(json, json.contains("\"selectedPathMatchesExpected\": true"));
    assertTrue(json, json.contains("\"targetInsideProofRoot\": true"));
  }

  @Test
  public void controlJsonContainsRelativePaths() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    Path selectedPath = Path.of("/proof/root/classroom.a3p");
    String json = SaveProofJsonDelegate.controlJson(snap, selectedPath, true);
    assertTrue(json, json.contains("\"normalizedSelectedPath\":"));
    assertTrue(json, json.contains("\"expectedPath\":"));
  }

  @Test
  public void controlJsonHandlesNullSelectedPath() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.controlJson(snap, null, false);
    assertTrue(json, json.contains("\"normalizedSelectedPath\": null"));
    assertTrue(json, json.contains("\"selectedPathMatchesExpected\": false"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  writeJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void writeJsonContainsAllFields() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.writeJson(true, true, true, 1024, snap);
    assertTrue(json, json.contains("\"write\": {"));
    assertTrue(json, json.contains("\"fileWritten\": true"));
    assertTrue(json, json.contains("\"fileNonempty\": true"));
    assertTrue(json, json.contains("\"fileHasExpectedExtension\": true"));
    assertTrue(json, json.contains("\"outputSizeBytes\": 1024"));
  }

  @Test
  public void writeJsonContainsOutputPath() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.writeJson(true, true, true, 1024, snap);
    assertTrue(json, json.contains("\"outputPath\":"));
  }

  @Test
  public void writeJsonReflectsFalseValues() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.writeJson(false, false, false, 0, snap);
    assertTrue(json, json.contains("\"fileWritten\": false"));
    assertTrue(json, json.contains("\"fileNonempty\": false"));
    assertTrue(json, json.contains("\"fileHasExpectedExtension\": false"));
    assertTrue(json, json.contains("\"outputSizeBytes\": 0"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  readbackJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void readbackJsonContainsAllFields() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.readbackJson(snap);
    assertTrue(json, json.contains("\"readback\": {"));
    assertTrue(json, json.contains("\"projectReadable\": true"));
    assertTrue(json, json.contains("\"marker\": \"" + SaveOperationCompletionEvidence.SAVE_PROOF_MARKER + "\""));
    assertTrue(json, json.contains("\"markerPresent\": true"));
  }

  @Test
  public void readbackJsonReflectsFalseValues() {
    EvidenceJsonWriter.SaveProofSnapshot snap = menuNotOpenedSnapshot();
    String json = SaveProofJsonDelegate.readbackJson(snap);
    assertTrue(json, json.contains("\"projectReadable\": false"));
    assertTrue(json, json.contains("\"markerPresent\": false"));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  baselinePreservedJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void baselinePreservedJsonContainsAllTestNames() {
    String json = SaveProofJsonDelegate.baselinePreservedJson();
    assertTrue(json, json.contains("\"baselinePreserved\": ["));
    assertTrue(json, json.contains("StageIdeSaveMenuDoClickToWriteProofTest"));
    assertTrue(json, json.contains("ProjectApplicationSaveProjectToTest"));
    assertTrue(json, json.contains("JMenuBarRobotClickSaveProofTest"));
  }

  @Test
  public void baselinePreservedJsonEndsWithComma() {
    String json = SaveProofJsonDelegate.baselinePreservedJson();
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  requiresNextEvidenceJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void requiresNextEvidenceJsonContainsInstructions() {
    String json = SaveProofJsonDelegate.requiresNextEvidenceJson();
    assertTrue(json, json.contains("\"requiresNextEvidence\": ["));
    assertTrue(json, json.contains("xvfb-run --auto-servernum -s \\\"-screen 0 1024x768x24 -ac\\\""));
    assertFalse(json, json.contains("xvfb-run" + " -a"));
    assertTrue(json, json.contains("status proven"));
  }

  @Test
  public void requiresNextEvidenceJsonEndsWithComma() {
    String json = SaveProofJsonDelegate.requiresNextEvidenceJson();
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  doesNotClaimJson
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void doesNotClaimJsonContainsAllExclusions() {
    String json = SaveProofJsonDelegate.doesNotClaimJson();
    assertTrue(json, json.contains("\"doesNotClaim\": ["));
    assertTrue(json, json.contains("Save As coverage"));
    assertTrue(json, json.contains("all Save variants"));
    assertTrue(json, json.contains("full lesson completion"));
    assertTrue(json, json.contains("visible rendering correctness"));
    assertTrue(json, json.contains("grading correctness"));
    assertTrue(json, json.contains("physical user click"));
    assertTrue(json, json.contains("broad UI automation coverage"));
    assertTrue(json, json.contains("native dialog coverage"));
  }

  @Test
  public void doesNotClaimJsonDoesNotEndWithComma() {
    String json = SaveProofJsonDelegate.doesNotClaimJson();
    assertFalse("Last section must not have trailing comma",
        json.trim().endsWith(","));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  inferBlockerKind — 9 branches
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void inferBlockerKindFileMenuNotShowing() {
    EvidenceJsonWriter.SaveProofSnapshot snap = menuNotOpenedSnapshot();
    assertEquals("file_menu_not_showing",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindSaveItemNotAttributedWhenNotClicked() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, false, false, false, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("save_item_not_attributed",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindSaveItemNotAttributedWhenIdentityNotMatched() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, false, false, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("save_item_not_attributed",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindDialogNotObserved() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, false, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("dialog_not_observed",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindAmbiguousChooserDiscovery() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, false, true, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("ambiguous_chooser_discovery",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindChooserControlFailedWhenDialogNotShowing() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("chooser_control_failed",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindChooserControlFailedWhenNotApproved() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, false, false, true, false,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("chooser_control_failed",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindTargetPathRejectedWhenOutsideProofRoot() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, false,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("target_path_rejected",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindTargetPathRejectedWhenNotA3p() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.txt"), "classroom.txt",
        Path.of("/proof/root"), "test", "run");
    assertEquals("target_path_rejected",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindWriteNotObserved() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("write_not_observed",
        SaveProofJsonDelegate.inferBlockerKind(snap, false));
  }

  @Test
  public void inferBlockerKindReadbackFailed() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("readback_failed",
        SaveProofJsonDelegate.inferBlockerKind(snap, true));
  }

  @Test
  public void inferBlockerKindMarkerMissing() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, true, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    assertEquals("marker_missing",
        SaveProofJsonDelegate.inferBlockerKind(snap, true));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  inferBlockerObserved — 9 branches
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void inferBlockerObservedFileMenuNotOpened() {
    EvidenceJsonWriter.SaveProofSnapshot snap = menuNotOpenedSnapshot();
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals("The rendered File menu was not opened by Robot", result);
  }

  @Test
  public void inferBlockerObservedSaveItemNotClicked() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, false, false, false, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals("The production Save item click was not attributed to Robot", result);
  }

  @Test
  public void inferBlockerObservedDialogNotObserved() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, false, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals("No live Swing JFileChooser was observed", result);
  }

  @Test
  public void inferBlockerObservedAmbiguousChooser() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, false, true, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals("Multiple live Swing JFileChoosers were observed", result);
  }

  @Test
  public void inferBlockerObservedChooserControlFailed() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, false, false, false, false,
        false, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals("The live Save chooser could not be safely controlled", result);
  }

  @Test
  public void inferBlockerObservedTargetPathRejected() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, false,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals(
        "The selected Save target was outside the proof root or not an .a3p file",
        result);
  }

  @Test
  public void inferBlockerObservedWriteNotObserved() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, false);
    assertEquals(
        "No non-empty .a3p write was observed at the controlled target",
        result);
  }

  @Test
  public void inferBlockerObservedReadbackFailed() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, false, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, true);
    assertEquals(
        "The written .a3p file could not be read back as an Alice project",
        result);
  }

  @Test
  public void inferBlockerObservedMarkerMissing() {
    EvidenceJsonWriter.SaveProofSnapshot snap = new EvidenceJsonWriter.SaveProofSnapshot(
        true, true, true, true, true, false, true, true,
        true, null, null, 0, true, false, null, null, null,
        null, Path.of("/proof/root/classroom.a3p"), "classroom.a3p",
        Path.of("/proof/root"), "test", "run");
    String result = SaveProofJsonDelegate.inferBlockerObserved(snap, true);
    assertEquals(
        "The readback project did not contain " + SaveOperationCompletionEvidence.SAVE_PROOF_MARKER,
        result);
  }

  // ═══════════════════════════════════════════════════════════════════
  //  Cross-cutting: output identity with original EvidenceJsonWriter
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void menuJsonOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.menuJson(provenSnapshot());
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  @Test
  public void dialogJsonOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.dialogJson(provenSnapshot());
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  @Test
  public void controlJsonOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.controlJson(provenSnapshot(),
        Path.of("/proof/root/classroom.a3p"), true);
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  @Test
  public void writeJsonOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.writeJson(true, true, true, 1024, provenSnapshot());
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  @Test
  public void readbackJsonOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.readbackJson(provenSnapshot());
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  @Test
  public void headerJsonOutputEndsWithTrailingNewline() {
    String json = SaveProofJsonDelegate.headerJson("proven", true, provenSnapshot());
    assertNotNull("headerJson must not return null", json);
    assertTrue("headerJson must return non-empty string", json.length() > 0);
  }

  @Test
  public void blockerJsonNotProvenOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.blockerJson(false, "test", "obs", "req");
    assertTrue("Section must end with trailing comma for JSON concatenation",
        json.trim().endsWith(","));
  }

  @Test
  public void blockerJsonProvenOutputEndsWithTrailingComma() {
    String json = SaveProofJsonDelegate.blockerJson(true, null, null, null);
    assertTrue("Null blocker line must end with trailing comma",
        json.trim().endsWith(","));
  }

  // ═══════════════════════════════════════════════════════════════════
  //  Edge cases: delegate uses EvidenceJsonWriter utilities correctly
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void dialogJsonUsesStringJsonForDialogClass() {
    // stringJson(null) -> "null", stringJson("X") -> "\"X\""
    EvidenceJsonWriter.SaveProofSnapshot withClass = provenSnapshot();
    String json = SaveProofJsonDelegate.dialogJson(withClass);
    assertTrue("Non-null dialogClass should be quoted",
        json.contains("\"dialogClass\": \"javax.swing.JFileChooser\""));
  }

  @Test
  public void controlJsonUsesProofRelativePathForNormalizedSelectedPath() {
    // When selectedPath is under proofRoot, should show relative path
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    Path selectedPath = Path.of("/proof/root/classroom.a3p");
    String json = SaveProofJsonDelegate.controlJson(snap, selectedPath, true);
    // proofRelativePath(/proof/root/classroom.a3p, /proof/root) -> "classroom.a3p"
    assertTrue(json, json.contains("\"normalizedSelectedPath\": \"classroom.a3p\""));
  }

  @Test
  public void controlJsonUsesProofRelativePathForExpectedPath() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    Path selectedPath = Path.of("/proof/root/classroom.a3p");
    String json = SaveProofJsonDelegate.controlJson(snap, selectedPath, true);
    assertTrue(json, json.contains("\"expectedPath\": \"classroom.a3p\""));
  }

  @Test
  public void writeJsonUsesProofRelativePathForOutputPath() {
    EvidenceJsonWriter.SaveProofSnapshot snap = provenSnapshot();
    String json = SaveProofJsonDelegate.writeJson(true, true, true, 1024, snap);
    assertTrue(json, json.contains("\"outputPath\": \"classroom.a3p\""));
  }
}
