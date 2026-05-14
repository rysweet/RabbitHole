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

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.story.resources.DynamicResource;
import org.lgna.story.resources.ModelResource;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/**
 * Filename construction and URL resolution for textures, visuals, and thumbnails.
 * Extracted from AliceResourceUtilities to separate concerns.
 *
 * @see AliceResourceUtilities
 */
class ResourceTextureManager {

  private ResourceTextureManager() {
    throw new AssertionError();
  }

  private static String createTextureBaseName(String modelName, String textureName) {
    if (modelName == null) {
      return null;
    }
    if (textureName == null) {
      textureName = "_cls";
    } else if (textureName.equalsIgnoreCase(ResourceEnumResolver.getDefaultTextureEnumName(modelName))
        || modelName.equalsIgnoreCase(ResourceEnumResolver.enumToCamelCase(textureName))
        || textureName.equalsIgnoreCase(ResourceEnumResolver.makeEnumName(modelName))) {
      textureName = "";
    } else if (!textureName.isEmpty()) {
      textureName = "_" + ResourceEnumResolver.makeEnumName(textureName);
    }
    return (modelName != null ? modelName.toLowerCase(Locale.ENGLISH) : null) + textureName;
  }

  static String getThumbnailResourceFileName(String modelName, String textureName) {
    String baseName = createTextureBaseName(modelName, textureName);
    if (baseName == null) {
      return null;
    }
    return baseName + ".png";
  }

  static String getTextureResourceFileName(String modelName, String textureName) {
    return createTextureBaseName(modelName, textureName) + "." + AliceResourceUtilities.TEXTURE_RESOURCE_EXTENSION;
  }

  static String getVisualResourceFileNameFromModelName(String modelName, String extension) {
    return modelName.toLowerCase(Locale.ENGLISH) + "." + extension;
  }

  static String getVisualResourceFileNameFromModelName(String modelName) {
    return getVisualResourceFileNameFromModelName(modelName, AliceResourceUtilities.MODEL_RESOURCE_EXTENSION);
  }

  static String getTextureResourceFileName(ModelResource resource, String resourceName) {
    String modelName = ResourceEnumResolver.getModelNameFromClassAndResource(resource, resourceName);
    String textureName = ResourceEnumResolver.getTextureNameFromClassAndResource(resource, resourceName);
    return getTextureResourceFileName(modelName, textureName);
  }

  static String getTextureResourceFileName(ModelResource resource) {
    return getTextureResourceFileName(resource, resource.toString());
  }

  static String getVisualResourceFileName(ModelResource resource, String resourceName) {
    String modelName = ResourceEnumResolver.getModelNameFromClassAndResource(resource, resourceName);
    return getVisualResourceFileNameFromModelName(modelName);
  }

  private static String getVisualResourceFileName(ModelResource resource) {
    return getVisualResourceFileName(resource, resource.toString());
  }

  static String getThumbnailResourceFileName(ModelResource resource, String resourceName) {
    String modelName = ResourceEnumResolver.getModelNameFromClassAndResource(resource, resourceName);
    if (modelName != null) {
      String textureName = ResourceEnumResolver.getTextureNameFromClassAndResource(resource, resourceName);
      return getThumbnailResourceFileName(modelName, textureName);
    } else {
      return null;
    }
  }

  static boolean checkVisualAndTextureName(ModelResource resource, String visualName, String textureName) {
    String thumbnailFileName = getThumbnailResourceFileName(visualName, textureName);
    return getThumbnailURLInternalFromFilename(resource, thumbnailFileName) != null;
  }

  private static URL getThumbnailURLInternalFromFilename(ModelResource modelResource, String thumbnailFilename) {
    return AliceResourceUtilities.getAliceResource(modelResource.getClass(),
                            getResourceSubDirWithSeparator(modelResource.getClass()) + thumbnailFilename);
  }

  private static String getResourceSubDirWithSeparator(Class<?> resource) {
    return ModelResourceIoUtilities.getResourceSubDirWithSeparator(resource.getSimpleName());
  }

  static URL getTextureURL(ModelResource resource) {
    if (resource instanceof DynamicResource dynamicResource) {
      final URI textureURI = dynamicResource.getTextureURI();
      if (textureURI == null) {
        return null;
      }
      try {
        return textureURI.toURL();
      } catch (MalformedURLException e) {
        Logger.severe("Failed to get texture URL for " + textureURI, e);
        return null;
      }
    }
    return AliceResourceUtilities.getUrl(resource, getTextureResourceFileName(resource));
  }

  static URL getVisualURL(ModelResource resource) {
    if (resource instanceof DynamicResource dynamicResource) {
      final URI visualURI = dynamicResource.getVisualURI();
      if (visualURI == null) {
        return null;
      }
      try {
        return visualURI.toURL();
      } catch (MalformedURLException e) {
        Logger.severe("Failed to get visual URL for " + visualURI, e);
        return null;
      }
    }
    return AliceResourceUtilities.getUrl(resource, getVisualResourceFileName(resource));
  }

  static URL getThumbnailURL(ModelResource modelResource, String instanceName) {
    String thumbnailName = getThumbnailResourceFileName(modelResource, instanceName);
    return getThumbnailURLInternalFromFilename(modelResource, thumbnailName);
  }

  static URL getThumbnailURL(Class<?> modelResource) {
    String thumbnailFilename = getThumbnailResourceFileName(AliceResourceUtilities.getName(modelResource), null);
    return AliceResourceUtilities.getAliceResource(modelResource, getResourceSubDirWithSeparator(modelResource) + thumbnailFilename);
  }
}
