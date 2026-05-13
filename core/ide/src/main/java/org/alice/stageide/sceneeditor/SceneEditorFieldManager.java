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

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.render.gl.GlrRenderFactory;
import edu.cmu.cs.dennisc.scenegraph.AbstractCamera;
import edu.cmu.cs.dennisc.scenegraph.Element;
import org.alice.ide.ReasonToDisableSomeAmountOfRendering;
import org.alice.ide.instancefactory.InstanceFactory;
import org.alice.ide.instancefactory.ThisFieldAccessFactory;
import org.alice.ide.instancefactory.croquet.InstanceFactoryState;
import org.alice.interact.InputState;
import org.alice.interact.event.SelectionEvent;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.AxisAlignedBox;
import org.alice.stageide.StageIDE;
import org.alice.stageide.oneshot.DynamicOneShotMenuModel;
import org.alice.stageide.sceneeditor.side.SideComposite;
import org.alice.stageide.sceneeditor.viewmanager.*;
import org.lgna.croquet.RefreshableDataSingleSelectListState;
import org.lgna.croquet.triggers.InputEventTrigger;
import org.lgna.project.ast.*;
import org.lgna.story.*;
import org.lgna.story.implementation.*;

import java.util.Map;

/**
 * Consolidates selection/manipulator wiring, camera switching, marker handling,
 * right-click menu, show/hide lifecycle, rendering control, and code generation
 * delegation — all extracted from StorytellingSceneEditor (issue #528).
 */
class SceneEditorFieldManager {

  final StorytellingSceneEditor editor;
  final SceneFieldCodeGenerator codeGenerator;

  SceneEditorFieldManager(StorytellingSceneEditor editor) {
    this.editor = editor;
    this.codeGenerator = new SceneFieldCodeGenerator(editor);
  }

  // ── Selection and manipulator wiring ───────────────────────────────

  void setSelectedFieldOnManipulator(UserField field) {
    if (editor.globalDragAdapter != null) {
      SThing selectedEntity = editor.getInstanceInJavaVMForField(field, SThing.class);
      TransformableImp transImp = null;
      if (selectedEntity != null) {
        EntityImp imp = selectedEntity.getImplementation();
        if (imp instanceof TransformableImp transformableImp) {
          transImp = transformableImp;
        }
      }
      editor.globalDragAdapter.setSelectedImplementation(transImp);
    }
  }

  void setSelectedExpressionOnManipulator(Expression expression) {
    if (editor.globalDragAdapter != null) {
      SThing selectedEntity = editor.getInstanceInJavaVMForExpression(expression, SThing.class);
      AbstractTransformableImp transImp = null;
      if (selectedEntity != null) {
        EntityImp imp = selectedEntity.getImplementation();
        if (imp instanceof AbstractTransformableImp transformableImp) {
          transImp = transformableImp;
        }
      }
      editor.globalDragAdapter.setSelectedImplementation(transImp);
    }
  }

  void setSelectedInstance(InstanceFactory instanceFactory) {
    Expression expression = instanceFactory != null ? instanceFactory.createExpression() : null;
    if (expression instanceof FieldAccess fa) {
      AbstractField field = fa.field.getValue();
      if (field instanceof UserField uf) {
        editor.setSelectedField(uf.getDeclaringType(), uf);
      }
    } else if (expression instanceof MethodInvocation) {
      editor.setSelectedExpression(expression);
    } else if (expression instanceof ArrayAccess) {
      editor.setSelectedExpression(expression);
    } else if (expression instanceof ThisExpression) {
      UserField uf = editor.getActiveSceneField();
      if (uf != null) {
        editor.setSelectedField(uf.getDeclaringType(), uf);
      } else {
        return;
      }
    }
    editor.getPropertyPanel().setSelectedInstance(instanceFactory);
  }

  void handleManipulatorSelection(SelectionEvent e) {
    EntityImp imp = e.getTransformable();
    if (imp != null) {
      UserField field = editor.getFieldForInstanceInJavaVM(imp.getAbstraction());
      if (field != null) {
        if (field.getValueType().isAssignableFrom(SCameraMarker.class)) {
          setSelectedCameraMarker(field);
        } else if (field.getValueType().isAssignableFrom(SThingMarker.class)) {
          setSelectedObjectMarker(field);
        } else {
          editor.setSelectedField(field.getDeclaringType(), field);
        }
      }
      if (imp instanceof PerspectiveCameraMarkerImp markerImp) {
        editor.globalDragAdapter.setSelectedImplementation(markerImp);
      }
    } else {
      UserField uf = editor.getActiveSceneField();
      editor.setSelectedField(uf.getDeclaringType(), uf);
    }
  }

