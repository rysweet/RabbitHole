package org.lgna.project.migration;

import org.lgna.project.Version;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * TDD tests for the TextMigrationRegistry extraction.
 *
 * These tests define the contract that the new registry classes must satisfy.
 * They verify structural properties (counts, ordering, boundaries) and
 * behavioral identity (registry output matches the original PMM output).
 */
public class TextMigrationRegistryTest {

  // ── Registry assembler: createAll() ──────────────────────────────────

  @Test
  public void createAllReturns28Migrations() {
    TextMigration[] all = TextMigrationRegistry.createAll();

    assertEquals("Total text migration count", 28, all.length);
  }

  @Test
  public void createAllContainsNoNulls() {
    TextMigration[] all = TextMigrationRegistry.createAll();

    for (int i = 0; i < all.length; i++) {
      assertNotNull("Migration at index " + i + " is null", all[i]);
    }
  }

  @Test
  public void createAllVersionsAreStrictlyIncreasing() {
    TextMigration[] all = TextMigrationRegistry.createAll();
    Version previous = null;

    for (TextMigration migration : all) {
      Version v = migration.getResultVersion();
      if (previous != null) {
        assertTrue(previous + " should be before " + v, previous.compareTo(v) < 0);
      }
      previous = v;
    }
  }

  @Test
  public void createAllFirstVersionIs3_1_8() {
    TextMigration[] all = TextMigrationRegistry.createAll();

    assertEquals(new Version("3.1.8.0.0"), all[0].getResultVersion());
  }

  @Test
  public void createAllLastVersionIs3_9_0() {
    TextMigration[] all = TextMigrationRegistry.createAll();

    assertEquals(new Version("3.9.0.0"), all[all.length - 1].getResultVersion());
  }

  @Test
  public void createAllContainsFactoryMigration3_2_110() {
    TextMigration[] all = TextMigrationRegistry.createAll();
    Version target = new Version("3.2.110.0.0");

    boolean found = false;
    for (TextMigration migration : all) {
      if (migration.getResultVersion().compareTo(target) == 0) {
        found = true;
        break;
      }
    }
    assertTrue("Factory migration 3.2.110.0.0 must be present", found);
  }

  @Test
  public void createAllVersionsHaveNoDuplicates() {
    TextMigration[] all = TextMigrationRegistry.createAll();
    Set<String> seen = new HashSet<>();

    for (TextMigration migration : all) {
      String v = migration.getResultVersion().toString();
      assertTrue("Duplicate version: " + v, seen.add(v));
    }
  }

  @Test
  public void createAllExpectedVersionsPresent() {
    TextMigration[] all = TextMigrationRegistry.createAll();
    Set<String> versions = new HashSet<>();
    for (TextMigration m : all) {
      versions.add(m.getResultVersion().toString());
    }

    String[] expected = {
        "3.1.8.0.0", "3.1.9.0.0", "3.1.11.0.0", "3.1.14.0.0",
        "3.1.15.1.0", "3.1.20.0.0", "3.1.33.0.0", "3.1.34.0.0",
        "3.1.35.0.0", "3.1.38.0.0", "3.1.39.0.0", "3.1.48.0.0",
        "3.1.58.0.0", "3.1.59.0.0", "3.1.68.0.0", "3.1.69.0.0",
        "3.1.70.0.0", "3.1.85.0.0", "3.1.92.0.0", "3.1.93.0.0",
        "3.2.108.0.0", "3.2.110.0.0", "3.2.111.0.0", "3.2.112.0.0",
        "3.2.113.0.0", "3.3.0.0.0", "3.4.0.0", "3.9.0.0"
    };

    for (String v : expected) {
      assertTrue("Missing version: " + v, versions.contains(v));
    }
  }

  // ── Identity: registry output matches original PMM ───────────────────

