package org.lgna.project.io;

import org.alice.tweedle.file.ManifestEncoderDecoder;
import org.alice.tweedle.file.ProjectManifest;
import org.lgna.common.Resource;
import org.lgna.common.resources.ImageResource;
import org.lgna.project.Project;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.CrawlPolicy;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.ResourceExpression;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.story.SProgram;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class GoldenProjectCorpus {
  private final List<GoldenProjectCorpusCase> cases;

  private GoldenProjectCorpus(List<GoldenProjectCorpusCase> cases) {
    this.cases = List.copyOf(cases);
  }

  static GoldenProjectCorpus load(Path path) throws IOException {
    Properties properties = new Properties();
    try (InputStream inputStream = new FileInputStream(path.toFile())) {
      properties.load(inputStream);
    }
    return load(properties);
  }

  static GoldenProjectCorpus load(Properties properties) {
    Objects.requireNonNull(properties, "properties");
    List<String> caseIds = requiredList(properties, "cases", "corpus");
    Set<String> seenIds = new LinkedHashSet<>();
    List<GoldenProjectCorpusCase> cases = new ArrayList<>(caseIds.size());
    for (String id : caseIds) {
      if (!isSafeCaseId(id)) {
        throw new IllegalArgumentException("Invalid golden corpus case id '" + id + "'");
      }
      if (!seenIds.add(id)) {
        throw new IllegalArgumentException("Duplicate golden corpus case id '" + id + "'");
      }
      cases.add(loadCase(properties, id));
    }
    if (cases.isEmpty()) {
      throw new IllegalArgumentException("Golden corpus 'cases' must name at least one case");
    }
    return new GoldenProjectCorpus(cases);
  }

  List<GoldenProjectCorpusCase> cases() {
    return cases;
  }

  List<String> caseIds() {
    List<String> ids = new ArrayList<>(cases.size());
    for (GoldenProjectCorpusCase corpusCase : cases) {
      ids.add(corpusCase.id());
    }
    return ids;
  }

  GoldenProjectCorpusCase caseNamed(String id) {
    for (GoldenProjectCorpusCase corpusCase : cases) {
      if (corpusCase.id().equals(id)) {
        return corpusCase;
      }
    }
    throw new IllegalArgumentException("Golden corpus case not found: " + id);
  }

  private static GoldenProjectCorpusCase loadCase(Properties properties, String id) {
    String prefix = "case." + id + ".";
    GoldenProjectCorpusCase.ArchiveType archiveType = parseArchiveType(
        required(properties, prefix + "archiveType", id), id);
    String projectName = required(properties, prefix + "projectName", id);
    if (!isSafeProjectName(projectName)) {
      throw new IllegalArgumentException("Case '" + id + "' has unsafe projectName '" + projectName + "'");
    }
    Project.SceneCameraType cameraType = parseCameraType(required(properties, prefix + "cameraType", id), id);
    List<GoldenProjectCorpusCase.ResourceSpec> resources = parseResources(
        required(properties, prefix + "resources", id), id);
    List<String> expectedEntries = requiredList(properties, prefix + "expectedEntries", id);
    if (expectedEntries.isEmpty()) {
      throw new IllegalArgumentException("Case '" + id + "' expectedEntries must not be empty");
    }
    validateEntries(id, "expectedEntries", expectedEntries);
    List<String> absentEntries = optionalList(properties, prefix + "absentEntries");
    validateEntries(id, "absentEntries", absentEntries);
    boolean roundTripEditable = parseRequiredBoolean(
        required(properties, prefix + "roundTripEditable", id), id, "roundTripEditable");
    boolean exportAfterReopen = parseOptionalBoolean(
        properties.getProperty(prefix + "exportAfterReopen"), id, "exportAfterReopen");

    if ((archiveType == GoldenProjectCorpusCase.ArchiveType.PLAYER_EXPORT) && roundTripEditable) {
      throw new IllegalArgumentException("Case '" + id + "' .a3w cases must not set roundTripEditable=true");
    }
    return new GoldenProjectCorpusCase(
        id,
        archiveType,
        projectName,
        cameraType,
        resources,
        expectedEntries,
        absentEntries,
        roundTripEditable,
        exportAfterReopen);
  }

  private static GoldenProjectCorpusCase.ArchiveType parseArchiveType(String value, String id) {
    if ("a3p".equals(value)) {
      return GoldenProjectCorpusCase.ArchiveType.EDITABLE_PROJECT;
    }
    if ("a3w".equals(value)) {
      return GoldenProjectCorpusCase.ArchiveType.PLAYER_EXPORT;
    }
    throw new IllegalArgumentException(
        "Case '" + id + "' archiveType must be a3p or a3w, not '" + value + "'");
  }

  private static Project.SceneCameraType parseCameraType(String value, String id) {
    try {
      return Project.SceneCameraType.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Case '" + id + "' cameraType must be one of "
              + Arrays.toString(Project.SceneCameraType.values())
              + ", not '" + value + "'",
          e);
    }
  }

  private static List<GoldenProjectCorpusCase.ResourceSpec> parseResources(String value, String id) {
    if ("none".equals(value)) {
      return Collections.emptyList();
    }
    List<GoldenProjectCorpusCase.ResourceSpec> resources = new ArrayList<>();
    for (String declaration : splitList(value)) {
      int separator = declaration.indexOf(':');
      if (separator < 0) {
        throw new IllegalArgumentException(
            "Case '" + id + "' resource declaration must use kind:fileName: " + declaration);
      }
      String kind = declaration.substring(0, separator);
      String fileName = declaration.substring(separator + 1);
      if (!"image".equals(kind)) {
        throw new IllegalArgumentException(
            "Case '" + id + "' has unsupported resource kind '" + kind + "' in " + declaration);
      }
      assertSafeResourceFileName(id, fileName);
      resources.add(new GoldenProjectCorpusCase.ResourceSpec(
          GoldenProjectCorpusCase.ResourceSpec.Kind.IMAGE,
          fileName));
    }
    if (resources.isEmpty()) {
      throw new IllegalArgumentException("Case '" + id + "' resources must be none or a non-empty list");
    }
    return List.copyOf(resources);
  }

  private static boolean parseRequiredBoolean(String value, String id, String field) {
    if ("true".equals(value)) {
      return true;
    }
    if ("false".equals(value)) {
      return false;
    }
    throw new IllegalArgumentException("Case '" + id + "' " + field + " must be true or false, not '" + value + "'");
  }

  private static boolean parseOptionalBoolean(String value, String id, String field) {
    if ((value == null) || value.trim().isEmpty()) {
      return false;
    }
    return parseRequiredBoolean(value.trim(), id, field);
  }

  private static String required(Properties properties, String key, String id) {
    String value = properties.getProperty(key);
    if ((value == null) || value.trim().isEmpty()) {
      String field = key.substring(key.lastIndexOf('.') + 1);
      throw new IllegalArgumentException("Case '" + id + "' is missing required field " + field);
    }
    return value.trim();
  }

  private static List<String> requiredList(Properties properties, String key, String id) {
    return splitRequiredList(required(properties, key, id), key, id);
  }

  private static List<String> optionalList(Properties properties, String key) {
    String value = properties.getProperty(key);
    if ((value == null) || value.trim().isEmpty()) {
      return Collections.emptyList();
    }
    return splitRequiredList(value, key, key);
  }

  private static List<String> splitRequiredList(String value, String key, String id) {
    List<String> entries = splitList(value);
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("Case '" + id + "' field " + key + " must not be empty");
    }
    return entries;
  }

  private static List<String> splitList(String value) {
    List<String> entries = new ArrayList<>();
    for (String rawEntry : value.split(",")) {
      String entry = rawEntry.trim();
      if (!entry.isEmpty()) {
        entries.add(entry);
      }
    }
    return List.copyOf(entries);
  }

  private static void validateEntries(String id, String field, List<String> entries) {
    for (String entry : entries) {
      try {
        ZipEntryContainer.validateSafeEntryName(entry);
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Case '" + id + "' " + field + " contains unsafe archive entry '" + entry + "'",
            e);
      }
    }
  }

  private static void assertSafeResourceFileName(String id, String fileName) {
    if ((fileName == null) || fileName.trim().isEmpty() || !fileName.equals(fileName.trim())) {
      throw new IllegalArgumentException("Case '" + id + "' has unsafe resource file name '" + fileName + "'");
    }
    try {
      ZipEntryContainer.validateSafeEntryName("resources/" + fileName);
    } catch (IOException e) {
      throw new IllegalArgumentException("Case '" + id + "' has unsafe resource file name '" + fileName + "'", e);
    }
    if (fileName.contains("/")) {
      throw new IllegalArgumentException("Case '" + id + "' resource file name must not contain directories: " + fileName);
    }
  }

  private static boolean isSafeCaseId(String id) {
    return Pattern.matches("[A-Za-z0-9._-]+", id);
  }

  private static boolean isSafeProjectName(String name) {
    return Pattern.matches("[A-Za-z_][A-Za-z0-9_]*", name) && !JAVA_RESERVED_WORDS.contains(name);
  }

  private static final Set<String> JAVA_RESERVED_WORDS = Set.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
      "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
      "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
      "null", "var", "yield", "record", "sealed", "permits", "non-sealed");
}

