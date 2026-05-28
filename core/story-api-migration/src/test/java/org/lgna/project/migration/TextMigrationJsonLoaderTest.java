package org.lgna.project.migration;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class TextMigrationJsonLoaderTest {
  @Test
  public void jsonLoaderMatchesLegacyRegistryDefinitions() throws Exception {
    TextMigration[] expected = createLegacyMigrations();
    TextMigration[] actual = TextMigrationJsonLoader.load();

    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].getResultVersion().toString(), actual[i].getResultVersion().toString());

      List<PairData> expectedPairs = pairsOf(expected[i]);
      List<PairData> actualPairs = pairsOf(actual[i]);
      assertEquals(expectedPairs.size(), actualPairs.size());
      for (int j = 0; j < expectedPairs.size(); j++) {
        assertEquals(expectedPairs.get(j).pattern, actualPairs.get(j).pattern);
        assertEquals(expectedPairs.get(j).replacement, actualPairs.get(j).replacement);
      }
    }
  }

  @Test
  public void jsonLoaderContainsKnownResolvedPairs() throws Exception {
    TextMigration[] migrations = TextMigrationJsonLoader.load();

    assertMigrationContainsPair(migrationForVersion(migrations, "3.1.9.0.0"),
        "ARMOIRE_CLOTHING",
        null);
    assertMigrationContainsPair(migrationForVersion(migrations, "3.1.34.0.0"),
        "org.lgna.story.Program",
        "org.lgna.story.SProgram");
    assertMigrationContainsPair(migrationForVersion(migrations, "3.2.110.0.0"),
        "name=\"OVAL\">\\s*<declaringClass name=\"org.lgna.story.resources.prop.SandDunesResource\"",
        "name=\"OVAL_DESERT\"> <declaringClass name=\"org.lgna.story.resources.prop.SandDunesResource\"");
  }

  private static TextMigration[] createLegacyMigrations() {
    TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();
    TextMigration[] v3134 = TextMigrationRegistryV3134.create();
    TextMigration[] mid = TextMigrationRegistrySmallVersions.createMid();
    TextMigration[] v3159 = TextMigrationRegistryV3159.create();
    TextMigration[] late = TextMigrationRegistryLateVersions.create();

    TextMigration[] all = new TextMigration[early.length + v3134.length + mid.length + v3159.length + late.length];
    int offset = 0;
    System.arraycopy(early, 0, all, offset, early.length);
    offset += early.length;
    System.arraycopy(v3134, 0, all, offset, v3134.length);
    offset += v3134.length;
    System.arraycopy(mid, 0, all, offset, mid.length);
    offset += mid.length;
    System.arraycopy(v3159, 0, all, offset, v3159.length);
    offset += v3159.length;
    System.arraycopy(late, 0, all, offset, late.length);
    return all;
  }

  private static TextMigration migrationForVersion(TextMigration[] migrations, String version) {
    for (TextMigration migration : migrations) {
      if (migration.getResultVersion().toString().equals(version)) {
        return migration;
      }
    }
    throw new AssertionError("No migration found for version " + version);
  }

  private static void assertMigrationContainsPair(TextMigration migration, String expectedPattern, String expectedReplacement) throws Exception {
    for (PairData pair : pairsOf(migration)) {
      if (pair.pattern.equals(expectedPattern) && equalsNullable(pair.replacement, expectedReplacement)) {
        return;
      }
    }
    throw new AssertionError("Missing pair " + expectedPattern + " -> " + expectedReplacement + " in " + migration.getResultVersion());
  }

  private static boolean equalsNullable(String left, String right) {
    return left == null ? right == null : left.equals(right);
  }

  private static List<PairData> pairsOf(TextMigration textMigration) throws Exception {
    Field pairsField = TextMigration.class.getDeclaredField("pairs");
    pairsField.setAccessible(true);

    Class<?> pairClass = Class.forName(TextMigration.class.getName() + "$Pair");
    Field patternField = pairClass.getDeclaredField("pattern");
    patternField.setAccessible(true);
    Field replacementField = pairClass.getDeclaredField("replacement");
    replacementField.setAccessible(true);

    Object[] pairs = (Object[]) pairsField.get(textMigration);
    List<PairData> data = new ArrayList<>(pairs.length);
    for (Object pair : pairs) {
      data.add(new PairData(((Pattern) patternField.get(pair)).pattern(), (String) replacementField.get(pair)));
    }
    return data;
  }

  private static final class PairData {
    private final String pattern;
    private final String replacement;

    private PairData(String pattern, String replacement) {
      this.pattern = pattern;
      this.replacement = replacement;
    }
  }
}
