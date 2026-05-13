package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * TDD tests for EvidenceFileOperations — the extracted file-system
 * guard methods from SaveOperationCompletionEvidence (issue #561).
 *
 * These tests define the contract that EvidenceFileOperations must satisfy.
 * They will FAIL until the extraction is implemented.
 */
public class EvidenceFileOperationsTest {

  // ── RegularFileState ──────────────────────────────────────────────

  @Test
  public void regularFileStateMissingSentinelIsNotExistsNotNonEmpty() {
    EvidenceFileOperations.RegularFileState missing =
        EvidenceFileOperations.RegularFileState.MISSING;
    assertFalse(missing.exists());
    assertEquals(0, missing.sizeBytes());
    assertFalse(missing.nonEmpty());
  }

  @Test
  public void regularFileStateExistsWithSizeIsNonEmpty() {
    EvidenceFileOperations.RegularFileState state =
        new EvidenceFileOperations.RegularFileState(true, 512);
    assertTrue(state.exists());
    assertEquals(512, state.sizeBytes());
    assertTrue(state.nonEmpty());
  }

  @Test
  public void regularFileStateExistsWithZeroSizeIsNotNonEmpty() {
    EvidenceFileOperations.RegularFileState state =
        new EvidenceFileOperations.RegularFileState(true, 0);
    assertTrue(state.exists());
    assertFalse(state.nonEmpty());
  }

  // ── regularFileState(Path) ────────────────────────────────────────

  @Test
  public void regularFileStateReturnsExistsForRegularFile() throws Exception {
    Path testDir = newTestDir();
    Path file = Files.writeString(testDir.resolve("test.a3p"), "content");

    EvidenceFileOperations.RegularFileState state =
        EvidenceFileOperations.regularFileState(file);

    assertTrue(state.exists());
    assertTrue(state.sizeBytes() > 0);
    assertTrue(state.nonEmpty());
  }

  @Test
  public void regularFileStateReturnsMissingForNonexistentPath() {
    EvidenceFileOperations.RegularFileState state =
        EvidenceFileOperations.regularFileState(
            Path.of("target/does-not-exist-" + UUID.randomUUID()));

    assertFalse(state.exists());
    assertEquals(0, state.sizeBytes());
  }

  @Test
  public void regularFileStateReturnsMissingForNullPath() {
    EvidenceFileOperations.RegularFileState state =
        EvidenceFileOperations.regularFileState(null);

    assertFalse(state.exists());
  }

  @Test
  public void regularFileStateReturnsMissingForDirectory() throws Exception {
    Path testDir = newTestDir();
    EvidenceFileOperations.RegularFileState state =
        EvidenceFileOperations.regularFileState(testDir);

    assertFalse(state.exists());
  }

  @Test
  public void regularFileStateReturnsExistsForEmptyFile() throws Exception {
    Path testDir = newTestDir();
    Path emptyFile = Files.createFile(testDir.resolve("empty.a3p"));

    EvidenceFileOperations.RegularFileState state =
        EvidenceFileOperations.regularFileState(emptyFile);

    assertTrue(state.exists());
    assertEquals(0, state.sizeBytes());
    assertFalse(state.nonEmpty());
  }

  // ── artifactPath ──────────────────────────────────────────────────

  @Test
  public void artifactPathResolvesUnderEvidenceDir() throws Exception {
    Path evidenceDir = newTestDir();
    Path artifact = EvidenceFileOperations.artifactPath(
        evidenceDir, "test-artifact.json");

    assertEquals(evidenceDir.resolve("test-artifact.json").normalize(), artifact);
  }

  @Test
  public void artifactPathRejectsTraversalAttempt() throws Exception {
    Path evidenceDir = newTestDir();

    assertThrows(IllegalArgumentException.class,
        () -> EvidenceFileOperations.artifactPath(
            evidenceDir, "../escape.json"));
  }

  @Test
  public void artifactPathRejectsAbsoluteArtifactNames() throws Exception {
    Path evidenceDir = newTestDir();
    // An artifact name starting with / would escape the evidence dir
    // after resolution+normalization
    assertThrows(IllegalArgumentException.class,
        () -> EvidenceFileOperations.artifactPath(
            evidenceDir, "../../etc/passwd"));
  }

  // ── requireNonEmptyRegularFile ────────────────────────────────────

  @Test
  public void requireNonEmptyRegularFileSucceedsForNonEmptyFile() throws Exception {
    Path testDir = newTestDir();
    Path file = Files.writeString(testDir.resolve("evidence.json"), "{\"ok\":true}");

    // Should not throw
    EvidenceFileOperations.requireNonEmptyRegularFile(file, "test message");
  }

  @Test
  public void requireNonEmptyRegularFileThrowsForEmptyFile() throws Exception {
    Path testDir = newTestDir();
    Path emptyFile = Files.createFile(testDir.resolve("empty.json"));

    IOException thrown = assertThrows(IOException.class,
        () -> EvidenceFileOperations.requireNonEmptyRegularFile(
            emptyFile, "Must not be empty"));

    assertTrue(thrown.getMessage(), thrown.getMessage().contains("Must not be empty"));
  }

  @Test
  public void requireNonEmptyRegularFileThrowsForMissingFile() throws Exception {
    Path testDir = newTestDir();
    Path missing = testDir.resolve("does-not-exist.json");

    IOException thrown = assertThrows(IOException.class,
        () -> EvidenceFileOperations.requireNonEmptyRegularFile(
            missing, "Must exist"));

    assertTrue(thrown.getMessage(), thrown.getMessage().contains("Must exist"));
  }

  // ── canonicalDirectory ────────────────────────────────────────────

  @Test
  public void canonicalDirectoryCreatesAndResolvesPath() throws Exception {
    Path testDir = newTestDir();
    Path subDir = testDir.resolve("nested/canonical");

    Path result = EvidenceFileOperations.canonicalDirectory(subDir, "test");

    assertTrue(Files.isDirectory(result));
    // Result should be a real (canonical) path
    assertEquals(result, result.toRealPath());
  }

  // ── requirePathUnderProofRoot ─────────────────────────────────────

  @Test
  public void requirePathUnderProofRootSucceedsForChildPath() throws Exception {
    Path proofRoot = newTestDir();
    Path child = proofRoot.resolve("nested/artifact.json");

    // Should not throw
    EvidenceFileOperations.requirePathUnderProofRoot(
        child, proofRoot, "must be under root");
  }

  @Test
  public void requirePathUnderProofRootThrowsForEscapedPath() throws Exception {
    Path proofRoot = newTestDir();
    Path outside = proofRoot.resolve("../../outside");

    assertThrows(IllegalArgumentException.class,
        () -> EvidenceFileOperations.requirePathUnderProofRoot(
            outside, proofRoot, "must be under root"));
  }

  // ── canonicalDirectoryUnderProofRoot ──────────────────────────────

  @Test
  public void canonicalDirectoryUnderProofRootCreatesNestedDir() throws Exception {
    Path proofRoot = Files.createDirectories(newTestDir().resolve("proof-root")).toRealPath();
    Path nested = proofRoot.resolve("sub/dir");

    Path result = EvidenceFileOperations.canonicalDirectoryUnderProofRoot(
        nested, proofRoot, "test message");

    assertTrue(Files.isDirectory(result));
    assertTrue(result.toString(), result.startsWith(proofRoot));
  }

  @Test
  public void canonicalDirectoryUnderProofRootRejectsEscapedPath() throws Exception {
    Path proofRoot = Files.createDirectories(newTestDir().resolve("proof-root")).toRealPath();
    Path escaped = proofRoot.resolve("../../outside-root");

    assertThrows(IllegalArgumentException.class,
        () -> EvidenceFileOperations.canonicalDirectoryUnderProofRoot(
            escaped, proofRoot, "must stay under root"));
  }

  @Test
  public void canonicalDirectoryUnderProofRootRejectsSymlinkParent() throws Exception {
    Path proofRoot = Files.createDirectories(newTestDir().resolve("proof-root")).toRealPath();
    Path outsideDir = Files.createDirectories(newTestDir().resolve("outside"));
    Path symlinkInRoot = proofRoot.resolve("symlinked-dir");
    try {
      Files.createSymbolicLink(symlinkInRoot, outsideDir);
    } catch (IOException | SecurityException | UnsupportedOperationException e) {
      // Symlinks not supported on this platform/config
      return;
    }

    assertThrows(IOException.class,
        () -> EvidenceFileOperations.canonicalDirectoryUnderProofRoot(
            symlinkInRoot.resolve("nested"), proofRoot, "no symlinks"));
  }

  // ── ensureDirectoryWithoutFollowingSymlink ────────────────────────

  @Test
  public void ensureDirectoryWithoutFollowingSymlinkCreatesRealDir() throws Exception {
    Path testDir = newTestDir();
    Path newDir = testDir.resolve("new-subdir");

    EvidenceFileOperations.ensureDirectoryWithoutFollowingSymlink(
        newDir, "test dir");

    assertTrue(Files.isDirectory(newDir));
    assertFalse(Files.isSymbolicLink(newDir));
  }

  @Test
  public void ensureDirectoryWithoutFollowingSymlinkRejectsSymlink() throws Exception {
    Path testDir = newTestDir();
    Path realDir = Files.createDirectory(testDir.resolve("real"));
    Path symlinkDir = testDir.resolve("symlinked");
    try {
      Files.createSymbolicLink(symlinkDir, realDir);
    } catch (IOException | SecurityException | UnsupportedOperationException e) {
      return;
    }

    assertThrows(IOException.class,
        () -> EvidenceFileOperations.ensureDirectoryWithoutFollowingSymlink(
            symlinkDir, "no symlinks"));
  }

  @Test
  public void ensureDirectoryWithoutFollowingSymlinkAcceptsExistingRealDir() throws Exception {
    Path testDir = newTestDir();
    Path existingDir = Files.createDirectory(testDir.resolve("existing"));

    // Should not throw for an existing, non-symlink directory
    EvidenceFileOperations.ensureDirectoryWithoutFollowingSymlink(
        existingDir, "existing dir");

    assertTrue(Files.isDirectory(existingDir));
  }

  // ── redactedSavedPath ─────────────────────────────────────────────

  @Test
  public void redactedSavedPathReturnsRelativePathUnchanged() {
    String result = EvidenceFileOperations.redactedSavedPath(
        Path.of("some/relative/path.a3p"));
    assertEquals("some/relative/path.a3p", result);
  }

  @Test
  public void redactedSavedPathRelativizesPathUnderCwd() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path underCwd = cwd.resolve("target/test.a3p");
    String result = EvidenceFileOperations.redactedSavedPath(underCwd);
    // Should be relative to cwd
    assertEquals("target/test.a3p", result);
  }

  @Test
  public void redactedSavedPathRedactsAbsolutePathOutsideCwd() throws Exception {
    Path external = Files.createTempFile("alice-redact-", ".a3p");
    try {
      String result = EvidenceFileOperations.redactedSavedPath(external);
      assertTrue(result, result.startsWith("[redacted]/"));
      assertTrue(result, result.contains(external.getFileName().toString()));
    } finally {
      Files.deleteIfExists(external);
    }
  }

  // ── redactedSavedFilePath ─────────────────────────────────────────

  @Test
  public void redactedSavedFilePathDelegatesToRedactedSavedPath() throws Exception {
    Path external = Files.createTempFile("alice-redact-file-", ".a3p");
    try {
      String result = EvidenceFileOperations.redactedSavedFilePath(
          external.toFile());
      assertTrue(result, result.contains(external.getFileName().toString()));
    } finally {
      Files.deleteIfExists(external);
    }
  }

  // ── Integration: round-trip with regularFileState ─────────────────

  @Test
  public void regularFileStateRoundTripsFileLifecycle() throws Exception {
    Path testDir = newTestDir();
    Path file = testDir.resolve("lifecycle.a3p");

    // File doesn't exist yet
    EvidenceFileOperations.RegularFileState before =
        EvidenceFileOperations.regularFileState(file);
    assertFalse(before.exists());

    // Create file
    Files.writeString(file, "content");
    EvidenceFileOperations.RegularFileState after =
        EvidenceFileOperations.regularFileState(file);
    assertTrue(after.exists());
    assertTrue(after.nonEmpty());
    assertEquals(7, after.sizeBytes());  // "content" = 7 bytes
  }

  // ── helper ────────────────────────────────────────────────────────

  private static Path newTestDir() throws Exception {
    return Files.createDirectories(Path.of(
        "target",
        "evidence-file-operations-test",
        UUID.randomUUID().toString()));
  }
}