final class GoldenProjectCorpusCase {
  enum ArchiveType {
    EDITABLE_PROJECT(IoUtilities.PROJECT_EXTENSION),
    PLAYER_EXPORT(IoUtilities.EXPORT_EXTENSION);

    private final String extension;

    ArchiveType(String extension) {
      this.extension = extension;
    }

    String extension() {
      return extension;
    }
  }

  static final class ResourceSpec {
    enum Kind {
      IMAGE
    }

    private final Kind kind;
    private final String fileName;

    ResourceSpec(Kind kind, String fileName) {
      this.kind = kind;
      this.fileName = fileName;
    }

    Kind kind() {
      return kind;
    }

    String fileName() {
      return fileName;
    }
  }

  private final String id;
  private final ArchiveType archiveType;
  private final String projectName;
  private final Project.SceneCameraType cameraType;
  private final List<ResourceSpec> resources;
  private final List<String> expectedEntries;
  private final List<String> absentEntries;
  private final boolean roundTripEditable;
  private final boolean exportAfterReopen;

  GoldenProjectCorpusCase(
      String id,
      ArchiveType archiveType,
      String projectName,
      Project.SceneCameraType cameraType,
      List<ResourceSpec> resources,
      List<String> expectedEntries,
      List<String> absentEntries,
      boolean roundTripEditable,
      boolean exportAfterReopen) {
    this.id = id;
    this.archiveType = archiveType;
    this.projectName = projectName;
    this.cameraType = cameraType;
    this.resources = List.copyOf(resources);
    this.expectedEntries = List.copyOf(expectedEntries);
    this.absentEntries = List.copyOf(absentEntries);
    this.roundTripEditable = roundTripEditable;
    this.exportAfterReopen = exportAfterReopen;
  }

