package org.lgna.ik.poser.scene;

import edu.cmu.cs.dennisc.color.Color4f;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.render.OnscreenRenderTarget;
import org.alice.interact.DragAdapter;
import org.alice.interact.DragAdapter.ObjectType;
import org.alice.interact.InteractionGroup;
import org.alice.interact.ModifierMask;
import org.alice.interact.MovementDirection;
import org.alice.interact.PickHint;
import org.alice.interact.PickHint.PickType;
import org.alice.interact.condition.DragAndDropCondition;
import org.alice.interact.condition.ManipulatorConditionSet;
import org.alice.interact.condition.MouseDragCondition;
import org.alice.interact.condition.MousePressCondition;
import org.alice.interact.condition.MouseWheelCondition;
import org.alice.interact.condition.PickCondition;
import org.alice.interact.handle.HandleSet;
import org.alice.interact.handle.HandleStyle;
import org.alice.interact.handle.JointRotationRingHandle;
import org.alice.interact.manipulator.CameraOrbitDragManipulator;
import org.alice.interact.manipulator.ObjectRotateDragManipulator;
import org.alice.math.immutable.Point3;
import org.lgna.ik.poser.PoserSphereManipulatorListener;
import org.lgna.story.SModel;
import org.lgna.story.implementation.EntityImp;

import java.awt.event.MouseEvent;

public class PoserAnimatorDragAdapter extends DragAdapter {
  private static final class TargetCameraOrbitManipulator extends CameraOrbitDragManipulator {
    private EntityImp target;

    public void setTarget(SModel target) {
      this.target = target.getImplementation();
    }

    @Override
    public void setPivotPoint(Point3 pivotPoint) {
      if (target != null) {
        super.setPivotPoint(target.getAbsoluteTransformation().translation());
      } else {
        super.setPivotPoint(pivotPoint);
      }
    }
  }

  private PoserPicturePlaneInteraction dragAdapter = null;
  private final AbstractPoserScene poserScene;
  private ManipulatorConditionSet selectObject;
  private final PoserSceenMouseWheelManipulator manipulator = new PoserSceenMouseWheelManipulator();
  private static final TargetCameraOrbitManipulator ORBITER = new TargetCameraOrbitManipulator();

  public PoserAnimatorDragAdapter(AbstractPoserScene poserScene) {
    this.poserScene = poserScene;
    this.setUpControls();
  }

  private void setUpControls() {
    MouseDragCondition middleMouseAndAnything = new MouseDragCondition(MouseEvent.BUTTON2, new PickCondition(PickHint.getAnythingHint()));

    ManipulatorConditionSet cameraOrbit = new ManipulatorConditionSet(ORBITER);
    cameraOrbit.addCondition(middleMouseAndAnything);
    addManipulatorConditionSet(cameraOrbit);

    JointRotationRingHandle rotateJointAboutZAxis = new JointRotationRingHandle(MovementDirection.BACKWARD, Color4f.BLUE);
    rotateJointAboutZAxis.setManipulation(new ObjectRotateDragManipulator());
    rotateJointAboutZAxis.addToSet(HandleSet.JOINT_ROTATION_INTERACTION);
    rotateJointAboutZAxis.addToGroups(HandleSet.HandleGroup.Z_AXIS, HandleSet.HandleGroup.VISUALIZATION, HandleSet.HandleGroup.JOINT);
    rotateJointAboutZAxis.setDragAdapterAndAddHandle(this);

    JointRotationRingHandle rotateJointAboutYAxis = new JointRotationRingHandle(MovementDirection.UP, Color4f.GREEN);
    rotateJointAboutYAxis.setManipulation(new ObjectRotateDragManipulator());
    rotateJointAboutYAxis.addToSet(HandleSet.JOINT_ROTATION_INTERACTION);
    rotateJointAboutYAxis.addToGroups(HandleSet.HandleGroup.Y_AXIS, HandleSet.HandleGroup.VISUALIZATION, HandleSet.HandleGroup.JOINT);
    rotateJointAboutYAxis.setDragAdapterAndAddHandle(this);

    JointRotationRingHandle rotateJointAboutXAxis = new JointRotationRingHandle(MovementDirection.LEFT, Color4f.RED);
    rotateJointAboutXAxis.setManipulation(new ObjectRotateDragManipulator());
    rotateJointAboutXAxis.addToSet(HandleSet.JOINT_ROTATION_INTERACTION);
    rotateJointAboutXAxis.addToGroups(HandleSet.HandleGroup.X_AXIS, HandleSet.HandleGroup.VISUALIZATION, HandleSet.HandleGroup.JOINT);
    rotateJointAboutXAxis.setDragAdapterAndAddHandle(this);

    addHandle(rotateJointAboutXAxis);
    addHandle(rotateJointAboutYAxis);
    addHandle(rotateJointAboutZAxis);

    ManipulatorConditionSet mouseWheelCameraZoom = new ManipulatorConditionSet(manipulator);
    MouseWheelCondition mouseWheelCondition = new MouseWheelCondition(new ModifierMask(ModifierMask.NO_MODIFIERS_DOWN));
    mouseWheelCameraZoom.addCondition(mouseWheelCondition);
    addManipulatorConditionSet(mouseWheelCameraZoom);

    selectObject = new ManipulatorConditionSet(new ObjectRotateDragManipulator());
    selectObject.setEnabled(false);
    selectObject.addCondition(new MousePressCondition(MouseEvent.BUTTON1, new PickCondition(PickHint.PickType.SELECTABLE.pickHint())));
    selectObject.addCondition(new DragAndDropCondition());

    InteractionGroup group = new InteractionGroup(new InteractionGroup.InteractionInfo(new InteractionGroup.PossibleObjects(ObjectType.JOINT), HandleSet.JOINT_ROTATION_INTERACTION, selectObject, PickType.JOINT));
    this.mapHandleStyleToInteractionGroup.put(HandleStyle.ROTATION, group);
    setInteractionState(HandleStyle.ROTATION);

    addManipulatorConditionSet(selectObject);
  }

  public final void setTarget(SModel model) {
    ORBITER.setTarget(model);
    manipulator.setModel(model.getImplementation());
  }

  @Override
  public void setOnscreenRenderTarget(OnscreenRenderTarget onscreenRenderTarget) {
    super.setOnscreenRenderTarget(onscreenRenderTarget);
    initDragAdapter(onscreenRenderTarget);
  }

  private void initDragAdapter(OnscreenRenderTarget onscreenRenderTarget) {
    dragAdapter = new PoserPicturePlaneInteraction(onscreenRenderTarget, poserScene);
    dragAdapter.startUp();
  }

  public void addSphereDragListener(PoserSphereManipulatorListener sphereDragListener) {
    try {
      dragAdapter.addListener(sphereDragListener);
    } catch (NullPointerException e) {
      Logger.severe("Drag Adapter cannot be initialized until OnscreenLookingGlass is set");
      Thread.dumpStack();
    }
  }

  @Override
  public void setHandleVisibility(boolean isVisible) {
    super.setHandleVisibility(isVisible);
    selectObject.setEnabled(isVisible);
  }
}
