package org.alice.ide.ast.export;

import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.*;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link ProjectInfo}.
 */
public class ProjectInfoExtendedTest {

  private NamedUserType createProgramType() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("Program");
    return programType;
  }

  private Project createProject(NamedUserType programType) {
    return new Project(programType, Project.SceneCameraType.WindowCamera);
  }

  @Test
  public void constructor_withSimpleProject() {
    NamedUserType programType = createProgramType();
    Project project = createProject(programType);
    ProjectInfo info = new ProjectInfo(project);
    assertNotNull(info);
  }

  @Test
  public void getTypeInfos_notNull() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    Collection<TypeInfo> typeInfos = info.getTypeInfos();
    assertNotNull(typeInfos);
  }

  @Test
  public void getTypeInfos_containsProgramType() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    Collection<TypeInfo> typeInfos = info.getTypeInfos();
    assertFalse(typeInfos.isEmpty());
  }

  @Test
  public void getInfoForType_programType_notNull() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    TypeInfo typeInfo = info.getInfoForType(programType);
    assertNotNull(typeInfo);
  }

  @Test
  public void getInfoForType_unknownType_returnsNull() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    NamedUserType unknownType = new NamedUserType();
    unknownType.name.setValue("Unknown");
    TypeInfo typeInfo = info.getInfoForType(unknownType);
    assertNull(typeInfo);
  }

  @Test
  public void isInTheMidstOfChange_initiallyFalse() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    assertFalse(info.isInTheMidstOfChange());
  }

  @Test
  public void getTypeInfosAsTree_notNull() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    assertNotNull(info.getTypeInfosAsTree());
  }

  @Test
  public void update_canBeCalled() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    info.update();
    assertFalse(info.isInTheMidstOfChange());
  }

  @Test
  public void projectWithField_typeInfoContainsField() {
    NamedUserType programType = createProgramType();
    UserField field = new UserField();
    field.name.setValue("testField");
    field.valueType.setValue(JavaType.getInstance(String.class));
    programType.fields.add(field);
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    TypeInfo typeInfo = info.getInfoForType(programType);
    assertNotNull(typeInfo);
    assertFalse(typeInfo.getFieldInfos().isEmpty());
  }

  @Test
  public void projectWithMethod_typeInfoContainsMethod() {
    NamedUserType programType = createProgramType();
    UserMethod method = new UserMethod();
    method.name.setValue("doAction");
    method.managementLevel.setValue(ManagementLevel.NONE);
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.body.setValue(new BlockStatement());
    programType.methods.add(method);
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    TypeInfo typeInfo = info.getInfoForType(programType);
    assertNotNull(typeInfo);
    assertFalse(typeInfo.getMethodInfos().isEmpty());
  }

  @Test
  public void update_multipleTimesDoesNotThrow() {
    NamedUserType programType = createProgramType();
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectInfo info = new ProjectInfo(project);
    info.update();
    info.update();
    info.update();
    assertFalse(info.isInTheMidstOfChange());
  }
}
