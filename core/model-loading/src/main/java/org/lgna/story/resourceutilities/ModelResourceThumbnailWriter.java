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
 *    this list of conditions and the disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */

package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.image.ImageUtilities;
import edu.cmu.cs.dennisc.java.io.FileUtilities;
import org.lgna.story.implementation.alice.AliceResourceUtilities;
import org.lgna.story.implementation.alice.ModelResourceIoUtilities;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final class ModelResourceThumbnailWriter {
  private ModelResourceThumbnailWriter() {
  }

  static String getThumbnailPath(String rootPath, String packageString, String className, String thumbnailName) {
    return getThumbnailDirectory(rootPath, packageString, className) + thumbnailName;
  }

  private static String getThumbnailDirectory(String rootPath, String packageString, String className) {
    if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
      rootPath += "/";
    }
    return rootPath + JavaCodeUtilities.getDirectoryStringForPackage(packageString) + ModelResourceIoUtilities.getResourceSubDirWithSeparator(className);
  }

  static BufferedImage createClassThumb(BufferedImage imgSrc) {
    return imgSrc;
  }

  static List<File> saveThumbnailsToDir(
      String root,
      String packageString,
      String className,
      String resourceName,
      Map<String, File> existingThumbnails,
      Map<ModelSubResourceExporter, Image> thumbnails,
      List<ModelSubResourceExporter> subResources) throws IOException {
    int expectedThumbnailCount = ((existingThumbnails != null) ? existingThumbnails.size() : 0) + thumbnails.size() + 1;
    List<File> thumbnailFiles = new ArrayList<>(expectedThumbnailCount);
    Set<String> thumbnailsCreated = HashSet.newHashSet(expectedThumbnailCount);
    String thumbnailDirectory = getThumbnailDirectory(root, packageString, className);
    if ((existingThumbnails != null) && !existingThumbnails.isEmpty()) {
      for (Entry<String, File> entry : existingThumbnails.entrySet()) {
        if (entry.getValue().exists()) {
          thumbnailFiles.add(entry.getValue());
          thumbnailsCreated.add(entry.getKey());
        } else {
          throw new FileNotFoundException("Missing thumbnail file '" + entry.getValue() + "'");
        }
      }
    }
    for (Entry<ModelSubResourceExporter, Image> entry : thumbnails.entrySet()) {
      String thumbnailName = AliceResourceUtilities.getThumbnailResourceFileName(entry.getKey().getModelName(), entry.getKey().getTextureName());
      if (thumbnailsCreated.add(thumbnailName)) {
        File f = saveImageToFile(thumbnailDirectory + thumbnailName, entry.getValue());
        thumbnailFiles.add(f);
      }
    }
    if (subResources.isEmpty()) {
      throw new IOException("Cannot create thumbnails for " + resourceName + " because no sub resources were registered");
    }
    ModelSubResourceExporter firstSubResource = subResources.getFirst();
    String firstThumbName = AliceResourceUtilities.getThumbnailResourceFileName(firstSubResource.getModelName(), firstSubResource.getTextureName());
    String classThumbName = AliceResourceUtilities.getThumbnailResourceFileName(className, null);
    File firstThumbFile = new File(thumbnailDirectory + firstThumbName);
    File classThumbFile = new File(thumbnailDirectory + classThumbName);

    try {
      BufferedImage classThumb = createClassThumb(ImageUtilities.read(firstThumbFile));
      if (classThumb == null) {
        throw new IOException("Thumbnail image is unreadable: " + firstThumbFile);
      }
      ImageUtilities.write(classThumbFile, classThumb);
      thumbnailFiles.add(classThumbFile);
    } catch (IOException ioe) {
      throw new IOException("Failed to create class thumbnail " + classThumbFile + " from " + firstThumbFile, ioe);
    } catch (RuntimeException e) {
      throw new IOException("Failed to create class thumbnail " + classThumbFile + " from " + firstThumbFile, e);
    }

    return thumbnailFiles;
  }

  private static File saveImageToFile(String fileName, Image image) throws IOException {
    if (image == null) {
      throw new IOException("Cannot write thumbnail " + fileName + " because the image is null");
    }
    int width;
    int height;
    try {
      width = image.getWidth(null);
      height = image.getHeight(null);
    } catch (RuntimeException e) {
      throw new IOException("Cannot read thumbnail dimensions for " + fileName, e);
    }
    if ((width <= 0) || (height <= 0)) {
      throw new IOException("Cannot write thumbnail " + fileName + " with invalid dimensions " + width + "x" + height);
    }
    File outputFile = new File(fileName);
    try {
      ensureOutputFile(outputFile, "thumbnail");
      ImageUtilities.write(outputFile, image);
      return outputFile;
    } catch (IOException e) {
      throw new IOException("Failed to write thumbnail " + outputFile, e);
    } catch (RuntimeException e) {
      throw new IOException("Failed to write thumbnail " + outputFile, e);
    }
  }

  private static void ensureOutputFile(File outputFile, String description) throws IOException {
    FileUtilities.createParentDirectoriesIfNecessary(outputFile);
    if (!outputFile.exists() && !outputFile.createNewFile()) {
      throw new IOException("Failed to create " + description + " file: " + outputFile);
    }
    if (!outputFile.isFile()) {
      throw new IOException(description + " path is not a file: " + outputFile);
    }
  }
}
