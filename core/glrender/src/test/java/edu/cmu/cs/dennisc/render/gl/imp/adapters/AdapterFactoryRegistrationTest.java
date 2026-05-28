package edu.cmu.cs.dennisc.render.gl.imp.adapters;



import edu.cmu.cs.dennisc.scenegraph.Background;
import edu.cmu.cs.dennisc.scenegraph.Element;
import edu.cmu.cs.dennisc.scenegraph.Layer;
import edu.cmu.cs.dennisc.scenegraph.OrthographicCamera;
import edu.cmu.cs.dennisc.scenegraph.Scene;
import edu.cmu.cs.dennisc.scenegraph.SymmetricPerspectiveCamera;
import edu.cmu.cs.dennisc.scenegraph.Transformable;
import edu.cmu.cs.dennisc.texture.BufferedImageTexture;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

/**
 * Tests for {@link AdapterFactory} registration and lookup logic.
 * Uses the public getAdapterFor methods. Verifies adapter creation, caching, 
 * and class mapping without GL context.
 */
public class AdapterFactoryRegistrationTest {



  private static BufferedImageTexture createTexture() {
    BufferedImageTexture t = new BufferedImageTexture();
    t.setBufferedImage(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB));
    return t;
  }

  // ── getAdapterFor: Scene ──────────────────────────────────────────

  @Test
  public void getAdapterForScene_returnsGlrScene() {
    Scene scene = new Scene();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) scene);
    assertNotNull(adapter);
    assertTrue(adapter instanceof GlrScene);
  }

  @Test
  public void getAdapterForScene_idempotent() {
    Scene scene = new Scene();
    GlrElement<?> a1 = AdapterFactory.getAdapterFor((Element) scene);
    GlrElement<?> a2 = AdapterFactory.getAdapterFor((Element) scene);
    assertSame(a1, a2);
  }

  // ── getAdapterFor: Transformable ──────────────────────────────────

  @Test
  public void getAdapterForTransformable_returnsGlrTransformable() {
    Transformable t = new Transformable();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) t);
    assertNotNull(adapter);
    assertTrue(adapter instanceof GlrTransformable);
  }

  @Test
  public void getAdapterForTransformable_sameInstance() {
    Transformable t = new Transformable();
    assertSame(AdapterFactory.getAdapterFor((Element) t), AdapterFactory.getAdapterFor((Element) t));
  }

  // ── getAdapterFor: Cameras ────────────────────────────────────────

  @Test
  public void getAdapterForSymmetricPerspectiveCamera_notNull() {
    SymmetricPerspectiveCamera cam = new SymmetricPerspectiveCamera();
    GlrAbstractCamera<?> adapter = AdapterFactory.getAdapterFor(cam);
    assertNotNull(adapter);
  }

  @Test
  public void getAdapterForOrthographicCamera_notNull() {
    OrthographicCamera cam = new OrthographicCamera();
    GlrAbstractCamera<?> adapter = AdapterFactory.getAdapterFor(cam);
    assertNotNull(adapter);
  }

  @Test
  public void getAdapterForCamera_cached() {
    SymmetricPerspectiveCamera cam = new SymmetricPerspectiveCamera();
    assertSame(AdapterFactory.getAdapterFor(cam), AdapterFactory.getAdapterFor(cam));
  }

  // ── getAdapterFor: TexturedAppearance ───────────────────────────────

  @Test
  public void getAdapterForTexturedAppearance_notNull() {
    edu.cmu.cs.dennisc.scenegraph.TexturedAppearance app = new edu.cmu.cs.dennisc.scenegraph.TexturedAppearance();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) app);
    assertNotNull(adapter);
  }

  // ── getAdapterFor: Background ─────────────────────────────────────

  @Test
  public void getAdapterForBackground_notNull() {
    Background bg = new Background();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) bg);
    assertNotNull(adapter);
  }

  // ── getAdapterFor: Layer ──────────────────────────────────────────

  @Test
  public void getAdapterForLayer_notNull() {
    Layer layer = new Layer();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) layer);
    assertNotNull(adapter);
  }

  // ── getAdapterFor: BufferedImageTexture ───────────────────────────

  @Test
  public void getAdapterForBufferedImageTexture_notNull() {
    BufferedImageTexture texture = createTexture();
    GlrObject<?> adapter = AdapterFactory.getAdapterFor(texture);
    assertNotNull(adapter);
  }

  @Test
  public void getAdapterForBufferedImageTexture_isGlrBufferedImageTexture() {
    BufferedImageTexture texture = createTexture();
    GlrObject<?> adapter = AdapterFactory.getAdapterFor(texture);
    assertTrue(adapter instanceof GlrBufferedImageTexture);
  }

  @Test
  public void getAdapterForBufferedImageTexture_cached() {
    BufferedImageTexture texture = createTexture();
    GlrObject<?> a1 = AdapterFactory.getAdapterFor(texture);
    GlrObject<?> a2 = AdapterFactory.getAdapterFor(texture);
    assertSame(a1, a2);
  }

  // ── Different elements get different adapters ─────────────────────

  @Test
  public void differentScenes_getDifferentAdapters() {
    Scene s1 = new Scene();
    Scene s2 = new Scene();
    GlrElement<?> a1 = AdapterFactory.getAdapterFor((Element) s1);
    GlrElement<?> a2 = AdapterFactory.getAdapterFor((Element) s2);
    assertNotSame(a1, a2);
  }

  @Test
  public void differentTypes_getDifferentAdapterClasses() {
    Scene scene = new Scene();
    Transformable trans = new Transformable();
    GlrElement<?> sceneAdapter = AdapterFactory.getAdapterFor((Element) scene);
    GlrElement<?> transAdapter = AdapterFactory.getAdapterFor((Element) trans);
    assertNotEquals(sceneAdapter.getClass(), transAdapter.getClass());
  }

  // ── register custom mapping ───────────────────────────────────────

  @Test
  public void register_customClass_noException() {
    AdapterFactory.register(BufferedImageTexture.class, GlrBufferedImageTexture::new);
  }

  // ── Multiple lookups ──────────────────────────────────────────────

  @Test
  public void manyAdapters_allUnique() {
    Scene s1 = new Scene();
    Scene s2 = new Scene();
    Transformable t1 = new Transformable();
    Transformable t2 = new Transformable();

    GlrElement<?> a1 = AdapterFactory.getAdapterFor((Element) s1);
    GlrElement<?> a2 = AdapterFactory.getAdapterFor((Element) s2);
    GlrElement<?> a3 = AdapterFactory.getAdapterFor((Element) t1);
    GlrElement<?> a4 = AdapterFactory.getAdapterFor((Element) t2);

    assertNotSame(a1, a2);
    assertNotSame(a3, a4);
    assertNotSame(a1, a3);
  }

  // ── Null element ──────────────────────────────────────────────────

  @Test
  public void getAdapterFor_nullElement_returnsNull() {
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) null);
    assertNull(adapter);
  }

  // ── Concurrent lookups ────────────────────────────────────────────

  @Test
  public void concurrentLookups_sameElement_returnSameAdapter() throws Exception {
    Scene scene = new Scene();
    GlrElement<?>[] results = new GlrElement<?>[10];
    Thread[] threads = new Thread[10];
    for (int i = 0; i < 10; i++) {
      final int idx = i;
      threads[i] = new Thread(() -> results[idx] = AdapterFactory.getAdapterFor((Element) scene));
      threads[i].start();
    }
    for (Thread t : threads) {
      t.join();
    }
    for (int i = 1; i < 10; i++) {
      assertSame("All threads should get same adapter", results[0], results[i]);
    }
  }

  // ── Adapter owner after lookup ────────────────────────────────────

  @Test
  public void adapter_hasOwner() {
    Scene scene = new Scene();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) scene);
    assertNotNull(adapter);
    assertNotNull(adapter.getOwner());
  }

  @Test
  public void adapter_ownerIsOriginalElement() {
    Scene scene = new Scene();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) scene);
    assertSame(scene, adapter.getOwner());
  }

  @Test
  public void transformableAdapter_ownerIsOriginalTransformable() {
    Transformable t = new Transformable();
    GlrElement<?> adapter = AdapterFactory.getAdapterFor((Element) t);
    assertSame(t, adapter.getOwner());
  }
}
