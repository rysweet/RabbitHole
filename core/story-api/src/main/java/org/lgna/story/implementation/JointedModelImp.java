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

package org.lgna.story.implementation;

import edu.cmu.cs.dennisc.animation.Animated;
import edu.cmu.cs.dennisc.animation.DurationBasedAnimation;
import edu.cmu.cs.dennisc.animation.Style;
import edu.cmu.cs.dennisc.math.EpsilonUtilities;
import edu.cmu.cs.dennisc.property.InstanceProperty;
import edu.cmu.cs.dennisc.property.event.PropertyListener;
import edu.cmu.cs.dennisc.scenegraph.*;
import edu.cmu.cs.dennisc.scenegraph.bound.CumulativeBound;
import org.alice.math.immutable.*;
import org.lgna.ik.core.solver.Bone;
import org.lgna.story.Paint;
import org.lgna.story.Pose;
import org.lgna.story.SJointedModel;
import org.lgna.story.implementation.visualization.JointedModelVisualization;
import org.lgna.story.resources.JointArrayId;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.JointedModelResource;

import java.util.List;

/**
 * @author Dennis Cosgrove
 */
public abstract class JointedModelImp<A extends SJointedModel, R extends JointedModelResource> extends ModelImp {
  public interface VisualData<R extends JointedModelResource> {
    Visual[] getSgVisuals();

    SkeletonVisual getSgVisualForExporting(R resource);

    SimpleAppearance[] getSgAppearances();

    void setSGParent(Composite parent);

    Composite getSGParent();
  }

  public interface JointImplementationAndVisualDataFactory<R extends JointedModelResource> {
    R getResource();

    JointImp createJointImplementation(JointedModelImp<?, R> jointedModelImplementation, JointId jointId);

    boolean hasJointImplementation(JointedModelImp<?, R> jointedModelImplementation, JointId jointId);

    JointId[] getJointArrayIds(JointedModelImp<?, R> jointedModelImplementation, JointArrayId jointArrayId);

    VisualData<R> createVisualData();

    UnitQuaternion getOriginalJointOrientation(JointId jointId);

    AffineMatrix4x4 getOriginalJointTransformation(JointId jointId);

    boolean isSims();
  }

  public interface TreeWalkObserver {
    void pushJoint(JointImp joint);

    void handleBone(JointImp parent, JointImp child);

    void popJoint(JointImp joint);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Construction
  // ════════════════════════════════════════════════════════════════════════════

