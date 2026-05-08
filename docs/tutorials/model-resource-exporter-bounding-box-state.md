# Tutorial: Characterize ModelResourceExporter Bounding-Box State

This tutorial walks through the focused model-loading characterization for
stateful XML bounding-box generation.

## Goal

Protect this model export behavior:

```text
ModelResourceExporter XML generation may populate a missing class bounding box
and may update matching non-class subresource bounding-box state. The mutation is
part of the exporter contract and must be explicitly asserted when the XML
generator is changed.
```

The test uses synthetic model names and in-memory `AxisAlignedBox` values. It
does not require Alice gallery assets, Git LFS files, or any workflow outside
`core/model-loading`.

## 1. Use the model-loading exporter test package

Add or update focused coverage in:

```text
core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java
```

Use the same package as the exporter:

```java
package org.lgna.story.resourceutilities;
```

Keeping the test in this package lets it observe same-package exporter behavior
without adding new public API.

## 2. Build the smallest stateful fixture

Create an exporter with one non-class subresource and a bounding box registered
for that subresource model name. Keep this fixture limited to the boxes that
should contribute to the class-level union:

```java
ModelResourceExporter exporter =
    new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
exporter.addResource("VariantProp", "Default", "ALICE", null, null);

AxisAlignedBox variantBox =
    AxisAlignedBox.createAxisAlignedBox(-0.5, 0.0, -0.5, 0.5, 1.0, 0.5);
exporter.setBoundingBox("VariantProp", variantBox);
ModelSubResourceExporter subResource = exporter.getSubResources().get(0);
```

Do not set a class bounding box. The missing class box is what causes XML
generation to compute and record the class-level value.

## 3. Assert the pre-generation state

Before generating XML, assert that the state is incomplete:

```java
assertNull(exporter.getBoundingBox("TestProp"));
assertNull(subResource.getBbox());
```

These assertions document that the later values are produced by XML generation,
not by fixture setup.

## 4. Generate XML through the exporter

Call the exporter XML boundary:

```java
assertNotNull(exporter.createXMLString());
```

`createXMLFile(...)` follows the same XML-generation contract only when it
generates fresh XML before writing to the package resource path. If it copies an
existing `xmlFile` with `forceRebuild=false`, it does not call XML generation.
Use `createXMLString()` for this characterization so the test stays in memory
and focused on exporter state.

## 5. Assert the post-generation mutation

After XML generation, assert the intended exporter state:

```java
assertEquals(variantBox, exporter.getBoundingBox("TestProp"));
assertEquals(variantBox, subResource.getBbox());
```

For multiple registrations, the expected class box is the union of the
exporter's registered bounding-box values, not a filtered pass over only the
current subresource list. For a single registered subresource box, the union
equals that box.

## 6. Keep the implementation contract explicit

The production implementation should express state changes through exporter and
subresource methods, such as `setBoundingBox(...)` and `setBbox(...)`. The
subresource update is not limited to missing local values: a non-class
subresource with a matching exporter box receives that exporter box before its
`Resource` element is emitted. Avoid patterns that make mutation look
accidental, such as assigning the exporter's live bounding-box map to a local
variable and mutating the map through that alias.

If XML generation is intentionally changed to become read-only later, update this
characterization and the reference documentation in the same change. Do not let a
read-only design happen as a side effect of refactoring.

## 7. Run focused validation

Initialize the Tweedle grammar submodule in fresh checkouts, then run the focused
model export test:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

This validation lane belongs to `core/model-loading` and does not require any
workflow outside the model export hotspot.
