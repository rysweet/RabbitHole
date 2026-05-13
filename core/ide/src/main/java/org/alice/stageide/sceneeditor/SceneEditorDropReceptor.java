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
package org.alice.stageide.sceneeditor;

import org.alice.ide.croquet.models.gallerybrowser.GalleryDragModel;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.stageide.sceneeditor.draganddrop.SceneDropSite;
import org.lgna.croquet.AbstractDropReceptor;
import org.lgna.croquet.DragModel;
import org.lgna.croquet.DropSite;
import org.lgna.croquet.Triggerable;
import org.lgna.croquet.history.DragStep;
import org.lgna.croquet.views.DragComponent;
import org.lgna.croquet.views.SwingComponentView;
import org.lgna.croquet.views.TrackableShape;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Drop receptor for the scene editor looking glass.
 * Extracted from StorytellingSceneEditor inner class (issue #528).
 */
class SceneEditorDropReceptor extends AbstractDropReceptor {
  private final StorytellingSceneEditor editor;
  private boolean overLookingGlass = false;

  SceneEditorDropReceptor(StorytellingSceneEditor editor) {
    this.editor = editor;
  }

  @Override
  public boolean isPotentiallyAcceptingOf(DragModel dragModel) {
    return dragModel instanceof GalleryDragModel;
  }

  @Override
  public void dragStarted(DragStep step) {
    DragComponent dragSource = step.getDragSource();
    dragSource.showDragProxy();
  }

  @Override
  public void dragEntered(DragStep dragAndDropContext) {
  }

  private boolean isDropLocationOverLookingGlass(DragStep dragAndDropContext) {
    MouseEvent eSource = dragAndDropContext.getLatestMouseEvent();
    Point pointInLookingGlass = SwingUtilities.convertPoint(eSource.getComponent(), eSource.getPoint(), editor.lookingGlassPanel.getAwtComponent());
    return editor.lookingGlassPanel.getAwtComponent().contains(pointInLookingGlass);
  }

  @Override
  public DropSite dragUpdated(DragStep dragStep) {
    if (isDropLocationOverLookingGlass(dragStep)) {
      if (!overLookingGlass) {
        overLookingGlass = true;
        editor.globalDragAdapter.dragEntered(dragStep);
      }
      editor.globalDragAdapter.dragUpdated(dragStep);
    } else {
      if (overLookingGlass) {
        overLookingGlass = false;
        editor.globalDragAdapter.dragExited(dragStep);
      }
    }
    AffineMatrix4x4 t = editor.globalDragAdapter.getDropTargetTransformation();
    return t != null ? new SceneDropSite(t) : null;
  }

  @Override
  protected Triggerable dragDroppedPostRejectorCheck(DragStep dragStep) {
    if (isDropLocationOverLookingGlass(dragStep)) {
      DropSite dropSite = new SceneDropSite(editor.globalDragAdapter.getDropTargetTransformation());
      return dragStep.getModel().getDropOperation(dragStep, dropSite);
    }
    return null;
  }

  @Override
  public void dragExited(DragStep dragAndDropContext, boolean isDropRecipient) {
  }

  @Override
  public void dragStopped(DragStep dragStep) {
    editor.globalDragAdapter.dragExited(dragStep);
  }

  @Override
  public TrackableShape getTrackableShape(DropSite potentialDropSite) {
    return editor;
  }

  @Override
  public SwingComponentView<?> getViewController() {
    return editor;
  }
}
