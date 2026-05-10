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
package org.lgna.project.migration;

import org.lgna.project.ProjectVersion;
import org.lgna.project.Version;
import org.lgna.project.migration.ast.ChangeDeclaringClassForAxesSetVehicle;
import org.lgna.project.migration.ast.CompoundMigration;
import org.lgna.project.migration.ast.EventAstMigration;
import org.lgna.project.migration.ast.MethodMovedToSuperclass;
import org.lgna.project.migration.ast.MouseClickAstMigration;
import org.lgna.project.migration.ast.RemoveGetMySceneMethodFromProgramTypeAstMigration;
import org.lgna.project.migration.ast.ReplaceTypeWithResourcedForm;
import org.lgna.project.migration.ast.UnderscoreFieldAccessAstMigration;
import org.lgna.story.SJointedModel;
import org.lgna.story.SModel;
import org.lgna.story.Say;
import org.lgna.story.Think;
import org.lgna.story.resources.prop.FirTreeTrunkResource;
import org.lgna.story.resources.prop.IceFloeResource;

/**
 * @author Dennis Cosgrove
 */
public class ProjectMigrationManager extends AbstractMigrationManager {
  // @formatter:off
  private final AstMigration[] astMigrations = {
      new MouseClickAstMigration(new Version("3.1.39.0.0")),
      new UnderscoreFieldAccessAstMigration(new Version("3.1.68.0.0")),
      new EventAstMigration(new Version("3.1.70.0.0")),
      new RemoveGetMySceneMethodFromProgramTypeAstMigration(new Version("3.1.72.0.0")),
      new ChangeDeclaringClassForAxesSetVehicle(new Version("3.2.113.0.0")),
      new CompoundMigration(
          new Version("3.5.0.0"),
          new MethodMovedToSuperclass(SJointedModel.class, SModel.class, "say", String.class, Say.Detail[].class),
          new MethodMovedToSuperclass(SJointedModel.class, SModel.class, "think", String.class, Think.Detail[].class)
      ),
      new CompoundMigration(
          new Version("3.6.0.0"),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunk", FirTreeTrunkResource.DEFAULT),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkTall", FirTreeTrunkResource.TALL),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkSnow", FirTreeTrunkResource.SNOW),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkSnowTall", FirTreeTrunkResource.SNOW_TALL),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkMirror", FirTreeTrunkResource.MIRROR),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkTallMirror", FirTreeTrunkResource.TALL_MIRROR),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkSnowMirror", FirTreeTrunkResource.SNOW_MIRROR),
          new ReplaceTypeWithResourcedForm<>("FirTreeTrunkSnowTallMirror", FirTreeTrunkResource.SNOW_TALL_MIRROR),
          // Unlike the trees, an ice floe model that does not take a resource does not have a distinct name to pick the resource.
          // Some worlds may thus end up with the floe1 resource instead of floe2, but they will work, and that can be changed by the user.
          new ReplaceTypeWithResourcedForm<>("IceFloe", IceFloeResource.ICE_FLOE1)
      )
  };

  // @formatter:on

  private static class SingletonHolder {
    private static ProjectMigrationManager instance = new ProjectMigrationManager();
  }

  public static ProjectMigrationManager getInstance() {
    return SingletonHolder.instance;
  }

  private ProjectMigrationManager() {
    super(ProjectVersion.getCurrentVersion());
  }

  @Override
  protected TextMigration[] getTextMigrations() {
    return TextMigrationRegistry.createAll();
  }

  @Override
  protected AstMigration[] getAstMigrations() {
    return this.astMigrations;
  }
}
