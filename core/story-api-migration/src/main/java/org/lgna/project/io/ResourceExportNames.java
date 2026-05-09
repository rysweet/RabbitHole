package org.lgna.project.io;

import org.lgna.common.Resource;

final class ResourceExportNames {
  private ResourceExportNames() {
  }

  static String entryFileName(Resource resource) {
    String sanitizedFileName = sanitizeFileName(resource.getOriginalFileName());
    if (!sanitizedFileName.isEmpty()) {
      return sanitizedFileName;
    }
    sanitizedFileName = sanitizeFileName(resource.getName());
    return sanitizedFileName.isEmpty() ? resource.getId().toString() : sanitizedFileName;
  }

  static String metadataName(String name, String fallback) {
    if ((name == null) || name.trim().isEmpty()) {
      return fallback;
    }
    if (!isAbsolutePath(name.trim())) {
      return name;
    }
    String sanitizedName = sanitizeFileName(name);
    return sanitizedName.isEmpty() ? fallback : sanitizedName;
  }

  static String metadataOriginalFileName(String originalFileName, String fallback) {
    if (originalFileName == null) {
      return fallback;
    }
    if (originalFileName.trim().isEmpty()) {
      return "";
    }
    if (!isAbsolutePath(originalFileName.trim())) {
      return originalFileName;
    }
    String sanitizedName = sanitizeFileName(originalFileName);
    return sanitizedName.isEmpty() ? fallback : sanitizedName;
  }

  static String fileNameFromEntry(String entryName) {
    int slash = entryName.lastIndexOf('/');
    return (slash < 0) ? entryName : entryName.substring(slash + 1);
  }

  static boolean isResourceEntryName(String entryName) {
    int slash = (entryName == null) ? -1 : entryName.indexOf('/');
    if (slash <= 0) {
      return false;
    }
    String directory = entryName.substring(0, slash);
    if ("resources".equals(directory)) {
      return true;
    }
    if (!directory.startsWith("resources")) {
      return false;
    }
    for (int i = "resources".length(); i < directory.length(); i++) {
      if (!Character.isDigit(directory.charAt(i))) {
        return false;
      }
    }
    return directory.length() > "resources".length();
  }

  static boolean isSourceEntryName(String entryName) {
    return (entryName != null) && entryName.startsWith("src/");
  }

  private static String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "";
    }
    String sanitized = stripAbsolutePath(fileName.trim()).replace('/', '_').replace('\\', '_').trim();
    if (sanitized.equals(".") || sanitized.equals("..")) {
      return "";
    }
    return sanitized;
  }

  private static String stripAbsolutePath(String fileName) {
    if (!isAbsolutePath(fileName)) {
      return fileName;
    }
    int segmentStart = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1;
    if (hasWindowsDrivePrefix(fileName)) {
      segmentStart = Math.max(segmentStart, 2);
    }
    return fileName.substring(segmentStart);
  }

  private static boolean isAbsolutePath(String fileName) {
    return !fileName.isEmpty()
        && ((fileName.charAt(0) == '/')
        || (fileName.charAt(0) == '\\')
        || hasWindowsDrivePrefix(fileName));
  }

  private static boolean hasWindowsDrivePrefix(String fileName) {
    return (fileName.length() >= 2)
        && (fileName.charAt(1) == ':')
        && Character.isLetter(fileName.charAt(0));
  }
}
