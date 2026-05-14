package org.lgna.croquet;

import edu.cmu.cs.dennisc.java.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TDD contract tests for {@link CompositeTabManager}.
 *
 * <p>CompositeTabManager owns the subComposites list, registeredTabStates set,
 * and the tab/split/card activation loops extracted from AbstractComposite.
 * These tests define the expected behavioral contract; they will fail until
 * CompositeTabManager is implemented.</p>
 *
 * <p>Design contract:
 * <ul>
 *   <li>{@code registerSubComposite(C)} — adds to internal list, returns argument</li>
 *   <li>{@code unregisterSubComposite(Composite)} — removes from internal list</li>
 *   <li>{@code registerTabState(TabState)} — adds to registered tab states set</li>
 *   <li>{@code unregisterTabState(TabState)} — removes from registered tab states set</li>
 *   <li>{@code activateAll(view, mapTabStates)} — activates subComposites → map tab states → registered tab states</li>
 *   <li>{@code deactivateAll(view, mapTabStates)} — deactivates registered tab states → map tab states → subComposites (LIFO)</li>
 *   <li>SubComposite list is copy-on-write safe for concurrent iteration</li>
 * </ul></p>
 */
public class CompositeTabManagerTest {

  private CompositeTabManager manager;

  @Before
  public void setUp() {
    manager = new CompositeTabManager();
  }

  // ── subComposite registration ────────────────────────────────────────

  @Test
  public void newManager_hasNoSubComposites() {
    assertTrue("Fresh manager must have empty subComposites",
        manager.getSubComposites().isEmpty());
  }

  @Test
  public void registerSubComposite_returnsTheArgument() {
    RecordingComposite c = new RecordingComposite("A");
    Composite<?> returned = manager.registerSubComposite(c);
    assertSame("registerSubComposite must return the exact object passed in", c, returned);
  }

  @Test
  public void registerSubComposite_addsToSubCompositesList() {
    RecordingComposite c = new RecordingComposite("A");
    manager.registerSubComposite(c);
    assertEquals("After registration, subComposites must have one entry", 1,
        manager.getSubComposites().size());
    assertTrue("subComposites must contain the registered composite",
        manager.getSubComposites().contains(c));
  }

  @Test
  public void registerSubComposite_preservesInsertionOrder() {
    RecordingComposite a = new RecordingComposite("A");
    RecordingComposite b = new RecordingComposite("B");
    RecordingComposite c = new RecordingComposite("C");

    manager.registerSubComposite(a);
    manager.registerSubComposite(b);
    manager.registerSubComposite(c);

    List<Composite<?>> subs = manager.getSubComposites();
    assertEquals(3, subs.size());
    assertSame(a, subs.get(0));
    assertSame(b, subs.get(1));
    assertSame(c, subs.get(2));
  }

  @Test
  public void unregisterSubComposite_removesFromList() {
    RecordingComposite a = new RecordingComposite("A");
    RecordingComposite b = new RecordingComposite("B");
    manager.registerSubComposite(a);
    manager.registerSubComposite(b);

    manager.unregisterSubComposite(a);

    assertEquals(1, manager.getSubComposites().size());
    assertFalse(manager.getSubComposites().contains(a));
    assertTrue(manager.getSubComposites().contains(b));
  }

  @Test
  public void unregisterSubComposite_noOpForUnregistered() {
    RecordingComposite c = new RecordingComposite("X");
    manager.unregisterSubComposite(c); // must not throw
    assertTrue(manager.getSubComposites().isEmpty());
  }

  // ── tab state registration ───────────────────────────────────────────

  @Test
  public void registerTabState_addsToSet() {
    assertTrue("Fresh manager must have no registered tab states",
        manager.getRegisteredTabStates().isEmpty());

    RecordingTabState ts = new RecordingTabState("tab1");
    manager.registerTabState(ts);

    assertEquals(1, manager.getRegisteredTabStates().size());
    assertTrue(manager.getRegisteredTabStates().contains(ts));
  }

  @Test
  public void unregisterTabState_removesFromSet() {
    RecordingTabState ts = new RecordingTabState("tab1");
    manager.registerTabState(ts);
    manager.unregisterTabState(ts);
    assertTrue(manager.getRegisteredTabStates().isEmpty());
  }

  @Test
  public void registerTabState_duplicateIsNoOp() {
    RecordingTabState ts = new RecordingTabState("tab1");
    manager.registerTabState(ts);
    manager.registerTabState(ts); // duplicate
    assertEquals("Duplicate registration must not grow the set", 1,
        manager.getRegisteredTabStates().size());
  }

  // ── activation ordering ──────────────────────────────────────────────

