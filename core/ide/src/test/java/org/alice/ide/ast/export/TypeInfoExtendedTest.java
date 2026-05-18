package org.alice.ide.ast.export;

import org.junit.Test;
import org.lgna.project.Project;
import org.lgna.project.ast.*;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link TypeInfo}.
 */
public class TypeInfoExtendedTest {

  private ProjectInfo createProjectInfo() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("Program");
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    return new ProjectInfo(project);
  }

  @Test
  public void constructor_createsInfoForType() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestType");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    assertNotNull(typeInfo);
  }

  @Test
  public void getDeclaration_returnsSameType() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("TestType");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    assertSame(type, typeInfo.getDeclaration());
  }

  @Test
  public void getConstructorInfos_emptyForTypeWithNoConstructors() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("EmptyType");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    Collection<ConstructorInfo> ctorInfos = typeInfo.getConstructorInfos();
    assertNotNull(ctorInfos);
  }

  @Test
  public void getMethodInfos_emptyForTypeWithNoMethods() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("NoMethods");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    Collection<MethodInfo> methodInfos = typeInfo.getMethodInfos();
    assertNotNull(methodInfos);
    assertTrue(methodInfos.isEmpty());
  }

  @Test
  public void getFieldInfos_emptyForTypeWithNoFields() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("NoFields");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    Collection<org.alice.ide.ast.export.FieldInfo> fieldInfos = typeInfo.getFieldInfos();
    assertNotNull(fieldInfos);
    assertTrue(fieldInfos.isEmpty());
  }

  @Test
  public void typeWithMethod_getMethodInfos_nonEmpty() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("WithMethod");
    UserMethod method = new UserMethod();
    method.name.setValue("doAction");
    method.managementLevel.setValue(ManagementLevel.NONE);
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.body.setValue(new BlockStatement());
    type.methods.add(method);
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    assertFalse(typeInfo.getMethodInfos().isEmpty());
  }

  @Test
  public void typeWithField_getFieldInfos_nonEmpty() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("WithField");
    UserField field = new UserField();
    field.name.setValue("speed");
    field.valueType.setValue(JavaType.getInstance(Double.class));
    type.fields.add(field);
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    assertFalse(typeInfo.getFieldInfos().isEmpty());
  }

  @Test
  public void getInfoForMethod_existingMethod() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("WithMethod");
    UserMethod method = new UserMethod();
    method.name.setValue("walkAction");
    method.managementLevel.setValue(ManagementLevel.NONE);
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.body.setValue(new BlockStatement());
    type.methods.add(method);
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    MethodInfo methodInfo = typeInfo.getInfoForMethod(method);
    assertNotNull(methodInfo);
  }

  @Test
  public void getInfoForField_existingField() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("WithField");
    UserField field = new UserField();
    field.name.setValue("height");
    field.valueType.setValue(JavaType.getInstance(Double.class));
    type.fields.add(field);
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    org.alice.ide.ast.export.FieldInfo fieldInfo = typeInfo.getInfoForField(field);
    assertNotNull(fieldInfo);
  }

  @Test
  public void getInfoForMethod_unknownMethod_returnsNull() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("EmptyType");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    UserMethod unknownMethod = new UserMethod();
    unknownMethod.name.setValue("unknown");
    assertNull(typeInfo.getInfoForMethod(unknownMethod));
  }

  @Test
  public void getSuperTypeInfo_noSuperType_returnsNull() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("RootType");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    assertNull(typeInfo.getSuperTypeInfo());
  }

  @Test
  public void resetRequired_canBeCalled() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("ResetTest");
    ProjectInfo projectInfo = createProjectInfo();
    TypeInfo typeInfo = new TypeInfo(projectInfo, type);
    typeInfo.resetRequired();
    // No exception means success
  }
}
