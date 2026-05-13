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

import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;

public class ModelResourceExporter {

  private String resourceName;
  private String className;
  private List<String> tags = new ArrayList<>();
  private List<String> groupTags = new ArrayList<>();
  private List<String> themeTags = new ArrayList<>();
  private Map<String, AxisAlignedBox> boundingBoxes = new HashMap<>();
  private File xmlFile;
  private List<String> jointIdsToSuppress = new ArrayList<>();
  private List<String> arraysToExposeFirstElementOf = new ArrayList<>();
  private List<String> arraysToHideElementsOf = new ArrayList<>();
  private Map<ModelSubResourceExporter, Image> thumbnails = new HashMap<>();
  private Map<String, File> existingThumbnails = null;
  private List<ModelSubResourceExporter> subResources = new ArrayList<>();
  private boolean isSims = false;
  private boolean hasNewData = false;
  private boolean forceRebuildCode = false;
  private boolean forceRebuildXML = false;
  private boolean shouldRecenter = false;
  private boolean recenterXZ = false;
  private boolean moveCenterToBottom = true;
  private List<String> forcedOverridingEnumNames = new ArrayList<>();
  private Map<String, List<String>> forcedEnumNamesMap = new HashMap<>();
  private Map<String, String> customArrayNameMap = new HashMap<>();
  private Map<String, Map<String, AffineMatrix4x4>> poses = new HashMap<>();
  private String[] arrayNamesToSkip = null;
  private boolean exportGalleryResources = true;
  private boolean isDeprecated = false;
  private boolean placeOnGround = false;
  private boolean validData = false;
  private boolean enableArraySupport = true;
  private String attributionName;
  private String attributionYear;
  private ModelClassData classData;
  private List<Tuple2<String, String>> jointList;

  public ModelResourceExporter(String className) {
    this.className = className;
    if (Character.isLowerCase(this.className.charAt(0))) {
      this.className = this.className.substring(0, 1).toUpperCase() + this.className.substring(1);
    }
  }

  public ModelResourceExporter(String className, String resourceName) {
    this(className);
    this.resourceName = resourceName;
  }

  public ModelResourceExporter(String className, ModelClassData classData) {
    this(className);
    this.classData = classData;
  }

  public ModelResourceExporter(String className, String resourceName, ModelClassData classData) {
    this(className, resourceName);
    this.classData = classData;
  }

  public ModelResourceExporter(String className, ModelClassData classData, Class<?> jointAndVisualFactoryClass) {
    this(className, classData);
  }

