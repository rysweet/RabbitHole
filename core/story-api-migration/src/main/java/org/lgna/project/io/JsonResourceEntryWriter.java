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
import edu.cmu.cs.dennisc.pattern.IsInstanceCrawler;
import edu.cmu.cs.dennisc.print.PrintUtilities;
import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.alice.tweedle.file.Manifest;
import org.alice.tweedle.file.ResourceReference;
import org.alice.tweedle.file.TypeManifest;
import org.alice.tweedle.file.TypeReference;
import org.lgna.common.Resource;
import org.lgna.project.ProjectVersion;
import org.lgna.project.ast.*;
import org.lgna.story.resources.DynamicResource;
import org.lgna.story.resources.JointedModelResource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates type and resource data source entries for writing JSON project archives.
 * Used by {@link JsonProjectIo.JsonProjectWriter}.
 */
final class JsonResourceEntryWriter {
  private static final String TWEEDLE_EXTENSION = "twe";
  private static final String TWEEDLE_FORMAT = "tweedle";

  private final TweedleEncoderDecoder coder;

  JsonResourceEntryWriter(TweedleEncoderDecoder coder) {
    this.coder = coder;
  }

  static TypeManifest createTypeManifest(AbstractType<?, ?, ?> type) {
    final TypeManifest manifest = new TypeManifest();
    manifest.description.name = type.getName();
    manifest.provenance.aliceVersion = ProjectVersion.getCurrentVersion().toString();
    manifest.metadata.fileType = IoUtilities.TYPE_EXTENSION;
    manifest.metadata.identifier.name = type.getId().toString();
    manifest.metadata.identifier.type = Manifest.ProjectType.Library;
    return manifest;
  }

