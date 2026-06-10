package org.lgna.croquet.data;

import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.CroquetTestUtils;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ListDataContractTest {
  private MutableListData<String> mutable;

  @Before
  public void setUp() {
    mutable = new MutableListData<>(CroquetTestUtils.STRING_CODEC);
  }

  @Test
  public void mutableListDataMaintainsOrderAcrossMutations() {
    mutable.internalAddItem("bravo");
    mutable.internalAddItem(0, "alpha");
    mutable.internalSetAllItems(Arrays.asList("delta", "echo", "foxtrot"));
    mutable.internalRemoveItem("echo");

    assertEquals(2, mutable.getItemCount());
    assertEquals("delta", mutable.getItemAt(0));
    assertEquals("foxtrot", mutable.getItemAt(1));
    assertTrue(mutable.contains("foxtrot"));
    assertFalse(mutable.contains("echo"));
    assertEquals(1, mutable.indexOf("foxtrot"));
    assertArrayEquals(new String[]{"delta", "foxtrot"}, mutable.toArray());
  }

  @Test
  public void mutableListDataReturnsNullForOutOfRangeAccess() {
    mutable.internalAddItem("alpha");

    assertNull(mutable.getItemAt(-1));
    assertNull(mutable.getItemAt(1));
  }

  @Test
  public void mutableListDataNotifiesContentsChangedForEachMutationUntilListenerRemoved() {
    AtomicInteger calls = new AtomicInteger();
    List<String> ranges = new ArrayList<>();
    ListDataListener listener = new TestListDataListener() {
      @Override
      public void contentsChanged(ListDataEvent e) {
        calls.incrementAndGet();
        ranges.add(e.getIndex0() + ":" + e.getIndex1());
      }
    };

    mutable.addListener(listener);
    mutable.internalAddItem("alpha");
    mutable.internalAddItem("bravo");
    mutable.internalRemoveItem("alpha");
    mutable.removeListener(listener);
    mutable.internalSetAllItems(Arrays.asList("charlie"));

    assertEquals(3, calls.get());
    assertEquals(Arrays.asList("0:0", "0:1", "0:0"), ranges);
  }

  @Test
  public void immutableListDataExposesFixedContents() {
    ImmutableListData<String> immutable =
        new ImmutableListData<>(CroquetTestUtils.STRING_CODEC, new String[]{"alpha", "bravo"});

    assertEquals(2, immutable.getItemCount());
    assertEquals("alpha", immutable.getItemAt(0));
    assertTrue(immutable.contains("bravo"));
    assertEquals(1, immutable.indexOf("bravo"));
    assertArrayEquals(new String[]{"alpha", "bravo"}, immutable.toArray());
  }

  @Test
  public void immutableListDataRejectsMutation() {
    ImmutableListData<String> immutable =
        new ImmutableListData<>(CroquetTestUtils.STRING_CODEC, new String[]{"alpha"});

    expectUnsupported(() -> immutable.internalAddItem("bravo"));
    expectUnsupported(() -> immutable.internalAddItem(0, "bravo"));
    expectUnsupported(() -> immutable.internalRemoveItem("alpha"));
    expectUnsupported(() -> immutable.internalSetAllItems(Arrays.asList("charlie")));
  }

  @Test
  public void listDataExposesCodecAndPreferenceKey() {
    assertSame(CroquetTestUtils.STRING_CODEC, mutable.getItemCodec());
    assertNotNull(mutable.getPreferenceKey());
    assertTrue(mutable.getPreferenceKey().contains("MutableListData"));
  }

  private static void expectUnsupported(Runnable action) {
    try {
      action.run();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
