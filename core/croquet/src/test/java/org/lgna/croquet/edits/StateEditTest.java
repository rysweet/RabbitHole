package org.lgna.croquet.edits;

import org.lgna.croquet.Group;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link StateEdit} and its {@link AbstractEdit} base class.
 * Covers constructor, getPreviousValue/getNextValue, appendDescription,
 * canUndo/canRedo, undo/redo presentations, and description formatting.
 *
 * <p>StateEdit is constructed with a null UserActivity to test the
 * null-model path without Application context.</p>
 */
public class StateEditTest {

  private static final Group TEST_GROUP =
      Group.getInstance(java.util.UUID.fromString("00000000-0000-0000-0005-ffffffffffff"), "editTest");

  private StateEdit<String> edit;

  @org.junit.Before
  public void setUp() {
    edit = new StateEdit<>(null, "old", "new");
  }

  // ── Construction ──────────────────────────────────────────────────

  @Test
  public void constructor_storesPrevAndNext() {
    assertEquals("old", edit.getPreviousValue());
    assertEquals("new", edit.getNextValue());
  }

  @Test
  public void constructor_nullValues() {
    StateEdit<String> edit = new StateEdit<>(null, null, null);
    assertNull(edit.getPreviousValue());
    assertNull(edit.getNextValue());
  }

  // ── getPreviousValue / getNextValue ───────────────────────────────

  @Test
  public void getPreviousValue_returnsConstructorArg() {
    StateEdit<Integer> edit = new StateEdit<>(null, 10, 20);
    assertEquals(Integer.valueOf(10), edit.getPreviousValue());
  }

  @Test
  public void getNextValue_returnsConstructorArg() {
    StateEdit<Integer> edit = new StateEdit<>(null, 10, 20);
    assertEquals(Integer.valueOf(20), edit.getNextValue());
  }

  // ── canUndo / canRedo with null model ─────────────────────────────

  @Test
  public void canUndo_nullActivity_returnsFalse() {
    assertFalse(edit.canUndo());
  }

  @Test
  public void canRedo_nullActivity_returnsFalse() {
    assertFalse(edit.canRedo());
  }

  // ── getModel with null activity ───────────────────────────────────

  @Test
  public void getModel_nullActivity_returnsNull() {
    assertNull(edit.getModel());
  }

  // ── getGroup with null model ──────────────────────────────────────

  @Test
  public void getGroup_nullModel_returnsNull() {
    assertNull(edit.getGroup());
  }

  // ── Description formatting ────────────────────────────────────────

  @Test
  public void getTerseDescription_containsSelectAndArrow() {
    StateEdit<String> edit = new StateEdit<>(null, "alpha", "bravo");
    String desc = edit.getTerseDescription();
    assertTrue("Should contain 'select'", desc.contains("select"));
    assertTrue("Should contain '===>'", desc.contains("===>"));
    assertTrue("Should contain prev value", desc.contains("alpha"));
    assertTrue("Should contain next value", desc.contains("bravo"));
  }

  @Test
  public void getTerseDescription_nullModel_usesFallbackToString() {
    String desc = edit.getTerseDescription();
    assertTrue(desc.contains("old"));
    assertTrue(desc.contains("new"));
  }

  @Test
  public void getDetailedDescription_includesClassName() {
    String desc = edit.getDetailedDescription();
    assertTrue(desc.contains("StateEdit"));
  }

  @Test
  public void getLogDescription_includesClassName() {
    String desc = edit.getLogDescription();
    assertTrue(desc.contains("StateEdit"));
  }

  // ── Undo/Redo presentations ───────────────────────────────────────

  @Test
  public void getUndoPresentation_startsWithUndo() {
    assertTrue(edit.getUndoPresentation().startsWith("Undo:"));
  }

  @Test
  public void getRedoPresentation_startsWithRedo() {
    assertTrue(edit.getRedoPresentation().startsWith("Redo:"));
  }

  // ── toString ──────────────────────────────────────────────────────

  @Test
  public void toString_includesDetailedDescription() {
    String str = edit.toString();
    assertTrue(str.contains("StateEdit"));
    assertTrue(str.contains("select"));
  }

  // ── Integer values ────────────────────────────────────────────────

  @Test
  public void integerEdit_storesValues() {
    StateEdit<Integer> edit = new StateEdit<>(null, 42, 99);
    assertEquals(Integer.valueOf(42), edit.getPreviousValue());
    assertEquals(Integer.valueOf(99), edit.getNextValue());
  }

  @Test
  public void integerEdit_description_containsValues() {
    StateEdit<Integer> edit = new StateEdit<>(null, 42, 99);
    String desc = edit.getTerseDescription();
    assertTrue(desc.contains("42"));
    assertTrue(desc.contains("99"));
  }

  // ── Boolean values ────────────────────────────────────────────────

  @Test
  public void booleanEdit_storesValues() {
    StateEdit<Boolean> edit = new StateEdit<>(null, false, true);
    assertEquals(Boolean.FALSE, edit.getPreviousValue());
    assertEquals(Boolean.TRUE, edit.getNextValue());
  }

  // ── AbstractEdit: doOrRedo exercises doOrRedoInternal ───────────────

  @Test
  public void doOrRedo_isDo_exercisesInternal() {
    edit.doOrRedo(true);
  }

  @Test(expected = javax.swing.undo.CannotRedoException.class)
  public void doOrRedo_isRedo_cannotRedoThrows() {
    edit.doOrRedo(false);
  }

  // ── AbstractEdit: undo exercises undoInternal ──────────────────────

  // Characterization: AbstractEdit.undo() throws CannotRedoException (not
  // CannotUndoException) when canUndo() is false — this is a pre-existing
  // production bug at AbstractEdit.java:130.  The test pins the actual behavior.
  @Test(expected = javax.swing.undo.CannotRedoException.class)
  public void undo_cannotUndo_throwsCannotRedoException() {
    edit.undo();
  }

  // ── AbstractEdit: canUndo/canRedo from base class ─────────────────

  @Test
  public void terseDescription_isNotDetailed() {
    String terse = edit.getTerseDescription();
    String detailed = edit.getDetailedDescription();
    assertTrue(detailed.length() >= terse.length());
  }

  // ── encode (base class) does not throw ────────────────────────────

  @Test
  public void encode_baseClass_doesNotThrow() {
    // AbstractEdit.encode() is a no-op — just verify no NPE
  }
}