  // ── Camera switching ───────────────────────────────────────────────

  void switchToCamera(AbstractCamera camera) {
    if (editor.onscreenRenderTarget.getSgCameraCount() != 1
            || editor.onscreenRenderTarget.getSgCameraAt(0) != camera) {
      editor.onscreenRenderTarget.clearSgCameras();
      editor.onscreenRenderTarget.addSgCamera(camera);
    }
    editor.snapGrid.setCurrentCamera(camera);
    editor.globalDragAdapter.makeCameraActive(camera);
    editor.onscreenRenderTarget.repaint();
    editor.revalidateAndRepaint();
  }

  void switchToOrthographicCamera() {
    switchToCamera(editor.orthographicCameraImp.getSgCamera());
    editor.mainCameraNavigatorWidget.setToOrthographicMode();
  }

  void switchToPerspectiveCamera(AbstractCamera sgCamera) {
    switchToCamera(sgCamera);
    editor.mainCameraNavigatorWidget.setToPerspectiveMode();
  }

  // ── Marker handling ────────────────────────────────────────────────

  void handleCameraMarkerFieldSelection(UserField cameraMarkerField) {
    CameraMarkerImp newMarker = (CameraMarkerImp) getMarkerForField(cameraMarkerField);
    editor.globalDragAdapter.setSelectedCameraMarker(newMarker);
    MoveActiveCameraToMarkerActionOperation.getInstance().setMarkerField(cameraMarkerField);
    MoveMarkerToActiveCameraActionOperation.getInstance().setMarkerField(cameraMarkerField);
  }

  void handleObjectMarkerFieldSelection(UserField objectMarkerField) {
    ObjectMarkerImp newMarker = (ObjectMarkerImp) getMarkerForField(objectMarkerField);
    editor.globalDragAdapter.setSelectedObjectMarker(newMarker);
    MoveSelectedObjectToMarkerActionOperation.getInstance().setMarkerField(objectMarkerField);
    MoveMarkerToSelectedObjectActionOperation.getInstance().setMarkerField(objectMarkerField);
  }

  void setSelectedObjectMarker(UserField objectMarkerField) {
    RefreshableDataSingleSelectListState<UserField> markerList = SideComposite.getInstance().getObjectMarkersTab().getMarkerListState();
    markerList.setSelectedIndex(markerList.indexOf(objectMarkerField));
  }

  void setSelectedCameraMarker(UserField cameraMarkerField) {
    RefreshableDataSingleSelectListState<UserField> markerList = SideComposite.getInstance().getCameraMarkersTab().getMarkerListState();
    markerList.setSelectedIndex(markerList.indexOf(cameraMarkerField));
  }

  void handleMainCameraViewSelection() {
    StageIDE ide = StageIDE.getActiveInstance();
    InstanceFactoryState instanceFactoryState = ide.getDocumentFrame().getInstanceFactoryState();
    UserField field = editor.getSelectedField();
    if (field != editor.getActiveSceneField()) {
      instanceFactoryState.setValueTransactionlessly(ide.getInstanceFactoryForSceneField(field));
    }
  }

  // ── Right-click context menu ───────────────────────────────────────

  void showRightClickMenuForModel(InputState clickInput) {
    Element element = clickInput.getClickPickedTransformable(true);
    if (element != null) {
      EntityImp entityImp = EntityImp.getInstance(element);
      SThing entity = entityImp.getAbstraction();
      UserField field;
      if (entity != null) {
        field = editor.getFieldForInstanceInJavaVM(entity);
      } else {
        //todo: handle camera
        field = null;
      }
      if (field != null) {
        InstanceFactory instanceFactory = ThisFieldAccessFactory.getInstance(field);

        DynamicOneShotMenuModel.getInstance().getPopupPrepModel().fire(InputEventTrigger.createUserActivity(clickInput.getInputEvent()));
      } else {
        Logger.severe(entityImp);
      }
    }
  }

  // ── Show/hide lifecycle ────────────────────────────────────────────

  void showLookingGlassPanel() {
    synchronized (editor.getTreeLock()) {
      editor.addCenterComponent(editor.lookingGlassPanel);
    }
  }

