package org.lgna.croquet;

import org.junit.Test;
import org.lgna.croquet.views.CompositeView;
import org.lgna.croquet.views.ScrollPane;
import org.lgna.croquet.views.SwingComponentView;

import javax.swing.JPanel;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * TDD contract tests for {@link CompositeViewLifecycle}.
 *
 * <p>CompositeViewLifecycle owns the view/scrollPane/cardId state that was
 * previously embedded in AbstractComposite. These tests define the expected
 * behavioral contract; they will fail until CompositeViewLifecycle is
 * implemented.</p>
 *
 * <p>Design contract:
 * <ul>
 *   <li>Constructor: {@code CompositeViewLifecycle(ScrollPane scrollPane)}</li>
 *   <li>{@code getCardId()} — synchronized lazy-init of a random UUID</li>
 *   <li>{@code initView(viewFactory)} — synchronized lazy-init; wires scrollPane viewport</li>
 *   <li>{@code peekView()} — returns current view or null (no creation)</li>
 *   <li>{@code getScrollPaneIfItExists()} — returns constructor-provided scrollPane</li>
 *   <li>{@code getRootComponent(viewFactory)} — returns scrollPane if present, else view</li>
 *   <li>{@code releaseView()} — nulls cached view for GC</li>
 * </ul></p>
 */
public class CompositeViewLifecycleTest {

  // ── cardId contract ──────────────────────────────────────────────────

  @Test
  public void getCardId_returnsNonNull() {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    assertNotNull("cardId must never be null", lifecycle.getCardId());
  }

  @Test
  public void getCardId_returnsValidUUID() {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    UUID id = lifecycle.getCardId();
    // Should be a type-4 random UUID
    assertEquals("UUID version must be 4", 4, id.version());
  }

  @Test
  public void getCardId_isIdempotent() {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    UUID first = lifecycle.getCardId();
    UUID second = lifecycle.getCardId();
    assertSame("Repeated calls must return the same UUID instance", first, second);
  }

  @Test
  public void getCardId_differentInstancesGetDifferentIds() {
    CompositeViewLifecycle<?> a = new CompositeViewLifecycle<>(null);
    CompositeViewLifecycle<?> b = new CompositeViewLifecycle<>(null);
    assertNotEquals("Separate instances must have distinct cardIds",
        a.getCardId(), b.getCardId());
  }

  // ── peekView contract ────────────────────────────────────────────────

  @Test
  public void peekView_returnsNullBeforeViewCreated() {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    assertNull("peekView must return null before any view creation", lifecycle.peekView());
  }

  // ── releaseView contract ─────────────────────────────────────────────

  @Test
  public void releaseView_clearsViewToNull() {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    lifecycle.releaseView();
    assertNull("After releaseView, peekView must return null", lifecycle.peekView());
  }

  // ── scrollPane passthrough ───────────────────────────────────────────

  @Test
  public void getScrollPaneIfItExists_returnsNullWhenNoneProvided() {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    assertNull(lifecycle.getScrollPaneIfItExists());
  }

  @Test
  public void getScrollPaneIfItExists_returnsSameInstanceFromConstructor() {
    ScrollPane sp = new ScrollPane();
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(sp);
    assertSame("Must return the exact ScrollPane passed to constructor", sp,
        lifecycle.getScrollPaneIfItExists());
  }

  // ── initView (lazy view factory) ─────────────────────────────────────

  @Test
  public void initView_createsViewOnFirstCall() {
    StubView view = new StubView();
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(null);

    StubView result = lifecycle.initView(() -> view);
    assertSame("First initView call must return the factory-created view", view, result);
  }

  @Test
  public void initView_returnsSameViewOnRepeatedCalls() {
    StubView view = new StubView();
    AtomicInteger callCount = new AtomicInteger(0);
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(null);

    StubView first = lifecycle.initView(() -> {
      callCount.incrementAndGet();
      return view;
    });
    StubView second = lifecycle.initView(() -> {
      callCount.incrementAndGet();
      return new StubView(); // different instance — should never be used
    });

    assertSame("Repeated calls must return the same cached view", first, second);
    assertEquals("Factory must be called exactly once", 1, callCount.get());
  }

