package org.alice.ide.croquet.models.html;

import org.junit.Before;
import org.junit.Test;
import org.lgna.croquet.views.SwingComponentView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Dimension;

import static org.junit.Assert.*;

public class SvgEncoderExtendedTest {

  private Document document;
  private Element parent;

  @Before
  public void setUp() throws Exception {
    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    parent = document.createElement("div");
    document.appendChild(parent);
  }

  @Test
  public void pushSvgCanAppendDirectlyToDocument() throws Exception {
    Document localDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    SvgEncoder encoder = new SvgEncoder(localDocument);

    encoder.pushSvg(localDocument, () -> {
    });

    assertNotNull(localDocument.getDocumentElement());
    assertEquals("svg", localDocument.getDocumentElement().getTagName());
  }

  @Test
  public void pushSvgRunsContentBeforeAppendingRootElement() {
    SvgEncoder encoder = new SvgEncoder(document);

    encoder.pushSvg(parent, () -> {
      assertNotNull(encoder.getActiveSVG());
      assertEquals(0, parent.getChildNodes().getLength());
    });

    assertEquals(1, parent.getChildNodes().getLength());
  }

  @Test
  public void addToSvgUsesPreferredComponentSize() {
    SvgEncoder encoder = new SvgEncoder(document);

    encoder.pushSvg(parent, () -> {
      invokeOnEdt(() -> encoder.addToSvg(new TestPanelView(80, 25)));
      Dimension size = encoder.getActiveSVG().getSVGCanvasSize();
      assertEquals(80, size.width);
      assertEquals(25, size.height);
    });
  }

  @Test
  public void addToSvgKeepsMaximumWidthAndHeightAcrossComponents() {
    SvgEncoder encoder = new SvgEncoder(document);

    encoder.pushSvg(parent, () -> {
      invokeOnEdt(() -> encoder.addToSvg(new TestPanelView(40, 90)));
      invokeOnEdt(() -> encoder.addToSvg(new TestPanelView(120, 30)));
      Dimension size = encoder.getActiveSVG().getSVGCanvasSize();
      assertEquals(120, size.width);
      assertEquals(90, size.height);
    });
  }

  @Test
  public void pushSvgCanBeUsedRepeatedlyAfterRenderingComponents() {
    SvgEncoder encoder = new SvgEncoder(document);

    encoder.pushSvg(parent, () -> invokeOnEdt(() -> encoder.addToSvg(new TestPanelView(10, 10))));
    encoder.pushSvg(parent, () -> invokeOnEdt(() -> encoder.addToSvg(new TestPanelView(20, 20))));

    assertEquals(2, parent.getChildNodes().getLength());
    assertNull(encoder.getActiveSVG());
  }

  private void invokeOnEdt(Runnable runnable) {
    try {
      SwingUtilities.invokeAndWait(runnable);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final class TestPanelView extends SwingComponentView<JPanel> {
    private final Dimension preferredSize;

    private TestPanelView(int width, int height) {
      this.preferredSize = new Dimension(width, height);
    }

    @Override
    protected JPanel createAwtComponent() {
      JPanel panel = new JPanel();
      panel.setPreferredSize(preferredSize);
      panel.setSize(preferredSize);
      return panel;
    }
  }
}
