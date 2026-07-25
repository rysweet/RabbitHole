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

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.javax.swing.option.Dialogs;
import org.alice.ide.ProjectStack;
import org.alice.ide.ast.type.croquet.ImportTypeWizard;
import org.alice.ide.ast.type.merge.core.ImportDependencyResolver;
import org.alice.ide.ast.type.merge.core.MergeUtilities;
import org.alice.nonfree.NebulousIde;
import org.alice.stageide.ast.declaration.AddResourceKeyManagedFieldComposite;
import org.alice.stageide.modelresource.ClassResourceKey;
import org.alice.stageide.modelresource.EnumConstantResourceKey;
import org.alice.stageide.modelresource.ResourceKey;
import org.lgna.common.Resource;
import org.lgna.croquet.Application;
import org.lgna.croquet.Operation;
import org.lgna.croquet.SingleThreadIteratingOperation;
import org.lgna.croquet.history.UserActivity;
import org.lgna.project.Project;
import org.lgna.project.VersionNotSupportedException;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.io.IoUtilities;
import org.lgna.project.io.TypeResourcesPair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Dennis Cosgrove
 */
public abstract class ResourceKeyUriIteratingOperation extends SingleThreadIteratingOperation {
  public static ResourceKeyUriIteratingOperation getInstance(ResourceKey resourceKey, Class<?> thingCls, URI uri) {
    ResourceKeyUriIteratingOperation rv;
    if (resourceKey != null) {
      if (resourceKey instanceof ClassResourceKey) {
        //        org.alice.stageide.modelresource.ClassResourceKey classResourceKey = (org.alice.stageide.modelresource.ClassResourceKey)resourceKey;
        rv = ClassResourceKeyUriIteratingOperation.getInstance();
      } else if (resourceKey instanceof EnumConstantResourceKey) {
        rv = EnumConstantResourceKeyUriIteratingOperation.getInstance();
      } else if (NebulousIde.nonfree.isInstanceOfPersonResourceKey(resourceKey)) {
        rv = NebulousIde.nonfree.getPersonResourceKeyUriIteratingOperation();
      } else {
        rv = null;
      }
    } else {
      rv = ThingClsUriIteratingOperation.getInstance();
    }
    if (rv != null) {
      rv.setResourceKeyThingClsAndUri(resourceKey, thingCls, uri);
    }
    return rv;
  }

  public ResourceKeyUriIteratingOperation(UUID migrationId) {
    super(Application.PROJECT_GROUP, migrationId);
  }

  public ResourceKey getResourceKey() {
    return this.resourceKey;
  }

  public Class<?> getThingCls() {
    return this.thingCls;
  }

  public URI getUri() {
    return this.uri;
  }

  private void setResourceKeyThingClsAndUri(ResourceKey resourceKey, Class<?> thingCls, URI uri) {
    this.resourceKey = resourceKey;
    this.thingCls = thingCls;
    this.uri = uri;
  }

  protected abstract int getStepCount();

  @Override
  protected boolean hasNext(List<UserActivity> finishedSteps) {
    return finishedSteps.size() < this.getStepCount();
  }

  Operation getAddResourceKeyManagedFieldCompositeOperation(EnumConstantResourceKey enumConstantResourceKey) {
    return AddResourceKeyManagedFieldComposite.getInstance().
        getLaunchOperationToCreateValue(enumConstantResourceKey, false);
  }

  protected Operation getMergeTypeOperation() {
    TypeResourcesPair typeResourcesPair;
    try {
      typeResourcesPair = IoUtilities.readType(new File(this.uri));
    } catch (IOException ioe) {
      typeResourcesPair = null;
      Logger.throwable(ioe, this.uri);
    } catch (VersionNotSupportedException vnse) {
      typeResourcesPair = null;
      Logger.throwable(vnse, this.uri);
    }
    if (typeResourcesPair != null) {
      NamedUserType importedRootType = typeResourcesPair.getType();
      if (!confirmMissingDependencies(importedRootType)) {
        return null;
      }
      Set<Resource> importedResources = typeResourcesPair.getResources();
      NamedUserType srcType = importedRootType;
      NamedUserType dstType = MergeUtilities.findMatchingTypeInExistingTypes(srcType);
      ImportTypeWizard wizard = new ImportTypeWizard(this.uri, importedRootType, importedResources, srcType, dstType);
      return wizard.getLaunchOperation();
    } else {
      return null;
    }
  }

  /**
   * When the imported type declares bounded dependencies on user types that are
   * not present in the current project (nor built-in story-API types), prompt
   * the author to continue (importing anyway, leaving those references
   * unresolved) or cancel. Returns {@code true} to proceed, {@code false} to
   * cancel. When no project is active or nothing is missing, proceeds silently.
   */
  private static boolean confirmMissingDependencies(NamedUserType importedRootType) {
    Project project = ProjectStack.peekProject();
    Set<String> availableTypeNames = new HashSet<>();
    if (project != null) {
      for (NamedUserType existingType : project.getNamedUserTypes()) {
        String name = existingType.getName();
        if (name != null) {
          availableTypeNames.add(name);
        }
      }
    }
    Set<String> missing = ImportDependencyResolver.findMissingDependencyNames(importedRootType, availableTypeNames);
    if (missing.isEmpty()) {
      return true;
    }
    String message = "The class \"" + importedRootType.getName()
        + "\" depends on types that are not present in this project:\n    "
        + String.join(", ", missing)
        + "\n\nImport anyway? Those references will remain unresolved until the missing classes are added.";
    return Dialogs.confirm("Import: missing dependencies", message);
  }

  @Override
  protected void handleSuccessfulCompletionOfSubModels(UserActivity activity) {
    super.handleSuccessfulCompletionOfSubModels(activity);
    this.resourceKey = null;
  }

  protected ResourceKey resourceKey;
  protected Class<?> thingCls;
  private URI uri;
}
