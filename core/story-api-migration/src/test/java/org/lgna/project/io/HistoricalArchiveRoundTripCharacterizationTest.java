package org.lgna.project.io;

/** Audit note: this characterization test is ~1762 LOC and should be split into focused suites in a future refactoring. */

import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import org.alice.tweedle.file.ImageReference;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeManifest;
import org.alice.tweedle.file.TypeReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.AssignmentExpression;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.StatementListProperty;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.story.SProgram;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HistoricalArchiveRoundTripCharacterizationTest {
  private static final byte[] CURRENT_VERSION_ENTRY_BYTES =
      ProjectVersion.getCurrentVersion().toString().getBytes(StandardCharsets.UTF_8);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedClassArchiveCharacterizesXmlFallbackTypeRoundTripWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-type-texture.png", 0xFF336699);
    NamedUserType generatedType = typeReferencingImageResource("GeneratedHistoricalType", imageResource);
    File typeArchive = temporaryFolder.newFile("generated-historical-type.a3c");

    IoUtilities.writeType(typeArchive, generatedType);

    assertXmlTypeArchiveFacts(typeArchive, "GeneratedHistoricalType");
    TypeResourcesPair firstRead = IoUtilities.readType(typeArchive);
    assertTypeFacts(firstRead, "GeneratedHistoricalType", imageResource);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-type-roundtrip.a3c");
    IoUtilities.writeType(roundTripArchive, firstRead.getType());

    assertXmlTypeArchiveFacts(roundTripArchive, "GeneratedHistoricalType");
    TypeResourcesPair secondRead = IoUtilities.readType(roundTripArchive);
    assertTypeFacts(secondRead, "GeneratedHistoricalType", imageResource);
  }

  @Test
  public void generatedWorldArchiveCharacterizesManifestProgramRoundTripWithoutExternalFixture() throws Exception {
    Project project = new Project(programType("GeneratedHistoricalProgram"), Project.SceneCameraType.VRHeadset);
    File exportArchive = temporaryFolder.newFile("generated-historical-world.a3w");

    IoUtilities.exportProject(exportArchive, project);

    assertWorldManifestFacts(exportArchive, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);
    Project firstRead = IoUtilities.readProject(exportArchive);
    assertProgramFacts(firstRead, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-world-roundtrip.a3w");
    IoUtilities.exportProject(roundTripArchive, firstRead);

    assertWorldManifestFacts(roundTripArchive, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);
    Project secondRead = IoUtilities.readProject(roundTripArchive);
    assertProgramFacts(secondRead, "GeneratedHistoricalProgram", Project.SceneCameraType.VRHeadset);
  }

  @Test
  public void generatedJsonProjectArchiveDecodesProgramFieldTypedBySceneWithoutExternalFixture() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-project-with-scene-field.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithScene",
        "class GeneratedProgramWithScene extends SProgram { GeneratedScene scene; }",
        "GeneratedScene",
        "class GeneratedScene extends SScene {}");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(manifest, "GeneratedProgramWithScene", "src/GeneratedProgramWithScene.twe");
      assertTypeReference(manifest, "GeneratedScene", "src/GeneratedScene.twe");
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a scene field should decode", readProgramType);
    assertEquals("GeneratedProgramWithScene", readProgramType.getName());
    assertEquals(1, readProgramType.getDeclaredFields().size());
    UserField readSceneField = readProgramType.getDeclaredFields().get(0);
    assertEquals("scene", readSceneField.getName());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedScene");
    assertSame("Decoded program field should resolve to the scene type decoded from the same archive",
        readSceneType,
        readSceneField.getValueType());
    assertEquals("SScene", readSceneType.getSuperType().getName());
  }

  @Test
  public void generatedJsonProjectArchiveReadsSiblingTypeAndImageResourceWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("json-project-texture.png", 0xFF669933);
    File projectArchive = temporaryFolder.newFile("generated-json-project-with-resource.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithResource",
        "class GeneratedProgramWithResource extends SProgram { GeneratedResourceScene scene; }",
        "GeneratedResourceScene",
        "class GeneratedResourceScene extends SScene { WholeNumber count; }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(manifest, "GeneratedProgramWithResource", "src/GeneratedProgramWithResource.twe");
      assertTypeReference(manifest, "GeneratedResourceScene", "src/GeneratedResourceScene.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a sibling scene type should decode", readProgramType);
    assertEquals("GeneratedProgramWithResource", readProgramType.getName());
    assertEquals(1, readProgramType.getDeclaredFields().size());
    UserField readSceneField = readProgramType.getDeclaredFields().get(0);
    assertEquals("scene", readSceneField.getName());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedResourceScene");
    assertSame(readSceneType, readSceneField.getValueType());
    assertEquals("SScene", readSceneType.getSuperType().getName());
    assertEquals(1, readSceneType.getDeclaredFields().size());
    assertEquals("count", readSceneType.getDeclaredFields().get(0).getName());

    Resource readResource = onlyResource(readProject.getResources());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals(imageResource.getName(), readResource.getName());
    assertEquals(imageResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(imageResource.getContentType(), readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void generatedJsonProjectArchiveDecodesNullInitializedSiblingTypeFieldWithoutExternalFixture() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-project-null-sibling-field.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithNullSiblingField",
        "class GeneratedProgramWithNullSiblingField extends SProgram { GeneratedNullableScene scene <- null; }",
        "GeneratedNullableScene",
        "class GeneratedNullableScene extends SScene {}");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(manifest, "GeneratedProgramWithNullSiblingField", "src/GeneratedProgramWithNullSiblingField.twe");
      assertTypeReference(manifest, "GeneratedNullableScene", "src/GeneratedNullableScene.twe");
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a null sibling type field should decode", readProgramType);
    assertEquals("GeneratedProgramWithNullSiblingField", readProgramType.getName());
    assertEquals(1, readProgramType.getDeclaredFields().size());
    UserField readSceneField = readProgramType.getDeclaredFields().get(0);
    assertEquals("scene", readSceneField.getName());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedNullableScene");
    assertSame(readSceneType, readSceneField.getValueType());
    assertTrue(readSceneField.initializer.getValue() instanceof NullLiteral);
    assertEquals("SScene", readSceneType.getSuperType().getName());
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesProgramMethodWithPrimitiveReturn() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-method-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithMethodBoundary",
        "class GeneratedProgramWithMethodBoundary extends SProgram { WholeNumber count() { return 1; } }",
        "GeneratedMethodBoundaryScene",
        "class GeneratedMethodBoundaryScene extends SScene {}");

    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a primitive return method should decode", readProgramType);
    assertEquals("GeneratedProgramWithMethodBoundary", readProgramType.getName());
    assertPrimitiveReturnMethod(readProgramType, "count", 1);
    assertEquals(
        "GeneratedMethodBoundaryScene",
        namedUserTypeNamed(readProject, "GeneratedMethodBoundaryScene").getName());
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesProgramMethodReturningThisField() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-this-field-method.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithThisFieldMethod",
        """
            class GeneratedProgramWithThisFieldMethod extends SProgram {
              WholeNumber count <- 7;
              WholeNumber getCount() { return this.count; }
            }
            """,
        "GeneratedThisFieldMethodScene",
        "class GeneratedThisFieldMethodScene extends SScene {}");

    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a this.field return method should decode", readProgramType);
    assertEquals("GeneratedProgramWithThisFieldMethod", readProgramType.getName());
    assertFieldReturnMethod(readProgramType, "getCount", "count");
    assertEquals(
        "GeneratedThisFieldMethodScene",
        namedUserTypeNamed(readProject, "GeneratedThisFieldMethodScene").getName());
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesSiblingMethodWithPrimitiveReturn() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-method-sibling-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithMethodSiblingBoundary",
        "class GeneratedProgramWithMethodSiblingBoundary extends SProgram { WholeNumber count; }",
        "GeneratedMethodSiblingBoundaryScene",
        "class GeneratedMethodSiblingBoundaryScene extends SScene { WholeNumber count() { return 1; } }");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithMethodSiblingBoundary",
          "src/GeneratedProgramWithMethodSiblingBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedMethodSiblingBoundaryScene",
          "src/GeneratedMethodSiblingBoundaryScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedMethodSiblingBoundaryScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the method-bearing sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("WholeNumber count()"));
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a method-bearing sibling type should decode", readProgramType);
    assertEquals("GeneratedProgramWithMethodSiblingBoundary", readProgramType.getName());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedMethodSiblingBoundaryScene");
    assertPrimitiveReturnMethod(readSceneType, "count", 1);
  }

  @Test
  public void constructorBearingJsonA3wProgramTypeDecodesEmptyConstructor() throws Exception {
    File projectArchive = temporaryFolder.newFile("constructor-bearing-json-a3w-program-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithConstructorBoundary",
        "class GeneratedProgramWithConstructorBoundary extends SProgram { GeneratedProgramWithConstructorBoundary() { } }",
        "GeneratedConstructorBoundaryScene",
        "class GeneratedConstructorBoundaryScene extends SScene {}");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      ZipEntry programTypeEntry = zipFile.getEntry("src/GeneratedProgramWithConstructorBoundary.twe");
      assertNotNull(
          "Constructor-bearing JSON .a3w fixture should contain the manifest-declared program type source",
          programTypeEntry);
      assertTrue(
          readEntry(zipFile, programTypeEntry).contains("GeneratedProgramWithConstructorBoundary()"));
      assertTypeReference(
          manifest,
          "GeneratedProgramWithConstructorBoundary",
          "src/GeneratedProgramWithConstructorBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedConstructorBoundaryScene",
          "src/GeneratedConstructorBoundaryScene.twe");
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertEquals("GeneratedProgramWithConstructorBoundary", readProgramType.getName());
    assertEmptyConstructor(readProgramType);
    assertEquals(
        "GeneratedConstructorBoundaryScene",
        namedUserTypeNamed(readProject, "GeneratedConstructorBoundaryScene").getName());
  }

  @Test
  public void generatedJsonPlayerArchiveWithConstructorBearingSiblingTypeDecodesWithoutSilentOmission() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-constructor-sibling-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithConstructorSiblingBoundary",
        "class GeneratedProgramWithConstructorSiblingBoundary extends SProgram { WholeNumber count; }",
        "GeneratedConstructorSiblingBoundaryScene",
        "class GeneratedConstructorSiblingBoundaryScene extends SScene { GeneratedConstructorSiblingBoundaryScene() { } }");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithConstructorSiblingBoundary",
          "src/GeneratedProgramWithConstructorSiblingBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedConstructorSiblingBoundaryScene",
          "src/GeneratedConstructorSiblingBoundaryScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedConstructorSiblingBoundaryScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the constructor-bearing sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("GeneratedConstructorSiblingBoundaryScene()"));
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertEquals("GeneratedProgramWithConstructorSiblingBoundary", readProgramType.getName());
    assertEquals(1, readProgramType.getDeclaredFields().size());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedConstructorSiblingBoundaryScene");
    assertEmptyConstructor(readSceneType);
  }

  @Test
  public void jsonProjectArchiveWithOnlyEmptyConstructorManifestTypesDecodes() throws Exception {
    File projectArchive = temporaryFolder.newFile("all-empty-constructor-json-a3w-types-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramAllEmptyConstructorBoundary",
        "class GeneratedProgramAllEmptyConstructorBoundary extends SProgram { GeneratedProgramAllEmptyConstructorBoundary() { } }",
        "GeneratedSceneAllEmptyConstructorBoundary",
        "class GeneratedSceneAllEmptyConstructorBoundary extends SScene { GeneratedSceneAllEmptyConstructorBoundary() { } }");

    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertEquals("GeneratedProgramAllEmptyConstructorBoundary", readProgramType.getName());
    assertEmptyConstructor(readProgramType);
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedSceneAllEmptyConstructorBoundary");
    assertEmptyConstructor(readSceneType);
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesProgramLiteralArithmeticFieldInitializer() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-arithmetic-initializer-program.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithArithmeticInitializer",
        "class GeneratedProgramWithArithmeticInitializer extends SProgram { WholeNumber count <- 1 + 2; }",
        "GeneratedArithmeticInitializerScene",
        "class GeneratedArithmeticInitializerScene extends SScene {}");

    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a literal arithmetic field initializer should decode", readProgramType);
    assertEquals("GeneratedProgramWithArithmeticInitializer", readProgramType.getName());
    assertArithmeticFieldInitializer(readProgramType, "count", ArithmeticInfixExpression.Operator.PLUS, 1, 2);
    assertEquals(
        "GeneratedArithmeticInitializerScene",
        namedUserTypeNamed(readProject, "GeneratedArithmeticInitializerScene").getName());
  }

  @Test
  public void generatedJsonPlayerArchiveWithMixedIdentifierProgramInitializerIsRejectedWithoutPartialProgramDecode() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-mixed-initializer-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithMixedInitializerBoundary",
        """
            class GeneratedProgramWithMixedInitializerBoundary extends SProgram {
              WholeNumber seed <- 1;
              WholeNumber count <- seed + 2;
            }
            """,
        "GeneratedMixedInitializerBoundaryScene",
        "class GeneratedMixedInitializerBoundaryScene extends SScene {}");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramWithMixedInitializerBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are [GeneratedMixedInitializerBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithArgumentBearingExplicitThisMethodCallReportsUnsupportedDecodeBoundary() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-argument-this-call-boundary.a3w");

    // Same-class argument-bearing this-calls that resolve (matching label set) now
    // decode; this boundary fixture uses an UNRESOLVED arg-bearing call (the label
    // `other` does not match the `value` parameter) so the loud decode boundary is
    // still exercised at the archive/readProject level.
    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithArgumentThisCallBoundary",
        """
            class GeneratedProgramWithArgumentThisCallBoundary extends SProgram {
              void caller() { this.helper(other: 1); }
              void helper(WholeNumber value) { }
            }
            """,
        "GeneratedArgumentThisCallBoundaryScene",
        "class GeneratedArgumentThisCallBoundaryScene extends SScene {}");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));
    String message = thrown.getMessage();

    assertTrue(message.contains(
        "Project archive manifest names program type 'GeneratedProgramWithArgumentThisCallBoundary'"));
    assertTrue(message.contains("decoded type names are [GeneratedArgumentThisCallBoundaryScene]"));
    assertTrue(message.contains(
        "unsupported manifest-declared Tweedle type names are [GeneratedProgramWithArgumentThisCallBoundary]"));
    assertTrue(message.contains(
        "GeneratedProgramWithArgumentThisCallBoundary: Tweedle argument-bearing explicit this method calls"));
    assertTrue(message.contains("caller.this.helper"));
    assertFalse(message.contains("this.helper(value: 1)"));
    assertFalse(message.contains("void helper(WholeNumber value)"));
    assertFalse(message.contains("\n"));
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesArgumentBearingSameClassThisMethodCall() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-argument-this-call-decodes.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithArgumentThisCall",
        """
            class GeneratedProgramWithArgumentThisCall extends SProgram {
              void caller() { this.helper(value: 1); }
              void helper(WholeNumber value) { }
            }
            """,
        "GeneratedArgumentThisCallScene",
        "class GeneratedArgumentThisCallScene extends SScene {}");

    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull(
        "Resolvable same-class argument-bearing this-call program should decode", readProgramType);
    UserMethod caller = userMethodNamed(readProgramType, "caller");
    UserMethod helper = userMethodNamed(readProgramType, "helper");
    assertEquals(1, caller.body.getValue().statements.size());
    assertTrue(caller.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement statement = (ExpressionStatement) caller.body.getValue().statements.get(0);
    assertTrue(statement.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(helper, invocation.method.getValue());
    assertEquals(1, invocation.requiredArguments.size());
    assertEquals(
        "GeneratedArgumentThisCallScene",
        namedUserTypeNamed(readProject, "GeneratedArgumentThisCallScene").getName());
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesSimpleIfWithZeroArgumentThisMethodCall() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-simple-if-this-call.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithSimpleIfThisCall",
        """
            class GeneratedProgramWithSimpleIfThisCall extends SProgram {
              void run(WholeNumber n) {
                if (n > 0) { this.helper(); }
              }
              void helper() { }
            }
            """,
        "GeneratedSimpleIfThisCallScene",
        "class GeneratedSimpleIfThisCallScene extends SScene {}");

    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with a simple if method body should decode", readProgramType);
    assertEquals("GeneratedProgramWithSimpleIfThisCall", readProgramType.getName());
    assertSimpleIfMethodInvocation(readProgramType, "run", "helper", RelationalInfixExpression.Operator.GREATER);
    assertEquals(
        "GeneratedSimpleIfThisCallScene",
        namedUserTypeNamed(readProject, "GeneratedSimpleIfThisCallScene").getName());
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesSiblingLiteralArithmeticFieldInitializer() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-arithmetic-initializer-sibling.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithArithmeticInitializerSibling",
        "class GeneratedProgramWithArithmeticInitializerSibling extends SProgram { WholeNumber count; }",
        "GeneratedArithmeticInitializerSiblingScene",
        "class GeneratedArithmeticInitializerSiblingScene extends SScene { WholeNumber count <- 1 + 2; }");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithArithmeticInitializerSibling",
          "src/GeneratedProgramWithArithmeticInitializerSibling.twe");
      assertTypeReference(
          manifest,
          "GeneratedArithmeticInitializerSiblingScene",
          "src/GeneratedArithmeticInitializerSiblingScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedArithmeticInitializerSiblingScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the arithmetic-initializer sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("WholeNumber count <- 1 + 2"));
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertEquals("GeneratedProgramWithArithmeticInitializerSibling", readProgramType.getName());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedArithmeticInitializerSiblingScene");
    assertArithmeticFieldInitializer(readSceneType, "count", ArithmeticInfixExpression.Operator.PLUS, 1, 2);
  }

  @Test
  public void generatedJsonPlayerArchiveWithMixedIdentifierInitializerSiblingTypeIsRejectedWithoutSilentOmission() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-mixed-initializer-sibling-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithMixedInitializerSiblingBoundary",
        "class GeneratedProgramWithMixedInitializerSiblingBoundary extends SProgram { WholeNumber count; }",
        "GeneratedMixedInitializerSiblingBoundaryScene",
        """
            class GeneratedMixedInitializerSiblingBoundaryScene extends SScene {
              WholeNumber seed <- 1;
              WholeNumber count <- seed + 2;
            }
            """);

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithMixedInitializerSiblingBoundary",
          "src/GeneratedProgramWithMixedInitializerSiblingBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedMixedInitializerSiblingBoundaryScene",
          "src/GeneratedMixedInitializerSiblingBoundaryScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedMixedInitializerSiblingBoundaryScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the mixed-initializer sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("WholeNumber count <- seed + 2"));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive contains unsupported manifest-declared Tweedle type names [GeneratedMixedInitializerSiblingBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithUnresolvedProgramParentIsRejectedWithoutPartialProgramDecode() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-unresolved-parent-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithUnresolvedParentBoundary",
        "class GeneratedProgramWithUnresolvedParentBoundary extends MissingLegacyProgramParent { WholeNumber count; }",
        "GeneratedUnresolvedParentBoundaryScene",
        "class GeneratedUnresolvedParentBoundaryScene extends SScene {}");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithUnresolvedParentBoundary",
          "src/GeneratedProgramWithUnresolvedParentBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedUnresolvedParentBoundaryScene",
          "src/GeneratedUnresolvedParentBoundaryScene.twe");
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramWithUnresolvedParentBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are [GeneratedUnresolvedParentBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithUnresolvedParentSiblingTypeIsRejectedWithoutSilentOmission() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-unresolved-parent-sibling-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithUnresolvedParentSiblingBoundary",
        "class GeneratedProgramWithUnresolvedParentSiblingBoundary extends SProgram { WholeNumber count; }",
        "GeneratedUnresolvedParentSiblingBoundaryScene",
        "class GeneratedUnresolvedParentSiblingBoundaryScene extends MissingLegacySceneParent { WholeNumber count; }");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithUnresolvedParentSiblingBoundary",
          "src/GeneratedProgramWithUnresolvedParentSiblingBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedUnresolvedParentSiblingBoundaryScene",
          "src/GeneratedUnresolvedParentSiblingBoundaryScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedUnresolvedParentSiblingBoundaryScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the unresolved-parent sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("extends MissingLegacySceneParent"));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive contains unsupported manifest-declared Tweedle type names [GeneratedUnresolvedParentSiblingBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveMissingManifestDeclaredProgramEntryFailsClearly() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-missing-program-entry-boundary.a3w");

    writeJsonProjectArchiveOmittingProgramEntry(
        projectArchive,
        "GeneratedProgramWithMissingEntryBoundary",
        "GeneratedMissingEntryBoundaryScene",
        "class GeneratedMissingEntryBoundaryScene extends SScene {}");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithMissingEntryBoundary",
          "src/GeneratedProgramWithMissingEntryBoundary.twe");
      assertNull("Generated fixture intentionally omits the manifest-declared program type entry",
          zipFile.getEntry("src/GeneratedProgramWithMissingEntryBoundary.twe"));
      assertNotNull("Generated fixture should still include the sibling scene type entry",
          zipFile.getEntry("src/GeneratedMissingEntryBoundaryScene.twe"));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Archive does not contain type entry src/GeneratedProgramWithMissingEntryBoundary.twe"));
  }

  @Test
  public void generatedJsonPlayerArchiveMissingManifestDeclaredSiblingEntryFailsClearly() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-missing-sibling-entry-boundary.a3w");

    writeJsonProjectArchiveOmittingSceneEntry(
        projectArchive,
        "GeneratedProgramWithMissingSiblingEntryBoundary",
        "class GeneratedProgramWithMissingSiblingEntryBoundary extends SProgram { GeneratedMissingSiblingEntryScene scene; }",
        "GeneratedMissingSiblingEntryScene");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithMissingSiblingEntryBoundary",
          "src/GeneratedProgramWithMissingSiblingEntryBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedMissingSiblingEntryScene",
          "src/GeneratedMissingSiblingEntryScene.twe");
      assertNotNull("Generated fixture should include the manifest-declared program type entry",
          zipFile.getEntry("src/GeneratedProgramWithMissingSiblingEntryBoundary.twe"));
      assertNull("Generated fixture intentionally omits the manifest-declared sibling type entry",
          zipFile.getEntry("src/GeneratedMissingSiblingEntryScene.twe"));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Archive does not contain type entry src/GeneratedMissingSiblingEntryScene.twe"));
  }

  @Test
  public void generatedJsonPlayerArchiveDecodesUnnamedSiblingTypeWithPrimitiveReturnMethod() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-unnamed-unsupported-sibling-boundary.a3w");

    writeJsonProjectArchiveWithUnnamedSiblingTypeReference(
        projectArchive,
        "GeneratedProgramWithUnnamedUnsupportedSiblingBoundary",
        "class GeneratedProgramWithUnnamedUnsupportedSiblingBoundary extends SProgram { WholeNumber count; }",
        "GeneratedUnnamedUnsupportedSiblingScene",
        "class GeneratedUnnamedUnsupportedSiblingScene extends SScene { WholeNumber count() { return 1; } }");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithUnnamedUnsupportedSiblingBoundary",
          "src/GeneratedProgramWithUnnamedUnsupportedSiblingBoundary.twe");
      assertUnnamedTypeReference(manifest, "src/GeneratedUnnamedUnsupportedSiblingScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedUnnamedUnsupportedSiblingScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the unnamed unsupported sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("WholeNumber count()"));
    }
    Project readProject = IoUtilities.readProject(projectArchive);

    NamedUserType readProgramType = readProject.getProgramType();
    assertNotNull("Generated JSON .a3w program with an unnamed method-bearing sibling should decode", readProgramType);
    assertEquals("GeneratedProgramWithUnnamedUnsupportedSiblingBoundary", readProgramType.getName());
    NamedUserType readSceneType = namedUserTypeNamed(readProject, "GeneratedUnnamedUnsupportedSiblingScene");
    assertPrimitiveReturnMethod(readSceneType, "count", 1);
  }

  @Test
  public void generatedWorldArchiveWithUnsupportedResourceExpressionIsRejectedWithoutPartialProgramDecode() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-world-texture.png", 0xFF663399);
    Project project = new Project(
        typeReferencingImageResource("GeneratedResourceWorld", imageResource),
        Project.SceneCameraType.WindowCamera);
    project.addResource(imageResource);
    File exportArchive = temporaryFolder.newFile("generated-resource-world.a3w");

    IoUtilities.exportProject(exportArchive, project);

    assertWorldResourceManifestFacts(exportArchive, "GeneratedResourceWorld", imageResource);
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(exportArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedResourceWorld'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithResourceFieldInitializerProgramTypeIsRejectedWithoutPartialProgramDecode() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-world-program-texture.png", 0xFF996633);
    File projectArchive = temporaryFolder.newFile("generated-json-player-resource-field-initializer-program-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithResourceInitializer",
        "class GeneratedProgramWithResourceInitializer extends SProgram { ImageResource texture <- \""
            + imageResource.getName()
            + "\"; }",
        "GeneratedResourceInitializerScene",
        "class GeneratedResourceInitializerScene extends SScene {}",
        imageResource);

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithResourceInitializer",
          "src/GeneratedProgramWithResourceInitializer.twe");
      assertTypeReference(
          manifest,
          "GeneratedResourceInitializerScene",
          "src/GeneratedResourceInitializerScene.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
      ZipEntry programTypeEntry = zipFile.getEntry("src/GeneratedProgramWithResourceInitializer.twe");
      assertNotNull("Generated JSON .a3w fixture should contain the resource-initializer program type source",
          programTypeEntry);
      assertTrue(readEntry(zipFile, programTypeEntry).contains(
          "ImageResource texture <- \"" + imageResource.getName() + "\""));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage(), thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramWithResourceInitializer'"));
    assertTrue(thrown.getMessage(), thrown.getMessage().contains(
        "decoded type names are [GeneratedResourceInitializerScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithResourceFieldInitializerSiblingTypeIsRejectedWithoutSilentOmission() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-world-sibling-texture.png", 0xFF993366);
    File projectArchive = temporaryFolder.newFile("generated-json-player-resource-field-initializer-sibling-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithResourceInitializerSibling",
        "class GeneratedProgramWithResourceInitializerSibling extends SProgram { GeneratedResourceInitializerSiblingScene scene; }",
        "GeneratedResourceInitializerSiblingScene",
        "class GeneratedResourceInitializerSiblingScene extends SScene { ImageResource texture <- \""
            + imageResource.getName()
            + "\"; }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithResourceInitializerSibling",
          "src/GeneratedProgramWithResourceInitializerSibling.twe");
      assertTypeReference(
          manifest,
          "GeneratedResourceInitializerSiblingScene",
          "src/GeneratedResourceInitializerSiblingScene.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedResourceInitializerSiblingScene.twe");
      assertNotNull("Generated JSON .a3w fixture should contain the resource-initializer sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains(
          "ImageResource texture <- \"" + imageResource.getName() + "\""));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage(), thrown.getMessage().contains(
        "Project archive contains unsupported manifest-declared Tweedle type names [GeneratedResourceInitializerSiblingScene]"));
  }

  @Test
  public void generatedJsonTypeArchiveDecodesFieldOnlyTweedleWithResourceReadbackWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("json-type-texture.png", 0xFF339966);
    File typeArchive = temporaryFolder.newFile("generated-json-type.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonType",
        "class GeneratedJsonType extends SProgram { WholeNumber count; }",
        imageResource);

    TypeResourcesPair readType = IoUtilities.readType(typeArchive);

    assertNotNull("Generated JSON .a3c archive should read a type/resources pair", readType);
    NamedUserType decodedType = readType.getType();
    assertNotNull("JSON .a3c type reads now decode field-only Tweedle classes.", decodedType);
    assertEquals("GeneratedJsonType", decodedType.getName());
    assertEquals("SProgram", decodedType.getSuperType().getName());
    assertEquals(1, decodedType.getDeclaredFields().size());
    UserField field = decodedType.getDeclaredFields().get(0);
    assertEquals("count", field.getName());
    assertSame(JavaType.getInstance(Integer.class), field.getValueType());
    Resource readResource = onlyResource(readType.getResources());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals(imageResource.getName(), readResource.getName());
    assertEquals(imageResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(imageResource.getContentType(), readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void methodBearingJsonTypeArchiveDecodesPrimitiveReturnMethod() throws Exception {
    ImageResource imageResource = generatedImageResource("json-type-method-boundary-texture.png", 0xFF663366);
    File typeArchive = temporaryFolder.newFile("method-bearing-json-a3c-boundary.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonTypeWithMethodBoundary",
        "class GeneratedJsonTypeWithMethodBoundary extends SProgram { WholeNumber count() { return 1; } }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(typeArchive)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      ZipEntry typeEntry = zipFile.getEntry("src/GeneratedJsonTypeWithMethodBoundary.twe");
      assertNotNull("Method-bearing JSON .a3c fixture should contain the manifest-declared type source", typeEntry);
      assertTrue(readEntry(zipFile, typeEntry).contains("WholeNumber count()"));
      assertTypeReference(
          manifest,
          "GeneratedJsonTypeWithMethodBoundary",
          "src/GeneratedJsonTypeWithMethodBoundary.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
    }
    TypeResourcesPair readType = IoUtilities.readType(typeArchive);

    NamedUserType readUserType = readType.getType();
    assertNotNull("Method-bearing JSON .a3c fixture should decode the type", readUserType);
    assertEquals("GeneratedJsonTypeWithMethodBoundary", readUserType.getName());
    assertPrimitiveReturnMethod(readUserType, "count", 1);
    Resource readResource = onlyResource(readType.getResources());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals(imageResource.getName(), readResource.getName());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void constructorBearingJsonTypeArchiveDecodesEmptyConstructor() throws Exception {
    ImageResource imageResource = generatedImageResource("json-type-constructor-boundary-texture.png", 0xFF336666);
    File typeArchive = temporaryFolder.newFile("constructor-bearing-json-a3c-boundary.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonTypeWithConstructorBoundary",
        "class GeneratedJsonTypeWithConstructorBoundary extends SProgram { GeneratedJsonTypeWithConstructorBoundary() { } }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(typeArchive)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      ZipEntry typeEntry = zipFile.getEntry("src/GeneratedJsonTypeWithConstructorBoundary.twe");
      assertNotNull("Constructor-bearing JSON .a3c fixture should contain the manifest-declared type source", typeEntry);
      assertTrue(readEntry(zipFile, typeEntry).contains("GeneratedJsonTypeWithConstructorBoundary()"));
      assertTypeReference(
          manifest,
          "GeneratedJsonTypeWithConstructorBoundary",
          "src/GeneratedJsonTypeWithConstructorBoundary.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
    }
    TypeResourcesPair typeResourcesPair = IoUtilities.readType(typeArchive);

    NamedUserType readType = typeResourcesPair.getType();
    assertEquals("GeneratedJsonTypeWithConstructorBoundary", readType.getName());
    assertEmptyConstructor(readType);
    Resource readResource = onlyResource(typeResourcesPair.getResources());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals(imageResource.getName(), readResource.getName());
  }

  @Test
  public void constructorAssignmentJsonTypeArchiveDecodesFieldAssignment() throws Exception {
    File typeArchive = temporaryFolder.newFile("constructor-assignment-json-a3c-boundary.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonTypeWithConstructorAssignmentBoundary",
        "class GeneratedJsonTypeWithConstructorAssignmentBoundary extends SProgram { "
            + "WholeNumber count; "
            + "GeneratedJsonTypeWithConstructorAssignmentBoundary() { this.count <- 1; } "
            + "}");

    String typeSourceEntry = "src/GeneratedJsonTypeWithConstructorAssignmentBoundary.twe";
    try (ZipFile zipFile = new ZipFile(typeArchive)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      Set<String> entryNames = zipFile.stream().map(ZipEntry::getName).collect(Collectors.toSet());
      assertEquals(
          "Constructor-assignment JSON .a3c fixture should contain only version, manifest, and Tweedle source entries",
          new HashSet<>(Arrays.asList(ProjectIo.VERSION_ENTRY_NAME, ProjectIo.MANIFEST_ENTRY_NAME, typeSourceEntry)),
          entryNames);
      assertEquals(IoUtilities.TYPE_EXTENSION, manifest.metadata.fileType);
      assertEquals("GeneratedJsonTypeWithConstructorAssignmentBoundary", manifest.metadata.identifier.name);
      assertEquals(Manifest.ProjectType.Library, manifest.metadata.identifier.type);
      assertEquals("GeneratedJsonTypeWithConstructorAssignmentBoundary", manifest.description.name);
      assertTypeReference(
          manifest,
          "GeneratedJsonTypeWithConstructorAssignmentBoundary",
          typeSourceEntry);
      ZipEntry typeEntry = zipFile.getEntry(typeSourceEntry);
      assertNotNull(
          "Constructor-assignment JSON .a3c fixture should contain the manifest-declared type source",
          typeEntry);
      assertTrue(readEntry(zipFile, typeEntry).contains("this.count <- 1"));
    }

    TypeResourcesPair typeResourcesPair = IoUtilities.readType(typeArchive);

    NamedUserType readType = typeResourcesPair.getType();
    assertNotNull("Constructor-assignment JSON .a3c fixture should decode the type", readType);
    assertEquals("GeneratedJsonTypeWithConstructorAssignmentBoundary", readType.getName());
    assertEquals("SProgram", readType.getSuperType().getName());
    assertConstructorAssignsIntegerField(readType, "count", 1);
    assertTrue("Generated constructor-assignment JSON .a3c fixture should not require resources",
        typeResourcesPair.getResources().isEmpty());
  }

  @Test
  public void complexInitializerJsonTypeArchiveIsRejectedWithoutPartialTypeDecode() throws Exception {
    ImageResource imageResource = generatedImageResource("json-type-complex-initializer-boundary-texture.png", 0xFF666633);
    File typeArchive = temporaryFolder.newFile("complex-initializer-json-a3c-boundary.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonTypeWithComplexInitializerBoundary",
        "class GeneratedJsonTypeWithComplexInitializerBoundary extends SProgram { WholeNumber count <- 1 + 2; }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(typeArchive)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      ZipEntry typeEntry = zipFile.getEntry("src/GeneratedJsonTypeWithComplexInitializerBoundary.twe");
      assertNotNull("Complex-initializer JSON .a3c fixture should contain the manifest-declared type source", typeEntry);
      assertTrue(readEntry(zipFile, typeEntry).contains("WholeNumber count <- 1 + 2"));
      assertTypeReference(
          manifest,
          "GeneratedJsonTypeWithComplexInitializerBoundary",
          "src/GeneratedJsonTypeWithComplexInitializerBoundary.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeArchive));

    assertTrue(thrown.getMessage().contains(
        "Type archive manifest names 'GeneratedJsonTypeWithComplexInitializerBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
  }

  @Test
  public void nullInitializerJsonTypeArchiveDecodesTextFieldInitializer() throws Exception {
    ImageResource imageResource = generatedImageResource("json-type-null-initializer-boundary-texture.png", 0xFF663333);
    File typeArchive = temporaryFolder.newFile("null-initializer-json-a3c-boundary.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonTypeWithNullInitializerBoundary",
        "class GeneratedJsonTypeWithNullInitializerBoundary extends SProgram { TextString label <- null; }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(typeArchive)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      ZipEntry typeEntry = zipFile.getEntry("src/GeneratedJsonTypeWithNullInitializerBoundary.twe");
      assertNotNull("Null-initializer JSON .a3c fixture should contain the manifest-declared type source", typeEntry);
      assertTrue(readEntry(zipFile, typeEntry).contains("TextString label <- null"));
      assertTypeReference(
          manifest,
          "GeneratedJsonTypeWithNullInitializerBoundary",
          "src/GeneratedJsonTypeWithNullInitializerBoundary.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
    }
    TypeResourcesPair readPair = IoUtilities.readType(typeArchive);

    NamedUserType readType = readPair.getType();
    assertNotNull("Null-initializer JSON .a3c fixture should decode the type", readType);
    assertEquals("GeneratedJsonTypeWithNullInitializerBoundary", readType.getName());
    assertEquals("SProgram", readType.getSuperType().getName());
    assertEquals(1, readType.getDeclaredFields().size());
    UserField field = readType.getDeclaredFields().get(0);
    assertEquals("label", field.getName());
    assertTrue(field.initializer.getValue() instanceof NullLiteral);

    Resource readResource = onlyResource(readPair.getResources());
    assertEquals(imageResource.getId(), readResource.getId());
    assertEquals(imageResource.getName(), readResource.getName());
    assertEquals(imageResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(imageResource.getContentType(), readResource.getContentType());
    assertArrayEquals(imageResource.getData(), readResource.getData());
  }

  @Test
  public void unresolvedParentJsonTypeArchiveIsRejectedWithoutPartialTypeDecode() throws Exception {
    ImageResource imageResource = generatedImageResource("json-type-unresolved-parent-boundary-texture.png", 0xFF336699);
    File typeArchive = temporaryFolder.newFile("unresolved-parent-json-a3c-boundary.a3c");

    writeJsonTypeArchive(
        typeArchive,
        "GeneratedJsonTypeWithUnresolvedParentBoundary",
        "class GeneratedJsonTypeWithUnresolvedParentBoundary extends MissingLegacyParentType { WholeNumber count; }",
        imageResource);

    try (ZipFile zipFile = new ZipFile(typeArchive)) {
      TypeManifest manifest = readTypeManifest(zipFile);
      ZipEntry typeEntry = zipFile.getEntry("src/GeneratedJsonTypeWithUnresolvedParentBoundary.twe");
      assertNotNull("Unresolved-parent JSON .a3c fixture should contain the manifest-declared type source", typeEntry);
      assertTrue(readEntry(zipFile, typeEntry).contains("extends MissingLegacyParentType"));
      assertTypeReference(
          manifest,
          "GeneratedJsonTypeWithUnresolvedParentBoundary",
          "src/GeneratedJsonTypeWithUnresolvedParentBoundary.twe");
      assertImageReference(
          manifest,
          imageResource.getId(),
          imageResource.getName(),
          "resources/" + imageResource.getName());
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeArchive));

    assertTrue(thrown.getMessage().contains(
        "Type archive manifest names 'GeneratedJsonTypeWithUnresolvedParentBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
  }

  @Test
  public void generatedProjectArchiveCharacterizesXmlFallbackProjectRoundTripWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-project-texture.png", 0xFF996633);
    Project project = new Project(
        typeReferencingImageResource("GeneratedHistoricalProject", imageResource),
        Project.SceneCameraType.VRHeadset);
    project.addResource(imageResource);
    File projectArchive = temporaryFolder.newFile("generated-historical-project.a3p");

    IoUtilities.writeProject(projectArchive, project);

    assertXmlProjectArchiveFacts(
        projectArchive,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);
    Project firstRead = IoUtilities.readProject(projectArchive);
    assertProjectResourceFacts(
        firstRead,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-project-roundtrip.a3p");
    IoUtilities.writeProject(roundTripArchive, firstRead);

    assertXmlProjectArchiveFacts(
        roundTripArchive,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);
    Project secondRead = IoUtilities.readProject(roundTripArchive);
    assertProjectResourceFacts(
        secondRead,
        "GeneratedHistoricalProject",
        Project.SceneCameraType.VRHeadset,
        imageResource);
  }

  @Test
  public void generatedXmlArchiveBeforeSupportedMigrationFloorFailsClosed() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-xml-before-migration-floor.a3p");

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(projectArchive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          "3.0.0.0".getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "programType.xml",
          "<node version=\"3.0\" type=\"org.lgna.project.ast.NamedUserType\"/>"
              .getBytes(StandardCharsets.UTF_8));
    }
    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      assertNotNull(zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
      assertNotNull(zipFile.getEntry("programType.xml"));
      assertNull(zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
    }

    VersionNotSupportedException thrown =
        assertThrows(VersionNotSupportedException.class, () -> IoUtilities.readProject(projectArchive));

    assertEquals("XML decoder floor should be explicit", 3.1, thrown.getMinimumSupportedVersion(), 0.0);
    assertEquals("Synthetic archive should fail on its declared XML version", 3.0, thrown.getVersion(), 0.0);
  }

  private static void assertXmlTypeArchiveFacts(File archive, String expectedTypeName) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
      assertNotNull("Generated .a3c archives should declare a version", versionEntry);
      assertEquals(ProjectVersion.getCurrentVersion().toString(), readEntry(zipFile, versionEntry).trim());
      assertNotNull("Generated .a3c archives should contain the XML type payload", zipFile.getEntry("type.xml"));
      assertNotNull("Generated .a3c archives should contain resource metadata", zipFile.getEntry("resources.xml"));
      assertNotNull("Generated .a3c archives should contain generated image data",
          zipFile.getEntry("resources/historical-type-texture.png"));
      assertNotNull("Hybrid .a3c archives should also contain the Tweedle manifest",
          zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull("Hybrid .a3c archives should also contain the Tweedle source payload",
          zipFile.getEntry("src/" + expectedTypeName + ".twe"));
      Manifest manifest = readTypeManifest(zipFile);
      assertEquals(IoUtilities.TYPE_EXTENSION, manifest.metadata.fileType);
      assertTrue("Hybrid .a3c manifest should reference the Tweedle type source",
          containsTypeReference(manifest, expectedTypeName));
    }
  }

  private static void assertXmlProjectArchiveFacts(
      File archive,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType,
      ImageResource expectedResource) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
      assertNotNull("Generated .a3p archives should declare a version", versionEntry);
      assertEquals(ProjectVersion.getCurrentVersion().toString(), readEntry(zipFile, versionEntry).trim());
      assertNotNull("Generated .a3p archives should contain a manifest",
          zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
      assertNotNull("Generated .a3p archives should contain the XML program payload",
          zipFile.getEntry("programType.xml"));
      assertNotNull("Generated .a3p archives should contain resource metadata",
          zipFile.getEntry("resources.xml"));
      assertNotNull("Generated .a3p archives should contain generated image data",
          zipFile.getEntry("resources/" + expectedResource.getName()));
      assertNotNull("Hybrid .a3p archives should also carry the Tweedle source payload alongside XML",
          zipFile.getEntry("src/" + expectedProgramName + ".twe"));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(expectedProgramName, manifest.description.name);
      assertEquals(IoUtilities.PROJECT_EXTENSION, manifest.metadata.fileType);
      assertEquals(Manifest.ProjectType.World, manifest.metadata.identifier.type);
      assertEquals(expectedSceneCameraType, manifest.projectStructure.sceneCameraType);
      assertTrue("Hybrid .a3p manifest should reference the Tweedle program source",
          containsTypeReference(manifest, expectedProgramName));
    }
  }

  private static boolean containsTypeReference(Manifest manifest, String typeName) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference && typeName.equals(typeReference.name)) {
        return true;
      }
    }
    return false;
  }

  private static void assertTypeFacts(TypeResourcesPair typeResourcesPair, String expectedTypeName, ImageResource expectedResource) {
    assertNotNull("Generated .a3c archive should read a type/resources pair", typeResourcesPair);
    NamedUserType readType = typeResourcesPair.getType();
    assertNotNull("Generated .a3c archive should rehydrate a type", readType);
    assertEquals(expectedTypeName, readType.getName());
    assertEquals("SProgram", readType.getSuperType().getName());

    Resource readResource = onlyResource(typeResourcesPair.getResources());
    assertEquals(expectedResource.getId(), readResource.getId());
    assertEquals(expectedResource.getName(), readResource.getName());
    assertEquals(expectedResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(expectedResource.getContentType(), readResource.getContentType());
    assertArrayEquals(expectedResource.getData(), readResource.getData());
    assertEquals(readResource, firstResourceExpressionResource(readType));
  }

  private static void assertProjectResourceFacts(
      Project project,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType,
      ImageResource expectedResource) throws Exception {
    assertNotNull("Generated .a3p archive should reopen as a project", project);
    NamedUserType readType = project.getProgramType();
    assertNotNull("Generated .a3p XML fallback should rehydrate the program type", readType);
    assertEquals(expectedProgramName, readType.getName());
    assertEquals("SProgram", readType.getSuperType().getName());
    assertEquals(expectedSceneCameraType, sceneCameraType(project));

    Resource readResource = onlyResource(project.getResources());
    assertEquals(expectedResource.getId(), readResource.getId());
    assertEquals(expectedResource.getName(), readResource.getName());
    assertEquals(expectedResource.getOriginalFileName(), readResource.getOriginalFileName());
    assertEquals(expectedResource.getContentType(), readResource.getContentType());
    assertArrayEquals(expectedResource.getData(), readResource.getData());
    assertSame("Resource expressions should be rebound to the resource decoded from the archive",
        readResource,
        firstResourceExpressionResource(readType));
  }

  private static void assertWorldManifestFacts(
      File archive,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      assertWorldManifestFacts(zipFile, expectedProgramName, expectedSceneCameraType);
    }
  }

  private static ProjectManifest assertWorldManifestFacts(
      ZipFile zipFile,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType) throws Exception {
    assertNotNull("Generated .a3w archives should declare a version",
        zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME));
    assertNotNull("Generated .a3w archives should contain a manifest",
        zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
    assertNotNull("Generated .a3w archives should contain Tweedle source for the program",
        zipFile.getEntry("src/" + expectedProgramName + ".twe"));

    ProjectManifest manifest = readProjectManifest(zipFile);
    assertEquals(expectedProgramName, manifest.description.name);
    assertEquals(IoUtilities.EXPORT_EXTENSION, manifest.metadata.fileType);
    assertEquals(Manifest.ProjectType.World, manifest.metadata.identifier.type);
    assertEquals(expectedSceneCameraType, manifest.projectStructure.sceneCameraType);
    assertTrue("Generated .a3w archives should include the standard library prerequisite",
        manifest.prerequisites.stream().anyMatch(identifier -> "SceneGraphLibrary".equals(identifier.name)));
    assertTypeReference(manifest, expectedProgramName, "src/" + expectedProgramName + ".twe");
    return manifest;
  }

  private static void assertWorldResourceManifestFacts(
      File archive,
      String expectedProgramName,
      ImageResource expectedResource) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ProjectManifest manifest =
          assertWorldManifestFacts(zipFile, expectedProgramName, Project.SceneCameraType.WindowCamera);
      assertNotNull("Generated .a3w archives should contain exported image data",
          zipFile.getEntry("resources/" + expectedResource.getName()));

      assertImageReference(
          manifest,
          expectedResource.getId(),
          expectedResource.getName(),
          "resources/" + expectedResource.getName());
    }
  }

  private static void assertProgramFacts(
      Project project,
      String expectedProgramName,
      Project.SceneCameraType expectedSceneCameraType) throws Exception {
    assertNotNull("Generated .a3w archive should reopen as a project", project);
    assertNotNull("Simple generated .a3w program source should decode", project.getProgramType());
    assertEquals(expectedProgramName, project.getProgramType().getName());
    assertEquals("SProgram", project.getProgramType().getSuperType().getName());
    assertEquals(expectedSceneCameraType, sceneCameraType(project));
    assertTrue(project.getResources().isEmpty());
  }

  private static void assertTypeReference(Manifest manifest, String expectedName, String expectedFile) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference
          && Objects.equals(expectedName, typeReference.name)) {
        assertEquals(expectedFile, typeReference.file);
        assertEquals("tweedle", typeReference.format);
        return;
      }
    }
    throw new AssertionError("Missing type reference for " + expectedName);
  }

  private static void assertUnnamedTypeReference(Manifest manifest, String expectedFile) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof TypeReference typeReference && typeReference.name == null) {
        assertEquals(expectedFile, typeReference.file);
        assertEquals("tweedle", typeReference.format);
        return;
      }
    }
    throw new AssertionError("Missing unnamed type reference for " + expectedFile);
  }

  private static void assertImageReference(Manifest manifest, UUID expectedId, String expectedName, String expectedFile) {
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference instanceof ImageReference imageReference && expectedId.equals(imageReference.uuid)) {
        assertEquals(expectedName, imageReference.name);
        assertEquals(expectedFile, imageReference.file);
        assertEquals("png", imageReference.format);
        assertEquals(1.0, imageReference.width, 0.0);
        assertEquals(1.0, imageReference.height, 0.0);
        return;
      }
    }
    throw new AssertionError("Missing image reference for " + expectedId);
  }

  private static ProjectManifest readProjectManifest(ZipFile zipFile) throws Exception {
    ZipEntry manifestEntry = zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME);
    assertNotNull("Generated project archive should contain " + ProjectIo.MANIFEST_ENTRY_NAME, manifestEntry);
    return ManifestEncoderDecoder.fromJson(
        readEntry(zipFile, manifestEntry),
        ProjectManifest.class);
  }

  private static TypeManifest readTypeManifest(ZipFile zipFile) throws Exception {
    ZipEntry manifestEntry = zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME);
    assertNotNull("Generated type archive should contain " + ProjectIo.MANIFEST_ENTRY_NAME, manifestEntry);
    return ManifestEncoderDecoder.fromJson(
        readEntry(zipFile, manifestEntry),
        TypeManifest.class);
  }

  private static void writeJsonTypeArchive(
      File archive,
      String typeName,
      String tweedleSource) throws Exception {
    TypeManifest manifest = new TypeManifest();
    manifest.description.name = typeName;
    manifest.metadata.fileType = IoUtilities.TYPE_EXTENSION;
    manifest.metadata.identifier.name = typeName;
    manifest.metadata.identifier.type = Manifest.ProjectType.Library;
    manifest.resources.add(new TypeReference(typeName, "src/" + typeName + ".twe", "tweedle"));

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          CURRENT_VERSION_ENTRY_BYTES);
      writeEntry(
          zipOutputStream,
          ProjectIo.MANIFEST_ENTRY_NAME,
          ManifestEncoderDecoder.toJson(manifest).getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + typeName + ".twe",
          tweedleSource.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void writeJsonTypeArchive(
      File archive,
      String typeName,
      String tweedleSource,
      ImageResource imageResource) throws Exception {
    TypeManifest manifest = new TypeManifest();
    manifest.description.name = typeName;
    manifest.metadata.fileType = IoUtilities.TYPE_EXTENSION;
    manifest.metadata.identifier.name = typeName;
    manifest.metadata.identifier.type = Manifest.ProjectType.Library;
    manifest.resources.add(new TypeReference(typeName, "src/" + typeName + ".twe", "tweedle"));

    ImageReference imageReference = new ImageReference(imageResource);
    imageReference.file = "resources/" + imageResource.getName();
    manifest.resources.add(imageReference);

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          CURRENT_VERSION_ENTRY_BYTES);
      writeEntry(
          zipOutputStream,
          ProjectIo.MANIFEST_ENTRY_NAME,
          ManifestEncoderDecoder.toJson(manifest).getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + typeName + ".twe",
          tweedleSource.getBytes(StandardCharsets.UTF_8));
      writeEntry(zipOutputStream, imageReference.file, imageResource.getData());
    }
  }

  private static void writeJsonProjectArchive(
      File archive,
      String programTypeName,
      String programTweedleSource,
      String sceneTypeName,
      String sceneTweedleSource) throws Exception {
    writeJsonProjectArchive(
        archive,
        programTypeName,
        programTweedleSource,
        sceneTypeName,
        sceneTweedleSource,
        null);
  }

  private static void writeJsonProjectArchive(
      File archive,
      String programTypeName,
      String programTweedleSource,
      String sceneTypeName,
      String sceneTweedleSource,
      ImageResource imageResource) throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = programTypeName;
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = programTypeName;
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(new TypeReference(programTypeName, "src/" + programTypeName + ".twe", "tweedle"));
    manifest.resources.add(new TypeReference(sceneTypeName, "src/" + sceneTypeName + ".twe", "tweedle"));
    ImageReference imageReference = null;
    if (imageResource != null) {
      imageReference = new ImageReference(imageResource);
      imageReference.file = "resources/" + imageResource.getName();
      manifest.resources.add(imageReference);
    }

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          CURRENT_VERSION_ENTRY_BYTES);
      writeEntry(
          zipOutputStream,
          ProjectIo.MANIFEST_ENTRY_NAME,
          ManifestEncoderDecoder.toJson(manifest).getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + programTypeName + ".twe",
          programTweedleSource.getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + sceneTypeName + ".twe",
          sceneTweedleSource.getBytes(StandardCharsets.UTF_8));
      if (imageReference != null) {
        writeEntry(zipOutputStream, imageReference.file, imageResource.getData());
      }
    }
  }

  private static void writeJsonProjectArchiveOmittingProgramEntry(
      File archive,
      String programTypeName,
      String sceneTypeName,
      String sceneTweedleSource) throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = programTypeName;
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = programTypeName;
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(new TypeReference(programTypeName, "src/" + programTypeName + ".twe", "tweedle"));
    manifest.resources.add(new TypeReference(sceneTypeName, "src/" + sceneTypeName + ".twe", "tweedle"));

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          CURRENT_VERSION_ENTRY_BYTES);
      writeEntry(
          zipOutputStream,
          ProjectIo.MANIFEST_ENTRY_NAME,
          ManifestEncoderDecoder.toJson(manifest).getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + sceneTypeName + ".twe",
          sceneTweedleSource.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void writeJsonProjectArchiveOmittingSceneEntry(
      File archive,
      String programTypeName,
      String programTweedleSource,
      String sceneTypeName) throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = programTypeName;
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = programTypeName;
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(new TypeReference(programTypeName, "src/" + programTypeName + ".twe", "tweedle"));
    manifest.resources.add(new TypeReference(sceneTypeName, "src/" + sceneTypeName + ".twe", "tweedle"));

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          CURRENT_VERSION_ENTRY_BYTES);
      writeEntry(
          zipOutputStream,
          ProjectIo.MANIFEST_ENTRY_NAME,
          ManifestEncoderDecoder.toJson(manifest).getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + programTypeName + ".twe",
          programTweedleSource.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void writeJsonProjectArchiveWithUnnamedSiblingTypeReference(
      File archive,
      String programTypeName,
      String programTweedleSource,
      String siblingTypeName,
      String siblingTweedleSource) throws Exception {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = programTypeName;
    manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
    manifest.metadata.identifier.name = programTypeName;
    manifest.metadata.identifier.type = Manifest.ProjectType.World;
    manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
    manifest.resources.add(new TypeReference(programTypeName, "src/" + programTypeName + ".twe", "tweedle"));
    manifest.resources.add(new TypeReference(null, "src/" + siblingTypeName + ".twe", "tweedle"));

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(archive))) {
      writeEntry(
          zipOutputStream,
          ProjectIo.VERSION_ENTRY_NAME,
          CURRENT_VERSION_ENTRY_BYTES);
      writeEntry(
          zipOutputStream,
          ProjectIo.MANIFEST_ENTRY_NAME,
          ManifestEncoderDecoder.toJson(manifest).getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + programTypeName + ".twe",
          programTweedleSource.getBytes(StandardCharsets.UTF_8));
      writeEntry(
          zipOutputStream,
          "src/" + siblingTypeName + ".twe",
          siblingTweedleSource.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void writeEntry(ZipOutputStream zipOutputStream, String entryName, byte[] bytes) throws IOException {
    zipOutputStream.putNextEntry(new ZipEntry(entryName));
    zipOutputStream.write(bytes);
    zipOutputStream.closeEntry();
  }

  private static String readEntry(ZipFile zipFile, ZipEntry zipEntry) throws Exception {
    assertNotNull("Expected archive entry to exist", zipEntry);
    return new String(zipFile.getInputStream(zipEntry).readAllBytes(), StandardCharsets.UTF_8);
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static NamedUserType typeReferencingImageResource(String name, ImageResource imageResource) {
    NamedUserType type = programType(name);
    BlockStatement body = new BlockStatement();
    UserLocal resource = new UserLocal("resource", ImageResource.class, true);
    body.statements.add(new LocalDeclarationStatement(resource, new ResourceExpression(ImageResource.class, imageResource)));
    type.methods.add(new UserMethod("rememberGeneratedFixtureResource", Void.TYPE, new org.lgna.project.ast.UserParameter[0], body));
    return type;
  }

  private static ImageResource generatedImageResource(String fileName, int rgb) throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, rgb);
    return new ImageResource(image, fileName, "png");
  }

  private static Resource onlyResource(Collection<Resource> resources) {
    assertEquals(1, resources.size());
    return resources.iterator().next();
  }

  private static void assertEmptyConstructor(NamedUserType type) {
    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertTrue(constructor.getRequiredParameters().isEmpty());
    assertTrue(constructor.body.getValue().statements.isEmpty());
  }

  private static void assertConstructorAssignsIntegerField(NamedUserType type, String expectedFieldName, int expectedValue) {
    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals(expectedFieldName, field.getName());
    assertSame(JavaType.getInstance(Integer.class), field.getValueType());

    assertEquals(1, type.getDeclaredConstructors().size());
    NamedUserConstructor constructor = (NamedUserConstructor) type.getDeclaredConstructors().get(0);
    assertTrue(constructor.getRequiredParameters().isEmpty());
    StatementListProperty statements = constructor.body.getValue().statements;
    assertEquals(1, statements.size());
    assertTrue(statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement expressionStatement = (ExpressionStatement) statements.get(0);
    assertTrue(expressionStatement.expression.getValue() instanceof AssignmentExpression);
    AssignmentExpression assignment = (AssignmentExpression) expressionStatement.expression.getValue();
    assertSame(JavaType.getInstance(Integer.class), assignment.expressionType.getValue());
    assertEquals(AssignmentExpression.Operator.ASSIGN, assignment.operator.getValue());
    assertTrue(assignment.leftHandSide.getValue() instanceof FieldAccess);
    FieldAccess fieldAccess = (FieldAccess) assignment.leftHandSide.getValue();
    assertTrue(fieldAccess.expression.getValue() instanceof ThisExpression);
    assertSame(field, fieldAccess.field.getValue());
    assertTrue(assignment.rightHandSide.getValue() instanceof IntegerLiteral);
    assertEquals(
        expectedValue,
        ((IntegerLiteral) assignment.rightHandSide.getValue()).value.getValue().intValue());
  }

  private static void assertPrimitiveReturnMethod(NamedUserType type, String expectedName, int expectedValue) {
    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(expectedName, method.getName());
    assertSame(JavaType.getInstance(Integer.class), method.getReturnType());
    assertTrue(method.getRequiredParameters().isEmpty());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertTrue(returnStatement.expression.getValue() instanceof IntegerLiteral);
    assertEquals(
        expectedValue,
        ((IntegerLiteral) returnStatement.expression.getValue()).value.getValue().intValue());
  }

  private static void assertFieldReturnMethod(NamedUserType type, String expectedMethodName, String expectedFieldName) {
    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals(expectedFieldName, field.getName());
    assertEquals(1, type.getDeclaredMethods().size());
    UserMethod method = type.getDeclaredMethods().get(0);
    assertEquals(expectedMethodName, method.getName());
    assertSame(JavaType.getInstance(Integer.class), method.getReturnType());
    assertTrue(method.getRequiredParameters().isEmpty());
    assertEquals(1, method.body.getValue().statements.size());
    assertTrue(method.body.getValue().statements.get(0) instanceof ReturnStatement);
    ReturnStatement returnStatement = (ReturnStatement) method.body.getValue().statements.get(0);
    assertSame(JavaType.getInstance(Integer.class), returnStatement.expressionType.getValue());
    assertTrue(returnStatement.expression.getValue() instanceof FieldAccess);
    assertSame(field, ((FieldAccess) returnStatement.expression.getValue()).field.getValue());
  }

  private static void assertSimpleIfMethodInvocation(
      NamedUserType type,
      String callerName,
      String targetName,
      RelationalInfixExpression.Operator expectedOperator) {
    UserMethod caller = userMethodNamed(type, callerName);
    UserMethod target = userMethodNamed(type, targetName);
    assertSame(JavaType.VOID_TYPE, caller.getReturnType());
    assertEquals(1, caller.getRequiredParameters().size());
    assertEquals(1, caller.body.getValue().statements.size());
    assertTrue(caller.body.getValue().statements.get(0) instanceof ConditionalStatement);
    ConditionalStatement conditional = (ConditionalStatement) caller.body.getValue().statements.get(0);
    assertEquals(1, conditional.booleanExpressionBodyPairs.size());
    BooleanExpressionBodyPair pair = conditional.booleanExpressionBodyPairs.get(0);
    assertTrue(pair.expression.getValue() instanceof RelationalInfixExpression);
    RelationalInfixExpression condition = (RelationalInfixExpression) pair.expression.getValue();
    assertSame(expectedOperator, condition.operator.getValue());
    assertEquals(1, pair.body.getValue().statements.size());
    assertTrue(pair.body.getValue().statements.get(0) instanceof ExpressionStatement);
    ExpressionStatement statement = (ExpressionStatement) pair.body.getValue().statements.get(0);
    assertTrue(statement.expression.getValue() instanceof MethodInvocation);
    MethodInvocation invocation = (MethodInvocation) statement.expression.getValue();
    assertTrue(invocation.expression.getValue() instanceof ThisExpression);
    assertSame(target, invocation.method.getValue());
    assertTrue(invocation.requiredArguments.isEmpty());
    assertTrue(invocation.variableArguments.isEmpty());
    assertTrue(invocation.keyedArguments.isEmpty());
    assertTrue(conditional.elseBody.getValue().statements.isEmpty());
  }

  private static UserMethod userMethodNamed(NamedUserType type, String name) {
    return type.getDeclaredMethods().stream()
        .filter(method -> name.equals(method.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing method: " + name));
  }

  private static void assertArithmeticFieldInitializer(
      NamedUserType type,
      String expectedFieldName,
      ArithmeticInfixExpression.Operator expectedOperator,
      int expectedLeft,
      int expectedRight) {
    assertEquals(1, type.getDeclaredFields().size());
    UserField field = type.getDeclaredFields().get(0);
    assertEquals(expectedFieldName, field.getName());
    assertSame(JavaType.getInstance(Integer.class), field.getValueType());
    assertTrue(field.initializer.getValue() instanceof ArithmeticInfixExpression);
    ArithmeticInfixExpression initializer = (ArithmeticInfixExpression) field.initializer.getValue();
    assertSame(expectedOperator, initializer.operator.getValue());
    assertSame(JavaType.getInstance(Integer.class), initializer.getType());
    assertTrue(initializer.leftOperand.getValue() instanceof IntegerLiteral);
    assertEquals(expectedLeft, ((IntegerLiteral) initializer.leftOperand.getValue()).value.getValue().intValue());
    assertTrue(initializer.rightOperand.getValue() instanceof IntegerLiteral);
    assertEquals(expectedRight, ((IntegerLiteral) initializer.rightOperand.getValue()).value.getValue().intValue());
  }

  private static NamedUserType namedUserTypeNamed(Project project, String name) {
    return project.getNamedUserTypes().stream()
        .filter(type -> name.equals(type.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing decoded user type " + name));
  }

  private static Resource firstResourceExpressionResource(NamedUserType type) {
    IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<ResourceExpression>(ResourceExpression.class) {
      @Override
      protected boolean isAcceptable(ResourceExpression resourceExpression) {
        return true;
      }
    };
    type.crawl(crawler, CrawlPolicy.COMPLETE);
    assertFalse(crawler.getList().isEmpty());
    return crawler.getList().get(0).resource.getValue();
  }

  private static Project.SceneCameraType sceneCameraType(Project project) throws Exception {
    Field field = Project.class.getDeclaredField("sceneCameraType");
    field.setAccessible(true);
    return (Project.SceneCameraType) field.get(project);
  }
}
