# core/croquet Test Suite

## Overview

The `core/croquet` module has a headless JUnit 4 test suite covering the
state-management and data-layer classes that form the backbone of the croquet
UI framework.  Tests run without an Alice `Application` context and without a
visible Swing display.

**Coverage target:** ≥ 20 % line coverage (up from 8.54 % baseline).

## Quick start

```bash
# From repository root — run only core/croquet tests
mvn -pl core/croquet test -Djava.awt.headless=true

# With JaCoCo coverage report
mvn -pl core/croquet verify -Djava.awt.headless=true
# open core/croquet/target/site/jacoco/index.html
```

## Test architecture

### Headless Swing isolation

Every croquet `State` subclass registers a Swing listener during construction
(e.g. `DocumentListener`, `ItemListener`, `ChangeListener`).  These listeners
route events through `Application.getActiveInstance()`, which is `null` in
unit tests.

The test suite removes these listeners in `@Before` so that
`setValueTransactionlessly()` can be called without triggering the
Application dependency chain.

| State class | Swing model | Listener type | Removal target |
|---|---|---|---|
| `StringState` | `PlainDocument` | `PausableDocumentListener` (implements `DocumentListener`) | `getSwingModel().getDocument()` |
| `BooleanState` | `ToggleButtonModel` | `ItemListener` | `getImp().getSwingModel().getButtonModel()` |
| `BoundedIntegerState` | `SpinnerNumberModel` | `ChangeListener` | `getSwingModel().getSpinnerModel()` |
| `BoundedDoubleState` | `SpinnerNumberModel` | `ChangeListener` | `getSwingModel().getSpinnerModel()` |
| `SingleSelectListState` | `ListSelectionModel` | `ListSelectionListener` | `getSwingModel().getListSelectionModel()` |

### Concrete test subclasses

Abstract classes (`StringState`, `BooleanState`, etc.) require two
localization methods:

```java
@Override protected Class<?> getClassUsedForLocalization() {
  return TestStringState.class;  // the test subclass itself
}

@Override protected String getSubKeyForLocalization() {
  return "test";
}
```

Each test file defines a `static` inner class implementing these methods.
All instances use `UUID.randomUUID()` to avoid `Group` registration collisions.

### Group instances

Tests obtain a `Group` via the static factory:

```java
private static final Group TEST_GROUP =
    Group.getInstance(UUID.randomUUID(), "test-string-state");
```

`Group.getInstance()` is safe to call outside an Application context.

## Test classes

### Data layer (no Swing dependencies)

#### `MutableListDataTest`
**Package:** `org.lgna.croquet.data`
**Source under test:** `MutableListData`, `AbstractMutableListData`

Tests the thread-safe `CopyOnWriteArrayList`-backed mutable list:

- Construction from array, collection, and empty
- `getItemAt()` with valid, negative, and out-of-bounds indices
- `getItemCount()`, `contains()`, `iterator()`
- `internalAddItem(T)` — append to end
- `internalAddItem(int, T)` — insert at position
- `internalRemoveItem(T)` — remove by value
- `internalSetAllItems(Collection)` — bulk replace
- `toArray()` — snapshot
- `indexOf()` — forward search
- Contents-changed listener notification via `ListDataListener` (add/remove via `addListener()`/`removeListener()`)

#### `ImmutableListDataTest`
**Package:** `org.lgna.croquet.data`
**Source under test:** `ImmutableListData`

Tests the fixed-content list variant:

- Construction from array
- `getItemAt()`, `getItemCount()`, `contains()`, `iterator()`
- Mutation methods (`internalAddItem`, `internalRemoveItem`, `internalSetAllItems`)
  throw `UnsupportedOperationException`
- `addListener()` / `removeListener()` are no-ops (immutable, no events)
- `indexOf()` correctness

#### `RefreshableListDataTest`
**Package:** `org.lgna.croquet.data`
**Source under test:** `RefreshableListData`

Tests the lazy-refresh caching list via a concrete test subclass that
overrides `createValues()` with a controllable supplier:

