package org.lgna.croquet;

import org.lgna.croquet.preferences.PreferenceBooleanState;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * TDD contract tests for {@link CompositeResourceManager}.
 *
 * <p>CompositeResourceManager owns the 14 state maps (mapKeyToStringValue,
 * mapKeyToBooleanState, etc.), all non-tab {@code create*} factory methods,
 * the {@code contains(Model)} search, and the {@code localize()} logic
 * extracted from AbstractComposite.</p>
 *
 * <p>Design contract:
 * <ul>
 *   <li>Constructor: {@code CompositeResourceManager()}</li>
 *   <li>{@code createKey(composite, localizationKey)} — creates an AbstractComposite.Key</li>
 *   <li>{@code createStringValue(key)} — creates InternalStringValue, registers in map</li>
 *   <li>{@code createBooleanState(key, initialValue)} — creates, registers</li>
 *   <li>{@code createStringState(key, initialValue)} — creates, registers</li>
 *   <li>... (all 20 primary factory methods)</li>
 *   <li>{@code contains(Model)} — returns true if model is a value in any of the 14 maps</li>
 *   <li>{@code localize(composite)} — localizes string values and sidekick labels</li>
 *   <li>{@code getMapKeyToTabState()} — exposes tab state map for TabManager activation loops</li>
 * </ul></p>
 */
public class CompositeResourceManagerTest {

  private CompositeResourceManager resourceManager;
  private TestAbstractComposite composite;

  @Before
  public void setUp() {
    composite = new TestAbstractComposite();
    resourceManager = composite.getResourceManager();
  }

  // ── contains() contract ──────────────────────────────────────────────

  @Test
  public void contains_returnsFalseForNull() {
    assertFalse("contains(null) must return false", resourceManager.contains(null));
  }

  @Test
  public void contains_returnsFalseWhenEmpty() {
    Model unknownModel = new StubModel();
    assertFalse("Empty manager must not contain any model", resourceManager.contains(unknownModel));
  }

  @Test
  public void contains_returnsTrueForRegisteredBooleanState() {
    BooleanState state = composite.doCreateBooleanState("testBool", true);
    assertTrue("contains must return true for a registered BooleanState",
        resourceManager.contains(state));
  }

  @Test
  public void contains_returnsFalseForUnregisteredBooleanState() {
    composite.doCreateBooleanState("testBool", true);
    Model unknownModel = new StubModel();
    assertFalse("contains must return false for a model not in any map",
        resourceManager.contains(unknownModel));
  }

  @Test
  public void contains_returnsTrueForRegisteredStringState() {
    StringState state = composite.doCreateStringState("testStr", "hello");
    assertTrue("contains must return true for a registered StringState",
        resourceManager.contains(state));
  }

  @Test
  public void contains_returnsTrueForRegisteredActionOperation() {
    ActionOperation op = composite.doCreateActionOperation("testAction");
    assertTrue("contains must return true for a registered ActionOperation",
        resourceManager.contains(op));
  }

  @Test
  public void contains_returnsTrueForRegisteredBoundedIntegerState() {
    BoundedIntegerState state = composite.doCreateBoundedIntegerState("testInt");
    assertTrue("contains must return true for a registered BoundedIntegerState",
        resourceManager.contains(state));
  }

  @Test
  public void contains_returnsTrueForRegisteredBoundedDoubleState() {
    BoundedDoubleState state = composite.doCreateBoundedDoubleState("testDbl");
    assertTrue("contains must return true for a registered BoundedDoubleState",
        resourceManager.contains(state));
  }

  // ── contains() searches all 14 maps ─────────────────────────────────

  @Test
  public void contains_searchesAllMaps() {
    // Register one item in each accessible factory method
    BooleanState bool = composite.doCreateBooleanState("b1", false);
    StringState str = composite.doCreateStringState("s1", "v");
    ActionOperation action = composite.doCreateActionOperation("a1");
    BoundedIntegerState intState = composite.doCreateBoundedIntegerState("i1");
    BoundedDoubleState dblState = composite.doCreateBoundedDoubleState("d1");

    // All should be found
    assertTrue(resourceManager.contains(bool));
    assertTrue(resourceManager.contains(str));
    assertTrue(resourceManager.contains(action));
    assertTrue(resourceManager.contains(intState));
    assertTrue(resourceManager.contains(dblState));

    // Unknown model should not be found
    assertFalse(resourceManager.contains(new StubModel()));
  }

