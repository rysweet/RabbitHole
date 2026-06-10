/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.java.io.FileUtilities;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessageSeverity;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.alice.nonfree.NebulousStoryApi;
import org.alice.tweedle.file.ModelManifest;
import org.lgna.story.implementation.StoryApiDirectoryUtilities;
import org.lgna.story.resources.ModelResource;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.prefs.Preferences;

public enum StorytellingResources {
  INSTANCE;

  private static final String ALICE_RESOURCE_DIRECTORY_PREF_KEY = "ALICE_RESOURCE_DIRECTORY_PREF_KEY";
  static final String GALLERY_DIRECTORY_PREF_KEY = "GALLERY_DIRECTORY_PREF_KEY";

  private static final String ALICE_RESOURCE_INSTALL_PATH = "assets/alice";

  private List<Class<? extends ModelResource>> installedAliceClassesLoaded = null;
  private List<URLClassLoader> resourceClassLoaders;
  private final ModelManifestManager manifestManager = new ModelManifestManager();

  public static File getGalleryDirectory(File dir) {
    if (dir.exists() && dir.isDirectory()) {
      File[] dirs = FileUtilities.listDescendants(dir, File::isDirectory, 4);
      for (File subDir : dirs) {
        String galleryDir = getGalleryPathFromResourcePath(subDir.getAbsolutePath());
        if (galleryDir != null) {
          return new File(galleryDir);
        }
      }
    }
    return null;
  }

  public static File getGalleryRootDirectory() {
    return StoryApiDirectoryUtilities.getModelGalleryDirectory();
  }

  static File findResourcePath(String relativePath) {
    File rootGallery = getGalleryRootDirectory();
    if ((rootGallery != null) && rootGallery.exists()) {
      File path = new File(rootGallery, relativePath);
      if (path.exists()) {
        return path;
      }
    }
    return null;
  }

  private static String getGalleryPathFromResourcePath(String resourcePath) {
    if (resourcePath != null) {
      int resourceIndex = -1;
      if (NebulousStoryApi.nonfree.getNebulousResourceInstallPath() != null) {
        resourceIndex = resourcePath.lastIndexOf(NebulousStoryApi.nonfree.getNebulousResourceInstallPath());
      }
      if (resourceIndex == -1) {
        resourceIndex = resourcePath.lastIndexOf(ALICE_RESOURCE_INSTALL_PATH);
      }
      if (resourceIndex != -1) {
        resourcePath = resourcePath.substring(0, resourceIndex);
        while (resourcePath.endsWith("/")) {
          resourcePath = resourcePath.substring(0, resourcePath.length() - 1);
        }
        File galleryDir = new File(resourcePath);
        if (galleryDir.exists()) {
          return resourcePath;
        }
      }
    }
    return null;
  }

  private static String[] getGalleryPathsFromResourcePath(String resourcePath) {
    if (resourcePath != null) {
      resourcePath = resourcePath.replace('\\', '/');
      String[] resourcePaths = resourcePath.split(PATH_SEPARATOR);
      Set<String> galleryPaths = new LinkedHashSet<>();
      for (String path : resourcePaths) {
        String galleryPath = getGalleryPathFromResourcePath(path);
        if (galleryPath != null) {
          galleryPaths.add(galleryPath);
        }
      }
      return galleryPaths.toArray(new String[0]);
    }
    return null;
  }

  private static String getPreference(String key, String def) {
    return Preferences.userRoot().get(key, def);
  }

  static File[] getDirsFromPref(String key, String relativeDir) {
    String dir = getPreference(key, "");
    if ((dir != null) && (dir.length() > 0)) {
      String[] splitDir = dir.split(PATH_SEPARATOR);
      File[] fileDirs = new File[splitDir.length];
      for (int i = 0; i < splitDir.length; i++) {
        fileDirs[i] = new File(splitDir[i] + relativeDir);
      }
      return fileDirs;
    }
    return null;
  }

