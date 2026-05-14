/*******************************************************************************
 * Copyright (c) 2018 Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.lgna.project.io;

import org.lgna.common.Resource;

/**
 * File name sanitization and archive entry name validation for JSON project I/O.
 * Centralizes path safety checks that prevent path traversal attacks in archive
 * entries. Both read and write paths use this class to enforce safe entry names.
 */
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

  static String diagnosticName(Resource resource) {
    if (resource == null) {
      return "<null>";
    }
    return entryFileName(resource) + " (" + resource.getId() + ")";
  }

  static boolean isResourceEntryName(String entryName) {
    if (!isSafeRelativeEntryName(entryName)) {
      return false;
    }
    int slash = entryName.indexOf('/');
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
    return isSafeRelativeEntryName(entryName) && entryName.startsWith("src/");
  }

  private static boolean isSafeRelativeEntryName(String entryName) {
    if ((entryName == null) || entryName.isEmpty() || isAbsolutePath(entryName) || (entryName.indexOf('\\') >= 0)) {
      return false;
    }

    int segmentStart = 0;
    while (segmentStart <= entryName.length()) {
      int segmentEnd = entryName.indexOf('/', segmentStart);
      if (segmentEnd < 0) {
        segmentEnd = entryName.length();
      }
      String segment = entryName.substring(segmentStart, segmentEnd);
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..") || hasWindowsDrivePrefix(segment)) {
        return false;
      }
      if (segmentEnd == entryName.length()) {
        return true;
      }
      segmentStart = segmentEnd + 1;
    }
    return false;
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
