package org.lgna.project.migration;

import org.lgna.project.Version;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class ProjectMigrationManagerTest {

  private final ProjectMigrationManager manager = ProjectMigrationManager.getInstance();

  @Test
  public void textMigrationResultVersionsAreValidRoundTrippableAndIncreasing() {
    Version previous = null;

    for (TextMigration migration : manager.getTextMigrations()) {
      Version resultVersion = migration.getResultVersion();

      assertTrue(resultVersion.toString(), resultVersion.isValid());
      assertEquals(resultVersion.toString(), new Version(resultVersion.toString()).toString());
      if (previous != null) {
        assertTrue(previous + " should be before " + resultVersion, previous.compareTo(resultVersion) < 0);
      }
      previous = resultVersion;
    }
  }

  @Test
  public void astMigrationResultVersionsAreValidRoundTrippableAndIncreasing() {
    Version previous = null;

    for (AstMigration migration : manager.getAstMigrations()) {
      Version resultVersion = migration.getResultVersion();

      assertTrue(resultVersion.toString(), resultVersion.isValid());
      assertEquals(resultVersion.toString(), new Version(resultVersion.toString()).toString());
      if (previous != null) {
        assertTrue(previous + " should be before " + resultVersion, previous.compareTo(resultVersion) < 0);
      }
      previous = resultVersion;
    }
  }

  @Test
  public void migrationIsApplicableOnlyBeforeItsResultVersion() {
    TextMigration migration = textMigrationFor("3.1.20.0.0");

    assertTrue(migration.isApplicable(new Version("3.1.19.0.0")));
    assertFalse(migration.isApplicable(new Version("3.1.20.0.0")));
    assertFalse(migration.isApplicable(new Version("3.1.21.0.0")));
  }

  @Test
  public void textMigrationRewritesKnownLegacyStoryAndResourceNames() {
    String source = String.join("\n",
        "org.lgna.story.resources.dresser.DresserCentralAsian",
        "org.lgna.story.Program",
        "INDIA_BRICK_D",
        "<method isVarArgs=\"false\" name=\"getModelAtMouseLocation\"><declaringClass name=\"org.lgna.story.event.MouseClickEvent\"/><parameters/></method>",
        "<method isVarArgs=\"true\" name=\"getDistanceTo\"><declaringClass name=\"org.lgna.story.STurnable\"/><parameters><type name=\"org.lgna.story.STurnable\"/>"
    );

    String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");

    assertTrue(migrated.contains("org.lgna.story.resources.prop.DresserResource"));
    assertTrue(migrated.contains("org.lgna.story.SProgram"));
    assertTrue(migrated.contains("GRAY"));
    assertTrue(migrated.contains("org.lgna.story.event.MouseClickOnObjectEvent"));
    assertTrue(migrated.contains("<type name=\"org.lgna.story.SThing\"/>"));
    assertFalse(migrated.contains("org.lgna.story.resources.dresser.DresserCentralAsian"));
    assertFalse(migrated.contains("org.lgna.story.Program"));
    assertFalse(migrated.contains("INDIA_BRICK_D"));
    assertFalse(migrated.contains("org.lgna.story.event.MouseClickEvent"));
  }

  @Test
  public void textMigrationCascadesLegacyDresserThroughIntermediateResourceNames() {
    String source = String.join("\n",
        "<type name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>",
        "<declaringClass name=\"org.lgna.story.resources.dresser.DresserCentralAsian\"/>"
    );

    String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");

    assertEquals(String.join("\n",
        "<type name=\"org.lgna.story.resources.prop.DresserResource\"/>",
        "<declaringClass name=\"org.lgna.story.resources.prop.DresserResource\"/>"
    ), migrated);
    assertFalse(migrated.contains("org.lgna.story.resources.dresser.DresserCentralAsian"));
    assertFalse(migrated.contains("org.lgna.story.resources.prop.DresserCentralAsian"));
    assertFalse(migrated.contains("org.lgna.story.resources.prop.Dresser\""));
  }

  @Test
  public void textMigrationCascadesLegacyDresserFieldThroughIntermediateResourceNames() {
    String source = String.join("\n",
        "name=\"DRESSER_CENTRAL_ASIAN_GREEN\">",
        "<declaringClass name=\"org.lgna.story.resources.dresser.DresserCentralAsian\""
    );

    String migrated = migrateWithoutTestLogNoise(source, "3.1.19.0.0");

    assertEquals("name=\"CENTRAL_ASIAN_GREEN\"> <declaringClass name=\"org.lgna.story.resources.prop.DresserResource\"", migrated);
    assertFalse(migrated.contains("CENTRAL_ASIAN_DRESSER_CENTRAL_ASIAN_GREEN"));
    assertFalse(migrated.contains("org.lgna.story.resources.dresser.DresserCentralAsian"));
    assertFalse(migrated.contains("org.lgna.story.resources.prop.DresserCentralAsian"));
    assertFalse(migrated.contains("org.lgna.story.resources.prop.Dresser\""));
  }

  @Test
  public void textMigrationStartingAfterDresserPackageMoveStillAppliesLaterConsolidations() {
    String source = String.join("\n",
        "<type name=\"org.lgna.story.resources.prop.DresserCentralAsian\"/>",
        "<declaringClass name=\"org.lgna.story.resources.prop.DresserCentralAsian\"/>"
    );

    String migrated = migrateWithoutTestLogNoise(source, "3.1.20.0.0");

    assertEquals(String.join("\n",
        "<type name=\"org.lgna.story.resources.prop.DresserResource\"/>",
        "<declaringClass name=\"org.lgna.story.resources.prop.DresserResource\"/>"
    ), migrated);
    assertFalse(migrated.contains("org.lgna.story.resources.prop.DresserCentralAsian"));
    assertFalse(migrated.contains("org.lgna.story.resources.prop.Dresser\""));
  }

  @Test
  public void textMigrationDoesNotRewriteWhenVersionIsAlreadyAtThreshold() {
    String source = "org.lgna.story.resources.dresser.DresserCentralAsian";

    String migrated = migrateWithoutTestLogNoise(source, "3.1.20.0.0");

    assertEquals(source, migrated);
  }

  @Test
  public void managerReportsNoPendingMigrationsAtCurrentVersion() {
    Version currentVersion = manager.getCurrentVersion();

    assertFalse(manager.hasTextMigrationsFor(currentVersion));
    assertFalse(manager.hasAstMigrationsFor(currentVersion));
  }

  private TextMigration textMigrationFor(String versionText) {
    Version version = new Version(versionText);
    for (TextMigration migration : manager.getTextMigrations()) {
      if (migration.getResultVersion().compareTo(version) == 0) {
        return migration;
      }
    }
    fail("No text migration found for " + versionText);
    return null;
  }

  private String migrateWithoutTestLogNoise(String source, String versionText) {
    PrintStream previousOut = System.out;
    try {
      System.setOut(new PrintStream(new ByteArrayOutputStream()));
      return manager.migrate(source, new Version(versionText));
    } finally {
      System.setOut(previousOut);
    }
  }
}
