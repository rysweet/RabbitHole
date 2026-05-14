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
