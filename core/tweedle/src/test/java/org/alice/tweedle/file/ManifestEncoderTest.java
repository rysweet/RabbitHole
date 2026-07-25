package org.alice.tweedle.file;

import org.junit.Test;

import java.io.IOException;
import java.time.YearMonth;

import static org.junit.Assert.*;

public class ManifestEncoderTest {
  private static final String SAMPLE_LIBRARY = """
    {
      "description": {
        "name": "Alice SThing Library",
        "icon": "thumbnail.png",
        "tags": [],
        "groupTags": [],
        "themeTags": []
      },
      "metadata": {
        "formatVersion": "0.1",
        "identifier": {
          "name": "SystemLibrary",
          "type": "Library",
          "version": "0.7"
        }
      },
      "provenance": {
        "aliceVersion": "3.4.1.0",
        "created": "2018",
        "creator": "Not Dennis"
      },
      "prerequisites": []
    }""";

  private static final String LIBRARY_WITH_DATE = """
    {
      "description": {
        "name": "Alice SThing Library",
        "icon": "thumbnail.png",
        "tags": [],
        "groupTags": [],
        "themeTags": []
      },
      "metadata": {
        "formatVersion": "0.1",
        "identifier": {
          "name": "SystemLibrary",
          "type": "Library",
          "version": "0.7"
        }
      },
      "provenance": {
        "aliceVersion": "3.4.1.0",
        "created": "2010-01-01T12:00:00+01:00",
        "creator": "Not Dennis"
      },
      "prerequisites": []
    }""";

  private static final String SAMPLE_MODEL = """
    {
        "description": {
          "name": "Alien",
          "icon": "thumbnail.png",
          "tags": [
            "alien",
            "space"
          ],
          "groupTags": [
            "characters"
          ],
          "themeTags": [
            "*outer space"
          ]
        },
        "metadata": {
          "formatVersion": "0.1",
          "identifier": {
            "name": "Alien",
            "type": "Model",
            "version": "1.0"
          }
        },
        "provenance": {
          "aliceVersion": "3.4.1.0",
          "created": "2011",
          "creator": "Laura Paoletti"
        },
        "rootJoints": [
          "ROOT"
        ],
        "additionalJoints": [
          {
            "name": "LOWER_LIP",
            "parent": "MOUTH",
            "visibility": "COMPLETELY_HIDDEN"
          }
        ],
        "additionalJointArrays": [],
        "poses": [],
        "boundingBox": {
          "min": [
            "-0.2",
            "7.2",
            "-0.1"
          ],
          "max": [
            "0.2",
            "1.4",
            "0.3"
          ]
        },
        "textureSets": [
          {
            "name":"Alien_DEFAULT",
            "idToResourceMap":{"1":"Alien_DEFAULT_texture_1_diffuseMap"}
          }
        ],
        "models": [
          {
            "structure": "defaultSkeletonVisual",
            "textureSet": "default",
            "icon": "thumbnail.png"
          }
        ]
      }""";

