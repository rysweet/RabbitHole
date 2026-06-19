---
title: Use Open 3D Assets
description: Explains RabbitHole's default OSS/open 3D asset path, optional Sims opt-in, and how to create, use, and test open assets.
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: howto
---

# Use open 3D assets

RabbitHole defaults to the redistributable open 3D asset path. A fresh Maven
build, test run, package, or IDE launch uses the open `gallery/assets/alice`
resource distribution and does not require Sims/nonfree modules.

Use this guide when you need to create an open asset, verify the default runtime
path, or intentionally opt into legacy Sims assets for compatibility testing.

## Default asset path

The default Maven profile set excludes `core-nonfree` modules. These commands use
open assets without extra asset flags:

```bash
mvn -Dinstall4j.skip clean install
cd alice-ide
mvn exec:java -Dalice-ide
```

The NetBeans library definition packaged by default uses `org-alice/*.jar`
classpath entries and omits `org-alice-nonfree/*.jar`. The resource distribution
keeps open assets under:

```text
core/resources/src/application/resources/gallery/assets/alice/
```

Do not copy Sims or other proprietary gallery files into this tree.

## Opt into legacy Sims assets

Sims assets remain available for compatibility work, but they are no longer the
default. Opt in explicitly for every Maven command that needs the nonfree modules:

```bash
mvn -DincludeSims=true -Dinstall4j.skip clean install
cd alice-ide
mvn -DincludeSims=true exec:java -Dalice-ide
```

The `includeSims` profile activates only when the property is exactly `true`.
Unset `includeSims` means open assets.

## Create an open asset

Prefer assets with redistribution-friendly licenses, especially CC0. Keep the
source URL, author, license, and attribution text with the asset or in the
metadata generated for it.

For authored models, follow the Alice web prototype pipeline conventions:

| Area | Rule |
| --- | --- |
| Format | Prefer glTF/GLB for interchange; use normalized COLLADA for the current Java import proof path. |
| Joints | Provide a stable `ROOT` or `root` joint and map source bones to Alice-friendly names. |
| Scale | Normalize to Alice-sized meter units before export. |
| Orientation | Preserve explicit up-axis or forward-direction metadata and verify the imported visual. |
| License | Record SPDX ID, source URL, author, and attribution requirements. |

The web prototype uses procedural profiles, glTF/GLB import, and Blender export
helpers as the reference design. RabbitHole's Java path uses the checked-in
`OpenAssetImportPipeline` and existing COLLADA/model resource utilities; do not
copy TypeScript runtime code into RabbitHole.

## Import and use an asset in Java

Use the Java facade to prove one normalized COLLADA asset can become Alice
scenegraph data and proof outputs:

```bash
mvn -pl core/model-loading -Dtest=OpenAssetImportPipelineTest test
```

See [Import an Open 3D Asset](./import-open-3d-asset.md) for the full example.
The proof path writes `.glb`, `.a3r`, and optional `.a3t` outputs under `target/`
and returns `AliceModelImportData` for assertions or inspection.

When an asset graduates from import proof to gallery integration, keep generated
or reviewed resource files under the open `gallery/assets/alice` tree and add
tests that prove the default distribution still contains open assets and omits
Sims assets.

## Test the default and optional paths

Run the default open-asset validation lane:

```bash
git submodule update --init tweedle-lang
mvn -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean install
```

Run the focused NetBeans packaging/default-path tests:

```bash
mvn -pl netbeans -am -Dinstall4j.skip -Dcheckstyle.skip \
  -Dtest=Alice3LibraryRegistrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  clean test
```

Run optional Sims compatibility tests only when nonfree modules are available:

```bash
mvn -DincludeSims=true -pl netbeans -am -Dinstall4j.skip -Dcheckstyle.skip \
  -Dtest=Alice3LibraryRegistrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  clean test
```

Expected default-path assertions:

| Assertion | Why it matters |
| --- | --- |
| `includeSims` profiles require `<value>true</value>` | Proves Sims assets are explicit opt-in. |
| Default classpath omits `org-alice-nonfree` | Proves a fresh run does not require Sims jars. |
| Default resource distribution includes `gallery/assets/alice` | Proves open assets are packaged. |
| Default resource distribution omits `gallery/assets/sims` and the Sims EULA | Proves proprietary assets are not selected accidentally. |
| `-DincludeSims=true` classpath includes Sims-only entries | Proves legacy compatibility remains reachable. |

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Maven tries to resolve `org.alice.nonfree:*` unexpectedly | Remove `-DincludeSims=true` from the command unless you are intentionally testing Sims assets. |
| NetBeans tests cannot find `target/distribution/application` | Run with `-pl netbeans -am` so `core/resources` is built before NetBeans tests. |
| Tweedle parser classes are missing | Run `git submodule update --init tweedle-lang` and confirm `tweedle-lang/Grammar` exists. |
| Imported model has no geometry | Re-export the asset with triangulated mesh data and run the focused model-loading import test. |
