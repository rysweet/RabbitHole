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
package org.alice.stageide.gallerybrowser.uri;

import edu.cmu.cs.dennisc.java.io.FileUtilities;
import edu.cmu.cs.dennisc.java.util.InitializingIfAbsentMap;
import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.ResourceBundleUtilities;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.java.util.zip.ZipUtilities;
import org.alice.ide.ast.export.type.*;
import org.alice.ide.ast.type.merge.croquet.MembersToolPalette;
import org.alice.ide.croquet.models.ui.formatter.FormatterState;
import org.alice.ide.formatter.Formatter;
import org.alice.ide.icons.IconFactoryManager;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.nonfree.NebulousIde;
import org.alice.stageide.modelresource.*;
import org.lgna.croquet.DropSite;
import org.lgna.croquet.SingleSelectTreeState;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.history.DragStep;
import org.lgna.croquet.icon.EmptyIconFactory;
import org.lgna.croquet.icon.IconFactory;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.JavaType;
import org.lgna.story.resources.ModelResource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Dennis Cosgrove
 */
public final class UriGalleryDragModel extends ResourceGalleryDragModel {
  private static InitializingIfAbsentMap<URI, UriGalleryDragModel> map = Maps.newInitializingIfAbsentHashMap();

  public static UriGalleryDragModel getInstance(URI uri) {
    return map.get(uri, UriGalleryDragModel::new);
  }

  private final URI uri;
  private String text;
  private Map<String, byte[]> mapFilenameToExtractedData;
  private TypeSummary typeSummary;
  private InstanceCreatorKey resourceKey;
  private Class<?> thingCls;
  private IconFactory iconFactory;

  private UriGalleryDragModel(URI uri) {
    super(UUID.fromString("9b784c07-6857-4f3f-83c4-ef6f2334c62a"));
    this.uri = uri;
  }