  @Test
  public void initView_wiresScrollPaneViewport() {
    ScrollPane sp = new ScrollPane();
    StubView view = new StubView();
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(sp);

    lifecycle.initView(() -> view);

    // The scroll pane's JScrollPane viewport should now contain the view's AWT component
    assertNotNull("ScrollPane viewport must be wired after initView",
        sp.getAwtComponent().getViewport().getView());
  }

  @Test
  public void initView_skipsScrollPaneWiringWhenNull() {
    StubView view = new StubView();
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(null);

    // Should not throw when scrollPane is null
    StubView result = lifecycle.initView(() -> view);
    assertNotNull(result);
  }

  @Test
  public void peekView_returnsViewAfterInit() {
    StubView view = new StubView();
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(null);

    lifecycle.initView(() -> view);
    assertSame("peekView must return the initialized view", view, lifecycle.peekView());
  }

  // ── releaseView + re-creation ────────────────────────────────────────

  @Test
  public void releaseView_thenInitView_createsNewView() {
    StubView first = new StubView();
    StubView second = new StubView();
    AtomicInteger callCount = new AtomicInteger(0);
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(null);

    lifecycle.initView(() -> {
      return callCount.incrementAndGet() == 1 ? first : second;
    });
    assertSame(first, lifecycle.peekView());

    lifecycle.releaseView();
    assertNull("peekView must be null after release", lifecycle.peekView());

    StubView recreated = lifecycle.initView(() -> {
      callCount.incrementAndGet();
      return second;
    });
    assertSame("After release, initView must create a fresh view", second, recreated);
  }

  // ── getRootComponent ─────────────────────────────────────────────────

  @Test
  public void getRootComponent_returnsScrollPaneWhenPresent() {
    ScrollPane sp = new ScrollPane();
    StubView view = new StubView();
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(sp);

    SwingComponentView<?> root = lifecycle.getRootComponent(() -> view);
    assertSame("getRootComponent must return scrollPane when provided", sp, root);
  }

  @Test
  public void getRootComponent_returnsViewWhenNoScrollPane() {
    StubView view = new StubView();
    CompositeViewLifecycle<StubView> lifecycle = new CompositeViewLifecycle<>(null);

    SwingComponentView<?> root = lifecycle.getRootComponent(() -> view);
    assertSame("getRootComponent must return view when no scrollPane", view, root);
  }

  // ── thread safety ────────────────────────────────────────────────────

  @Test
  public void getCardId_isThreadSafe() throws InterruptedException {
    CompositeViewLifecycle<?> lifecycle = new CompositeViewLifecycle<>(null);
    int threadCount = 8;
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch go = new CountDownLatch(1);
    AtomicReference<UUID>[] results = new AtomicReference[threadCount];

    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      final int idx = i;
      results[idx] = new AtomicReference<>();
      threads[i] = new Thread(() -> {
        ready.countDown();
        try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        results[idx].set(lifecycle.getCardId());
      });
      threads[i].start();
    }

    ready.await();
    go.countDown();
    for (Thread t : threads) { t.join(5000); }

    UUID expected = results[0].get();
    for (int i = 1; i < threadCount; i++) {
      assertSame("All threads must see the same cardId instance", expected, results[i].get());
    }
  }

  // ── Test helper ──────────────────────────────────────────────────────

  /**
   * Minimal CompositeView implementation for testing.
   * Uses a plain JPanel as the underlying Swing component.
   */
  static class StubView extends CompositeView<JPanel, Composite<?>> {
    StubView() {
      super(null); // null composite is safe — just skips initializeIfNecessary
    }

    @Override
    protected JPanel createAwtComponent() {
      return new JPanel();
    }
  }
}
