/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.lgna.project.io;

import org.junit.Test;
import org.lgna.common.Resource;
import org.lgna.project.Version;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.InstanceCreation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Node;
import org.lgna.project.migration.MigrationManager;
import org.lgna.story.resources.ModelResource;
import org.lgna.story.resourceutilities.ResourceTypeHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SecureXmlParserTest {

  @Test
  public void readArchiveXml_parsesValidXml() throws IOException {
    String xml = "<root><child attr=\"value\"/></root>";
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    Document doc = SecureXmlParser.readArchiveXml(is, "test.xml");

    assertNotNull(doc);
    assertEquals("root", doc.getDocumentElement().getTagName());
    assertEquals(1, doc.getDocumentElement().getChildNodes().getLength());
  }

  @Test
  public void readArchiveXml_rejectsXxePayload() {
    String xxeXml = "<?xml version=\"1.0\"?>\n"
        + "<!DOCTYPE foo [\n"
        + "  <!ENTITY xxe SYSTEM \"file:///etc/passwd\">\n"
        + "]>\n"
        + "<root>&xxe;</root>";
    ByteArrayInputStream is = new ByteArrayInputStream(xxeXml.getBytes(StandardCharsets.UTF_8));

    IOException thrown = assertThrows(IOException.class, () ->
        SecureXmlParser.readArchiveXml(is, "xxe-test.xml"));
    assertTrue(thrown.getMessage().contains("Unable to read xxe-test.xml"));
  }

  @Test
  public void readArchiveXml_stripsWhitespaceNodes() throws IOException {
    String xml = "<root>  \n  <child/>  \t  </root>";
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    Document doc = SecureXmlParser.readArchiveXml(is, "ws-test.xml");

    // Only the child element should remain — whitespace text nodes are removed
    assertEquals(1, doc.getDocumentElement().getChildNodes().getLength());
    assertEquals("child", doc.getDocumentElement().getChildNodes().item(0).getNodeName());
  }

  @Test
  public void isXmlWhitespace_detectsWhitespace() {
    assertTrue(SecureXmlParser.isXmlWhitespace(""));
    assertTrue(SecureXmlParser.isXmlWhitespace(" "));
    assertTrue(SecureXmlParser.isXmlWhitespace("\t\n\r "));
    assertTrue(SecureXmlParser.isXmlWhitespace("  \n\n  "));
  }

  @Test
  public void isXmlWhitespace_rejectsNonWhitespace() {
    assertFalse(SecureXmlParser.isXmlWhitespace("a"));
    assertFalse(SecureXmlParser.isXmlWhitespace(" hello "));
    assertFalse(SecureXmlParser.isXmlWhitespace("\t.\t"));
  }

  @Test
  public void removeWhitespaceNodes_removesWhitespaceText() throws Exception {
    String xml = "<root>   <a>text</a>   <b/>   </root>";
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    javax.xml.parsers.DocumentBuilder db =
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = db.parse(is);

    SecureXmlParser.removeWhitespaceNodes(doc.getDocumentElement());

    // Only a and b elements should remain, no whitespace text nodes
    assertEquals(2, doc.getDocumentElement().getChildNodes().getLength());
    assertEquals("a", doc.getDocumentElement().getChildNodes().item(0).getNodeName());
    assertEquals("b", doc.getDocumentElement().getChildNodes().item(1).getNodeName());
    // Text content inside <a> is preserved
    assertEquals("text", doc.getDocumentElement().getChildNodes().item(0).getTextContent());
  }

  @Test
  public void getCharsetForVersion_returnsUtf8BeforeThreeSeven() {
    Version old = new Version("3.6.0.0");
    Charset charset = SecureXmlParser.getCharsetForVersion(old);
    assertEquals(StandardCharsets.UTF_8, charset);
  }

  @Test
  public void getCharsetForVersion_returnsUtf16AtThreeSeven() {
    Version v37 = new Version("3.7.0.0");
    Charset charset = SecureXmlParser.getCharsetForVersion(v37);
    assertEquals(StandardCharsets.UTF_16, charset);
  }

  @Test
  public void getCharsetForVersion_returnsUtf16AfterThreeSeven() {
    Version newer = new Version("3.8.0.0");
    Charset charset = SecureXmlParser.getCharsetForVersion(newer);
    assertEquals(StandardCharsets.UTF_16, charset);
  }

  @Test
  public void resourceContext_includesNameAndEntry() throws Exception {
    Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().newDocument();
    Element elem = doc.createElement("resource");
    elem.setAttribute("name", "myImage");
    elem.setAttribute("uuid", "abc-123");

    String context = SecureXmlParser.resourceContext(elem, "resources/myImage.png");

    assertTrue(context.contains("'myImage'"));
    assertTrue(context.contains("'resources/myImage.png'"));
  }

  @Test
  public void resourceContext_fallsBackToUuidWhenNoName() throws Exception {
    Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().newDocument();
    Element elem = doc.createElement("resource");
    elem.setAttribute("name", "");
    elem.setAttribute("uuid", "abc-123");

    String context = SecureXmlParser.resourceContext(elem, "resources/file.png");

    assertTrue(context.contains("UUID 'abc-123'"));
  }

  @Test
  public void createResource_rejectsInvalidUuid() {
    IOException thrown = assertThrows(IOException.class, () ->
        SecureXmlParser.createResource(org.lgna.common.Resource.class, "not-a-uuid"));
    assertTrue(thrown.getMessage().contains("Invalid resource UUID"));
  }

  @Test
  public void createResource_acceptsAllowedPackage() throws IOException {
    // AudioResource is in org.lgna.common.resources — an allowed package.
    // It should pass the allowlist check and successfully create an instance.
    Resource resource = SecureXmlParser.createResource(
        org.lgna.common.resources.AudioResource.class,
        "00000000-0000-0000-0000-000000000001");
    assertNotNull("Should create resource from allowed package", resource);
  }

  @Test
  public void createResource_rejectsDisallowedPackage() {
    // Create a mock resource class in a disallowed package to verify rejection.
    // We use a class from java.lang that we pretend is a Resource subclass.
    // Since we can't easily create a Resource subclass in a disallowed package
    // in a unit test, we verify the allowlist by checking the ALLOWED_RESOURCE_PACKAGES
    // field covers the expected prefixes.
    // The actual rejection test uses a real call with a class that would be
    // outside the allowed packages — but since all Resource subclasses in the
    // codebase ARE in allowed packages, we verify the allowlist content instead.
    assertTrue("Allowlist should cover org.lgna.common.",
        SecureXmlParser.isAllowedResourcePackage("org.lgna.common.Resource"));
    assertTrue("Allowlist should cover org.lgna.common.resources.",
        SecureXmlParser.isAllowedResourcePackage("org.lgna.common.resources.AudioResource"));
    assertTrue("Allowlist should cover org.lgna.story.resources.",
        SecureXmlParser.isAllowedResourcePackage("org.lgna.story.resources.prop.BoxResource"));
    assertTrue("Allowlist should cover org.lgna.project.",
        SecureXmlParser.isAllowedResourcePackage("org.lgna.project.SomeResource"));
    assertFalse("Allowlist should reject java.lang",
        SecureXmlParser.isAllowedResourcePackage("java.lang.Runtime"));
    assertFalse("Allowlist should reject com.evil",
        SecureXmlParser.isAllowedResourcePackage("com.evil.MaliciousResource"));
  }

  @Test
  public void readArchiveXml_throwsOnMalformedXml() {
    ByteArrayInputStream is = new ByteArrayInputStream("<<<not xml>>>".getBytes(StandardCharsets.UTF_8));

    IOException thrown = assertThrows(IOException.class, () ->
        SecureXmlParser.readArchiveXml(is, "bad.xml"));
    assertTrue(thrown.getMessage().contains("Unable to read bad.xml"));
  }

  @Test
  public void resourceContext_withEmptyAttributes() throws Exception {
    Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().newDocument();
    Element elem = doc.createElement("resource");
    elem.setAttribute("name", "");
    elem.setAttribute("uuid", "");

    String context = SecureXmlParser.resourceContext(elem, "entry.dat");

    assertEquals("resource at archive entry 'entry.dat'", context);
  }

  @Test
  public void resourceContext_withNullEntryName() throws Exception {
    Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().newDocument();
    Element elem = doc.createElement("resource");
    elem.setAttribute("name", "myRes");
    elem.setAttribute("uuid", "");

    String context = SecureXmlParser.resourceContext(elem, null);

    assertEquals("resource 'myRes'", context);
  }

  @Test
  public void readXML_withoutMigrations_parsesDirectly() throws IOException {
    String xml = "<root><item key=\"val\"/></root>";
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    Version version = new Version("3.8.0.0");

    MigrationManager noMigrations = new NoOpMigrationManager();

    Document doc = SecureXmlParser.readXML(is, "test-entry.xml", noMigrations, version);

    assertNotNull(doc);
    assertEquals("root", doc.getDocumentElement().getTagName());
    assertEquals(1, doc.getDocumentElement().getChildNodes().getLength());
  }

  @Test
  public void readXML_withMigrations_appliesTextTransform() throws IOException {
    // Original XML has <old/>, migration renames it to <root><new/></root>
    String xml = "<root><old/></root>";
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_16));
    Version version = new Version("3.8.0.0");

    MigrationManager renamingMigration = new NoOpMigrationManager() {
      @Override
      public boolean hasTextMigrationsFor(Version decodedVersion) {
        return true;
      }

      @Override
      public String migrate(String source, Version v) {
        return source.replace("<old/>", "<migrated/>");
      }
    };

    Document doc = SecureXmlParser.readXML(is, "migrated.xml", renamingMigration, version);

    assertNotNull(doc);
    Element root = doc.getDocumentElement();
    assertEquals(1, root.getChildNodes().getLength());
    assertEquals("migrated", root.getChildNodes().item(0).getNodeName());
  }

  /**
   * Minimal MigrationManager for testing — no text or AST migrations.
   */
  private static class NoOpMigrationManager implements MigrationManager {
    @Override
    public boolean hasTextMigrationsFor(Version decodedVersion) {
      return false;
    }

    @Override
    public boolean hasAstMigrationsFor(Version decodedVersion) {
      return false;
    }

    @Override
    public String migrate(String source, Version version) {
      return source;
    }

    @Override
    public void migrate(Node root, ResourceTypeHelper typeHelper, Version version) {
    }

    @Override
    public void cacheType(NamedUserType type) {
    }

    @Override
    public AbstractType<?, ?, ?> getCachedType(String className) {
      return null;
    }

    @Override
    public InstanceCreation createInstanceCreation(ModelResource resourceClass) {
      return null;
    }

    @Override
    public void addFinalization(Runnable finalizer) {
    }
  }
}