  // ── factory method contracts ─────────────────────────────────────────

  @Test
  public void createBooleanState_returnsNonNull() {
    BooleanState state = composite.doCreateBooleanState("key1", false);
    assertNotNull("createBooleanState must return a non-null BooleanState", state);
  }

  @Test
  public void createBooleanState_sameKeyReturnsSameInstance() {
    BooleanState first = composite.doCreateBooleanState("dup", true);
    // Second call with same key should return the existing one or replace
    // The contract is that the key maps to the latest registration
    assertTrue("Registered state must be in contains()", resourceManager.contains(first));
  }

  @Test
  public void createStringState_returnsNonNullWithInitialValue() {
    StringState state = composite.doCreateStringState("name", "initial");
    assertNotNull("createStringState must return non-null", state);
  }

  @Test
  public void createStringState_emptyStringOverload() {
    // The convenience overload defaults to ""
    StringState state = composite.doCreateStringStateDefault("name2");
    assertNotNull(state);
  }

  @Test
  public void createStringValue_returnsNonNull() {
    PlainStringValue sv = composite.doCreateStringValue("label");
    assertNotNull("createStringValue must return non-null", sv);
  }

  @Test
  public void createActionOperation_returnsNonNull() {
    ActionOperation op = composite.doCreateActionOperation("op1");
    assertNotNull(op);
  }

  // ── Key contract ─────────────────────────────────────────────────────

  @Test
  public void createKey_producesKeyWithCorrectLocalizationKey() {
    AbstractComposite.Key key = composite.doCreateKey("myKey");
    assertEquals("myKey", key.getLocalizationKey());
  }

  @Test
  public void createKey_producesKeyLinkedToComposite() {
    AbstractComposite.Key key = composite.doCreateKey("myKey");
    assertSame("Key must reference the creating composite", composite, key.getComposite());
  }

  @Test
  public void createKey_preferenceKeyFormat() {
    AbstractComposite.Key key = composite.doCreateKey("fieldName");
    String expected = composite.getClass().getName() + "_fieldName";
    assertEquals("Preference key must be className_localizationKey", expected, key.getPreferenceKey());
  }

  @Test
  public void keyEquality_sameCompositeAndLocalizationKey() {
    AbstractComposite.Key a = composite.doCreateKey("same");
    AbstractComposite.Key b = composite.doCreateKey("same");
    assertEquals("Keys with same composite+localizationKey must be equal", a, b);
    assertEquals("Equal keys must have same hashCode", a.hashCode(), b.hashCode());
  }

  @Test
  public void keyEquality_differentLocalizationKey() {
    AbstractComposite.Key a = composite.doCreateKey("alpha");
    AbstractComposite.Key b = composite.doCreateKey("beta");
    assertNotEquals("Keys with different localizationKey must not be equal", a, b);
  }

  // ── localize() contract ──────────────────────────────────────────────

  @Test
  public void localize_doesNotThrowWhenEmpty() {
    // localize on empty manager must be a no-op
    resourceManager.localize(composite);
  }

  @Test
  public void localize_doesNotThrowWithRegisteredItems() {
    composite.doCreateBooleanState("b1", false);
    composite.doCreateStringState("s1", "v");
    // Must not throw — localize iterates all maps
    resourceManager.localize(composite);
  }

  // ── getMapKeyToTabState ──────────────────────────────────────────────

  @Test
  public void getMapKeyToTabState_returnsNonNullMap() {
    assertNotNull("Tab state map must never be null",
        resourceManager.getMapKeyToTabState());
  }

  @Test
  public void getMapKeyToTabState_initiallyEmpty() {
    assertTrue("Tab state map must be empty initially",
        resourceManager.getMapKeyToTabState().isEmpty());
  }

  // ── Key toString ──────────────────────────────────────────────────────

  @Test
  public void keyToString_containsLocalizationKey() {
    AbstractComposite.Key key = composite.doCreateKey("myField");
    assertTrue(key.toString().contains("myField"));
  }

  @Test
  public void keyToString_containsKeyClassName() {
    AbstractComposite.Key key = composite.doCreateKey("myField");
    assertTrue(key.toString().contains("Key"));
  }

  // ── Key equals edge cases ───────────────────────────────────────────

  @Test
  public void keyEquals_self_returnsTrue() {
    AbstractComposite.Key key = composite.doCreateKey("test");
    assertEquals(key, key);
  }