  private static final String SAMPLE_MODEL_WITH_3_RESOURCES = """
    {
      "rootJoints": [],
      "additionalJoints": [
        {
          "name": "LOWER_LIP",
          "parent": "MOUTH",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "LEFT_THUMB_TIP",
          "parent": "LEFT_THUMB_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "LEFT_INDEX_FINGER_TIP",
          "parent": "LEFT_INDEX_FINGER_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "LEFT_MIDDLE_FINGER_TIP",
          "parent": "LEFT_MIDDLE_FINGER_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "LEFT_PINKY_FINGER_TIP",
          "parent": "LEFT_PINKY_FINGER_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "RIGHT_THUMB_TIP",
          "parent": "RIGHT_THUMB_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "RIGHT_INDEX_FINGER_TIP",
          "parent": "RIGHT_INDEX_FINGER_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "RIGHT_MIDDLE_FINGER_TIP",
          "parent": "RIGHT_MIDDLE_FINGER_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "RIGHT_PINKY_FINGER_TIP",
          "parent": "RIGHT_PINKY_FINGER_KNUCKLE",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "LEFT_TOES",
          "parent": "LEFT_FOOT",
          "visibility": "COMPLETELY_HIDDEN"
        },
        {
          "name": "RIGHT_TOES",
          "parent": "RIGHT_FOOT",
          "visibility": "COMPLETELY_HIDDEN"
        }
      ],
      "additionalJointArrays": [],
      "additionalJointArrayIds": [],
      "poses": [],
      "boundingBox": {
        "min": [
          -0.281617,
          7.213192E-4,
          -0.18244708
        ],
        "max": [
          0.2846615,
          1.4431626,
          0.39562756
        ]
      },
      "placeOnGround": false,
      "textureSets": [
        {
          "name": "Alien_DEFAULT",
          "idToResourceMap": {
            "1": "Alien_DEFAULT_texture_1_diffuseMap"
          }
        }
      ],
      "models": [
        {
          "name": "DEFAULT",
          "structure": "Alien",
          "textureSet": "Alien_DEFAULT",
          "icon": "DEFAULT.png"
        }
      ],
      "description": {
        "name": "Alien",
        "icon": "Alien_cls.png",
        "tags": [
          "alien",
          "space"
        ],
        "groupTags": [
          "characters"
        ],
        "themeTags": [
          "*outer space"
        ]
      },
      "provenance": {
        "aliceVersion": "3.4.0.0-alpha",
        "created": "2011",
        "creator": "Laura Paoletti"
      },
      "metadata": {
        "formatVersion": "0.1+alpha",
        "identifier": {
          "version": "1.0",
          "type": "Model"
        }
      },
      "prerequisites": [],
      "resources": [
        {
          "uuid": "ec707422-033d-4c74-99f2-f4ebeea77642",
          "height": 512.0,
          "width": 512.0,
          "name": "Alien_DEFAULT_texture_1_diffuseMap",
          "format": "image/png",
          "file": "Alien_DEFAULT_texture_1_diffuseMap.png",
          "type": "image"
        },
        {
          "uuid": "d1cb1b8c-e08f-4e9a-8f89-3b6847fc1dbb",
          "height": 120.0,
          "width": 43.0,
          "name": "DEFAULT.png",
          "format": "image/png",
          "file": "DEFAULT.png",
          "type": "image"
        },
        {
          "uuid": "463c8b29-2fad-4203-9abe-5b7acad26544",
          "height": 120.0,
          "width": 43.0,
          "name": "Alien_cls.png",
          "format": "image/png",
          "file": "Alien_cls.png",
          "type": "image"
        }
      ]
    }""";

  @Test
  public void aLibraryManifestShouldBeCreatedFromLibraryManifestJson() {
    LibraryManifest lib = ManifestEncoderDecoder.fromJson(SAMPLE_LIBRARY, LibraryManifest.class);

    assertNotNull("The encoder should have returned something.", lib);
  }

  @Test
  public void aLibraryManifestShouldHaveAProvenance() {
    LibraryManifest lib = ManifestEncoderDecoder.fromJson(SAMPLE_LIBRARY, LibraryManifest.class);

    assertNotNull("The manifest should have a provenance.", lib.provenance);
  }

  @Test
  public void aLibraryManifestProvenanceShouldHaveACreatedYear() {
    LibraryManifest lib = ManifestEncoderDecoder.fromJson(SAMPLE_LIBRARY, LibraryManifest.class);

    assertNotNull("The manifest's provenance should have a created year.", lib.provenance.created);
  }

  @Test
  public void aLibraryManifestProvenanceShouldHaveACreatedDate() {
    LibraryManifest lib = ManifestEncoderDecoder.fromJson(LIBRARY_WITH_DATE, LibraryManifest.class);

    assertNotNull("The manifest's provenance should have a created date.", lib.provenance.created);
  }

