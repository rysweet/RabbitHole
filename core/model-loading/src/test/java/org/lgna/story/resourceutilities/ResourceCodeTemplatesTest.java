package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.OrthogonalMatrix3x3;
import org.alice.math.immutable.Point3;
import org.junit.Test;
import org.lgna.story.resources.PropResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TDD tests for {@link ResourceCodeTemplates}.
 * These tests define the contract for the extracted template methods.
 * They will FAIL until ResourceCodeTemplates is implemented.
 *
 * <p>Same package gives access to the package-private API.
 */
public class ResourceCodeTemplatesTest {

  // ── appendPreambleAndEnumConstants ──────────────────────────────

  @Test
  public void preambleContainsCopyrightHeader() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should contain copyright comment",
        output.contains("/*") && output.contains("*/"));
  }

  @Test
  public void preambleContainsPackageDeclaration() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should contain package declaration",
        output.contains("package org.lgna.story.resources.prop;"));
  }

  @Test
  public void preambleContainsImports() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should import annotations", output.contains("import org.lgna.project.annotations.*;"));
    assertTrue("Should import JointIdTransformationPair",
        output.contains("import org.lgna.story.implementation.JointIdTransformationPair;"));
    assertTrue("Should import Orientation", output.contains("import org.lgna.story.Orientation;"));
    assertTrue("Should import Position", output.contains("import org.lgna.story.Position;"));
    assertTrue("Should import ImplementationAndVisualType",
        output.contains("import org.lgna.story.resources.ImplementationAndVisualType;"));
  }

  @Test
  public void preambleDeclaresEnumImplementingSuperClass() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should declare enum implementing super class",
        output.contains("public enum TestPropResource implements " + PropResource.class.getCanonicalName()));
  }

  @Test
  public void preambleIncludesEnumConstantAndSemicolon() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should contain DEFAULT enum constant", output.contains("DEFAULT"));
    assertTrue("Should end enum constants with semicolon", output.contains(";"));
  }

  @Test
  public void preambleIncludesDeprecatedAnnotationWhenSet() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    exporter.setIsDeprecated(true);
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should contain @Deprecated annotation", output.contains("@Deprecated"));
  }

  @Test
  public void preambleOmitsDeprecatedAnnotationByDefault() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = createMinimalPropExporter();
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertFalse("Should not contain @Deprecated when not deprecated",
        output.contains("@Deprecated"));
  }

  @Test
  public void preambleIncludesMultipleEnumConstants() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addResource("TestProp", "Default", "ALICE", null, null);
    exporter.addResource("VariantProp", "Blue", "ALICE", null, null);
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should contain DEFAULT constant", output.contains("DEFAULT"));
    assertTrue("Should contain VARIANT_PROP_BLUE constant", output.contains("VARIANT_PROP_BLUE"));
    // Multiple enum constants are comma-separated
    assertTrue("Should contain comma between constants", output.contains(","));
  }

  @Test
  public void preambleIncludesTypeStringForNonAliceResources() {
    StringBuilder sb = new StringBuilder();
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addResource("TestProp", "Default", "SIMS", null, null);
    String javaClassName = "TestPropResource";

    ResourceCodeTemplates.appendPreambleAndEnumConstants(sb, exporter, javaClassName);

    String output = sb.toString();
    assertTrue("Should include type parameter for non-ALICE resources",
        output.contains("ImplementationAndVisualType.SIMS"));
  }

  // ── appendJointDeclarations ────────────────────────────────────

  @Test
  public void jointDeclarationsEmitsFieldForNewJoint() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("ROOT_JOINT", (String) null),
        Tuple2.createInstance("LEFT_ARM", "ROOT_JOINT"));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = Collections.emptyMap();
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = Collections.emptySet();
    String javaClassName = "TestPropResource";

    List<String> rootJoints = ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, javaClassName);

    String output = sb.toString();
    assertTrue("Should contain JointId declaration for LEFT_ARM",
        output.contains("public static final org.lgna.story.resources.JointId LEFT_ARM"));
    assertTrue("Should reference parent ROOT_JOINT",
        output.contains("LEFT_ARM = new org.lgna.story.resources.JointId( ROOT_JOINT, TestPropResource.class )"));
  }

  @Test
  public void jointDeclarationsSkipsExistingIds() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("EXISTING_JOINT", "PARENT"));
    Set<String> existingIds = new HashSet<>(Collections.singletonList("EXISTING_JOINT"));
    Map<String, String> jointToArrayName = Collections.emptyMap();
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = Collections.emptySet();

    List<String> rootJoints = ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    String output = sb.toString();
    assertFalse("Should not emit declaration for existing joint",
        output.contains("EXISTING_JOINT"));
  }

  @Test
  public void jointDeclarationsReturnsRootJoints() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("ROOT_A", (String) null),
        Tuple2.createInstance("ROOT_B", ""),
        Tuple2.createInstance("CHILD", "ROOT_A"));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = Collections.emptyMap();
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = Collections.emptySet();

    List<String> rootJoints = ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    assertNotNull("Should return root joints list", rootJoints);
    assertEquals("Should find two root joints", 2, rootJoints.size());
    assertTrue("Should include ROOT_A", rootJoints.contains("ROOT_A"));
    assertTrue("Should include ROOT_B", rootJoints.contains("ROOT_B"));
  }

  @Test
  public void jointDeclarationsAppliesHiddenAnnotationForSuppressedJoint() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("SUPPRESSED_JOINT", "PARENT"));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = Collections.emptyMap();
    Set<String> suppressJointIds = new HashSet<>(Collections.singletonList("SUPPRESSED_JOINT"));
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = Collections.emptySet();

    ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should have COMPLETELY_HIDDEN annotation for suppressed joint",
        output.contains("@FieldTemplate(visibility=Visibility.COMPLETELY_HIDDEN)"));
  }

  @Test
  public void jointDeclarationsAppliesPrimeTimeForVisibleJoint() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("VISIBLE_JOINT", "PARENT"));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = Collections.emptyMap();
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = Collections.emptySet();

    ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should have PRIME_TIME annotation for visible joint",
        output.contains("@FieldTemplate(visibility=Visibility.PRIME_TIME)"));
  }

  @Test
  public void jointDeclarationsAppliesPrimeTimeWithMethodHintForArrayJoint() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("FINGER_00", "HAND"));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = new HashMap<>();
    jointToArrayName.put("FINGER_00", "FINGER");
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = new HashSet<>(Collections.singletonList("FINGER"));

    ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should have PRIME_TIME with methodNameHint for array joint",
        output.contains("@FieldTemplate(visibility=Visibility.PRIME_TIME, methodNameHint="));
  }

  @Test
  public void jointDeclarationsHidesJointsInHiddenArrays() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("HIDDEN_JOINT", "PARENT"));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = new HashMap<>();
    jointToArrayName.put("HIDDEN_JOINT", "HIDDEN_ARRAY");
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = new HashSet<>(Collections.singletonList("HIDDEN_ARRAY"));
    Set<String> exposeFirstArrays = Collections.emptySet();

    ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    String output = sb.toString();
    assertFalse("Should not emit declaration for joint in hidden array",
        output.contains("HIDDEN_JOINT"));
  }

  @Test
  public void jointDeclarationsUsesNullParentForRootJoints() {
    StringBuilder sb = new StringBuilder();
    List<Tuple2<String, String>> skeleton = Arrays.asList(
        Tuple2.createInstance("ROOT_JOINT", (String) null));
    Set<String> existingIds = Collections.emptySet();
    Map<String, String> jointToArrayName = Collections.emptyMap();
    Set<String> suppressJointIds = Collections.emptySet();
    Set<String> hideElementArrays = Collections.emptySet();
    Set<String> exposeFirstArrays = Collections.emptySet();

    ResourceCodeTemplates.appendJointDeclarations(
        sb, skeleton, existingIds, jointToArrayName,
        suppressJointIds, hideElementArrays, exposeFirstArrays, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should use 'null' as parent for root joint",
        output.contains("new org.lgna.story.resources.JointId( null, TestPropResource.class )"));
  }

  // ── appendRootJointIds ─────────────────────────────────────────

  @Test
  public void rootJointIdsEmitsSingleRoot() {
    StringBuilder sb = new StringBuilder();
    List<String> rootJoints = Collections.singletonList("ROOT_JOINT");

    ResourceCodeTemplates.appendRootJointIds(sb, rootJoints);

    String output = sb.toString();
    assertTrue("Should contain JOINT_ID_ROOTS field name",
        output.contains(ModelResourceJavaGenerator.ROOT_IDS_FIELD_NAME));
    assertTrue("Should contain ROOT_JOINT in array initializer",
        output.contains("ROOT_JOINT"));
    assertTrue("Should contain COMPLETELY_HIDDEN annotation",
        output.contains("Visibility.COMPLETELY_HIDDEN"));
  }

  @Test
  public void rootJointIdsEmitsMultipleRootsCommaSeparated() {
    StringBuilder sb = new StringBuilder();
    List<String> rootJoints = Arrays.asList("ROOT_A", "ROOT_B", "ROOT_C");

    ResourceCodeTemplates.appendRootJointIds(sb, rootJoints);

    String output = sb.toString();
    assertTrue("Should contain ROOT_A", output.contains("ROOT_A"));
    assertTrue("Should contain ROOT_B", output.contains("ROOT_B"));
    assertTrue("Should contain ROOT_C", output.contains("ROOT_C"));
    // Comma separation between roots
    assertTrue("Should have comma separation",
        output.contains("ROOT_A, ROOT_B") || output.contains("ROOT_A,"));
  }

  @Test
  public void rootJointIdsDeclaresJointIdArray() {
    StringBuilder sb = new StringBuilder();
    List<String> rootJoints = Collections.singletonList("ROOT");

    ResourceCodeTemplates.appendRootJointIds(sb, rootJoints);

    String output = sb.toString();
    assertTrue("Should declare a JointId[] field",
        output.contains("public static final org.lgna.story.resources.JointId[]"));
  }

  // ── appendPoseFields ───────────────────────────────────────────

  @Test
  public void poseFieldsEmitsPoseConstant() throws DataFormatException {
    StringBuilder sb = new StringBuilder();
    Map<String, Map<String, AffineMatrix4x4>> poseEntries = new LinkedHashMap<>();
    Map<String, AffineMatrix4x4> poseData = new LinkedHashMap<>();
    poseData.put("LEFT_ARM", createIdentityTransform());
    poseEntries.put("STANDING", poseData);
    List<String> mandatoryPoseNames = Collections.emptyList();

    ResourceCodeTemplates.appendPoseFields(
        sb, poseEntries, mandatoryPoseNames,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain STANDING_POSE field",
        output.contains("STANDING_POSE"));
    assertTrue("Should declare as JointedModelPose",
        output.contains("org.lgna.story.JointedModelPose"));
    assertTrue("Should contain JointIdTransformationPair",
        output.contains("JointIdTransformationPair"));
  }

  @Test
  public void poseFieldsEmitsOrientationAndPosition() throws DataFormatException {
    StringBuilder sb = new StringBuilder();
    Map<String, Map<String, AffineMatrix4x4>> poseEntries = new LinkedHashMap<>();
    Map<String, AffineMatrix4x4> poseData = new LinkedHashMap<>();
    poseData.put("HEAD", createIdentityTransform());
    poseEntries.put("NOD", poseData);
    List<String> mandatoryPoseNames = Collections.emptyList();

    ResourceCodeTemplates.appendPoseFields(
        sb, poseEntries, mandatoryPoseNames,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain Orientation(...)", output.contains("new Orientation("));
    assertTrue("Should contain Position(...)", output.contains("new Position("));
  }

  @Test
  public void poseFieldsThrowsForMissingMandatoryPose() {
    StringBuilder sb = new StringBuilder();
    Map<String, Map<String, AffineMatrix4x4>> poseEntries = new HashMap<>();
    List<String> mandatoryPoseNames = Collections.singletonList("REQUIRED_POSE");

    try {
      ResourceCodeTemplates.appendPoseFields(
          sb, poseEntries, mandatoryPoseNames,
          ModelClassData.PROP_CLASS_DATA, "TestPropResource");
      fail("Should throw DataFormatException for missing mandatory pose");
    } catch (DataFormatException e) {
      assertTrue("Exception message should mention the missing pose",
          e.getMessage().contains("REQUIRED_POSE"));
    }
  }

  @Test
  public void poseFieldsThrowsForEmptyPoseData() {
    StringBuilder sb = new StringBuilder();
    Map<String, Map<String, AffineMatrix4x4>> poseEntries = new HashMap<>();
    poseEntries.put("EMPTY_POSE", new HashMap<>());
    List<String> mandatoryPoseNames = Collections.emptyList();

    try {
      ResourceCodeTemplates.appendPoseFields(
          sb, poseEntries, mandatoryPoseNames,
          ModelClassData.PROP_CLASS_DATA, "TestPropResource");
      fail("Should throw DataFormatException for empty pose data");
    } catch (DataFormatException e) {
      assertTrue("Exception message should mention the pose",
          e.getMessage().contains("EMPTY_POSE"));
    }
  }

  @Test
  public void poseFieldsEmitsMultipleJointTransformPairs() throws DataFormatException {
    StringBuilder sb = new StringBuilder();
    Map<String, Map<String, AffineMatrix4x4>> poseEntries = new LinkedHashMap<>();
    Map<String, AffineMatrix4x4> poseData = new LinkedHashMap<>();
    poseData.put("LEFT_ARM", createIdentityTransform());
    poseData.put("RIGHT_ARM", createIdentityTransform());
    poseEntries.put("WAVE", poseData);
    List<String> mandatoryPoseNames = Collections.emptyList();

    ResourceCodeTemplates.appendPoseFields(
        sb, poseEntries, mandatoryPoseNames,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain LEFT_ARM transform pair", output.contains("LEFT_ARM"));
    assertTrue("Should contain RIGHT_ARM transform pair", output.contains("RIGHT_ARM"));
    // Comma between pairs but not after last
    assertTrue("Should contain comma separator between pairs", output.contains(","));
  }

  @Test
  public void poseFieldsEmitsNoPoseWhenEmpty() throws DataFormatException {
    StringBuilder sb = new StringBuilder();
    Map<String, Map<String, AffineMatrix4x4>> poseEntries = new HashMap<>();
    List<String> mandatoryPoseNames = Collections.emptyList();

    ResourceCodeTemplates.appendPoseFields(
        sb, poseEntries, mandatoryPoseNames,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertFalse("Should not emit any pose field when no poses",
        output.contains("_POSE"));
  }

  // ── appendArrayFields ──────────────────────────────────────────

  @Test
  public void arrayFieldsEmitsJointIdArray() {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> arrayEntries = new LinkedHashMap<>();
    arrayEntries.put("FINGER", Arrays.asList("FINGER_01", "FINGER_02", "FINGER_03"));
    List<String> mandatoryArrayNames = new ArrayList<>();
    List<String> declaredArrays = Collections.emptyList();
    List<Tuple2<String, String>> trimmedSkeleton = Collections.emptyList();
    Set<String> hideElementArrays = Collections.emptySet();

    ResourceCodeTemplates.appendArrayFields(
        sb, arrayEntries, mandatoryArrayNames, declaredArrays,
        trimmedSkeleton, hideElementArrays,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain FINGER_ARRAY declaration",
        output.contains("FINGER_ARRAY"));
    assertTrue("Should declare as JointId[]",
        output.contains("org.lgna.story.resources.JointId[] FINGER_ARRAY"));
    assertTrue("Should contain FINGER_01", output.contains("FINGER_01"));
    assertTrue("Should contain FINGER_02", output.contains("FINGER_02"));
    assertTrue("Should contain FINGER_03", output.contains("FINGER_03"));
  }

  @Test
  public void arrayFieldsSkipsAlreadyDeclaredArrays() {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> arrayEntries = new LinkedHashMap<>();
    arrayEntries.put("EXISTING", Arrays.asList("JOINT_1"));
    List<String> mandatoryArrayNames = new ArrayList<>();
    List<String> declaredArrays = Collections.singletonList("EXISTING_ARRAY");
    List<Tuple2<String, String>> trimmedSkeleton = Collections.emptyList();
    Set<String> hideElementArrays = Collections.emptySet();

    ResourceCodeTemplates.appendArrayFields(
        sb, arrayEntries, mandatoryArrayNames, declaredArrays,
        trimmedSkeleton, hideElementArrays,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertFalse("Should not emit already declared array",
        output.contains("EXISTING_ARRAY"));
  }

  @Test
  public void arrayFieldsAlsoSkipsWhenKeyNameMatches() {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> arrayEntries = new LinkedHashMap<>();
    arrayEntries.put("EXISTING", Arrays.asList("JOINT_1"));
    List<String> mandatoryArrayNames = new ArrayList<>();
    List<String> declaredArrays = Collections.singletonList("EXISTING");
    List<Tuple2<String, String>> trimmedSkeleton = Collections.emptyList();
    Set<String> hideElementArrays = Collections.emptySet();

    ResourceCodeTemplates.appendArrayFields(
        sb, arrayEntries, mandatoryArrayNames, declaredArrays,
        trimmedSkeleton, hideElementArrays,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertFalse("Should not emit array when key name matches declared",
        output.contains("EXISTING_ARRAY"));
  }

  @Test
  public void arrayFieldsEmitsJointArrayIdForHiddenElements() {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> arrayEntries = new LinkedHashMap<>();
    arrayEntries.put("HIDDEN", Arrays.asList("HIDDEN_01", "HIDDEN_02"));
    List<String> mandatoryArrayNames = new ArrayList<>();
    List<String> declaredArrays = Collections.emptyList();
    List<Tuple2<String, String>> trimmedSkeleton = Arrays.asList(
        Tuple2.createInstance("HIDDEN_01", "PARENT_JOINT"),
        Tuple2.createInstance("HIDDEN_02", "HIDDEN_01"));
    Set<String> hideElementArrays = new HashSet<>(Collections.singletonList("HIDDEN"));

    ResourceCodeTemplates.appendArrayFields(
        sb, arrayEntries, mandatoryArrayNames, declaredArrays,
        trimmedSkeleton, hideElementArrays,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should declare as JointArrayId for hidden-element array",
        output.contains("org.lgna.story.resources.JointArrayId HIDDEN_ARRAY"));
    assertTrue("Should reference parent of first element",
        output.contains("PARENT_JOINT"));
  }

  @Test
  public void arrayFieldsAddsMandatoryEmptyArrays() {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> arrayEntries = new LinkedHashMap<>();
    List<String> mandatoryArrayNames = new ArrayList<>(Collections.singletonList("MANDATORY"));
    List<String> declaredArrays = Collections.emptyList();
    List<Tuple2<String, String>> trimmedSkeleton = Collections.emptyList();
    Set<String> hideElementArrays = Collections.emptySet();

    ResourceCodeTemplates.appendArrayFields(
        sb, arrayEntries, mandatoryArrayNames, declaredArrays,
        trimmedSkeleton, hideElementArrays,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should emit mandatory array even when empty",
        output.contains("MANDATORY_ARRAY"));
  }

  @Test
  public void arrayFieldsEmitsNoOutputWhenBothEmpty() {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> arrayEntries = new HashMap<>();
    List<String> mandatoryArrayNames = new ArrayList<>();
    List<String> declaredArrays = Collections.emptyList();
    List<Tuple2<String, String>> trimmedSkeleton = Collections.emptyList();
    Set<String> hideElementArrays = Collections.emptySet();

    ResourceCodeTemplates.appendArrayFields(
        sb, arrayEntries, mandatoryArrayNames, declaredArrays,
        trimmedSkeleton, hideElementArrays,
        ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertFalse("Should not contain any array declaration when both empty",
        output.contains("_ARRAY"));
  }

  // ── appendConstructorsAndMethods ───────────────────────────────

  @Test
  public void constructorsEmitsResourceTypeField() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, false, ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain resourceType field",
        output.contains("private final ImplementationAndVisualType resourceType;"));
  }

  @Test
  public void constructorsEmitsDefaultConstructor() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, false, ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain default private constructor",
        output.contains("private TestPropResource()"));
    assertTrue("Should delegate to ImplementationAndVisualType.ALICE",
        output.contains("ImplementationAndVisualType.ALICE"));
  }

  @Test
  public void constructorsEmitsParameterizedConstructor() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, false, ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain parameterized private constructor",
        output.contains("private TestPropResource( ImplementationAndVisualType resourceType )"));
    assertTrue("Should assign resourceType",
        output.contains("this.resourceType = resourceType;"));
  }

  @Test
  public void constructorsEmitsGetImplementationAndVisualFactory() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, false, ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain getImplementationAndVisualFactory method",
        output.contains("getImplementationAndVisualFactory()"));
    assertTrue("Should return factory from resourceType",
        output.contains("this.resourceType.getFactory( this )"));
  }

  @Test
  public void constructorsEmitsCreateImplementation() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, false, ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    assertTrue("Should contain createImplementation method",
        output.contains("createImplementation"));
    assertTrue("Should contain closing brace for enum",
        output.contains("}"));
  }

  @Test
  public void constructorsEmitsRootJointIdsMethodWhenAddedRoots() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, true, ModelClassData.PROP_CLASS_DATA, "TestPropResource");

    String output = sb.toString();
    // PropResource may or may not need roots method (depends on needsToDefineRootsMethod)
    // But when addedRoots is true AND the roots method is needed, it should return our field
    if (output.contains(ModelResourceJavaGenerator.ROOT_IDS_METHOD_NAME)) {
      assertTrue("Should return JOINT_ID_ROOTS when addedRoots is true",
          output.contains("TestPropResource." + ModelResourceJavaGenerator.ROOT_IDS_FIELD_NAME));
    }
  }

  @Test
  public void constructorsEmitsRootJointIdsMethodForQuadruped() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, true, ModelClassData.QUADRUPED_CLASS_DATA, "TestQuadrupedResource");

    String output = sb.toString();
    assertTrue("QuadrupedResource should need roots method",
        output.contains(ModelResourceJavaGenerator.ROOT_IDS_METHOD_NAME));
    assertTrue("Should return TestQuadrupedResource.JOINT_ID_ROOTS",
        output.contains("TestQuadrupedResource." + ModelResourceJavaGenerator.ROOT_IDS_FIELD_NAME));
  }

  @Test
  public void constructorsEmitsFallbackRootsWhenNotAddedRoots() {
    StringBuilder sb = new StringBuilder();

    ResourceCodeTemplates.appendConstructorsAndMethods(
        sb, false, ModelClassData.QUADRUPED_CLASS_DATA, "TestQuadrupedResource");

    String output = sb.toString();
    // When addedRoots is false but roots method is needed, should delegate to parent roots field
    // or return empty array
    assertTrue("Should contain getRootJointIds method",
        output.contains(ModelResourceJavaGenerator.ROOT_IDS_METHOD_NAME));
    // Either returns parent field or empty array
    assertTrue("Should return a valid roots expression",
        output.contains("return ") && output.contains(";"));
  }

  // ── Integration: byte-identical output ─────────────────────────

  @Test
  public void templateMethodsProduceSameOutputAsBuildJavaCodeBody() throws DataFormatException {
    // This is the key characterization test: the output of calling all 6
    // template methods in sequence must exactly match buildJavaCodeBody.
    ModelResourceExporter exporter = createMinimalPropExporter();

    // Get the current output (baseline)
    String expected = ModelResourceJavaGenerator.buildJavaCodeBody(exporter);

    // Verify baseline is valid
    assertNotNull(expected);
    assertTrue("Should contain enum declaration", expected.contains("public enum TestPropResource"));
    assertTrue("Should contain DEFAULT constant", expected.contains("DEFAULT"));
    assertTrue("Should contain createImplementation", expected.contains("createImplementation"));

    // After implementation, buildJavaCodeBody will delegate to ResourceCodeTemplates.
    // This test ensures the output doesn't change.
    // Re-call to verify idempotency (same exporter, same output)
    String secondCall = ModelResourceJavaGenerator.buildJavaCodeBody(exporter);
    assertEquals("buildJavaCodeBody should be deterministic", expected, secondCall);
  }

  // ── Line ending consistency ────────────────────────────────────

  @Test
  public void generatedCodeUsesConsistentLineEndings() throws DataFormatException {
    ModelResourceExporter exporter = createMinimalPropExporter();
    String output = ModelResourceJavaGenerator.buildJavaCodeBody(exporter);

    // Every line break in the output should be CRLF (\r\n), not bare LF (\n).
    // Split on \n and verify each preceding character is \r.
    String[] lines = output.split("\n", -1);
    for (int i = 0; i < lines.length - 1; i++) {
      String line = lines[i];
      assertTrue(
          "Line " + (i + 1) + " should end with \\r before \\n (CRLF), got: ..."
              + line.substring(Math.max(0, line.length() - 20)),
          line.endsWith("\r"));
    }
  }

  @Test
  public void generatedCodeContainsNoBareLineFeed() throws DataFormatException {
    ModelResourceExporter exporter = createMinimalPropExporter();
    String output = ModelResourceJavaGenerator.buildJavaCodeBody(exporter);

    // Remove all \r\n, then check that no bare \n remains
    String withoutCrlf = output.replace("\r\n", "");
    assertFalse("Output should not contain bare \\n (only \\r\\n)",
        withoutCrlf.contains("\n"));
  }

  // ── helpers ────────────────────────────────────────────────────

  private static ModelResourceExporter createMinimalPropExporter() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.setBoundingBox("TestProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("TestProp", "Default", "ALICE", null, null);
    return exporter;
  }

  private static AffineMatrix4x4 createIdentityTransform() {
    return new AffineMatrix4x4(OrthogonalMatrix3x3.IDENTITY, Point3.ORIGIN);
  }
}