  String id() {
    return id;
  }

  ArchiveType archiveType() {
    return archiveType;
  }

  String projectName() {
    return projectName;
  }

  Project.SceneCameraType cameraType() {
    return cameraType;
  }

  List<ResourceSpec> resources() {
    return resources;
  }

  List<String> expectedEntries() {
    return expectedEntries;
  }

  List<String> absentEntries() {
    return absentEntries;
  }

  boolean roundTripEditable() {
    return roundTripEditable;
  }

  boolean exportAfterReopen() {
    return exportAfterReopen;
  }
}

final class GoldenProjectFixtureGenerator {
  Project createProject(GoldenProjectCorpusCase corpusCase) {
    Objects.requireNonNull(corpusCase, "corpusCase");
    List<Resource> resources = createResources(corpusCase);
    NamedUserType programType = resources.isEmpty()
        ? programType(corpusCase.projectName())
        : programTypeReferencingResources(corpusCase.projectName(), resources);
    Project project = new Project(programType, corpusCase.cameraType());
    for (Resource resource : resources) {
      project.addResource(resource);
    }
    return project;
  }

  private static List<Resource> createResources(GoldenProjectCorpusCase corpusCase) {
    List<Resource> resources = new ArrayList<>(corpusCase.resources().size());
    for (GoldenProjectCorpusCase.ResourceSpec resourceSpec : corpusCase.resources()) {
      if (resourceSpec.kind() == GoldenProjectCorpusCase.ResourceSpec.Kind.IMAGE) {
        resources.add(imageResource(corpusCase.id(), resourceSpec.fileName()));
      } else {
        throw new IllegalArgumentException(
            "Unsupported golden corpus resource kind " + resourceSpec.kind()
                + " in case " + corpusCase.id());
      }
    }
    return resources;
  }

  private static ImageResource imageResource(String caseId, String fileName) {
    UUID uuid = UUID.nameUUIDFromBytes(
        ("golden-project-corpus:" + caseId + ":" + fileName).getBytes(StandardCharsets.UTF_8));
    ImageResource resource = new ImageResource(uuid);
    resource.setOriginalFileName(fileName);
    resource.setName(fileName);
    resource.setContent("image.png", imageBytes(caseId, fileName));
    resource.setWidth(1);
    resource.setHeight(1);
    return resource;
  }

