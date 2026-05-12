package org.alice.serialization.tweedle;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Tuple3;
import org.alice.math.immutable.UnitQuaternion;
import org.lgna.project.annotations.FieldTemplate;
import org.lgna.project.code.IdentifiableTweedleNode;
import org.lgna.project.code.InstantiableTweedleNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Companion class that encapsulates resource-encoding logic extracted from
 * {@link TweedleEncoder}. Delegates back to the owning encoder for
 * super-class calls and protected formatting methods via package-private
 * bridge methods.
 */
class ResourceEncoder {

  private final TweedleEncoder encoder;

  ResourceEncoder(TweedleEncoder encoder) {
    this.encoder = encoder;
  }

  void processResourceType(String jointedModelResource) {
    try {
      Class<?> resourceClass = Class.forName(jointedModelResource);
      final String superclass = resourceClass.getInterfaces().length == 1 ? resourceClass.getInterfaces()[0].getSimpleName() : "JointedModelInterface";
      encoder.forwardGetCodeStringBuilder().append("class ").append(resourceClass.getSimpleName()).append(" extends ").append(superclass);
      encoder.openBlock();
      appendResourceConstructor(superclass, resourceClass.getSimpleName());
      appendResourceFields(superclass, resourceClass);
      appendResourceInstances(resourceClass);
      encoder.appendClassFooter(jointedModelResource);
    } catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("Unable to find class " + jointedModelResource + " which should have been the caller type. This should not happen and yet it has.", cnfe);
    }
  }

  void processDynamicResource(String dynamicResourceClass, String variant, InstantiableTweedleNode[] addedJoints) {
    try {
      final String variantName = variant + "Resource";
      Class<?> parentClass = Class.forName(dynamicResourceClass);
      final String superclass = parentClass.getInterfaces().length == 1 ? parentClass.getInterfaces()[0].getSimpleName() : "JointedModelInterface";
      encoder.forwardGetCodeStringBuilder().append("class ").append(variantName).append(" extends ").append(superclass);
      encoder.openBlock();
      appendResourceConstructor(superclass, variantName);
      List<String> jointNames = new ArrayList<>();
      for (InstantiableTweedleNode joint : addedJoints) {
        final String jointIdentifier = getUserJointIdentifier(joint.toString());
        appendStaticField(null, "JointId", jointIdentifier, () -> joint.encodeDefinition(encoder));
        jointNames.add(jointIdentifier);
      }
      appendAddedJoints(superclass, jointNames);
      appendResourceInstance(variantName, "DEFAULT");
      encoder.appendClassFooter(dynamicResourceClass);
    } catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("Unable to find class " + dynamicResourceClass + " which should have been the caller type. This should not happen and yet it has.", cnfe);
    }
  }

  String getUserJointIdentifier(String jointIdentifier) {
    return "root".equalsIgnoreCase(jointIdentifier) ? jointIdentifier : TweedleEncoder.USER_PREFIX + jointIdentifier;
  }

  private void appendResourceConstructor(String superclass, String resourceName) {
    encoder.appendIndent();
    encoder.forwardAppendString(resourceName);
    encoder.forwardAppendString("(TextString name)");
    encoder.forwardBracketize(() -> {
      encoder.appendIndent();
      encoder.forwardAppendString("super(name: name");
      if ("FlyerResource".equals(superclass)) {
        encoder.forwardAppendString(",\n"
                         + "          spreadWingsPose: " + resourceName + ".SPREAD_WINGS_POSE,\n"
                         + "          foldWingsPose: " + resourceName + ".FOLD_WINGS_POSE,\n"
                         + "          tailArray: " + resourceName + ".TAIL_ARRAY,\n"
                         + "          neckArray: " + resourceName + ".NECK_ARRAY");
      }
      if ("QuadrupedResource".equals(superclass)) {
        encoder.forwardAppendString(", tailArray: " + resourceName + ".TAIL_ARRAY");
      }
      if ("SlithererResource".equals(superclass)) {
        encoder.forwardAppendString(", tailArray: " + resourceName + ".TAIL_ARRAY");
      }
      encoder.forwardAppendString(");\n");
    });
  }

  private void appendResourceInstances(Class<?> resourceClass) {
    if (!resourceClass.isEnum()) {
      return;
    }
    final String className = resourceClass.getSimpleName();
    final List<String> resourceNames = Arrays.stream(resourceClass.getEnumConstants()).map(Object::toString).toList();
    for (String resourceName: resourceNames) {
      appendResourceInstance(className, resourceName);
    }
  }

  private void appendResourceInstance(String className, String resourceName) {
    encoder.forwardAppendNewLine();
    encoder.appendIndent();
    encoder.forwardAppendString("static ");
    encoder.forwardAppendString(className);
    encoder.forwardAppendSpace();
    encoder.forwardAppendString(resourceName);
    encoder.appendAssignmentOperator();
    encoder.appendInstantiation(className, () -> encoder.appendArg("name", () -> encoder.forwardAppendEscapedString(className.substring(0, className.length() - 8) + "/" + resourceName)));
    encoder.appendStatementCompletion();
  }

  private void appendResourceFields(String superclass, Class<?> resourceClass) {
    Field[] fields = resourceClass.getDeclaredFields();
    List<String> newJoints = new ArrayList<>();
    for (Field field : fields) {
      try {
        Object value = field.get(resourceClass);
        if (value instanceof InstantiableTweedleNode node) {
          if (field.getType().getSimpleName().equals("JointId")) {
            newJoints.add(field.getName());
          }
          appendStaticField(field, () -> node.encodeDefinition(encoder));
        } else {
          if (value.getClass().isArray() && IdentifiableTweedleNode.class.isAssignableFrom(field.getType().getComponentType())) {
            Object[] values = (Object[]) value;
            appendStaticField(field, () -> {
              encoder.forwardAppendString("new ");
              encoder.forwardAppendString(field.getType().getSimpleName());
              encoder.appendList(values, (v) -> encoder.forwardAppendString(((IdentifiableTweedleNode) v).getCodeIdentifier(encoder)), encoder.getListSeparator());
            });
          } else {
            Logger.info("Export will skip non-generator field " + resourceClass.getSimpleName() + "." + field.getName());
          }
        }
      } catch (IllegalAccessException e) {
        Logger.info("Export will skip inaccessible field " + resourceClass.getSimpleName() + "." + field.getName());
      }
    }
    appendAddedJoints(superclass, newJoints);
  }

  private void appendAddedJoints(String superclass, Collection<String> newJoints) {
    if (newJoints.isEmpty()) {
      return;
    }
    encoder.forwardAppendNewLine();
    encoder.appendSingleCodeLine(() -> {
      encoder.forwardAppendString("@CompletelyHidden static JointId[] ADDED_JOINTS");
      encoder.appendAssignmentOperator();
      encoder.forwardAppendString("new JointId[]");
      encoder.appendList(newJoints.toArray(), (v) -> encoder.forwardAppendString((String) v), encoder.getListSeparator());
    });
    encoder.appendSingleCodeLine(() -> {
      encoder.forwardAppendString("@CompletelyHidden static JointId[] ALL_JOINTS");
      encoder.appendAssignmentOperator();
      encoder.forwardAppendString("concat(a: ");
      encoder.forwardAppendString(superclass);
      encoder.forwardAppendString(".EXPECTED_JOINTS, b: ADDED_JOINTS)");
    });
    encoder.forwardAppendNewLine();
    encoder.appendIndent();
    encoder.forwardAppendString("@CompletelyHidden JointId[] getJointIds() ");
    encoder.forwardBracketize(() -> encoder.appendSingleCodeLine(() -> encoder.forwardAppendString("return ALL_JOINTS")));
  }

  private void appendStaticField(Field field, Runnable value) {
    appendStaticField(field.getAnnotation(FieldTemplate.class),
                      field.getType().getSimpleName(),
                      field.getName(),
                      value);
  }

  private void appendStaticField(FieldTemplate annotation, String fieldType, String fieldName, Runnable value) {
    encoder.appendSingleCodeLine(() -> {
      encoder.appendVisibilityTag(annotation);
      encoder.forwardAppendString("static ");
      encoder.forwardAppendString(fieldType);
      encoder.forwardAppendSpace();
      encoder.forwardAppendString(fieldName);
      encoder.appendAssignmentOperator();
      value.run();
    });
  }

  void appendNewJointId(String joint, String parentReference) {
    encoder.appendInstantiation("JointId", () -> {
      encoder.appendArg("name", () -> encoder.quoteString(joint));
      encoder.appendAnotherArg("parent", parentReference);
    });
  }

  void appendNewJointArrayId(String pattern, String startingJoint) {
    encoder.appendInstantiation("JointArrayId", () -> {
      encoder.appendArg("root", startingJoint);
      encoder.appendAnotherArg("pattern", () -> encoder.quoteString(pattern));
    });
  }

  String getFieldReference(String type, String field) {
    return encoder.tweedleTypeName(type) + "." + field;
  }

  void appendNewPose(InstantiableTweedleNode[] jointTransformations) {
    encoder.appendInstantiation("JointedModelPose", () -> {
      encoder.forwardAppendString("pairs: new JointIdTransformationPair[]");
      encoder.appendList(jointTransformations, (v) -> v.encodeDefinition(encoder), ",\n");
    });
  }

  void appendNewJointTransformation(String jointId, AffineMatrix4x4 transformation) {
    encoder.forwardAppendString("        ");
    encoder.appendInstantiation("JointIdTransformationPair", () -> {
      encoder.appendArg("joint", jointId);
      encoder.appendAnotherArg("orientation", () -> {
        final UnitQuaternion orientationUnitQuaternion = transformation.orientation().asUnitQuaternion();
        encoder.appendInstantiation("Orientation", () -> {
          encoder.appendArg("x", Double.toString(orientationUnitQuaternion.x()));
          encoder.appendAnotherArg("y", Double.toString(orientationUnitQuaternion.y()));
          encoder.appendAnotherArg("z", Double.toString(orientationUnitQuaternion.z()));
          encoder.appendAnotherArg("w", Double.toString(orientationUnitQuaternion.w()));
        });
      });
      encoder.appendAnotherArg("position", () -> {
        final Tuple3 position = transformation.translation();
        encoder.appendInstantiation("Position", () -> {
          encoder.appendArg("x", Double.toString(position.x()));
          encoder.appendAnotherArg("y", Double.toString(position.y()));
          encoder.appendAnotherArg("z", Double.toString(position.z()));
        });
      });
    });
  }
}
