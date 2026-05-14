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

package org.lgna.story.implementation.alice;

import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.ResourceBundleUtilities;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import edu.cmu.cs.dennisc.scenegraph.TexturedAppearance;
import edu.cmu.cs.dennisc.xml.XMLUtilities;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.UnitQuaternion;
import org.lgna.story.resources.*;
import org.lgna.story.resourceutilities.ModelResourceInfo;
import org.lgna.story.resourceutilities.StorytellingResources;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Facade for Alice model resource operations. Delegates to
 * {@link ResourceEnumResolver}, {@link ResourceTextureManager}, and {@link ModelResourceLoader}.
 *
 * @author Dennis Cosgrove
 */
public class AliceResourceUtilities {
  public static final String MODEL_RESOURCE_EXTENSION = "a3r";
  public static final String TEXTURE_RESOURCE_EXTENSION = "a3t";

  private static final Map<String, ModelResourceInfo> classToInfoMap = Maps.newHashMap();

  /*private*/
  protected AliceResourceUtilities() {
    throw new AssertionError();
  }

  public static InputStream getAliceResourceAsStream(Class<?> cls, String resourceString) {
    return StorytellingResources.INSTANCE.getAliceResourceAsStream(cls.getPackage().getName().replace(".", "/") + "/" + resourceString);
  }

  public static URL getAliceResource(Class<?> cls, String resourceString) {
    return StorytellingResources.INSTANCE.getAliceResource(cls.getPackage().getName().replace(".", "/") + "/" + resourceString);
  }

  public static String getName(Class<?> modelResource) {
    return AliceResourceClassUtilities.getAliceClassName(modelResource);
  }

