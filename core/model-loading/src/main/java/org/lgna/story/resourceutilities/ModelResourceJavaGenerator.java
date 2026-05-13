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

import edu.cmu.cs.dennisc.java.lang.reflect.ReflectionUtilities;
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
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.lgna.story.resources.BipedResource;
import org.lgna.story.resources.FlyerResource;
import org.lgna.story.resources.ImplementationAndVisualType;
import org.lgna.story.resources.JointArrayId;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.QuadrupedResource;
import org.lgna.story.resources.SlithererResource;
import org.lgna.story.resources.SwimmerResource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final class ModelResourceJavaGenerator {
  private ModelResourceJavaGenerator() {
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
    List<Method> methods = new LinkedList<Method>();
    for (Method method : superClass.getMethods()) {
      if (returnType.isAssignableFrom(method.getReturnType()) && method.getDeclaringClass().isInterface()) {
        methods.add(method);
      }
    }
    return methods;
  }

  static List<String> getMandatoryJointArrayNames(Class<?> superClass) {
    List<String> methodNames = new LinkedList<String>();
    for (Method method : getMandatoryMethods(superClass, JointId[].class)) {
      methodNames.add(method.getName());
    }
    List<String> arrayNames = new LinkedList<String>();
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
    List<String> methodNames = new LinkedList<String>();
    for (Method method : getMandatoryMethods(superClass, Pose.class)) {
      methodNames.add(method.getName());
    }
    List<String> poseNames = new LinkedList<String>();
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
    List<String> fieldNames = new LinkedList<String>();
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
    sb.append("public enum " + exporter.getJavaClassName() + " implements " + classData.superClass.getCanonicalName() + " {" + JavaCodeUtilities.LINE_RETURN);
    appendEnumConstants(sb, exporter);
    sb.append(";" + JavaCodeUtilities.LINE_RETURN);
  }

  private static void appendEnumConstants(StringBuilder sb, ModelResourceExporter exporter) {
    assert !exporter.getSubResources().isEmpty();
    boolean isFirst = true;
    for (ModelSubResourceExporter resource : exporter.getSubResources()) {
      String resourceEnumName = ModelResourceExporter.createResourceEnumName(exporter, resource);
      if (exporter.isValidEnumName(resource.getModelName(), resourceEnumName)) {
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

  static boolean shouldSuppressJoint(String jointString, List<String> jointIdsToSuppress) {
    if (jointIdsToSuppress.contains(jointString)) {
      return true;
    }
    return ModelResourceJointTreeUtilities.isRootJoint(jointString);
  }

  static String getArrayNameFromMapForJoint(String jointString, Map<String, List<String>> arrayEntries) {
    for (Map.Entry<String, List<String>> entry : arrayEntries.entrySet()) {
      if (entry.getValue().contains(jointString)) {
        return entry.getKey();
      }
    }
    return null;
  }

  static boolean shouldSuppressJointInArray(String jointString,
      Map<String, List<String>> arrayEntries, List<String> arraysToExposeFirstElementOf) {
    for (Map.Entry<String, List<String>> entry : arrayEntries.entrySet()) {
      if (entry.getValue().contains(jointString)) {
        if (arraysToExposeFirstElementOf.contains(entry.getKey())
            && (ModelResourceArrayUtilities.getArrayIndexForJoint(jointString) == 0)) {
          return false;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  static boolean shouldHideJointInArray(String jointString,
      Map<String, List<String>> arrayEntries, List<String> arraysToHideElementsOf) {
    for (Map.Entry<String, List<String>> entry : arrayEntries.entrySet()) {
      if (entry.getValue().contains(jointString)) {
        if (arraysToHideElementsOf.contains(entry.getKey())) {
          return true;
        }
      }
    }
    return false;
  }
}
