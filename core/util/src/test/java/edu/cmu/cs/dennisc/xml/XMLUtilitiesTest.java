package edu.cmu.cs.dennisc.xml;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for XMLUtilities — document creation, write/read round-trips,
 * child element queries, and whitespace node removal.
 */
public class XMLUtilitiesTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  // --- Document creation ---

  @Test
  public void createDocument_returnsNonNull() {
    Document doc = XMLUtilities.createDocument();
    assertNotNull(doc);
  }

  @Test
  public void createDocument_hasNoChildren() {
    Document doc = XMLUtilities.createDocument();
    assertFalse(doc.hasChildNodes());
  }

  // --- Write/Read round-trip via OutputStream/InputStream ---

  @Test
  public void writeRead_stream_roundTrip() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);
    root.setAttribute("name", "test");
    Element child = doc.createElement("child");
    child.setTextContent("hello");
    root.appendChild(child);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLUtilities.write(doc, baos);
    assertTrue(baos.size() > 0);

    Document parsed = XMLUtilities.read(new ByteArrayInputStream(baos.toByteArray()));
    assertNotNull(parsed);
    Element parsedRoot = parsed.getDocumentElement();
    assertEquals("root", parsedRoot.getTagName());
    assertEquals("test", parsedRoot.getAttribute("name"));
  }

  // --- Write/Read round-trip via File ---

  @Test
  public void writeRead_file_roundTrip() throws IOException {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("data");
    doc.appendChild(root);
    Element item = doc.createElement("item");
    item.setAttribute("id", "42");
    item.setTextContent("value");
    root.appendChild(item);

    File xmlFile = tempFolder.newFile("test.xml");
    XMLUtilities.write(doc, xmlFile);
    assertTrue(xmlFile.exists());
    assertTrue(xmlFile.length() > 0);

    Document parsed = XMLUtilities.read(xmlFile);
    assertNotNull(parsed);
    assertEquals("data", parsed.getDocumentElement().getTagName());
  }

  // --- Write/Read round-trip via String path ---

  @Test
  public void writeRead_path_roundTrip() throws IOException {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("config");
    doc.appendChild(root);

    String path = new File(tempFolder.getRoot(), "path-test.xml").getAbsolutePath();
    XMLUtilities.write(doc, path);
    assertTrue(new File(path).exists());

    Document parsed = XMLUtilities.read(path);
    assertNotNull(parsed);
    assertEquals("config", parsed.getDocumentElement().getTagName());
  }

  // --- getChildElementsByTagName ---

  @Test
  public void getChildElementsByTagName_findsMatching() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);
    for (int i = 0; i < 3; i++) {
      Element child = doc.createElement("item");
      child.setTextContent("item-" + i);
      root.appendChild(child);
    }
    Element other = doc.createElement("other");
    root.appendChild(other);

    List<Element> items = XMLUtilities.getChildElementsByTagName(root, "item");
    assertEquals(3, items.size());
  }

  @Test
  public void getChildElementsByTagName_noMatches() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);
    Element child = doc.createElement("child");
    root.appendChild(child);

    List<Element> items = XMLUtilities.getChildElementsByTagName(root, "nonexistent");
    assertNotNull(items);
    assertTrue(items.isEmpty());
  }

  @Test
  public void getChildElementsByTagName_directChildrenOnly() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);
    Element child = doc.createElement("level1");
    root.appendChild(child);
    Element grandchild = doc.createElement("target");
    child.appendChild(grandchild);
    Element directTarget = doc.createElement("target");
    root.appendChild(directTarget);

    List<Element> targets = XMLUtilities.getChildElementsByTagName(root, "target");
    // Should find only direct children, not grandchildren
    assertEquals(1, targets.size());
  }

  // --- getSingleChildElementByTagName ---

  @Test
  public void getSingleChildElementByTagName_findsOne() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);
    Element unique = doc.createElement("unique");
    unique.setTextContent("only-one");
    root.appendChild(unique);

    Element found = XMLUtilities.getSingleChildElementByTagName(root, "unique");
    assertNotNull(found);
    assertEquals("only-one", found.getTextContent());
  }

  @Test
  public void getSingleChildElementByTagName_noMatch_returnsNull() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);

    Element found = XMLUtilities.getSingleChildElementByTagName(root, "absent");
    assertNull(found);
  }

  // --- Complex document structure ---

  @Test
  public void complexDocument_roundTrip() throws IOException {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("project");
    root.setAttribute("version", "1.0");
    doc.appendChild(root);

    for (int i = 0; i < 5; i++) {
      Element module = doc.createElement("module");
      module.setAttribute("name", "module-" + i);
      Element desc = doc.createElement("description");
      desc.setTextContent("Description for module " + i);
      module.appendChild(desc);
      root.appendChild(module);
    }

    File xmlFile = tempFolder.newFile("complex.xml");
    XMLUtilities.write(doc, xmlFile);

    Document parsed = XMLUtilities.read(xmlFile);
    Element parsedRoot = parsed.getDocumentElement();
    assertEquals("1.0", parsedRoot.getAttribute("version"));

    List<Element> modules = XMLUtilities.getChildElementsByTagName(parsedRoot, "module");
    assertEquals(5, modules.size());
    assertEquals("module-0", modules.get(0).getAttribute("name"));

    Element desc = XMLUtilities.getSingleChildElementByTagName(modules.get(2), "description");
    assertNotNull(desc);
    assertEquals("Description for module 2", desc.getTextContent());
  }

  // --- Read from well-formed XML string ---

  @Test
  public void read_wellFormedXmlString() {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child attr=\"v\">text</child></root>";
    Document doc = XMLUtilities.read(new ByteArrayInputStream(xml.getBytes()));
    assertNotNull(doc);
    assertEquals("root", doc.getDocumentElement().getTagName());
    Element child = XMLUtilities.getSingleChildElementByTagName(doc.getDocumentElement(), "child");
    assertEquals("v", child.getAttribute("attr"));
    assertEquals("text", child.getTextContent());
  }

  // --- Write produces valid XML ---

  @Test
  public void write_producesValidXml() {
    Document doc = XMLUtilities.createDocument();
    Element root = doc.createElement("valid");
    doc.appendChild(root);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLUtilities.write(doc, baos);
    byte[] bytes = baos.toByteArray();
    assertTrue("XML output should not be empty", bytes.length > 0);
    // Verify it can be read back as valid XML
    Document parsed = XMLUtilities.read(new java.io.ByteArrayInputStream(bytes));
    assertNotNull(parsed);
    assertEquals("valid", parsed.getDocumentElement().getTagName());
  }

  // --- XXE attack prevention ---

  @Test(expected = RuntimeException.class)
  public void read_rejectsXxeExternalEntity() {
    String xxePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<!DOCTYPE foo [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>"
        + "<root>&xxe;</root>";
    XMLUtilities.read(new ByteArrayInputStream(xxePayload.getBytes()));
  }
}
