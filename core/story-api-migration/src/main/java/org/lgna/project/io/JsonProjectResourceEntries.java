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

import edu.cmu.cs.dennisc.java.util.zip.ByteArrayDataSource;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import org.alice.tweedle.file.AudioReference;
import org.alice.tweedle.file.ImageReference;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ResourceReference;
import org.lgna.common.Resource;
import org.lgna.common.resources.AudioResource;
import org.lgna.common.resources.ImageResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates resource archive entries with collision-avoidance naming.
 * Used by {@link JsonResourceEntryWriter} to add resource data sources
 * and manifest references when writing JSON project archives.
 */
final class JsonProjectResourceEntries {
  private JsonProjectResourceEntries() {
  }

  static void addResources(Manifest manifest, List<DataSource> dataSources, Set<Resource> resources) {
    Set<String> usedEntryNames = new HashSet<>(resources.size() * 2);
    Map<String, Integer> nextDirectorySuffixByFileName = new HashMap<>(resources.size() * 2);
    for (Resource resource : resources) {
      String entryName = generateEntryName(resource, usedEntryNames, nextDirectorySuffixByFileName);
      usedEntryNames.add(entryName);
      addResourceReference(manifest, resource, entryName);
      // TODO Expand to cover arbitrary data files
      dataSources.add(new ByteArrayDataSource(entryName, resource.getData()));
    }
  }

  private static void addResourceReference(Manifest manifest, Resource resource, String entryName) {
    final ResourceReference resourceReference = resourceReference(resource);
    resourceReference.name = ResourceExportNames.metadataName(
        resourceReference.name,
        ResourceExportNames.fileNameFromEntry(entryName));
    resourceReference.file = entryName;
    manifest.resources.add(resourceReference);
  }

  private static ResourceReference resourceReference(Resource resource) {
    if (resource instanceof AudioResource audioResource) {
      return new AudioReference(audioResource);
    }
    if (resource instanceof ImageResource imageResource) {
      return new ImageReference(imageResource);
    }
    throw new RuntimeException("Resource of unexpected type " + resource);
  }

  private static String generateEntryName(
      Resource resource,
      Set<String> usedEntryNames,
      Map<String, Integer> nextDirectorySuffixByFileName) {
    String fileName = ResourceExportNames.entryFileName(resource);
    int i = nextDirectorySuffixByFileName.getOrDefault(fileName, 1);
    String entryName = potentialEntryName(fileName, i);
    while (usedEntryNames.contains(entryName)) {
      i++;
      entryName = potentialEntryName(fileName, i);
    }
    nextDirectorySuffixByFileName.put(fileName, i + 1);
    return entryName;
  }

  private static String potentialEntryName(String validFilename, int i) {
    return "resources" + ((i == 1) ? "" : String.valueOf(i)) + "/" + validFilename;
  }
}
