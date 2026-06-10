package org.lgna.croquet;

import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.event.ValueEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StateContractTest {
  private TestBooleanState state;

  @Before
  public void setUp() {
    Group testGroup = Group.getInstance(CroquetTestUtils.nextTestUUID(), "stateContract");
    state = new TestBooleanState(testGroup, false);
    CroquetTestUtils.removeItemListeners(state);
  }

  @Test
  public void transactionlessUpdateChangesCurrentValueAndSwingModel() {
    state.setValueTransactionlessly(true);

    assertTrue(state.getValue());
    assertTrue(state.getImp().getSwingModel().getButtonModel().isSelected());

    state.setValueTransactionlessly(false);

    assertFalse(state.getValue());
    assertFalse(state.getImp().getSwingModel().getButtonModel().isSelected());
  }

  @Test
  public void oldSchoolListenersSeeChangingBeforeChangedWithPreviousAndNextValues() {
    List<String> calls = new ArrayList<>();

    state.addValueListener(new State.ValueListener<Boolean>() {
      @Override
      public void changing(State<Boolean> source, Boolean previousValue, Boolean nextValue) {
        assertSame(state, source);
        calls.add("changing:" + previousValue + "->" + nextValue);
      }

      @Override
      public void changed(State<Boolean> source, Boolean previousValue, Boolean nextValue) {
        assertSame(state, source);
        calls.add("changed:" + previousValue + "->" + nextValue);
      }
    });

    state.setValueTransactionlessly(true);

    assertEquals(List.of("changing:false->true", "changed:false->true"), calls);
  }

  @Test
  public void newSchoolListenersReceivePreviousNextAndAdjustingFlag() {
    AtomicReference<ValueEvent<Boolean>> eventRef = new AtomicReference<>();

    state.addNewSchoolValueListener(eventRef::set);
    state.setValueTransactionlessly(true);

    ValueEvent<Boolean> event = eventRef.get();
    assertTrue(event.isPreviousValueValid());
    assertEquals(Boolean.FALSE, event.getPreviousValue());
    assertEquals(Boolean.TRUE, event.getNextValue());
    assertFalse(event.isAdjusting());
  }
}
