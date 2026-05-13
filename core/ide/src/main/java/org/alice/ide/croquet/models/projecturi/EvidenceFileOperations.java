package org.alice.ide.croquet.models.projecturi;

import edu.cmu.cs.dennisc.java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * File-system guards extracted from {@link SaveOperationCompletionEvidence}:
 * path traversal protection, symlink safety, file inspection, and the
 * {@link RegularFileState} record.
 *
 * <p>All methods are package-private statics with no I/O side-effects beyond
 * directory creation in {@link #canonicalDirectory} and
 * {@link #canonicalDirectoryUnderProofRoot}.
 */
final class EvidenceFileOperations {
  private EvidenceFileOperations() {
  }

  record RegularFileState(boolean exists, long sizeBytes) {
    static final RegularFileState MISSING = new RegularFileState(false, 0);

    boolean nonEmpty() {
      return exists && sizeBytes > 0;
    }
  }

  static RegularFileState regularFileState(Path path) {
    if (path == null) {
      return RegularFileState.MISSING;
    }
    try {
      BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
      return attributes.isRegularFile()
          ? new RegularFileState(true, attributes.size())
          : RegularFileState.MISSING;
    } catch (NoSuchFileException nsfe) {
      return RegularFileState.MISSING;
    } catch (IOException ioe) {
      Logger.throwable(ioe, "eatme Save operation completion evidence could not inspect file: " + redactedSavedPath(path));
      return RegularFileState.MISSING;
    }
  }

  static void requireNonEmptyRegularFile(Path artifact, String message) throws IOException {
    RegularFileState state = regularFileState(artifact);
    if (!state.exists() || !state.nonEmpty()) {
      throw new IOException(message + ": " + artifact);
    }
  }

  static Path artifactPath(Path evidenceDir, String artifactName) {
    Path artifact = evidenceDir.resolve(artifactName).normalize();
    if (!artifact.startsWith(evidenceDir.normalize())) {
      throw new IllegalArgumentException("Save operation artifact escapes evidence dir");
    }
    return artifact;
  }

  static Path canonicalDirectory(Path directory, String label) {
    try {
      return Files.createDirectories(directory).toRealPath();
    } catch (IOException ioe) {
      throw new IllegalArgumentException(label + " must be a writable canonical directory", ioe);
    }
  }

  static Path canonicalDirectoryUnderProofRoot(
      Path directory,
      Path proofRoot,
      String message) throws IOException {
    Path normalizedDirectory = directory.toAbsolutePath().normalize();
    Path normalizedProofRoot = proofRoot.toAbsolutePath().normalize();
    requirePathUnderProofRoot(normalizedDirectory, normalizedProofRoot, message);
    Path current = normalizedProofRoot;
    for (Path segment : normalizedProofRoot.relativize(normalizedDirectory)) {
      current = current.resolve(segment);
      ensureDirectoryWithoutFollowingSymlink(current, message);
    }
    Path canonicalDirectory = current.toRealPath();
    requirePathUnderProofRoot(canonicalDirectory, normalizedProofRoot, message);
    return canonicalDirectory;
  }

  static void requirePathUnderProofRoot(Path path, Path proofRoot, String message) {
    if (!path.normalize().startsWith(proofRoot)) {
      throw new IllegalArgumentException(message);
    }
  }

  static void ensureDirectoryWithoutFollowingSymlink(Path directory, String message)
      throws IOException {
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
        throw new IOException(message + ": " + directory);
      }
    } catch (NoSuchFileException nsfe) {
      Files.createDirectory(directory);
      BasicFileAttributes attributes =
          Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
        throw new IOException(message + ": " + directory);
      }
    }
  }

  static String redactedSavedPath(Path savedPath) {
    Path path = savedPath.normalize();
    if (!path.isAbsolute()) {
      return path.toString();
    }
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (path.startsWith(cwd)) {
      return cwd.relativize(path).toString();
    }
    Path fileName = path.getFileName();
    return "[redacted]/" + (fileName == null ? "" : fileName.toString());
  }

  static String redactedSavedFilePath(File savedFile) {
    return redactedSavedPath(savedFile.toPath());
  }

  static String proofRelativePath(Path path, Path proofRoot) {
    if (path == null) {
      return null;
    }
    if (path.normalize().startsWith(proofRoot)) {
      return proofRoot.relativize(path).toString().replace(File.separatorChar, '/');
    }
    return "[outside-proof-root]";
  }
}