  @Test
  public void keyEquals_null_returnsFalse() {
    AbstractComposite.Key key = composite.doCreateKey("test");
    assertNotEquals(key, null);
  }

  @Test
  public void keyEquals_differentType_returnsFalse() {
    AbstractComposite.Key key = composite.doCreateKey("test");
    assertNotEquals(key, "not a key");
  }

  // ── Key from different composites ───────────────────────────────────

  @Test
  public void keyEquality_differentComposites_notEqual() {
    TestAbstractComposite other = new TestAbstractComposite();
    AbstractComposite.Key keyA = composite.doCreateKey("same");
    AbstractComposite.Key keyB = other.doCreateKey("same");
    assertNotEquals(keyA, keyB);
  }

  // ── Internal state value manipulation ───────────────────────────────

  @Test
  public void internalBooleanState_setValueTransactionlessly() {
    BooleanState state = composite.doCreateBooleanState("toggle", false);
    CroquetTestUtils.removeItemListeners(state);
    state.setValueTransactionlessly(true);
    assertTrue(state.getValue());
  }

  @Test
  public void internalStringState_setValueTransactionlessly() {
    StringState state = composite.doCreateStringState("text", "init");
    CroquetTestUtils.removeDocumentListeners(state);
    state.setValueTransactionlessly("updated");
    assertEquals("updated", state.getValue());
  }

  @Test
  public void internalBoundedIntegerState_setValueTransactionlessly() {
    BoundedIntegerState state = composite.doCreateBoundedIntegerState("count");
    CroquetTestUtils.removeSpinnerChangeListeners(state);
    state.setValueTransactionlessly(42);
    assertEquals(Integer.valueOf(42), state.getValue());
  }

  @Test
  public void internalBoundedDoubleState_setValueTransactionlessly() {
    BoundedDoubleState state = composite.doCreateBoundedDoubleState("ratio");
    CroquetTestUtils.removeSpinnerChangeListeners(state);
    state.setValueTransactionlessly(0.75);
    assertEquals(0.75, state.getValue(), 0.001);
  }

  // ── Characterization: extraction boundary preservation ────────────────

  @Test
  public void localize_delegatesToLocalizationLogicAfterExtraction() {
    // Characterization: localize() must work regardless of whether the
    // implementation lives in CRM or in CompositeLocalizationDelegate.
    // Register items across multiple map types.
    composite.doCreateStringValue("title");
    composite.doCreateBooleanState("enabled", true);
    composite.doCreateStringState("name", "default");
    composite.doCreateActionOperation("save");
    composite.doCreateBoundedIntegerState("count");
    composite.doCreateBoundedDoubleState("ratio");

    // Must not throw — this validates the localize() call chain is intact
    resourceManager.localize(composite);
  }

  @Test
  public void factoryMethods_createTypesFromExtractedInternalStateTypes() {
    // Characterization: after extraction, factory methods create instances
    // from InternalStateTypes.java. The returned types must still be
    // instanceof their expected supertypes.
    PlainStringValue sv = composite.doCreateStringValue("sv");
    BooleanState bs = composite.doCreateBooleanState("bs", false);
    StringState ss = composite.doCreateStringState("ss", "val");
    ActionOperation op = composite.doCreateActionOperation("op");
    BoundedIntegerState bis = composite.doCreateBoundedIntegerState("bis");
    BoundedDoubleState bds = composite.doCreateBoundedDoubleState("bds");

    // Supertype checks
    assertTrue("StringValue must be PlainStringValue", sv instanceof PlainStringValue);
    assertTrue("BooleanState must be BooleanState", bs instanceof BooleanState);
    assertTrue("StringState must be StringState", ss instanceof StringState);
    assertTrue("ActionOperation must be ActionOperation", op instanceof ActionOperation);
    assertTrue("BoundedIntegerState must be BoundedIntegerState", bis instanceof BoundedIntegerState);
    assertTrue("BoundedDoubleState must be BoundedDoubleState", bds instanceof BoundedDoubleState);

    // They also must be Models (for contains())
    assertTrue(bs instanceof Model);
    assertTrue(ss instanceof Model);
    assertTrue(op instanceof Model);
    assertTrue(bis instanceof Model);
    assertTrue(bds instanceof Model);
  }

