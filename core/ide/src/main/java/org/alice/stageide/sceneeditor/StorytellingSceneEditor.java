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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import edu.cmu.cs.dennisc.animation.Animator;
import edu.cmu.cs.dennisc.animation.ClockBasedAnimator;
import edu.cmu.cs.dennisc.java.lang.SystemUtilities;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import edu.cmu.cs.dennisc.render.RenderCapabilities;
import edu.cmu.cs.dennisc.render.event.*;
import edu.cmu.cs.dennisc.render.gl.GlrRenderFactory;
import edu.cmu.cs.dennisc.scenegraph.*;
import edu.cmu.cs.dennisc.scenegraph.AsSeenBy;
import edu.cmu.cs.dennisc.scenegraph.Element;
import org.alice.ide.IDE;
import org.alice.ide.ProjectDocumentFrame;
import org.alice.ide.ReasonToDisableSomeAmountOfRendering;
import org.alice.ide.icons.Icons;
import org.alice.ide.instancefactory.InstanceFactory;
import org.alice.ide.instancefactory.ThisFieldAccessFactory;
import org.alice.ide.instancefactory.croquet.InstanceFactoryState;
import org.alice.ide.preferences.IsToolBarShowing;
import org.alice.ide.sceneeditor.AbstractSceneEditor;
import org.alice.interact.DragAdapter.CameraView;
import org.alice.interact.InputState;
import org.alice.interact.PickHint;
import org.alice.interact.condition.ClickedObjectCondition;
import org.alice.interact.condition.PickCondition;
import org.alice.interact.event.SelectionEvent;
import org.alice.interact.event.SelectionListener;
import org.alice.interact.manipulator.ManipulatorClickAdapter;
import org.alice.interact.manipulator.scenegraph.SnapGrid;
import org.alice.math.immutable.*;
import org.alice.nonfree.NebulousIde;
import org.alice.stageide.StageIDE;
import org.alice.stageide.croquet.models.sceneditor.ViewListSelectionState;
import org.alice.stageide.oneshot.DynamicOneShotMenuModel;
import org.alice.stageide.run.RunComposite;
import org.alice.stageide.sceneeditor.interact.CameraNavigatorWidget;
import org.alice.stageide.sceneeditor.interact.GlobalDragAdapter;
import org.alice.stageide.sceneeditor.side.SideComposite;
import org.alice.stageide.sceneeditor.snap.SnapState;
import org.alice.stageide.sceneeditor.viewmanager.*;
import org.alice.stageide.sceneeditor.views.InstanceFactorySelectionPanel;
import org.alice.stageide.sceneeditor.views.SceneObjectPropertyManagerPanel;
import org.lgna.croquet.*;
import org.lgna.croquet.history.UserActivity;
import org.lgna.croquet.triggers.InputEventTrigger;
import org.lgna.croquet.views.*;
import org.lgna.croquet.views.SpringPanel.Horizontal;
import org.lgna.croquet.views.SpringPanel.Vertical;
import org.lgna.project.Project;
import org.lgna.project.ast.*;
import org.lgna.project.virtualmachine.UserInstance;
import org.lgna.story.*;
import org.lgna.story.implementation.*;


import javax.swing.Icon;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * @author dculyba
 */
public class StorytellingSceneEditor extends AbstractSceneEditor implements RenderTargetListener {
  private boolean isVrScene;

  private static final String SHOW_JOINTED_MODEL_VISUALIZATIONS_KEY = StorytellingSceneEditor.class.getName() + ".showJointedModelVisualizations";

  private static class SingletonHolder {
    private static StorytellingSceneEditor instance = new StorytellingSceneEditor();
  }

  public static StorytellingSceneEditor getInstance() {
    return SingletonHolder.instance;
  }

  private final SceneEditorDropReceptor dropReceptor = new SceneEditorDropReceptor(this);
  private final SceneFieldCodeGenerator codeGenerator = new SceneFieldCodeGenerator(this);

  private StorytellingSceneEditor() {
  }

  public DropReceptor getDropReceptor() {
    return this.dropReceptor;
  }

  private static Icon EXPAND_ICON = new FlatSVGIcon(Icons.class.getResource("images/expand.svg")).derive(24, 24);
  private static Icon CONTRACT_ICON = new FlatSVGIcon(Icons.class.getResource("images/contract.svg")).derive(24, 24);

  private AutomaticDisplayListener automaticDisplayListener = new AutomaticDisplayListener() {
    @Override
    public void automaticDisplayCompleted(AutomaticDisplayEvent e) {
      StorytellingSceneEditor.this.animator.update();
    }
  };
  private OnscreenRenderTarget onscreenRenderTarget = GlrRenderFactory.getInstance().createOnscreenRenderTarget(new RenderCapabilities.Builder().stencilBits(0).build());

