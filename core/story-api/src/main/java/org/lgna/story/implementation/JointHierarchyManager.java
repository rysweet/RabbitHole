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

import edu.cmu.cs.dennisc.java.util.Lists;
import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.render.gl.imp.adapters.AdapterFactory;
import edu.cmu.cs.dennisc.scenegraph.*;
import edu.cmu.cs.dennisc.scenegraph.bound.CumulativeBound;
import org.alice.math.immutable.*;
import org.lgna.ik.core.solver.Bone;
import org.lgna.ik.core.solver.Bone.Direction;
import org.lgna.story.SJoint;
import org.lgna.story.implementation.JointedModelImp.TreeWalkObserver;
import org.lgna.story.resources.JointArrayId;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.JointedModelResource;

import java.util.*;

/**
 * Manages joint hierarchy: JointImpWrappers, joint maps, tree walking,
 * IK chain computation, and pose/straighten operations extracted from JointedModelImp.
 */
class JointHierarchyManager<R extends JointedModelResource> {

  private final JointedModelResourceBinder<R> resourceBinder;
  private final Map<JointId, JointImpWrapper> mapIdToJoint = Maps.newHashMap();
  private final Map<JointArrayId, JointId[]> mapArrayIdToJointIdArray = Maps.newHashMap();
  // Cached derived structures, invalidated on build/update
  private List<JointImp> cachedRootJoints;
  private List<JointImp> cachedJointsDfs;

  JointHierarchyManager(JointedModelResourceBinder<R> resourceBinder) {
    this.resourceBinder = Objects.requireNonNull(resourceBinder, "resourceBinder");
  }

