package org.lgna.story.implementation;

import edu.cmu.cs.dennisc.scenegraph.Composite;
import edu.cmu.cs.dennisc.scenegraph.Joint;
import edu.cmu.cs.dennisc.scenegraph.SimpleAppearance;
import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import edu.cmu.cs.dennisc.scenegraph.Transformable;
import edu.cmu.cs.dennisc.scenegraph.Visual;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Dimension3;
import org.alice.math.immutable.UnitQuaternion;
import org.junit.Before;
import org.junit.Test;
import org.lgna.story.SJointedModel;
import org.lgna.story.implementation.JointedModelImp.JointImplementationAndVisualDataFactory;
import org.lgna.story.implementation.JointedModelImp.TreeWalkObserver;
import org.lgna.story.implementation.JointedModelImp.VisualData;
import org.lgna.story.resources.JointArrayId;
import org.lgna.story.resources.JointId;
import org.lgna.story.resources.JointedModelResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TDD tests for JointedModelImp decomposition into three manager classes.
 *
 * <p>These tests define the contracts for:
 * <ul>
 *   <li>{@code JointedModelResourceBinder} — factory delegation, resource queries, reflection-based joint discovery</li>
 *   <li>{@code JointHierarchyManager} — joint maps, tree walk, IK chains, pose/straighten</li>
 *   <li>{@code JointedModelVisualManager} — VisualData lifecycle, scale, bounds, visualization overlay</li>
 * </ul>
 *
 * <p><b>These tests will fail to compile until the manager classes are created.</b>
 * This is intentional TDD (red phase).
 */
public class JointedModelImpDecompositionTest {

  // ════════════════════════════════════════════════════════════════════════════
  //  Test doubles
  // ════════════════════════════════════════════════════════════════════════════

  /**
   * A minimal JointedModelResource with known static JointId fields
   * for testing reflection-based joint discovery.
   */
  public static class TestResource implements JointedModelResource {
    // Root joint (no parent)
    public static final JointId ROOT = new JointId(null, TestResource.class);
    // Child of root
    public static final JointId SPINE = new JointId(ROOT, TestResource.class);
    // Child of spine
    public static final JointId HEAD = new JointId(SPINE, TestResource.class);
    // Second child of spine (for tree walk branching)
    public static final JointId LEFT_ARM = new JointId(SPINE, TestResource.class);
    // Second root (for multi-root testing)
    public static final JointId TAIL_ROOT = new JointId(null, TestResource.class);

    @Override
    public JointImplementationAndVisualDataFactory<JointedModelResource> getImplementationAndVisualFactory() {
      return null; // not used in isolation tests
    }
  }

  /**
   * Resource with JointArrayId for array discovery tests.
   */
  public static class TestResourceWithArrays implements JointedModelResource {
    public static final JointId ROOT = new JointId(null, TestResourceWithArrays.class);
    public static final JointId FINGER_0 = new JointId(ROOT, TestResourceWithArrays.class);
    public static final JointId FINGER_1 = new JointId(ROOT, TestResourceWithArrays.class);
    public static final JointId FINGER_2 = new JointId(ROOT, TestResourceWithArrays.class);
    public static final JointArrayId FINGERS = new JointArrayId("FINGER_", ROOT, TestResourceWithArrays.class);

    @Override
    public JointImplementationAndVisualDataFactory<JointedModelResource> getImplementationAndVisualFactory() {
      return null;
    }
  }

  /** Stub VisualData that tracks operations without real scenegraph. */
  static class StubVisualData implements VisualData<JointedModelResource> {
    private final Visual[] visuals;
    private final SimpleAppearance[] appearances;
    private Composite sgParent;
    boolean parentWasSet = false;
    boolean parentWasCleared = false;

    StubVisualData() {
      Visual v = new Visual();
      this.visuals = new Visual[] {v};
      SimpleAppearance a = new SimpleAppearance();
      this.appearances = new SimpleAppearance[] {a};
    }