  private static byte[] imageBytes(String caseId, String fileName) {
    byte[] seed = (caseId + ":" + fileName).getBytes(StandardCharsets.UTF_8);
    byte[] data = new byte[Math.min(32, Math.max(8, seed.length))];
    for (int i = 0; i < data.length; i++) {
      data[i] = seed[i % seed.length];
    }
    return data;
  }

  private static NamedUserType programType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SProgram.class));
    return type;
  }

  private static NamedUserType programTypeReferencingResources(String name, List<Resource> resources) {
    NamedUserType type = programType(name);
    BlockStatement body = new BlockStatement();
    for (int i = 0; i < resources.size(); i++) {
      Resource resource = resources.get(i);
      UserLocal local = new UserLocal("resource" + i, resource.getClass(), true);
      if (!(resource instanceof ImageResource imageResource)) {
        throw new IllegalArgumentException("Unsupported generated resource type " + resource.getClass());
      }
      body.statements.add(new LocalDeclarationStatement(
          local,
          new ResourceExpression(ImageResource.class, imageResource)));
    }
    UserMethod method = new UserMethod("rememberGoldenResources", Void.TYPE, new org.lgna.project.ast.UserParameter[0], body);
    type.methods.add(method);
    return type;
  }
}

final class GoldenProjectCorpusValidator {
  private final File outputDirectory;
  private final GoldenProjectFixtureGenerator fixtureGenerator;

  GoldenProjectCorpusValidator(File outputDirectory) {
    this(outputDirectory, new GoldenProjectFixtureGenerator());
  }

  GoldenProjectCorpusValidator(File outputDirectory, GoldenProjectFixtureGenerator fixtureGenerator) {
    this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
    this.fixtureGenerator = Objects.requireNonNull(fixtureGenerator, "fixtureGenerator");
  }

  GoldenProjectValidationResult validateCase(GoldenProjectCorpusCase corpusCase) throws Exception {
    Objects.requireNonNull(corpusCase, "corpusCase");
    try {
      if (corpusCase.archiveType() == GoldenProjectCorpusCase.ArchiveType.EDITABLE_PROJECT) {
        return validateEditableProject(corpusCase);
      }
      return validatePlayerExport(corpusCase);
    } catch (AssertionError e) {
      throw new AssertionError("Golden corpus case '" + corpusCase.id() + "' failed: " + e.getMessage(), e);
    }
  }

  private GoldenProjectValidationResult validateEditableProject(GoldenProjectCorpusCase corpusCase) throws Exception {
    if (!corpusCase.roundTripEditable()) {
      throw new AssertionError("Editable .a3p case must set roundTripEditable=true");
    }
    Project project = fixtureGenerator.createProject(corpusCase);
    File primaryArchive = archiveFile(corpusCase, "primary", IoUtilities.PROJECT_EXTENSION);
    File secondEditableSave = archiveFile(corpusCase, "second", IoUtilities.PROJECT_EXTENSION);
    File exportArchive = archiveFile(corpusCase, "export", IoUtilities.EXPORT_EXTENSION);

    IoUtilities.writeProject(primaryArchive, project);
    Project firstReopen = IoUtilities.readProject(primaryArchive);
    assertProjectMatches(corpusCase, firstReopen, "first reopen");

    IoUtilities.writeProject(secondEditableSave, firstReopen);
    Project secondReopen = IoUtilities.readProject(secondEditableSave);
    assertProjectMatches(corpusCase, secondReopen, "second reopen");

    if (corpusCase.exportAfterReopen()) {
      IoUtilities.exportProject(exportArchive, secondReopen);
    }

    ProjectManifest primaryManifest;
    try (ZipFile primaryZip = new ZipFile(primaryArchive)) {
      GoldenProjectZipAssertions.assertNoUnsafeEntries(primaryZip);
      GoldenProjectZipAssertions.assertExpectedEntries(primaryZip, corpusCase.expectedEntries());
      GoldenProjectZipAssertions.assertAbsentEntries(primaryZip, corpusCase.absentEntries());
      primaryManifest = readProjectManifest(primaryZip);
      assertManifestMatches(corpusCase, primaryManifest, IoUtilities.PROJECT_EXTENSION, "primary manifest");
    }

    try (ZipFile secondZip = new ZipFile(secondEditableSave)) {
      GoldenProjectZipAssertions.assertNoUnsafeEntries(secondZip);
      GoldenProjectZipAssertions.assertExpectedEntries(secondZip, corpusCase.expectedEntries());
    }

    ProjectManifest exportManifest = null;
    if (corpusCase.exportAfterReopen()) {
      try (ZipFile exportZip = new ZipFile(exportArchive)) {
        GoldenProjectZipAssertions.assertNoUnsafeEntries(exportZip);
        GoldenProjectZipAssertions.assertExpectedEntries(exportZip, exportEntriesFor(corpusCase));
        GoldenProjectZipAssertions.assertAbsentEntries(exportZip, List.of("programType.xml"));
        exportManifest = readProjectManifest(exportZip);
        assertManifestMatches(corpusCase, exportManifest, IoUtilities.EXPORT_EXTENSION, "export manifest");
      }
    }

    return new GoldenProjectValidationResult(
        primaryArchive,
        secondEditableSave,
        exportAfterReopenArchive(corpusCase, exportArchive),
        firstReopen,
        secondReopen,
        primaryManifest,
        exportManifest);
  }