  static String makeDirectoryPreferenceString(String[] dirs) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < dirs.length; i++) {
      if (i != 0) {
        sb.append(PATH_SEPARATOR);
      }
      sb.append(dirs[i]);
    }
    return sb.toString();
  }

  private File[] getAliceDirsFromGalleryPref() {
    return getDirsFromPref(GALLERY_DIRECTORY_PREF_KEY, ALICE_RESOURCE_INSTALL_PATH);
  }

  public void setAliceResourceDirs(String[] dirs) {
    Preferences rv = Preferences.userRoot();
    String dirsString = makeDirectoryPreferenceString(dirs);
    rv.put(ALICE_RESOURCE_DIRECTORY_PREF_KEY, dirsString);
    String[] galleryDir = getGalleryPathsFromResourcePath(dirsString);
    if (galleryDir != null) {
      setGalleryResourceDirs(galleryDir);
    }
  }

  public File[] getAliceDirsFromPref() {
    File[] dirs = getDirsFromPref(ALICE_RESOURCE_DIRECTORY_PREF_KEY, "");
    if (dirs != null) {
      return dirs;
    } else {
      return getAliceDirsFromGalleryPref();
    }
  }

  public void setGalleryResourceDirs(String[] dirs) {
    Preferences rv = Preferences.userRoot();
    rv.put(GALLERY_DIRECTORY_PREF_KEY, makeDirectoryPreferenceString(dirs));
  }

  private static final String PATH_SEPARATOR = System.getProperty("path.separator");

  private List<File> findAliceResources() {
    File customResourcesPath = findResourcePath("assets/custom");
    if (customResourcesPath != null) {
      ResourcePathManager.addPath(ResourcePathManager.MODEL_RESOURCE_KEY, customResourcesPath);
    }

    File alicePath = findResourcePath(ALICE_RESOURCE_INSTALL_PATH);
    if (alicePath != null) {
      ResourcePathManager.addPath(ResourcePathManager.MODEL_RESOURCE_KEY, alicePath);
      return ResourcePathManager.getPaths(ResourcePathManager.MODEL_RESOURCE_KEY);
    } else {
      List<File> directoryFromSavedPreference = new ArrayList<>();
      File[] resourceDirs = getAliceDirsFromPref();
      if (resourceDirs != null) {
        Collections.addAll(directoryFromSavedPreference, resourceDirs);
      }
      return directoryFromSavedPreference;
    }
  }

  private StorytellingResources() {
  }

  public static Map<File, List<String>> getClassNamesFromResources(File... resourceFiles) {
    return ResourceClassLoader.getClassNamesFromResources(resourceFiles);
  }

  public void getGalleryLocationFromUser() {
    ResourcePromptResult result = UiPrompts.requestResourceLocation(aliceResourcePrompt(Collections.emptyList(), true));
    if (result.selectedGalleryDirectory().isPresent()) {
      String[] dirArray = {result.selectedGalleryDirectory().get().getAbsolutePath()};
      setGalleryResourceDirs(dirArray);
    }
  }

  private void clearAliceResourceInfo() {
    ResourcePathManager.clearPaths(ResourcePathManager.MODEL_RESOURCE_KEY);
    Preferences preferences = Preferences.userRoot();
    preferences.put(ALICE_RESOURCE_DIRECTORY_PREF_KEY, "");
    preferences.put(GALLERY_DIRECTORY_PREF_KEY, "");
  }

  List<ModelManifest> findAndLoadUserGalleryResourcesIfNecessary() {
    return manifestManager.findAndLoadUserGalleryResources();
  }

  List<ModelManifest> findAndLoadInternalResourcesIfNecessary() {
    return manifestManager.findAndLoadInternalResources();
  }

  List<ModelManifest> findNewUserGalleryResources() {
    return manifestManager.findNewUserGalleryResources();
  }

  List<Class<? extends ModelResource>> findAndLoadInstalledAliceResourcesIfNecessary() {
    if (this.installedAliceClassesLoaded == null) {
      List<File> resourcePaths = ResourcePathManager.getPaths(ResourcePathManager.MODEL_RESOURCE_KEY);
      if (resourcePaths.isEmpty()) {
        resourcePaths = findAliceResources();
      }

      ResourceClassLoader.LoadResult loadResult = ResourceClassLoader.getAndLoadModelResourceClasses(resourcePaths);
      this.installedAliceClassesLoaded = loadResult.classes();
      addClassLoaders(loadResult.classLoaders());

      if (installedAliceClassesLoaded.isEmpty()) {
        clearAliceResourceInfo();
        ResourcePromptResult result = UiPrompts.requestResourceLocation(aliceResourcePrompt(resourcePaths, false));
        if (result.selectedGalleryDirectory().isPresent()) {
          String[] dirArray = {result.selectedGalleryDirectory().get().getAbsolutePath()};
          setGalleryResourceDirs(dirArray);
          resourcePaths = findAliceResources();
          loadResult = ResourceClassLoader.getAndLoadModelResourceClasses(resourcePaths);
          this.installedAliceClassesLoaded = loadResult.classes();
          addClassLoaders(loadResult.classLoaders());
        }
      }
      if (this.installedAliceClassesLoaded.isEmpty()) {
        clearAliceResourceInfo();
        ResourcePromptRequest request = aliceResourcePrompt(resourcePaths, false);
        UiPrompts.showMessage(new MessagePromptRequest(MessageSeverity.INFO, null, aliceMissingResourceMessage(request)));
      } else {
        String[] galleryDirs = new String[resourcePaths.size()];
        for (int i = 0; i < resourcePaths.size(); i++) {
          File galleryFile = resourcePaths.get(i);
          if (galleryFile.isDirectory()) {
            galleryDirs[i] = galleryFile.getAbsolutePath();
          } else {
            galleryDirs[i] = galleryFile.getParentFile().getAbsolutePath();
          }
        }
        setAliceResourceDirs(galleryDirs);
      }
    }
    return this.installedAliceClassesLoaded;
  }

  private static ResourcePromptRequest aliceResourcePrompt(List<File> searchedDirectories, boolean alwaysPrompt) {
    return new ResourcePromptRequest(
        "Locate Resources",
        "Alice gallery resources",
        ALICE_RESOURCE_INSTALL_PATH,
        searchedDirectories == null ? Collections.emptyList() : searchedDirectories,
        "Cannot find the Alice gallery resources.",
        alwaysPrompt);
  }

  private static String aliceMissingResourceMessage(ResourcePromptRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append(request.missingMessage());
    if (request.searchedDirectories().isEmpty()) {
      sb.append("\nNo gallery directories were detected. Make sure Alice is properly installed and has been run at least once.");
    } else {
      sb.append("\nFailed to locate the resources in:");
      String separator = "\n   ";
      for (File path : request.searchedDirectories()) {
        sb.append(separator).append("'").append(path).append("'");
      }
      String phrase = request.searchedDirectories().size() > 1 ? "these directories exist" : "this directory exists";
      sb.append("\nVerify that ").append(phrase).append(" and verify that Alice is properly installed.");
    }
    return sb.toString();
  }

  private void addClassLoaders(List<URLClassLoader> loaders) {
    if (this.resourceClassLoaders == null) {
      this.resourceClassLoaders = new ArrayList<>();
    }
    this.resourceClassLoaders.addAll(loaders);
  }

  public ModelManifest getModelManifest(String modelName) {
    return manifestManager.getModelManifest(modelName);
  }

  public ModelManifest getInternalModelManifest(String modelName) {
    return manifestManager.getInternalModelManifest(modelName);
  }

  public URL getAliceResource(String resourceString) {
    if (resourceString.contains("ı")) {
      Logger.severe(resourceString);
      resourceString = resourceString.replaceAll("ı", "i");
    }
    this.findAndLoadInstalledAliceResourcesIfNecessary();
    assert this.resourceClassLoaders != null;
    URL foundResource = null;
    for (URLClassLoader cl : this.resourceClassLoaders) {
      foundResource = cl.findResource(resourceString);
      if (foundResource != null) {
        break;
      }
    }
    return foundResource;
  }

  public InputStream getAliceResourceAsStream(String resourceString) {
    this.findAndLoadInstalledAliceResourcesIfNecessary();
    assert this.resourceClassLoaders != null;
    InputStream foundResource = null;
    for (URLClassLoader cl : this.resourceClassLoaders) {
      foundResource = cl.getResourceAsStream(resourceString);
      if (foundResource != null) {
        break;
      }
    }
    return foundResource;
  }
}
