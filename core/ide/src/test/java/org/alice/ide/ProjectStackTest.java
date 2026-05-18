package org.alice.ide;

import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.NamedUserType;

import static org.junit.Assert.*;

/**
 * Tests for {@link ProjectStack} — stack push/pop operations.
 * Only tests the stack operations; IDE fallback is not tested (requires running IDE).
 */
public class ProjectStackTest {

  private static Project createTestProject() {
    NamedUserType sceneType = new NamedUserType();
    sceneType.name.setValue("TestScene");
    return new Project(sceneType, Project.SceneCameraType.WindowCamera);
  }

  @Test
  public void pushAndPop_returnsSameProject() {
    Project project = createTestProject();
    ProjectStack.pushProject(project);
    Project popped = ProjectStack.popProject();
    assertSame(project, popped);
  }

  @Test
  public void pushTwoPop_lastInFirstOut() {
    Project p1 = createTestProject();
    Project p2 = createTestProject();
    ProjectStack.pushProject(p1);
    ProjectStack.pushProject(p2);
    assertSame(p2, ProjectStack.popProject());
    assertSame(p1, ProjectStack.popProject());
  }

  @Test
  public void popAndCheck_matchingProject_succeeds() {
    Project project = createTestProject();
    ProjectStack.pushProject(project);
    Project popped = ProjectStack.popAndCheckProject(project);
    assertSame(project, popped);
  }

  @Test
  public void popAndCheck_mismatchingProject_stillPops() {
    Project actual = createTestProject();
    Project expected = createTestProject();
    ProjectStack.pushProject(actual);
    Project popped = ProjectStack.popAndCheckProject(expected);
    assertSame(actual, popped);
  }

  @Test
  public void peekProject_afterPush_returnsProject() {
    Project project = createTestProject();
    ProjectStack.pushProject(project);
    try {
      Project peeked = ProjectStack.peekProject();
      assertSame(project, peeked);
    } finally {
      ProjectStack.popProject();
    }
  }

  @Test
  public void peekUpToDateProject_afterPush_returnsProject() {
    Project project = createTestProject();
    ProjectStack.pushProject(project);
    try {
      Project peeked = ProjectStack.peekUpToDateProject();
      assertSame(project, peeked);
    } finally {
      ProjectStack.popProject();
    }
  }
}