  private GoldenProjectValidationResult validatePlayerExport(GoldenProjectCorpusCase corpusCase) throws Exception {
    if (corpusCase.roundTripEditable()) {
      throw new AssertionError("Player .a3w case must set roundTripEditable=false");
    }
    Project project = fixtureGenerator.createProject(corpusCase);
    File exportArchive = archiveFile(corpusCase, "primary", IoUtilities.EXPORT_EXTENSION);
    IoUtilities.exportProject(exportArchive, project);

    ProjectManifest exportManifest;
    try (ZipFile exportZip = new ZipFile(exportArchive)) {
      GoldenProjectZipAssertions.assertNoUnsafeEntries(exportZip);
      GoldenProjectZipAssertions.assertExpectedEntries(exportZip, corpusCase.expectedEntries());
      GoldenProjectZipAssertions.assertAbsentEntries(exportZip, corpusCase.absentEntries());
      exportManifest = readProjectManifest(exportZip);
      assertManifestMatches(corpusCase, exportManifest, IoUtilities.EXPORT_EXTENSION, "player export manifest");
    }

    return new GoldenProjectValidationResult(exportArchive, null, null, null, null, exportManifest, null);
  }

  private File archiveFile(GoldenProjectCorpusCase corpusCase, String phase, String extension) {
    return new File(outputDirectory, corpusCase.id() + "-" + phase + "." + extension);
  }

  private static File exportAfterReopenArchive(GoldenProjectCorpusCase corpusCase, File exportArchive) {
    return corpusCase.exportAfterReopen() ? exportArchive : null;
  }

  private static void assertProjectMatches(GoldenProjectCorpusCase corpusCase, Project project, String label) {
    if (project == null) {
      throw new AssertionError(label + " returned null project");
    }
    if (project.getProgramType() == null) {
      throw new AssertionError(label + " returned null program type");
    }
    if (!corpusCase.projectName().equals(project.getProgramType().getName())) {
      throw new AssertionError(
          label + " program name expected " + corpusCase.projectName()
              + " but was " + project.getProgramType().getName());
    }
    if (corpusCase.cameraType() != project.createSaveManifest().projectStructure.sceneCameraType) {
      throw new AssertionError(
          label + " camera type expected " + corpusCase.cameraType()
              + " but was " + project.createSaveManifest().projectStructure.sceneCameraType);
    }
    if (corpusCase.resources().size() != project.getResources().size()) {
      throw new AssertionError(
          label + " resource count expected " + corpusCase.resources().size()
              + " but was " + project.getResources().size());
    }
  }

  private static void assertManifestMatches(
      GoldenProjectCorpusCase corpusCase,
      ProjectManifest manifest,
      String expectedFileType,
      String label) {
    if (manifest == null) {
      throw new AssertionError(label + " is missing");
    }
    if ((manifest.metadata == null) || !expectedFileType.equals(manifest.metadata.fileType)) {
      throw new AssertionError(
          label + " metadata.fileType expected " + expectedFileType
              + " but was " + ((manifest.metadata == null) ? null : manifest.metadata.fileType));
    }
    if (!corpusCase.projectName().equals(manifest.description.name)) {
      throw new AssertionError(
          label + " description.name expected " + corpusCase.projectName()
              + " but was " + manifest.description.name);
    }
    if (corpusCase.cameraType() != manifest.projectStructure.sceneCameraType) {
      throw new AssertionError(
          label + " camera type expected " + corpusCase.cameraType()
              + " but was " + manifest.projectStructure.sceneCameraType);
    }
  }

