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
import edu.cmu.cs.dennisc.java.io.InputStreamUtilities;
import org.alice.serialization.tweedle.TweedleEncoderDecoder;
import org.alice.serialization.tweedle.UnsupportedTweedleDecodeException;
import org.alice.tweedle.file.*;
import org.lgna.common.Resource;
import org.lgna.common.resources.AudioResource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ProjectVersion;
import org.lgna.project.Version;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.*;
import org.lgna.story.resources.DynamicResource;
import org.lgna.story.resources.JointedModelResource;
import org.lgna.story.resourceutilities.ResourceTypeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

//TODO add migration on read - ProjectMigrationManager, MigrationManager, and DecodedVersion
public class JsonProjectIo extends DataSourceIo implements ProjectIo {
  private static final String TWEEDLE_EXTENSION = "twe";
  private static final String TWEEDLE_FORMAT = "tweedle";
  private static final String LEGACY_PROGRAM_TYPE_NAME = "Program";
  private static final String UNSUPPORTED_LEGACY_JSON_PROJECT_ARCHIVE_MESSAGE =
      "Unsupported legacy JSON project archive: manifest-declared Program Tweedle decode is unsupported and no safe legacy resource recovery applies";

  public static JsonProjectReader reader(ZipEntryContainer container) {
    return new JsonProjectReader(container);
  }

  public static JsonProjectWriter writer() {
    return new JsonProjectWriter();
  }

  private static class JsonProjectReader implements ProjectReader {
    private static final int MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH = 512;

    private final ZipEntryContainer container;
    private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

    JsonProjectReader(ZipEntryContainer container) {
      this.container = container;
    }

    @Override
    public Project readProject(boolean makeVrReady) throws IOException {
      ProjectManifest manifest = readManifest(ProjectManifest.class);
      TypeReadResult decodedTypes = readTypes(manifest, true);
      NamedUserType programType = decodedTypes.findByName(manifestName(manifest));
      if ((programType == null) && isUnsupportedLegacyProgramArchive(manifest, decodedTypes)) {
        if (hasExactlyOneRecoverableImageReference(manifest)) {
          Set<Resource> resources;
          try {
            resources = readResources(manifest);
          } catch (IOException e) {
            throw unsupportedLegacyJsonProjectArchive(manifest, decodedTypes, e);
          }
          if (hasExactlyOneRecoveredImageResource(resources)) {
            return new Project(null, new HashSet<>(decodedTypes.types), resources, sceneCameraType(manifest));
          }
        }
        throw unsupportedLegacyJsonProjectArchive(manifest, decodedTypes);
      }
      Set<Resource> resources = readResources(manifest);
      if (programType == null) {
        verifyProjectArchiveHasExpectedProgramType(manifest, decodedTypes);
      }
      verifyArchiveHasNoUnsupportedManifestTypes("Project archive", decodedTypes);
      Set<NamedUserType> namedUserTypes = new HashSet<>(decodedTypes.types);
      namedUserTypes.remove(programType);
      return new Project(programType, namedUserTypes, resources, sceneCameraType(manifest));
    }