  @Test
  public void registryMatchesProjectMigrationManagerTextMigrations() {
    TextMigration[] fromRegistry = TextMigrationRegistry.createAll();
    TextMigration[] fromManager = ProjectMigrationManager.getInstance().getTextMigrations();

    assertEquals("Array lengths must match",
        fromManager.length, fromRegistry.length);

    for (int i = 0; i < fromManager.length; i++) {
      assertEquals("Version mismatch at index " + i,
          fromManager[i].getResultVersion().toString(),
          fromRegistry[i].getResultVersion().toString());
    }
  }

  @Test
  public void registryMigrationsProduceSameOutputAsOriginal() {
    TextMigration[] fromRegistry = TextMigrationRegistry.createAll();
    TextMigration[] fromManager = ProjectMigrationManager.getInstance().getTextMigrations();

    // Test with a sample input that exercises multiple migration versions
    String testInput = String.join("\n",
        "org.lgna.story.resources.dresser.DresserCentralAsian",
        "org.lgna.story.Program",
        "INDIA_BRICK_D",
        "name=\"LEFT_THUMB_1\">",
        "name=\"ICE_FLOW",
        "name=\"OVAL\">\n<declaringClass name=\"org.lgna.story.resources.prop.SandDunesResource\""
    );

    for (int i = 0; i < fromManager.length; i++) {
      String managerResult = fromManager[i].migrate(testInput);
      String registryResult = fromRegistry[i].migrate(testInput);
      assertEquals("Migration output mismatch at version "
              + fromManager[i].getResultVersion(),
          managerResult, registryResult);
    }
  }

  // ── Sub-component: SmallVersions ─────────────────────────────────────

  @Test
  public void smallVersionsEarlyReturns7Migrations() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();

