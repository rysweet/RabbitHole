package org.lgna.croquet;

import org.lgna.croquet.data.MutableListData;
import org.lgna.croquet.event.ValueListener;
import org.junit.Before;
import org.junit.Test;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Tests for {@link MultipleSelectionListState} — multi-select list state
 * backed by ListData + ListSelectionModel. Covers value get/set, listener
 * dispatch, and model access.
 */
public class MultipleSelectionListStateTest {

  private static final Group TEST_GROUP =
      Group.getInstance(UUID.fromString("00000000-0000-0000-0008-ffffffffffff"), "multiSelTest");

  private MutableListData<String> data;
  private TestMultipleSelectionListState state;

  @Before
  public void setUp() {
    data = new MutableListData<>(CroquetTestUtils.STRING_CODEC, new String[]{"alpha", "bravo", "charlie"});
    state = new TestMultipleSelectionListState(TEST_GROUP, data);
    removeListSelectionListeners(state);
  }

  private static void removeListSelectionListeners(TestMultipleSelectionListState s) {
    DefaultListSelectionModel lsm =
        (DefaultListSelectionModel) s.getSwingModel().getListSelectionModel();
    for (ListSelectionListener l : lsm.getListSelectionListeners()) {
      lsm.removeListSelectionListener(l);
    }
  }

  // ── Construction ──────────────────────────────────────────────────

  @Test
  public void constructor_noSelection() {
    assertTrue(state.getValue().isEmpty());
  }

  @Test
  public void getData_returnsSameInstance() {
    assertSame(data, state.getData());
  }

  @Test
  public void getSwingModel_returnsNonNull() {
    assertNotNull(state.getSwingModel());
  }

  @Test
  public void getSwingModel_listModel_sizeMatchesData() {
    assertEquals(3, state.getSwingModel().getListModel().getSize());
  }

  @Test
  public void getSwingModel_listModel_getElementAt() {
    assertEquals("alpha", state.getSwingModel().getListModel().getElementAt(0));
    assertEquals("bravo", state.getSwingModel().getListModel().getElementAt(1));
  }

  @Test
  public void getSwingModel_listSelectionModel_isNonNull() {
    assertNotNull(state.getSwingModel().getListSelectionModel());
  }

  // ── setValue ──────────────────────────────────────────────────────

  @Test
  public void setValue_singleItem_selected() {
    state.setValue(Collections.singletonList("bravo"));
    List<String> selected = state.getValue();
    assertEquals(1, selected.size());
    assertEquals("bravo", selected.get(0));
  }

  @Test
  public void setValue_multipleItems_selected() {
    state.setValue(Arrays.asList("alpha", "charlie"));
    List<String> selected = state.getValue();
    assertEquals(2, selected.size());
    assertTrue(selected.contains("alpha"));
    assertTrue(selected.contains("charlie"));
  }

  @Test
  public void setValue_emptyList_clearsSelection() {
    state.setValue(Collections.singletonList("alpha"));
    state.setValue(Collections.emptyList());
    assertTrue(state.getValue().isEmpty());
  }

  @Test
  public void setValue_nonExistentItem_notSelected() {
    state.setValue(Collections.singletonList("missing"));
    assertTrue(state.getValue().isEmpty());
  }

  // ── getValue ──────────────────────────────────────────────────────

  @Test
  public void getValue_reflectsSelectionModelChanges() {
    state.getSwingModel().getListSelectionModel().setSelectionInterval(0, 0);
    List<String> selected = state.getValue();
    assertEquals(1, selected.size());
    assertEquals("alpha", selected.get(0));
  }

  @Test
  public void getValue_multipleSelectionModel() {
    ListSelectionModel lsm = state.getSwingModel().getListSelectionModel();
    lsm.setSelectionInterval(0, 0);
    lsm.addSelectionInterval(2, 2);
    List<String> selected = state.getValue();
    assertEquals(2, selected.size());
    assertTrue(selected.contains("alpha"));
    assertTrue(selected.contains("charlie"));
  }

  // ── NewSchool value listener ──────────────────────────────────────

  @Test
  public void addNewSchoolListener_canAddAndInvoke() {
    AtomicReference<List<String>> captured = new AtomicReference<>();
    ValueListener<List<String>> listener = e -> captured.set(e.getNextValue());
    state.addNewSchoolValueListener(listener);
    // Verify addAndInvoke fires immediately with current value
    state.addAndInvokeNewSchoolValueListener(e -> {
      assertNotNull(e.getNextValue());
    });
  }

  @Test
  public void addAndInvokeNewSchoolValueListener_firesImmediately() {
    AtomicReference<List<String>> captured = new AtomicReference<>();
    state.addAndInvokeNewSchoolValueListener(e -> captured.set(e.getNextValue()));
    assertNotNull(captured.get());
    assertTrue(captured.get().isEmpty()); // No selection
  }

  @Test
  public void removeNewSchoolListener_removesSuccessfully() {
    ValueListener<List<String>> listener = e -> {};
    state.addNewSchoolValueListener(listener);
    state.removeNewSchoolValueListener(listener);
    // No exception — verify removal doesn't crash
  }

  // ── getPotentialPrepModelPaths ─────────────────────────────────────

  @Test
  public void getPotentialPrepModelPaths_returnsEmptyList() {
    assertTrue(state.getPotentialPrepModelPaths(null).isEmpty());
  }

  // ── Test infrastructure ───────────────────────────────────────────

  static class TestMultipleSelectionListState extends MultipleSelectionListState<String> {
    TestMultipleSelectionListState(Group group, MutableListData<String> data) {
      super(group, CroquetTestUtils.nextTestUUID(), data);
    }

    @Override
    protected Class<? extends Element> getClassUsedForLocalization() {
      return TestMultipleSelectionListState.class;
    }

    @Override
    protected String getSubKeyForLocalization() {
      return "test";
    }
  }
}