  public JointedModelImp(A abstraction, JointImplementationAndVisualDataFactory<R> factory) {
    this.abstraction = abstraction;
    this.resourceBinder = new JointedModelResourceBinder<>(factory);
    this.visualManager = new JointedModelVisualManager<>(factory, null);
    this.hierarchyManager = new JointHierarchyManager<>(this.resourceBinder);

    this.hierarchyManager.buildJointHierarchy(this);

    this.visualManager.attachToParent(getSgComposite());

    for (JointImp joint : this.hierarchyManager.getJointWrappers()) {
      final AbstractTransformable sgJoint = joint.getSgComposite();
      if (sgJoint.getParent() == null) {
        sgJoint.setParent(getSgComposite());
      }
    }

    for (Visual sgVisual : this.visualManager.getSgVisuals()) {
      putInstance(sgVisual);
    }
    for (SimpleAppearance sgAppearance : this.visualManager.getSgAppearances()) {
      putInstance(sgAppearance);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Resource change orchestration
  // ════════════════════════════════════════════════════════════════════════════

  @SuppressWarnings("unchecked")
  public void setNewResource(JointedModelResource resource) {
    if (resource == this.getResource()) {
      return;
    }
    Composite originalParent = this.visualManager.getVisualData().getSGParent();
    VisualData<?> oldVisualData = this.visualManager.getVisualData();
    Dimension3 oldScale = this.getScale();
    InstanceProperty[] oldScaleProperties = this.getScaleProperties();

    JointImplementationAndVisualDataFactory<R> newFactory =
        (JointImplementationAndVisualDataFactory<R>) resource.getImplementationAndVisualFactory();
    this.resourceBinder.updateFactory(newFactory);
    this.visualManager.updateFactory(newFactory);

    float originalOpacity = this.opacity.getValue();
    Paint originalPaint = this.paint.getValue();

    this.visualManager.replaceVisualData();

    if (!this.hierarchyManager.isEmpty()) {
      this.hierarchyManager.updateSkeleton(this);
    }

    this.visualManager.attachToParent(originalParent);
    this.visualManager.detachOldVisualData(oldVisualData);
    this.opacity.setValue(originalOpacity);
    this.paint.setValue(originalPaint);

    InstanceProperty<?>[] newScaleProperties = this.getScaleProperties();
    for (int i = 0; i < oldScaleProperties.length; i++) {
      InstanceProperty<?> oldProp = oldScaleProperties[i];
      assert oldProp != null : i;
      for (PropertyListener propListener : oldProp.getPropertyListeners()) {
        newScaleProperties[i].addPropertyListener(propListener);
      }
    }
    this.setScale(oldScale);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Delegations — ResourceBinder
  // ════════════════════════════════════════════════════════════════════════════

  @Override
  public A getAbstraction() {
    return this.abstraction;
  }

  public R getResource() {
    return this.resourceBinder.getResource();
  }

  public JointedModelResource getVisualResource() {
    return this.resourceBinder.getResource();
  }

  public UnitQuaternion getOriginalJointOrientation(JointId jointId) {
    return this.resourceBinder.getOriginalJointOrientation(jointId);
  }

  public AffineMatrix4x4 getOriginalJointTransformation(JointId jointId) {
    return this.resourceBinder.getOriginalJointTransformation(jointId);
  }

  protected final JointImp createJointImplementation(JointId jointId) {
    return this.resourceBinder.createJointImplementation(this, jointId);
  }

  public JointArrayId[] getJointArrayIds() {
    return this.resourceBinder.getJointArrayIds();
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Delegations — VisualManager
  // ════════════════════════════════════════════════════════════════════════════

  public VisualData<R> getVisualData() {
    return this.visualManager.getVisualData();
  }

  @Override
  public final Visual[] getSgVisuals() {
    return this.visualManager.getSgVisuals();
  }

  @Override
  protected final SimpleAppearance[] getSgPaintAppearances() {
    return this.visualManager.getSgAppearances();
  }

  @Override
  protected final SimpleAppearance[] getSgOpacityAppearances() {
    return this.getSgPaintAppearances();
  }

  @Override
  protected InstanceProperty[] getScaleProperties() {
    return this.visualManager.getScaleProperties();
  }

  @Override
  public Dimension3 getScale() {
    return this.visualManager.getScale();
  }

  @Override
  public void setScale(Dimension3 scale) {
    if (this.visualManager.hasSgScalable()) {
      this.visualManager.setScaleViaSgScalable(scale);
    } else {
      this.visualManager.setScaleOnVisuals(scale);
      this.hierarchyManager.setScaleOnJoints(scale);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Delegations — HierarchyManager
  // ════════════════════════════════════════════════════════════════════════════

  public JointImp getJointImplementation(JointId jointId) {
    return this.hierarchyManager.getJointImplementation(jointId);
  }

  //String based lookup for DynamicJointIds
  public JointImp getJointImplementation(String jointName) {
    return this.hierarchyManager.getJointImplementation(jointName);
  }

  public JointId[] getJointIdArray(JointArrayId jointArrayId) {
    return this.hierarchyManager.getJointIdArray(jointArrayId);
  }

  public Iterable<JointImp> getJoints() {
    return this.hierarchyManager.getJoints();
  }

  public void setAllJointPivotsVisible(boolean isPivotVisible) {
    this.hierarchyManager.setAllJointPivotsVisible(isPivotVisible);
  }

  public void treeWalk(TreeWalkObserver observer) {
    this.hierarchyManager.treeWalk(observer);
  }

  public void straightenOutJoints() {
    this.hierarchyManager.straightenOutJoints();
  }

  public List<JointImp> getInclusiveListOfJointsBetween(JointImp jointA, JointImp jointB, List<Bone.Direction> directions) {
    return this.hierarchyManager.getInclusiveListOfJointsBetween(jointA, jointB, directions, this);
  }

  public List<JointImp> getInclusiveListOfJointsBetween(JointId idA, JointId idB, List<Bone.Direction> directions) {
    return this.getInclusiveListOfJointsBetween(this.getJointImplementation(idA), this.getJointImplementation(idB), directions);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Offset helpers (use this as reference frame)
  // ════════════════════════════════════════════════════════════════════════════

  protected Vector4 getFrontOffsetForJoint(JointImp jointImp) {
    AxisAlignedBox bbox = jointImp.getAxisAlignedMinimumBoundingBox(this);
    Point3 point = bbox.getCenterOfFrontFace();
    return new Vector4(point.x(), point.y(), point.z(), 1);
  }

  protected Vector4 getTopOffsetForJoint(JointImp jointImp) {
    AxisAlignedBox bbox = jointImp.getAxisAlignedMinimumBoundingBox(this);
    Point3 point = bbox.getCenterOfTopFace();
    return new Vector4(point.x(), point.y(), point.z(), 1);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Bounding box / Size
  // ════════════════════════════════════════════════════════════════════════════

  public AxisAlignedBox getAxisAlignedMinimumBoundingBox(ReferenceFrame asSeenBy, boolean ignoreJointOrientations) {
    AffineMatrix4x4 trans = this.getTransformation(asSeenBy);
    CumulativeBound cumulativeBound = new CumulativeBound();
    this.updateCumulativeBound(cumulativeBound, trans, ignoreJointOrientations);
    return cumulativeBound.getBoundingBox();
  }

  public AxisAlignedBox getAxisAlignedMinimumBoundingBox(boolean ignoreJointOrientations) {
    return getAxisAlignedMinimumBoundingBox(AsSeenBy.SELF, ignoreJointOrientations);
  }

  @Override
  public AxisAlignedBox getAxisAlignedMinimumBoundingBox(ReferenceFrame asSeenBy) {
    return getAxisAlignedMinimumBoundingBox(asSeenBy, true);
  }

  @Override
  public AxisAlignedBox getAxisAlignedMinimumBoundingBox() {
    return getAxisAlignedMinimumBoundingBox(AsSeenBy.SELF, true);
  }

  @Override
  public AxisAlignedBox getDynamicAxisAlignedMinimumBoundingBox(ReferenceFrame asSeenBy) {
    return getAxisAlignedMinimumBoundingBox(asSeenBy, false);
  }

  @Override
  public AxisAlignedBox getDynamicAxisAlignedMinimumBoundingBox() {
    return getDynamicAxisAlignedMinimumBoundingBox(AsSeenBy.SELF);
  }

  @Override
  public Dimension3 getSize() {
    return getAxisAlignedMinimumBoundingBox().getSize();
  }

  public Dimension3 getSize(boolean ignoreJointOrientations) {
    return getAxisAlignedMinimumBoundingBox(ignoreJointOrientations).getSize();
  }

  @Override
  public void setSize(Dimension3 size) {
    setScale(getScaleForSize(size));
  }

  protected void updateCumulativeBound(CumulativeBound rv, AffineMatrix4x4 trans, boolean ignoreJointOrientations) {
    for (Visual sgVisual : this.getSgVisuals()) {
      if (sgVisual instanceof SkeletonVisual visual) {
        rv.addSkeletonVisual(visual, trans, ignoreJointOrientations);
      } else {
        rv.add(sgVisual, trans);
      }
    }
  }

  @Override
  protected void updateCumulativeBound(CumulativeBound rv, AffineMatrix4x4 trans) {
    updateCumulativeBound(rv, trans, true);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Visualization
  // ════════════════════════════════════════════════════════════════════════════

  private JointedModelVisualization visualization;

  @Override
  protected Leaf getVisualization() {
    if (this.visualization == null) {
      this.visualization = new JointedModelVisualization(this);
    }
    return this.visualization;
  }

  @Override
  public void showVisualization() {
    this.getVisualization().setParent(this.getSgComposite());
  }

  @Override
  public void hideVisualization() {
    if (this.visualization != null) {
      this.visualization.setParent(null);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Animation
  // ════════════════════════════════════════════════════════════════════════════

  public void animateStraightenOutJoints(double duration, Style style) {
    duration = adjustDurationIfNecessary(duration);
    if (EpsilonUtilities.isWithinReasonableEpsilon(duration, RIGHT_NOW)) {
      this.straightenOutJoints();
    } else {
      final List<JointHierarchyManager.JointData> jointDataList = hierarchyManager.collectStraightenData();
      class StraightenOutJointsAnimation extends DurationBasedAnimation {
        public StraightenOutJointsAnimation(double duration, Style style) {
          super(duration, style);
        }

        @Override
        protected void prologue() {
        }

        @Override
        protected void setPortion(double portion) {
          for (JointHierarchyManager.JointData jointData : jointDataList) {
            jointData.setPortion(portion);
          }
        }

        @Override
        protected void epilogue() {
          for (JointHierarchyManager.JointData jointData : jointDataList) {
            jointData.epilogue();
          }
        }

        @Override
        public Animated getAnimated() {
          return JointedModelImp.this;
        }
      }
      perform(new StraightenOutJointsAnimation(duration, style));
    }
  }

  public void strikePose(Pose<A> pose, double duration, Style style) {
    ProgramImp program = getProgram();
    if (program == null) {
      return;
    }
    program.perform(new PoseAnimation(duration, style, this, pose), null);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Fields
  // ════════════════════════════════════════════════════════════════════════════

  private final A abstraction;
  private final JointedModelResourceBinder<R> resourceBinder;
  private final JointHierarchyManager<R> hierarchyManager;
  private final JointedModelVisualManager<R> visualManager;
}
