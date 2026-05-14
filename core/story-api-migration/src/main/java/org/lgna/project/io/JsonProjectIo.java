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

import edu.cmu.cs.dennisc.java.io.InputStreamUtilities;
import edu.cmu.cs.dennisc.java.util.zip.DataSource;
import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.alice.tweedle.file.*;
import org.lgna.common.Resource;
import org.lgna.common.resources.AudioResource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;
import org.lgna.project.Version;
import org.lgna.project.ast.*;
import org.lgna.story.resources.DynamicResource;
import org.lgna.story.resources.JointedModelResource;
import org.lgna.story.resourceutilities.ResourceTypeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

//TODO add migration on read - ProjectMigrationManager, MigrationManager, and DecodedVersion
public class JsonProjectIo extends DataSourceIo implements ProjectIo {
  private static final String LEGACY_PROGRAM_TYPE_NAME = "Program";
  private static final String UNSUPPORTED_LEGACY_JSON_PROJECT_ARCHIVE_MESSAGE =
      "Unsupported legacy JSON project archive: manifest-declared Program Tweedle decode is unsupported and no safe legacy resource recovery applies";

  public static JsonProjectReader reader(ZipEntryContainer container) {
    return new JsonProjectReader(container);
  }

  public static JsonProjectWriter writer() {
    return new JsonProjectWriter();
  }

  static class JsonProjectReader implements ProjectReader {
    private final ZipEntryContainer container;
    private final JsonTypeResolver typeResolver;

    JsonProjectReader(ZipEntryContainer container) {
      this.container = container;
      this.typeResolver = new JsonTypeResolver(container, new TweedleEncoderDecoder());
    }

    @Override
    public Project readProject(boolean makeVrReady) throws IOException {
      ProjectManifest manifest = JsonProjectManifest.readManifest(container, ProjectManifest.class);
      JsonTypeResolver.TypeReadResult decodedTypes = typeResolver.readTypes(manifest, true);
      NamedUserType programType = decodedTypes.findByName(JsonProjectManifest.manifestName(manifest));
      if ((programType == null) && isUnsupportedLegacyProgramArchive(manifest, decodedTypes)) {
        if (hasExactlyOneRecoverableImageReference(manifest)) {
          Set<Resource> resources;
          try {
            resources = readResources(manifest);
          } catch (IOException e) {
            throw unsupportedLegacyJsonProjectArchive(manifest, decodedTypes, e);
          }
          if (hasExactlyOneRecoveredImageResource(resources)) {
            return new Project(null, new HashSet<>(decodedTypes.types), resources, JsonProjectManifest.sceneCameraType(manifest));
          }
        }
        throw unsupportedLegacyJsonProjectArchive(manifest, decodedTypes);
      }
      Set<Resource> resources = readResources(manifest);
      if (programType == null) {
        JsonTypeResolver.verifyProjectArchiveHasExpectedProgramType(manifest, decodedTypes);
      }
      JsonTypeResolver.verifyArchiveHasNoUnsupportedManifestTypes("Project archive", decodedTypes);
      Set<NamedUserType> namedUserTypes = new HashSet<>(decodedTypes.types);
      namedUserTypes.remove(programType);
      return new Project(programType, namedUserTypes, resources, JsonProjectManifest.sceneCameraType(manifest));
    }

    @Override
    public TypeResourcesPair readType() throws IOException {
      TypeManifest manifest = JsonProjectManifest.readManifest(container, TypeManifest.class);
      Set<Resource> resources = readResources(manifest);
      JsonTypeResolver.TypeReadResult decodedTypes = typeResolver.readTypes(manifest, false);
      NamedUserType type = decodedTypes.findByName(JsonProjectManifest.manifestName(manifest));
      if (type == null) {
        type = JsonTypeResolver.fallbackTypeForUnnamedManifest(manifest, decodedTypes);
      }
      if (type == null) {
        JsonTypeResolver.verifyTypeArchiveHasExpectedType(manifest, decodedTypes);
      }
      JsonTypeResolver.verifyArchiveHasNoUnsupportedManifestTypes("Type archive", decodedTypes);
      return new TypeResourcesPair(type, resources);
    }

    @Override
    public Version checkForFutureVersion() throws IOException {
      Version decodedProjectVersion = readSourceProgramVersion();
      if (ProjectVersion.getCurrentVersion().compareTo(decodedProjectVersion) < 0) {
        return decodedProjectVersion;
      }
      return null;
    }

