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

import edu.cmu.cs.dennisc.java.io.InputStreamUtilities;
import edu.cmu.cs.dennisc.java.io.TextFileUtilities;
import edu.cmu.cs.dennisc.java.lang.ClassUtilities;
import edu.cmu.cs.dennisc.java.util.zip.ByteArrayDataSource;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import edu.cmu.cs.dennisc.java.util.zip.ZipUtilities;
import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import edu.cmu.cs.dennisc.print.PrintUtilities;
import edu.cmu.cs.dennisc.xml.XMLUtilities;
import org.alice.serialization.xml.XmlEncoderDecoder;
import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ProjectManifest;
import org.lgna.common.Resource;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;
import org.lgna.project.Version;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.migration.MigrationManager;
import org.lgna.project.migration.OptionalMigrationManager;
import org.lgna.project.migration.ProjectMigrationManager;
import org.lgna.project.migration.ast.ReplaceCameraWithVR;
import org.lgna.story.resourceutilities.ResourceTypeHelper;
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
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipOutputStream;

public class XmlProjectIo implements ProjectIo {

  private static final String PROGRAM_TYPE_ENTRY_NAME = "programType.xml";
  private static final String TYPE_ENTRY_NAME = "type.xml";
  private static final String RESOURCES_ENTRY_NAME = "resources.xml";

  private static final String XML_RESOURCE_TAG_NAME = "resource";

  private static final String XML_RESOURCE_CLASSNAME_ATTRIBUTE = "className";
  private static final String XML_RESOURCE_UUID_ATTRIBUTE = "uuid";
  private static final String XML_RESOURCE_ENTRY_NAME_ATTRIBUTE = "entryName";
  private static final String XML_RESOURCE_NAME_ATTRIBUTE = "name";
  private static final String XML_RESOURCE_ORIGINAL_FILE_NAME_ATTRIBUTE = "originalFileName";

  private static OptionalMigrationManager CAMERA_TO_VR = new OptionalMigrationManager(new ReplaceCameraWithVR());

  public static XmlProjectReader reader(ZipEntryContainer container) {
    return new XmlProjectReader(container);
  }

  public static XmlProjectWriter writer() {
    return new XmlProjectWriter();
  }

  private static class XmlProjectReader implements ProjectReader {
    private final ZipEntryContainer container;

    XmlProjectReader(ZipEntryContainer container) {
      this.container = container;
    }

    @Override
    public Project readProject(boolean makeVrReady) throws IOException, VersionNotSupportedException {
      ProjectManifest manifest = readManifest();
      Project.SceneCameraType cameraType = sceneCameraType(manifest);
      NamedUserType type = readType(PROGRAM_TYPE_ENTRY_NAME);
      if (makeVrReady) {
        CAMERA_TO_VR.migrate(type, typeHelper, ProjectVersion.getCurrentVersion());
        cameraType = Project.SceneCameraType.VRHeadset;
      }
      Set<Resource> resources = readResources();
      bindResourceExpressions(type, resources);
      Set<NamedUserType> namedUserTypes = Collections.emptySet();
      return new Project(type, namedUserTypes, resources, cameraType);
    }

    private ProjectManifest readManifest() throws IOException {
      InputStream is = container.getInputStream(MANIFEST_ENTRY_NAME);
      if (is == null) {
        return null;
      }
      return ManifestEncoderDecoder.fromJson(readContent(is), ProjectManifest.class);
    }

    private static Project.SceneCameraType sceneCameraType(ProjectManifest manifest) {
      if ((manifest == null) || (manifest.projectStructure == null) || (manifest.projectStructure.sceneCameraType == null)) {
        return Project.SceneCameraType.WindowCamera;
      }
      return manifest.projectStructure.sceneCameraType;
    }

    @Override
    public TypeResourcesPair readType() throws IOException, VersionNotSupportedException {
      NamedUserType type = readType(TYPE_ENTRY_NAME);
      Set<Resource> resources = readResources();
      bindResourceExpressions(type, resources);
      return new TypeResourcesPair(type, resources);
    }

    @Override
    public Version checkForFutureVersion() throws IOException {
      Version decodedProjectVersion = readSourceProgramVersion();
      if (ProjectVersion.getCurrentVersion().compareTo(decodedProjectVersion) < 0) {
        return decodedProjectVersion;
      }
      return null;
    }

