# Tutorial: Trace the Legacy Program JSON Archive Boundary

This tutorial walks through the fail-closed JSON archive boundary for a
`.a3w` archive that advertises `Program`, fails supported decoding, and does not
satisfy the narrow one-image legacy resource recovery predicate.

**Implementation status:** implemented in `JsonProjectIo.Reader.readProject`.

The goal is to prove this behavior:

```text
A JSON .a3w archive whose manifest names Program fails closed with IOException
after supported Program decoding fails and the archive does not match the narrow
legacy image-resource recovery predicate.
```

## 1. Start with the manifest name

The boundary starts at the manifest, not at the player runtime:

```java
ProjectManifest manifest = new ProjectManifest();
manifest.description.name = "Program";
manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
```

The name `Program` is not inherently unsupported. It becomes a candidate
legacy/player signal only after the manifest-named program type fails supported
decoding.

## 2. Add the manifest-declared type

Reference `Program` as Tweedle source:

```java
TypeReference programReference =
    new TypeReference("Program", "src/Program.twe", "tweedle");
manifest.resources.add(programReference);
```

The archive should contain:

```text
version.txt
manifest.json
src/Program.twe
```

## 3. Use unsupported Program source

Use source that cannot produce a supported program type:

```java
class Program extends MissingSuper {
}
```

The exact unsupported Tweedle reason is not the feature. The feature is the
archive-level decision: after `Program` fails supported decoding, this archive
must not be accepted as a partial project unless it satisfies the explicit
one-image resource recovery rule.

## 4. Avoid the recoverable legacy image case

Do not include exactly one recoverable image resource in this negative fixture.
That one-image shape is the existing compatibility recovery path.

For the fail-closed tutorial, use no resources or use a non-image resource shape.
The reader then cannot decode the manifest program type or satisfy the narrow
one-image resource recovery predicate.

## 5. Read through IoUtilities

Read through the public production API:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(archiveFile));
```

The archive reader fails closed because the manifest names `Program`, supported
decoding fails, and the archive does not match the narrow one-image resource
recovery predicate.

## 6. Check the diagnostic

Assert stable message fragments:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Unsupported legacy JSON project archive"));
assertTrue(message.contains("Program"));
```

Those assertions prove that the public read seam rejects the unsupported legacy
archive explicitly instead of returning `Project` with a `null` program type.

## 7. Keep the claim narrow

Do not add tutorial text or tests that imply support for:

- full player archive loading;
- player runtime behavior;
- broad legacy `.a3w` conversion;
- general Tweedle decode;
- general resource recovery;
- `.a3c` type archive changes.

The finished behavior is a precise fail-closed boundary in the JSON archive
reader. Supported JSON program reads still return projects, and the explicitly
characterized one-image legacy recovery path remains the only resource-only
legacy recovery case.

## See also

- [Legacy Program JSON Archive Boundary](../reference/legacy-program-json-archive-boundary.md)
- [Player archive unsupported Tweedle diagnostics](../reference/player-archive-unsupported-tweedle-diagnostics.md)
