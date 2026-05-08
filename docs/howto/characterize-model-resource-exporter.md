# Characterize ModelResourceExporter behavior

Use this guide when adding or reviewing behavior-backed coverage for
`ModelResourceExporter` before touching the protected model-loading hotspot. The
goal is to preserve downstream-visible export behavior, not to pad coverage.

For the API and output contract, see the
[Model resource exporter reference](../reference/model-resource-exporter.md).

## When to use this guide

Use this guide for focused `core/model-loading` work that exercises
`org.lgna.story.resourceutilities.ModelResourceExporter` through
`ModelExportTest`.

Do not use this lane for:

- decoder behavior;
- project Save or Save As behavior;
- Select Project behavior;
- broad formatting, broad renames, framework swaps, or unrelated cleanup;
- claims that the repository has reached a 70 percent coverage target.

## 1. Choose one downstream-visible behavior

Pick one behavior that generated Alice resources depend on. Prefer output that a
future refactor could accidentally break:

| Behavior surface | Useful characterization |
| --- | --- |
| XML metadata | Assert root attributes, resource attributes, tag nesting, and bounding boxes. |
| Generated Java | Assert package, enum declaration, constants, annotations, constructors, and compilation. |
| Thumbnail output | Assert saved file paths, class thumbnail creation, and checked failure reporting. |
| Protected hotspot metadata | Assert that one exporter flag updates every generated surface that consumes it. |

Keep the test narrow. A good characterization fails when compatibility breaks
and stays stable across harmless internal extractions.

## 2. Use the existing exporter fixture

Use the same package as the exporter so tests can exercise same-package
generation seams without opening new public API:

```java
package org.lgna.story.resourceutilities;

ModelResourceExporter exporter =
    new ModelResourceExporter("TestProp", ModelClassData.PROP_CLASS_DATA);
exporter.addAttribution("Alice Test", "2026");
exporter.addResource("TestProp", "Default", "ALICE", "Resource Artist", "2025");
```

Add only the setup needed for the selected behavior. Do not introduce binary
fixtures, network access, Git LFS dependencies, or privileged filesystem
assumptions.

## 3. Example: characterize deprecated metadata

Deprecated resource metadata is a protected behavior because one exporter flag
drives both XML metadata and generated Java source. The characterization sets the
flag once, then checks both generated outputs:

```java
ModelResourceExporter exporter = createSyntheticPropExporter();
exporter.setIsDeprecated(true);

Document xml = parseXml(exporter.createXMLString());
String javaCode = exporter.createJavaCode();

assertEquals("TRUE", xml.getDocumentElement().getAttribute("deprecated"));
assertTrue(javaCode.contains("@Deprecated"));
assertTrue(javaCode.contains(
    "public enum TestPropResource implements org.lgna.story.resources.PropResource"));
assertCompiles("org/lgna/story/resources/prop/TestPropResource.java", javaCode);
```

This is behavior-backed because it protects two generated artifacts consumed by
resource loading and compilation. It does not depend on private helper names or
line-level implementation trivia.

## 4. Validate the focused model-loading scope

Initialize the Tweedle grammar submodule, then run the focused model export test
with the configured heap setting for Node-backed tooling:

```sh
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ModelExportTest \
  test
```

Run broader module or checkstyle validation only when the change also touches
production code or style-sensitive files.

## 5. Keep review proof outside repository docs

Repository documentation describes the stable workflow and behavior contract. It
is not the required proof artifact for a characterization change.

When the review workflow asks for proof, store it outside the repository in the
session artifact directory assigned by that workflow. Keep it concise and
include:

- changed-file summary;
- why the characterization is behavior-backed;
- confirmation that production code stayed unchanged, or the exact tiny
  behavior-preserving extraction if one was necessary;
- validation command;
- short validation result excerpt.

Do not commit proof files to `docs/`, and do not copy generated binaries,
complete generated resources, secrets, user data, or large logs into the proof
artifact.