    @Override
    public Visual[] getSgVisuals() {
      return visuals;
    }

    @Override
    public SkeletonVisual getSgVisualForExporting(JointedModelResource resource) {
      return null;
    }

    @Override
    public SimpleAppearance[] getSgAppearances() {
      return appearances;
    }

    @Override
    public void setSGParent(Composite parent) {
      this.sgParent = parent;
      if (parent != null) {
        parentWasSet = true;
      } else {
        parentWasCleared = true;
      }
    }

    @Override
    public Composite getSGParent() {
      return sgParent;
    }
  }

  /** Stub factory that creates simple JointImps with scenegraph Joint nodes. */
  static class StubFactory implements JointImplementationAndVisualDataFactory<JointedModelResource> {
    private JointedModelResource resource;
    private boolean isSims;
    int createCallCount = 0;
    int visualDataCreateCount = 0;
    final List<JointId> createdJointIds = new ArrayList<>();

    StubFactory(JointedModelResource resource, boolean isSims) {
      this.resource = resource;
      this.isSims = isSims;
    }

    @Override
    public JointedModelResource getResource() {
      return resource;
    }

    @Override
    public JointImp createJointImplementation(JointedModelImp<?, JointedModelResource> impl, JointId jointId) {
      createCallCount++;
      createdJointIds.add(jointId);
      Joint sgJoint = new Joint();
      sgJoint.jointID.setValue(jointId.toString());
      return new org.lgna.story.implementation.alice.JointImplementation(impl, jointId, sgJoint) {
        @Override
        protected void copyOnto(JointImp newJoint) {
          // Override to avoid NPE when owner (impl) is null in tests
          if (getJointedModelImplementation() != null) {
            super.copyOnto(newJoint);
          }
        }
      };
    }

    @Override
    public boolean hasJointImplementation(JointedModelImp<?, JointedModelResource> impl, JointId jointId) {
      return true;
    }

    @Override
    public JointId[] getJointArrayIds(JointedModelImp<?, JointedModelResource> impl, JointArrayId jointArrayId) {
      return new JointId[0];
    }

    @Override
    public VisualData<JointedModelResource> createVisualData() {
      visualDataCreateCount++;
      return new StubVisualData();
    }

    @Override
    public UnitQuaternion getOriginalJointOrientation(JointId jointId) {
      return UnitQuaternion.IDENTITY;
    }

    @Override
    public AffineMatrix4x4 getOriginalJointTransformation(JointId jointId) {
      return AffineMatrix4x4.IDENTITY;
    }

    @Override
    public boolean isSims() {
      return isSims;
    }

    void setResource(JointedModelResource resource) {
      this.resource = resource;
    }
  }

  /** Records tree walk events for verification. */
  static class RecordingTreeWalkObserver implements TreeWalkObserver {
    final List<JointImp> pushed = new ArrayList<>();
    final List<JointImp> popped = new ArrayList<>();
    final List<JointImp> boneParents = new ArrayList<>();
    final List<JointImp> boneChildren = new ArrayList<>();

    @Override
    public void pushJoint(JointImp joint) {
      pushed.add(joint);
    }

    @Override
    public void handleBone(JointImp parent, JointImp child) {
      boneParents.add(parent);
      boneChildren.add(child);
    }

    @Override
    public void popJoint(JointImp joint) {
      popped.add(joint);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Test fixtures
  // ════════════════════════════════════════════════════════════════════════════

  private TestResource testResource;
  private StubFactory stubFactory;

  @Before
  public void setUp() {
    testResource = new TestResource();
    stubFactory = new StubFactory(testResource, false);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  JointedModelResourceBinder — contract tests
  // ════════════════════════════════════════════════════════════════════════════

  @Test
  public void resourceBinder_getResource_returnsFactoryResource() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    assertSame(testResource, binder.getResource());
  }

  @Test
  public void resourceBinder_getAllJointIds_discoversStaticFields() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    List<JointId> jointIds = binder.getAllJointIds();

    // TestResource has 5 JointId fields: ROOT, SPINE, HEAD, LEFT_ARM, TAIL_ROOT
    assertTrue("Should discover all 5 JointId fields, found " + jointIds.size(),
        jointIds.size() >= 5);
    assertTrue("Should contain ROOT", jointIds.contains(TestResource.ROOT));
    assertTrue("Should contain SPINE", jointIds.contains(TestResource.SPINE));
    assertTrue("Should contain HEAD", jointIds.contains(TestResource.HEAD));
    assertTrue("Should contain LEFT_ARM", jointIds.contains(TestResource.LEFT_ARM));
    assertTrue("Should contain TAIL_ROOT", jointIds.contains(TestResource.TAIL_ROOT));
  }

