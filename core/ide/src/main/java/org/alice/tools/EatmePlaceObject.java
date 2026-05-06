package org.alice.tools;

import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.AccessLevel;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.FieldModifierFinalVolatileOrNeither;
import org.lgna.project.ast.InstanceCreation;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;
import org.lgna.project.io.IoUtilities;
import org.lgna.story.SBiped;
import org.lgna.story.SScene;
import org.lgna.story.resources.BipedResource;
import org.lgna.story.resources.biped.BunnyResource;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EatmePlaceObject {
  private static final String SUPPORTED_OBJECT = "alice-gallery://animals/bunny";
  private static final String PLACEMENT_ARTIFACT = "placement.json";
  private static final String DIFF_ARTIFACT = "scene.diff.json";
  private static final String PLACED_PROJECT = "placed-project.a3p";

  private EatmePlaceObject() {
  }

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) {
      System.exit(status);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    try {
      Arguments arguments = Arguments.parse(args);
      Placement placement = placeObject(arguments);
      out.println(resultJson(arguments.objectIdentifier()));
      return 0;
    } catch (IllegalArgumentException | IOException | VersionNotSupportedException ex) {
      err.println(ex.getMessage());
      return 2;
    } catch (RuntimeException ex) {
      err.println("object placement failed: " + ex.getMessage());
      return 3;
    }
  }

  private static Placement placeObject(Arguments arguments) throws IOException, VersionNotSupportedException {
    if (!SUPPORTED_OBJECT.equals(arguments.objectIdentifier())) {
      throw new IllegalArgumentException("unsupported object identifier: " + arguments.objectIdentifier());
    }
    if (!Files.isRegularFile(arguments.project())) {
      throw new IllegalArgumentException("project file does not exist: " + arguments.project());
    }
    Files.createDirectories(arguments.evidenceDir());

    Project project = IoUtilities.readProject(arguments.project().toFile());
    NamedUserType sceneType = findSceneType(project);
    List<String> beforeFields = fieldNames(sceneType);
    UserField field = createBunnyField(project, uniqueFieldName(sceneType, "bunny"));
    sceneType.fields.add(field);
    List<String> afterFields = fieldNames(sceneType);

    Path placedProject = arguments.evidenceDir().resolve(PLACED_PROJECT);
    IoUtilities.writeProject(placedProject.toFile(), project);
    if (!Files.isRegularFile(placedProject) || Files.size(placedProject) == 0) {
      throw new IOException("modified project was not written: " + placedProject);
    }

    Placement placement = new Placement(
        arguments.objectIdentifier(),
        sceneType.getName(),
        field.getName(),
        field.getValueType().getName(),
        placedProject.getFileName().toString(),
        beforeFields,
        afterFields);
    Files.writeString(
        arguments.evidenceDir().resolve(PLACEMENT_ARTIFACT),
        placementArtifactJson(placement),
        StandardCharsets.UTF_8);
    Files.writeString(
        arguments.evidenceDir().resolve(DIFF_ARTIFACT),
        diffArtifactJson(placement),
        StandardCharsets.UTF_8);
    return placement;
  }

  private static NamedUserType findSceneType(Project project) {
    for (UserField field : project.getProgramType().getDeclaredFields()) {
      AbstractType<?, ?, ?> valueType = field.getValueType();
      if (valueType instanceof NamedUserType namedUserType && valueType.isAssignableTo(SScene.class)) {
        return namedUserType;
      }
    }
    throw new IllegalArgumentException("project does not contain a program field typed by an SScene subtype");
  }

  private static UserField createBunnyField(Project project, String fieldName) {
    InstanceCreation creation = AstUtilities.createInstanceCreation(
        SBiped.class,
        new Class<?>[] {BipedResource.class},
        AstUtilities.createStaticFieldAccess(BunnyResource.class, BunnyResource.DEFAULT.name()));
    UserField field = new UserField(fieldName, creation.getType(), creation);
    field.accessLevel.setValue(AccessLevel.PRIVATE);
    field.finalVolatileOrNeither.setValue(FieldModifierFinalVolatileOrNeither.FINAL);
    field.managementLevel.setValue(ManagementLevel.MANAGED);
    return field;
  }

  private static String uniqueFieldName(NamedUserType sceneType, String preferredName) {
    Set<String> names = new HashSet<>(fieldNames(sceneType));
    if (!names.contains(preferredName)) {
      return preferredName;
    }
    int suffix = 2;
    while (names.contains(preferredName + suffix)) {
      suffix++;
    }
    return preferredName + suffix;
  }

  private static List<String> fieldNames(NamedUserType type) {
    List<String> names = new ArrayList<>();
    for (UserField field : type.getDeclaredFields()) {
      names.add(field.getName());
    }
    return names;
  }

  private static String resultJson(String objectIdentifier) {
    return "{"
        + "\"schema_version\":\"eatme.alice-object-placement-result/v1\","
        + "\"status\":\"placed\","
        + "\"object_identifier\":\"" + json(objectIdentifier) + "\","
        + "\"placement_artifact\":\"" + PLACEMENT_ARTIFACT + "\","
        + "\"scene_or_project_diff\":\"" + DIFF_ARTIFACT + "\""
        + "}";
  }

  private static String placementArtifactJson(Placement placement) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-object-placement-artifact/v1\",\n"
        + "  \"object_identifier\": \"" + json(placement.objectIdentifier()) + "\",\n"
        + "  \"scene_type\": \"" + json(placement.sceneType()) + "\",\n"
        + "  \"field_name\": \"" + json(placement.fieldName()) + "\",\n"
        + "  \"field_type\": \"" + json(placement.fieldType()) + "\",\n"
        + "  \"resource\": \"org.lgna.story.resources.biped.BunnyResource.DEFAULT\",\n"
        + "  \"placed_project\": \"" + json(placement.placedProject()) + "\"\n"
        + "}\n";
  }

  private static String diffArtifactJson(Placement placement) {
    return "{\n"
        + "  \"schema_version\": \"eatme.alice-object-placement-diff/v1\",\n"
        + "  \"scene_type\": \"" + json(placement.sceneType()) + "\",\n"
        + "  \"added_field\": \"" + json(placement.fieldName()) + "\",\n"
        + "  \"before_fields\": " + jsonArray(placement.beforeFields()) + ",\n"
        + "  \"after_fields\": " + jsonArray(placement.afterFields()) + ",\n"
        + "  \"placed_project\": \"" + json(placement.placedProject()) + "\"\n"
        + "}\n";
  }

  private static String jsonArray(List<String> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append('"').append(json(values.get(i))).append('"');
    }
    return builder.append(']').toString();
  }

  private static String json(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  record Arguments(Path project, String objectIdentifier, Path evidenceDir) {
    static Arguments parse(String[] args) {
      Path project = null;
      String objectIdentifier = null;
      Path evidenceDir = null;
      boolean json = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--project" -> project = Path.of(requireValue(args, ++i, "--project")).toAbsolutePath().normalize();
          case "--object" -> objectIdentifier = requireValue(args, ++i, "--object");
          case "--evidence-dir" -> evidenceDir = Path.of(requireValue(args, ++i, "--evidence-dir")).toAbsolutePath().normalize();
          case "--json" -> json = true;
          default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
        }
      }
      if (project == null) {
        throw new IllegalArgumentException("--project is required");
      }
      if (objectIdentifier == null || objectIdentifier.isBlank()) {
        throw new IllegalArgumentException("--object is required");
      }
      if (evidenceDir == null) {
        throw new IllegalArgumentException("--evidence-dir is required");
      }
      if (!json) {
        throw new IllegalArgumentException("--json is required");
      }
      return new Arguments(project, objectIdentifier, evidenceDir);
    }

    private static String requireValue(String[] args, int index, String option) {
      if (index >= args.length || args[index].startsWith("--")) {
        throw new IllegalArgumentException(option + " requires a value");
      }
      return args[index];
    }
  }

  record Placement(
      String objectIdentifier,
      String sceneType,
      String fieldName,
      String fieldType,
      String placedProject,
      List<String> beforeFields,
      List<String> afterFields) {
  }
}
