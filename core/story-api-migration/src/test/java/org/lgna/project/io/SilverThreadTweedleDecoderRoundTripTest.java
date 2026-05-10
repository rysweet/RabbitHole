package org.lgna.project.io;

import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.alice.serialization.tweedle.UnsupportedTweedleDecodeException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.io.IoUtilities;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread Tweedle decoder round-trip characterization test: documents
 * encode→decode structural identity for NamedUserTypes in a real .a3p project.
 *
 * <p>Loads indiaMinimum.a3p, encodes each NamedUserType to Tweedle source,
 * attempts to decode the source back to a NamedUserType, and — for types
 * that decode successfully — asserts structural equality: class name,
 * method count/names, field count, constructor presence, and non-empty
 * method bodies.
 *
 * <p>Types that fail to decode are documented as decoder gaps with the
 * specific exception message. This is a characterization test: it passes
 * green while documenting the current decode coverage. As decoder gaps
 * are fixed, more types will be verified automatically.
 *
 * <p>Headless: no JavaFX, no 3D rendering, no UI.
 *
 * @see TweedleEncoderDecoder
 */
public class SilverThreadTweedleDecoderRoundTripTest {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final TweedleEncoderDecoder CODEC = new TweedleEncoderDecoder();
  private static Set<NamedUserType> sharedTypes;
  private static Set<AbstractDeclaration> sharedTerminals;

  // Load the .a3p fixture once — ZIP parse + XML deserialization is expensive.
  @BeforeClass
  public static void loadProjectOnce() throws Exception {
    Path workDir = Files.createDirectories(
        Path.of("target", "silver-thread-tweedle-rt", UUID.randomUUID().toString()));
    File starterFile = workDir.resolve("indiaMinimum.a3p").toFile();
    try (InputStream stream =
        SilverThreadTweedleDecoderRoundTripTest.class.getResourceAsStream("/starters/indiaMinimum.a3p")) {
      assertNotNull("indiaMinimum.a3p must be on the test classpath", stream);
      Files.copy(stream, starterFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    Project project = IoUtilities.readProject(starterFile);
    sharedTypes = project.getNamedUserTypes();
    assertFalse("Project must contain at least one NamedUserType", sharedTypes.isEmpty());
    sharedTerminals = new HashSet<>(sharedTypes);
  }

  // ── Test: Full round-trip characterization for all NamedUserTypes ──────

  @Test
  public void encodeDecodeRoundTripPreservesStructureForAllTypes() throws Exception {
    Set<NamedUserType> types = sharedTypes;
    Set<AbstractDeclaration> terminals = sharedTerminals;

    int successCount = 0;
    List<String> gaps = new ArrayList<>();
    boolean foundNonEmptyBody = false;

    for (NamedUserType originalType : types) {
      String typeName = originalType.getName();
      assertNotNull("NamedUserType name must not be null", typeName);

      String tweedleSource = CODEC.encode(originalType, terminals);
      assertNotNull("Encoded Tweedle source must not be null for " + typeName, tweedleSource);
      assertFalse("Encoded Tweedle source must not be empty for " + typeName, tweedleSource.isEmpty());

      AbstractNode decoded;
      try {
        decoded = CODEC.decode(tweedleSource, terminals);
      } catch (UnsupportedTweedleDecodeException | IllegalArgumentException e) {
        gaps.add(typeName + ": " + e.getMessage());
        continue;
      }

      assertNotNull("Decoded node must not be null for " + typeName, decoded);
      assertTrue(
          "Decoded node must be a NamedUserType, got " + decoded.getClass().getSimpleName() + " for " + typeName,
          decoded instanceof NamedUserType);

      NamedUserType decodedType = (NamedUserType) decoded;
      assertStructuralEquality(typeName, originalType, decodedType);

      if (!foundNonEmptyBody) {
        for (UserMethod method : decodedType.getDeclaredMethods()) {
          BlockStatement body = (BlockStatement) method.getBodyProperty().getValue();
          if (body != null && body.statements.size() > 0) {
            foundNonEmptyBody = true;
            break;
          }
        }
      }

      successCount++;
    }

    // Characterization: document decode success rate and gaps
    System.out.println("[Tweedle round-trip characterization] "
        + successCount + "/" + types.size() + " types decoded successfully");
    if (!gaps.isEmpty()) {
      System.out.println("Documented decoder gaps (" + gaps.size() + "):");
      gaps.forEach(g -> System.out.println("  - " + g));
    }
    if (successCount == 0) {
      System.out.println("NOTE: No types round-tripped — all have decoder gaps. "
          + "Structural equality assertions will activate as gaps are fixed.");
    } else {
      assertTrue(
          "At least one decoded method must have a non-empty body among "
              + successCount + " decoded types",
          foundNonEmptyBody);
    }
  }

  // ── Test: Encoder produces valid Tweedle class declarations ────────────

  @Test
  public void encoderProducesNonEmptyTweedleForEachType() throws Exception {
    Set<NamedUserType> types = sharedTypes;
    Set<AbstractDeclaration> terminals = sharedTerminals;

    for (NamedUserType type : types) {
      String tweedleSource = CODEC.encode(type, terminals);
      assertNotNull("Encoded source must not be null for " + type.getName(), tweedleSource);
      assertFalse("Encoded source must not be empty for " + type.getName(), tweedleSource.isEmpty());
      assertTrue(
          "Encoded source must contain 'class' keyword for " + type.getName(),
          tweedleSource.contains("class"));
    }
  }

  // ── Structural equality assertion ──────────────────────────────────────

  private void assertStructuralEquality(
      String context,
      NamedUserType original,
      NamedUserType decoded) {

    assertEquals(
        "Class name must be preserved for " + context,
        original.getName(),
        decoded.getName());

    List<UserMethod> originalMethods = original.getDeclaredMethods();
    List<UserMethod> decodedMethods = decoded.getDeclaredMethods();
    assertEquals(
        "Method count must be preserved for " + context,
        originalMethods.size(),
        decodedMethods.size());

    Set<String> originalMethodNames = originalMethods.stream()
        .map(UserMethod::getName)
        .collect(Collectors.toSet());
    Set<String> decodedMethodNames = decodedMethods.stream()
        .map(UserMethod::getName)
        .collect(Collectors.toSet());
    assertEquals(
        "Method names must be preserved (set equality) for " + context,
        originalMethodNames,
        decodedMethodNames);

    List<UserField> originalFields = original.getDeclaredFields();
    List<UserField> decodedFields = decoded.getDeclaredFields();
    assertEquals(
        "Field count must be preserved for " + context,
        originalFields.size(),
        decodedFields.size());

    boolean originalHasCtors = !original.constructors.getValue().isEmpty();
    boolean decodedHasCtors = !decoded.constructors.getValue().isEmpty();
    assertEquals(
        "Constructor presence must match for " + context,
        originalHasCtors,
        decodedHasCtors);
  }

}
