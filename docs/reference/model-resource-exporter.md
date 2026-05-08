# Model resource exporter reference

`ModelResourceExporter` is the supported boundary for Alice model resource
metadata, generated Java resource enums, XML resource descriptions, and
thumbnail registration/output. The exporter lives in
`org.lgna.story.resourceutilities` and is a contributor-facing API for
model-loading and resource-generation code, not a public Alice user API.

The current exporter is split into small package-private generators for Java,
XML, and thumbnail output. Those helpers are implementation details. The
supported contract remains the behavior exposed through `ModelResourceExporter`
and protected by `ModelExportTest`.

## Basic usage

Create an exporter with the model class name and the Alice resource class data,
then add attribution, tags, bounds, and one or more subresources:

```java
package org.lgna.story.resourceutilities;

import org.alice.math.immutable.AxisAlignedBox;

public final class ResourceExportExample {
  public String createPropXml() {
    ModelResourceExporter exporter =
        new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);

    exporter.addAttribution("Alice Test", "2026");
    exporter.setIsDeprecated(true);
    exporter.setPlaceOnGround(true);
    exporter.addTags("class-tag");
    exporter.addGroupTags("class-group");
    exporter.addThemeTags("class-theme");
    exporter.setBoundingBox(
        "TestProp",
        AxisAlignedBox.createAxisAlignedBox(-1.0, 0.0, -2.0, 1.0, 3.0, 2.0));
    exporter.addResource("TestProp", "Default", "ALICE", "Resource Artist", "2025");

    return exporter.createXMLString();
  }
}
```

The generated XML root is an `AliceModel` element with class-level attributes
and one `Resource` element for each subresource. A subresource with the same
model name as the parent and the default texture produces the `DEFAULT`
resource enum name.

## Exporter API

The protected contract centers on these public methods and same-package
generation seams. Other legacy setters exist for pipeline-specific behavior, but
callers should stay on `ModelResourceExporter` rather than depending directly on
the helper classes.

| API | Visibility | Purpose |
| --- | --- | --- |
| `ModelResourceExporter` constructors | Public | Create an exporter for a resource class such as a prop, biped, flyer, or other model class, with overloads for resource name, class data, and joint/visual factory data. |
| `addAttribution(String name, String year)` | Public | Adds class-level creator and creation-year metadata. |
| `setIsDeprecated(boolean isDeprecated)` | Public | Marks the exported resource class as deprecated in generated XML and Java output when enabled. |
| `setPlaceOnGround(boolean placeOnGround)` | Public | Writes `placeOnGround="TRUE"` on the XML root when enabled. |
| `setBoundingBox(String modelName, AxisAlignedBox boundingBox)` | Public | Records bounds for the class or a named subresource. |
| `addTags`, `addGroupTags`, `addThemeTags` | Public | Adds class-level gallery tags, group tags, and theme tags. |
| `addResource(String modelName, String textureName, String resourceType, String attributionName, String attributionYear)` | Public | Adds a subresource and optional subresource-level attribution. |
| `addSubResourceTags`, `addSubResourceGroupTags`, `addSubResourceThemeTags` | Public | Adds tags to matching subresources by model name and optional texture name. |
| `addForcedEnumNames(String resourceName, List<String> enumNames)` | Public | Restricts generated enum constants globally when `resourceName` is `null`, or for one model name when provided. |
| `createResourceEnumName(String modelName, String textureName)` | Public | Calculates the enum constant name for a model and texture pair. |
| `createJavaCode()` | Public | Creates the Java enum source for the resource class. |
| `getThumbnailPath(String rootPath, String thumbnailName)` | Public | Returns the output path for a thumbnail under the package resource directory. |
| `createClassThumb(BufferedImage imgSrc)` | Public static | Creates the class thumbnail image from a source thumbnail. |
| `addExistingThumbnail(String name, File thumbnailFile)` | Public | Registers an already-written thumbnail if the file exists at registration time. |
| `createXMLString()` | Package-private | Creates the XML resource description as a string for same-package generators and tests. |
| `createXMLFile(String root, boolean forceRebuild)` | Package-private | Writes the XML resource description into the package resource directory and surfaces output failures as `IOException`. |
| `saveThumbnailsToDir(String root)` | Package-private | Reuses registered thumbnails, writes generated thumbnails when present, creates the class thumbnail from the first subresource thumbnail, and returns the files it saved or reused. |

## XML contract

The XML generator writes one `AliceModel` root:

```xml
<AliceModel name="TestProp" creator="Alice Test" creationYear="2026" deprecated="TRUE" placeOnGround="TRUE">
    <BoundingBox>
        <Min x="-1.0" y="0.0" z="-2.0"/>
        <Max x="1.0" y="3.0" z="2.0"/>
    </BoundingBox>
    <Tags>
        <Tag>class-tag</Tag>
    </Tags>
    <GroupTags>
        <GroupTag>class-group</GroupTag>
    </GroupTags>
    <ThemeTags>
        <ThemeTag>class-theme</ThemeTag>
    </ThemeTags>
    <Resource textureName="DEFAULT" resourceName="DEFAULT" modelName="TestProp" creator="Resource Artist" creationYear="2025"/>
</AliceModel>
```

