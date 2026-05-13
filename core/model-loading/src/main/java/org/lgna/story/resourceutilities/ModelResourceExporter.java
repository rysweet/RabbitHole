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
import edu.cmu.cs.dennisc.java.io.TextFileUtilities;
import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.lgna.story.implementation.alice.AliceResourceClassUtilities;
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.lgna.story.implementation.alice.JointImplementationAndVisualDataFactory;
import org.lgna.story.resources.*;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;

public class ModelResourceExporter {

  private static boolean REMOVE_ROOT_JOINTS = false;

  static final String ROOT_IDS_FIELD_NAME = "JOINT_ID_ROOTS";
  static final String ROOT_IDS_METHOD_NAME = "getRootJointIds";

  private class NamedFile {
    public String name;
    public File file;

    public NamedFile(String name, File file) {
      this.name = name;
      this.file = file;
    }
  }

  private interface SubResourceTagUpdater {
    void addTags(ModelSubResourceExporter subResource, String... tags);
  }

  private String resourceName;
  private String className;
  private List<String> tags = new LinkedList<String>();
  private List<String> groupTags = new LinkedList<String>();
  private List<String> themeTags = new LinkedList<String>();
  private Map<String, AxisAlignedBox> boundingBoxes = new HashMap<String, AxisAlignedBox>();
  private File xmlFile;
  private File javaFile;
  private List<String> jointIdsToSuppress = new ArrayList<String>();
  private List<String> arraysToExposeFirstElementOf = new ArrayList<String>();
  private List<String> arraysToHideElementsOf = new ArrayList<String>();
  private Map<ModelSubResourceExporter, Image> thumbnails = new HashMap<ModelSubResourceExporter, Image>();
  private Map<String, File> existingThumbnails = null;
  private List<ModelSubResourceExporter> subResources = new LinkedList<ModelSubResourceExporter>();
  private boolean isSims = false;
  private boolean hasNewData = false;
  private boolean forceRebuildCode = false;
  private boolean forceRebuildXML = false;
  private Date lastEdited = null;
  private boolean shouldRecenter = false;
  private boolean recenterXZ = false;
  private boolean moveCenterToBottom = true;
  private List<String> forcedOverridingEnumNames = new ArrayList<String>();
  private Map<String, List<String>> forcedEnumNamesMap = new HashMap<String, List<String>>();
  private Map<String, String> customArrayNameMap = new HashMap<String, String>();
  private Map<String, Map<String, AffineMatrix4x4>> poses = new HashMap<String, Map<String, AffineMatrix4x4>>();
  private String[] arrayNamesToSkip = null;
  private boolean exportGalleryResources = true;
  private boolean isDeprecated = false;
  private boolean placeOnGround = false;
  private boolean validData = false;

  private boolean enableArraySupport = true;

  private String attributionName;
  private String attributionYear;