- Initial `getItemCount()` triggers `createValues()` (lazy init)
- Subsequent `getItemCount()` returns cached values (no re-creation)
- `refresh()` invalidates cache **and immediately** calls `createValues()`
- `getItemCount()` and `iterator()` call `refreshIfNecessary()`; `getItemAt()` does NOT
- Contents-changed events fire on `refresh()` only if data actually changed

### State classes (Swing listeners removed in setUp)

#### `StringStateTest`
**Package:** `org.lgna.croquet`
**Source under test:** `StringState`

- Construction with initial value (empty string, non-empty, null)
- `getValue()` / `setValueTransactionlessly()` round-trip
- `getSwingModel().getDocument()` text sync after `setValueTransactionlessly()`
- Codec `decodeValue()` / `encodeValue()` symmetry

#### `BooleanStateTest`
**Package:** `org.lgna.croquet`
**Source under test:** `BooleanState`

- Construction with `true` and `false` initial values
- `getValue()` / `setValueTransactionlessly()` round-trip
- `getImp().getSwingModel().getButtonModel().isSelected()` sync
- `isEnabled()` / `setEnabled()` delegation to button model
- `getTrueText()` / `getFalseText()` accessors
- `getTrueIcon()` / `getFalseIcon()` accessors (null by default)
- Codec round-trip

#### `BoundedIntegerStateTest`
**Package:** `org.lgna.croquet`
**Source under test:** `BoundedIntegerState`, `BoundedNumberState`

- Construction via `Details` builder: minimum, maximum, stepSize, initialValue
- `getValue()` returns initial value
- `setValueTransactionlessly()` clamps to [min, max]
- `getMinimum()` / `getMaximum()` getters
- `setMinimum()` / `setMaximum()` adjust spinner model
- `setValueTransactionlessly()` with boundary values (min, max, mid)
- `decodeValue()` / `encodeValue()` codec symmetry
- Spinner model sync: `getSwingModel().getSpinnerModel()` reflects state
- Step size configured via `Details` builder (no public `getStepSize()`/`setStepSize()` on state)

#### `BoundedDoubleStateTest`
**Package:** `org.lgna.croquet`
**Source under test:** `BoundedDoubleState`, `BoundedNumberState`

- Construction via `Details` builder
- Double-precision `getValue()` / `setValueTransactionlessly()`
- Boundary clamping at min/max
- Spinner model sync

### Edit and undo layer

#### `DataIndexPairTest`
**Package:** `org.lgna.croquet`
**Source under test:** `DataIndexPair` (package-private `ComboBoxModel` adapter)

Tests the `ComboBoxModel` adapter that bridges `ListData` + selection index to Swing:

- `getSize()` delegates to `data.getItemCount()`
- `getElementAt(int)` returns `data.getItemAt(index)`, null for `-1`
- `getSelectedItem()` returns item at current `index`, null when `-1`
- `setSelectedItem(Object)` delegates to `selectionIndexSetter` via `data.indexOf()`
- `addListDataListener()` / `removeListDataListener()` delegate to `data`
- Package-private `data` and `index` fields accessible from same-package tests

#### `StateEditTest`
**Package:** `org.lgna.croquet.edits`
**Source under test:** `StateEdit`, `AbstractEdit`

Tests the undo/redo edit object for State changes:

- `getPreviousValue()` / `getNextValue()` return constructor args
- `canUndo()` / `canRedo()` return `getModel() != null`
- With null `UserActivity` / model — returns `false` for both
- `appendDescription()` with null model produces safe output (no NPE)

#### `UndoHistoryTest`
**Package:** `org.lgna.croquet.undo`
**Source under test:** `UndoHistory`

Tests the undo stack independently of the Application:

- Construction via `Group.getInstance()`
- `push(Edit)` adds to stack; `getInsertionIndex()` increments
- `push()` only accepts edits whose `getGroup()` matches the history's group
- `performUndo()` / `performRedo()` adjust insertion index via `setInsertionIndex()`
- `setInsertionIndex(int)` clamps to [0, stack.size()] and fires index-change events
- Listener notification: `HistoryListener.operationPushing()`/`operationPushed()`
- Listener notification: `HistoryListener.insertionIndexChanging()`/`insertionIndexChanged()`
- `getGroup()` returns the construction group