    @Override
    public void setResourceTypeHelper(ResourceTypeHelper typeHelper) {
      this.typeHelper = typeHelper;
    }


    private Version readSourceProgramVersion() throws IOException {
      if (sourceProgramVersion != null) {
        return sourceProgramVersion;
      }
      if (container == null) {
        throw new IOException("There is no file to read");
      }
      InputStream is = container.getInputStream(VERSION_ENTRY_NAME);
      if (is == null) {
        throw new IOException(container.toString() + " does not contain entry " + VERSION_ENTRY_NAME);
      }

      String content;
      try (InputStream versionStream = is) {
        content = readContent(versionStream);
      }
      sourceProgramVersion = new Version(content);
      return sourceProgramVersion;
    }

    private static String readContent(InputStream is) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int count;
      while ((count = is.read(chunk)) != -1) {
        buffer.write(chunk, 0, count);
      }
      return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Document readXML(
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

    private Document readXML(String entryName, MigrationManager migrationManager, Version decodedVersion) throws IOException {
      InputStream is = container.getInputStream(entryName);
      if (is == null) {
        throw new IOException("Archive does not contain entry " + entryName);
      }
      try (InputStream xmlStream = is) {
        return readXML(xmlStream, entryName, migrationManager, decodedVersion);
      }
    }

    private NamedUserType readType(String entryName) throws IOException, VersionNotSupportedException {
      Version decodedProjectVersion = readSourceProgramVersion();

      Document xmlDocument = readXML(entryName, ProjectMigrationManager.getInstance(), decodedProjectVersion);
      NamedUserType type = (NamedUserType) (new XmlEncoderDecoder()).decode(xmlDocument);
      ProjectMigrationManager.getInstance().migrate(type, typeHelper, decodedProjectVersion);
      return type;
    }

    private Set<Resource> readResources() throws IOException {
      Set<Resource> resources = new HashSet<>();
      InputStream isResources = container.getInputStream(RESOURCES_ENTRY_NAME);
      if (isResources != null) {
        Document xmlDocument;
        try (InputStream resourcesStream = isResources) {
          xmlDocument = readArchiveXml(resourcesStream, RESOURCES_ENTRY_NAME);
        }
        List<Element> xmlElements = XMLUtilities.getChildElementsByTagName(xmlDocument.getDocumentElement(), XML_RESOURCE_TAG_NAME);
        for (Element xmlElement : xmlElements) {
          String className = xmlElement.getAttribute(XML_RESOURCE_CLASSNAME_ATTRIBUTE);
          String uuidText = xmlElement.getAttribute(XML_RESOURCE_UUID_ATTRIBUTE);
          String entryName = xmlElement.getAttribute(XML_RESOURCE_ENTRY_NAME_ATTRIBUTE);
          if ((className != null) && (uuidText != null) && (entryName != null)) {
            byte[] data = readResourceData(entryName, xmlElement);
            try {
              Class<? extends Resource> resourceCls = (Class<? extends Resource>) ClassUtilities.forName(className);
              Resource resource = createResource(resourceCls, uuidText);
              resource.decodeAttributes(xmlElement, data);
              resources.add(resource);
            } catch (ClassNotFoundException cnfe) {
              throw new IOException(
                  "Unknown resource class '" + className + "' for " + resourceContext(xmlElement, entryName),
                  cnfe);
            }
          }
        }
      }
      return resources;
    }

    private static Document readArchiveXml(InputStream is, String entryName) throws IOException {
      try {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(is);
        removeWhitespaceNodes(document.getDocumentElement());
        return document;
      } catch (ParserConfigurationException | SAXException | IOException e) {
        throw new IOException("Unable to read " + entryName, e);
      }
    }

    private static void removeWhitespaceNodes(Element element) {
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

    private static boolean isXmlWhitespace(String text) {
      for (int i = 0; i < text.length(); i++) {
        char ch = text.charAt(i);
        if ((ch != ' ') && (ch != '\n') && (ch != '\r') && (ch != '\t')) {
          return false;
        }
      }
      return true;
    }

    private byte[] readResourceData(String entryName, Element xmlElement) throws IOException {
      InputStream resourceStream = container.getInputStream(entryName);
      if (resourceStream == null) {
        throw new IOException("Missing resource data for " + resourceContext(xmlElement, entryName));
      }
      try (InputStream is = resourceStream) {
        byte[] data = InputStreamUtilities.getBytes(is);
        if (data == null) {
          throw new IOException("Missing resource data for " + resourceContext(xmlElement, entryName));
        }
        return data;
      } catch (IOException ioe) {
        throw new IOException("Unable to read resource data for " + resourceContext(xmlElement, entryName), ioe);
      }
    }

    private static String resourceContext(Element xmlElement, String entryName) {
      String resourceName = xmlElement.getAttribute("name");
      String uuidText = xmlElement.getAttribute(XML_RESOURCE_UUID_ATTRIBUTE);
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

    private static Resource createResource(Class<? extends Resource> resourceCls, String uuidText) throws IOException {
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

    private static void bindResourceExpressions(NamedUserType type, Set<Resource> resources) {
      if ((type == null) || resources.isEmpty()) {
        return;
      }
      Map<UUID, Resource> resourcesById = new HashMap<>(resources.size() * 2);
      for (Resource resource : resources) {
        resourcesById.put(resource.getId(), resource);
      }
      IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<ResourceExpression>(ResourceExpression.class) {
        @Override
        protected boolean isAcceptable(ResourceExpression resourceExpression) {
          return true;
        }
      };
      type.crawl(crawler, CrawlPolicy.COMPLETE);
      for (ResourceExpression resourceExpression : crawler.getList()) {
        Resource expressionResource = resourceExpression.resource.getValue();
        if (expressionResource != null) {
          Resource decodedResource = resourcesById.get(expressionResource.getId());
          if (decodedResource != null) {
            resourceExpression.resource.setValue(decodedResource);
          }
        }
      }
    }

    private ResourceTypeHelper typeHelper;
    private Version sourceProgramVersion;
  }

  // Encoding of project XML changed from UTF-8 to UTF-16 in 3.7.
  private static Charset getCharsetForVersion(Version version) {
    return (version.compareTo(Version.VERSION_3_7) < 0) ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16;
  }

  private static class XmlProjectWriter implements ProjectWriter {

    private static void writeVersion(ZipOutputStream zos) throws IOException {
      ZipUtilities.write(zos,
          new ByteArrayDataSource(ProjectIo.VERSION_ENTRY_NAME,
                                  ProjectVersion.getCurrentVersion().toString()));
    }

    private static void writeXML(final Document xmlDocument, ZipOutputStream zos, final String entryName) throws IOException {
      ZipUtilities.write(zos, new DataSource() {
        @Override
        public String getName() {
          return entryName;
        }

        @Override
        public void write(OutputStream os) {
          XMLUtilities.write(xmlDocument, os);
        }
      });
    }

    private static void writeType(NamedUserType type, ZipOutputStream zos, String entryName) throws IOException {
      writeXML((new XmlEncoderDecoder()).encode(type), zos, entryName);
    }

    private static void writeDataSources(ZipOutputStream zos, DataSource... dataSources) throws IOException {
      for (DataSource dataSource : dataSources) {
        ZipUtilities.write(zos, dataSource);
      }
    }

    private static void writeManifest(Project project, ZipOutputStream zos, DataSource... dataSources) throws IOException {
      if (!hasDataSource(ProjectIo.MANIFEST_ENTRY_NAME, dataSources)) {
        ProjectManifest manifest = project.createSaveManifest();
        ZipUtilities.write(zos, new ByteArrayDataSource(
            ProjectIo.MANIFEST_ENTRY_NAME,
            ManifestEncoderDecoder.toJson(manifest)));
      }
    }

    private static boolean hasDataSource(String name, DataSource... dataSources) {
      for (DataSource dataSource : dataSources) {
        if (name.equals(dataSource.getName())) {
          return true;
        }
      }
      return false;
    }

    private static String getValidFileName(Resource resource) {
      return ResourceExportNames.entryFileName(resource);
    }

    private static String generateEntryName(Resource resource, Set<String> usedEntryNames) {
      String validFilename = getValidFileName(resource);
      final String DESIRED_DIRECTORY_NAME = "resources";
      int i = 1;
      while (true) {
        StringBuilder sb = new StringBuilder();
        sb.append(DESIRED_DIRECTORY_NAME);
        if (i > 1) {
          sb.append(i);
        }
        sb.append("/");
        sb.append(validFilename);
        String potentialEntryName = sb.toString();
        if (usedEntryNames.contains(potentialEntryName)) {
          i += 1;
        } else {
          return potentialEntryName;
        }
      }
    }

    private static void writeResources(ZipOutputStream zos, Set<Resource> resources) throws IOException {
      if (resources.isEmpty()) {
        return;
      }
      Document xmlDocument = XMLUtilities.createDocument();
      Element xmlRootElement = xmlDocument.createElement("root");
      xmlDocument.appendChild(xmlRootElement);
      List<DataSource> resourceDataSources = new ArrayList<>();
      synchronized (resources) {
        Set<String> usedEntryNames = new HashSet<>();
        for (Resource resource : resources) {
          Element xmlElement = xmlDocument.createElement(XML_RESOURCE_TAG_NAME);
          resource.encodeAttributes(xmlElement);
          UUID uuid = resource.getId();
          assert uuid != null;

          xmlElement.setAttribute(XML_RESOURCE_CLASSNAME_ATTRIBUTE, resource.getClass().getName());
          xmlElement.setAttribute(XML_RESOURCE_UUID_ATTRIBUTE, uuid.toString());

          String entryName = generateEntryName(resource, usedEntryNames);
          usedEntryNames.add(entryName);
          resourceDataSources.add(new ByteArrayDataSource(entryName, resource.getData()));
          String fallbackName = ResourceExportNames.fileNameFromEntry(entryName);
          xmlElement.setAttribute(
              XML_RESOURCE_NAME_ATTRIBUTE,
              ResourceExportNames.metadataName(resource.getName(), fallbackName));
          xmlElement.setAttribute(
              XML_RESOURCE_ORIGINAL_FILE_NAME_ATTRIBUTE,
              ResourceExportNames.metadataOriginalFileName(resource.getOriginalFileName(), fallbackName));
          xmlElement.setAttribute(XML_RESOURCE_ENTRY_NAME_ATTRIBUTE, entryName);
          xmlRootElement.appendChild(xmlElement);
        }
      }
      writeXML(xmlDocument, zos, RESOURCES_ENTRY_NAME);
      for (DataSource dataSource : resourceDataSources) {
        ZipUtilities.write(zos, dataSource);
      }
    }

    @Override
    public void writeProject(OutputStream os, final Project project, DataSource... dataSources) throws IOException {
      ZipOutputStream zos = new ZipOutputStream(os);
      writeVersion(zos);
      writeManifest(project, zos, dataSources);
      NamedUserType programType = project.getProgramType();
      writeType(programType, zos, PROGRAM_TYPE_ENTRY_NAME);
      writeDataSources(zos, dataSources);
      Set<Resource> resources = project.getResources();

      IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<ResourceExpression>(ResourceExpression.class) {
        @Override
        protected boolean isAcceptable(ResourceExpression resourceExpression) {
          return true;
        }
      };
      programType.crawl(crawler, CrawlPolicy.COMPLETE);

      for (ResourceExpression resourceExpression : crawler.getList()) {
        Resource resource = resourceExpression.resource.getValue();
        if (!resources.contains(resource)) {
          PrintUtilities.println("WARNING: adding missing resource", resource);
          resources.add(resource);
        }
      }

      writeResources(zos, resources);
      zos.flush();
      zos.close();
    }

    @Override
    public void writeType(OutputStream os, NamedUserType type, DataSource... dataSources) throws IOException {
      ZipOutputStream zos = new ZipOutputStream(os);
      writeVersion(zos);
      writeType(type, zos, TYPE_ENTRY_NAME);
      writeDataSources(zos, dataSources);

      IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<ResourceExpression>(ResourceExpression.class) {
        @Override
        protected boolean isAcceptable(ResourceExpression resourceExpression) {
          return true;
        }
      };
      type.crawl(crawler, CrawlPolicy.EXCLUDE_REFERENCES_ENTIRELY);
      Set<Resource> resources = new HashSet<>();
      for (ResourceExpression resourceExpression : crawler.getList()) {
        resources.add(resourceExpression.resource.getValue());
      }
      writeResources(zos, resources);

      zos.flush();
      zos.close();
    }
  }
}
