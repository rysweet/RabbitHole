/*
 * Copyright (c) 2006-2011, Carnegie Mellon University. All rights reserved.
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
 */

package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.java.io.FileUtilities;
import org.lgna.story.implementation.alice.ModelResourceIoUtilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

/**
 * Package-private utility class holding file and JAR helper methods
 * extracted from {@link ModelResourceExporter}.
 */
class ModelResourceFileUtilities {

  private ModelResourceFileUtilities() {
  }

  private static String forwardSlash(String path) {
    return path.replace('\\', '/');
  }

  static void ensureOutputFile(File outputFile, String description) throws IOException {
    FileUtilities.createParentDirectoriesIfNecessary(outputFile);
    if (!outputFile.exists() && !outputFile.createNewFile()) {
      throw new IOException("Failed to create " + description + " file: " + outputFile);
    }
    if (!outputFile.isFile()) {
      throw new IOException(description + " path is not a file: " + outputFile);
    }
  }

  static void add(File source, JarOutputStream target, String destPathPrefix, boolean recursive) throws IOException {
    if (destPathPrefix == null) {
      destPathPrefix = "";
    }
    if (!destPathPrefix.isEmpty()) {
      destPathPrefix = forwardSlash(destPathPrefix);
      if (!destPathPrefix.endsWith("/")) {
        destPathPrefix += "/";
      }
      if (destPathPrefix.startsWith("/")) {
        destPathPrefix = destPathPrefix.substring(1);
      }
    }

    String root = forwardSlash(source.getAbsolutePath()) + "/";
    add(source, target, root, destPathPrefix, recursive);
  }

  static void add(File source, JarOutputStream target, String root, String destPathPrefix, boolean recursive) throws IOException {
    if (source.isDirectory()) {
      String name = forwardSlash(source.getPath());
      name = name.replace("//", "/");
      if (name.length() > 0) {
        if (!name.endsWith("/")) {
          name += "/";
        }
        name = name.substring(root.length());
        if (name.startsWith("/")) {
          name = name.substring(1);
        }
        if (name.length() > 0) {
          name = destPathPrefix + name;
          JarEntry entry = new JarEntry(name);
          entry.setTime(source.lastModified());
          try {
            System.out.println("   Adding: " + name);
            target.putNextEntry(entry);
            target.closeEntry();
          } catch (ZipException ze) {
            System.err.println(ze.getMessage());
          }
        }
      }
      File[] children = source.listFiles();
      if (children == null) {
        return;
      }
      for (File nestedFile : children) {
        if (!nestedFile.isDirectory() || recursive) {
          add(nestedFile, target, root, destPathPrefix, recursive);
        }
      }
      return;
    }

    String entryName = forwardSlash(source.getPath());
    entryName = entryName.substring(root.length());
    if (entryName.startsWith("/")) {
      entryName = entryName.substring(1);
    }
    entryName = destPathPrefix + entryName;
    JarEntry entry = new JarEntry(entryName);
    entry.setTime(source.lastModified());
    target.putNextEntry(entry);
    try (InputStream in = Files.newInputStream(source.toPath())) {
      in.transferTo(target);
    }
    target.closeEntry();
  }

  static File getJavaCodeDir(String root, String packageString) {
    String packageDirectory = JavaCodeUtilities.getDirectoryStringForPackage(packageString);
    return new File(root + packageDirectory);
  }

  static File getJavaClassFile(String root, String packageString, String javaClassName) {
    String filename = JavaCodeUtilities.getDirectoryStringForPackage(packageString) + javaClassName + ".class";
    return new File(root + filename);
  }

  static File getJavaFile(String root, String packageString, String javaClassName) {
    String filename = JavaCodeUtilities.getDirectoryStringForPackage(packageString) + javaClassName + ".java";
    return new File(root + filename);
  }

  static File getXMLFile(String root, String packageString, String className) {
    root = forwardSlash(root);
    if (!root.endsWith("/")) {
      root += "/";
    }
    String resourceDirectory = root + JavaCodeUtilities.getDirectoryStringForPackage(packageString) + ModelResourceIoUtilities.getResourceSubDirWithSeparator("");
    return new File(resourceDirectory, className + ".xml");
  }
}
