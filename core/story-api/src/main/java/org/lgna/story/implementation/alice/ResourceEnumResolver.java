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

import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import org.lgna.story.resources.ModelResource;

import java.util.Map;

/**
 * Enum naming conventions and resource name resolution.
 * Extracted from AliceResourceUtilities to separate concerns.
 *
 * @see AliceResourceUtilities
 */
class ResourceEnumResolver {

  private static final Map<String, ResourceNames> resourceIdentifierToResourceNamesMap = Maps.newHashMap();

  static final class ResourceNames {
    public final String visualName;
    public final String textureName;

    public ResourceNames(String visualName, String textureName) {
      this.visualName = visualName;
      this.textureName = textureName;
    }
  }

  private ResourceEnumResolver() {
    throw new AssertionError();
  }

  static String enumToCamelCase(String enumName, boolean startWithLowerCase) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < enumName.length(); i++) {
      if (i == 0) {
        if (startWithLowerCase) {
          sb.append(Character.toLowerCase(enumName.charAt(i)));
        } else {
          sb.append(Character.toUpperCase(enumName.charAt(i)));
        }
      } else if (enumName.charAt(i - 1) == '_') {
        sb.append(Character.toUpperCase(enumName.charAt(i)));
      } else if (enumName.charAt(i) != '_') {
        sb.append(Character.toLowerCase(enumName.charAt(i)));
      }
    }
    return sb.toString();
  }

  static String enumToCamelCase(String enumName) {
    return enumToCamelCase(enumName, false);
  }

  static String camelCaseToEnum(String name) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      if ((i != 0) && Character.isUpperCase(name.charAt(i))) {
        sb.append('_');
      }
      sb.append(Character.toUpperCase(name.charAt(i)));
    }
    return sb.toString();
  }

  static boolean isEnumName(String name) {
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if ((c != '_') && !(Character.isUpperCase(c) || Character.isDigit(c))) {
        return false;
      }
    }
    return true;
  }

  static String makeEnumName(String name) {
    if (isEnumName(name)) {
      return name;
    }
    if (name.contains("_")) {
      return name.toUpperCase();
    } else {
      return camelCaseToEnum(name);
    }
  }

  static String makeLocalizationKey(String key) {
    return key.replace(' ', '_');
  }

  static String arrayToEnum(String[] nameArray, int start, int end) {
    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;
    for (int i = start; i < end; i++) {
      if (!nameArray[i].isEmpty()) {
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append("_");
        }
        sb.append(nameArray[i].toUpperCase());
      }
    }
    return sb.toString();
  }

  static String getDefaultTextureEnumName(String resourceName) {
    return "DEFAULT";
  }

  /**
   * Visual and Texture info is encoded into the enumeration like this: public
   * enum BaseVisualName { TEXTURE_NAME_1, TEXTURE_NAME_2,
   * DIFFERENT_VISUAL_NAME_TEXTURE_NAME_1,
   * DIFFERENT_VISUAL_NAME_TEXTURE_NAME_2 }
   *
   * Both 'BaseVisualName' and DIFFERENT_VISUAL_NAME are potentially the names
   * of visual resources. If the resource uses the base visual, then the enum
   * name is just the name of the texture (like the entries TEXTURE_NAME_1 and
   * TEXTURE_NAME_2) If the resource uses a different visual resource, then
   * the visual resource name is the first half of the enum constant (like the
   * entries DIFFERENT_VISUAL_NAME_TEXTURE_NAME_1 and
   * DIFFERENT_VISUAL_NAME_TEXTURE_NAME_2)
   **/
  private static void findAndStoreResourceNames(ModelResource resource, String resourceName) {
    String[] splitName = resourceName.split("_");
    StringBuilder modelName = new StringBuilder();
    String visualName = AliceResourceClassUtilities.getAliceClassName(resource.getClass().getSimpleName());
    String textureName = resourceName;

    boolean found = false;
    if (ResourceTextureManager.checkVisualAndTextureName(resource, visualName, textureName)) {
      found = true;
    } else if (ResourceTextureManager.checkVisualAndTextureName(resource, enumToCamelCase(resourceName), "")) {
      visualName = enumToCamelCase(resourceName);
      textureName = "";
      found = true;
    } else {
      for (int i = 0; i < splitName.length; i++) {
        if (!splitName[i].isEmpty()) {
          if (i != 0) {
            modelName.append("_");
          }
          modelName.append(splitName[i]);
          visualName = enumToCamelCase(modelName.toString());
          textureName = arrayToEnum(splitName, i + 1, splitName.length);
          if (ResourceTextureManager.checkVisualAndTextureName(resource, visualName, textureName)) {
            found = true;
            break;
          }
        }
      }
    }
    if (!found) {
      return;
    }
    String identifier = resource.identifierFor(resourceName);
    resourceIdentifierToResourceNamesMap.put(identifier, new ResourceNames(visualName, textureName));
  }

  static String getModelNameFromClassAndResource(ModelResource resource, String resourceName) {
    if (resourceName == null) {
      return AliceResourceUtilities.getName(resource.getClass());
    }
    String identifier = resource.identifierFor(resourceName);
    ResourceNames names = resourceIdentifierToResourceNamesMap.get(identifier);
    if (names == null) {
      findAndStoreResourceNames(resource, resourceName);
      names = resourceIdentifierToResourceNamesMap.get(identifier);
    }
    if (names != null) {
      return names.visualName;
    } else {
      Logger.warning("Failed to find resource names for '" + resource + "' and '" + resourceName + "'");
      return null;
    }
  }

  static String getTextureNameFromClassAndResource(ModelResource resource, String resourceName) {
    if (resourceName == null) {
      return null;
    }
    String identifier = resource.identifierFor(resourceName);
    ResourceNames resourceNames = resourceIdentifierToResourceNamesMap.get(identifier);
    if (resourceNames == null) {
      findAndStoreResourceNames(resource, resourceName);
      resourceNames = resourceIdentifierToResourceNamesMap.get(identifier);
    }
    if (resourceNames != null) {
      return resourceNames.textureName;
    } else {
      Logger.severe(resource, resourceName, identifier);
      return null;
    }
  }

  static String getVisualResourceName(ModelResource resource) {
    return getModelNameFromClassAndResource(resource, resource.toString());
  }

  static String getTextureResourceName(ModelResource resource) {
    return getTextureNameFromClassAndResource(resource, resource.toString());
  }
}