  private Class<?> jointAndVisualFactory = JointImplementationAndVisualDataFactory.class;

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
    this.jointAndVisualFactory = jointAndVisualFactoryClass;
  }

  public ModelResourceExporter(String className, String resourceName, ModelClassData classData, Class<?> jointAndVisualFactoryClass) {
    this(className, resourceName, classData);
    this.jointAndVisualFactory = jointAndVisualFactoryClass;
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

  public void setLastEdited(Date lastEdited) {
    this.lastEdited = lastEdited;
  }

  public void addLastEditedDate(Date date) {
    if (date == null) {
      return;
    }
    if (this.lastEdited == null) {
      this.lastEdited = date;
    } else if (date.after(this.lastEdited)) {
      this.lastEdited = date;
    }
  }

  public void setExportGalleryResources(boolean exportResources) {
    this.exportGalleryResources = exportResources;
  }

  public boolean getExportGalleryResources() {
    return this.exportGalleryResources;
  }

  public static boolean isMoreRecentThan(Date dataDate, File file) {
    if (dataDate == null) {
      return false;
    }
    Date fileDate = new Date(file.lastModified());
    boolean isNewer = dataDate.after(fileDate);
    return isNewer;
  }

  public static boolean isMoreRecentThan(Date dataData, Date otherDate) {
    if (dataData == null) {
      return false;
    }
    return dataData.after(otherDate);
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

  public void setJointAndVisualFactory(Class<?> jointAndVisualFactoryClass) {
    this.jointAndVisualFactory = jointAndVisualFactoryClass;
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
    for (String arrayName : arrayNames) {
      if (!this.arraysToExposeFirstElementOf.contains(arrayName)) {
        this.arraysToExposeFirstElementOf.add(arrayName);
      }
    }
  }

  public void addArrayNamesToExposeFirstElementOf(String[] arrayNames) {
    for (String arrayName : arrayNames) {
      if (!this.arraysToExposeFirstElementOf.contains(arrayName)) {
        this.arraysToExposeFirstElementOf.add(arrayName);
      }
    }
  }

  public void addArrayNamesToHideElementsOf(List<String> arrayNames) {
    for (String arrayName : arrayNames) {
      if (!this.arraysToHideElementsOf.contains(arrayName)) {
        this.arraysToHideElementsOf.add(arrayName);
      }
    }
  }

  public void addArrayNamesToHideElementsOf(String[] arrayNames) {
    for (String arrayName : arrayNames) {
      if (!this.arraysToHideElementsOf.contains(arrayName)) {
        this.arraysToHideElementsOf.add(arrayName);
      }
    }
  }

  public void addJointIdsToSuppress(List<String> jointIds) {
    for (String jointId : jointIds) {
      if (!this.jointIdsToSuppress.contains(jointId)) {
        this.jointIdsToSuppress.add(jointId);
      }
    }
  }

  public void addJointIdsToSuppress(String[] jointIds) {
    for (String jointId : jointIds) {
      if (!this.jointIdsToSuppress.contains(jointId)) {
        this.jointIdsToSuppress.add(jointId);
      }
    }
  }

  public static int getArrayIndexForJoint(String jointName) {
    return ModelResourceArrayUtilities.getArrayIndexForJoint(jointName);
  }

  public static boolean hasArray(String arrayName, List<Tuple2<String, String>> jointList) {
    return ModelResourceArrayUtilities.hasArray(arrayName, jointList);
  }

  public static String getArrayNameForJoint(String jointName, Map<String, String> customArrayNameMap, String[] namesToSkip) {
    return ModelResourceArrayUtilities.getArrayNameForJoint(jointName, customArrayNameMap, namesToSkip);
  }

  public static Map<String, List<String>> getArrayEntriesFromJointList(List<Tuple2<String, String>> jointList, Map<String, String> customArrayNameMap, List<String> jointsToSuppress, String[] arrayNamesToSkip) throws DataFormatException {
    return ModelResourceArrayUtilities.getArrayEntriesFromJointList(jointList, customArrayNameMap, jointsToSuppress, arrayNamesToSkip);
  }

  public static Map<String, List<String>> getArrayEntries(List<String> jointNames, Map<String, String> customArrayNameMap, List<String> jointsToSuppress, String[] arrayNamesToSkip) throws DataFormatException {
    return ModelResourceArrayUtilities.getArrayEntries(jointNames, customArrayNameMap, jointsToSuppress, arrayNamesToSkip);
  }

  List<Tuple2<String, String>> makeCodeReadyTree(List<Tuple2<String, String>> sourceList) {
    return ModelResourceJointTreeUtilities.makeCodeReadyTree(sourceList, REMOVE_ROOT_JOINTS);
  }

  public void setJointMap(List<Tuple2<String, String>> jointList) {
    this.jointList = jointList;
    //    if (this.classData == null)
    //    {
    //      this.classData = ModelResourceExporter.getBestClassDataForJointList(jointList);
    //    }
  }

  public void addSubResource(ModelSubResourceExporter subResource) {
    this.subResources.add(subResource);
  }

  public void addResource(String modelName, String textureName, String resourceType, String attributionName, String attributionYear) {
    String attributionNameToUse = null;
    String attributionYearToUse = null;
    if ((attributionName != null) && !attributionName.equals(this.attributionName)) {
      attributionNameToUse = attributionName;
    }
    if ((attributionYear != null) && !attributionYear.equals(this.attributionYear)) {
      attributionYearToUse = attributionYear;
    }
    if ((attributionNameToUse != null) && (attributionNameToUse.length() == 0)) {
      attributionNameToUse = null;
    }
    if ((attributionYearToUse != null) && (attributionYearToUse.length() == 0)) {
      attributionYearToUse = null;
    }
    this.addSubResource(new ModelSubResourceExporter(modelName, textureName, resourceType, attributionNameToUse, attributionYearToUse));
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

  private void addTagsToMatchingSubResources(String modelName, String textureName, String[] tags, SubResourceTagUpdater updater) {
    if ((tags != null) && (tags.length > 0)) {
      for (ModelSubResourceExporter subResource : this.subResources) {
        if (subResource.getModelName().equalsIgnoreCase(modelName)) {
          if ((textureName == null) || subResource.getTextureName().equalsIgnoreCase(textureName)) {
            updater.addTags(subResource, tags);
          }
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

  AxisAlignedBox computeBoundingBoxUnion() {
    AxisAlignedBox superBox = AxisAlignedBox.NaN;
    for (AxisAlignedBox boundingBox : this.boundingBoxes.values()) {
      superBox = superBox.union(boundingBox);
    }
    return superBox;
  }

  //  public void addThumbnail( String modelName, String textureName, String resourceType, String attributionName, String attributionYear, Image thumbnail )
  //  {
  //    this.thumbnails.put( new ModelSubResourceExporter( modelName, textureName, resourceType, attributionName, attributionYear ), thumbnail );
  //  }

  public void addExistingThumbnail(String name, File thumbnailFile) {
    if ((thumbnailFile != null) && thumbnailFile.exists()) {
      if (this.existingThumbnails == null) {
        this.existingThumbnails = new HashMap<String, File>();
      }
      this.existingThumbnails.put(name, thumbnailFile);
    } else {
      System.err.println("FAILED TO ADDED THUMBAIL: " + thumbnailFile + " does not exist.");
    }
  }

  public void setXMLFile(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  public void setJavaFile(File javaFile) {
    this.javaFile = javaFile;
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

  private static List<ModelClassData> POTENTIAL_MODEL_CLASS_DATA_OPTIONS = null;

  public static ModelClassData getBestClassDataForJointList(List<Tuple2<String, String>> jointList) {
    if (POTENTIAL_MODEL_CLASS_DATA_OPTIONS == null) {
      POTENTIAL_MODEL_CLASS_DATA_OPTIONS = new LinkedList<ModelClassData>();
      Field[] dataFields = AliceResourceClassUtilities.getFieldsOfType(ModelClassData.class, ModelClassData.class);
      for (Field f : dataFields) {
        ModelClassData data = null;
        try {
          Object o = f.get(null);
          if ((o != null) && (o instanceof ModelClassData modelClassData)) {
            data = modelClassData;
          }
        } catch (Exception e) {
        }
        if (data != null) {
          POTENTIAL_MODEL_CLASS_DATA_OPTIONS.add(data);
        }
      }
    }

    int highScore = Integer.MIN_VALUE;
    ModelClassData bestFit = null;
    for (ModelClassData mcd : POTENTIAL_MODEL_CLASS_DATA_OPTIONS) {
      List<Tuple2<String, String>> modelDataJoints = getExistingJointIdPairs(mcd.superClass);
      int score = -Math.abs(modelDataJoints.size() - jointList.size());
      for (Tuple2<String, String> inputPair : jointList) {
        for (Tuple2<String, String> testPair : modelDataJoints) {
          if (inputPair.equals(testPair)) {
            score++;
            break;
          }
        }
      }
      if (score > highScore) {
        highScore = score;
        bestFit = mcd;
      }
    }
    return bestFit;
  }

  static List<Tuple2<String, String>> getExistingJointIdPairs(Class<?> resourceClass) {
    List<Tuple2<String, String>> ids = new LinkedList<Tuple2<String, String>>();
    Field[] fields = resourceClass.getDeclaredFields();
    for (Field f : fields) {
      if (JointId.class.isAssignableFrom(f.getType())) {
        String fieldName = f.getName();
        String parentName = null;
        JointId fieldData = null;
        try {
          Object o = f.get(null);
          if ((o != null) && (o instanceof JointId id)) {
            fieldData = id;
          }
        } catch (Exception e) {
        }
        if ((fieldData != null) && (fieldData.getParent() != null)) {
          parentName = fieldData.getParent().toString();
        }

        ids.add(Tuple2.createInstance(fieldName, parentName));
      }
    }
    Class<?>[] interfaces = resourceClass.getInterfaces();
    for (Class<?> i : interfaces) {
      ids.addAll(getExistingJointIdPairs(i));
    }
    return ids;
  }

  static List<String> getExistingJointIds(Class<?> resourceClass) {
    List<String> ids = new LinkedList<String>();
    Field[] fields = resourceClass.getDeclaredFields();
    for (Field f : fields) {
      if (JointId.class.isAssignableFrom(f.getType())) {
        String fieldName = f.getName();
        ids.add(fieldName);
      }
    }
    Class<?>[] interfaces = resourceClass.getInterfaces();
    for (Class<?> i : interfaces) {
      ids.addAll(getExistingJointIds(i));
    }
    return ids;
  }

  Field getJointRootsField(Class<?> cls) {
    if (cls == null) {
      return null;
    }
    Field[] rootFields = AliceResourceClassUtilities.getFieldsOfType(cls, JointId[].class);
    if (rootFields.length == 1) {
      return rootFields[0];
    } else {
      Class[] interfaces = cls.getInterfaces();
      for (Class i : interfaces) {
        Field rootField = getJointRootsField(i);
        if (rootField != null) {
          return rootField;
        }
      }
    }
    return null;
  }

  boolean needsToDefineRootsMethod(Class<?> cls) {
    if (cls == null) {
      return false;
    }
    Method[] methods = cls.getMethods();
    for (Method m : methods) {
      if (JointId[].class.isAssignableFrom(m.getReturnType())) {
        return true;
      }
    }
    Class[] interfaces = cls.getInterfaces();
    for (Class i : interfaces) {
      boolean needToDefineMethod = needsToDefineRootsMethod(i);
      if (needToDefineMethod) {
        return needToDefineMethod;
      }
    }
    return false;
  }

  public static String getAccessorMethodsForResourceClass(Class<? extends JointedModelResource> resourceClass) {
    StringBuilder sb = new StringBuilder();
    List<String> jointIds = getExistingJointIds(resourceClass);
    for (String id : jointIds) {
      sb.append("public Joint get" + AliceResourceClassUtilities.getAliceMethodNameForEnum(id) + "() {\n");
      sb.append("\t return org.lgna.story.Joint.getJoint( this, " + resourceClass.getCanonicalName() + "." + id + ");\n");
      sb.append("}\n");
    }
    return sb.toString();
  }

  private static String createResourceEnumName(ModelResourceExporter parentExporter, String modelName, String textureName) {
    if (modelName.equalsIgnoreCase(parentExporter.getClassName())) {
      return AliceResourceUtilities.makeEnumName(textureName);
    }
    String modelEnumName = AliceResourceUtilities.makeEnumName(modelName);
    if (modelName.equalsIgnoreCase(textureName) || textureName.equalsIgnoreCase(AliceResourceUtilities.getDefaultTextureEnumName(modelName)) || textureName.equalsIgnoreCase(modelEnumName)) {
      return modelEnumName;
    } else {
      return modelEnumName + "_" + AliceResourceUtilities.makeEnumName(textureName);
    }
  }

  static String createResourceEnumName(ModelResourceExporter parentExporter, ModelSubResourceExporter resource) {
    return createResourceEnumName(parentExporter, resource.getModelName(), resource.getTextureName());
  }

  public String createResourceEnumName(String modelName, String textureName) {
    return createResourceEnumName(this, modelName, textureName);
  }

  boolean shouldSuppressJoint(String jointString) {
    return ModelResourceJavaGenerator.shouldSuppressJoint(jointString, this.jointIdsToSuppress);
  }

  boolean shouldSuppressJointInArray(String jointString, Map<String, List<String>> arrayEntries) {
    return ModelResourceJavaGenerator.shouldSuppressJointInArray(jointString, arrayEntries, this.arraysToExposeFirstElementOf);
  }

  boolean shouldHideJointInArray(String jointString, Map<String, List<String>> arrayEntries) {
    return ModelResourceJavaGenerator.shouldHideJointInArray(jointString, arrayEntries, this.arraysToHideElementsOf);
  }

  public boolean shouldHideJointsOfArray(String arrayName) {
    return this.arraysToHideElementsOf.contains(arrayName);
  }

  String getJointAccessMethodNameForArrayJoint(String jointName) {
    String arrayName = getArrayNameForJoint(jointName, null, null);
    return ModelResourceJavaGenerator.getAccessorMethodName(arrayName);
  }

  private String getAccessorMethodName(String arrayName) {
    return "get" + AliceResourceUtilities.enumToCamelCase(arrayName);
  }

  //If a parent interface has declared an accessor for a given field, return true
  // otherwisse return false
  private boolean needsAccessorMethodForFieldName(ModelClassData classData, String fieldName) {
    return ModelResourceJavaGenerator.needsAccessorMethodForFieldName(classData, fieldName);
  }

  public String createJavaCode() throws DataFormatException {
    return ModelResourceJavaGenerator.buildJavaCodeBody(this);
  }

  String getJavaClassName() {
    return this.className + AliceResourceClassUtilities.RESOURCE_SUFFIX;
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


  private File createJavaCode(String root) throws DataFormatException {
    String packageDirectory = JavaCodeUtilities.getDirectoryStringForPackage(this.classData.packageString);
    System.out.println(packageDirectory);
    String javaCode = createJavaCode();
    System.out.println(javaCode);
    System.out.println(System.getProperty("java.class.path"));
    File javaFile = ModelResourceFileUtilities.getJavaFile(root, this.classData.packageString, getJavaClassName());
    TextFileUtilities.write(javaFile, javaCode);
    return javaFile;
  }

  String createXMLString() {
    return ModelResourceXmlGenerator.createXMLString(this);
  }

  File createXMLFile(String root, boolean forceRebuild) throws IOException {
    File outputFile = ModelResourceFileUtilities.getXMLFile(root, this.classData.packageString, this.className);
    ModelResourceFileUtilities.ensureOutputFile(outputFile, "XML resource");
    if (!forceRebuild && (this.xmlFile != null) && this.xmlFile.exists()) {
      FileUtilities.copyFile(this.xmlFile, outputFile);
      return outputFile;
    } else {
      //This path does not indent the xml
      //            Document doc = this.createXMLDocument();
      //            XMLUtilities.write(doc, outputFile);

      //This path does indenting
      String xmlString = this.createXMLString();
      try (FileWriter fw = new FileWriter(outputFile)) {
        fw.write(xmlString);
      }

      return outputFile;
    }

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
    if (this.forcedEnumNamesMap.containsKey(modelName)) {
      List<String> validEnums = this.forcedEnumNamesMap.get(modelName);
      for (String e : validEnums) {
        String otherToCheck = modelName.toUpperCase() + "_" + e;
        if (e.equalsIgnoreCase(enumName) || otherToCheck.equalsIgnoreCase(enumName)) {
          return true;
        }
      }
      return false;
    }
    //If we aren't forcing any enum names then all enum names are valid
    if (this.forcedOverridingEnumNames.isEmpty()) {
      return true;
    }
    for (String e : this.forcedOverridingEnumNames) {
      if (e.equalsIgnoreCase(enumName)) {
        return true;
      }
    }
    return false;
  }

  public void setArrayNamesToSkip(String[] arrayNamesToSkip) {
    this.arrayNamesToSkip = arrayNamesToSkip;
  }

  public void addCustomArrayName(String arrayName, String customName) {
    this.customArrayNameMap.put(arrayName, customName);
  }

  public void addCustomArrayNames(Map<String, String> customArrayNames) {
    for (Entry<String, String> entry : customArrayNames.entrySet()) {
      this.customArrayNameMap.put(entry.getKey(), entry.getValue());
    }
  }

  public void addForcedEnumNames(String resourceName, List<String> enumNames) {
    if ((enumNames != null) && (!enumNames.isEmpty())) {
      if (resourceName == null) {
        this.forcedOverridingEnumNames.addAll(enumNames);
      } else {
        List<String> nameList;
        if (!this.forcedEnumNamesMap.containsKey(resourceName)) {
          nameList = new ArrayList<String>();
          this.forcedEnumNamesMap.put(resourceName, nameList);
        } else {
          nameList = this.forcedEnumNamesMap.get(resourceName);
        }
        nameList.addAll(enumNames);
      }
    }
  }

  /*
   * public Joint getRightWrist() {
   * return org.lgna.story.Joint.getJoint( this, org.lgna.story.resources.BipedResource.RIGHT_WRIST );
   * }
   */

  public static String getJointAccessCodeForClass(Class<?> resourceClass) {
    List<String> ids = getExistingJointIds(resourceClass);
    StringBuilder sb = new StringBuilder();
    for (String id : ids) {
      sb.append("public org.lgna.story.Joint get" + AliceResourceClassUtilities.getAliceMethodNameForEnum(id) + "() {\n");
      sb.append("\treturn org.lgna.story.Joint.getJoint( this, " + resourceClass.getName() + "." + id + " );\n");
      sb.append("}\n");
    }

    return sb.toString();

  }

  public static void main(String[] args) {
    System.out.println(getJointAccessCodeForClass(BipedResource.class));
  }
}
