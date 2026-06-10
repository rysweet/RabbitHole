package org.alice.ide.ast.importers;

import edu.cmu.cs.dennisc.java.io.FileUtilities;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lgna.common.resources.AudioResource;
import org.lgna.croquet.importer.Importer;
import org.lgna.story.implementation.StoryApiDirectoryUtilities;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.Assert.*;

public class AudioResourceImporterCoverageTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void getInstance_returnsSingleton() {
    assertSame(AudioResourceImporter.getInstance(), AudioResourceImporter.getInstance());
  }

  @Test
  public void extendsImporter() {
    assertTrue(Importer.class.isAssignableFrom(AudioResourceImporter.class));
  }

  @Test
  public void constructorIsPrivate() throws Exception {
    Constructor<?> constructor = AudioResourceImporter.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
  }

  @Test
  public void initialDirectory_matchesSoundGalleryDirectory() throws Exception {
    Field field = Importer.class.getDeclaredField("initialDirectory");
    field.setAccessible(true);
    File initialDirectory = (File) field.get(AudioResourceImporter.getInstance());
    File soundGalleryDirectory = StoryApiDirectoryUtilities.getSoundGalleryDirectory();
    assertEquals(soundGalleryDirectory != null ? soundGalleryDirectory : FileUtilities.getDefaultDirectory(), initialDirectory);
  }

  @Test
  public void initialDirectory_fallsBackToDefaultDirectoryWhenSoundGalleryIsMissing() throws Exception {
    String previousRootDirectory = System.getProperty("org.alice.ide.rootDirectory");
    try {
      System.setProperty("org.alice.ide.rootDirectory", temporaryFolder.newFolder("missing-alice-install").getAbsolutePath());
      Constructor<?> constructor = AudioResourceImporter.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object importer = constructor.newInstance();

      Field field = Importer.class.getDeclaredField("initialDirectory");
      field.setAccessible(true);
      assertEquals(FileUtilities.getDefaultDirectory(), field.get(importer));
    } finally {
      if (previousRootDirectory == null) {
        System.clearProperty("org.alice.ide.rootDirectory");
      } else {
        System.setProperty("org.alice.ide.rootDirectory", previousRootDirectory);
      }
    }
  }

  @Test
  public void lowerCaseExtensions_matchAudioResourceExtensions() throws Exception {
    Field field = Importer.class.getDeclaredField("lowerCaseExtensions");
    field.setAccessible(true);
    Set<?> extensions = (Set<?>) field.get(AudioResourceImporter.getInstance());
    assertEquals(AudioResource.getFileExtensions(), extensions);
  }
}