  public ModelResourceExporter(String className, String resourceName, ModelClassData classData, Class<?> jointAndVisualFactoryClass) {
    this(className, resourceName, classData);
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public void setHasNewData(boolean hasNewData) {
    this.hasNewData = hasNewData;
  }

  public void setIsDeprecated(boolean isDeprecated) {
    this.isDeprecated = isDeprecated;
  }

  public void setPlaceOnGround(boolean placeOnGround) {
    this.placeOnGround = placeOnGround;
  }

  public void setShouldRecenter(boolean shouldRecenter) {
    this.shouldRecenter = shouldRecenter;
    if (!this.shouldRecenter) {
      this.moveCenterToBottom = false;
      this.recenterXZ = false;
    }
  }

  public void addPose(String poseName, Map<String, AffineMatrix4x4> pose) {
    this.poses.put(poseName, pose);
  }

  public void setEnableArrays(boolean enableArrays) {
    this.enableArraySupport = enableArrays;
  }

  public boolean shouldRecenter() {
    return this.shouldRecenter;
  }
  public void setMoveCenterToBottom(boolean moveCenter) {
    this.moveCenterToBottom = moveCenter;
  }
  public boolean shouldMoveCenterToBottom() {
    return this.moveCenterToBottom;
  }
  public void setRecenterXZ(boolean recenterXZ) {
    this.recenterXZ = recenterXZ;
  }
  public boolean shouldRecenterXZ() {
    return this.recenterXZ;
  }
  public boolean hasValidData() {
    return this.validData;
  }
  public void setHasValidData(boolean validData) {
    this.validData = validData;
  }
  public boolean hasNewData() {
    return this.hasNewData;
  }
  public void setExportGalleryResources(boolean exportResources) {
    this.exportGalleryResources = exportResources;
  }
  public boolean getExportGalleryResources() {
    return this.exportGalleryResources;
  }

  public String getResourceName() {
    return this.resourceName;
  }
  public ModelClassData getClassData() {
    return this.classData;
  }
  public void setForceRebuildCode(boolean rebuildCode) {
    this.forceRebuildCode = rebuildCode;
  }
  public void setForceRebuildXML(boolean rebuildXML) {
    this.forceRebuildXML = rebuildXML;
  }
  public boolean getForceRebuildCode() {
    return this.forceRebuildCode;
  }
  public boolean getForceRebuildXML() {
    return this.forceRebuildXML;
  }
  public boolean hasJointMap() {
    return this.jointList != null;
  }
  public boolean hasJoints() {
    return hasJointMap() && !this.jointList.isEmpty();
  }
  public List<Tuple2<String, String>> getJointMap() {
    return this.jointList;
  }

  public void addArrayNamesToExposeFirstElementOf(List<String> arrayNames) {
    addAllUnique(this.arraysToExposeFirstElementOf, arrayNames);
  }

  public void addArrayNamesToExposeFirstElementOf(String[] arrayNames) {
    addAllUnique(this.arraysToExposeFirstElementOf, Arrays.asList(arrayNames));
  }

  public void addArrayNamesToHideElementsOf(List<String> arrayNames) {
    addAllUnique(this.arraysToHideElementsOf, arrayNames);
  }

  public void addArrayNamesToHideElementsOf(String[] arrayNames) {
    addAllUnique(this.arraysToHideElementsOf, Arrays.asList(arrayNames));
  }

  public void addJointIdsToSuppress(List<String> jointIds) {
    addAllUnique(this.jointIdsToSuppress, jointIds);
  }

  public void addJointIdsToSuppress(String[] jointIds) {
    addAllUnique(this.jointIdsToSuppress, Arrays.asList(jointIds));
  }

  private static void addAllUnique(List<String> target, List<String> source) {
    for (String item : source) {
      if (!target.contains(item)) {
        target.add(item);
      }
    }
  }

  public void setJointMap(List<Tuple2<String, String>> jointList) {
    this.jointList = jointList;
  }

  public void addSubResource(ModelSubResourceExporter subResource) {
    this.subResources.add(subResource);
  }

  public void addResource(String modelName, String textureName, String resourceType, String attributionName, String attributionYear) {
    String nameToUse = normalizeAttribution(attributionName, this.attributionName);
    String yearToUse = normalizeAttribution(attributionYear, this.attributionYear);
    this.addSubResource(new ModelSubResourceExporter(modelName, textureName, resourceType, nameToUse, yearToUse));
  }

  private static String normalizeAttribution(String value, String parentValue) {
    if ((value != null) && !value.equals(parentValue) && !value.isEmpty()) {
      return value;
    }
    return null;
  }

  public void addSubResourceTags(String modelName, String textureName, String... tags) {
    addTagsToMatchingSubResources(modelName, textureName, tags, ModelSubResourceExporter::addTags);
  }

  public void addSubResourceGroupTags(String modelName, String textureName, String... tags) {
    addTagsToMatchingSubResources(modelName, textureName, tags, ModelSubResourceExporter::addGroupTags);
  }

  public void addSubResourceThemeTags(String modelName, String textureName, String... tags) {
    addTagsToMatchingSubResources(modelName, textureName, tags, ModelSubResourceExporter::addThemeTags);
  }

  private void addTagsToMatchingSubResources(String modelName, String textureName,
      String[] tags, java.util.function.BiConsumer<ModelSubResourceExporter, String[]> updater) {
    if ((tags != null) && (tags.length > 0)) {
      for (ModelSubResourceExporter subResource : this.subResources) {
        if (subResource.getModelName().equalsIgnoreCase(modelName)
            && ((textureName == null) || subResource.getTextureName().equalsIgnoreCase(textureName))) {
          updater.accept(subResource, tags);
        }
      }
    }
  }

  public void addAttribution(String name, String year) {
    this.attributionName = name;
    this.attributionYear = year;
  }

  public void addTags(String... tags) {
    if (tags != null) {
      Collections.addAll(this.tags, tags);
    }
  }

  public void addGroupTags(String... tags) {
    if (tags != null) {
      Collections.addAll(this.groupTags, tags);
    }
  }

  public void addThemeTags(String... tags) {
    if (tags != null) {
      Collections.addAll(this.themeTags, tags);
    }
  }

  public boolean isSims() {
    return this.isSims;
  }

  public void setIsSims(boolean isSims) {
    this.isSims = isSims;
  }

  public String getClassName() {
    return this.className;
  }

  public String getPackageString() {
    return this.classData.packageString;
  }

  public void setBoundingBox(String modelName, AxisAlignedBox boundingBox) {
    this.boundingBoxes.put(modelName, boundingBox);
  }

  AxisAlignedBox getBoundingBox(String modelName) {
    return this.boundingBoxes.get(modelName);
  }

  boolean hasBoundingBox(String modelName) {
    return this.boundingBoxes.containsKey(modelName);
  }

  Collection<AxisAlignedBox> getBoundingBoxValues() {
    return this.boundingBoxes.values();
  }

  public void addExistingThumbnail(String name, File thumbnailFile) {
    if ((thumbnailFile != null) && thumbnailFile.exists()) {
      if (this.existingThumbnails == null) {
        this.existingThumbnails = new HashMap<>();
      }
      this.existingThumbnails.put(name, thumbnailFile);
    } else {
      System.err.println("FAILED TO ADDED THUMBAIL: " + thumbnailFile + " does not exist.");
    }
  }

  public void setXMLFile(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  String getAttributionName() {
    return this.attributionName;
  }
  String getAttributionYear() {
    return this.attributionYear;
  }
  boolean isDeprecated() {
    return this.isDeprecated;
  }
  boolean isPlaceOnGround() {
    return this.placeOnGround;
  }
  List<String> getTags() {
    return this.tags;
  }
  List<String> getGroupTags() {
    return this.groupTags;
  }
  List<String> getThemeTags() {
    return this.themeTags;
  }
  List<ModelSubResourceExporter> getSubResources() {
    return this.subResources;
  }

  public String createResourceEnumName(String modelName, String textureName) {
    return ModelResourceJavaGenerator.createResourceEnumNameForModelAndTexture(this, modelName, textureName);
  }

  public boolean shouldHideJointsOfArray(String arrayName) {
    return this.arraysToHideElementsOf.contains(arrayName);
  }

  public String createJavaCode() throws DataFormatException {
    return ModelResourceJavaGenerator.buildJavaCodeBody(this);
  }

  Map<String, Map<String, AffineMatrix4x4>> getPoses() {
    return this.poses;
  }
  boolean isEnableArraySupport() {
    return this.enableArraySupport;
  }
  List<String> getArraysToHideElementsOf() {
    return this.arraysToHideElementsOf;
  }
  List<String> getArraysToExposeFirstElementOf() {
    return this.arraysToExposeFirstElementOf;
  }
  List<Tuple2<String, String>> getJointList() {
    return this.jointList;
  }
  Map<String, String> getCustomArrayNameMap() {
    return this.customArrayNameMap;
  }
  List<String> getJointIdsToSuppress() {
    return this.jointIdsToSuppress;
  }
  String[] getArrayNamesToSkip() {
    return this.arrayNamesToSkip;
  }
  Map<String, List<String>> getForcedEnumNamesMap() {
    return this.forcedEnumNamesMap;
  }
  List<String> getForcedOverridingEnumNames() {
    return this.forcedOverridingEnumNames;
  }
  File getXmlFile() {
    return this.xmlFile;
  }

  String createXMLString() {
    return ModelResourceXmlGenerator.createXMLString(this);
  }

  File createXMLFile(String root, boolean forceRebuild) throws IOException {
    return ModelResourceXmlGenerator.createXMLFile(this, root, forceRebuild);
  }

  public String getThumbnailPath(String rootPath, String thumbnailName) {
    return ModelResourceThumbnailWriter.getThumbnailPath(rootPath, this.classData.packageString, this.className, thumbnailName);
  }

  public static BufferedImage createClassThumb(BufferedImage imgSrc) {
    return ModelResourceThumbnailWriter.createClassThumb(imgSrc);
  }

  List<File> saveThumbnailsToDir(String root) throws IOException {
    return ModelResourceThumbnailWriter.saveThumbnailsToDir(
        root,
        this.classData.packageString,
        this.className,
        this.resourceName,
        this.existingThumbnails,
        this.thumbnails,
        this.subResources);
  }

  public boolean isValidEnumName(String modelName, String enumName) {
    return ModelResourceJavaGenerator.isValidEnumName(this, modelName, enumName);
  }

  public void setArrayNamesToSkip(String[] arrayNamesToSkip) {
    this.arrayNamesToSkip = arrayNamesToSkip;
  }

  public void addCustomArrayName(String arrayName, String customName) {
    this.customArrayNameMap.put(arrayName, customName);
  }

  public void addCustomArrayNames(Map<String, String> customArrayNames) {
    this.customArrayNameMap.putAll(customArrayNames);
  }

  public void addForcedEnumNames(String resourceName, List<String> enumNames) {
    if ((enumNames != null) && !enumNames.isEmpty()) {
      if (resourceName == null) {
        this.forcedOverridingEnumNames.addAll(enumNames);
      } else {
        this.forcedEnumNamesMap.computeIfAbsent(resourceName, k -> new ArrayList<>()).addAll(enumNames);
      }
    }
  }

}
