package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for JSON writing operations.
 */
public class EvidenceJsonWriterExtendedTest {

  @Test
  public void stringWriter_canWriteJson() {
    StringWriter sw = new StringWriter();
    sw.write("{\"key\":\"value\"}");
    assertEquals("{\"key\":\"value\"}", sw.toString());
  }

  @Test
  public void stringWriter_multipleWrites() {
    StringWriter sw = new StringWriter();
    sw.write("{");
    sw.write("\"a\":1");
    sw.write(",");
    sw.write("\"b\":2");
    sw.write("}");
    assertEquals("{\"a\":1,\"b\":2}", sw.toString());
  }

  @Test
  public void jsonLikeContent_canBeWrittenToFile() throws IOException {
    Path temp = Files.createTempFile("alice_json_", ".json");
    try {
      String json = "{\"type\":\"evidence\",\"timestamp\":\"2024-01-01\",\"data\":[1,2,3]}";
      Files.writeString(temp, json);
      String readBack = Files.readString(temp);
      assertEquals(json, readBack);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void emptyJsonObject_roundTrip() throws IOException {
    Path temp = Files.createTempFile("alice_json_", ".json");
    try {
      Files.writeString(temp, "{}");
      assertEquals("{}", Files.readString(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void emptyJsonArray_roundTrip() throws IOException {
    Path temp = Files.createTempFile("alice_json_", ".json");
    try {
      Files.writeString(temp, "[]");
      assertEquals("[]", Files.readString(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void nestedJson_roundTrip() throws IOException {
    Path temp = Files.createTempFile("alice_json_", ".json");
    try {
      String json = "{\"outer\":{\"inner\":{\"deep\":\"value\"}}}";
      Files.writeString(temp, json);
      assertEquals(json, Files.readString(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void jsonWithUnicode_roundTrip() throws IOException {
    Path temp = Files.createTempFile("alice_json_", ".json");
    try {
      String json = "{\"name\":\"日本語テスト\"}";
      Files.writeString(temp, json);
      assertEquals(json, Files.readString(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void stringWriter_bufferGrows() {
    StringWriter sw = new StringWriter();
    StringBuilder expected = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      String part = "\"key" + i + "\":" + i;
      sw.write(part);
      expected.append(part);
    }
    assertEquals(expected.toString(), sw.toString());
  }
}
