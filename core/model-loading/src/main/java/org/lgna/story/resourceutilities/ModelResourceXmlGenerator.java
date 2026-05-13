/*
 * Copyright (c) 2006-2011, Carnegie Mellon University. All rights reserved.
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
 */

package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.java.io.FileUtilities;
import edu.cmu.cs.dennisc.xml.XMLUtilities;
import org.alice.math.immutable.AxisAlignedBox;
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ModelResourceXmlGenerator {
  private ModelResourceXmlGenerator() {
  }

  static File createXMLFile(ModelResourceExporter exporter, String root, boolean forceRebuild) throws IOException {
    File outputFile = ModelResourceFileUtilities.getXMLFile(root, exporter.getPackageString(), exporter.getClassName());
    ModelResourceFileUtilities.ensureOutputFile(outputFile, "XML resource");
    File existingXmlFile = exporter.getXmlFile();
    if (!forceRebuild && (existingXmlFile != null) && existingXmlFile.exists()) {
      FileUtilities.copyFile(existingXmlFile, outputFile);
      return outputFile;
    } else {
      String xmlString = createXMLString(exporter);
      try (FileWriter fw = new FileWriter(outputFile)) {
        fw.write(xmlString);
      }
      return outputFile;
    }
  }

  static AxisAlignedBox computeBoundingBoxUnion(Iterable<AxisAlignedBox> boundingBoxes) {
    AxisAlignedBox superBox = AxisAlignedBox.NaN;
    for (AxisAlignedBox boundingBox : boundingBoxes) {
      superBox = superBox.union(boundingBox);
    }
    return superBox;
  }

  static String createXMLString(ModelResourceExporter exporter) {
    Document doc = createXMLDocument(exporter);
    if (doc != null) {
      try {
        TransformerFactory transfac = TransformerFactory.newInstance();
        transfac.setAttribute("indent-number", 4);
        Transformer trans = transfac.newTransformer();
        //                  trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        String xmlString = sw.toString();

        return xmlString;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static Document createXMLDocument(ModelResourceExporter exporter) {
    try {
      Document doc = XMLUtilities.createDocument();
      String className = exporter.getClassName();
      String attributionName = exporter.getAttributionName();
      String attributionYear = exporter.getAttributionYear();
      List<String> tags = exporter.getTags();
      List<String> groupTags = exporter.getGroupTags();
      List<String> themeTags = exporter.getThemeTags();
      List<ModelSubResourceExporter> subResources = exporter.getSubResources();
      Set<String> tagSet = new HashSet<String>(tags);
      Set<String> groupTagSet = new HashSet<String>(groupTags);
      Set<String> themeTagSet = new HashSet<String>(themeTags);

      Element modelRoot = doc.createElement("AliceModel");
      modelRoot.setAttribute("name", className);
      if ((attributionName != null) && (attributionName.length() > 0)) {
        modelRoot.setAttribute("creator", attributionName);
      }
      if ((attributionYear != null) && (attributionYear.length() > 0)) {
        modelRoot.setAttribute("creationYear", attributionYear);
      }
      if (exporter.isDeprecated()) {
        modelRoot.setAttribute("deprecated", "TRUE");
      }
      if (exporter.isPlaceOnGround()) {
        modelRoot.setAttribute("placeOnGround", "TRUE");
      }
      doc.appendChild(modelRoot);
      AxisAlignedBox classBoundingBox = persistComputedClassBoundingBoxIfMissing(exporter, className);
      modelRoot.appendChild(createBoundingBoxElement(doc, classBoundingBox));
      modelRoot.appendChild(createTagsElement(doc, tags));
      modelRoot.appendChild(createGroupTagsElement(doc, groupTags));
      modelRoot.appendChild(createThemeTagsElement(doc, themeTags));

      for (ModelSubResourceExporter subResource : subResources) {
        refreshSubResourceBoundingBoxFromExporterState(exporter, subResource, className);
        modelRoot.appendChild(createSubResourceElement(doc, subResource, exporter, tagSet, groupTagSet, themeTagSet));
      }

      return doc;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static AxisAlignedBox persistComputedClassBoundingBoxIfMissing(ModelResourceExporter exporter, String className) {
    AxisAlignedBox classBoundingBox = exporter.getBoundingBox(className);
    if (classBoundingBox == null) {
      classBoundingBox = computeBoundingBoxUnion(exporter.getBoundingBoxValues());
      exporter.setBoundingBox(className, classBoundingBox);
    }
    return classBoundingBox;
  }

  private static void refreshSubResourceBoundingBoxFromExporterState(ModelResourceExporter exporter, ModelSubResourceExporter subResource, String className) {
    String modelName = subResource.getModelName();
    if (!modelName.equalsIgnoreCase(className) && exporter.hasBoundingBox(modelName)) {
      subResource.setBbox(exporter.getBoundingBox(modelName));
    }
  }

  private static Element createBoundingBoxElement(Document doc, AxisAlignedBox bbox) {
    Element bboxElement = doc.createElement("BoundingBox");
    Element minElement = doc.createElement("Min");
    minElement.setAttribute("x", Double.toString(bbox.getXMinimum()));
    minElement.setAttribute("y", Double.toString(bbox.getYMinimum()));
    minElement.setAttribute("z", Double.toString(bbox.getZMinimum()));
    Element maxElement = doc.createElement("Max");
    maxElement.setAttribute("x", Double.toString(bbox.getXMaximum()));
    maxElement.setAttribute("y", Double.toString(bbox.getYMaximum()));
    maxElement.setAttribute("z", Double.toString(bbox.getZMaximum()));

    bboxElement.appendChild(minElement);
    bboxElement.appendChild(maxElement);

    return bboxElement;
  }

  private static Element createTagsElement(Document doc, List<String> tagList) {
    Element tagsElement = doc.createElement("Tags");
    for (String tag : tagList) {
      Element tagElement = doc.createElement("Tag");
      tagElement.setTextContent(tag);
      tagsElement.appendChild(tagElement);
    }
    return tagsElement;
  }

  private static Element createGroupTagsElement(Document doc, List<String> tagList) {
    Element tagsElement = doc.createElement("GroupTags");
    for (String tag : tagList) {
      Element tagElement = doc.createElement("GroupTag");
      tagElement.setTextContent(tag);
      tagsElement.appendChild(tagElement);
    }
    return tagsElement;
  }

  private static Element createThemeTagsElement(Document doc, List<String> tagList) {
    Element tagsElement = doc.createElement("ThemeTags");
    for (String tag : tagList) {
      Element tagElement = doc.createElement("ThemeTag");
      tagElement.setTextContent(tag);
      tagsElement.appendChild(tagElement);
    }
    return tagsElement;
  }

  private static Element createSubResourceElement(Document doc, ModelSubResourceExporter subResource, ModelResourceExporter exporter, Set<String> parentTags, Set<String> parentGroupTags, Set<String> parentThemeTags) {
    Element resourceElement = doc.createElement("Resource");
    String modelName = subResource.getModelName();
    String textureName = subResource.getTextureName();
    resourceElement.setAttribute("textureName", AliceResourceUtilities.makeEnumName(textureName));
    resourceElement.setAttribute("resourceName", exporter.createResourceEnumName(modelName, textureName));
    if (modelName != null) {
      resourceElement.setAttribute("modelName", modelName);
    }
    if (subResource.getAttributionName() != null) {
      resourceElement.setAttribute("creator", subResource.getAttributionName());
    }
    if (subResource.getAttributionYear() != null) {
      resourceElement.setAttribute("creationYear", subResource.getAttributionYear());
    }
    if (subResource.getBbox() != null) {
      resourceElement.appendChild(createBoundingBoxElement(doc, subResource.getBbox()));
    }
    appendUniqueTags(doc, resourceElement, "Tags", "Tag", subResource.getTags(), parentTags);
    appendUniqueTags(doc, resourceElement, "GroupTags", "GroupTag", subResource.getGroupTags(), parentGroupTags);
    appendUniqueTags(doc, resourceElement, "ThemeTags", "ThemeTag", subResource.getThemeTags(), parentThemeTags);
    return resourceElement;
  }

  private static void appendUniqueTags(Document doc, Element resourceElement, String groupName, String itemName, List<String> resourceTags, Set<String> parentTags) {
    if (!resourceTags.isEmpty()) {
      Element tagsElement = null;
      for (String tag : resourceTags) {
        if ((parentTags == null) || !parentTags.contains(tag)) {
          if (tagsElement == null) {
            tagsElement = doc.createElement(groupName);
          }
          Element tagElement = doc.createElement(itemName);
          tagElement.setTextContent(tag);
          tagsElement.appendChild(tagElement);
        }
      }
      if (tagsElement != null) {
        resourceElement.appendChild(tagsElement);
      }
    }
  }
}
