package org.lgna.project.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ZipEntryContainer {
  private final ZipFile zipFile;

  ZipEntryContainer(ZipFile file) {
    zipFile = file;
  }

  public InputStream getInputStream(String name) throws IOException {
    validateSafeEntryName(name);
    ZipEntry zipEntry = zipFile.getEntry(name);
    return zipEntry == null ? null : zipFile.getInputStream(zipEntry);
  }

  static void validateSafeEntryName(String name) throws IOException {
    if ((name == null) || name.isEmpty()) {
      throw new IOException("Unsafe archive entry " + name);
    }
    if ((name.charAt(0) == '/') || (name.charAt(0) == '\\') || hasWindowsDrivePrefix(name)) {
      throw new IOException("Unsafe archive entry " + name);
    }
    int segmentStart = 0;
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if ((ch == '\0') || Character.isISOControl(ch) || (ch == '\\')) {
        throw new IOException("Unsafe archive entry " + name);
      }
      if (ch == '/') {
        validateSafeSegment(name, segmentStart, i);
        segmentStart = i + 1;
      }
    }
    validateSafeSegment(name, segmentStart, name.length());
  }

  private static void validateSafeSegment(String name, int start, int end) throws IOException {
    int length = end - start;
    if ((length == 0)
        || ((length == 1) && (name.charAt(start) == '.'))
        || ((length == 2) && (name.charAt(start) == '.') && (name.charAt(start + 1) == '.'))) {
      throw new IOException("Unsafe archive entry " + name);
    }
  }

  private static boolean hasWindowsDrivePrefix(String name) {
    return (name.length() >= 2)
        && (name.charAt(1) == ':')
        && Character.isLetter(name.charAt(0));
  }
}