    assertEquals("Early migration count", 7, early.length);
  }

  @Test
  public void smallVersionsEarlyFirstVersionIs3_1_8() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();

    assertEquals(new Version("3.1.8.0.0"), early[0].getResultVersion());
  }

  @Test
  public void smallVersionsEarlyLastVersionIs3_1_33() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();

    assertEquals(new Version("3.1.33.0.0"),
        early[early.length - 1].getResultVersion());
  }

  @Test
  public void smallVersionsEarlyVersionsAreStrictlyIncreasing() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();
    Version previous = null;

    for (TextMigration m : early) {
      Version v = m.getResultVersion();
      if (previous != null) {
        assertTrue(previous + " should be before " + v,
            previous.compareTo(v) < 0);
      }
      previous = v;
    }
  }

  @Test
  public void smallVersionsMidReturns5Migrations() {
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();

    assertEquals("Mid migration count", 5, mid.length);
  }

  @Test
  public void smallVersionsMidFirstVersionIs3_1_35() {
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();

    assertEquals(new Version("3.1.35.0.0"), mid[0].getResultVersion());
  }

  @Test
  public void smallVersionsMidLastVersionIs3_1_58() {
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();

    assertEquals(new Version("3.1.58.0.0"),
        mid[mid.length - 1].getResultVersion());
  }

  @Test
  public void smallVersionsMidVersionsAreStrictlyIncreasing() {
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();
    Version previous = null;

    for (TextMigration m : mid) {
      Version v = m.getResultVersion();
      if (previous != null) {
        assertTrue(previous + " should be before " + v,
            previous.compareTo(v) < 0);
      }
      previous = v;
    }
  }

  // ── Sub-component: V3134 ─────────────────────────────────────────────

  @Test
  public void v3134Returns1Migration() {
    TextMigration[] v3134 = TextMigrationRegistryV3134.create();

    assertEquals("V3134 migration count", 1, v3134.length);
  }

  @Test
  public void v3134VersionIs3_1_34() {
    TextMigration[] v3134 = TextMigrationRegistryV3134.create();

    assertEquals(new Version("3.1.34.0.0"), v3134[0].getResultVersion());
  }

  // ── Sub-component: V3159 ─────────────────────────────────────────────

  @Test
  public void v3159Returns1Migration() {
    TextMigration[] v3159 = TextMigrationRegistryV3159.create();

    assertEquals("V3159 migration count", 1, v3159.length);
  }

  @Test
  public void v3159VersionIs3_1_59() {
    TextMigration[] v3159 = TextMigrationRegistryV3159.create();

    assertEquals(new Version("3.1.59.0.0"), v3159[0].getResultVersion());
  }

  // ── Sub-component: LateVersions ──────────────────────────────────────

  @Test
  public void lateVersionsReturns14Migrations() {
    TextMigration[] late = TextMigrationRegistryLateVersions.create();

    assertEquals("Late migration count", 14, late.length);
  }

  @Test
  public void lateVersionsFirstVersionIs3_1_68() {
    TextMigration[] late = TextMigrationRegistryLateVersions.create();

    assertEquals(new Version("3.1.68.0.0"), late[0].getResultVersion());
  }

  @Test
  public void lateVersionsLastVersionIs3_9_0() {
    TextMigration[] late = TextMigrationRegistryLateVersions.create();

    assertEquals(new Version("3.9.0.0"),
        late[late.length - 1].getResultVersion());
  }

  @Test
  public void lateVersionsContainsFactoryMigration() {
    TextMigration[] late = TextMigrationRegistryLateVersions.create();
    Version target = new Version("3.2.110.0.0");

    boolean found = false;
    for (TextMigration m : late) {
      if (m.getResultVersion().compareTo(target) == 0) {
        found = true;
        break;
      }
    }
    assertTrue("Factory migration 3.2.110 must be in late versions", found);
  }

  @Test
  public void lateVersionsAreStrictlyIncreasing() {
    TextMigration[] late = TextMigrationRegistryLateVersions.create();
    Version previous = null;

    for (TextMigration m : late) {
      Version v = m.getResultVersion();
      if (previous != null) {
        assertTrue(previous + " should be before " + v,
            previous.compareTo(v) < 0);
      }
      previous = v;
    }
  }

  // ── Assembly correctness ─────────────────────────────────────────────

  @Test
  public void assemblyOrderMatchesSubComponentOrder() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();
    TextMigration[] v3134 = TextMigrationRegistryV3134.create();
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();
    TextMigration[] v3159 = TextMigrationRegistryV3159.create();
    TextMigration[] late = TextMigrationRegistryLateVersions.create();
    TextMigration[] all = TextMigrationRegistry.createAll();

    int expectedTotal = early.length + v3134.length + mid.length
        + v3159.length + late.length;
    assertEquals("Sum of parts must equal whole", expectedTotal, all.length);

    int offset = 0;
    for (TextMigration m : early) {
      assertEquals("Early mismatch at offset " + offset,
          m.getResultVersion().toString(),
          all[offset++].getResultVersion().toString());
    }
    for (TextMigration m : v3134) {
      assertEquals("V3134 mismatch at offset " + offset,
          m.getResultVersion().toString(),
          all[offset++].getResultVersion().toString());
    }
    for (TextMigration m : mid) {
      assertEquals("Mid mismatch at offset " + offset,
          m.getResultVersion().toString(),
          all[offset++].getResultVersion().toString());
    }
    for (TextMigration m : v3159) {
      assertEquals("V3159 mismatch at offset " + offset,
          m.getResultVersion().toString(),
          all[offset++].getResultVersion().toString());
    }
    for (TextMigration m : late) {
      assertEquals("Late mismatch at offset " + offset,
          m.getResultVersion().toString(),
          all[offset++].getResultVersion().toString());
    }
  }

  // ── Each sub-array returns a fresh copy ──────────────────────────────

  @Test
  public void createAllReturnsFreshArrayEachCall() {
    TextMigration[] first = TextMigrationRegistry.createAll();
    TextMigration[] second = TextMigrationRegistry.createAll();

    assertNotSame("createAll must return a new array each time",
        first, second);
    assertEquals(first.length, second.length);
  }
}