  public static String trimName(String name) {
    name = name.trim();
    while (name.contains("__")) {
      name = name.replace("__", "_");
    }
    while (name.startsWith("_")) {
      name = name.substring(1);
    }
    while (name.endsWith("_")) {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }

  public static URL getUrl(ModelResource resource, String visualResourceFileName) {
    return getAliceResource(resource.getClass(),
        ModelResourceIoUtilities.getResourceSubDirWithSeparator(resource.getClass().getSimpleName()) + visualResourceFileName);
  }

  /*private*/
  protected static String getKey(Class<?> modelResource, String resourceName) {
    if (resourceName != null) {
      return modelResource.getName() + resourceName;
    }
    return modelResource.getName();
  }

  // ── Delegates → ResourceEnumResolver ──────────────────────

  public static String enumToCamelCase(String enumName, boolean startWithLowerCase) {
    return ResourceEnumResolver.enumToCamelCase(enumName, startWithLowerCase);
  }
  public static String enumToCamelCase(String enumName) {
    return ResourceEnumResolver.enumToCamelCase(enumName);
  }
  public static String camelCaseToEnum(String name) {
    return ResourceEnumResolver.camelCaseToEnum(name);
  }
  public static boolean isEnumName(String name) {
    return ResourceEnumResolver.isEnumName(name);
  }
  public static String makeEnumName(String name) {
    return ResourceEnumResolver.makeEnumName(name);
  }
  public static String makeLocalizationKey(String key) {
    return ResourceEnumResolver.makeLocalizationKey(key);
  }
  public static String arrayToEnum(String[] nameArray, int start, int end) {
    return ResourceEnumResolver.arrayToEnum(nameArray, start, end);
  }
  public static String getDefaultTextureEnumName(String resourceName) {
    return ResourceEnumResolver.getDefaultTextureEnumName(resourceName);
  }
  public static String getModelNameFromClassAndResource(ModelResource resource, String resourceName) {
    return ResourceEnumResolver.getModelNameFromClassAndResource(resource, resourceName);
  }
  public static String getTextureNameFromClassAndResource(ModelResource resource, String resourceName) {
    return ResourceEnumResolver.getTextureNameFromClassAndResource(resource, resourceName);
  }
  public static String getVisualResourceName(ModelResource resource) {
    return ResourceEnumResolver.getVisualResourceName(resource);
  }
  public static String getTextureResourceName(ModelResource resource) {
    return ResourceEnumResolver.getTextureResourceName(resource);
  }

  // ── Delegates → ResourceTextureManager ───────────────────

  public static String getThumbnailResourceFileName(String modelName, String textureName) {
    return ResourceTextureManager.getThumbnailResourceFileName(modelName, textureName);
  }
  public static String getTextureResourceFileName(String modelName, String textureName) {
    return ResourceTextureManager.getTextureResourceFileName(modelName, textureName);
  }
  public static String getVisualResourceFileNameFromModelName(String modelName, String extension) {
    return ResourceTextureManager.getVisualResourceFileNameFromModelName(modelName, extension);
  }
  public static String getVisualResourceFileNameFromModelName(String modelName) {
    return ResourceTextureManager.getVisualResourceFileNameFromModelName(modelName);
  }
  public static String getTextureResourceFileName(ModelResource resource, String resourceName) {
    return ResourceTextureManager.getTextureResourceFileName(resource, resourceName);
  }
  public static String getTextureResourceFileName(ModelResource resource) {
    return ResourceTextureManager.getTextureResourceFileName(resource);
  }
  public static String getVisualResourceFileName(ModelResource resource, String resourceName) {
    return ResourceTextureManager.getVisualResourceFileName(resource, resourceName);
  }
  public static String getThumbnailResourceFileName(ModelResource resource, String resourceName) {
    return ResourceTextureManager.getThumbnailResourceFileName(resource, resourceName);
  }
  public static URL getTextureURL(ModelResource resource) {
    return ResourceTextureManager.getTextureURL(resource);
  }
  public static URL getThumbnailURL(ModelResource modelResource, String instanceName) {
    return ResourceTextureManager.getThumbnailURL(modelResource, instanceName);
  }
  public static URL getThumbnailURL(Class<?> modelResource) {
    return ResourceTextureManager.getThumbnailURL(modelResource);
  }

  // ── Delegates → ModelResourceLoader ──────────────────────

  public static SkeletonVisual decodeVisual(URL url) {
    return ModelResourceLoader.decodeVisual(url);
  }
  public static TexturedAppearance[] decodeTexture(URL url) {
    return ModelResourceLoader.decodeTexture(url);
  }
  public static void encodeVisual(final SkeletonVisual toSave, OutputStream os) throws IOException {
    ModelResourceLoader.encodeVisual(toSave, os);
  }
  public static void encodeVisual(final SkeletonVisual toSave, File file) throws IOException {
    ModelResourceLoader.encodeVisual(toSave, file);
  }
  public static void encodeTexture(final TexturedAppearance[] toSave, OutputStream os) throws IOException {
    ModelResourceLoader.encodeTexture(toSave, os);
  }
  public static void encodeTexture(final TexturedAppearance[] toSave, File file) throws IOException {
    ModelResourceLoader.encodeTexture(toSave, file);
  }
  public static SkeletonVisual getVisual(ModelResource resource) {
    return ModelResourceLoader.getVisual(resource);
  }
  public static SkeletonVisual getVisualCopy(ModelResource resource) {
    return ModelResourceLoader.getVisualCopy(resource);
  }
  public static TexturedAppearance[] getTexturedAppearances(ModelResource resource) {
    return ModelResourceLoader.getTexturedAppearances(resource);
  }
  public static SkeletonVisual createCopy(SkeletonVisual sgOriginal) {
    return ModelResourceLoader.createCopy(sgOriginal);
  }
  public static SkeletonVisual createReplaceVisualElements(SkeletonVisual sgOriginal, ModelResource resource) {
    return ModelResourceLoader.createReplaceVisualElements(sgOriginal, resource);
  }
  public static AffineMatrix4x4 getOriginalJointTransformation(ModelResource resource, JointId jointId) {
    return ModelResourceLoader.getOriginalJointTransformation(resource, jointId);
  }
  public static UnitQuaternion getOriginalJointOrientation(ModelResource resource, JointId jointId) {
    return ModelResourceLoader.getOriginalJointOrientation(resource, jointId);
  }

  // ── Localization (retained) ──────────────────────────────

  private static String findLocalizedText(String bundleName, String key, Locale locale) {
    if ((bundleName != null) && (key != null)) {
      try {
        ResourceBundle resourceBundle = ResourceBundleUtilities.getUtf8Bundle(bundleName, locale);
        String rv = resourceBundle.getString(key);
        return rv;
      } catch (MissingResourceException mre) {
        return null;
      }
    } else {
      return null;
    }
  }

  private static String CLASS_NAME_LOCALIZATION_BUNDLE = ModelResource.class.getPackage().getName() + ".GalleryNames";
  private static String GROUP_TAGS_LOCALIZATION_BUNDLE = ModelResource.class.getPackage().getName() + ".GalleryTags";
  private static String THEME_TAGS_LOCALIZATION_BUNDLE = ModelResource.class.getPackage().getName() + ".GalleryTags";
  private static String TAGS_LOCALIZATION_BUNDLE = ModelResource.class.getPackage().getName() + ".GalleryTags";

  private static String getClassNameLocalizationBundleName() { return CLASS_NAME_LOCALIZATION_BUNDLE; }
  private static String getGroupTagsLocalizationBundleName() { return GROUP_TAGS_LOCALIZATION_BUNDLE; }
  private static String getThemeTagsLocalizationBundleName() { return THEME_TAGS_LOCALIZATION_BUNDLE; }
  private static String getTagsLocalizationBundleName() { return TAGS_LOCALIZATION_BUNDLE; }

  public static ModelResourceInfo getModelResourceInfo(Class<?> modelResource, String resourceName) {
    if (modelResource == null) {
      return null;
    }
    String key = getKey(modelResource, resourceName);
    // Single get for the common cache-hit case (avoids double hash lookup)
    ModelResourceInfo cachedInfo = classToInfoMap.get(key);
    if (cachedInfo != null || classToInfoMap.containsKey(key)) {
      return cachedInfo;
    }

    String parentKey = getKey(modelResource, null);
    ModelResourceInfo parentInfo = classToInfoMap.get(parentKey);
    if (parentInfo == null && !classToInfoMap.containsKey(parentKey)) {
      String name = getName(modelResource);
      try {
        InputStream is = getAliceResourceAsStream(modelResource, ModelResourceIoUtilities.getResourceSubDirWithSeparator("") + name + ".xml");
        if (is != null) {
          Document doc = XMLUtilities.read(is);
          parentInfo = new ModelResourceInfo(doc);
          classToInfoMap.put(parentKey, parentInfo);
        } else {
          // Acceptable — classes like Biped don't have class infos
          classToInfoMap.put(parentKey, null);
        }
      } catch (Exception e) {
        Logger.severe("Failed to parse class info for " + name + ": " + e);
        classToInfoMap.put(parentKey, null);
      }
    }
    if (parentInfo != null) {
      if (parentKey.equals(key)) {
        return parentInfo;
      }
      ModelResourceInfo subResource = parentInfo.getSubResource(resourceName);
      if (subResource == null) {
        Logger.severe("Failed to find a resource for " + modelResource + " : " + resourceName);
      }
      classToInfoMap.put(key, subResource);
      return subResource;
    }
    return null;
  }

  public static AxisAlignedBox getBoundingBox(Class<?> modelResource, String resourceName) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, resourceName);
    if (info != null) {
      return info.getBoundingBox();
    }
    //TODO: implement better solution for getting bounding boxes for general resources (like Swimmer, Flyer, etc.)
    if (modelResource != null) {
      if (SwimmerResource.class.isAssignableFrom(modelResource)) {
        return new AxisAlignedBox(new Point3(-.5, -.5, -.5), new Point3(.5, .5, .5));
      }
    }
    return new AxisAlignedBox(new Point3(-.5, 0, -.5), new Point3(.5, 1, .5));
  }

  public static AxisAlignedBox getBoundingBox(Class<?> modelResource) {
    return getBoundingBox(modelResource, null);
  }

  public static AffineMatrix4x4 getDefaultInitialTransform(Class<?> modelResource) {
    AxisAlignedBox bbox = getBoundingBox(modelResource);
    if ((bbox != null) && !bbox.isNaN()) {
      boolean placeOnGround = getPlaceOnGround(modelResource);
      if (placeOnGround) {
        return AffineMatrix4x4.createTranslation(0, -bbox.getYMinimum(), 0);
      }
    }
    return AffineMatrix4x4.IDENTITY;
  }

  public static boolean getPlaceOnGround(Class<?> modelResource, String resourceName) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, resourceName);
    if (info != null) {
      return info.getPlaceOnGround();
    }
    //TODO: implement better solution for getting placeOnGround for general resources (like Swimmer, Flyer, etc.)
    if (modelResource != null) {
      if (SwimmerResource.class.isAssignableFrom(modelResource)) {
        return true;
      }
    }
    return false;
  }

  public static boolean getPlaceOnGround(Class<?> modelResource) {
    return getPlaceOnGround(modelResource, null);
  }

  /*private*/
  protected static String getModelName(Class<?> modelResource, String resourceName, Locale locale) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, resourceName);
    if (info != null) {
      if (locale == null) {
        return info.getModelName();
      } else {
        String packageName = modelResource.getPackage().getName();
        return findLocalizedText(getClassNameLocalizationBundleName(), packageName + "." + info.getModelName(), locale);
      }
    }
    return null;
  }

  public static String getModelClassName(Class<?> modelResource, String resourceName, Locale locale) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, null);
    String className;
    String packageName = modelResource.getPackage().getName();
    if (info != null) {
      className = info.getModelName();
    } else {
      className = modelResource.getSimpleName().replace("Resource", "");
    }

    if (locale == null) {
      return className;
    } else {
      String localizedText = findLocalizedText(getClassNameLocalizationBundleName(), packageName + "." + className, locale);
      if (localizedText == null) {
        localizedText = className;
      }
      return localizedText;
    }
  }

  private static String[] getLocalizedTags(String[] tags, String localizerBundleName, Locale locale, boolean acceptNull) {
    //    if( Locale.ENGLISH.getLanguage().equals( locale.getLanguage() ) ) {
    List<String> localizedTags = Lists.newArrayList();
    for (String tag : tags) {
      String[] splitTags = tag.split(":");
      StringBuilder finalTag = new StringBuilder();
      for (int i = 0; i < splitTags.length; i++) {
        String t = splitTags[i];
        boolean hasStar = t.startsWith("*");
        String stringToUse;
        if (hasStar) {
          stringToUse = t.substring(1);
        } else {
          stringToUse = t;
        }
        String localizationKey = makeLocalizationKey(stringToUse);
        String localizedTag = findLocalizedText(localizerBundleName, localizationKey, locale);
        if (acceptNull && (localizedTag == null)) {
          localizedTag = stringToUse;
        }
        if (localizedTag != null) {
          if (i > 0) {
            finalTag.append(":");
          }
          if (hasStar) {
            finalTag.append("*");
          }
          finalTag.append(localizedTag);
        }
      }
      if (!finalTag.isEmpty()) {
        localizedTags.add(finalTag.toString());
      }
    }
    return localizedTags.toArray(new String[localizedTags.size()]);
    //    } else {
    //      return new String[ 0 ];
    //    }
  }

  public static String[] getTags(Class<?> modelResource, String resourceName, Locale locale) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, resourceName);
    if (info != null) {
      if (locale == null) {
        return info.getTags();
      } else {
        boolean acceptNull = locale.getLanguage().equals("en");
        return getLocalizedTags(info.getTags(), getTagsLocalizationBundleName(), locale, acceptNull);
      }
    } else {
      return null;
    }
  }

  public static String[] getGroupTags(Class<?> modelResource, String resourceName, Locale locale) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, resourceName);
    if (info != null) {
      if ((locale == null) || true) {
        return info.getGroupTags();
      } else {
        return getLocalizedTags(info.getGroupTags(), getGroupTagsLocalizationBundleName(), locale, true);
      }
    } else {
      return null;
    }
  }

  public static String getLocalizedTag(String tag, Locale locale) {
    if (locale == null) {
      return tag;
    }
    if (tag.contains(" ")) {
      tag = tag.replace(" ", "_");
    }
    String result = findLocalizedText(getTagsLocalizationBundleName(), tag, locale);
    if (result != null) {
      return result;
    } else {
      Logger.severe("No localization for gallery tag '" + tag + "' for locale " + locale);
      return tag;
    }
  }

  public static String[] getThemeTags(Class<?> modelResource, String resourceName, Locale locale) {
    ModelResourceInfo info = getModelResourceInfo(modelResource, resourceName);
    if (info != null) {
      if ((locale == null) || true) {
        return info.getThemeTags();
      } else {
        return getLocalizedTags(info.getThemeTags(), getThemeTagsLocalizationBundleName(), locale, true);
      }
    } else {
      return null;
    }
  }
}