  @Test
  public void somethingShouldBeCreatedForSerializedLibraryManifest() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());

    assertNotNull("The encoder should have returned a String.", json);
  }

  @Test
  public void aJsonObjectShouldBeCreatedForSerializedLibraryManifest() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());

    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);

    assertNotNull("The decoder should have returned a manifest.", manifest);
  }

  @Test
  public void serializedLibraryManifestShouldHaveMetadata() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);

    assertNotNull("The json object should have metadata object.", manifest.metadata);
  }

  @Test
  public void serializedLibraryManifestsMetadataShouldHaveIdentifier() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);

    assertNotNull("The metadata object should have an identifier.", manifest.metadata.identifier);
  }

  @Test
  public void serializedLibraryManifestsMetadataIdentifierShouldHaveId() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);
    String id = manifest.metadata.identifier.name;

    assertEquals("The metadata identifier should have an name of 'testProject'.", "testProject", id);
  }

  @Test
  public void serializedLibraryManifestsDescriptionShouldHaveName() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);
    String name = manifest.description.name;

    assertEquals("The description should have a name of 'A test project'.", "A test project", name);
  }

  @Test
  public void serializedLibraryManifestShouldHaveProvenance() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);

    assertNotNull("The manifest should have a provenance.", manifest.provenance);
  }

  @Test
  public void serializedLibraryManifestShouldHaveProvenanceWithCreated() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);

    assertNotNull("The manifest's provenance should have a created.", manifest.provenance.created);
  }

  @Test
  public void serializedLibraryManifestProvenanceCreatedShouldBeReadAsTemporal() {
    String json = ManifestEncoderDecoder.toJson(getSimpleLibraryManifest());
    Manifest manifest = ManifestEncoderDecoder.fromJson(json, Manifest.class);

    assertNotNull("The manifest's provenance should have a parsed created.", manifest.provenance.created);
  }

  @Test
  public void aModelManifestShouldBeCreatedFromModelManifestJson() {
    ModelManifest model = ManifestEncoderDecoder.fromJson(SAMPLE_MODEL, ModelManifest.class);

    assertNotNull("The encoder should have returned something.", model);
  }

  @Test
  public void aModelManifestShouldBeCreatedFromModelWithResourcesManifestJson() {
    ModelManifest model = ManifestEncoderDecoder.fromJson(SAMPLE_MODEL_WITH_3_RESOURCES, ModelManifest.class);

    assertNotNull("The encoder should have returned something.", model);
  }

  @Test
  public void aResourcesShouldBeCreatedFromModelWithResourcesManifestJson() {
    ModelManifest model = ManifestEncoderDecoder.fromJson(SAMPLE_MODEL_WITH_3_RESOURCES, ModelManifest.class);

    assertNotNull("The model.resources should have been initialized to something.", model.resources);
  }

  @Test
  public void theRightNumberOfResourceShouldBeCreatedFromModelWithResourcesManifestJson() {
    ModelManifest model = ManifestEncoderDecoder.fromJson(SAMPLE_MODEL_WITH_3_RESOURCES, ModelManifest.class);

    assertEquals("The model.resources should have 3 resources in it.", 3, model.resources.size());
  }

  @Test
  public void invalidManifestJsonReturnsNullForLegacyDecoder() {
    Manifest manifest =
        ManifestEncoderDecoder.fromJson("{not-json", Manifest.class);

    assertNull(
        "Legacy manifest decoder should preserve null-on-error compatibility.",
        manifest);
  }

  @Test
  public void invalidManifestJsonThrowsForExplicitDecoder() {
    try {
      ManifestEncoderDecoder.fromJsonOrThrow("{not-json", Manifest.class);
      fail("Explicit manifest decoder should throw for invalid JSON.");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("Unable to read manifest"));
    }
  }

  @Test
  public void libraryManifestRoundTripPreservesMetadataAndTemporalValues() throws IOException {
    LibraryManifest original = getSimpleLibraryManifest();
    original.provenance.aliceVersion = "3.10.0.0";
    original.provenance.creator = "Regression Tester";
    original.provenance.created = YearMonth.of(2024, 5);

    Manifest.ProjectIdentifier prerequisite = new Manifest.ProjectIdentifier();
    prerequisite.name = "shared-library";
    prerequisite.type = Manifest.ProjectType.Library;
    prerequisite.version = "2.0";
    original.prerequisites.add(prerequisite);

    String json = ManifestEncoderDecoder.toJson(original);
    LibraryManifest decoded = ManifestEncoderDecoder.fromJsonOrThrow(json, LibraryManifest.class);

    assertEquals("testProject", decoded.metadata.identifier.name);
    assertEquals("0.1", decoded.metadata.identifier.version);
    assertEquals(Manifest.ProjectType.Library, decoded.metadata.identifier.type);
    assertEquals("A test project", decoded.description.name);
    assertEquals("Regression Tester", decoded.provenance.creator);
    assertEquals("3.10.0.0", decoded.provenance.aliceVersion);
    assertTrue(decoded.provenance.created instanceof YearMonth);
    assertEquals(YearMonth.of(2024, 5), decoded.provenance.created);
    assertEquals(1, decoded.prerequisites.size());
    assertEquals("shared-library", decoded.prerequisites.get(0).name);
    assertEquals("2.0", decoded.prerequisites.get(0).version);
  }

  @Test
  public void modelManifestRoundTripPreservesTypedResourcesAndStructure() throws IOException {
    ModelManifest original = ManifestEncoderDecoder.fromJsonOrThrow(SAMPLE_MODEL_WITH_3_RESOURCES, ModelManifest.class);

    String json = ManifestEncoderDecoder.toJson(original);
    ModelManifest decoded = ManifestEncoderDecoder.fromJsonOrThrow(json, ModelManifest.class);

    assertEquals("Alien", decoded.description.name);
    assertEquals(11, decoded.additionalJoints.size());
    assertEquals("LOWER_LIP", decoded.additionalJoints.get(0).name);
    assertEquals(Boolean.FALSE, decoded.placeOnGround);
    assertEquals(1, decoded.textureSets.size());
    assertEquals("Alien_DEFAULT", decoded.textureSets.get(0).name);
    assertEquals("Alien_DEFAULT_texture_1_diffuseMap", decoded.textureSets.get(0).idToResourceMap.get(1));
    assertEquals(1, decoded.models.size());
    assertEquals("DEFAULT", decoded.models.get(0).name);
    assertEquals(3, decoded.resources.size());
    assertTrue(decoded.resources.get(0) instanceof ImageReference);
    assertTrue(decoded.resources.get(1) instanceof ImageReference);
    assertTrue(decoded.resources.get(2) instanceof ImageReference);
    assertEquals("Alien_DEFAULT_texture_1_diffuseMap", decoded.resources.get(0).name);
  }

  @Test
  public void typeReferenceDependenciesRoundTripThroughJson() throws IOException {
    ModelManifest original = new ModelManifest();
    original.resources = new java.util.ArrayList<>();
    TypeReference typeReference = new TypeReference("Balloon", "src/Balloon.twe", "tweedle");
    typeReference.dependencies = java.util.Arrays.asList("Basket", "SBird");
    original.resources.add(typeReference);

    String json = ManifestEncoderDecoder.toJson(original);
    ModelManifest decoded = ManifestEncoderDecoder.fromJsonOrThrow(json, ModelManifest.class);

    assertEquals(1, decoded.resources.size());
    assertTrue(decoded.resources.get(0) instanceof TypeReference);
    TypeReference decodedReference = (TypeReference) decoded.resources.get(0);
    assertEquals(java.util.Arrays.asList("Basket", "SBird"), decodedReference.dependencies);
  }

  @Test
  public void typeReferenceWithoutDependenciesOmitsTheFieldFromJson() {
    ModelManifest original = new ModelManifest();
    original.resources = new java.util.ArrayList<>();
    original.resources.add(new TypeReference("Balloon", "src/Balloon.twe", "tweedle"));

    String json = ManifestEncoderDecoder.toJson(original);

    assertFalse(
        "A dependency-free type reference must not emit a dependencies field, so "
            + "legacy archives and dependency-free types stay byte-identical.",
        json.contains("dependencies"));
  }

  private LibraryManifest getSimpleLibraryManifest() {
    LibraryManifest lib = new LibraryManifest();
    lib.metadata = new Manifest.MetaData();
    final Manifest.ProjectIdentifier projectIdentifier = new Manifest.ProjectIdentifier();
    projectIdentifier.name = "testProject";
    projectIdentifier.version = "0.1";
    projectIdentifier.type = Manifest.ProjectType.Library;
    lib.metadata.identifier = projectIdentifier;
    lib.metadata.formatVersion = "0.3";

    final Manifest.Description desc = new Manifest.Description();
    desc.name = "A test project";
    lib.description = desc;
    return lib;
  }
}
