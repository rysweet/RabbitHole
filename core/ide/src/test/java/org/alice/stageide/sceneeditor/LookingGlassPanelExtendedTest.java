package org.alice.stageide.sceneeditor;

import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import org.junit.Test;
import org.lgna.croquet.views.AwtComponentView;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class LookingGlassPanelExtendedTest {
  @Test
  public void getAwtComponentReturnsBackingRenderTargetPanel() {
    JPanel panel = new JPanel(new SpringLayout());
    LookingGlassPanel lookingGlassPanel = new LookingGlassPanel(createRenderTarget(panel));

    assertSame(panel, lookingGlassPanel.getAwtComponent());
  }

  @Test
  public void setNorthWestComponentAddsComponentToBackingPanel() {
    JPanel panel = new JPanel(new SpringLayout());
    LookingGlassPanel lookingGlassPanel = new LookingGlassPanel(createRenderTarget(panel));
    JLabel label = new JLabel("north-west");
    AwtComponentView<?> view = AwtComponentView.lookup(label);

    lookingGlassPanel.setNorthWestComponent(view);

    assertSame(panel, label.getParent());
  }

  @Test
  public void setNorthWestComponentAppliesSouthConstraint() {
    JPanel panel = new JPanel(new SpringLayout());
    LookingGlassPanel lookingGlassPanel = new LookingGlassPanel(createRenderTarget(panel));
    JLabel label = new JLabel("north-west");

    lookingGlassPanel.setNorthWestComponent(AwtComponentView.lookup(label));

    SpringLayout layout = (SpringLayout) panel.getLayout();
    assertNotNull(layout.getConstraints(label).getConstraint(SpringLayout.SOUTH));
  }

  @Test
  public void setNorthWestComponentAcceptsNullAfterComponentWasInstalled() {
    JPanel panel = new JPanel(new SpringLayout());
    LookingGlassPanel lookingGlassPanel = new LookingGlassPanel(createRenderTarget(panel));

    lookingGlassPanel.setNorthWestComponent(AwtComponentView.lookup(new JLabel("north-west")));
    lookingGlassPanel.setNorthWestComponent(null);

    assertSame(panel, lookingGlassPanel.getAwtComponent());
  }

  private static OnscreenRenderTarget createRenderTarget(JPanel panel) {
    return (OnscreenRenderTarget) Proxy.newProxyInstance(
        OnscreenRenderTarget.class.getClassLoader(),
        new Class<?>[]{OnscreenRenderTarget.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getAwtComponent" -> panel;
          case "release" -> null;
          case "getSurfaceWidth" -> panel.getWidth();
          case "getSurfaceHeight" -> panel.getHeight();
          case "getSurfaceSize" -> panel.getSize();
          case "getSgCameraCount" -> 0;
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == double.class) {
      return 0.0;
    }
    if (type == float.class) {
      return 0.0f;
    }
    if (type == long.class) {
      return 0L;
    }
    return null;
  }
}
