package org.alice.ide.croquet.models.html;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;

import static org.junit.Assert.*;

public class HtmlEncoderAdditionalTest {

  @Test
  public void encodesSpecialHtmlCharacters() throws Exception {
    String value = "Rock & Roll <Stage> \"One\" 'A'";

    EncodedSpan encoded = addSpan(value);

    assertEquals(value, encoded.element.getTextContent());
    assertTrue(serialize(encoded.document).contains("Rock &amp; Roll &lt;Stage&gt; \"One\" 'A'"));
  }

  @Test
  public void encodesEmptyStrings() throws Exception {
    EncodedSpan encoded = addSpan("");

    assertEquals("", encoded.element.getTextContent());
  }

  @Test
  public void encodesNullInputsAsEmptyTextContent() throws Exception {
    EncodedSpan encoded = addSpan(null);

    assertEquals("", encoded.element.getTextContent());
  }

  @Test
  public void encodesStringsWithNewlines() throws Exception {
    String value = "Line 1\nLine 2";

    EncodedSpan encoded = addSpan(value);

    assertEquals(value, encoded.element.getTextContent());
  }

  @Test
  public void encodesLongStrings() throws Exception {
    String value = "VeryLongSceneName".repeat(20);

    EncodedSpan encoded = addSpan(value);
    String xml = serialize(encoded.document);

    assertEquals(value, encoded.element.getTextContent());
    assertTrue(xml.length() > value.length());
  }

  @Test
  public void encodesUnicodeCharacters() throws Exception {
    String value = "こんにちは世界🌟";

    EncodedSpan encoded = addSpan(value);

    assertEquals(value, encoded.element.getTextContent());
  }

  private EncodedSpan addSpan(String value) throws Exception {
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    HtmlEncoder encoder = new HtmlEncoder(document);
    Element root = document.createElement("div");
    document.appendChild(root);

    Field activeElementsField = HtmlEncoder.class.getDeclaredField("activeElements");
    activeElementsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Deque<Element> activeElements = (Deque<Element>) activeElementsField.get(encoder);
    activeElements.push(root);

    Method addSpan = HtmlEncoder.class.getDeclaredMethod("addSpan", String.class, String.class);
    addSpan.setAccessible(true);
    Element element = (Element) addSpan.invoke(encoder, "encoded-value", value);
    return new EncodedSpan(document, element);
  }

  private String serialize(Document document) throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }

  private record EncodedSpan(Document document, Element element) {
  }
}