Class-level tags are written once on the root. Subresource tags are written only
when they are unique from the class-level tags. For example, if the class has
`shared-tag` and the `VariantProp` subresource has both `shared-tag` and
`variant-tag`, only `variant-tag` is emitted under that subresource.

When `setIsDeprecated(true)` is applied, the XML root includes
`deprecated="TRUE"`. The flag is class-level metadata; it does not add
subresource attributes or change resource enum names.

Bounding boxes are class-scoped by model name. If the class itself has no
bounding box, the XML generator computes the class box as the union of the
registered subresource boxes.

## Generated Java contract

`createJavaCode()` produces a resource enum named
`<ClassName>Resource` in the package from `ModelClassData`. For a prop named
`TestProp`, the generated source begins with:

```java
package org.lgna.story.resources.prop;

import org.lgna.project.annotations.*;
import org.lgna.story.implementation.JointIdTransformationPair;
import org.lgna.story.Orientation;
import org.lgna.story.Position;
import org.lgna.story.resources.ImplementationAndVisualType;

@Deprecated
public enum TestPropResource implements org.lgna.story.resources.PropResource {
    DEFAULT;
```

Resource enum constants follow these rules:

| Input | Generated constant |
| --- | --- |
| Parent model name with default texture | `DEFAULT` |
| Different model name with default texture | Model enum name, such as `VARIANT_PROP` |
| Different model name and distinct texture | Model enum name plus texture enum name, such as `VARIANT_PROP_BLUE` |
| Resource type `ALICE` | No constructor argument; the default constructor uses `ImplementationAndVisualType.ALICE`. |
| Non-`ALICE` resource type | Constructor argument such as `( ImplementationAndVisualType.SIMS2 )`. |
| Forced enum names | Only matching constants are emitted, and the enum remains valid Java with no trailing comma. |

The generated enum includes `getImplementationAndVisualFactory()` and
`createImplementation(...)` methods so the resource can create the matching
Alice implementation class. Joint and pose declarations are generated from the
exporter's joint map and pose data when those inputs are present.

Deprecated resources add `@Deprecated` directly to the generated enum
declaration. The annotation is emitted from the same exporter flag that writes
the XML `deprecated="TRUE"` attribute, so XML metadata and generated Java source
stay in sync for downstream resource consumers.

## Thumbnail behavior

Existing thumbnail files can be registered with `addExistingThumbnail`. The
method registers only files that exist at registration time; a missing file is
reported to standard error and is not added to the thumbnail map. Later,
`saveThumbnailsToDir` reuses registered thumbnails, writes generated thumbnail
images when present, creates the class thumbnail from the first subresource
thumbnail path, and returns the class thumbnail plus each registered or generated
thumbnail file.

Thumbnail failures are not ignored:

| Condition | Result |
| --- | --- |
| No subresources were registered | `IOException` explains that thumbnails cannot be created. |
| Registered thumbnail path disappears before save | `FileNotFoundException` identifies the missing file. |
| Generated image is `null` or has invalid dimensions | `IOException` identifies the thumbnail that cannot be written. |
| First subresource thumbnail is unreadable | `IOException` identifies the class thumbnail and source thumbnail. |

The exporter preserves the bad source thumbnail when thumbnail reading fails so
the caller can diagnose the original file.

## Protected hotspot contract

The exporter is a protected hotspot: it may be refactored only when behavior is
already characterized and the refactor is limited to extraction, naming, or
small flow simplification.

Behavior-preserving refactors must keep these outputs stable:

| Surface | Required compatibility |
| --- | --- |
| XML shape | Root attributes, `Resource` attributes, tag nesting, unique subresource tags, and bounding-box values remain compatible. |
| Java source shape | Package, enum name, constants, resource type constructor arguments, factory methods, joint declarations, and pose declarations remain compatible. |
| Deprecated metadata | `setIsDeprecated(true)` continues to write XML `deprecated="TRUE"` and a generated Java `@Deprecated` enum annotation together. |
| File paths | XML and thumbnail output paths continue to use the package directory and resource subdirectory conventions. |
| Error handling | Output and thumbnail failures surface as checked `IOException` where the exporter API declares them. |
| Dependencies | Export behavior does not require Git LFS checkout changes, new CI dependencies, or artificial coverage exclusions. |

`ModelResourceJavaGenerator`, `ModelResourceXmlGenerator`, and
`ModelResourceThumbnailWriter` are package-private helpers. Callers use
`ModelResourceExporter`; they do not depend on the helper classes directly.

## Validation

Run the focused exporter characterization test after changing the exporter or
one of its helper generators:

```sh
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

`ModelExportTest` includes behavior-backed characterization for XML output,
generated Java output, thumbnail handling, and deprecated metadata. In
particular, the deprecated metadata characterization verifies that
`ModelResourceExporter` keeps generated XML and generated Java aligned without
requiring decoder, project Save, or Select Project coverage.

When a production refactor changes code style-sensitive files, run the
repository's relevant checkstyle or module validation gate in addition to the
focused characterization test.