  Collection<? extends DataSource> createEntriesForTypes(
      Manifest manifest,
      Set<NamedUserType> userTypes,
      Set<String> manifestResourceNames) {
    return userTypes.stream()
        .sorted(Comparator.comparingInt(AbstractType::hierarchyDepth))
        .map(ut -> dataSourceForType(manifest, ut, manifestResourceNames))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  DataSource dataSourceForType(Manifest manifest, NamedUserType ut) {
    return dataSourceForType(manifest, ut, manifestResourceNames(manifest));
  }

  DataSource dataSourceForType(Manifest manifest, NamedUserType ut, Set<String> manifestResourceNames) {
    // Special case to catch older models from starter worlds
    String likelyTypeName = ut.getName();
    String typeName = "SandDunes".equals(likelyTypeName) ? "Terrain" : likelyTypeName;

    if (manifestResourceNames.add(typeName)) {
      final String fileName = "src/" + typeName + '.' + TWEEDLE_EXTENSION;
      TypeReference typeReference = new TypeReference(typeName, fileName, TWEEDLE_FORMAT);
      typeReference.dependencies = dependencyNames(ut);
      manifest.resources.add(typeReference);
      return new ByteArrayDataSource(fileName, serializedClass(ut));
    }
    return null;
  }

  /**
   * Collect the names of the other user-authored types that {@code type}
   * references. Uses a bounded, non-tunneling crawl so the result is the type's
   * direct dependency set (supertype, field types, parameter/return types, and
   * types named in its bodies) without recursing into those types' own members.
   * Returns {@code null} when there are no dependencies so the manifest omits the
   * field entirely. Package-private for direct unit testing.
   */
  static List<String> dependencyNames(NamedUserType type) {
    IsInstanceCrawler<NamedUserType> crawler = IsInstanceCrawler.createInstance(NamedUserType.class);
    type.crawl(crawler, CrawlPolicy.INCLUDE_REFERENCES_BUT_DO_NOT_TUNNEL);
    SortedSet<String> names = new TreeSet<>();
    for (NamedUserType referenced : crawler.getList()) {
      if (referenced != type) {
        names.add(referenced.getName());
      }
    }
    return names.isEmpty() ? null : new ArrayList<>(names);
  }

  Collection<? extends DataSource> createEntriesForResourceTypes(Manifest manifest, Set<JointedModelResource> resources) {
    Set<Class<?>> distinctResources = new HashSet<>();
    return resources.stream()
                    .filter(resource -> distinctResources.add(resource.getClass()))
                    .map(resource -> dataSourceForResource(manifest, resource))
                    .collect(Collectors.toList());
  }

  DataSource createEntryForResourceTypes(Manifest manifest, DynamicResource<?, ?> resource) {
    String typeName = resource.getModelVariantName() + "Resource";
    final String fileName = "src/" + typeName + '.' + TWEEDLE_EXTENSION;
    manifest.resources.add(new TypeReference(typeName, fileName, TWEEDLE_FORMAT));
    return new ByteArrayDataSource(fileName, coder.encodeProcessable(resource));
  }

  DataSource dataSourceForResource(Manifest manifest, JointedModelResource resource) {
    String typeName = resource.getClass().getSimpleName();
    final String fileName = "src/" + typeName + '.' + TWEEDLE_EXTENSION;
    manifest.resources.add(new TypeReference(typeName, fileName, TWEEDLE_FORMAT));
    return new ByteArrayDataSource(fileName, coder.encodeProcessable(resource));
  }

  // --- Resource name sanitization ---

  Map<Resource, ResourceNames> temporarilySanitizeResourceNames(NamedUserType userType) {
    IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<>(ResourceExpression.class) {
      @Override
      protected boolean isAcceptable(ResourceExpression resourceExpression) {
        return true;
      }
    };
    userType.crawl(crawler, CrawlPolicy.COMPLETE);
    Map<Resource, ResourceNames> originalNames = new IdentityHashMap<>();
    for (ResourceExpression resourceExpression : crawler.getList()) {
      Resource resource = resourceExpression.resource.getValue();
      if ((resource == null) || originalNames.containsKey(resource)) {
        continue;
      }
      String fallbackName = ResourceExportNames.entryFileName(resource);
      String safeName = ResourceExportNames.metadataName(resource.getName(), fallbackName);
      String safeOriginalFileName = ResourceExportNames.metadataOriginalFileName(resource.getOriginalFileName(), fallbackName);
      if (!Objects.equals(resource.getName(), safeName)
          || !Objects.equals(resource.getOriginalFileName(), safeOriginalFileName)) {
        originalNames.put(resource, new ResourceNames(resource.getName(), resource.getOriginalFileName()));
        resource.setName(safeName);
        resource.setOriginalFileName(safeOriginalFileName);
      }
    }
    return originalNames;
  }

  static void restoreResourceNames(Map<Resource, ResourceNames> originalNames) {
    for (Map.Entry<Resource, ResourceNames> entry : originalNames.entrySet()) {
      Resource resource = entry.getKey();
      ResourceNames names = entry.getValue();
      resource.setName(names.name);
      resource.setOriginalFileName(names.originalFileName);
    }
  }

  // --- Static helpers ---

  static void compareResources(Set<Resource> projectResources, Set<Resource> crawledResources) {
    for (Resource crawledResource : crawledResources) {
      if (!projectResources.contains(crawledResource)) {
        PrintUtilities.println(
            "WARNING: added missing resource",
            ResourceExportNames.diagnosticName(crawledResource));
      }
    }
  }

  static List<DataSource> collectEntries(Manifest manifest, Set<Resource> resources, DataSource[] dataSources) {
    List<DataSource> entries = new ArrayList<>(dataSources.length + 1 + resources.size());
    Collections.addAll(entries, dataSources);
    entries.add(JsonProjectManifest.versionDataSource());
    JsonProjectResourceEntries.addResources(manifest, entries, resources);
    return entries;
  }

  static Set<Resource> getResources(AbstractType<?, ?, ?> type, CrawlPolicy crawlPolicy) {
    IsInstanceCrawler<ResourceExpression> crawler = new IsInstanceCrawler<>(ResourceExpression.class) {
      @Override
      protected boolean isAcceptable(ResourceExpression resourceExpression1) {
        return true;
      }
    };

    type.crawl(crawler, crawlPolicy);
    Set<Resource> resources = new HashSet<>();
    for (ResourceExpression resourceExpression : crawler.getList()) {
      resources.add(resourceExpression.resource.getValue());
    }
    return resources;
  }

  static Set<String> manifestResourceNames(Manifest manifest) {
    Set<String> resourceNames = new HashSet<>();
    for (ResourceReference resourceReference : manifest.resources) {
      if (resourceReference.name != null) {
        resourceNames.add(resourceReference.name);
      }
    }
    return resourceNames;
  }

  static final class ResourceNames {
    final String name;
    final String originalFileName;

    ResourceNames(String name, String originalFileName) {
      this.name = name;
      this.originalFileName = originalFileName;
    }
  }

  private String serializedClass(NamedUserType userType) {
    Map<Resource, ResourceNames> originalNames = temporarilySanitizeResourceNames(userType);
    try {
      return coder.encode(userType);
    } finally {
      restoreResourceNames(originalNames);
    }
  }
}