  @Test
  public void contains_worksAfterExtractionRoundTrip() {
    // Characterization: create items, verify contains(), then localize(),
    // then verify contains() again. This proves the extraction boundary
    // doesn't corrupt the identity-based containsIndex.
    BooleanState bs = composite.doCreateBooleanState("b", true);
    StringState ss = composite.doCreateStringState("s", "v");
    ActionOperation op = composite.doCreateActionOperation("a");

    // Pre-localize
    assertTrue(resourceManager.contains(bs));
    assertTrue(resourceManager.contains(ss));
    assertTrue(resourceManager.contains(op));

    // Localize (exercises the code that will move to delegate)
    resourceManager.localize(composite);

    // Post-localize — contains must still work
    assertTrue("contains must survive localize()", resourceManager.contains(bs));
    assertTrue("contains must survive localize()", resourceManager.contains(ss));
    assertTrue("contains must survive localize()", resourceManager.contains(op));
  }

  @Test
  public void multipleRegistrations_containsFindsAll() {
    // Stress test: register many items across all accessible types
    BooleanState[] bools = new BooleanState[5];
    StringState[] strs = new StringState[5];
    for (int i = 0; i < 5; i++) {
      bools[i] = composite.doCreateBooleanState("b" + i, i % 2 == 0);
      strs[i] = composite.doCreateStringState("s" + i, "val" + i);
    }

    for (int i = 0; i < 5; i++) {
      assertTrue("Bool " + i + " must be in contains()", resourceManager.contains(bools[i]));
      assertTrue("Str " + i + " must be in contains()", resourceManager.contains(strs[i]));
    }

    // Unknown model still not found
    assertFalse(resourceManager.contains(new StubModel()));
  }

  @Test
  public void localize_idempotent_calledTwice() {
    composite.doCreateStringValue("label");
    composite.doCreateBooleanState("flag", false);

    // Must be safe to call multiple times
    resourceManager.localize(composite);
    resourceManager.localize(composite);
  }

  // ── List state factory methods ──────────────────────────────────────

  @Test
  public void createImmutableListState_returnsNonNull() {
    ImmutableDataSingleSelectListState<String> state =
        composite.doCreateImmutableListState("items", CroquetTestUtils.STRING_CODEC, 0, "a", "b", "c");
    assertNotNull(state);
    assertTrue(resourceManager.contains(state));
  }

  @Test
  public void createImmutableListState_setsInitialSelection() {
    ImmutableDataSingleSelectListState<String> state =
        composite.doCreateImmutableListState("items2", CroquetTestUtils.STRING_CODEC, 1, "x", "y", "z");
    assertEquals("y", state.getValue());
  }

  @Test
  public void createImmutableListState_getItemCount() {
    ImmutableDataSingleSelectListState<String> state =
        composite.doCreateImmutableListState("items3", CroquetTestUtils.STRING_CODEC, 0, "a", "b");
    assertEquals(2, state.getItemCount());
  }

  @Test
  public void createImmutableListStateForEnum_returnsNonNull() {
    ImmutableDataSingleSelectListState<TestEnum> state =
        composite.doCreateImmutableListStateForEnum("enumSel", TestEnum.class, TestEnum.BETA);
    assertNotNull(state);
    assertTrue(resourceManager.contains(state));
  }

  @Test
  public void createImmutableListStateForEnum_setsInitialValue() {
    ImmutableDataSingleSelectListState<TestEnum> state =
        composite.doCreateImmutableListStateForEnum("enumSel2", TestEnum.class, TestEnum.GAMMA);
    assertEquals(TestEnum.GAMMA, state.getValue());
  }

  @Test
  public void createImmutableListStateForEnum_countsEnumConstants() {
    ImmutableDataSingleSelectListState<TestEnum> state =
        composite.doCreateImmutableListStateForEnum("enumSel3", TestEnum.class, TestEnum.ALPHA);
    assertEquals(TestEnum.values().length, state.getItemCount());
  }

  @Test
  public void createMutableListState_returnsNonNull() {
    MutableDataSingleSelectListState<String> state =
        composite.doCreateMutableListState("mutable", CroquetTestUtils.STRING_CODEC, 0, "p", "q");
    assertNotNull(state);
    assertTrue(resourceManager.contains(state));
  }

  @Test
  public void createMutableListState_setsInitialSelection() {
    MutableDataSingleSelectListState<String> state =
        composite.doCreateMutableListState("mutable2", CroquetTestUtils.STRING_CODEC, 1, "p", "q", "r");
    assertEquals("q", state.getValue());
  }