    @Override
    public void setResourceTypeHelper(ResourceTypeHelper typeHelper) {
      // Ignored for now
    }

    private Version readSourceProgramVersion() throws IOException {
      InputStream is = container.getInputStream(VERSION_ENTRY_NAME);
      if (is == null) {
        throw new IOException("Archive does not contain entry " + VERSION_ENTRY_NAME);
      }
      try (InputStream versionStream = is) {
        byte[] versionBytes = InputStreamUtilities.getBytes(versionStream);
        return new Version(new String(versionBytes, StandardCharsets.UTF_8));
      }
    }

    private Set<Resource> readResources(Manifest manifest) throws IOException {
      Set<Resource> resources = new HashSet<>();
      if (manifest == null) {
        return resources;
      }
      for (ResourceReference resourceReference : manifest.resources) {
        Resource resource = readResource(resourceReference);
        if (resource != null) {
          resources.add(resource);
        }
      }
      return resources;
    }

    private Resource readResource(ResourceReference resourceReference) throws IOException {
      if (!(resourceReference instanceof ImageReference) && !(resourceReference instanceof AudioReference)) {
        return null;
      }
      String entry = resourceReference.file;
      if (entry == null) {
        throw new IOException("Resource " + resourceReference.name + " does not specify archive entry");
      }
      if (!ResourceExportNames.isResourceEntryName(entry)) {
        throw new IOException("Resource " + resourceReference.name + " references archive entry outside resources directory: " + entry);
      }
      InputStream is = container.getInputStream(entry);
      if (is == null) {
        throw new IOException("Archive does not contain resource entry " + entry);
      }
      try (InputStream resourceStream = is) {
        byte[] data = InputStreamUtilities.getBytes(resourceStream);
        if (resourceReference instanceof ImageReference imageReference) {
          ImageResource resource = new ImageResource(requireUuid(imageReference, imageReference.uuid));
          applyResourceReference(resource, imageReference, data);
          resource.setWidth((int) imageReference.width);
          resource.setHeight((int) imageReference.height);
          return resource;
        }
        if (resourceReference instanceof AudioReference audioReference) {
          AudioResource resource = new AudioResource(requireUuid(audioReference, audioReference.uuid));
          applyResourceReference(resource, audioReference, data);
          resource.setDuration(audioReference.duration);
          return resource;
        }
      }
      return null;
    }

    // --- Legacy recovery ---

    private static boolean isUnsupportedLegacyProgramArchive(
        ProjectManifest manifest,
        JsonTypeResolver.TypeReadResult decodedTypes) {
      String expectedProgramName = JsonProjectManifest.manifestName(manifest);
      return isLegacyProgramArchive(manifest)
          && decodedTypes.hasUnsupportedTweedleDecodeFor(expectedProgramName);
    }

    private static boolean isLegacyProgramArchive(ProjectManifest manifest) {
      return (manifest != null)
          && (manifest.metadata != null)
          && LEGACY_PROGRAM_TYPE_NAME.equals(JsonProjectManifest.manifestName(manifest))
          && IoUtilities.EXPORT_EXTENSION.equals(manifest.metadata.fileType);
    }

    private static boolean hasExactlyOneRecoverableImageReference(Manifest manifest) {
      int imageReferenceCount = 0;
      int programTypeReferenceCount = 0;
      for (ResourceReference resourceReference : manifest.resources) {
        if (resourceReference instanceof TypeReference typeReference) {
          if (!LEGACY_PROGRAM_TYPE_NAME.equals(typeReference.name) || (++programTypeReferenceCount > 1)) {
            return false;
          }
          continue;
        }
        if (!(resourceReference instanceof ImageReference) || (++imageReferenceCount > 1)) {
          return false;
        }
      }
      return (programTypeReferenceCount == 1) && (imageReferenceCount == 1);
    }

    private static boolean hasExactlyOneRecoveredImageResource(Set<Resource> resources) {
      return (resources.size() == 1) && (resources.iterator().next() instanceof ImageResource);
    }

    private static IOException unsupportedLegacyJsonProjectArchive(
        ProjectManifest manifest,
        JsonTypeResolver.TypeReadResult decodedTypes) {
      return unsupportedLegacyJsonProjectArchive(manifest, decodedTypes, null);
    }