  @Test
  public void activateAll_iteratesSubCompositesThenMapTabsThenRegisteredTabs() {
    List<String> activationOrder = Collections.synchronizedList(new ArrayList<>());

    RecordingComposite sub1 = new RecordingComposite("sub1", activationOrder);
    RecordingComposite sub2 = new RecordingComposite("sub2", activationOrder);
    manager.registerSubComposite(sub1);
    manager.registerSubComposite(sub2);

    RecordingTabState registered1 = new RecordingTabState("reg1", activationOrder);
    manager.registerTabState(registered1);

    // mapKeyToTabState entries simulate the map values iteration
    RecordingTabState mapTab1 = new RecordingTabState("map1", activationOrder);
    List<RecordingTabState> mapTabStates = Lists.newArrayList(mapTab1);

    manager.activateAll(mapTabStates);

    // Expected order: subComposites first, then map tab states, then registered tab states
    assertEquals("Activation must follow: subComposites → mapTabStates → registeredTabStates",
        List.of("sub1:preActivation", "sub2:preActivation",
            "map1:preActivation", "reg1:preActivation"),
        activationOrder);
  }

  @Test
  public void deactivateAll_iteratesRegisteredTabsThenMapTabsThenSubComposites() {
    List<String> deactivationOrder = Collections.synchronizedList(new ArrayList<>());

    RecordingComposite sub1 = new RecordingComposite("sub1", deactivationOrder);
    RecordingComposite sub2 = new RecordingComposite("sub2", deactivationOrder);
    manager.registerSubComposite(sub1);
    manager.registerSubComposite(sub2);

    RecordingTabState registered1 = new RecordingTabState("reg1", deactivationOrder);
    manager.registerTabState(registered1);

    RecordingTabState mapTab1 = new RecordingTabState("map1", deactivationOrder);
    List<RecordingTabState> mapTabStates = Lists.newArrayList(mapTab1);

    manager.deactivateAll(mapTabStates);

    // Expected LIFO order: registeredTabStates → mapTabStates → subComposites
    assertEquals("Deactivation must follow LIFO: registeredTabStates → mapTabStates → subComposites",
        List.of("reg1:postDeactivation", "map1:postDeactivation",
            "sub1:postDeactivation", "sub2:postDeactivation"),
        deactivationOrder);
  }

  @Test
  public void activateAll_withNoSubComposites_stillActivatesTabStates() {
    List<String> order = Collections.synchronizedList(new ArrayList<>());
    RecordingTabState mapTab = new RecordingTabState("map1", order);
    RecordingTabState regTab = new RecordingTabState("reg1", order);
    manager.registerTabState(regTab);

    manager.activateAll(Lists.newArrayList(mapTab));

    assertEquals(List.of("map1:preActivation", "reg1:preActivation"), order);
  }

  @Test
  public void deactivateAll_withNoTabStates_stillDeactivatesSubComposites() {
    List<String> order = Collections.synchronizedList(new ArrayList<>());
    RecordingComposite sub = new RecordingComposite("sub1", order);
    manager.registerSubComposite(sub);

    manager.deactivateAll(Lists.newArrayList());

    assertEquals(List.of("sub1:postDeactivation"), order);
  }

  @Test
  public void activateAll_emptyManager_isNoOp() {
    // Must not throw
    manager.activateAll(Lists.newArrayList());
  }

  @Test
  public void deactivateAll_emptyManager_isNoOp() {
    // Must not throw
    manager.deactivateAll(Lists.newArrayList());
  }

  // ── Test doubles ─────────────────────────────────────────────────────

  /**
   * Records handlePreActivation/handlePostDeactivation calls into a shared
   * trace list. Implements the Composite interface minimally.
   */
  static class RecordingComposite implements Composite<org.lgna.croquet.views.CompositeView<?, ?>> {
    private final String name;
    private final List<String> trace;

    RecordingComposite(String name) {
      this(name, new ArrayList<>());
    }

    RecordingComposite(String name, List<String> trace) {
      this.name = name;
      this.trace = trace;
    }

    @Override public void handlePreActivation() { trace.add(name + ":preActivation"); }
    @Override public void handlePostDeactivation() { trace.add(name + ":postDeactivation"); }
    @Override public java.util.UUID getCardId() { return null; }
    @Override public org.lgna.croquet.views.CompositeView<?, ?> getView() { return null; }
    @Override public org.lgna.croquet.views.ScrollPane getScrollPaneIfItExists() { return null; }
    @Override public org.lgna.croquet.views.SwingComponentView<?> getRootComponent() { return null; }
    @Override public void releaseView() {}
    @Override public boolean contains(Model model) { return false; }
    @Override public void initializeIfNecessary() {}
    @Override public void appendUserRepr(StringBuilder sb) { sb.append(name); }
  }

  /**
   * Minimal TabState double that records activation/deactivation events.
   * Since TabState is abstract with complex constructors, this test double
   * wraps the activation/deactivation tracking. The CompositeTabManager
   * will accept {@code Iterable<? extends TabState>} for its activation loop.
   *
   * Note: This is a simplified test double. In the actual implementation,
   * CompositeTabManager will iterate TabState instances and call their
   * handlePreActivation/handlePostDeactivation. This double tracks those calls.
   */
  static class RecordingTabState {
    private final String name;
    private final List<String> trace;

    RecordingTabState(String name) {
      this(name, new ArrayList<>());
    }

    RecordingTabState(String name, List<String> trace) {
      this.name = name;
      this.trace = trace;
    }

    public void handlePreActivation() { trace.add(name + ":preActivation"); }
    public void handlePostDeactivation() { trace.add(name + ":postDeactivation"); }

    String getName() { return name; }
  }
}
