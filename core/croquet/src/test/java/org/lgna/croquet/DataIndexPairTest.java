package org.lgna.croquet;

import org.lgna.croquet.data.MutableListData;
import org.junit.Before;
import org.junit.Test;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for {@link DataIndexPair} — package-private adapter that bridges
 * {@link org.lgna.croquet.data.ListData} and a mutable selection index
 * into the {@link ComboBoxModel} contract. Covers getSize, getElementAt,
 * getSelectedItem, setSelectedItem, and listener delegation.
 */
public class DataIndexPairTest {

  private MutableListData<String> data;
  private DataIndexPair<String, MutableListData<String>> pair;
  private int capturedIndex;

  @Before
  public void setUp() {
    data = new MutableListData<>(CroquetTestUtils.STRING_CODEC, new String[]{"alpha", "bravo", "charlie"});
    capturedIndex = -999;
    pair = new DataIndexPair<>(data, 1, idx -> capturedIndex = idx);
  }

  // ── getSize ───────────────────────────────────────────────────────

  @Test
  public void getSize_matchesDataItemCount() {
    assertEquals(3, pair.getSize());
  }

  @Test
  public void getSize_updatesAfterDataMutation() {
    data.internalAddItem("delta");
    assertEquals(4, pair.getSize());
  }

  // ── getElementAt ──────────────────────────────────────────────────

  @Test
  public void getElementAt_validIndex_returnsItem() {
    assertEquals("alpha", pair.getElementAt(0));
    assertEquals("bravo", pair.getElementAt(1));
    assertEquals("charlie", pair.getElementAt(2));
  }

  @Test
  public void getElementAt_negativeOne_returnsNull() {
    assertNull(pair.getElementAt(-1));
  }

  // ── getSelectedItem ───────────────────────────────────────────────

  @Test
  public void getSelectedItem_returnsItemAtIndex() {
    assertEquals("bravo", pair.getSelectedItem());
  }

  @Test
  public void getSelectedItem_negativeOneIndex_returnsNull() {
    DataIndexPair<String, MutableListData<String>> noSel =
        new DataIndexPair<>(data, -1, idx -> {});
    assertNull(noSel.getSelectedItem());
  }

  // ── setSelectedItem ───────────────────────────────────────────────

  @Test
  public void setSelectedItem_existingItem_invokesCallback() {
    pair.setSelectedItem("charlie");
    assertEquals(2, capturedIndex);
  }

  @Test
  public void setSelectedItem_missingItem_invokesCallbackWithNegativeOne() {
    pair.setSelectedItem("missing");
    assertEquals(-1, capturedIndex);
  }

  @Test
  public void setSelectedItem_null_invokesCallbackWithNegativeOne() {
    pair.setSelectedItem(null);
    assertEquals(-1, capturedIndex);
  }

  // ── index field directly accessible ───────────────────────────────

  @Test
  public void indexField_reflectsConstructorArg() {
    assertEquals(1, pair.index);
  }

  @Test
  public void dataField_reflectsConstructorArg() {
    assertSame(data, pair.data);
  }

  // ── ListDataListener delegation ───────────────────────────────────

  @Test
  public void addListDataListener_delegatesToData() {
    AtomicInteger fireCount = new AtomicInteger(0);
    ListDataListener listener = new ListDataListener() {
      @Override public void intervalAdded(ListDataEvent e) {}
      @Override public void intervalRemoved(ListDataEvent e) {}
      @Override public void contentsChanged(ListDataEvent e) {
        fireCount.incrementAndGet();
      }
    };
    pair.addListDataListener(listener);
    data.internalAddItem("delta");
    assertEquals(1, fireCount.get());
  }

  @Test
  public void removeListDataListener_delegatesToData() {
    AtomicInteger fireCount = new AtomicInteger(0);
    ListDataListener listener = new ListDataListener() {
      @Override public void intervalAdded(ListDataEvent e) {}
      @Override public void intervalRemoved(ListDataEvent e) {}
      @Override public void contentsChanged(ListDataEvent e) {
        fireCount.incrementAndGet();
      }
    };
    pair.addListDataListener(listener);
    pair.removeListDataListener(listener);
    data.internalAddItem("delta");
    assertEquals(0, fireCount.get());
  }

  // ── Edge: empty data ──────────────────────────────────────────────

  @Test
  public void emptyData_getSize_returnsZero() {
    MutableListData<String> empty = new MutableListData<>(CroquetTestUtils.STRING_CODEC);
    DataIndexPair<String, MutableListData<String>> emptyPair =
        new DataIndexPair<>(empty, -1, idx -> {});
    assertEquals(0, emptyPair.getSize());
  }

  @Test
  public void emptyData_getSelectedItem_returnsNull() {
    MutableListData<String> empty = new MutableListData<>(CroquetTestUtils.STRING_CODEC);
    DataIndexPair<String, MutableListData<String>> emptyPair =
        new DataIndexPair<>(empty, -1, idx -> {});
    assertNull(emptyPair.getSelectedItem());
  }
}
