package org.lgna.project.io;

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
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
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
import java.util.Collection;
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
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void generatedClassArchiveCharacterizesXmlFallbackTypeRoundTripWithoutExternalFixture() throws Exception {
    ImageResource imageResource = generatedImageResource("historical-type-texture.png", 0xFF336699);
    NamedUserType generatedType = typeReferencingImageResource("GeneratedHistoricalType", imageResource);
    File typeArchive = temporaryFolder.newFile("generated-historical-type.a3c");

    IoUtilities.writeType(typeArchive, generatedType);

    assertXmlTypeArchiveFacts(typeArchive);
    TypeResourcesPair firstRead = IoUtilities.readType(typeArchive);
    assertTypeFacts(firstRead, "GeneratedHistoricalType", imageResource);

    File roundTripArchive = temporaryFolder.newFile("generated-historical-type-roundtrip.a3c");
    IoUtilities.writeType(roundTripArchive, firstRead.getType());

    assertXmlTypeArchiveFacts(roundTripArchive);
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
  public void generatedJsonPlayerArchiveWithMethodBearingProgramTypeIsRejectedWithoutPartialProgramDecode() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-method-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithMethodBoundary",
        "class GeneratedProgramWithMethodBoundary extends SProgram { WholeNumber count() { return 1; } }",
        "GeneratedMethodBoundaryScene",
        "class GeneratedMethodBoundaryScene extends SScene {}");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramWithMethodBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are [GeneratedMethodBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithMethodBearingSiblingTypeIsRejectedWithoutSilentOmission() throws Exception {
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
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive contains unsupported manifest-declared Tweedle type names [GeneratedMethodSiblingBoundaryScene]"));
  }

  @Test
  public void constructorBearingJsonA3wProgramTypeIsRejected() throws Exception {
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
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramWithConstructorBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are [GeneratedConstructorBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithConstructorBearingSiblingTypeIsRejectedWithoutSilentOmission() throws Exception {
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
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive contains unsupported manifest-declared Tweedle type names [GeneratedConstructorSiblingBoundaryScene]"));
  }

  @Test
  public void jsonProjectArchiveWithOnlyUnsupportedManifestTypesIsRejected() throws Exception {
    File projectArchive = temporaryFolder.newFile("all-unsupported-json-a3w-types-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramAllUnsupportedBoundary",
        "class GeneratedProgramAllUnsupportedBoundary extends SProgram { GeneratedProgramAllUnsupportedBoundary() { } }",
        "GeneratedSceneAllUnsupportedBoundary",
        "class GeneratedSceneAllUnsupportedBoundary extends SScene { GeneratedSceneAllUnsupportedBoundary() { } }");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramAllUnsupportedBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithComplexProgramInitializerIsRejectedWithoutPartialProgramDecode() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-complex-initializer-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithComplexInitializerBoundary",
        "class GeneratedProgramWithComplexInitializerBoundary extends SProgram { WholeNumber count <- 1 + 2; }",
        "GeneratedComplexInitializerBoundaryScene",
        "class GeneratedComplexInitializerBoundaryScene extends SScene {}");

    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive manifest names program type 'GeneratedProgramWithComplexInitializerBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are [GeneratedComplexInitializerBoundaryScene]"));
  }

  @Test
  public void generatedJsonPlayerArchiveWithComplexInitializerSiblingTypeIsRejectedWithoutSilentOmission() throws Exception {
    File projectArchive = temporaryFolder.newFile("generated-json-player-complex-initializer-sibling-boundary.a3w");

    writeJsonProjectArchive(
        projectArchive,
        "GeneratedProgramWithComplexInitializerSiblingBoundary",
        "class GeneratedProgramWithComplexInitializerSiblingBoundary extends SProgram { WholeNumber count; }",
        "GeneratedComplexInitializerSiblingBoundaryScene",
        "class GeneratedComplexInitializerSiblingBoundaryScene extends SScene { WholeNumber count <- 1 + 2; }");

    try (ZipFile zipFile = new ZipFile(projectArchive)) {
      ProjectManifest manifest = readProjectManifest(zipFile);
      assertTypeReference(
          manifest,
          "GeneratedProgramWithComplexInitializerSiblingBoundary",
          "src/GeneratedProgramWithComplexInitializerSiblingBoundary.twe");
      assertTypeReference(
          manifest,
          "GeneratedComplexInitializerSiblingBoundaryScene",
          "src/GeneratedComplexInitializerSiblingBoundaryScene.twe");
      ZipEntry siblingTypeEntry = zipFile.getEntry("src/GeneratedComplexInitializerSiblingBoundaryScene.twe");
      assertNotNull(
          "Generated JSON .a3w fixture should contain the complex-initializer sibling type source",
          siblingTypeEntry);
      assertTrue(readEntry(zipFile, siblingTypeEntry).contains("WholeNumber count <- 1 + 2"));
    }
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));

    assertTrue(thrown.getMessage().contains(
        "Project archive contains unsupported manifest-declared Tweedle type names [GeneratedComplexInitializerSiblingBoundaryScene]"));
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
  public void methodBearingJsonTypeArchiveIsRejectedWithoutPartialTypeDecode() throws Exception {
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
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeArchive));

    assertTrue(thrown.getMessage().contains(
        "Type archive manifest names 'GeneratedJsonTypeWithMethodBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
  }

  @Test
  public void constructorBearingJsonTypeArchiveIsRejectedWithoutPartialTypeDecode() throws Exception {
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
    IOException thrown = assertThrows(IOException.class, () -> IoUtilities.readType(typeArchive));

    assertTrue(thrown.getMessage().contains(
        "Type archive manifest names 'GeneratedJsonTypeWithConstructorBoundary'"));
    assertTrue(thrown.getMessage().contains("decoded type names are []"));
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

  private static void assertXmlTypeArchiveFacts(File archive) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntry versionEntry = zipFile.getEntry(ProjectIo.VERSION_ENTRY_NAME);
      assertNotNull("Generated .a3c archives should declare a version", versionEntry);
      assertEquals(ProjectVersion.getCurrentVersion().toString(), readEntry(zipFile, versionEntry).trim());
      assertNotNull("Generated .a3c archives should contain the XML type payload", zipFile.getEntry("type.xml"));
      assertNotNull("Generated .a3c archives should contain resource metadata", zipFile.getEntry("resources.xml"));
      assertNotNull("Generated .a3c archives should contain generated image data",
          zipFile.getEntry("resources/historical-type-texture.png"));
      assertNull("Generated .a3c fixtures intentionally exercise XML fallback rather than JSON manifest loading",
          zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME));
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
      assertNull("Generated .a3p fixtures intentionally exercise XML fallback despite manifest.json",
          zipFile.getEntry("src/" + expectedProgramName + ".twe"));

      ProjectManifest manifest = readProjectManifest(zipFile);
      assertEquals(expectedProgramName, manifest.description.name);
      assertEquals(IoUtilities.PROJECT_EXTENSION, manifest.metadata.fileType);
      assertEquals(Manifest.ProjectType.World, manifest.metadata.identifier.type);
      assertEquals(expectedSceneCameraType, manifest.projectStructure.sceneCameraType);
    }
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
    }
  }

  private static void assertWorldResourceManifestFacts(
      File archive,
      String expectedProgramName,
      ImageResource expectedResource) throws Exception {
    try (ZipFile zipFile = new ZipFile(archive)) {
      assertWorldManifestFacts(archive, expectedProgramName, Project.SceneCameraType.WindowCamera);
      assertNotNull("Generated .a3w archives should contain exported image data",
          zipFile.getEntry("resources/" + expectedResource.getName()));

      ProjectManifest manifest = readProjectManifest(zipFile);
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
      if (resourceReference instanceof TypeReference typeReference && expectedName.equals(typeReference.name)) {
        assertEquals(expectedFile, typeReference.file);
        assertEquals("tweedle", typeReference.format);
        return;
      }
    }
    throw new AssertionError("Missing type reference for " + expectedName);
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
          ProjectVersion.getCurrentVersion().toString().getBytes(StandardCharsets.UTF_8));
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
          ProjectVersion.getCurrentVersion().toString().getBytes(StandardCharsets.UTF_8));
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
          ProjectVersion.getCurrentVersion().toString().getBytes(StandardCharsets.UTF_8));
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
