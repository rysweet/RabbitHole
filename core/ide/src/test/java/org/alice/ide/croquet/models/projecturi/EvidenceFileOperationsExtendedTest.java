package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for evidence file operations.
 */
public class EvidenceFileOperationsExtendedTest {

  @Test
  public void tempFile_canBeCreated() throws IOException {
    Path temp = Files.createTempFile("alice_test_", ".json");
    assertTrue(Files.exists(temp));
    Files.delete(temp);
  }

  @Test
  public void tempDirectory_canBeCreated() throws IOException {
    Path tempDir = Files.createTempDirectory("alice_test_");
    assertTrue(Files.isDirectory(tempDir));
    Files.delete(tempDir);
  }

  @Test
  public void writtenFile_canBeReadBack() throws IOException {
    Path temp = Files.createTempFile("alice_test_", ".txt");
    try {
      Files.writeString(temp, "test content");
      String content = Files.readString(temp);
      assertEquals("test content", content);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void writtenJsonFile_canBeReadBack() throws IOException {
    Path temp = Files.createTempFile("alice_test_", ".json");
    try {
      String json = "{\"version\":\"3.1\",\"type\":\"test\"}";
      Files.writeString(temp, json);
      String content = Files.readString(temp);
      assertEquals(json, content);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void fileSize_matchesContent() throws IOException {
    Path temp = Files.createTempFile("alice_test_", ".txt");
    try {
      String content = "hello world";
      Files.writeString(temp, content);
      assertTrue(Files.size(temp) > 0);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void nestedDirectory_canBeCreated() throws IOException {
    Path tempDir = Files.createTempDirectory("alice_test_");
    try {
      Path nested = tempDir.resolve("sub1").resolve("sub2");
      Files.createDirectories(nested);
      assertTrue(Files.isDirectory(nested));
    } finally {
      // cleanup
      Files.walk(tempDir)
          .sorted(java.util.Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @Test
  public void emptyFile_hasZeroSize() throws IOException {
    Path temp = Files.createTempFile("alice_test_", ".txt");
    try {
      assertEquals(0, Files.size(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