    @Override
    public TypeResourcesPair readType() throws IOException {
      TypeManifest manifest = readManifest(TypeManifest.class);
      Set<Resource> resources = readResources(manifest);
      TypeReadResult decodedTypes = readTypes(manifest, false);
      NamedUserType type = decodedTypes.findByName(manifestName(manifest));
      if (type == null) {
        type = fallbackTypeForUnnamedManifest(manifest, decodedTypes);
      }
      if (type == null) {
        verifyTypeArchiveHasExpectedType(manifest, decodedTypes);
      }
      verifyArchiveHasNoUnsupportedManifestTypes("Type archive", decodedTypes);
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

    private <M extends Manifest> M readManifest(Class<M> manifestClass) throws IOException {
      InputStream is = container.getInputStream(MANIFEST_ENTRY_NAME);
      if (is == null) {
        return null;
      }
      try (InputStream manifestStream = is) {
        byte[] manifestBytes = InputStreamUtilities.getBytes(manifestStream);
        try {
          return ManifestEncoderDecoder.fromJsonOrThrow(
              new String(manifestBytes, StandardCharsets.UTF_8),
              manifestClass);
        } catch (IOException e) {
          throw new IOException("Unable to read " + MANIFEST_ENTRY_NAME, e);
        }
      }
    }

    private static Project.SceneCameraType sceneCameraType(ProjectManifest manifest) {
      if ((manifest == null) || (manifest.projectStructure == null) || (manifest.projectStructure.sceneCameraType == null)) {
        return Project.SceneCameraType.WindowCamera;
      }
      return manifest.projectStructure.sceneCameraType;
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

    // On XML side this reads the resources.xml and files in the referenced files in the resource directory.
    // It relies on further XML decoding inside Resource class as well.
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

    private TypeReadResult readTypes(
        Manifest manifest,
        boolean allowLiteralArithmeticFieldInitializers) throws IOException {
      TypeReadResult result = new TypeReadResult();
      if (manifest == null) {
        return result;
      }
      Set<AbstractDeclaration> typeTerminals = typeTerminals(manifest);
      for (ResourceReference resourceReference : manifest.resources) {
        if (resourceReference instanceof TypeReference typeReference) {
          result.hasTypeReferences = true;
          try {
            NamedUserType type = readTweedleType(
                typeReference,
                typeTerminals,
                allowLiteralArithmeticFieldInitializers);
            if (type != null) {
              result.add(type);
            }
          } catch (UnsupportedTweedleDecodeException e) {
            result.addUnsupportedTweedleType(typeReference, e);
          }
        }
      }
      return result;
    }

    private static Set<AbstractDeclaration> typeTerminals(Manifest manifest) {
      Map<String, NamedUserType> terminalsByName = new LinkedHashMap<>();
      for (ResourceReference resourceReference : manifest.resources) {
        if (resourceReference instanceof TypeReference typeReference
            && (typeReference.name != null)
            && !typeReference.name.isEmpty()) {
          terminalsByName.computeIfAbsent(typeReference.name, name -> {
            NamedUserType terminal = new NamedUserType();
            terminal.name.setValue(name);
            return terminal;
          });
        }
      }
      return new LinkedHashSet<>(terminalsByName.values());
    }

    private NamedUserType readTweedleType(
        TypeReference typeReference,
        Set<AbstractDeclaration> typeTerminals,
        boolean allowLiteralArithmeticFieldInitializers) throws IOException {
      if (!TWEEDLE_FORMAT.equals(typeReference.format)) {
        throw new IOException(
            "Unsupported type reference format '" + typeReference.format + "' for " + typeReferenceContext(typeReference));
      }
      if (typeReference.file == null) {
        throw new IOException("Type " + typeReference.name + " does not specify archive entry");
      }
      InputStream is = container.getInputStream(typeReference.file);
      if (is == null) {
        throw new IOException("Archive does not contain type entry " + typeReference.file);
      }
      try (InputStream typeStream = is) {
        byte[] typeBytes = InputStreamUtilities.getBytes(typeStream);
        AbstractNode decoded = coder.decode(
            new String(typeBytes, StandardCharsets.UTF_8),
            typeTerminals,
            allowLiteralArithmeticFieldInitializers);
        if (decoded == null) {
          throw new IOException("Tweedle type entry " + typeReference.file + " decoded to null");
        }
        if (decoded instanceof NamedUserType namedUserType) {
          return namedUserType;
        }
        throw new IOException("Tweedle type entry " + typeReference.file + " did not decode to a user type");
      } catch (UnsupportedTweedleDecodeException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new IOException("Unable to decode Tweedle type entry " + typeReference.file, e);
      } catch (VersionNotSupportedException e) {
        throw new IOException("Unable to decode Tweedle type entry " + typeReference.file, e);
      }
    }

    private static NamedUserType fallbackTypeForUnnamedManifest(Manifest manifest, TypeReadResult decodedTypes) {
      if (hasNoManifestName(manifest) && !decodedTypes.types.isEmpty()) {
        return decodedTypes.types.iterator().next();
      }
      return null;
    }

    private static void verifyTypeArchiveHasExpectedType(Manifest manifest, TypeReadResult decodedTypes) throws IOException {
      verifyArchiveHasExpectedType(
          "Type archive",
          "",
          "a type reference",
          manifest,
          decodedTypes);
    }

    private static void verifyProjectArchiveHasExpectedProgramType(Manifest manifest, TypeReadResult decodedTypes) throws IOException {
      if (hasNoManifestName(manifest)) {
        return;
      }
      verifyArchiveHasExpectedType(
          "Project archive",
          "program type ",
          "a type reference for the program type",
          manifest,
          decodedTypes);
    }

    private static boolean isUnsupportedLegacyProgramArchive(
        ProjectManifest manifest,
        TypeReadResult decodedTypes) {
      String expectedProgramName = manifestName(manifest);
      return isLegacyProgramArchive(manifest)
          && decodedTypes.hasUnsupportedTweedleDecodeFor(expectedProgramName);
    }

    private static boolean isLegacyProgramArchive(ProjectManifest manifest) {
      return (manifest != null)
          && (manifest.metadata != null)
          && LEGACY_PROGRAM_TYPE_NAME.equals(manifestName(manifest))
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
        TypeReadResult decodedTypes) {
      return unsupportedLegacyJsonProjectArchive(manifest, decodedTypes, null);
    }

    private static IOException unsupportedLegacyJsonProjectArchive(
        ProjectManifest manifest,
        TypeReadResult decodedTypes,
        IOException resourceRecoveryFailure) {
      String expectedProgramName = manifestName(manifest);
      UnsupportedTweedleDecodeException cause = decodedTypes.unsupportedTweedleDecodeCauseFor(expectedProgramName);
      Throwable effectiveCause = (resourceRecoveryFailure == null) ? cause : resourceRecoveryFailure;
      return (effectiveCause == null)
          ? new IOException(UNSUPPORTED_LEGACY_JSON_PROJECT_ARCHIVE_MESSAGE)
          : new IOException(UNSUPPORTED_LEGACY_JSON_PROJECT_ARCHIVE_MESSAGE, effectiveCause);
    }

    private static void verifyArchiveHasExpectedType(
        String archiveKind,
        String expectedNameRole,
        String missingReferenceDescription,
        Manifest manifest,
        TypeReadResult decodedTypes) throws IOException {
      String expectedName = manifestName(manifest);
      if (decodedTypes.hasTypeReferences) {
        throw new IOException(
            archiveKind + " manifest names " + expectedNameRole + "'" + expectedName
                + "' but decoded type names are " + decodedTypeNames(decodedTypes.types)
                + unsupportedTypeNamesClause(decodedTypes)
                + unsupportedDecodeReasonsClause(decodedTypes));
      }
      throw new IOException(
          archiveKind + " manifest for '" + expectedName + "' does not contain " + missingReferenceDescription);
    }

    private static void verifyArchiveHasNoUnsupportedManifestTypes(
        String archiveKind,
        TypeReadResult decodedTypes) throws IOException {
      if (decodedTypes.hasUnsupportedTweedleTypes()) {
        throw new IOException(
            archiveKind + " contains unsupported manifest-declared Tweedle type names "
                + decodedTypes.unsupportedTweedleTypeNames()
                + unsupportedDecodeReasonsClause(decodedTypes));
      }
    }

    private static boolean hasNoManifestName(Manifest manifest) {
      String name = manifestName(manifest);
      return (name == null) || name.isEmpty();
    }

    private static String decodedTypeNames(Set<NamedUserType> decodedTypes) {
      return decodedTypes.stream()
          .map(NamedUserType::getName)
          .sorted()
          .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String unsupportedTypeNamesClause(TypeReadResult decodedTypes) {
      if (!decodedTypes.hasUnsupportedTweedleTypes()) {
        return "";
      }
      return "; unsupported manifest-declared Tweedle type names are " + decodedTypes.unsupportedTweedleTypeNames();
    }

    private static String unsupportedDecodeReasonsClause(TypeReadResult decodedTypes) {
      if (!decodedTypes.hasUnsupportedTweedleTypes()) {
        return "";
      }
      return "; unsupported Tweedle decode reasons are " + decodedTypes.unsupportedTweedleDecodeReasons();
    }

    private static String typeReferenceContext(TypeReference typeReference) {
      StringBuilder sb = new StringBuilder("type reference");
      if ((typeReference.name != null) && !typeReference.name.isEmpty()) {
        sb.append(" '").append(typeReference.name).append("'");
      }
      if ((typeReference.file != null) && !typeReference.file.isEmpty()) {
        sb.append(" at archive entry '").append(typeReference.file).append("'");
      }
      return sb.toString();
    }

    private static String manifestName(Manifest manifest) {
      return (manifest == null) ? null : manifest.getName();
    }

    private static class TypeReadResult {
      private final Set<NamedUserType> types = new LinkedHashSet<>();
      private final Map<String, NamedUserType> typesByName = new HashMap<>();
      private final Map<String, UnsupportedTweedleDecodeException> unsupportedTweedleDecodeCausesByTypeName = new HashMap<>();
      private boolean hasTypeReferences;

      private void add(NamedUserType type) {
        types.add(type);
        if (type.getName() != null) {
          typesByName.putIfAbsent(type.getName(), type);
        }
      }

      private NamedUserType findByName(String name) {
        return (name == null) ? null : typesByName.get(name);
      }

      private void addUnsupportedTweedleType(TypeReference typeReference, UnsupportedTweedleDecodeException e) {
        String typeName = unsupportedTweedleTypeName(typeReference);
        unsupportedTweedleDecodeCausesByTypeName.putIfAbsent(typeName, e);
      }

      private static String unsupportedTweedleTypeName(TypeReference typeReference) {
        if ((typeReference.name != null) && !typeReference.name.isEmpty()) {
          return typeReference.name;
        }
        if ((typeReference.file != null) && !typeReference.file.isEmpty()) {
          return typeReference.file;
        }
        return "<unnamed>";
      }

      private boolean hasUnsupportedTweedleDecodeFor(String name) {
        return (name != null) && unsupportedTweedleDecodeCausesByTypeName.containsKey(name);
      }

      private UnsupportedTweedleDecodeException unsupportedTweedleDecodeCauseFor(String name) {
        return (name == null) ? null : unsupportedTweedleDecodeCausesByTypeName.get(name);
      }

      private boolean hasUnsupportedTweedleTypes() {
        return !unsupportedTweedleDecodeCausesByTypeName.isEmpty();
      }

      private String unsupportedTweedleTypeNames() {
        return unsupportedTweedleDecodeCausesByTypeName.keySet().stream()
            .sorted()
            .collect(Collectors.joining(", ", "[", "]"));
      }

      private String unsupportedTweedleDecodeReasons() {
        return unsupportedTweedleDecodeCausesByTypeName.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + ": " + unsupportedTweedleDecodeReason(entry.getValue()))
            .collect(Collectors.joining(", ", "[", "]"));
      }

      private static String unsupportedTweedleDecodeReason(UnsupportedTweedleDecodeException e) {
        String message = e.getMessage();
        if ((message != null) && !message.isBlank()) {
          return boundedSingleLineUnsupportedTweedleReason(message);
        }
        return e.getClass().getSimpleName();
      }

      private static String boundedSingleLineUnsupportedTweedleReason(String message) {
        String singleLine = message.strip().replaceAll("\\s+", " ");
        if (singleLine.length() <= MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH) {
          return singleLine;
        }
        return singleLine.substring(0, MAX_UNSUPPORTED_TWEEDLE_REASON_LENGTH - 3) + "...";
      }
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

  private static class JsonProjectWriter implements ProjectWriter {
    JsonProjectWriter() {
    }

    private final TweedleEncoderDecoder coder = new TweedleEncoderDecoder();

    @Override
    public void writeType(OutputStream os, NamedUserType type, DataSource... dataSources) throws IOException {
      TypeManifest manifest = createTypeManifest(type);
      Set<Resource> resources = getResources(type, CrawlPolicy.EXCLUDE_REFERENCES_ENTIRELY);

      List<DataSource> entries = collectEntries(manifest, resources, dataSources);
      entries.add(dataSourceForType(manifest, type));
      entries.add(manifestDataSource(manifest));
      writeDataSources(os, entries);
    }

    private TypeManifest createTypeManifest(AbstractType<?, ?, ?> type) {
      final TypeManifest manifest = new TypeManifest();
      manifest.description.name = type.getName();
      manifest.provenance.aliceVersion = ProjectVersion.getCurrentVersion().toString();
      manifest.metadata.fileType = IoUtilities.TYPE_EXTENSION;
      manifest.metadata.identifier.name = type.getId().toString();
      manifest.metadata.identifier.type = Manifest.ProjectType.Library;
      return manifest;
    }

    @Override
    public void writeProject(OutputStream os, final Project project, DataSource... dataSources) throws IOException {
      final JsonModelIo.ExportFormat format = JsonModelIo.ExportFormat.GLTF;
      Manifest manifest = project.createExportManifest();
      Set<Resource> resources = getResources(project.getProgramType(), CrawlPolicy.COMPLETE);
      compareResources(project.getResources(), resources);

      List<DataSource> entries = collectEntries(manifest, resources, dataSources);
      Set<String> manifestResourceNames = manifestResourceNames(manifest);
      ModelResourceCrawler crawler = new ModelResourceCrawler();
      project.getProgramType().crawl(crawler, CrawlPolicy.COMPLETE);
      entries.addAll(createEntriesForTypes(manifest, crawler.activeUserTypes, manifestResourceNames));
      Map<String, Set<JointedModelResource>> modelResources = crawler.modelResources;
      for (Set<JointedModelResource> resourceSet : modelResources.values()) {
        JsonModelIo modelIo = new JsonModelIo(resourceSet, format);
        entries.addAll(modelIo.createDataSources("models"));
        entries.addAll(createEntriesForResourceTypes(manifest, resourceSet));
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
        entries.add(createEntryForResourceTypes(manifest, dynamicResource));
        manifest.resources.add(modelIo.createModelReference("models"));
      }
      entries.add(manifestDataSource(manifest));
      writeDataSources(os, entries);
    }

    private Collection<? extends DataSource> createEntriesForTypes(
        Manifest manifest,
        Set<NamedUserType> userTypes,
        Set<String> manifestResourceNames) {
      return userTypes.stream()
          .sorted(Comparator.comparingInt(AbstractType::hierarchyDepth))
          .map(ut -> dataSourceForType(manifest, ut, manifestResourceNames))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    private DataSource dataSourceForType(Manifest manifest, NamedUserType ut) {
      return dataSourceForType(manifest, ut, manifestResourceNames(manifest));
    }

    private DataSource dataSourceForType(Manifest manifest, NamedUserType ut, Set<String> manifestResourceNames) {
      // Special case to catch older models from starter worlds
      String likelyTypeName = ut.getName();
      String typeName = "SandDunes".equals(likelyTypeName) ? "Terrain" : likelyTypeName;

      if (manifestResourceNames.add(typeName)) {
        final String fileName = "src/" + typeName + '.' + TWEEDLE_EXTENSION;
        manifest.resources.add(new TypeReference(typeName, fileName, TWEEDLE_FORMAT));
        return new ByteArrayDataSource(fileName, serializedClass(ut));
      }
      return null;
    }

    private String serializedClass(NamedUserType userType) {
      Map<Resource, ResourceNames> originalNames = temporarilySanitizeResourceNames(userType);
      try {
        return coder.encode(userType);
      } finally {
        restoreResourceNames(originalNames);
      }
    }

    private Map<Resource, ResourceNames> temporarilySanitizeResourceNames(NamedUserType userType) {
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

    private void restoreResourceNames(Map<Resource, ResourceNames> originalNames) {
      for (Map.Entry<Resource, ResourceNames> entry : originalNames.entrySet()) {
        Resource resource = entry.getKey();
        ResourceNames names = entry.getValue();
        resource.setName(names.name);
        resource.setOriginalFileName(names.originalFileName);
      }
    }

    private static class ResourceNames {
      private final String name;
      private final String originalFileName;

      private ResourceNames(String name, String originalFileName) {
        this.name = name;
        this.originalFileName = originalFileName;
      }
    }

    private Collection<? extends DataSource> createEntriesForResourceTypes(Manifest manifest, Set<JointedModelResource> resources) {
      Set<Class<?>> distinctResources = new HashSet<>();
      return resources.stream()
                      .filter(resource -> distinctResources.add(resource.getClass()))
                      .map(resource -> dataSourceForResource(manifest, resource))
                      .collect(Collectors.toList());
    }

    private DataSource createEntryForResourceTypes(Manifest manifest, DynamicResource<?, ?> resource) {
      String typeName = resource.getModelVariantName() + "Resource";
      final String fileName = "src/" + typeName + '.' + TWEEDLE_EXTENSION;
      manifest.resources.add(new TypeReference(typeName, fileName, TWEEDLE_FORMAT));
      return new ByteArrayDataSource(fileName, coder.encodeProcessable(resource));
    }

    private DataSource dataSourceForResource(Manifest manifest, JointedModelResource resource) {
      String typeName = resource.getClass().getSimpleName();
      final String fileName = "src/" + typeName + '.' + TWEEDLE_EXTENSION;
      manifest.resources.add(new TypeReference(typeName, fileName, TWEEDLE_FORMAT));
      return new ByteArrayDataSource(fileName, coder.encodeProcessable(resource));
    }

    private void compareResources(Set<Resource> projectResources, Set<Resource> crawledResources) {
      for (Resource crawledResource : crawledResources) {
        if (!projectResources.contains(crawledResource)) {
          PrintUtilities.println("WARNING: added missing resource", crawledResource);
        }
      }
    }

    private List<DataSource> collectEntries(Manifest manifest, Set<Resource> resources, DataSource[] dataSources) {
      List<DataSource> entries = new ArrayList<>();
      Collections.addAll(entries, dataSources);
      entries.add(versionDataSource());
      JsonProjectResourceEntries.addResources(manifest, entries, resources);
      return entries;
    }

    private static DataSource versionDataSource() {
      return new ByteArrayDataSource(VERSION_ENTRY_NAME, ProjectVersion.getCurrentVersion().toString());
    }

    private static DataSource manifestDataSource(Manifest manifest) {
      return new ByteArrayDataSource(MANIFEST_ENTRY_NAME, ManifestEncoderDecoder.toJson(manifest));
    }

    private Set<Resource> getResources(AbstractType<?, ?, ?> type, CrawlPolicy crawlPolicy) {
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


    private static Set<String> manifestResourceNames(Manifest manifest) {
      Set<String> resourceNames = new HashSet<>();
      for (ResourceReference resourceReference : manifest.resources) {
        if (resourceReference.name != null) {
          resourceNames.add(resourceReference.name);
        }
      }
      return resourceNames;
    }
  }
}
