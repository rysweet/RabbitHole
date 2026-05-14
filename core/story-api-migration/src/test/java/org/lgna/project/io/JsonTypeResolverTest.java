package org.lgna.project.io;

import org.alice.serialization.tweedle.UnsupportedTweedleDecodeException;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ProjectManifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeReference;
import org.junit.Test;
import org.lgna.project.ast.NamedUserType;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonTypeResolver}, focusing on the inner
 * {@link JsonTypeResolver.TypeReadResult} data structure and the
 * static verification helpers. The full type reading pipeline is
 * integration-tested via IoUtilitiesTest; these tests verify the
 * helper contracts in isolation.
 */
public class JsonTypeResolverTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // TypeReadResult: add / findByName
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void emptyResultHasNoTypesAndNoUnsupported() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    assertTrue(result.types.isEmpty());
    assertFalse(result.hasTypeReferences);
    assertFalse(result.hasUnsupportedTweedleTypes());
    assertNull(result.findByName("anything"));
  }

  @Test
  public void addedTypeIsFoundByName() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    NamedUserType type = namedUserType("Scene");
    result.add(type);
    assertSame(type, result.findByName("Scene"));
    assertEquals(1, result.types.size());
  }

  @Test
  public void findByNameReturnsNullForUnknownName() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.add(namedUserType("Scene"));
    assertNull(result.findByName("NotScene"));
  }

  @Test
  public void findByNameReturnsNullForNullName() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.add(namedUserType("Scene"));
    assertNull(result.findByName(null));
  }

  @Test
  public void duplicateAddKeepsFirstTypeByName() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    NamedUserType first = namedUserType("Scene");
    NamedUserType second = namedUserType("Scene");
    result.add(first);
    result.add(second);
    assertSame(first, result.findByName("Scene"));
    assertEquals(2, result.types.size());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TypeReadResult: unsupported types
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void addUnsupportedTweedleTypeTracksTypeName() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    TypeReference ref = new TypeReference("BadType", "src/BadType.twe", "tweedle");
    result.addUnsupportedTweedleType(ref, new UnsupportedTweedleDecodeException("bad superclass"));
    assertTrue(result.hasUnsupportedTweedleTypes());
    assertTrue(result.hasUnsupportedTweedleDecodeFor("BadType"));
    assertFalse(result.hasUnsupportedTweedleDecodeFor("OtherType"));
    assertFalse(result.hasUnsupportedTweedleDecodeFor(null));
  }

  @Test
  public void unsupportedTweedleDecodeCauseForReturnsException() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    TypeReference ref = new TypeReference("BadType", "src/BadType.twe", "tweedle");
    UnsupportedTweedleDecodeException cause = new UnsupportedTweedleDecodeException("missing super");
    result.addUnsupportedTweedleType(ref, cause);
    assertSame(cause, result.unsupportedTweedleDecodeCauseFor("BadType"));
    assertNull(result.unsupportedTweedleDecodeCauseFor("Other"));
    assertNull(result.unsupportedTweedleDecodeCauseFor(null));
  }

  @Test
  public void unsupportedTweedleTypeNamesFormatsAlphabetically() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.addUnsupportedTweedleType(
        new TypeReference("Zebra", "src/Zebra.twe", "tweedle"),
        new UnsupportedTweedleDecodeException("z"));
    result.addUnsupportedTweedleType(
        new TypeReference("Alpha", "src/Alpha.twe", "tweedle"),
        new UnsupportedTweedleDecodeException("a"));
    assertEquals("[Alpha, Zebra]", result.unsupportedTweedleTypeNames());
  }

  @Test
  public void unsupportedTweedleDecodeReasonsFormatsAlphabeticallyWithReasons() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.addUnsupportedTweedleType(
        new TypeReference("Zebra", "src/Zebra.twe", "tweedle"),
        new UnsupportedTweedleDecodeException("zebra reason"));
    result.addUnsupportedTweedleType(
        new TypeReference("Alpha", "src/Alpha.twe", "tweedle"),
        new UnsupportedTweedleDecodeException("alpha reason"));
    String reasons = result.unsupportedTweedleDecodeReasons();
    assertTrue(reasons.startsWith("[Alpha: alpha reason"));
    assertTrue(reasons.contains("Zebra: zebra reason"));
    assertTrue(reasons.endsWith("]"));
  }

  @Test
  public void unsupportedTweedleDecodeReasonTruncatesLongMessages() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    String longMessage = "x".repeat(600);
    result.addUnsupportedTweedleType(
        new TypeReference("Long", "src/Long.twe", "tweedle"),
        new UnsupportedTweedleDecodeException(longMessage));
    String reasons = result.unsupportedTweedleDecodeReasons();
    assertTrue(reasons.contains("..."));
    // The truncated reason should be at most MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH (512) chars
    String reason = reasons.substring(reasons.indexOf(": ") + 2, reasons.length() - 1);
    assertTrue(reason.length() <= 512);
  }

  @Test
  public void unsupportedTweedleDecodeReasonUsesClassNameForNullMessage() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.addUnsupportedTweedleType(
        new TypeReference("NoMsg", "src/NoMsg.twe", "tweedle"),
        new UnsupportedTweedleDecodeException(null));
    String reasons = result.unsupportedTweedleDecodeReasons();
    assertTrue(reasons.contains("UnsupportedTweedleDecodeException"));
  }

  @Test
  public void unsupportedTweedleTypeNameFallsBackToFileWhenNameIsNull() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    TypeReference ref = new TypeReference(null, "src/Unnamed.twe", "tweedle");
    result.addUnsupportedTweedleType(ref, new UnsupportedTweedleDecodeException("test"));
    assertTrue(result.hasUnsupportedTweedleDecodeFor("src/Unnamed.twe"));
  }

  @Test
  public void unsupportedTweedleTypeNameFallsBackToUnnamedWhenBothNullAndEmpty() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    TypeReference ref = new TypeReference("", null, "tweedle");
    result.addUnsupportedTweedleType(ref, new UnsupportedTweedleDecodeException("test"));
    assertTrue(result.hasUnsupportedTweedleDecodeFor("<unnamed>"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // fallbackTypeForUnnamedManifest
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void fallbackTypeReturnsNullForNamedManifest() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "Named";
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.add(namedUserType("Named"));
    assertNull(JsonTypeResolver.fallbackTypeForUnnamedManifest(manifest, result));
  }

  @Test
  public void fallbackTypeReturnsFirstTypeForUnnamedManifest() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = null;
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    NamedUserType type = namedUserType("OnlyType");
    result.add(type);
    assertSame(type, JsonTypeResolver.fallbackTypeForUnnamedManifest(manifest, result));
  }

  @Test
  public void fallbackTypeReturnsNullForUnnamedManifestWithNoTypes() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = null;
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    assertNull(JsonTypeResolver.fallbackTypeForUnnamedManifest(manifest, result));
  }

  @Test
  public void fallbackTypeReturnsFirstTypeForNullManifest() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    NamedUserType type = namedUserType("Type");
    result.add(type);
    // null manifest has no name → treated as unnamed → returns first type
    assertSame(type, JsonTypeResolver.fallbackTypeForUnnamedManifest(null, result));
  }

  @Test
  public void fallbackTypeReturnsNullForNullManifestWithNoTypes() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    assertNull(JsonTypeResolver.fallbackTypeForUnnamedManifest(null, result));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // verifyProjectArchiveHasExpectedProgramType
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void verifyProjectArchiveSkipsCheckForUnnamedManifest() throws IOException {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = null;
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    // Should not throw
    JsonTypeResolver.verifyProjectArchiveHasExpectedProgramType(manifest, result);
  }

  @Test
  public void verifyProjectArchiveThrowsForNamedManifestWithNoMatchingType() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "ExpectedProgram";
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.hasTypeReferences = true;
    result.add(namedUserType("OtherType"));

    IOException thrown = assertThrows(IOException.class,
        () -> JsonTypeResolver.verifyProjectArchiveHasExpectedProgramType(manifest, result));

    assertTrue(thrown.getMessage().contains("ExpectedProgram"));
    assertTrue(thrown.getMessage().contains("OtherType"));
  }

  @Test
  public void verifyProjectArchiveThrowsForNamedManifestWithNoTypeReferences() {
    ProjectManifest manifest = new ProjectManifest();
    manifest.description.name = "MissingProgram";
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();

    IOException thrown = assertThrows(IOException.class,
        () -> JsonTypeResolver.verifyProjectArchiveHasExpectedProgramType(manifest, result));

    assertTrue(thrown.getMessage().contains("MissingProgram"));
    assertTrue(thrown.getMessage().contains("type reference"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // verifyTypeArchiveHasExpectedType
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void verifyTypeArchiveThrowsWithDecodedTypeNamesForMismatch() {
    Manifest manifest = new ProjectManifest();
    manifest.description.name = "ExpectedType";
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.hasTypeReferences = true;
    result.add(namedUserType("ActualType"));

    IOException thrown = assertThrows(IOException.class,
        () -> JsonTypeResolver.verifyTypeArchiveHasExpectedType(manifest, result));

    assertTrue(thrown.getMessage().contains("Type archive"));
    assertTrue(thrown.getMessage().contains("ExpectedType"));
    assertTrue(thrown.getMessage().contains("ActualType"));
  }

  @Test
  public void verifyTypeArchiveThrowsForMissingTypeReference() {
    Manifest manifest = new ProjectManifest();
    manifest.description.name = "MissingType";
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();

    IOException thrown = assertThrows(IOException.class,
        () -> JsonTypeResolver.verifyTypeArchiveHasExpectedType(manifest, result));

    assertTrue(thrown.getMessage().contains("Type archive"));
    assertTrue(thrown.getMessage().contains("MissingType"));
    assertTrue(thrown.getMessage().contains("type reference"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // verifyArchiveHasNoUnsupportedManifestTypes
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void verifyNoUnsupportedPassesForCleanResult() throws IOException {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.add(namedUserType("GoodType"));
    // Should not throw
    JsonTypeResolver.verifyArchiveHasNoUnsupportedManifestTypes("Test archive", result);
  }

  @Test
  public void verifyNoUnsupportedThrowsForUnsupportedTypes() {
    JsonTypeResolver.TypeReadResult result = new JsonTypeResolver.TypeReadResult();
    result.addUnsupportedTweedleType(
        new TypeReference("BadType", "src/BadType.twe", "tweedle"),
        new UnsupportedTweedleDecodeException("missing superclass"));

    IOException thrown = assertThrows(IOException.class,
        () -> JsonTypeResolver.verifyArchiveHasNoUnsupportedManifestTypes("Test archive", result));

    assertTrue(thrown.getMessage().contains("Test archive"));
    assertTrue(thrown.getMessage().contains("BadType"));
    assertTrue(thrown.getMessage().contains("missing superclass"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private static NamedUserType namedUserType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    return type;
  }
}