> **Note:** `undo()` and `redo()` are private. There is no public `clear()` method.

## Common test patterns

### Listener removal helper

```java
// StringState — remove PausableDocumentListener (implements DocumentListener)
private static void removeDocumentListeners(StringState s) {
  Document doc = s.getSwingModel().getDocument();
  for (DocumentListener l : ((AbstractDocument) doc).getDocumentListeners()) {
    doc.removeDocumentListener(l);
  }
}

// BooleanState — remove ItemListeners
private static void removeItemListeners(BooleanState s) {
  ButtonModel bm = s.getImp().getSwingModel().getButtonModel();
  for (ItemListener l : bm.getItemListeners()) {
    bm.removeItemListener(l);
  }
}

// BoundedNumberState — remove ChangeListeners
private static void removeChangeListeners(BoundedNumberState<?> s) {
  SpinnerModel sm = s.getSwingModel().getSpinnerModel();
  for (ChangeListener l : ((AbstractSpinnerModel) sm).getChangeListeners()) {
    sm.removeChangeListener(l);
  }
}
```

### Concrete subclass stub

```java
static class TestStringState extends StringState {
  TestStringState(Group group, String initialValue) {
    super(group, UUID.randomUUID(), initialValue);
  }

  @Override protected Class<?> getClassUsedForLocalization() {
    return TestStringState.class;
  }

  @Override protected String getSubKeyForLocalization() {
    return "test";
  }
}
```

### ItemCodec for String

```java
private static final ItemCodec<String> STRING_CODEC = new ItemCodec<String>() {
  @Override public Class<String> getValueClass() { return String.class; }
  @Override public String decodeValue(BinaryDecoder d) { return d.decodeString(); }
  @Override public void encodeValue(BinaryEncoder e, String v) { e.encode(v); }
  @Override public void appendRepresentation(StringBuilder sb, String v) { sb.append(v); }
};
```

## Running a single test class

```bash
mvn -pl core/croquet test \
    -Dtest=org.lgna.croquet.StringStateTest \
    -Djava.awt.headless=true
```

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `NullPointerException` in `Application.getActiveInstance()` | Swing listener not removed in `@Before` | Add listener removal for the target State subclass |
| `HeadlessException` | Missing `-Djava.awt.headless=true` | Add to Maven Surefire or IDE run config |
| `Group already registered` | Two tests using the same UUID | Use `UUID.randomUUID()` for every State constructor |
| `ClassCastException` on spinner model | Wrong cast in listener removal | Cast to `AbstractSpinnerModel` for `getChangeListeners()` |

## Phase 2 — Issue #775 coverage push (30.4% → 50%)

Phase 2 adds 27 test files covering trigger classes, cascade runtime
internals, history steps, preferences, codec/icon helpers, and deeper
state/model/composite paths. All follow the same headless patterns above.

For the full test inventory, see the Phase 2 test packages listed below.

For running and troubleshooting instructions, see the Quick start section above.

### New test packages

| Package | Files | Focus |
| --- | --- | --- |
| `o.l.croquet.triggers` | 6 | All 21 trigger classes — data holders, no Application |
| `o.l.croquet.imp.cascade` | 6 | Cascade runtime tree: `RtNode`, `RtItem`, `RtBlank` |
| `o.l.croquet.history` | 5 | History step recording: `DragStep`, `MenuSelection` |
| `o.l.croquet.preferences` | 3 | Type-safe preference wrappers |
| `o.l.croquet.codecs/icon/data/meta` | 4 | Codec, icon factory, list data, meta-state |
| `o.l.croquet` (deep) | 6 | Edge cases: null values, Unicode, disabled state |
| `o.l.croquet.imp.*` (composite) | 4 | Wizard logic, menu-state binding, frame visibility |

### Trigger test pattern

```java
@Test
public void actionEventTrigger_getEvent_returnsWrappedEvent() {
  ActionEvent ae = new ActionEvent(new Object(), ActionEvent.ACTION_PERFORMED, "test");
  ActionEventTrigger trigger = new ActionEventTrigger(ae);
  assertSame(ae, trigger.getEvent());
}
```

Trigger classes are pure data holders — no listener removal needed.
