package org.alice.ide.type;

import org.junit.Test;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;

import static org.junit.Assert.*;

/**
 * Tests for the {@link TypeKey} hierarchy — equals, hashCode, and createType.
 */
public class TypeKeyTest {

  // ---- ExtendsTypeKey ----

  @Test
  public void extendsTypeKey_equalsSelf() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    ExtendsTypeKey key = new ExtendsTypeKey(type);
    assertEquals(key, key);
  }

  @Test
  public void extendsTypeKey_equalsSameSuperType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    ExtendsTypeKey key1 = new ExtendsTypeKey(type);
    ExtendsTypeKey key2 = new ExtendsTypeKey(type);
    assertEquals(key1, key2);
    assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  public void extendsTypeKey_notEqualDifferentSuperType() {
    ExtendsTypeKey key1 = new ExtendsTypeKey(JavaType.getInstance(String.class));
    ExtendsTypeKey key2 = new ExtendsTypeKey(JavaType.getInstance(Integer.class));
    assertNotEquals(key1, key2);
  }

  @Test
  public void extendsTypeKey_notEqualNull() {
    ExtendsTypeKey key = new ExtendsTypeKey(JavaType.getInstance(String.class));
    assertNotEquals(key, null);
  }

  @Test
  public void extendsTypeKey_notEqualDifferentClass() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    ExtendsTypeKey key1 = new ExtendsTypeKey(type);
    ExtendsTypeWithNamedType key2 = new ExtendsTypeWithNamedType(type, "MyType");
    assertNotEquals(key1, key2);
  }

  @Test
  public void extendsTypeKey_getSuperType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Double.class);
    ExtendsTypeKey key = new ExtendsTypeKey(type);
    assertSame(type, key.getSuperType());
  }

  @Test
  public void extendsTypeKey_createType_returnsNonNull() {
    ExtendsTypeKey key = new ExtendsTypeKey(JavaType.getInstance(Object.class));
    assertNotNull(key.createType());
  }

  // ---- ExtendsTypeWithNamedType ----

  @Test
  public void namedTypeKey_equalsSameNameAndSuperType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Object.class);
    ExtendsTypeWithNamedType key1 = new ExtendsTypeWithNamedType(type, "Alien");
    ExtendsTypeWithNamedType key2 = new ExtendsTypeWithNamedType(type, "Alien");
    assertEquals(key1, key2);
    assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  public void namedTypeKey_notEqualDifferentName() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Object.class);
    ExtendsTypeWithNamedType key1 = new ExtendsTypeWithNamedType(type, "Alien");
    ExtendsTypeWithNamedType key2 = new ExtendsTypeWithNamedType(type, "Robot");
    assertNotEquals(key1, key2);
  }

  @Test
  public void namedTypeKey_notEqualDifferentSuperType() {
    ExtendsTypeWithNamedType key1 = new ExtendsTypeWithNamedType(JavaType.getInstance(String.class), "X");
    ExtendsTypeWithNamedType key2 = new ExtendsTypeWithNamedType(JavaType.getInstance(Integer.class), "X");
    assertNotEquals(key1, key2);
  }

  @Test
  public void namedTypeKey_notEqualExtendsTypeKey() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Object.class);
    ExtendsTypeKey key1 = new ExtendsTypeKey(type);
    ExtendsTypeWithNamedType key2 = new ExtendsTypeWithNamedType(type, "X");
    assertNotEquals(key1, key2);
    assertNotEquals(key2, key1);
  }

  // ---- ExtendsTypeWithConstructorParameterTypeKey ----

  @Test
  public void constructorParamKey_equalsSameTypeAndParam() {
    AbstractType<?, ?, ?> superType = JavaType.getInstance(Object.class);
    AbstractType<?, ?, ?> paramType = JavaType.getInstance(String.class);
    ExtendsTypeWithConstructorParameterTypeKey key1 = new ExtendsTypeWithConstructorParameterTypeKey(superType, paramType);
    ExtendsTypeWithConstructorParameterTypeKey key2 = new ExtendsTypeWithConstructorParameterTypeKey(superType, paramType);
    assertEquals(key1, key2);
    assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  public void constructorParamKey_notEqualDifferentParam() {
    AbstractType<?, ?, ?> superType = JavaType.getInstance(Object.class);
    ExtendsTypeWithConstructorParameterTypeKey key1 = new ExtendsTypeWithConstructorParameterTypeKey(superType, JavaType.getInstance(String.class));
    ExtendsTypeWithConstructorParameterTypeKey key2 = new ExtendsTypeWithConstructorParameterTypeKey(superType, JavaType.getInstance(Integer.class));
    assertNotEquals(key1, key2);
  }

  @Test
  public void constructorParamKey_notEqualDifferentSuperType() {
    AbstractType<?, ?, ?> paramType = JavaType.getInstance(String.class);
    ExtendsTypeWithConstructorParameterTypeKey key1 = new ExtendsTypeWithConstructorParameterTypeKey(JavaType.getInstance(Object.class), paramType);
    ExtendsTypeWithConstructorParameterTypeKey key2 = new ExtendsTypeWithConstructorParameterTypeKey(JavaType.getInstance(Number.class), paramType);
    assertNotEquals(key1, key2);
  }

  // ---- ExtendsTypeWithSuperArgumentFieldKey ----

  @Test
  public void fieldKey_equalsSameFieldAndSuperType() {
    AbstractType<?, ?, ?> superType = JavaType.getInstance(Object.class);
    org.lgna.project.ast.UserField field = new org.lgna.project.ast.UserField();
    field.name.setValue("resource");
    ExtendsTypeWithSuperArgumentFieldKey key1 = new ExtendsTypeWithSuperArgumentFieldKey(superType, field);
    ExtendsTypeWithSuperArgumentFieldKey key2 = new ExtendsTypeWithSuperArgumentFieldKey(superType, field);
    assertEquals(key1, key2);
    assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  public void fieldKey_notEqualDifferentField() {
    AbstractType<?, ?, ?> superType = JavaType.getInstance(Object.class);
    org.lgna.project.ast.UserField field1 = new org.lgna.project.ast.UserField();
    field1.name.setValue("resource1");
    org.lgna.project.ast.UserField field2 = new org.lgna.project.ast.UserField();
    field2.name.setValue("resource2");
    ExtendsTypeWithSuperArgumentFieldKey key1 = new ExtendsTypeWithSuperArgumentFieldKey(superType, field1);
    ExtendsTypeWithSuperArgumentFieldKey key2 = new ExtendsTypeWithSuperArgumentFieldKey(superType, field2);
    assertNotEquals(key1, key2);
  }

  // ---- cross-type inequality ----

  @Test
  public void differentKeyTypes_neverEqual() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Object.class);
    AbstractType<?, ?, ?> param = JavaType.getInstance(String.class);
    org.lgna.project.ast.UserField field = new org.lgna.project.ast.UserField();
    field.name.setValue("f");

    TypeKey extendsKey = new ExtendsTypeKey(type);
    TypeKey namedKey = new ExtendsTypeWithNamedType(type, "N");
    TypeKey paramKey = new ExtendsTypeWithConstructorParameterTypeKey(type, param);
    TypeKey fieldKey = new ExtendsTypeWithSuperArgumentFieldKey(type, field);

    assertNotEquals(extendsKey, namedKey);
    assertNotEquals(extendsKey, paramKey);
    assertNotEquals(extendsKey, fieldKey);
    assertNotEquals(namedKey, paramKey);
    assertNotEquals(namedKey, fieldKey);
    assertNotEquals(paramKey, fieldKey);
  }

  // ---- hashCode consistency ----

  @Test
  public void hashCode_consistentAcrossMultipleCalls() {
    ExtendsTypeKey key = new ExtendsTypeKey(JavaType.getInstance(String.class));
    int h1 = key.hashCode();
    int h2 = key.hashCode();
    assertEquals(h1, h2);
  }
}
