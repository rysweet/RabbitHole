package org.alice.ide.croquet.models.projecturi;

import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.NamedUserType;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for save/export operations.
 * Tests the core data structures without requiring IDE singletons.
 */
public class SaveOperationFlowExtendedTest {

  @Test
  public void namedUserType_canBeCreated() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestScene");
    assertEquals("TestScene", type.getName());
  }

  @Test
  public void namedUserType_hasUniqueId() {
    NamedUserType t1 = new NamedUserType();
    NamedUserType t2 = new NamedUserType();
    assertNotEquals(t1.getId(), t2.getId());
  }

  @Test
  public void project_canBeCreated() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("Program");
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    assertNotNull(project);
    assertSame(programType, project.getProgramType());
  }

  @Test
  public void project_getNamedUserTypes_includesProgramType() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("TestProgram");
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    assertTrue(project.getNamedUserTypes().contains(programType));
  }

  @Test
  public void project_programType_nameIsPreserved() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("MyProgram");
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    assertEquals("MyProgram", project.getProgramType().getName());
  }

  @Test
  public void namedUserType_canAddField() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("Scene");
    org.lgna.project.ast.UserField field = new org.lgna.project.ast.UserField();
    field.name.setValue("testField");
    field.valueType.setValue(org.lgna.project.ast.JavaType.getInstance(String.class));
    type.fields.add(field);
    assertEquals(1, type.fields.size());
  }

  @Test
  public void namedUserType_canAddMethod() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("Scene");
    org.lgna.project.ast.UserMethod method = new org.lgna.project.ast.UserMethod();
    method.name.setValue("doSomething");
    method.managementLevel.setValue(org.lgna.project.ast.ManagementLevel.NONE);
    method.returnType.setValue(org.lgna.project.ast.JavaType.VOID_TYPE);
    method.body.setValue(new org.lgna.project.ast.BlockStatement());
    type.methods.add(method);
    assertEquals(1, type.methods.size());
  }
}