  private boolean isInitialized = false;

  private ClockBasedAnimator animator = new ClockBasedAnimator();
  LookingGlassPanel lookingGlassPanel = new LookingGlassPanel(onscreenRenderTarget);
  GlobalDragAdapter globalDragAdapter;
  final SceneEditorListeners listeners = new SceneEditorListeners(this);
  // The location of the Camera or VR user ground. Same as sceneCameraImp for non VR scenes.
  private TransformableImp movableSceneCameraImp;
  // The camera or VR headset. Same as movableSceneCameraImp for non VR scenes.
  private CameraImp<SymmetricPerspectiveCamera> sceneCameraImp;
  private CameraNavigatorWidget mainCameraNavigatorWidget = null;
  private Button expandButton;
  private Button contractButton;
  private InstanceFactorySelectionPanel instanceFactorySelectionPanel = null;

  private final Button runButton = IsToolBarShowing.getValue() ? null : RunComposite.getInstance().getLaunchOperation().createButton();

  private OrthographicCameraImp orthographicCameraImp = null;
  private final SymmetricPerspectiveCameraImp layoutCameraImp = new SymmetricPerspectiveCameraImp(null);

  private ComboBox<CameraOption> mainCameraViewSelector;
  private CameraMarkerTracker mainCameraViewTracker;
  private CameraOption savedSceneEditorViewSelection = null;

  private ImmutableDataSingleSelectListState<CameraOption> mainCameraMarkerList = ViewListSelectionState.getInstance();

  boolean selectionIsFromInstanceSelector = false;
  private boolean selectionIsFromMain = false;

  protected SnapGrid snapGrid;

  public boolean isStartingCameraView() {
    return mainCameraViewSelector.getModel().getListSelectionState().getValue() == CameraOption.STARTING_CAMERA_VIEW;
  }

  public void setStartingCameraMarkerTransformation(AffineMatrix4x4 transform) {
    movableSceneCameraImp.setLocalTransformation(transform);
  }

  public static class SceneEditorProgramImp extends ProgramImp {
    public SceneEditorProgramImp(SProgram abstraction) {
      super(abstraction, StorytellingSceneEditor.getInstance().onscreenRenderTarget);
    }

    @Override
    public Animator getAnimator() {
      return StorytellingSceneEditor.getInstance().animator;
    }
  }

  @Override
  protected UserInstance createProgramInstance() {
    ProgramImp.ACCEPTABLE_HACK_FOR_NOW_setClassForNextInstance(SceneEditorProgramImp.class);
    return super.createProgramInstance();
  }

  private void setSelectedFieldOnManipulator(UserField field) {
    if (this.globalDragAdapter != null) {
      SThing selectedEntity = this.getInstanceInJavaVMForField(field, SThing.class);
      TransformableImp transImp = null;
      if (selectedEntity != null) {
        EntityImp imp = selectedEntity.getImplementation();
        if (imp instanceof TransformableImp transformableImp) {
          transImp = transformableImp;
        }
      }
      this.globalDragAdapter.setSelectedImplementation(transImp);
    }
  }

  private void setSelectedExpressionOnManipulator(Expression expression) {
    if (this.globalDragAdapter != null) {
      SThing selectedEntity = this.getInstanceInJavaVMForExpression(expression, SThing.class);
      AbstractTransformableImp transImp = null;
      if (selectedEntity != null) {
        EntityImp imp = selectedEntity.getImplementation();
        if (imp instanceof AbstractTransformableImp transformableImp) {
          transImp = transformableImp;
        }
      }
      this.globalDragAdapter.setSelectedImplementation(transImp);
    }
  }

  void setSelectedInstance(InstanceFactory instanceFactory) {
    Expression expression = instanceFactory != null ? instanceFactory.createExpression() : null;
    if (expression instanceof FieldAccess fa) {
      AbstractField field = fa.field.getValue();
      if (field instanceof UserField uf) {
        setSelectedField(uf.getDeclaringType(), uf);
      }
    } else if (expression instanceof MethodInvocation) {
      setSelectedExpression(expression);
    } else if (expression instanceof ArrayAccess) {
      setSelectedExpression(expression);
    } else if (expression instanceof ThisExpression) {
      UserField uf = getActiveSceneField();
      if (uf != null) {
        setSelectedField(uf.getDeclaringType(), uf);
      } else {
        return;
      }
    }
    getPropertyPanel().setSelectedInstance(instanceFactory);
  }

  public void setSelectedExpression(Expression expression) {
    if (!this.selectionIsFromMain) {
      this.selectionIsFromMain = true;
      if (this.globalDragAdapter != null) {
        setSelectedExpressionOnManipulator(expression);
      }
      this.selectionIsFromMain = false;
    }
  }

