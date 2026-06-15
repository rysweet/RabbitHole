# RabbitHole to Alice.org release management checklist

Use this checklist when preparing a public Alice 3 release from RabbitHole for
Alice.org. It covers the release surfaces that must agree before a new build is
announced: RabbitHole source, installer artifacts, GitHub release hosting, and
the Alice.org download pages.

## Current public release surfaces

- RabbitHole is the modernization source repository used for current release
  preparation work.
- Alice.org is a WordPress-backed site. The live Alice 3 download page uses the
  `Alice` WordPress theme and exposes WordPress REST endpoints.
- The Alice 3 page links current installer downloads to GitHub release assets on
  `TheAliceProject/alice3`.
- Older Alice 3 downloads are still linked from Alice.org WordPress uploads.
- RabbitHole builds installers with Maven and Install4J through the
  `buildInstaller` profile.

Confirm private Alice.org repository or deployment access before treating this
as an executable runbook. The public GitHub repositories do not expose a clearly
named Alice.org website source repository.

## 1. Release decision and version freeze

- Choose the release version and whether it is prerelease or final.
- Update Maven and Alice build metadata consistently:
  - project version, when the Maven artifact version changes
  - `alice.build.version`
  - `alice.build.prerelease`
  - build metadata injected by CI or the release builder
- Freeze the release branch or release candidate commit.
- Write release notes with user-visible changes, compatibility notes, and known
  limitations.
- Confirm the asset and license position for every bundle, especially Sims/EA
  gallery assets.

## 2. Source readiness gate

- Start from a clean checkout of the approved RabbitHole commit.
- Initialize required submodules:

```bash
git submodule update --init tweedle-lang
```

- Pull Git LFS assets before packaging.
- Verify no release-blocking issues remain open.
- Run the maintained validation lanes:
  - Maven build and tests
  - Checkstyle
  - coverage
  - NetBeans package
  - Getting Started headless validation
  - headed GUI validation under Xvfb
  - documentation validation when docs changed

Do not continue to artifact publication until the release candidate is clean.

## 3. Installer and package production

Install4J is required for installer builds. Build release artifacts through the
installer profile:

```bash
mvn -DbuildInstaller=true clean package
```

Expected release outputs include:

- Windows x64 installer, `.exe`
- Windows x64 bundle, `.zip`
- macOS installer or bundle, `.dmg`
- Linux packages, `.deb` and `.rpm`
- Unix bundle, `.tar.gz`
- Unix installer, `.sh`
- NetBeans plugin, `.nbm`

For each artifact:

- verify the file name includes the intended version and build metadata
- verify the bundled application reports the intended version
- verify install, launch, save/open, and uninstall behavior on the target
  platform
- generate a checksum
- retain the builder logs outside Git as release evidence

## 4. Signing, notarization, and trust checks

- Sign the Windows installer and verify the signature.
- Sign and notarize macOS artifacts, staple the notarization ticket, and verify
  Gatekeeper behavior on a clean machine.
- Verify Linux package metadata and install/remove behavior.
- Run malware and security scans required by the release owner.
- Keep signing credentials, notarization credentials, and installer license keys
  outside Git and out of logs.

## 5. GitHub release publication

Choose the public hosting model before publishing:

| Hosting model | Required action |
| --- | --- |
| Alice.org continues to point at `TheAliceProject/alice3` | Mirror or publish RabbitHole-built assets to a `TheAliceProject/alice3` release. |
| Alice.org points at RabbitHole releases | Update Alice.org links and confirm project ownership, branding, and support expectations. |
| Alice.org hosts assets directly | Upload artifacts to WordPress or the site deployment storage and update links. |

For the chosen GitHub release repository:

- create an annotated release tag from the approved commit or mirrored source
- draft the GitHub release
- upload every artifact and checksum
- include release notes
- set prerelease/final status correctly
- download each uploaded artifact and verify its checksum

Do not open issues or pull requests against `TheAliceProject/alice3` for
RabbitHole modernization tracking.

## 6. Alice.org update

Update the Alice 3 download page after the release assets are available:

- primary Windows download link
- primary macOS download link
- primary Linux download link
- all-releases link
- version text or badges
- release notes link
- installation help links, if changed
- NetBeans plugin or Alice 3 with NetBeans page, if changed
- Alice 3 Player page, if compatibility changed

Preserve the EULA and Alice 3 Art Gallery License text unless release owners
explicitly approve legal copy changes.

## 7. Website deployment verification

- Preview or stage the Alice.org page update before publication.
- Trigger the WordPress or static-site deployment path.
- Invalidate caches or CDN entries when needed.
- Verify from a clean browser session:
  - Alice 3 page loads
  - each primary download link resolves
  - downloaded file names match the release
  - GitHub release and all-releases links resolve
  - EULA and resources links still resolve
  - mobile layout remains usable

## 8. Post-release validation

- Install the public artifacts on clean Windows, macOS, and Linux machines.
- Launch Alice and create, save, reopen, and run a project.
- Validate gallery resources and example projects.
- Validate NetBeans plugin installation when published.
- Confirm Java requirement messaging is still accurate.
- Monitor GitHub release downloads, Alice.org analytics, issue reports, and
  teacher/community feedback.

## 9. Rollback plan

- Keep prior Alice.org links and prior GitHub release assets available.
- Prepare an Alice.org page revert before publishing.
- If an artifact is bad, mark the release as superseded and update Alice.org
  links back to the previous known-good version.
- Publish a replacement release only after the replacement artifacts pass the
  same checks.
- Document the rollback or replacement in release notes or the release issue.

## 10. Automation gaps

Before the release process is comfortable, add automation for:

- reproducible multi-platform Install4J builds
- signing and notarization
- checksum generation
- GitHub release upload
- Alice.org link validation
- artifact install/launch checks
- release evidence generation

The key unresolved ownership decision is whether Alice.org should keep linking
to `TheAliceProject/alice3` release assets, link directly to RabbitHole release
assets, or host artifacts through Alice.org-managed storage.