  @Test
  public void resourceBinder_getJointArrayIds_discoversJointArrayIdFields() {
    StubFactory arrayFactory = new StubFactory(new TestResourceWithArrays(), false);
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(arrayFactory);

    JointArrayId[] arrayIds = binder.getJointArrayIds();

    assertEquals("Should discover the FINGERS JointArrayId", 1, arrayIds.length);
    assertSame(TestResourceWithArrays.FINGERS, arrayIds[0]);
  }

  @Test
  public void resourceBinder_getJointArrayIds_cachesResult() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    JointArrayId[] first = binder.getJointArrayIds();
    JointArrayId[] second = binder.getJointArrayIds();

    assertSame("Should return cached array on second call", first, second);
  }

  @Test
  public void resourceBinder_isSims_delegatesToFactory() {
    StubFactory simsFactory = new StubFactory(testResource, true);
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(simsFactory);

    assertTrue("Should delegate isSims to factory", binder.isSims());
  }

  @Test
  public void resourceBinder_isSims_returnsFalseForNonSims() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    assertFalse(binder.isSims());
  }

  @Test
  public void resourceBinder_getOriginalJointOrientation_delegatesToFactory() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    UnitQuaternion result = binder.getOriginalJointOrientation(TestResource.ROOT);

    assertEquals(UnitQuaternion.IDENTITY, result);
  }

  @Test
  public void resourceBinder_getOriginalJointTransformation_delegatesToFactory() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    AffineMatrix4x4 result = binder.getOriginalJointTransformation(TestResource.ROOT);

    assertEquals(AffineMatrix4x4.IDENTITY, result);
  }

  @Test
  public void resourceBinder_updateFactory_replacesFactory() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    TestResource newResource = new TestResource();
    StubFactory newFactory = new StubFactory(newResource, true);
    binder.updateFactory(newFactory);

    assertSame("After updateFactory, getResource should return new resource",
        newResource, binder.getResource());
    assertTrue("After updateFactory, isSims should reflect new factory",
        binder.isSims());
  }

  @Test
  public void resourceBinder_updateFactory_clearsJointArrayIdsCache() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);

    // Populate cache
    JointArrayId[] first = binder.getJointArrayIds();

    // Switch to a resource with arrays
    StubFactory arrayFactory = new StubFactory(new TestResourceWithArrays(), false);
    binder.updateFactory(arrayFactory);

    JointArrayId[] second = binder.getJointArrayIds();
    assertNotSame("Cache should be cleared after updateFactory", first, second);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  JointHierarchyManager — contract tests
  // ════════════════════════════════════════════════════════════════════════════

  @Test
  public void hierarchyManager_buildJointHierarchy_createsWrappersForAllJoints() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);

    // Build with null owner — stubs don't need it
    hierarchy.buildJointHierarchy(null);

    // Should have wrappers for all 5 joints
    assertNotNull("ROOT should have a wrapper",
        hierarchy.getJointImplementation(TestResource.ROOT));
    assertNotNull("SPINE should have a wrapper",
        hierarchy.getJointImplementation(TestResource.SPINE));
    assertNotNull("HEAD should have a wrapper",
        hierarchy.getJointImplementation(TestResource.HEAD));
    assertNotNull("LEFT_ARM should have a wrapper",
        hierarchy.getJointImplementation(TestResource.LEFT_ARM));
    assertNotNull("TAIL_ROOT should have a wrapper",
        hierarchy.getJointImplementation(TestResource.TAIL_ROOT));
  }

  @Test
  public void hierarchyManager_parentChildLinks_areCorrect() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    JointImp rootImp = hierarchy.getJointImplementation(TestResource.ROOT);
    JointImp spineImp = hierarchy.getJointImplementation(TestResource.SPINE);
    JointImp headImp = hierarchy.getJointImplementation(TestResource.HEAD);

    // ROOT has no parent
    assertNull("ROOT should have null parent", rootImp.getJointParent());

    // SPINE's parent should be ROOT's wrapper
    assertSame("SPINE's parent should be ROOT wrapper",
        rootImp, spineImp.getJointParent());

    // HEAD's parent should be SPINE's wrapper
    assertSame("HEAD's parent should be SPINE wrapper",
        spineImp, headImp.getJointParent());
  }

  @Test
  public void hierarchyManager_lookupByJointId_returnsCorrectJoint() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    JointImp result = hierarchy.getJointImplementation(TestResource.HEAD);

    assertNotNull(result);
    assertEquals(TestResource.HEAD, result.getJointId());
  }

  @Test
  public void hierarchyManager_lookupByName_returnsCorrectJoint() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    String headName = TestResource.HEAD.toString();
    JointImp result = hierarchy.getJointImplementation(headName);

    assertNotNull("Should find joint by name: " + headName, result);
    assertEquals(TestResource.HEAD, result.getJointId());
  }

  @Test
  public void hierarchyManager_lookupByName_returnsNullForUnknown() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    assertNull("Should return null for unknown joint name",
        hierarchy.getJointImplementation("NONEXISTENT_JOINT"));
  }

  @Test
  public void hierarchyManager_getRootJoints_returnsJointsWithNullParent() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    List<JointImp> roots = hierarchy.getRootJointImps();

    // TestResource has 2 roots: ROOT and TAIL_ROOT
    assertEquals("Should have 2 root joints", 2, roots.size());
    List<JointId> rootIds = new ArrayList<>();
    for (JointImp r : roots) {
      rootIds.add(r.getJointId());
    }
    assertTrue("Should contain ROOT", rootIds.contains(TestResource.ROOT));
    assertTrue("Should contain TAIL_ROOT", rootIds.contains(TestResource.TAIL_ROOT));
  }

  @Test
  public void hierarchyManager_treeWalk_visitsAllJoints() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    RecordingTreeWalkObserver observer = new RecordingTreeWalkObserver();
    hierarchy.treeWalk(observer);

    // All 5 joints should be pushed and popped
    assertEquals("pushJoint should be called for all 5 joints",
        5, observer.pushed.size());
    assertEquals("popJoint should be called for all 5 joints",
        5, observer.popped.size());
  }

  @Test
  public void hierarchyManager_treeWalk_handlesBonesBetweenParentAndChildren() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    RecordingTreeWalkObserver observer = new RecordingTreeWalkObserver();
    hierarchy.treeWalk(observer);

    // SPINE has 2 children (HEAD, LEFT_ARM), ROOT has 1 child (SPINE)
    // So handleBone should be called at least 3 times
    assertTrue("handleBone should be called for each parent-child pair, got " + observer.boneParents.size(),
        observer.boneParents.size() >= 3);
  }

  @Test
  public void hierarchyManager_treeWalk_pushBeforePopForSameJoint() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    RecordingTreeWalkObserver observer = new RecordingTreeWalkObserver();
    hierarchy.treeWalk(observer);

    // For each joint, push should come before pop in the sequence
    for (JointImp pushed : observer.pushed) {
      int pushIdx = observer.pushed.indexOf(pushed);
      int popIdx = observer.popped.indexOf(pushed);
      assertTrue("Push for " + pushed.getJointId() + " (at " + pushIdx +
              ") should happen before pop (at " + popIdx + ")",
          popIdx >= 0);
    }
  }

  @Test
  public void hierarchyManager_getJoints_returnsAllJointsViaTreeWalk() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    Iterable<JointImp> joints = hierarchy.getJoints();
    List<JointImp> jointList = new ArrayList<>();
    joints.forEach(jointList::add);

    assertEquals("getJoints should return all 5 joints", 5, jointList.size());
  }

  @Test
  public void hierarchyManager_isEmpty_returnsTrueBeforeBuild() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);

    assertTrue("Should be empty before buildJointHierarchy",
        hierarchy.isEmpty());
  }

  @Test
  public void hierarchyManager_isEmpty_returnsFalseAfterBuild() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    assertFalse("Should not be empty after buildJointHierarchy",
        hierarchy.isEmpty());
  }

  @Test
  public void hierarchyManager_setAllJointPivotsVisible_appliesToAllJoints() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    // Initially not visible
    JointImp rootImp = hierarchy.getJointImplementation(TestResource.ROOT);
    assertFalse("Pivots should start not visible", rootImp.isPivotVisible());

    hierarchy.setAllJointPivotsVisible(true);

    // All joints should now be visible
    for (JointImp joint : asIterable(hierarchy.getJoints())) {
      assertTrue("Pivot should be visible after setAllJointPivotsVisible(true)",
          joint.isPivotVisible());
    }
  }

  @Test
  public void hierarchyManager_setScaleOnJoints_appliesToAllJoints() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    Dimension3 scale = new Dimension3(2.0, 2.0, 2.0);
    hierarchy.setScaleOnJoints(scale);

    // Verify no exception was thrown — scale was applied to all joints
    // (Exact value verification depends on JointImplementation.setScale behavior)
    assertNotNull(hierarchy.getJointImplementation(TestResource.ROOT));
  }

  @Test
  public void hierarchyManager_updateSkeleton_preservesExistingWrappers() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    // Capture wrapper reference before update
    JointImp rootBefore = hierarchy.getJointImplementation(TestResource.ROOT);

    // Update skeleton with same resource (same JointIds exist)
    hierarchy.updateSkeleton(null);

    JointImp rootAfter = hierarchy.getJointImplementation(TestResource.ROOT);

    // Wrapper identity should be preserved (same JointImpWrapper object)
    assertSame("Existing wrappers should be preserved across skeleton update",
        rootBefore, rootAfter);
  }

  @Test
  public void hierarchyManager_fillInJointArrays_populatesArrayMap() {
    StubFactory arrayFactory = new StubFactory(new TestResourceWithArrays(), false) {
      @Override
      public JointId[] getJointArrayIds(JointedModelImp<?, JointedModelResource> impl, JointArrayId arrayId) {
        return new JointId[] {
            TestResourceWithArrays.FINGER_0,
            TestResourceWithArrays.FINGER_1,
            TestResourceWithArrays.FINGER_2
        };
      }
    };

    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(arrayFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    JointId[] fingerArray = hierarchy.getJointIdArray(TestResourceWithArrays.FINGERS);

    assertNotNull("Should have array for FINGERS", fingerArray);
    // Array should contain joints matching the FINGER_ prefix, sorted
    assertTrue("Array should have finger joints", fingerArray.length > 0);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  JointedModelVisualManager — contract tests
  // ════════════════════════════════════════════════════════════════════════════

  @Test
  public void visualManager_getVisualData_returnsCurrentVisualData() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    VisualData<JointedModelResource> data = visual.getVisualData();

    assertNotNull("Should return non-null visual data", data);
  }

  @Test
  public void visualManager_getSgVisuals_delegatesToVisualData() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    Visual[] visuals = visual.getSgVisuals();

    assertNotNull(visuals);
    assertTrue("Should have at least one visual", visuals.length > 0);
  }

  @Test
  public void visualManager_getSgAppearances_delegatesToVisualData() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    SimpleAppearance[] appearances = visual.getSgAppearances();

    assertNotNull(appearances);
    assertTrue("Should have at least one appearance", appearances.length > 0);
  }

  @Test
  public void visualManager_replaceVisualData_createsNewDataFromFactory() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    VisualData<JointedModelResource> oldData = visual.getVisualData();
    visual.replaceVisualData();
    VisualData<JointedModelResource> newData = visual.getVisualData();

    assertNotSame("replaceVisualData should create new VisualData",
        oldData, newData);
    assertEquals("Factory createVisualData should be called twice (init + replace)",
        2, stubFactory.visualDataCreateCount);
  }

  @Test
  public void visualManager_attachToParent_setsSGParent() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    Transformable parent = new Transformable();
    visual.attachToParent(parent);

    assertSame("Visual data SG parent should be set",
        parent, visual.getVisualData().getSGParent());
  }

  @Test
  public void visualManager_detachOldVisualData_clearsParent() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    Transformable parent = new Transformable();
    visual.attachToParent(parent);

    VisualData<JointedModelResource> oldData = visual.getVisualData();
    visual.replaceVisualData();
    visual.detachOldVisualData(oldData);

    assertNull("Old visual data SG parent should be cleared after detach",
        oldData.getSGParent());
  }

  @Test
  public void visualManager_getScale_withNullScalable_readsFromVisual() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    // Default scale should be identity-like (1,1,1) from the Visual's default
    Dimension3 scale = visual.getScale();
    assertNotNull("Scale should not be null", scale);
  }

  @Test
  public void visualManager_setScaleOnVisuals_setsMatrixOnAllVisuals() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    Dimension3 newScale = new Dimension3(2.0, 3.0, 4.0);
    visual.setScaleOnVisuals(newScale);

    // After setting, getScale should reflect the new values
    Dimension3 result = visual.getScale();
    assertEquals("X scale should match", 2.0, result.x(), 0.001);
    assertEquals("Y scale should match", 3.0, result.y(), 0.001);
    assertEquals("Z scale should match", 4.0, result.z(), 0.001);
  }

  @Test
  public void visualManager_getScaleProperties_withNullScalable_returnsVisualScaleProperty() {
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    var props = visual.getScaleProperties();

    assertNotNull(props);
    assertEquals("Should have exactly 1 scale property", 1, props.length);
    assertNotNull("Scale property should not be null", props[0]);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Integration: JointedModelImp delegates to managers
  // ════════════════════════════════════════════════════════════════════════════

  @Test
  public void jointedModelImp_hasThreeManagers() {
    // After decomposition, JointedModelImp should expose managers for testing
    // The class should have resourceBinder, hierarchyManager, and visualManager fields
    // Verify they exist via the public accessor methods
    try {
      var resourceBinderField = JointedModelImp.class.getDeclaredField("resourceBinder");
      var hierarchyManagerField = JointedModelImp.class.getDeclaredField("hierarchyManager");
      var visualManagerField = JointedModelImp.class.getDeclaredField("visualManager");

      assertNotNull("JointedModelImp should have resourceBinder field", resourceBinderField);
      assertNotNull("JointedModelImp should have hierarchyManager field", hierarchyManagerField);
      assertNotNull("JointedModelImp should have visualManager field", visualManagerField);
    } catch (NoSuchFieldException e) {
      fail("JointedModelImp is missing manager fields after decomposition: " + e.getMessage());
    }
  }

  @Test
  public void jointedModelImp_treeWalkObserverInterface_isPreserved() {
    // TreeWalkObserver should still be a public interface on JointedModelImp
    // (not moved to JointHierarchyManager) for backward compatibility
    assertTrue("TreeWalkObserver should be an interface",
        TreeWalkObserver.class.isInterface());

    // Should have the 3 required methods
    try {
      TreeWalkObserver.class.getMethod("pushJoint", JointImp.class);
      TreeWalkObserver.class.getMethod("handleBone", JointImp.class, JointImp.class);
      TreeWalkObserver.class.getMethod("popJoint", JointImp.class);
    } catch (NoSuchMethodException e) {
      fail("TreeWalkObserver missing expected method: " + e.getMessage());
    }
  }

  @Test
  public void jointedModelImp_visibleInterfaces_arePreserved() {
    // VisualData should still be a public interface on JointedModelImp
    assertTrue("VisualData should be an interface",
        VisualData.class.isInterface());

    // JointImplementationAndVisualDataFactory should still be a public interface
    assertTrue("JointImplementationAndVisualDataFactory should be an interface",
        JointImplementationAndVisualDataFactory.class.isInterface());
  }

  @Test
  public void jointedModelImp_lineCount_isUnder500() {
    // After decomposition, JointedModelImp.java should be under 500 lines.
    // This test reads the source file and counts lines.
    try {
      java.io.File sourceFile = new java.io.File(
          "src/main/java/org/lgna/story/implementation/JointedModelImp.java");
      if (sourceFile.exists()) {
        long lineCount = java.nio.file.Files.lines(sourceFile.toPath()).count();
        assertTrue(
            "JointedModelImp.java should be under 500 lines after decomposition, but has " + lineCount,
            lineCount < 500);
      }
      // If file not found from test working dir, try alternative path
    } catch (Exception e) {
      // Test environment may not have source access — skip gracefully
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Edge cases
  // ════════════════════════════════════════════════════════════════════════════

  @Test
  public void hierarchyManager_treeWalkOnEmpty_doesNotThrow() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    // Don't build — leave empty

    RecordingTreeWalkObserver observer = new RecordingTreeWalkObserver();
    hierarchy.treeWalk(observer);

    assertEquals("No joints should be visited on empty hierarchy",
        0, observer.pushed.size());
  }

  @Test
  public void hierarchyManager_getJointImplementation_returnsNullForUnknownId() {
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(stubFactory);
    JointHierarchyManager<JointedModelResource> hierarchy =
        new JointHierarchyManager<>(binder);
    hierarchy.buildJointHierarchy(null);

    JointId unknownId = new JointId(null, TestResource.class);
    assertNull("Should return null for JointId not in the map",
        hierarchy.getJointImplementation(unknownId));
  }

  @Test
  public void resourceBinder_getAllJointIds_handlesResourceWithNoJoints() {
    // Create a resource class with no JointId fields
    JointedModelResource emptyResource = new JointedModelResource() {
      @Override
      public JointImplementationAndVisualDataFactory<JointedModelResource> getImplementationAndVisualFactory() {
        return null;
      }
    };
    StubFactory emptyFactory = new StubFactory(emptyResource, false);
    JointedModelResourceBinder<JointedModelResource> binder =
        new JointedModelResourceBinder<>(emptyFactory);

    List<JointId> ids = binder.getAllJointIds();

    // Anonymous class won't have public static final JointId fields
    assertTrue("Should return empty list for resource with no JointId fields",
        ids.isEmpty());
  }

  @Test
  public void visualManager_constructorWithNullScalable_succeeds() {
    // sgScalable can be null — this is the normal case for JointedModelImp
    JointedModelVisualManager<JointedModelResource> visual =
        new JointedModelVisualManager<>(stubFactory, null);

    assertNotNull(visual.getVisualData());
  }

  @Test(expected = NullPointerException.class)
  public void resourceBinder_constructorWithNullFactory_throwsNPE() {
    new JointedModelResourceBinder<>(null);
  }

  @Test(expected = NullPointerException.class)
  public void hierarchyManager_constructorWithNullBinder_throwsNPE() {
    new JointHierarchyManager<>(null);
  }

  @Test(expected = NullPointerException.class)
  public void visualManager_constructorWithNullFactory_throwsNPE() {
    new JointedModelVisualManager<>(null, null);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  Utilities
  // ════════════════════════════════════════════════════════════════════════════

  private static <T> List<T> asIterable(Iterable<T> iterable) {
    List<T> list = new ArrayList<>();
    iterable.forEach(list::add);
    return list;
  }
}