  public void centerCameraOnSelectedField(UserActivity activity) {
    UserField field = getSelectedField();
    if (getActiveSceneField() != field) {
      mainCameraViewTracker.centerCameraOnField(activity, movableSceneCameraImp, field);
    }
  }

  @Override
  public void setSelectedField(UserType<?> declaringType, UserField field) {
    if (!this.selectionIsFromMain) {
      this.selectionIsFromMain = true;
      final AbstractType<?, ?, ?> valueType = field.getValueType();
      if (isSelectableType(valueType)) {
        super.setSelectedField(declaringType, field);

        MoveSelectedObjectToMarkerActionOperation.getInstance().setSelectedField(field);
        MoveMarkerToSelectedObjectActionOperation.getInstance().setSelectedField(field);
        if (!this.selectionIsFromInstanceSelector) {
          StageIDE ide = StageIDE.getActiveInstance();
          InstanceFactoryState instanceFactoryState = ide.getDocumentFrame().getInstanceFactoryState();
          if (field == this.getActiveSceneField()) {
            instanceFactoryState.setValueTransactionlessly(ide.getInstanceFactoryForScene());
          } else {
            instanceFactoryState.setValueTransactionlessly(ide.getInstanceFactoryForSceneField(field));
          }
        }
      }
      setSelectedFieldOnManipulator(field);
      this.selectionIsFromMain = false;
    }

    //TEST
    Runnable refresher = new Runnable() {
      @Override
      public void run() {
        StorytellingSceneEditor.this.revalidateAndRepaint();
        SideComposite.getInstance().getObjectPropertiesTab().getView().revalidateAndRepaint();
        SideComposite.getInstance().getObjectMarkersTab().getView().revalidateAndRepaint();
      }
    };
    try {
      SwingUtilities.invokeLater(refresher);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private boolean isSelectableType(AbstractType<?, ?, ?> valueType) {
    return !valueType.isAssignableFrom(SThingMarker.class)
            && !valueType.isAssignableFrom(SCameraMarker.class)
            && !valueType.isAssignableFrom(SVRHand.class)
            && !valueType.isAssignableFrom(SVRHeadset.class);
  }

  public Boolean isVrActive() {
    return isVrScene;
  }

  private void setIsVrActive(boolean isActive) {
    isVrScene = isActive;
    mainCameraViewTracker.setIsVrActive(isActive);

    // Update camera marker move buttons
    MoveActiveCameraToMarkerActionOperation.getInstance().updateDisplay();
    MoveMarkerToActiveCameraActionOperation.getInstance().updateDisplay();
  }

  private void setCameras() {
    SymmetricPerspectiveCamera mainCamera = sceneCameraImp.getSgCamera();
    SymmetricPerspectiveCamera layoutCamera = layoutCameraImp.getSgCamera();
    onscreenRenderTarget.setLetterboxed(layoutCamera, false);
    OrthographicCamera orthographicCamera = orthographicCameraImp.getSgCamera();
    globalDragAdapter.addCameraView(CameraView.MAIN, mainCamera, layoutCamera, orthographicCamera);
    globalDragAdapter.makeCameraActive(mainCamera);
    mainCameraViewTracker.setCameras(mainCamera, layoutCamera, orthographicCamera);
    snapGrid.addCamera(mainCamera);
    snapGrid.addCamera(layoutCamera);
    snapGrid.addCamera(orthographicCamera);
    snapGrid.setCurrentCamera(mainCamera);
  }

  private void showLookingGlassPanel() {
    synchronized (this.getTreeLock()) {
      this.addCenterComponent(this.lookingGlassPanel);
    }
  }

  private void hideLookingGlassPanel() {
    synchronized (this.getTreeLock()) {
      this.removeComponent(this.lookingGlassPanel);
    }
  }

  @Override
  protected void handleExpandContractChange(boolean isExpanded) {
    //todo
    synchronized (this.getTreeLock()) {
      this.mainCameraNavigatorWidget.setExpanded(isExpanded);
      if (this.runButton != null) {
        this.lookingGlassPanel.setNorthEastComponent(this.runButton);
      }
      if (isExpanded) {
        this.lookingGlassPanel.setNorthWestComponent(this.instanceFactorySelectionPanel);
        this.lookingGlassPanel.setSouthEastComponent(this.contractButton);

        this.lookingGlassPanel.setSouthComponent(this.mainCameraNavigatorWidget);

        if (this.savedSceneEditorViewSelection != null) {
          this.mainCameraMarkerList.setValueTransactionlessly(this.savedSceneEditorViewSelection);
        }
      } else {
        this.lookingGlassPanel.setNorthWestComponent(null);
        this.lookingGlassPanel.setSouthEastComponent(this.expandButton);
        this.lookingGlassPanel.setSouthComponent(null);

        this.savedSceneEditorViewSelection = this.mainCameraMarkerList.getValue();
        this.mainCameraMarkerList.setValueTransactionlessly(CameraOption.STARTING_CAMERA_VIEW);
      }
      this.mainCameraViewSelector.setVisible(isExpanded);
    }
  }

  private SceneObjectPropertyManagerPanel getPropertyPanel() {
    return SideComposite.getInstance().getObjectPropertiesTab().getView();
  }

  void handleCameraMarkerFieldSelection(UserField cameraMarkerField) {
    CameraMarkerImp newMarker = (CameraMarkerImp) this.getMarkerForField(cameraMarkerField);
    this.globalDragAdapter.setSelectedCameraMarker(newMarker);
    MoveActiveCameraToMarkerActionOperation.getInstance().setMarkerField(cameraMarkerField);
    MoveMarkerToActiveCameraActionOperation.getInstance().setMarkerField(cameraMarkerField);
  }

  void handleObjectMarkerFieldSelection(UserField objectMarkerField) {
    ObjectMarkerImp newMarker = (ObjectMarkerImp) this.getMarkerForField(objectMarkerField);
    this.globalDragAdapter.setSelectedObjectMarker(newMarker);
    MoveSelectedObjectToMarkerActionOperation.getInstance().setMarkerField(objectMarkerField);
    MoveMarkerToSelectedObjectActionOperation.getInstance().setMarkerField(objectMarkerField);
  }

  public void setSelectedObjectMarker(UserField objectMarkerField) {
    RefreshableDataSingleSelectListState<UserField> markerList = SideComposite.getInstance().getObjectMarkersTab().getMarkerListState();
    markerList.setSelectedIndex(markerList.indexOf(objectMarkerField));
  }

 private void setSelectedCameraMarker(UserField cameraMarkerField) {
    RefreshableDataSingleSelectListState<UserField> markerList = SideComposite.getInstance().getCameraMarkersTab().getMarkerListState();
    markerList.setSelectedIndex(markerList.indexOf(cameraMarkerField));
  }

  private void handleManipulatorSelection(SelectionEvent e) {
    EntityImp imp = e.getTransformable();
    if (imp != null) {
      UserField field = this.getFieldForInstanceInJavaVM(imp.getAbstraction());
      if (field != null) {
        if (field.getValueType().isAssignableFrom(SCameraMarker.class)) {
          this.setSelectedCameraMarker(field);
        } else if (field.getValueType().isAssignableFrom(SThingMarker.class)) {
          this.setSelectedObjectMarker(field);
        } else {
          this.setSelectedField(field.getDeclaringType(), field);
        }
      }
      if (imp instanceof PerspectiveCameraMarkerImp markerImp) {
        globalDragAdapter.setSelectedImplementation(markerImp);
      }
    } else {
      UserField uf = getActiveSceneField();
      setSelectedField(uf.getDeclaringType(), uf);
    }
  }

  private void showRightClickMenuForModel(InputState clickInput) {
    Element element = clickInput.getClickPickedTransformable(true);
    if (element != null) {
      EntityImp entityImp = EntityImp.getInstance(element);
      SThing entity = entityImp.getAbstraction();
      UserField field;
      if (entity != null) {
        field = this.getFieldForInstanceInJavaVM(entity);
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

  void handleMainCameraViewSelection() {
    StageIDE ide = StageIDE.getActiveInstance();
    InstanceFactoryState instanceFactoryState = ide.getDocumentFrame().getInstanceFactoryState();
    UserField field = getSelectedField();
    if (field != this.getActiveSceneField()) {
      instanceFactoryState.setValueTransactionlessly(ide.getInstanceFactoryForSceneField(field));
    }
  }

  private void switchToCamera(AbstractCamera camera) {
    if (onscreenRenderTarget.getSgCameraCount() != 1
            || onscreenRenderTarget.getSgCameraAt(0) != camera) {
      onscreenRenderTarget.clearSgCameras();
      onscreenRenderTarget.addSgCamera(camera);
    }
    snapGrid.setCurrentCamera(camera);
    globalDragAdapter.makeCameraActive(camera);
    onscreenRenderTarget.repaint();
    revalidateAndRepaint();
  }

  public void switchToOrthographicCamera() {
    switchToCamera(orthographicCameraImp.getSgCamera());
    mainCameraNavigatorWidget.setToOrthographicMode();
  }

  public void switchToPerspectiveCamera(AbstractCamera sgCamera) {
    switchToCamera(sgCamera);
    mainCameraNavigatorWidget.setToPerspectiveMode();
  }

  @Override
  protected void initializeComponents() {
    if (this.isInitialized) {
      return;
    }
    this.snapGrid = new SnapGrid();
    SnapState.getInstance().getShowSnapGridState().addAndInvokeNewSchoolValueListener(this.listeners.showSnapGridListener);
    SnapState.getInstance().getIsSnapEnabledState().addAndInvokeNewSchoolValueListener(this.listeners.snapEnabledListener);
    SnapState.getInstance().getSnapGridSpacingState().addAndInvokeNewSchoolValueListener(this.listeners.snapGridSpacingListener);

    ProjectDocumentFrame docFrame = IDE.getActiveInstance().getDocumentFrame();
    docFrame.getInstanceFactoryState().addAndInvokeNewSchoolValueListener(this.listeners.instanceFactorySelectionListener);

    this.globalDragAdapter = new GlobalDragAdapter(this);
    this.globalDragAdapter.setOnscreenRenderTarget(onscreenRenderTarget);
    this.onscreenRenderTarget.addRenderTargetListener(this);
    this.globalDragAdapter.setAnimator(animator);
    if (this.getSelectedField() != null) {
      setSelectedFieldOnManipulator(this.getSelectedField());
    }

    this.mainCameraNavigatorWidget = new CameraNavigatorWidget(this.globalDragAdapter, CameraView.MAIN);

    this.expandButton = docFrame.getSetToSetupScenePerspectiveOperation().createButton();
    this.expandButton.setClobberIcon(EXPAND_ICON);
    //todo: tool tip text
    this.contractButton = docFrame.getSetToCodePerspectiveOperation().createButton();
    this.contractButton.setClobberIcon(CONTRACT_ICON);
    this.instanceFactorySelectionPanel = new InstanceFactorySelectionPanel();
    this.orthographicCameraImp = new OrthographicCameraImp();
    this.orthographicCameraImp.getSgCamera().nearClippingPlaneDistance.setValue(.01d);

    this.globalDragAdapter.addSelectionListener(new SelectionListener() {
      @Override
      public void selecting(SelectionEvent e) {
      }

      @Override
      public void selected(SelectionEvent e) {
        StorytellingSceneEditor.this.handleManipulatorSelection(e);
      }
    });

    ClickedObjectCondition rightMouseAndInteractive = new ClickedObjectCondition(MouseEvent.BUTTON3, new PickCondition(PickHint.PickType.TURNABLE.pickHint()));
    ManipulatorClickAdapter rightClickAdapter = new ManipulatorClickAdapter() {
      @Override
      public void onClick(InputState clickInput) {
        showRightClickMenuForModel(clickInput);

      }
    };
    this.globalDragAdapter.addClickAdapter(rightClickAdapter, rightMouseAndInteractive);

    this.mainCameraViewTracker = new CameraMarkerTracker(this, animator);
    this.mainCameraViewSelector = this.mainCameraMarkerList.getPrepModel().createComboBox();
    this.mainCameraViewSelector.setRenderer(new CameraViewCellRenderer());
    this.mainCameraViewSelector.setFontSize(15);
    this.mainCameraMarkerList.addAndInvokeNewSchoolValueListener(this.mainCameraViewTracker);
    this.mainCameraMarkerList.addAndInvokeNewSchoolValueListener(this.listeners.mainCameraViewSelectionObserver);
    this.lookingGlassPanel.addComponent(this.mainCameraViewSelector, Horizontal.CENTER, 0, Vertical.NORTH, 20);

    SideComposite.getInstance().getCameraMarkersTab().getMarkerListState().addAndInvokeNewSchoolValueListener(this.listeners.cameraMarkerFieldSelectionListener);
    SideComposite.getInstance().getObjectMarkersTab().getMarkerListState().addAndInvokeNewSchoolValueListener(this.listeners.objectMarkerFieldSelectionListener);

    this.isInitialized = true;
  }

  @Override
  public void addField(UserType<?> declaringType, UserField field, int index, Statement... statements) {
    super.addField(declaringType, field, index, statements);
    if (field.getValueType().isAssignableTo(SMarker.class)) {
      SMarker marker = this.getInstanceInJavaVMForField(field, SMarker.class);
      MarkerImp markerImp = marker.getImplementation();
      markerImp.setDisplayVisuals(true);
      markerImp.setShowing(true);

      if (field.getValueType().isAssignableTo(CameraMarker.class)) {
        this.setSelectedCameraMarker(field);
        ((PerspectiveCameraMarkerImp) markerImp).setVrActive(isVrActive());
      } else if (field.getValueType().isAssignableTo(SThingMarker.class)) {
        this.setSelectedObjectMarker(field);
      }
    }
    setInitialCodeStateForField(field, getCurrentStateCodeForField(field));
    if (SystemUtilities.isPropertyTrue(SHOW_JOINTED_MODEL_VISUALIZATIONS_KEY)) {
      if (field.getValueType().isAssignableTo(SJointedModel.class)) {
        SJointedModel jointedModel = this.getInstanceInJavaVMForField(field, SJointedModel.class);
        JointedModelImp jointedModelImp = jointedModel.getImplementation();
        jointedModelImp.opacity.setValue(0.25f);
        jointedModelImp.showVisualization();
      } else if (field.getValueType().isAssignableTo(SModel.class)) {
        SModel model = this.getInstanceInJavaVMForField(field, SModel.class);
        ModelImp modelImp = model.getImplementation();
        modelImp.showVisualization();
      }
    }
  }

  @Override
  protected void setActiveScene(UserField sceneField) {
    super.setActiveScene(sceneField);
    // Restore to origin and upright
    if (movableSceneCameraImp != null) {
      movableSceneCameraImp.setLocalTransformation(AffineMatrix4x4.IDENTITY);
    }

    if (sceneField != null) {
      SProgram program = getProgramInstanceInJava();
      program.setSimulationSpeedFactor(Double.POSITIVE_INFINITY);

      UserInstance sceneAliceInstance = getActiveSceneInstance();
      SScene scene = sceneAliceInstance.getJavaInstance(SScene.class);

      SceneImp ACCEPTABLE_HACK_sceneImp = scene.getImplementation();
      ACCEPTABLE_HACK_sceneImp.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_pushPerformMinimalInitialization();
      try {
        program.setActiveScene(scene);
      } finally {
        ACCEPTABLE_HACK_sceneImp.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_popPerformMinimalInitialization();
      }
      UserMethod generatedSetupMethod = sceneAliceInstance.getType().getDeclaredMethod(StageIDE.PERFORM_GENERATED_SET_UP_METHOD_NAME);
      useSceneAsVehicleForDisconnectedModels(generatedSetupMethod);
      this.getVirtualMachine().ENTRY_POINT_invoke(sceneAliceInstance, generatedSetupMethod);

      getPropertyPanel().setSceneInstance(sceneAliceInstance);

      this.instanceFactorySelectionPanel.setType(sceneAliceInstance.getType());
      for (AbstractField field : sceneField.getValueType().getDeclaredFields()) {
        if (field.getValueType().isAssignableTo(SCamera.class)) {
          sceneCameraImp = getImplementation(field);
          movableSceneCameraImp = sceneCameraImp;
          setIsVrActive(false);
          break;
        }
        if (field.getValueType().isAssignableTo(SVRUser.class)) {
          VrUserImp vrUserImp = getImplementation(field);
          sceneCameraImp = vrUserImp.getAbstraction().getHeadset().getImplementation();
          movableSceneCameraImp = vrUserImp;
          setIsVrActive(true);
          break;
        }
      }

      assert ((this.globalDragAdapter != null) && (this.sceneCameraImp != null) && (this.orthographicCameraImp != null));
      this.globalDragAdapter.clearCameraViews();
      this.globalDragAdapter.addCameraView(CameraView.MAIN, this.sceneCameraImp.getSgCamera());
      this.globalDragAdapter.makeCameraActive(this.sceneCameraImp.getSgCamera());

      SceneImp sceneImp = this.getActiveSceneImplementation();
      //Add and set up the snap grid (this needs to happen before setting the camera)
      sceneImp.getSgComposite().addComponent(this.snapGrid);
      this.snapGrid.setTranslationOnly(0, 0, 0, AsSeenBy.SCENE);
      this.snapGrid.setShowing(SnapState.getInstance().shouldShowSnapGrid());

      // Initialize stuff that needs a camera
      this.setCameras();

      MoveActiveCameraToMarkerActionOperation.getInstance().setCamera(movableSceneCameraImp);
      MoveMarkerToActiveCameraActionOperation.getInstance().setCamera(movableSceneCameraImp);

      // Add orthographic camera, layout camera, and markers
      sceneImp.getSgComposite().addComponent(this.orthographicCameraImp.getSgCamera().getParent());
      sceneImp.getSgComposite().addComponent(layoutCameraImp.getSgCamera().getParent());

      mainCameraViewTracker.updateMarkersForNewScene(sceneImp, movableSceneCameraImp);

      savedSceneEditorViewSelection = null;
      mainCameraViewTracker.trackStartingCameraView();
      mainCameraViewSelector.refreshModel();

      this.setSelectedCameraMarker(null);
      this.setSelectedObjectMarker(null);

      for (AbstractField field : sceneField.getValueType().getDeclaredFields()) {
        // Turn markers on, so they're visible in the scene editor (note: markers are hidden by default so that when a world runs they aren't seen.
        // we have to manually make them visible to see them in the scene editor)
        if (field.getValueType() != null && field.getValueType().isAssignableTo(SMarker.class)) {
          SMarker marker = this.getInstanceInJavaVMForField(field, SMarker.class);
          MarkerImp markerImp = marker.getImplementation();
          if (field.getValueType().isAssignableTo(CameraMarker.class)) {
            ((PerspectiveCameraMarkerImp) markerImp).setVrActive(isVrActive());
          }
          markerImp.setDisplayVisuals(true);
          markerImp.setShowing(true);
        }
        if (field instanceof UserField userField) {
          if (userField.getManagementLevel() == ManagementLevel.MANAGED) {
            this.setInitialCodeStateForField(userField, getCurrentStateCodeForField(userField));
          }
        }
      }
      program.setSimulationSpeedFactor(1.0);
    }
  }

  private void useSceneAsVehicleForDisconnectedModels(UserMethod generatedSetupMethod) {
    for (Statement statement : generatedSetupMethod.body.getValue().statements.getValue()) {
      MethodInvocation setVehicleCall = SceneFieldCodeGenerator.asSetVehicleCall(statement);
      if (setVehicleCall == null) {
        continue;
      }
      ArrayList<SimpleArgument> args = setVehicleCall.requiredArguments.getValue();
      if (args.size() == 1 && args.getFirst().expression.getValue() instanceof NullLiteral) {
        args.getFirst().expression.setValue(new ThisExpression());
      }
    }
  }

  @Override
  public void enableRendering(ReasonToDisableSomeAmountOfRendering reasonToDisableSomeAmountOfRendering) {
    if ((reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN) || (reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.CLICK_AND_CLACK)) {
      this.onscreenRenderTarget.setRenderingEnabled(true);
    }
  }

  @Override
  public void disableRendering(ReasonToDisableSomeAmountOfRendering reasonToDisableSomeAmountOfRendering) {
    if ((reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.MODAL_DIALOG_WITH_RENDER_WINDOW_OF_ITS_OWN) || (reasonToDisableSomeAmountOfRendering == ReasonToDisableSomeAmountOfRendering.CLICK_AND_CLACK)) {
      this.onscreenRenderTarget.setRenderingEnabled(false);
    }
  }

  @Override
  public void preScreenCapture() {
    this.globalDragAdapter.setHandleVisibility(false);
  }

  @Override
  public void postScreenCapture() {
    this.globalDragAdapter.setHandleVisibility(true);
  }

  @Override
  public void setFieldToState(UserField field, Statement... statements) {
    EntityImp fieldImp = getImplementation(field);
    AffineMatrix4x4 originalTransform = fieldImp.getAbsoluteTransformation();
    super.setFieldToState(field, statements);
    if ((fieldImp == movableSceneCameraImp) && (mainCameraMarkerList.getValue() != CameraOption.STARTING_CAMERA_VIEW)) {
      movableSceneCameraImp.setTransformation(movableSceneCameraImp.getScene(), originalTransform);
    }
  }

  @Override
  public Statement getCurrentStateCodeForField(UserField field) {
    return codeGenerator.getCurrentStateCodeForField(field);
  }

  @Override
  public void generateCodeForSetUp(StatementListProperty bodyStatementsProperty) {
    codeGenerator.generateCodeForSetUp(bodyStatementsProperty);
  }

  @Override
  public Statement[] getDoStatementsForCopyField(UserField fieldToCopy, UserField newField, AffineMatrix4x4 initialTransform) {
    return codeGenerator.getDoStatementsForCopyField(fieldToCopy, newField, initialTransform);
  }

  @Override
  public Statement[] getDoStatementsForAddField(UserField field, AffineMatrix4x4 initialTransform) {
    return codeGenerator.getDoStatementsForAddField(field, initialTransform);
  }

  @Override
  public Statement[] getUndoStatementsForAddField(UserField field) {
    return codeGenerator.getUndoStatementsForAddField(field);
  }

  public Map<AbstractField, Statement> getRiders(UserField vehicle) {
    return codeGenerator.getRiders(vehicle);
  }

  @Override
  public Statement[] getDoStatementsForRemoveField(UserField field, Map<AbstractField, Statement> riders) {
    return codeGenerator.getDoStatementsForRemoveField(field, riders);
  }

  @Override
  public Statement[] getUndoStatementsForRemoveField(UserField field, Map<AbstractField, Statement> riders) {
    return codeGenerator.getUndoStatementsForRemoveField(field, riders);
  }

  @Override
  protected void handleProjectOpened(Project nextProject) {
    if (this.onscreenRenderTarget != null) {
      this.onscreenRenderTarget.forgetAllCachedItems();
      NebulousIde.nonfree.unloadNebulousModelData();
    }

    NebulousIde.nonfree.unloadPerson();
    if (this.globalDragAdapter != null) {
      this.globalDragAdapter.clear();
    }
    super.handleProjectOpened(nextProject);
  }

  public void handleShowing() {
    GlrRenderFactory renderFactory = GlrRenderFactory.getInstance();
    renderFactory.incrementAutomaticDisplayCount();
    renderFactory.addAutomaticDisplayListener(this.automaticDisplayListener);
    this.showLookingGlassPanel();
  }

  public void handleHiding() {
    this.hideLookingGlassPanel();
    GlrRenderFactory renderFactory = GlrRenderFactory.getInstance();
    renderFactory.removeAutomaticDisplayListener(this.automaticDisplayListener);
    renderFactory.decrementAutomaticDisplayCount();
  }

  private void paintHorizonLine(Graphics graphics, OnscreenRenderTarget renderTarget, OrthographicCamera camera) {
    AffineMatrix4x4 cameraTransform = camera.getAbsoluteTransformation();
    double dotProd = cameraTransform.orientation().up().dotProduct(Vector3.POSITIVE_Y_AXIS);
    if ((dotProd == 1) || (dotProd == -1)) {
      //TODO: Make this handle retina displays and the fact that surface size and screen size may be different
      Dimension lookingGlassSize = renderTarget.getSurfaceSize();

      Point3 cameraPosition = camera.getAbsoluteTransformation().translation();

      ClippedZPlane dummyPlane = camera.picturePlane.getValue().completeFrom(renderTarget.getActualViewport(camera));

      double lookingGlassHeight = lookingGlassSize.getHeight();

      double yRatio = this.onscreenRenderTarget.getSurfaceHeight() / dummyPlane.getHeight();
      double horizonInCameraSpace = 0.0d - cameraPosition.y();
      double distanceFromMaxY = dummyPlane.getYMaximum() - horizonInCameraSpace;
      int horizonLinePixelVal = (int) (yRatio * distanceFromMaxY);
      if ((horizonLinePixelVal >= 0) && (horizonLinePixelVal <= lookingGlassHeight)) {
        graphics.setColor(java.awt.Color.BLACK);
        graphics.drawLine(0, horizonLinePixelVal, lookingGlassSize.width, horizonLinePixelVal);
      }
    }
  }

  // ######### Begin implementation of edu.cmu.cs.dennisc.lookingglass.event.LookingGlassAdapter
  @Override
  public void initialized(RenderTargetInitializeEvent e) {
  }

  @Override
  public void cleared(RenderTargetRenderEvent e) {
  }

  @Override
  public void rendered(RenderTargetRenderEvent e) {
    if ((this.onscreenRenderTarget.getSgCameraCount() > 0) && (this.onscreenRenderTarget.getSgCameraAt(0) instanceof OrthographicCamera)) {
      paintHorizonLine(e.getGraphics2D(), this.onscreenRenderTarget, (OrthographicCamera) this.onscreenRenderTarget.getSgCameraAt(0));
    }
  }

  @Override
  public void resized(RenderTargetResizeEvent e) {
    // updateCameraMarkers();
  }

  @Override
  public void displayChanged(RenderTargetDisplayChangeEvent e) {
  }
  // ######### End implementation of edu.cmu.cs.dennisc.lookingglass.event.LookingGlassAdapter

  public void setHandleVisibilityForObject(TransformableImp imp, boolean b) {
    this.globalDragAdapter.setHandleShowingForSelectedImplementation(imp, b);
  }

  public AffineMatrix4x4 getTransformForNewCameraMarker() {
    return movableSceneCameraImp.getAbsoluteTransformation();
  }

  public AffineMatrix4x4 getTransformForNewObjectMarker() {
    EntityImp selectedImp = this.getImplementation(this.getSelectedField());
    AffineMatrix4x4 initialTransform = null;
    if (selectedImp != null) {
      initialTransform = selectedImp.getAbsoluteTransformation();
    } else {
      initialTransform = AffineMatrix4x4.IDENTITY;
    }
    return initialTransform;
  }

  public Color getColorForNewObjectMarker() {
    return MarkerUtilities.getNewObjectMarkerColor();
  }

  public Color getColorForNewCameraMarker() {
    return MarkerUtilities.getNewCameraMarkerColor();
  }

  public AffineMatrix4x4 getGoodPointOfViewInSceneForObject(AxisAlignedBox box) {
    throw new RuntimeException("todo");
  }

  public MarkerImp getMarkerForField(UserField field) {
    Object obj = this.getInstanceInJavaVMForField(field);
    if (obj instanceof SMarker marker) {
      return marker.getImplementation();
    }
    return null;
  }

  public AbstractCamera getSgCameraForCreatingThumbnails() {
    if (this.sceneCameraImp != null) {
      return this.sceneCameraImp.getSgCamera();
    } else {
      return null;
    }
  }

  public void setShowSnapGrid(boolean showSnapGrid) {
    if (this.snapGrid != null) {
      this.snapGrid.setShowing(showSnapGrid);
    }
  }

  public void setSnapGridSpacing(double gridSpacing) {
    if (this.snapGrid != null) {
      this.snapGrid.setSpacing(gridSpacing);
    }
  }

  public OnscreenRenderTarget getOnscreenRenderTarget() {
    return this.onscreenRenderTarget;
  }
}