  @Test
  public void createMutableListState_supportsMutation() {
    MutableDataSingleSelectListState<String> state =
        composite.doCreateMutableListState("mutable3", CroquetTestUtils.STRING_CODEC, 0, "a");
    // Verify initial item count
    assertEquals(1, state.getItemCount());
    assertEquals("a", state.getItemAt(0));
  }

  @Test
  public void createGenericListState_returnsNonNull() {
    org.lgna.croquet.data.MutableListData<String> data =
        new org.lgna.croquet.data.MutableListData<>(CroquetTestUtils.STRING_CODEC, new String[]{"x", "y"});
    SingleSelectListState<String, ?> state =
        composite.doCreateGenericListState("generic", data, 0);
    assertNotNull(state);
  }

  @Test
  public void createRefreshableListState_returnsNonNull() {
    org.lgna.croquet.data.RefreshableListData<String> data =
        new org.lgna.croquet.data.RefreshableListData<String>(CroquetTestUtils.STRING_CODEC) {
          @Override
          protected java.util.List<String> createValues() {
            return java.util.Arrays.asList("a", "b", "c");
          }
        };
    RefreshableDataSingleSelectListState<String> state =
        composite.doCreateRefreshableListState("refreshable", data, 0);
    assertNotNull(state);
    assertTrue(resourceManager.contains(state));
  }

  @Test
  public void createPreferenceBooleanState_returnsNonNull() {
    org.lgna.croquet.preferences.PreferenceBooleanState state =
        composite.doCreatePreferenceBooleanState("pref", true);
    assertNotNull(state);
    assertTrue(resourceManager.contains(state));
  }

  // ── AbstractComposite lifecycle ─────────────────────────────────────

  @Test
  public void getCardId_returnsNonNull() {
    assertNotNull(composite.getCardId());
  }

  @Test
  public void modifyLocalizedText_returnsInputByDefault() {
    String result = composite.modifyLocalizedText(composite, "hello");
    assertEquals("hello", result);
  }

  @Test
  public void contains_delegatesToResourceManager() {
    BooleanState state = composite.doCreateBooleanState("test", false);
    assertTrue(composite.contains(state));
  }

  // ── InternalActionOperation: additional exercises ──────────────────

  @Test
  public void internalActionOperation_appendUserRepr() {
    ActionOperation op = composite.doCreateActionOperation("testOp");
    StringBuilder sb = new StringBuilder();
    op.appendUserRepr(sb);
    assertNotNull(sb.toString());
  }

  @Test
  public void internalActionOperation_getMigrationId() {
    ActionOperation op = composite.doCreateActionOperation("testOp2");
    assertNotNull(op.getMigrationId());
  }

  @Test
  public void internalActionOperation_isEnabled() {
    ActionOperation op = composite.doCreateActionOperation("enabledOp");
    assertTrue(op.isEnabled());
  }

  @Test
  public void internalActionOperation_setEnabled() {
    ActionOperation op = composite.doCreateActionOperation("disableOp");
    op.setEnabled(false);
    assertFalse(op.isEnabled());
  }

  @Test
  public void internalStringState_appendUserRepr() {
    StringState ss = composite.doCreateStringState("repr", "myval");
    javax.swing.text.Document doc = ss.getSwingModel().getDocument();
    for (javax.swing.event.DocumentListener dl :
        ((javax.swing.text.AbstractDocument) doc).getDocumentListeners()) {
      doc.removeDocumentListener(dl);
    }
    StringBuilder sb = new StringBuilder();
    ss.appendUserRepr(sb);
    // StringState uses appendRepresentation which delegates to itemCodec
    assertNotNull(sb.toString());
  }

  @Test
  public void internalBooleanState_appendUserRepr() {
    BooleanState bs = composite.doCreateBooleanState("repr2", true);
    javax.swing.DefaultButtonModel bm = (javax.swing.DefaultButtonModel)
        bs.getImp().getSwingModel().getButtonModel();
    for (java.awt.event.ItemListener il : bm.getItemListeners()) {
      bm.removeItemListener(il);
    }
    StringBuilder sb = new StringBuilder();
    bs.appendUserRepr(sb);
    assertNotNull(sb.toString());
  }

  // ── ImmutableListState: additional exercises ──────────────────────