  private static ProjectManifest readProjectManifest(ZipFile zipFile) throws IOException {
    ZipEntry entry = zipFile.getEntry(ProjectIo.MANIFEST_ENTRY_NAME);
    if (entry == null) {
      throw new AssertionError("Missing archive entry " + ProjectIo.MANIFEST_ENTRY_NAME);
    }
    try (InputStream inputStream = zipFile.getInputStream(entry)) {
      return ManifestEncoderDecoder.fromJsonOrThrow(
          new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
          ProjectManifest.class);
    }
  }

  private static List<String> exportEntriesFor(GoldenProjectCorpusCase corpusCase) {
    List<String> entries = new ArrayList<>();
    entries.add(ProjectIo.VERSION_ENTRY_NAME);
    entries.add(ProjectIo.MANIFEST_ENTRY_NAME);
    entries.add("src/" + corpusCase.projectName() + ".twe");
    for (GoldenProjectCorpusCase.ResourceSpec resource : corpusCase.resources()) {
      entries.add("resources/" + resource.fileName());
    }
    return entries;
  }
}

final class GoldenProjectValidationResult {
  private final File primaryArchive;
  private final File secondEditableSave;
  private final File exportArchive;
  private final Project firstReopen;
  private final Project secondReopen;
  private final ProjectManifest primaryManifest;
  private final ProjectManifest exportManifest;

  GoldenProjectValidationResult(
      File primaryArchive,
      File secondEditableSave,
      File exportArchive,
      Project firstReopen,
      Project secondReopen,
      ProjectManifest primaryManifest,
      ProjectManifest exportManifest) {
    this.primaryArchive = primaryArchive;
    this.secondEditableSave = secondEditableSave;
    this.exportArchive = exportArchive;
    this.firstReopen = firstReopen;
    this.secondReopen = secondReopen;
    this.primaryManifest = primaryManifest;
    this.exportManifest = exportManifest;
  }

  File primaryArchive() {
    return primaryArchive;
  }

  File secondEditableSave() {
    return secondEditableSave;
  }

  File exportArchive() {
    return exportArchive;
  }

  Project firstReopen() {
    return firstReopen;
  }

  Project secondReopen() {
    return secondReopen;
  }

  ProjectManifest primaryManifest() {
    return primaryManifest;
  }

  ProjectManifest exportManifest() {
    return exportManifest;
  }
}

final class GoldenProjectZipAssertions {
  private GoldenProjectZipAssertions() {
  }

  static void assertExpectedEntries(ZipFile zipFile, List<String> expectedEntries) throws IOException {
    for (String expectedEntry : expectedEntries) {
      assertSafeEntryName(expectedEntry);
      ZipEntry entry = zipFile.getEntry(expectedEntry);
      if (entry == null) {
        throw new AssertionError("Missing expected archive entry " + expectedEntry);
      }
      if (entry.isDirectory()) {
        throw new AssertionError("Expected archive entry is a directory: " + expectedEntry);
      }
      try (InputStream inputStream = zipFile.getInputStream(entry)) {
        inputStream.readAllBytes();
      }
    }
  }

  static void assertAbsentEntries(ZipFile zipFile, List<String> absentEntries) throws IOException {
    for (String absentEntry : absentEntries) {
      assertSafeEntryName(absentEntry);
      if (zipFile.getEntry(absentEntry) != null) {
        throw new AssertionError("Archive entry should be absent: " + absentEntry);
      }
    }
  }

  static void assertNoUnsafeEntries(ZipFile zipFile) {
    zipFile.stream().forEach(entry -> assertSafeEntryName(entry.getName()));
  }

  private static void assertSafeEntryName(String entryName) {
    try {
      ZipEntryContainer.validateSafeEntryName(entryName);
    } catch (IOException e) {
      throw new AssertionError("Unsafe archive entry " + entryName, e);
    }
    String lowerCaseEntryName = entryName.toLowerCase(Locale.ROOT);
    if (lowerCaseEntryName.startsWith("c:/") || lowerCaseEntryName.startsWith("c:\\")) {
      throw new AssertionError("Unsafe archive entry " + entryName);
    }
  }
}
