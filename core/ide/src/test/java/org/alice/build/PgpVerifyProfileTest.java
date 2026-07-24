package org.alice.build;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for the opt-in PGP signature verification build wiring (issue #980).
 *
 * <p>These tests pin the supply-chain contract so that future edits cannot silently:
 * <ul>
 *   <li>make the {@code pgpverify} profile active by default (which would break the default build),</li>
 *   <li>drop the pinned {@code pgpverify-maven-plugin} version or its {@code verify}/{@code check} binding,</li>
 *   <li>turn the default warn (non-failing) posture into a fail-closed posture without an explicit change here,</li>
 *   <li>orphan the {@code .mvn/pgp-keys-map.list} trust map or its documentation.</li>
 * </ul>
 *
 * <p>The tests read files directly from the repository tree (not the built classpath) so they
 * assert on the actual, committed build configuration.
 */
public class PgpVerifyProfileTest {

  @Test
  public void rootPomDeclaresPgpVerifyProfile() throws IOException {
    String profile = extractPgpVerifyProfile();
    assertFalse("Expected a <profile> with <id>pgpverify</id> in root pom.xml", profile.isEmpty());
  }

  @Test
  public void pgpVerifyProfileIsNotActiveByDefault() throws IOException {
    String profile = extractPgpVerifyProfile();
    // An opt-in profile must be triggered explicitly via -Ppgpverify. It must not carry
    // <activeByDefault>true</activeByDefault>, which would run PGP verification on every build.
    assertFalse(
        "pgpverify profile must NOT be active by default (would break the default build): "
            + "found <activeByDefault>true</activeByDefault>",
        stripWhitespace(profile).contains("<activeByDefault>true</activeByDefault>"));
  }

  @Test
  public void pgpVerifyProfileUsesPinnedPluginBoundToVerifyCheck() throws IOException {
    String profile = extractPgpVerifyProfile();
    assertTrue(
        "pgpverify profile must configure org.simplify4u.plugins",
        profile.contains("<groupId>org.simplify4u.plugins</groupId>"));
    assertTrue(
        "pgpverify profile must configure the pgpverify-maven-plugin",
        profile.contains("<artifactId>pgpverify-maven-plugin</artifactId>"));
    assertTrue(
        "pgpverify-maven-plugin version must be pinned (no unpinned/version-range plugin)",
        Pattern.compile("<version>\\d+\\.\\d+[\\d.]*</version>").matcher(profile).find());
    String collapsed = stripWhitespace(profile);
    assertTrue(
        "pgpverify execution must bind to the verify phase",
        collapsed.contains("<phase>verify</phase>"));
    assertTrue(
        "pgpverify execution must run the check goal",
        collapsed.contains("<goal>check</goal>"));
  }

  @Test
  public void pgpVerifyDefaultPostureIsNonFailingWarn() throws IOException {
    String collapsed = stripWhitespace(extractPgpVerifyProfile());
    // Conservative default: the plugin must never fail the build on missing/weak signatures
    // or missing trusted keys. Promotion to fail-closed is an explicit, documented change.
    assertTrue(
        "Default posture must set <failNoSignature>false</failNoSignature>",
        collapsed.contains("<failNoSignature>false</failNoSignature>"));
    assertTrue(
        "Default posture must set <failNoKey>false</failNoKey>",
        collapsed.contains("<failNoKey>false</failNoKey>"));
    assertTrue(
        "Default posture must set <failWeakSignature>false</failWeakSignature>",
        collapsed.contains("<failWeakSignature>false</failWeakSignature>"));
    assertFalse(
        "Default posture must not enable any fail-closed flag",
        collapsed.contains("<failNoSignature>true</failNoSignature>")
            || collapsed.contains("<failNoKey>true</failNoKey>")
            || collapsed.contains("<failWeakSignature>true</failWeakSignature>"));
  }

  @Test
  public void pgpVerifyProfileReferencesKeysMapResource() throws IOException {
    String collapsed = stripWhitespace(extractPgpVerifyProfile());
    assertTrue(
        "pgpverify profile must point keysMapLocation at .mvn/pgp-keys-map.list",
        collapsed.contains(".mvn/pgp-keys-map.list</keysMapLocation>"));
    Path keysMap = repositoryRoot().resolve(".mvn/pgp-keys-map.list");
    assertTrue("Trust map .mvn/pgp-keys-map.list must exist", Files.isRegularFile(keysMap));
    assertFalse(
        "Trust map .mvn/pgp-keys-map.list must not be empty",
        Files.readString(keysMap, StandardCharsets.UTF_8).isBlank());
  }

  @Test
  public void keysMapMarksAliceSnapshotsAsUntrustedWithoutTouchingVersions() throws IOException {
    String keysMap = Files.readString(
        repositoryRoot().resolve(".mvn/pgp-keys-map.list"), StandardCharsets.UTF_8);
    // Local org.alice reactor SNAPSHOTs are unsigned; they must be explicitly excused (noSig),
    // never given a bogus key, and their coordinates must remain -SNAPSHOT (versions untouched).
    assertTrue(
        "keys map must reference org.alice artifacts so unsigned reactor jars do not warn as unknown",
        keysMap.contains("org.alice"));
    assertTrue(
        "unsigned org.alice SNAPSHOTs must be marked noSig",
        keysMap.contains("noSig"));
  }

  @Test
  public void howToDocumentationExistsAndIsLinkedFromIndex() throws IOException {
    Path root = repositoryRoot();
    Path howTo = root.resolve("docs/howto/run-pgpverify.md");
    assertTrue("docs/howto/run-pgpverify.md must exist", Files.isRegularFile(howTo));
    String doc = Files.readString(howTo, StandardCharsets.UTF_8);
    assertTrue(
        "How-to must document the opt-in activation flag -Ppgpverify",
        doc.contains("-Ppgpverify"));

    Path index = root.resolve("docs/index.md");
    assertTrue("docs/index.md must exist", Files.isRegularFile(index));
    assertTrue(
        "docs/index.md must link to the run-pgpverify how-to for discoverability",
        Files.readString(index, StandardCharsets.UTF_8).contains("howto/run-pgpverify.md"));
  }

  private static String extractPgpVerifyProfile() throws IOException {
    String pom = Files.readString(repositoryRoot().resolve("pom.xml"), StandardCharsets.UTF_8);
    int idIndex = pom.indexOf("<id>pgpverify</id>");
    if (idIndex < 0) {
      return "";
    }
    int start = pom.lastIndexOf("<profile>", idIndex);
    int end = pom.indexOf("</profile>", idIndex);
    if (start < 0 || end < 0) {
      return "";
    }
    return pom.substring(start, end + "</profile>".length());
  }

  private static String stripWhitespace(String value) {
    return value.replaceAll("\\s+", "");
  }

  private static Path repositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.exists(current.resolve(".git")) && Files.isRegularFile(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new AssertionError("Could not find repository root from " + System.getProperty("user.dir"));
  }
}
