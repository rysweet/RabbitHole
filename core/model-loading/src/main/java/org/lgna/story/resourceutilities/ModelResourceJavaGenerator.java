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

import edu.cmu.cs.dennisc.java.io.TextFileUtilities;
import edu.cmu.cs.dennisc.java.lang.reflect.ReflectionUtilities;
import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.UnitQuaternion;
import org.lgna.story.BipedPose;
import org.lgna.story.BipedPoseBuilder;
import org.lgna.story.FlyerPose;
import org.lgna.story.FlyerPoseBuilder;
import org.lgna.story.JointedModelPose;
import org.lgna.story.JointedModelPoseBuilder;
import org.lgna.story.Pose;
import org.lgna.story.QuadrupedPose;
import org.lgna.story.QuadrupedPoseBuilder;
import org.lgna.story.SlithererPose;
import org.lgna.story.SlithererPoseBuilder;
import org.lgna.story.SwimmerPose;
import org.lgna.story.SwimmerPoseBuilder;
import org.lgna.story.implementation.alice.AliceResourceClassUtilities;
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.lgna.story.resources.BipedResource;
import org.lgna.story.resources.FlyerResource;
import org.lgna.story.resources.ImplementationAndVisualType;
import org.lgna.story.resources.JointArrayId;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.JointedModelResource;
import org.lgna.story.resources.QuadrupedResource;
import org.lgna.story.resources.SlithererResource;
import org.lgna.story.resources.SwimmerResource;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.Map;

final class ModelResourceJavaGenerator {

  private static final boolean REMOVE_ROOT_JOINTS = false;
  static final String ROOT_IDS_FIELD_NAME = "JOINT_ID_ROOTS";
  static final String ROOT_IDS_METHOD_NAME = "getRootJointIds";

  private ModelResourceJavaGenerator() {
  }

  static String getJavaClassName(ModelResourceExporter exporter) {
    return exporter.getClassName() + AliceResourceClassUtilities.RESOURCE_SUFFIX;
  }

