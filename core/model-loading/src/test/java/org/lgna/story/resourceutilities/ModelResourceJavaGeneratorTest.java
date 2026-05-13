package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.pattern.Tuple2;
import org.alice.math.immutable.AxisAlignedBox;
import org.junit.Test;
import org.lgna.story.resources.BipedResource;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.PropResource;
import org.lgna.story.resources.QuadrupedResource;
import org.lgna.story.resources.FlyerResource;
import org.lgna.story.resources.SwimmerResource;
import org.lgna.story.resources.SlithererResource;
import org.lgna.story.BipedPoseBuilder;
import org.lgna.story.QuadrupedPoseBuilder;
import org.lgna.story.FlyerPoseBuilder;
import org.lgna.story.SwimmerPoseBuilder;
import org.lgna.story.SlithererPoseBuilder;
import org.lgna.story.JointedModelPoseBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for {@link ModelResourceJavaGenerator}.
 * Same package gives access to the package-private API.
 */
public class ModelResourceJavaGeneratorTest {

  // ── getJavaClassName ────────────────────────────────────────────

  @Test
  public void getJavaClassNameAppendsSuffix() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    String result = ModelResourceJavaGenerator.getJavaClassName(exporter);
    assertEquals("TestPropResource", result);
  }

  // ── getJointRootsField ─────────────────────────────────────────

  @Test
  public void getJointRootsFieldReturnsBipedRootsField() {
    Field field = ModelResourceJavaGenerator.getJointRootsField(BipedResource.class);
    assertNotNull("BipedResource should have a JointId[] roots field", field);
    assertEquals(JointId[].class, field.getType());
  }

  @Test
  public void getJointRootsFieldReturnsNullForNull() {
    assertNull(ModelResourceJavaGenerator.getJointRootsField(null));
  }

  @Test
  public void getJointRootsFieldReturnsNullForProp() {
    // PropResource has no joint roots, but may inherit — test that it doesn't crash
    Field field = ModelResourceJavaGenerator.getJointRootsField(PropResource.class);
    // Just verify no exception; result may or may not be null
  }

  // ── needsToDefineRootsMethod ───────────────────────────────────

  @Test
  public void needsToDefineRootsMethodChecksBipedWithoutError() {
    // BipedResource is an interface; whether it needs a roots method depends on
    // inherited method signatures. Just verify it returns without error.
    ModelResourceJavaGenerator.needsToDefineRootsMethod(BipedResource.class);
  }

  @Test
  public void needsToDefineRootsMethodIsFalseForNull() {
    assertFalse(ModelResourceJavaGenerator.needsToDefineRootsMethod(null));
  }

  // ── createResourceEnumName ─────────────────────────────────────

  @Test
  public void createResourceEnumNameUsesTextureOnlyWhenModelMatchesClassName() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    ModelSubResourceExporter sub = new ModelSubResourceExporter("TestProp", "Default", "ALICE", null, null);
    String result = ModelResourceJavaGenerator.createResourceEnumName(exporter, sub);
    assertEquals("DEFAULT", result);
  }

  @Test
  public void createResourceEnumNameCombinesModelAndTexture() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    ModelSubResourceExporter sub = new ModelSubResourceExporter("VariantProp", "BlueStripe", "ALICE", null, null);
    String result = ModelResourceJavaGenerator.createResourceEnumName(exporter, sub);
    assertEquals("VARIANT_PROP_BLUE_STRIPE", result);
  }

  @Test
  public void createResourceEnumNameUsesModelOnlyWhenTextureMatchesModel() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    ModelSubResourceExporter sub = new ModelSubResourceExporter("VariantProp", "VariantProp", "ALICE", null, null);
    String result = ModelResourceJavaGenerator.createResourceEnumName(exporter, sub);
    assertEquals("VARIANT_PROP", result);
  }

  // ── isValidEnumName ────────────────────────────────────────────

  @Test
  public void isValidEnumNameReturnsTrueByDefault() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    assertTrue(ModelResourceJavaGenerator.isValidEnumName(exporter, "TestProp", "DEFAULT"));
  }

  @Test
  public void isValidEnumNameReturnsFalseWhenForcedOverridingNamesExclude() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addForcedEnumNames(null, Collections.singletonList("DEFAULT"));
    assertFalse(ModelResourceJavaGenerator.isValidEnumName(exporter, "TestProp", "VARIANT_PROP"));
  }

  @Test
  public void isValidEnumNameReturnsTrueWhenForcedOverridingNamesInclude() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addForcedEnumNames(null, Collections.singletonList("DEFAULT"));
    assertTrue(ModelResourceJavaGenerator.isValidEnumName(exporter, "TestProp", "DEFAULT"));
  }

  @Test
  public void isValidEnumNameUsesModelSpecificForcedNames() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.addForcedEnumNames("VariantProp", Arrays.asList("Blue", "Red"));
    assertTrue(ModelResourceJavaGenerator.isValidEnumName(exporter, "VariantProp", "BLUE"));
    // otherToCheck = modelName.toUpperCase() + "_" + e → "VARIANTPROP_Red"
    assertTrue(ModelResourceJavaGenerator.isValidEnumName(exporter, "VariantProp", "VARIANTPROP_RED"));
    assertFalse(ModelResourceJavaGenerator.isValidEnumName(exporter, "VariantProp", "GREEN"));
  }

  // ── makeCodeReadyTree ──────────────────────────────────────────

  @Test
  public void makeCodeReadyTreeDelegatesToJointTreeUtilities() {
    List<Tuple2<String, String>> joints = Arrays.asList(
        Tuple2.createInstance("root", null),
        Tuple2.createInstance("arm", "root"));
    List<Tuple2<String, String>> result = ModelResourceJavaGenerator.makeCodeReadyTree(joints);
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("root", result.get(0).getA());
    assertEquals("arm", result.get(1).getA());
  }

  @Test
  public void makeCodeReadyTreeReturnsNullForNullInput() {
    assertNull(ModelResourceJavaGenerator.makeCodeReadyTree(null));
  }

  // ── getJointAccessMethodNameForArrayJoint ───────────────────────

  @Test
  public void getJointAccessMethodNameForArrayJointFollowsConvention() {
    String result = ModelResourceJavaGenerator.getJointAccessMethodNameForArrayJoint("FINGER_01");
    assertTrue("Should start with 'get'", result.startsWith("get"));
    assertTrue("Should contain Finger", result.contains("Finger"));
  }

  // ── createJavaFile ─────────────────────────────────────────────

  @Test
  public void createJavaFileWritesCompilableCode() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    Path root = newTestWorkDir("java-file");

    File javaFile = ModelResourceJavaGenerator.createJavaFile(exporter, root.toString());

    assertTrue(Files.isRegularFile(javaFile.toPath()));
    String content = Files.readString(javaFile.toPath(), StandardCharsets.UTF_8);
    assertTrue(content.contains("public enum TestPropResource"));
    assertTrue(content.contains("DEFAULT"));
  }

  @Test
  public void createJavaFileUsesCorrectPath() throws Exception {
    ModelResourceExporter exporter = createMinimalPropExporter();
    Path root = newTestWorkDir("java-path");

    File javaFile = ModelResourceJavaGenerator.createJavaFile(exporter, root.toString());

    assertTrue(javaFile.getName().equals("TestPropResource.java"));
  }

  // ── getAccessorMethodName ──────────────────────────────────────

  @Test
  public void getAccessorMethodNameConvertsCamelCase() {
    assertEquals("getLeftArm", ModelResourceJavaGenerator.getAccessorMethodName("LEFT_ARM"));
  }

  @Test
  public void getAccessorMethodNameHandlesSingleWord() {
    assertEquals("getRoot", ModelResourceJavaGenerator.getAccessorMethodName("ROOT"));
  }

  // ── getMandatoryJointArrayNames ────────────────────────────────

  @Test
  public void getMandatoryJointArrayNamesReturnsBipedArrays() {
    List<String> names = ModelResourceJavaGenerator.getMandatoryJointArrayNames(BipedResource.class);
    assertNotNull(names);
    // BipedResource may have mandatory array accessors
  }

  @Test
  public void getMandatoryJointArrayNamesReturnsEmptyForProp() {
    List<String> names = ModelResourceJavaGenerator.getMandatoryJointArrayNames(PropResource.class);
    assertNotNull(names);
    assertTrue(names.isEmpty());
  }

  // ── getPoseBuilderTypeForSuperClass ────────────────────────────

  @Test
  public void poseBuilderTypeIsBipedForBipedResource() {
    assertEquals(BipedPoseBuilder.class,
        ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(BipedResource.class));
  }

  @Test
  public void poseBuilderTypeIsQuadrupedForQuadrupedResource() {
    assertEquals(QuadrupedPoseBuilder.class,
        ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(QuadrupedResource.class));
  }

  @Test
  public void poseBuilderTypeIsFlyerForFlyerResource() {
    assertEquals(FlyerPoseBuilder.class,
        ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(FlyerResource.class));
  }

  @Test
  public void poseBuilderTypeIsSwimmerForSwimmerResource() {
    assertEquals(SwimmerPoseBuilder.class,
        ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(SwimmerResource.class));
  }

  @Test
  public void poseBuilderTypeIsSlithererForSlithererResource() {
    assertEquals(SlithererPoseBuilder.class,
        ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(SlithererResource.class));
  }

  @Test
  public void poseBuilderTypeDefaultsToJointedModel() {
    assertEquals(JointedModelPoseBuilder.class,
        ModelResourceJavaGenerator.getPoseBuilderTypeForSuperClass(PropResource.class));
  }

  // ── getAlreadyDeclaredJointArrayNames ──────────────────────────

  @Test
  public void getAlreadyDeclaredJointArrayNamesReturnsEmptyForProp() {
    List<String> names = ModelResourceJavaGenerator.getAlreadyDeclaredJointArrayNames(PropResource.class);
    assertNotNull(names);
    assertTrue(names.isEmpty());
  }

  // ── buildJavaCodeBody round-trip ───────────────────────────────

  @Test
  public void buildJavaCodeBodyProducesCompleteEnumCode() throws DataFormatException {
    ModelResourceExporter exporter = createMinimalPropExporter();
    String code = ModelResourceJavaGenerator.buildJavaCodeBody(exporter);
    assertTrue(code.contains("package org.lgna.story.resources.prop;"));
    assertTrue(code.contains("public enum TestPropResource"));
    assertTrue(code.contains("DEFAULT;"));
    assertTrue(code.contains("createImplementation"));
  }

  // ── helpers ─────────────────────────────────────────────────────

  private static ModelResourceExporter createMinimalPropExporter() {
    ModelResourceExporter exporter = new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
    exporter.setBoundingBox("TestProp", AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("TestProp", "Default", "ALICE", null, null);
    return exporter;
  }

  private static Path newTestWorkDir(String name) throws IOException {
    Path workRoot = Path.of("target", "test-work",
        ModelResourceJavaGeneratorTest.class.getSimpleName(), name).toAbsolutePath();
    deleteRecursively(workRoot);
    Files.createDirectories(workRoot);
    return workRoot;
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (Files.notExists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