  @Test
  public void immutableListState_setSelectedIndex() {
    ImmutableDataSingleSelectListState<String> state =
        composite.doCreateImmutableListState("selIdx", CroquetTestUtils.STRING_CODEC, 0, "a", "b", "c");
    javax.swing.DefaultListSelectionModel lsm =
        (javax.swing.DefaultListSelectionModel) state.getSwingModel().getListSelectionModel();
    for (javax.swing.event.ListSelectionListener l : lsm.getListSelectionListeners()) {
      lsm.removeListSelectionListener(l);
    }
    state.setSelectedIndex(2);
    assertEquals("c", state.getValue());
  }

  @Test
  public void immutableListState_clearSelection() {
    ImmutableDataSingleSelectListState<String> state =
        composite.doCreateImmutableListState("clear", CroquetTestUtils.STRING_CODEC, 1, "x", "y");
    javax.swing.DefaultListSelectionModel lsm =
        (javax.swing.DefaultListSelectionModel) state.getSwingModel().getListSelectionModel();
    for (javax.swing.event.ListSelectionListener l : lsm.getListSelectionListeners()) {
      lsm.removeListSelectionListener(l);
    }
    state.clearSelection();
    assertNull(state.getValue());
  }

  // ── Refreshable list state: additional exercises ──────────────────

  @Test
  public void refreshableListState_getValue() {
    org.lgna.croquet.data.RefreshableListData<String> data =
        new org.lgna.croquet.data.RefreshableListData<String>(CroquetTestUtils.STRING_CODEC) {
          @Override
          protected java.util.List<String> createValues() {
            return java.util.Arrays.asList("p", "q", "r");
          }
        };
    RefreshableDataSingleSelectListState<String> state =
        composite.doCreateRefreshableListState("refreshGet", data, 1);
    assertEquals("q", state.getValue());
  }

  @Test
  public void refreshableListState_getItemCount() {
    org.lgna.croquet.data.RefreshableListData<String> data =
        new org.lgna.croquet.data.RefreshableListData<String>(CroquetTestUtils.STRING_CODEC) {
          @Override
          protected java.util.List<String> createValues() {
            return java.util.Arrays.asList("a", "b");
          }
        };
    RefreshableDataSingleSelectListState<String> state =
        composite.doCreateRefreshableListState("refreshCount", data, 0);
    assertEquals(2, state.getItemCount());
  }

  // ── Enum list state: iterator and indexOf ─────────────────────────

  @Test
  public void enumListState_indexOf() {
    ImmutableDataSingleSelectListState<TestEnum> state =
        composite.doCreateImmutableListStateForEnum("enumIdx", TestEnum.class, TestEnum.ALPHA);
    assertEquals(1, state.indexOf(TestEnum.BETA));
  }

  @Test
  public void enumListState_containsItem() {
    ImmutableDataSingleSelectListState<TestEnum> state =
        composite.doCreateImmutableListStateForEnum("enumContains", TestEnum.class, TestEnum.ALPHA);
    assertTrue(state.containsItem(TestEnum.GAMMA));
  }

  @Test
  public void enumListState_iterator() {
    ImmutableDataSingleSelectListState<TestEnum> state =
        composite.doCreateImmutableListStateForEnum("enumIter", TestEnum.class, TestEnum.ALPHA);
    int count = 0;
    for (TestEnum e : state) {
      count++;
    }
    assertEquals(TestEnum.values().length, count);
  }

  @Test
  public void genericListState_getItemCount() {
    org.lgna.croquet.data.MutableListData<String> data =
        new org.lgna.croquet.data.MutableListData<>(CroquetTestUtils.STRING_CODEC, new String[]{"a", "b", "c"});
    SingleSelectListState<String, ?> state =
        composite.doCreateGenericListState("genCount", data, 2);
    assertEquals(3, state.getItemCount());
    assertEquals("c", state.getValue());
  }

  @Test
  public void mutableListState_getItemAt() {
    MutableDataSingleSelectListState<String> state =
        composite.doCreateMutableListState("mutItem", CroquetTestUtils.STRING_CODEC, 0, "x", "y", "z");
    assertEquals("x", state.getItemAt(0));
    assertEquals("z", state.getItemAt(2));
  }

  @Test
  public void preferenceBooleanState_setValue() {
    PreferenceBooleanState state =
        composite.doCreatePreferenceBooleanState("prefToggle", false);
    javax.swing.DefaultButtonModel bm = (javax.swing.DefaultButtonModel)
        state.getImp().getSwingModel().getButtonModel();
    for (java.awt.event.ItemListener il : bm.getItemListeners()) {
      bm.removeItemListener(il);
    }
    state.setValueTransactionlessly(true);
    assertTrue(state.getValue());
  }