  static Field getJointRootsField(Class<?> cls) {
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

  static boolean needsToDefineRootsMethod(Class<?> cls) {
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

  static String createResourceEnumNameForModelAndTexture(ModelResourceExporter parentExporter, String modelName, String textureName) {
    return createResourceEnumName(parentExporter, modelName, textureName);
  }

  static boolean isValidEnumName(ModelResourceExporter exporter, String modelName, String enumName) {
    Map<String, List<String>> forcedEnumNamesMap = exporter.getForcedEnumNamesMap();
    if (forcedEnumNamesMap.containsKey(modelName)) {
      List<String> validEnums = forcedEnumNamesMap.get(modelName);
      for (String e : validEnums) {
        String otherToCheck = modelName.toUpperCase() + "_" + e;
        if (e.equalsIgnoreCase(enumName) || otherToCheck.equalsIgnoreCase(enumName)) {
          return true;
        }
      }
      return false;
    }
    List<String> forcedOverridingEnumNames = exporter.getForcedOverridingEnumNames();
    if (forcedOverridingEnumNames.isEmpty()) {
      return true;
    }
    for (String e : forcedOverridingEnumNames) {
      if (e.equalsIgnoreCase(enumName)) {
        return true;
      }
    }
    return false;
  }

  static List<Tuple2<String, String>> makeCodeReadyTree(List<Tuple2<String, String>> sourceList) {
    return ModelResourceJointTreeUtilities.makeCodeReadyTree(sourceList, REMOVE_ROOT_JOINTS);
  }

  static String getJointAccessMethodNameForArrayJoint(String jointName) {
    String arrayName = ModelResourceArrayUtilities.getArrayNameForJoint(jointName, null, null);
    return getAccessorMethodName(arrayName);
  }

  static File createJavaFile(ModelResourceExporter exporter, String root) throws DataFormatException {
    String javaCode = buildJavaCodeBody(exporter);
    File javaFile = ModelResourceFileUtilities.getJavaFile(root, exporter.getPackageString(), getJavaClassName(exporter));
    TextFileUtilities.write(javaFile, javaCode);
    return javaFile;
  }

  static String getAccessorMethodName(String arrayName) {
    return "get" + AliceResourceUtilities.enumToCamelCase(arrayName);
  }

  static boolean needsAccessorMethodForFieldName(ModelClassData classData, String fieldName) {
    String fieldAccessorMethodName = getAccessorMethodName(fieldName);
    try {
      Method m = classData.superClass.getMethod(fieldAccessorMethodName);
      if ((m != null) && m.getDeclaringClass().isInterface()) {
        return true;
      }
    } catch (NoSuchMethodException me) {
    }
    return false;
  }

  static List<Method> getMandatoryMethods(Class<?> superClass, Class<?> returnType) {
    List<Method> methods = new ArrayList<>();
    for (Method method : superClass.getMethods()) {
      if (returnType.isAssignableFrom(method.getReturnType()) && method.getDeclaringClass().isInterface()) {
        methods.add(method);
      }
    }
    return methods;
  }

  static List<String> getMandatoryJointArrayNames(Class<?> superClass) {
    List<String> methodNames = new ArrayList<>();
    for (Method method : getMandatoryMethods(superClass, JointId[].class)) {
      methodNames.add(method.getName());
    }
    List<String> arrayNames = new ArrayList<>();
    for (String methodName : methodNames) {
      int index = methodName.indexOf("get");
      if ((index == 0)) {
        if (!methodName.equals("getRootJointIds")) {
          String newName = methodName.substring(3);
          int arrayIndex = newName.indexOf("Array");
          if (arrayIndex != -1) {
            newName = newName.substring(0, arrayIndex);
          }
          newName = AliceResourceUtilities.makeEnumName(newName);
          arrayNames.add(newName);
        }
      } else {
        System.err.println("FROM " + superClass
            + ": UNABLE TO CONVERT " + methodName + " INTO AN ARRAY NAME.");
      }
    }
    return arrayNames;
  }

  static List<String> getMandatoryPoseNames(Class<?> superClass) {
    List<String> methodNames = new ArrayList<>();
    for (Method method : getMandatoryMethods(superClass, Pose.class)) {
      methodNames.add(method.getName());
    }
    List<String> poseNames = new ArrayList<>();
    for (String methodName : methodNames) {
      int index = methodName.indexOf("get");
      if ((index == 0)) {
        String newName = methodName.substring(3);
        int arrayIndex = newName.indexOf("Pose");
        if (arrayIndex != -1) {
          newName = newName.substring(0, arrayIndex);
        }
        newName = AliceResourceUtilities.makeEnumName(newName);
        poseNames.add(newName);
      } else {
        System.err.println("FROM " + superClass
            + ": UNABLE TO CONVERT " + methodName + " INTO POSE NAME.");
      }
    }
    return poseNames;
  }

  static Class<?> getPoseBuilderTypeForSuperClass(Class<?> superClass) {
    if (FlyerResource.class.isAssignableFrom(superClass)) {
      return FlyerPoseBuilder.class;
    } else if (BipedResource.class.isAssignableFrom(superClass)) {
      return BipedPoseBuilder.class;
    } else if (QuadrupedResource.class.isAssignableFrom(superClass)) {
      return QuadrupedPoseBuilder.class;
    } else if (SwimmerResource.class.isAssignableFrom(superClass)) {
      return SwimmerPoseBuilder.class;
    } else if (SlithererResource.class.isAssignableFrom(superClass)) {
      return SlithererPoseBuilder.class;
    }
    return JointedModelPoseBuilder.class;
  }

  static Class<?> getPoseTypeForSuperClass(Class<?> superClass) {
    if (FlyerResource.class.isAssignableFrom(superClass)) {
      return FlyerPose.class;
    } else if (BipedResource.class.isAssignableFrom(superClass)) {
      return BipedPose.class;
    } else if (QuadrupedResource.class.isAssignableFrom(superClass)) {
      return QuadrupedPose.class;
    } else if (SwimmerResource.class.isAssignableFrom(superClass)) {
      return SwimmerPose.class;
    } else if (SlithererResource.class.isAssignableFrom(superClass)) {
      return SlithererPose.class;
    }
    return JointedModelPose.class;
  }

  static List<String> getAlreadyDeclaredJointArrayNames(Class<?> superClass) {
    List<String> fieldNames = new ArrayList<>();
    for (Field field : ReflectionUtilities.getPublicStaticFinalFields(superClass, JointArrayId.class)) {
      fieldNames.add(field.getName());
    }
    return fieldNames;
  }

  static void appendPreambleAndEnumConstants(StringBuilder sb, ModelResourceExporter exporter) {
    ModelClassData classData = exporter.getClassData();
    sb.append(JavaCodeUtilities.getCopyrightComment());
    sb.append(JavaCodeUtilities.LINE_RETURN);
    sb.append("package " + classData.packageString + ";" + JavaCodeUtilities.LINE_RETURN + JavaCodeUtilities.LINE_RETURN);
    sb.append("import org.lgna.project.annotations.*;" + JavaCodeUtilities.LINE_RETURN);
    sb.append("import org.lgna.story.implementation.JointIdTransformationPair;" + JavaCodeUtilities.LINE_RETURN);
    sb.append("import org.lgna.story.Orientation;" + JavaCodeUtilities.LINE_RETURN);
    sb.append("import org.lgna.story.Position;" + JavaCodeUtilities.LINE_RETURN);
    sb.append("import org.lgna.story.resources.ImplementationAndVisualType;" + JavaCodeUtilities.LINE_RETURN + JavaCodeUtilities.LINE_RETURN);
    if (exporter.isDeprecated()) {
      sb.append("@Deprecated" + JavaCodeUtilities.LINE_RETURN);
    }
    sb.append("public enum " + getJavaClassName(exporter) + " implements " + classData.superClass.getCanonicalName() + " {" + JavaCodeUtilities.LINE_RETURN);
    appendEnumConstants(sb, exporter);
    sb.append(";" + JavaCodeUtilities.LINE_RETURN);
  }

  private static void appendEnumConstants(StringBuilder sb, ModelResourceExporter exporter) {
    assert !exporter.getSubResources().isEmpty();
    boolean isFirst = true;
    for (ModelSubResourceExporter resource : exporter.getSubResources()) {
      String resourceEnumName = createResourceEnumName(exporter, resource);
      if (isValidEnumName(exporter, resource.getModelName(), resourceEnumName)) {
        if (!isFirst) {
          sb.append("," + JavaCodeUtilities.LINE_RETURN);
        }
        String typeString = "";
        if (!resource.getTypeString().equals(ImplementationAndVisualType.ALICE.toString())) {
          typeString = "( ImplementationAndVisualType." + resource.getTypeString() + " )";
        }
        sb.append("\t" + resourceEnumName + typeString);
        isFirst = false;
      } else {
        System.out.println("SKIPPING ENUM NAME: " + resourceEnumName);
      }
    }
  }

  static String buildJavaCodeBody(ModelResourceExporter exporter) throws java.util.zip.DataFormatException {
      StringBuilder sb = new StringBuilder();

      ModelResourceJavaGenerator.appendPreambleAndEnumConstants(sb, exporter);
      Set<String> existingIds = new HashSet<>(getExistingJointIds(exporter.getClassData().superClass));
      boolean addedRoots = false;
      List<Tuple2<String, String>> trimmedSkeleton = makeCodeReadyTree(exporter.getJointList());
      if (trimmedSkeleton != null) {
        Map<String, List<String>> arrayEntries;
        if (exporter.isEnableArraySupport()) {
          arrayEntries = ModelResourceArrayUtilities.getArrayEntriesFromJointList(trimmedSkeleton, exporter.getCustomArrayNameMap(), exporter.getJointIdsToSuppress(), exporter.getArrayNamesToSkip());
        } else {
          arrayEntries = new HashMap<>();
        }

        // Pre-compute reverse lookup: joint name → array name (O(1) instead of O(n) per query)
        Map<String, String> jointToArrayName = new HashMap<>();
        for (Map.Entry<String, List<String>> ae : arrayEntries.entrySet()) {
          for (String joint : ae.getValue()) {
            jointToArrayName.put(joint, ae.getKey());
          }
        }
        Set<String> suppressJointIds = new HashSet<>(exporter.getJointIdsToSuppress());
        Set<String> hideElementArrays = new HashSet<>(exporter.getArraysToHideElementsOf());
        Set<String> exposeFirstArrays = new HashSet<>(exporter.getArraysToExposeFirstElementOf());

        Map<String, Map<String, AffineMatrix4x4>> poseEntries = new HashMap<>(exporter.getPoses());
        List<String> rootJoints = new ArrayList<>();
        sb.append(JavaCodeUtilities.LINE_RETURN);
        for (Tuple2<String, String> entry : trimmedSkeleton) {
          String jointString = entry.getA();
          String parentString = entry.getB();
          if (existingIds.contains(jointString)) {
            continue;
          }
          String arrayNameForJoint = jointToArrayName.get(jointString);
          boolean hiddenInArray = (arrayNameForJoint != null) && hideElementArrays.contains(arrayNameForJoint);
          if (!hiddenInArray) {
            if ((parentString == null) || (parentString.length() == 0)) {
              parentString = "null";
              rootJoints.add(jointString);
              addedRoots = true;
            }
            boolean suppressJoint = suppressJointIds.contains(jointString) || ModelResourceJointTreeUtilities.isRootJoint(jointString);
            boolean suppressInArray = (arrayNameForJoint != null)
                && !(exposeFirstArrays.contains(arrayNameForJoint) && ModelResourceArrayUtilities.getArrayIndexForJoint(jointString) == 0);
            if (suppressJoint || suppressInArray) {
              sb.append("@FieldTemplate(visibility=Visibility.COMPLETELY_HIDDEN)" + JavaCodeUtilities.LINE_RETURN);
            } else {
              if (arrayNameForJoint != null) {
                sb.append("@FieldTemplate(visibility=Visibility.PRIME_TIME, methodNameHint=\"" + getJointAccessMethodNameForArrayJoint(jointString) + "\")" + JavaCodeUtilities.LINE_RETURN);
              } else {
                sb.append("@FieldTemplate(visibility=Visibility.PRIME_TIME)" + JavaCodeUtilities.LINE_RETURN);
              }
            }
            sb.append("\tpublic static final org.lgna.story.resources.JointId " + jointString + " = new org.lgna.story.resources.JointId( " + parentString + ", " + getJavaClassName(exporter) + ".class );" + JavaCodeUtilities.LINE_RETURN);
          }
        }

        if (addedRoots) {
          sb.append("\n@FieldTemplate( visibility = org.lgna.project.annotations.Visibility.COMPLETELY_HIDDEN )");
          sb.append("\n\tpublic static final org.lgna.story.resources.JointId[] " + ROOT_IDS_FIELD_NAME + " = { ");
          for (int i = 0; i < rootJoints.size(); i++) {
            sb.append(rootJoints.get(i));
            if (i < (rootJoints.size() - 1)) {
              sb.append(", ");
            }
          }
          sb.append(" };" + JavaCodeUtilities.LINE_RETURN);
        }
        //Handle pose code
        List<String> mandatoryPoseNames = ModelResourceJavaGenerator.getMandatoryPoseNames(exporter.getClassData().superClass);
        if (!poseEntries.isEmpty() || (!mandatoryPoseNames.isEmpty())) {
          for (String mandatoryPose : mandatoryPoseNames) {
            if (!poseEntries.containsKey(mandatoryPose)) {
              throw new DataFormatException("Missing pose definition for " + mandatoryPose + " on class " + exporter.getClassData().superClass);
            }
          }
          for (Entry<String, Map<String, AffineMatrix4x4>> poseEntry : poseEntries.entrySet()) {
            Map<String, AffineMatrix4x4> poseData = poseEntry.getValue();
            if (poseData.isEmpty()) {
              throw new DataFormatException("No pose data for " + poseEntry.getKey() + " on class " + exporter.getClassData().superClass);
            }
            String fullPoseName = poseEntry.getKey() + "_POSE";

            boolean needsAccessor = ModelResourceJavaGenerator.needsAccessorMethodForFieldName(exporter.getClassData(), fullPoseName);

            //If an accessor is needed, add a "COMPLETELY_HIDDEN" annotation.
            // The accessor is used to retrieve the pose via a parent class and therefore the pose itself is essentially already handled
            // If there is no accessor, then we want the code generation system to create an Alice level accessor at runtime (which this annotation prevents)
            if (needsAccessor) {
              sb.append("\n\t@FieldTemplate( visibility = org.lgna.project.annotations.Visibility.COMPLETELY_HIDDEN )");
            }

            //          Class poseType = ModelResourceJavaGenerator.getPoseTypeForSuperClass( exporter.getClassData().superClass );
            Class poseType = JointedModelPose.class;
            Class poseBuilderType = ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(exporter.getClassData().superClass);
            String poseTypeString = poseType.getName();
            sb.append("\n\tpublic static final " + poseTypeString + " " + fullPoseName + " = new " + poseTypeString + "( ");
            sb.append(JavaCodeUtilities.LINE_RETURN);
            int count = 0;
            for (Entry<String, AffineMatrix4x4> poseDataEntry : poseData.entrySet()) {
              count++;
              UnitQuaternion quat = poseDataEntry.getValue().orientation().asUnitQuaternion();
              Point3 pos = poseDataEntry.getValue().translation();
              sb.append("\t\tnew JointIdTransformationPair( " + poseDataEntry.getKey() + ", new Orientation(" + quat.x() + ", " + quat.y() + ", " + quat.z() + ", " + quat.w() + "), new Position(" + pos.x() + ", " + pos.y() + ", " + pos.z() + ") )");
              if (count != poseData.size()) {
                sb.append(",");
              }
              sb.append(JavaCodeUtilities.LINE_RETURN);
            }
            sb.append("\t);" + JavaCodeUtilities.LINE_RETURN + JavaCodeUtilities.LINE_RETURN);
            if (needsAccessor) {
              String poseAccessorName = ModelResourceJavaGenerator.getAccessorMethodName(fullPoseName);
              sb.append("\tpublic " + poseType.getName() + " " + poseAccessorName + "(){" + JavaCodeUtilities.LINE_RETURN);
              sb.append("\t\treturn " + getJavaClassName(exporter) + "." + fullPoseName + ";" + JavaCodeUtilities.LINE_RETURN);
              sb.append("\t}" + JavaCodeUtilities.LINE_RETURN);
            }
          }
        }
        //Handle array code
        List<String> mandatoryArrayNames = ModelResourceJavaGenerator.getMandatoryJointArrayNames(exporter.getClassData().superClass);
        List<String> declaredArrays = ModelResourceJavaGenerator.getAlreadyDeclaredJointArrayNames(exporter.getClassData().superClass);
        if (!arrayEntries.isEmpty() || (!mandatoryArrayNames.isEmpty())) {
          //Loop through and remove any existing arrays from the mandatory array list
          // This should leave only the mandatory arrays that need an empty list defined
          for (Entry<String, List<String>> arrayEntry : arrayEntries.entrySet()) {
            if (mandatoryArrayNames.contains(arrayEntry.getKey())) {
              mandatoryArrayNames.remove(arrayEntry.getKey());
            }
          }
          for (String mandatoryArray : mandatoryArrayNames) {
            arrayEntries.put(mandatoryArray, new ArrayList<>());
          }
          for (Entry<String, List<String>> arrayEntry : arrayEntries.entrySet()) {
            List<String> arrayElements = arrayEntry.getValue();
            String fullArrayName = arrayEntry.getKey() + "_ARRAY";

            if (declaredArrays.contains(fullArrayName) || declaredArrays.contains(arrayEntry.getKey())) {
              //If the array is already declared, skip it and trust that the previous declaration will capture the data
              continue;
            }

            boolean needsAccessor = ModelResourceJavaGenerator.needsAccessorMethodForFieldName(exporter.getClassData(), fullArrayName);

            //If an accessor is needed, add a "COMPLETELY_HIDDEN" annotation.
            // The accessor is used to retrieve the array via a parent class and therefore the array itself is essentially already handled
            // If there is no accessor, then we want the code generation system to create an Alice level accessor at runtime (which this annotation prevents)
            if (needsAccessor) {
              sb.append("\n\t@FieldTemplate( visibility = org.lgna.project.annotations.Visibility.COMPLETELY_HIDDEN )");
            }

            //If the array is one in the "hide all the elements of this array" list, then declare it as an arrayId rather than an array of joint ids
            if (hideElementArrays.contains(fullArrayName) || hideElementArrays.contains(arrayEntry.getKey())) {
              String firstEntry = arrayElements.getFirst();
              String parentString = "null";
              for (Tuple2<String, String> entry : trimmedSkeleton) {
                if (entry.getA().equals(firstEntry)) {
                  parentString = entry.getB();
                  break;
                }
              }
              sb.append("@FieldTemplate(visibility=Visibility.PRIME_TIME)" + JavaCodeUtilities.LINE_RETURN);
              sb.append("\tpublic static final org.lgna.story.resources.JointArrayId " + fullArrayName + " = new org.lgna.story.resources.JointArrayId( \"" + arrayEntry.getKey() + "\", " + parentString + ", " + getJavaClassName(exporter) + ".class );" + JavaCodeUtilities.LINE_RETURN);
            } else {
              sb.append("\n\tpublic static final org.lgna.story.resources.JointId[] " + fullArrayName + " = { ");
              for (int i = 0; i < arrayElements.size(); i++) {
                sb.append(arrayElements.get(i));
                if (i < (arrayElements.size() - 1)) {
                  sb.append(", ");
                }
              }
              sb.append(" };" + JavaCodeUtilities.LINE_RETURN);
            }
            if (needsAccessor) {
              String arrayAccessorName = ModelResourceJavaGenerator.getAccessorMethodName(fullArrayName);
              sb.append("\tpublic org.lgna.story.resources.JointId[] " + arrayAccessorName + "(){" + JavaCodeUtilities.LINE_RETURN);
              sb.append("\t\treturn " + getJavaClassName(exporter) + "." + fullArrayName + ";" + JavaCodeUtilities.LINE_RETURN);
              sb.append("\t}" + JavaCodeUtilities.LINE_RETURN);
            }
          }
        }
      }
      sb.append(JavaCodeUtilities.LINE_RETURN);
      sb.append("\tprivate final ImplementationAndVisualType resourceType;" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\tprivate " + getJavaClassName(exporter) + "() {" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t\tthis( ImplementationAndVisualType.ALICE );" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t}" + JavaCodeUtilities.LINE_RETURN + JavaCodeUtilities.LINE_RETURN);
      sb.append("\tprivate " + getJavaClassName(exporter) + "( ImplementationAndVisualType resourceType ) {" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t\tthis.resourceType = resourceType;" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t}" + JavaCodeUtilities.LINE_RETURN + JavaCodeUtilities.LINE_RETURN);
      if (needsToDefineRootsMethod(exporter.getClassData().superClass)) {
        sb.append("\tpublic org.lgna.story.resources.JointId[] " + ROOT_IDS_METHOD_NAME + "(){" + JavaCodeUtilities.LINE_RETURN);
        if (addedRoots) {
          sb.append("\t\treturn " + getJavaClassName(exporter) + "." + ROOT_IDS_FIELD_NAME + ";" + JavaCodeUtilities.LINE_RETURN);
        } else {
          Field rootsField = getJointRootsField(exporter.getClassData().superClass);
          if (rootsField != null) {
            sb.append("\t\treturn " + rootsField.getDeclaringClass().getCanonicalName() + "." + rootsField.getName() + ";" + JavaCodeUtilities.LINE_RETURN);
          } else {
            sb.append("\t\treturn new org.lgna.story.resources.JointId[0];" + JavaCodeUtilities.LINE_RETURN);
          }
        }
        sb.append("\t}" + JavaCodeUtilities.LINE_RETURN);
      }
      sb.append("\n\tpublic org.lgna.story.implementation.JointedModelImp.JointImplementationAndVisualDataFactory<org.lgna.story.resources.JointedModelResource> getImplementationAndVisualFactory() {" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t\treturn this.resourceType.getFactory( this );" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t}" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\tpublic " + exporter.getClassData().implementationClass.getCanonicalName() + " createImplementation( " + exporter.getClassData().abstractionClass.getCanonicalName() + " abstraction ) {" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t\treturn new " + exporter.getClassData().implementationClass.getCanonicalName() + "( abstraction, this.resourceType.getFactory( this ) );" + JavaCodeUtilities.LINE_RETURN);
      sb.append("\t}" + JavaCodeUtilities.LINE_RETURN);
      sb.append("}" + JavaCodeUtilities.LINE_RETURN);

      return sb.toString();
  }

  static List<String> getExistingJointIds(Class<?> resourceClass) {
    List<String> ids = new ArrayList<>();
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

  static String getAccessorMethodsForResourceClass(Class<? extends JointedModelResource> resourceClass) {
    StringBuilder sb = new StringBuilder();
    List<String> jointIds = getExistingJointIds(resourceClass);
    for (String id : jointIds) {
      sb.append("public Joint get" + AliceResourceClassUtilities.getAliceMethodNameForEnum(id) + "() {\n");
      sb.append("\t return org.lgna.story.Joint.getJoint( this, " + resourceClass.getCanonicalName() + "." + id + ");\n");
      sb.append("}\n");
    }
    return sb.toString();
  }

  static String getJointAccessCodeForClass(Class<?> resourceClass) {
    List<String> ids = getExistingJointIds(resourceClass);
    StringBuilder sb = new StringBuilder();
    for (String id : ids) {
      sb.append("public org.lgna.story.Joint get" + AliceResourceClassUtilities.getAliceMethodNameForEnum(id) + "() {\n");
      sb.append("\treturn org.lgna.story.Joint.getJoint( this, " + resourceClass.getName() + "." + id + " );\n");
      sb.append("}\n");
    }
    return sb.toString();
  }
}