  private Map<String, byte[]> getFilenameToExtractedData() {
    if (this.mapFilenameToExtractedData == null) {
      try {
        this.mapFilenameToExtractedData = ZipUtilities.extract(new File(this.uri));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
    return this.mapFilenameToExtractedData;
  }

  private TypeSummary getTypeSummary() {
    if (this.typeSummary == null) {
      Map<String, byte[]> mapFilenameToExtractedData = this.getFilenameToExtractedData();
      try {
        this.typeSummary = TypeSummaryReader.read(mapFilenameToExtractedData);
      } catch (VersionNotSupportedException vnse) {
        throw new RuntimeException(vnse);
      }
    }
    return this.typeSummary;
  }

  public InstanceCreatorKey getResourceKey() {
    if (this.resourceKey == null) {
      try {
        TypeSummary typeSummary = this.getTypeSummary();
        if (typeSummary != null) {
          ResourceInfo resourceInfo = typeSummary.getResourceInfo();
          if (resourceInfo != null) {
            String resourceClassName = resourceInfo.getClassName();
            String resourceFieldName = resourceInfo.getFieldName();
            Class<? extends ModelResource> resourceCls = (Class<? extends ModelResource>) Class.forName(resourceClassName);
            if (resourceFieldName != null) {
              Field fld = resourceCls.getField(resourceFieldName);
              Enum<? extends ModelResource> enumConstant = (Enum<? extends ModelResource>) fld.get(null);
              this.resourceKey = new EnumConstantResourceKey(enumConstant);
            } else {
              if (NebulousIde.nonfree.isPersonResourceAssignableFrom(resourceCls)) {
                this.resourceKey = NebulousIde.nonfree.getPersonResourceKeyInstanceForResourceClass(resourceCls);
              } else {
                this.resourceKey = new ClassResourceKey(resourceCls);
              }
            }
          }
        } else {
          Logger.severe(this);
        }
      } catch (Throwable t) {
        Logger.throwable(t, this);
      }
    }
    return this.resourceKey;
  }

  private Class<?> getThingCls() {
    if (this.thingCls == null) {
      try {
        TypeSummary typeSummary = this.getTypeSummary();
        if (typeSummary != null) {
          List<String> hierarchyClsNames = typeSummary.getHierarchyClassNames();
          if (!hierarchyClsNames.isEmpty()) {
            String clsName = hierarchyClsNames.getLast();
            this.thingCls = Class.forName(clsName);
          }
        } else {
          Logger.severe(this);
        }
      } catch (Throwable t) {
        Logger.throwable(t, this);
      }
    }
    return this.thingCls;
  }

  private void appendStartIfNecessary(StringBuilder sb) {
    File file = new File(this.uri);
    UriGalleryDragModelLogic.appendStartIfNecessary(sb, file.exists() ? file.getName() : null);
  }

  public String getTypeSummaryToolTipText() {
    File file = new File(this.uri);
    return UriGalleryDragModelLogic.buildTypeSummaryToolTipText(getTypeSummary(), file.exists() ? file.getName() : null);
  }

  @Override
  protected void localize() {
    TypeSummary typeSummary = getTypeSummary();
    String typeName = typeSummary != null ? typeSummary.getTypeName() : "???";

    InstanceCreatorKey resourceKey = this.getResourceKey();
    Formatter formatter = FormatterState.getInstance().getValue();
    String fallbackText = formatter.getNewFormat().formatted(typeName, "");
    File file = new File(this.uri);
    String fileName = file.exists() ? file.getName() : null;
    String baseName = file.exists() ? FileUtilities.getBaseName(file) : null;
    String fromFormat = ResourceBundleUtilities.getStringForKey("MembersToolPalette.fromImportHeader", MembersToolPalette.class);
    fromFormat = fromFormat.replaceFirst(":", "");
    if (fileName != null) {
      fromFormat = fromFormat.replaceFirst("</filename/>", fileName);
    }
    this.text = UriGalleryDragModelLogic.createLocalizedText(typeName,
        resourceKey != null ? resourceKey.getLocalizedCreationText() : null,
        (resourceKey != null) && (resourceKey.getModelResourceCls() != null) && resourceKey.getModelResourceCls().isInterface(),
        fallbackText,
        fileName,
        baseName,
        fromFormat);
  }

  @Override
  public String getText() {
    return this.text;
  }

  @Override
  public AxisAlignedBox getBoundingBox() {
    InstanceCreatorKey resourceKey = getResourceKey();
    return resourceKey != null ? resourceKey.getBoundingBox() : null;
  }

  @Override
  public boolean placeOnGround() {
    InstanceCreatorKey resourceKey = this.getResourceKey();
    return resourceKey != null && resourceKey.getPlaceOnGround();
  }

  @Override
  public List<ResourceNode> getNodeChildren() {
    ResourceKey resourceKey = this.getResourceKey();
    if (resourceKey instanceof ClassResourceKey classResourceKey) {
      Class<? extends ModelResource> modelResourceClass = classResourceKey.getModelResourceCls();
      Class<?> thingCls = this.getThingCls();
      if (modelResourceClass != null) {
        if (modelResourceClass.isEnum()) {
          List<ResourceNode> rv = Lists.newLinkedList();
          for (ModelResource modelResource : modelResourceClass.getEnumConstants()) {
            rv.add(new UriBasedResourceNode(new EnumConstantResourceKey((Enum) modelResource), thingCls, this.uri));
          }
          return rv;
        } else {
          return Collections.emptyList();
        }
      } else {
        return Collections.emptyList();
      }
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public Triggerable getLeftButtonClickOperation(SingleSelectTreeState<ResourceNode> controller) {
    ResourceKey resourceKey = this.getResourceKey();
    Class<?> thingCls = this.getThingCls();
    return ResourceKeyUriIteratingOperation.getInstance(resourceKey, thingCls, this.uri);
  }

  @Override
  public Triggerable getDropOperation(DragStep step, DropSite dropSite) {
    ResourceKey resourceKey = this.getResourceKey();
    if (resourceKey instanceof EnumConstantResourceKey) {
      return this.getLeftButtonClickOperation(null);
    } else if (resourceKey instanceof ClassResourceKey classResourceKey) {
      if (classResourceKey.isLeaf()) {
        //should not happen
        return null;
      } else {
        return new AddFieldCascade(this, dropSite);
      }
    } else if (NebulousIde.nonfree.isInstanceOfPersonResourceKey(resourceKey)) {
      return this.getLeftButtonClickOperation(null);
    } else {
      Class<?> thingCls = this.getThingCls();
      if (thingCls != null) {
        return this.getLeftButtonClickOperation(null);
      } else {
        return null;
      }
    }
  }

  @Override
  public IconFactory getIconFactory() {
    if (this.iconFactory == null) {
      IconFactory base;
      ResourceKey resourceKey = this.getResourceKey();
      if (resourceKey != null) {
        base = resourceKey.getIconFactory();
      } else {
        Class<?> thingCls = this.getThingCls();
        if (thingCls != null) {
          base = IconFactoryManager.getIconFactoryForType(JavaType.getInstance(thingCls));
        } else {
          base = EmptyIconFactory.getInstance();
        }
      }
      this.iconFactory = base;
    }
    return this.iconFactory;
  }

  @Override
  protected void appendRepr(StringBuilder sb) {
    super.appendRepr(sb);
    sb.append(this.uri);
  }
}