  void hideLookingGlassPanel() {
    synchronized (editor.getTreeLock()) {
      editor.removeComponent(editor.lookingGlassPanel);
    }
  }

  void handleShowing() {
    GlrRenderFactory renderFactory = GlrRenderFactory.getInstance();
    renderFactory.incrementAutomaticDisplayCount();
    renderFactory.addAutomaticDisplayListener(editor.automaticDisplayListener);
    showLookingGlassPanel();
  }

  void handleHiding() {
    hideLookingGlassPanel();
    GlrRenderFactory renderFactory = GlrRenderFactory.getInstance();
    renderFactory.removeAutomaticDisplayListener(editor.automaticDisplayListener);
    renderFactory.decrementAutomaticDisplayCount();
  }

  // ── Rendering control ──────────────────────────────────────────────

  void enableRendering(ReasonToDisableSomeAmountOfRendering reasonToDisableSomeAmountOfRendering) {
    if ((reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN) || (reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.CLICK_AND_CLACK)) {
      editor.onscreenRenderTarget.setRenderingEnabled(true);
    }
  }

  void disableRendering(ReasonToDisableSomeAmountOfRendering reasonToDisableSomeAmountOfRendering) {
    if ((reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN) || (reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.CLICK_AND_CLACK)) {
      editor.onscreenRenderTarget.setRenderingEnabled(false);
    }
  }

  void preScreenCapture() {
    editor.globalDragAdapter.setHandleVisibility(false);
  }

  void postScreenCapture() {
    editor.globalDragAdapter.setHandleVisibility(true);
  }

  void setHandleVisibilityForObject(TransformableImp imp, boolean b) {
    editor.globalDragAdapter.setHandleShowingForSelectedImplementation(imp, b);
  }

  // ── Camera/marker accessor helpers ─────────────────────────────────

  AffineMatrix4x4 getTransformForNewCameraMarker() {
    return editor.movableSceneCameraImp.getAbsoluteTransformation();
  }

  AffineMatrix4x4 getTransformForNewObjectMarker() {
    EntityImp selectedImp = editor.getImplementation(editor.getSelectedField());
    if (selectedImp != null) {
      return selectedImp.getAbsoluteTransformation();
    }
    return AffineMatrix4x4.IDENTITY;
  }

  Color getColorForNewObjectMarker() {
    return MarkerUtilities.getNewObjectMarkerColor();
  }

  Color getColorForNewCameraMarker() {
    return MarkerUtilities.getNewCameraMarkerColor();
  }

  AffineMatrix4x4 getGoodPointOfViewInSceneForObject(AxisAlignedBox box) {
    throw new RuntimeException("todo");
  }

  MarkerImp getMarkerForField(UserField field) {
    Object obj = editor.getInstanceInJavaVMForField(field);
    if (obj instanceof SMarker marker) {
      return marker.getImplementation();
    }
    return null;
  }

  AbstractCamera getSgCameraForCreatingThumbnails() {
    if (editor.sceneCameraImp != null) {
      return editor.sceneCameraImp.getSgCamera();
    }
    return null;
  }

  // ── Code generation delegation ─────────────────────────────────────

  Statement getCurrentStateCodeForField(UserField field) {
    return codeGenerator.getCurrentStateCodeForField(field);
  }

  void generateCodeForSetUp(StatementListProperty bodyStatementsProperty) {
    codeGenerator.generateCodeForSetUp(bodyStatementsProperty);
  }

  Statement[] getDoStatementsForCopyField(UserField fieldToCopy, UserField newField, AffineMatrix4x4 initialTransform) {
    return codeGenerator.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform);
  }

  Statement[] getDoStatementsForAddField(UserField field, AffineMatrix4x4 initialTransform) {
    return codeGenerator.getDoStatementsForAddField(field, initialTransform);
  }

  Statement[] getUndoStatementsForAddField(UserField field) {
    return codeGenerator.getUndoStatementsForAddField(field);
  }

  Map<AbstractField, Statement> getRiders(UserField vehicle) {
    return codeGenerator.getRiders(vehicle);
  }

  Statement[] getDoStatementsForRemoveField(UserField field, Map<AbstractField, Statement> riders) {
    return codeGenerator.getDoStatementsForRemoveField(field, riders);
  }

  Statement[] getUndoStatementsForRemoveField(UserField field, Map<AbstractField, Statement> riders) {
    return codeGenerator.getUndoStatementsForRemoveField(field, riders);
  }
}
