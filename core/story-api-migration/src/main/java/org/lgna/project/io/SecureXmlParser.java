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

import edu.cmu.cs.dennisc.java.io.TextFileUtilities;
import org.lgna.common.Resource;
import org.lgna.project.Version;
import org.lgna.project.migration.MigrationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * XXE-hardened XML parsing utilities and resource creation helpers extracted
 * from {@link XmlProjectIo}. All methods are package-private static.
 */
class SecureXmlParser {

  // Package prefixes allowed for reflective resource instantiation.
  // Defense-in-depth: prevents arbitrary class instantiation even if
  // an attacker controls the className attribute in a project XML file.
  private static final Set<String> ALLOWED_RESOURCE_PACKAGES = Set.of(
      "org.lgna.common.",
      "org.lgna.common.resources.",
      "org.lgna.story.resources.",
      "org.lgna.project."
  );

  private SecureXmlParser() {
  }

  // OWASP XXE Prevention — 7-layer defense applied once at class load.
  private static final DocumentBuilderFactory SECURE_FACTORY = createSecureFactory();

  private static DocumentBuilderFactory createSecureFactory() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      return factory;
    } catch (ParserConfigurationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  static Document readArchiveXml(InputStream is, String entryName) throws IOException {
    try {
      DocumentBuilder documentBuilder = SECURE_FACTORY.newDocumentBuilder();
      Document document = documentBuilder.parse(is);
      removeWhitespaceNodes(document.getDocumentElement());
      return document;
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new IOException("Unable to read " + entryName, e);
    }
  }

  static void removeWhitespaceNodes(Element element) {
    NodeList children = element.getChildNodes();
    for (int i = children.getLength() - 1; i >= 0; i--) {
      Node child = children.item(i);
      if ((child instanceof Text text) && isXmlWhitespace(text.getData())) {
        element.removeChild(child);
      } else if (child instanceof Element childElement) {
        removeWhitespaceNodes(childElement);
      }
    }
  }

  static boolean isXmlWhitespace(String text) {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if ((ch != ' ') && (ch != '\n') && (ch != '\r') && (ch != '\t')) {
        return false;
      }
    }
    return true;
  }

  static Document readXML(
      InputStream is,
      String entryName,
      MigrationManager migrationManager,
      Version decodedVersion) throws IOException {
    if (migrationManager.hasTextMigrationsFor(decodedVersion)) {
      Charset charSet = getCharsetForVersion(decodedVersion);
      String modifiedText =
          migrationManager.migrate(TextFileUtilities.read(new InputStreamReader(is, charSet)), decodedVersion);
      is = new ByteArrayInputStream(modifiedText.getBytes(charSet));
    }
    return readArchiveXml(is, entryName);
  }

  // Encoding of project XML changed from UTF-8 to UTF-16 in 3.7.
  static Charset getCharsetForVersion(Version version) {
    return (version.compareTo(Version.VERSION_3_7) < 0) ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16;
  }

  static String resourceContext(Element xmlElement, String entryName) {
    String resourceName = xmlElement.getAttribute("name");
    String uuidText = xmlElement.getAttribute("uuid");
    StringBuilder sb = new StringBuilder("resource");
    if ((resourceName != null) && !resourceName.isEmpty()) {
      sb.append(" '").append(resourceName).append("'");
    } else if ((uuidText != null) && !uuidText.isEmpty()) {
      sb.append(" with UUID '").append(uuidText).append("'");
    }
    if ((entryName != null) && !entryName.isEmpty()) {
      sb.append(" at archive entry '").append(entryName).append("'");
    }
    return sb.toString();
  }

  static boolean isAllowedResourcePackage(String className) {
    return ALLOWED_RESOURCE_PACKAGES.stream().anyMatch(className::startsWith);
  }

  static Resource createResource(Class<? extends Resource> resourceCls, String uuidText) throws IOException {
    // Defense-in-depth: verify the resource class belongs to an allowed package
    // before calling setAccessible. The caller already validates via ClassUtilities
    // and the cast to Class<? extends Resource>, but an explicit package check
    // prevents instantiation of classes from unexpected packages.
    String className = resourceCls.getName();
    if (!isAllowedResourcePackage(className)) {
      throw new IOException("Resource class '" + className
          + "' is not in an allowed package for reflective instantiation");
    }

    UUID uuid;
    try {
      uuid = UUID.fromString(uuidText);
    } catch (IllegalArgumentException iae) {
      throw new IOException("Invalid resource UUID " + uuidText, iae);
    }
    try {
      java.lang.reflect.Constructor<? extends Resource> constructor = resourceCls.getDeclaredConstructor(UUID.class);
      constructor.setAccessible(true);
      return constructor.newInstance(uuid);
    } catch (ReflectiveOperationException | SecurityException e) {
      throw new IOException("Unable to create resource " + resourceCls.getName() + " with UUID " + uuidText, e);
    }
  }
}