  private void invalidateCaches() {
    cachedRootJoints = null;
    cachedJointsDfs = null;
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  JointImpWrapper — wraps an inner JointImp for resource swapping
  // ════════════════════════════════════════════════════════════════════════════

  private class JointImpWrapper extends JointImp {
    public JointImpWrapper(JointedModelImp<?, ?> jointedModelImp, JointImp joint) {
      super(jointedModelImp);
      this.internalJointImp = joint;
    }

    @Override
    public final void setAbstraction(SJoint abstraction) {
      super.setAbstraction(abstraction);
      if (this.internalJointImp != null) {
        this.internalJointImp.setAbstraction(abstraction);
      }
    }

    @Override
    public JointImp getJointParent() {
      return jointParentWrapper;
    }

    @Override
    public List<JointImp> getJointChildren() {
      return jointChildrenWrapper;
    }

    @Override
    void setJointParent(JointImp jointParent) {
      if (this.jointParentWrapper != null) {
        this.jointParentWrapper.getJointChildren().remove(this);
      }
      this.jointParentWrapper = jointParent;
      if (this.jointParentWrapper != null) {
        jointParent.getJointChildren().add(this);
      }
    }

    @Override
    public void setScale(Dimension3 scale) {
      internalJointImp.setScale(scale);
    }

    @Override
    public boolean isReoriented() {
      return internalJointImp.isReoriented();
    }

    @Override
    public boolean isRelocated() {
      return internalJointImp.isRelocated();
    }

    @Override
    protected void copyOnto(JointImp newJoint) {
      internalJointImp.copyOnto(newJoint);
      if (JointHierarchyManager.this.resourceBinder.isSims()) {
        // Alice models reuse joints, but Sims regenerate them.
        // Creating the adapter is the public mechanism to rebuild internal listeners and show joint changes
        AdapterFactory.getAdapterFor(newJoint.getSgComposite());
      }
      if (getAbstraction() != null) {
        newJoint.setAbstraction(getAbstraction());
      }
    }

    @Override
    public String getName() {
      return internalJointImp.getJointId().toString();
    }

    @Override
    public SceneImp getScene() {
      return this.internalJointImp.getScene();
    }

    @Override
    public JointId getJointId() {
      return internalJointImp.getJointId();
    }

    @Override
    public boolean isFreeInX() {
      return internalJointImp.isFreeInX();
    }

    @Override
    public boolean isFreeInY() {
      return internalJointImp.isFreeInY();
    }

    @Override
    public boolean isFreeInZ() {
      return internalJointImp.isFreeInZ();
    }

    void replaceWithJoint(JointImp newJoint) {
      copyOnto(newJoint);
      AbstractTransformable oldSgComposite = internalJointImp.getSgComposite();
      AbstractTransformable newSgComposite = newJoint.getSgComposite();
      for (Component child : oldSgComposite.getComponents()) {
        if (!(child instanceof ModelJoint)) {
          child.setParent(newJoint.getSgComposite());
        }
      }
      // Sims models, with regenerated joints, need to be reconnected at their root, indicated by a null parent.
      if (newSgComposite.getParent() == null) {
        // Without this, things riding on the Sim or its joints when the resource changes will go out of the scene graph and disappear.
        newSgComposite.setParent(oldSgComposite.getParent());
      }
      internalJointImp = newJoint;
    }

    @Override
    public AxisAlignedBox getAxisAlignedMinimumBoundingBox(ReferenceFrame asSeenBy) {
      return internalJointImp.getAxisAlignedMinimumBoundingBox(asSeenBy);
    }

    @Override
    public AbstractTransformable getSgComposite() {
      return internalJointImp.getSgComposite();
    }

    @Override
    protected void updateCumulativeBound(CumulativeBound rv, AffineMatrix4x4 trans) {
      internalJointImp.updateCumulativeBound(rv, trans);
    }

    @Override
    public UnitQuaternion getOriginalOrientation() {
      return internalJointImp.getOriginalOrientation();
    }

    @Override
    public AffineMatrix4x4 getScaledOriginalTransformation() {
      return internalJointImp.getScaledOriginalTransformation();
    }

    @Override
    public AffineMatrix4x4 getLocalTransformation() {
      return internalJointImp.getLocalTransformation();
    }

    @Override
    public void setLocalTransformation(AffineMatrix4x4 transformation) {
      internalJointImp.setLocalTransformation(transformation);
    }

    @Override
    protected void postCheckSetVehicle(EntityImp vehicle) {
      internalJointImp.postCheckSetVehicle(vehicle);
    }

    @Override
    public boolean isFacing(EntityImp other) {
      return this.internalJointImp.isFacing(other);
    }

    @Override
    public void applyTranslation(double x, double y, double z, ReferenceFrame asSeenBy) {
      this.internalJointImp.applyTranslation(x, y, z, asSeenBy);
    }

    @Override
    public void applyRotationInRadians(Vector3 axis, double angleInRadians, ReferenceFrame asSeenBy) {
      this.internalJointImp.applyRotationInRadians(axis, angleInRadians, asSeenBy);
    }

    @Override
    public boolean isPivotVisible() {
      return internalJointImp.isPivotVisible();
    }

    @Override
    public void setPivotVisible(boolean isPivotVisible) {
      internalJointImp.setPivotVisible(isPivotVisible);
    }

    private JointImp internalJointImp;
    private JointImp jointParentWrapper;
    private final List<JointImp> jointChildrenWrapper = new ArrayList<>();
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Hierarchy construction and update
  // ════════════════════════════════════════════════════════════════════════════

  void buildJointHierarchy(JointedModelImp<?, R> owner) {
    Map<JointId, JointImp> jointMap = createJointImps(owner);
    //Make all the joint wrappers and put them in the map
    for (Map.Entry<JointId, JointImp> entry : jointMap.entrySet()) {
      JointImpWrapper wrapper = new JointImpWrapper(owner, entry.getValue());
      mapIdToJoint.put(entry.getKey(), wrapper);
    }
    fillInJointArrays();
    //Go through all the wrappers and link up the parent/child relationship
    for (Map.Entry<JointId, JointImpWrapper> entry : mapIdToJoint.entrySet()) {
      entry.getValue().setJointParent(mapIdToJoint.get(entry.getKey().getParent()));
    }
    invalidateCaches();
  }

  private Map<JointId, JointImp> createJointImps(JointedModelImp<?, R> owner) {
    List<JointId> allJointIds = resolveAllJointIds(owner);
    Map<JointId, JointImp> jointMap = new HashMap<>();
    for (JointId jointId : allJointIds) {
      jointMap.put(jointId, resourceBinder.createJointImplementation(owner, jointId));
    }
    for (JointId jointId : allJointIds) {
      JointImp jointImp = jointMap.get(jointId);
      JointImp parentImp = jointMap.get(jointId.getParent());
      jointImp.setJointParent(parentImp);
      if (jointImp.getSgVehicle() == null) {
        jointImp.setVehicle(parentImp);
      }
    }
    return jointMap;
  }

  private List<JointId> resolveAllJointIds(JointedModelImp<?, R> owner) {
    List<JointId> allJointIds = resourceBinder.getAllJointIds();
    for (JointArrayId arrayId : resourceBinder.getJointArrayIds()) {
      JointId[] jointArrayIds = resourceBinder.getJointArrayIdsFromFactory(owner, arrayId);
      Collections.addAll(allJointIds, jointArrayIds);
    }
    return allJointIds;
  }

  private void fillInJointArrays() {
    for (JointArrayId arrayId : resourceBinder.getJointArrayIds()) {
      mapArrayIdToJointIdArray.put(arrayId, findJointsMatching(arrayId.getElementNamePattern()));
    }
  }

  private JointId[] findJointsMatching(String prefix) {
    return mapIdToJoint.keySet().stream()
                       .filter(jointId -> jointId.toString().startsWith(prefix))
                       .sorted(Comparator.comparing(JointId::toString))
                       .toArray(JointId[]::new);
  }

  void updateSkeleton(JointedModelImp<?, R> owner) {
    Map<JointId, JointImp> newJoints = createJointImps(owner);
    matchNewDataToExistingJoints(newJoints);
    //Make joint wrappers for new entries and put them in the map
    for (Map.Entry<JointId, JointImp> entry : newJoints.entrySet()) {
      if (!mapIdToJoint.containsKey(entry.getKey())) {
        mapIdToJoint.put(entry.getKey(), new JointImpWrapper(owner, entry.getValue()));
      }
    }
    mapArrayIdToJointIdArray.clear();
    fillInJointArrays();
    invalidateCaches();
  }

  private void matchNewDataToExistingJoints(Map<JointId, JointImp> newJoints) {
    List<JointId> toRemove = new ArrayList<>();
    for (JointId oldJointId : mapIdToJoint.keySet()) {
      if (newJoints.containsKey(oldJointId)) {
        mapIdToJoint.get(oldJointId).replaceWithJoint(newJoints.get(oldJointId));
      } else {
        toRemove.add(oldJointId);
      }
    }
    // Order from outer-most toward root to always remove a leaf
    toRemove.sort(JointId::descendantComparison);
    for (JointId id : toRemove) {
      JointImpWrapper impToRemove = this.mapIdToJoint.remove(id);
      AbstractTransformable sgJoint = impToRemove.getSgComposite();
      if (!impToRemove.getJointChildren().isEmpty()) {
        Logger.severe("Removing a joint with child joints. There is a problem with this resource and may lead to errors.");
      }
      impToRemove.setJointParent(null);
      sgJoint.setParent(null);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Joint lookup and queries
  // ════════════════════════════════════════════════════════════════════════════

  JointImp getJointImplementation(JointId jointId) {
    return this.mapIdToJoint.get(jointId);
  }

  //String based lookup for DynamicJointIds
  JointImp getJointImplementation(String jointName) {
    for (Map.Entry<JointId, JointImpWrapper> entry : this.mapIdToJoint.entrySet()) {
      if (entry.getKey().toString().equals(jointName)) {
        return entry.getValue();
      }
    }
    return null;
  }

  JointId[] getJointIdArray(JointArrayId jointArrayId) {
    return this.mapArrayIdToJointIdArray.get(jointArrayId);
  }

  boolean isEmpty() {
    return mapIdToJoint.isEmpty();
  }

  Collection<? extends JointImp> getJointWrappers() {
    return mapIdToJoint.values();
  }

  List<JointImp> getRootJointImps() {
    if (cachedRootJoints == null) {
      List<JointImp> rootJoints = new ArrayList<>();
      for (Map.Entry<JointId, JointImpWrapper> entry : mapIdToJoint.entrySet()) {
        if (entry.getKey().getParent() == null) {
          rootJoints.add(entry.getValue());
        }
      }
      cachedRootJoints = Collections.unmodifiableList(rootJoints);
    }
    return cachedRootJoints;
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Tree walk
  // ════════════════════════════════════════════════════════════════════════════

  private void treeWalk(JointImp parentImp, TreeWalkObserver observer) {
    if (parentImp != null) {
      observer.pushJoint(parentImp);
      for (JointImp childImp : parentImp.getJointChildren()) {
        if (childImp != null) {
          observer.handleBone(parentImp, childImp);
        }
      }
      observer.popJoint(parentImp);
      for (JointImp childImp : parentImp.getJointChildren()) {
        treeWalk(childImp, observer);
      }
    }
  }

  void treeWalk(TreeWalkObserver observer) {
    for (JointImp root : this.getRootJointImps()) {
      this.treeWalk(root, observer);
    }
  }

  Iterable<JointImp> getJoints() {
    if (cachedJointsDfs == null) {
      List<JointImp> rv = new ArrayList<>();
      this.treeWalk(new TreeWalkObserver() {
        @Override
        public void pushJoint(JointImp joint) {
          rv.add(joint);
        }

        @Override
        public void handleBone(JointImp parent, JointImp child) {
        }

        @Override
        public void popJoint(JointImp joint) {
        }
      });
      cachedJointsDfs = Collections.unmodifiableList(rv);
    }
    return cachedJointsDfs;
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Joint property operations
  // ════════════════════════════════════════════════════════════════════════════

  void setAllJointPivotsVisible(boolean isPivotVisible) {
    for (JointImpWrapper joint : this.mapIdToJoint.values()) {
      joint.setPivotVisible(isPivotVisible);
    }
  }

  void setScaleOnJoints(Dimension3 scale) {
    for (JointImp jointImp : this.mapIdToJoint.values()) {
      jointImp.setScale(scale);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  IK chain computation
  // ════════════════════════════════════════════════════════════════════════════

  private enum AddOp {
    PREPEND {
      @Override
      public List<JointImp> add(List<JointImp> rv, JointImp joint, List<Bone.Direction> directions, Bone.Direction direction) {
        rv.addFirst(joint);
        if (directions != null) {
          directions.addFirst(direction);
        }
        return rv;
      }
    }, APPEND {
      @Override
      public List<JointImp> add(List<JointImp> rv, JointImp joint, List<Bone.Direction> directions, Bone.Direction direction) {
        rv.add(joint);
        if (directions != null) {
          directions.add(direction);
        }
        return rv;
      }
    };

    public abstract List<JointImp> add(List<JointImp> rv, JointImp joint, List<Bone.Direction> directions, Bone.Direction direction);
  }

  private List<JointImp> updateJointsBetween(List<JointImp> rv, List<Bone.Direction> directions, JointImp joint, EntityImp ancestorToReach, AddOp addOp) {
    if (joint != ancestorToReach) {
      JointId parentId = joint.getJointId().getParent();
      if (parentId != null) {
        JointImp parent = this.getJointImplementation(parentId);
        this.updateJointsBetween(rv, directions, parent, ancestorToReach, addOp);
      }
    }
    Bone.Direction direction;
    if (addOp == AddOp.APPEND) {
      direction = Bone.Direction.DOWNSTREAM;
    } else {
      direction = Bone.Direction.UPSTREAM;
    }
    addOp.add(rv, joint, directions, direction);
    return rv;
  }

  List<JointImp> getInclusiveListOfJointsBetween(JointImp jointA, JointImp jointB, List<Bone.Direction> directions, EntityImp owner) {
    assert jointA != null : this;
    assert jointB != null : this;
    List<JointImp> rv = Lists.newLinkedList();
    if (jointA == jointB) {
      rv.add(jointA);
      directions.add(Bone.Direction.DOWNSTREAM);
    } else {
      if (jointA.isDescendantOf(jointB)) {
        this.updateJointsBetween(rv, directions, jointA, jointB, AddOp.PREPEND);
      } else if (jointB.isDescendantOf(jointA)) {
        this.updateJointsBetween(rv, directions, jointB, jointA, AddOp.APPEND);
      } else {
        //It shouldn't even use the joint on which direction is changed (the common ancestor)
        //that's what the below call does
        this.updateJointsUpToAndExcludingCommonAncestor(rv, directions, jointA, jointB, owner);
      }
    }
    return rv;
  }

  private void updateJointsUpToAndExcludingCommonAncestor(List<JointImp> rvPath, List<Direction> rvDirections, JointImp jointA, JointImp jointB, EntityImp owner) {
    List<JointImp> pathA = Lists.newLinkedList();
    List<JointImp> pathB = Lists.newLinkedList();

    List<Direction> directionsA = new ArrayList<Direction>();
    List<Direction> directionsB = new ArrayList<Direction>();

    this.updateJointsBetween(pathA, directionsA, jointA, owner, AddOp.PREPEND);
    this.updateJointsBetween(pathB, directionsB, jointB, owner, AddOp.APPEND);

    JointImp commonAncestor = null;

    for (JointImp jointInA : pathA) {
      if (pathB.contains(jointInA)) {
        commonAncestor = jointInA;
        break;
      }
    }

    if (commonAncestor == null) {
      throw new RuntimeException("Probably not connected with a chain.");
    }

    ListIterator<JointImp> pathAIterator = pathA.listIterator(pathA.size());
    ListIterator<Direction> directionsAIterator = directionsA.listIterator(directionsA.size());
    for (; pathAIterator.hasPrevious(); ) {
      JointImp jointImp = pathAIterator.previous();
      directionsAIterator.previous();

      pathAIterator.remove();
      directionsAIterator.remove();

      if (jointImp == commonAncestor) {
        break;
      }
    }

    ListIterator<JointImp> pathBIterator = pathB.listIterator();
    ListIterator<Direction> directionsBIterator = directionsB.listIterator();
    for (; pathBIterator.hasNext(); ) {
      JointImp jointImp = pathBIterator.next();
      directionsBIterator.next();

      pathBIterator.remove();
      directionsBIterator.remove();

      if (jointImp == commonAncestor) {
        break;
      }
    }

    rvPath.addAll(pathA);
    rvPath.addAll(pathB);
    rvDirections.addAll(directionsA);
    rvDirections.addAll(directionsB);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Straighten / Pose support
  // ════════════════════════════════════════════════════════════════════════════

  static class JointData {
    private final JointImp jointImp;
    private final UnitQuaternion q0;
    private final UnitQuaternion q1;

    public JointData(JointImp jointImp) {
      this.jointImp = jointImp;
      this.q0 = this.jointImp.getLocalOrientation().asUnitQuaternion();
      UnitQuaternion q = this.jointImp.getOriginalOrientation();
      this.q1 = ((q == null) || this.q0.isAlignedWith(q)) ? null : q;
    }

    public void setPortion(double portion) {
      if (this.q1 != null) {
        this.jointImp.setLocalOrientationOnly(this.q0.interpolate(this.q1, portion).asMatrix3x3());
      }
    }

    public void epilogue() {
      if (this.q1 != null) {
        this.jointImp.setLocalOrientationOnly(this.q1.asMatrix3x3());
      }
    }
  }

  private static class StraightenTreeWalkObserver implements TreeWalkObserver {
    private final List<JointData> list = new ArrayList<>();

    @Override
    public void pushJoint(JointImp jointImp) {
      list.add(new JointData(jointImp));
    }

    @Override
    public void handleBone(JointImp parent, JointImp child) {
    }

    @Override
    public void popJoint(JointImp joint) {
    }
  }

  List<JointData> collectStraightenData() {
    StraightenTreeWalkObserver treeWalkObserver = new StraightenTreeWalkObserver();
    this.treeWalk(treeWalkObserver);
    return treeWalkObserver.list;
  }

  void straightenOutJoints() {
    for (JointData jointData : collectStraightenData()) {
      jointData.epilogue();
    }
  }
}