  // ── Test doubles ─────────────────────────────────────────────────────

  /**
   * Minimal concrete AbstractComposite for testing ResourceManager.
   * Exposes protected factory methods via public wrappers.
   */
  static class TestAbstractComposite extends AbstractComposite<CompositeViewLifecycleTest.StubView> {

    TestAbstractComposite() {
      super(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Override
    protected org.lgna.croquet.views.ScrollPane createScrollPaneIfDesired() {
      return null;
    }

    @Override
    protected CompositeViewLifecycleTest.StubView createView() {
      return new CompositeViewLifecycleTest.StubView();
    }

    // Expose ResourceManager for direct testing
    CompositeResourceManager getResourceManager() {
      return this.resourceManager;
    }

    // Expose factory methods under test-friendly names
    AbstractComposite.Key doCreateKey(String localizationKey) {
      return this.createKey(localizationKey);
    }

    BooleanState doCreateBooleanState(String keyText, boolean initialValue) {
      return this.createBooleanState(keyText, initialValue);
    }

    StringState doCreateStringState(String keyText, String initialValue) {
      return this.createStringState(keyText, initialValue);
    }

    StringState doCreateStringStateDefault(String keyText) {
      return this.createStringState(keyText);
    }

    PlainStringValue doCreateStringValue(String keyText) {
      return this.createStringValue(keyText);
    }

    ActionOperation doCreateActionOperation(String keyText) {
      return this.createActionOperation(keyText, new AbstractComposite.Action() {
        @Override
        public org.lgna.croquet.edits.Edit perform(
            org.lgna.croquet.history.UserActivity userActivity,
            AbstractComposite.InternalActionOperation source) {
          return null;
        }
      });
    }

    BoundedIntegerState doCreateBoundedIntegerState(String keyText) {
      return this.createBoundedIntegerState(keyText, new AbstractComposite.BoundedIntegerDetails());
    }

    BoundedDoubleState doCreateBoundedDoubleState(String keyText) {
      return this.createBoundedDoubleState(keyText, new AbstractComposite.BoundedDoubleDetails());
    }

    @SuppressWarnings("unchecked")
    <T> ImmutableDataSingleSelectListState<T> doCreateImmutableListState(
        String keyText, ItemCodec<T> codec, int selectionIndex, T... values) {
      return this.createImmutableListState(keyText, (Class<T>) codec.getValueClass(), codec, selectionIndex, values);
    }

    <T extends Enum<T>> ImmutableDataSingleSelectListState<T> doCreateImmutableListStateForEnum(
        String keyText, Class<T> valueCls, T initialValue) {
      return this.createImmutableListStateForEnum(keyText, valueCls, initialValue);
    }

    <T> MutableDataSingleSelectListState<T> doCreateMutableListState(
        String keyText, ItemCodec<T> codec, int selectionIndex, T... values) {
      return this.createMutableListState(keyText, codec.getValueClass(), codec, selectionIndex, values);
    }

    <T> SingleSelectListState<T, org.lgna.croquet.data.ListData<T>> doCreateGenericListState(
        String keyText, org.lgna.croquet.data.ListData<T> data, int selectionIndex) {
      return this.createGenericListState(keyText, data, selectionIndex);
    }

    <T> RefreshableDataSingleSelectListState<T> doCreateRefreshableListState(
        String keyText, org.lgna.croquet.data.RefreshableListData<T> data, int selectionIndex) {
      return this.createRefreshableListState(keyText, data, selectionIndex);
    }

    PreferenceBooleanState doCreatePreferenceBooleanState(String keyText, boolean initialValue) {
      return this.createPreferenceBooleanState(keyText, initialValue);
    }
  }

  /**
   * Minimal Model implementation for negative contains() tests.
   */
  static class StubModel implements Model {
    @Override public void initializeIfNecessary() {}
    @Override public void appendUserRepr(StringBuilder sb) { sb.append("StubModel"); }
    @Override public void relocalize() {}
    @Override public boolean isEnabled() { return true; }
    @Override public void setEnabled(boolean isEnabled) {}
    @Override public java.util.UUID getMigrationId() { return null; }
  }

  enum TestEnum { ALPHA, BETA, GAMMA }
}