    private static IOException unsupportedLegacyJsonProjectArchive(
        ProjectManifest manifest,
        JsonTypeResolver.TypeReadResult decodedTypes,
        IOException resourceRecoveryFailure) {
      String expectedProgramName = JsonProjectManifest.manifestName(manifest);
      org.alice.serialization.tweedle.UnsupportedTweedleDecodeException cause = decodedTypes.unsupportedTweedleDecodeCauseFor(expectedProgramName);
      Throwable effectiveCause = (resourceRecoveryFailure == null) ? cause : resourceRecoveryFailure;
      return (effectiveCause == null)
          ? new IOException(UNSUPPORTED_LEGACY_JSON_PROJECT_ARCHIVE_MESSAGE)
          : new IOException(UNSUPPORTED_LEGACY_JSON_PROJECT_ARCHIVE_MESSAGE, effectiveCause);
    }

    private static UUID requireUuid(
        ResourceReference resourceReference,
        UUID uuid) throws IOException {
      if (uuid == null) {
        throw new IOException(
            "Resource " + resourceReference.name + " does not specify UUID");
      }
      return uuid;
    }

    private static void applyResourceReference(Resource resource, ResourceReference resourceReference, byte[] data) {
      resource.setName(resourceReference.name);
      resource.setOriginalFileName(resourceReference.name);
      resource.setContent(resourceReference.format, data);
    }
  }

  static class JsonProjectWriter implements ProjectWriter {
    private final JsonResourceEntryWriter entryWriter;

    JsonProjectWriter() {
      this.entryWriter = new JsonResourceEntryWriter(new TweedleEncoderDecoder());
    }

    @Override
    public void writeType(OutputStream os, NamedUserType type, DataSource... dataSources) throws IOException {
      TypeManifest manifest = JsonResourceEntryWriter.createTypeManifest(type);
      Set<Resource> resources = JsonResourceEntryWriter.getResources(type, CrawlPolicy.EXCLUDE_REFERENCES_ENTIRELY);

      List<DataSource> entries = JsonResourceEntryWriter.collectEntries(manifest, resources, dataSources);
      entries.add(entryWriter.dataSourceForType(manifest, type));
      entries.add(JsonProjectManifest.manifestDataSource(manifest));
      writeDataSources(os, entries);
    }

    @Override
    public void writeProject(OutputStream os, final Project project, DataSource... dataSources) throws IOException {
      final JsonModelIo.ExportFormat format = JsonModelIo.ExportFormat.GLTF;
      Manifest manifest = project.createExportManifest();
      ModelResourceCrawler crawler = new ModelResourceCrawler();
      project.getProgramType().crawl(crawler, CrawlPolicy.COMPLETE);
      Set<Resource> resources = crawler.resources;
      JsonResourceEntryWriter.compareResources(project.getResources(), resources);

      List<DataSource> entries = JsonResourceEntryWriter.collectEntries(manifest, resources, dataSources);
      Set<String> manifestResourceNames = JsonResourceEntryWriter.manifestResourceNames(manifest);
      entries.addAll(entryWriter.createEntriesForTypes(manifest, crawler.activeUserTypes, manifestResourceNames));
      Map<String, Set<JointedModelResource>> modelResources = crawler.modelResources;
      for (Set<JointedModelResource> resourceSet : modelResources.values()) {
        JsonModelIo modelIo = new JsonModelIo(resourceSet, format);
        entries.addAll(modelIo.createDataSources("models"));
        entries.addAll(entryWriter.createEntriesForResourceTypes(manifest, resourceSet));
        manifest.resources.add(modelIo.createModelReference("models"));
      }
      final Set<InstanceCreation> personResourceCreations = crawler.personCreations;
      if (!personResourceCreations.isEmpty()) {
        JsonModelIo modelIo = new JsonPersonIo(personResourceCreations, format);
        entries.addAll(modelIo.createDataSources("models"));
        manifest.resources.add(modelIo.createModelReference("models"));
      }
      for (DynamicResource<?, ?> dynamicResource: crawler.dynamicResources) {
        JsonModelIo modelIo = new JsonModelIo(dynamicResource, format);
        entries.addAll(modelIo.createDataSources("models"));
        entries.add(entryWriter.createEntryForResourceTypes(manifest, dynamicResource));
        manifest.resources.add(modelIo.createModelReference("models"));
      }
      entries.add(JsonProjectManifest.manifestDataSource(manifest));
      writeDataSources(os, entries);
    }
  }
}
