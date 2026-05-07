package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleArrayType;
import org.alice.tweedle.TweedleClass;
import org.alice.tweedle.TweedleConstructor;
import org.alice.tweedle.TweedleLinkException;
import org.alice.tweedle.TweedleField;
import org.alice.tweedle.TweedleMethod;
import org.alice.tweedle.TweedleNull;
import org.alice.tweedle.TweedlePrimitiveValue;
import org.alice.tweedle.TweedleRequiredParameter;
import org.alice.tweedle.TweedleStatement;
import org.alice.tweedle.TweedleType;
import org.alice.tweedle.TweedleVoidType;
import org.alice.tweedle.ast.IdentifierReference;
import org.alice.tweedle.ast.LocalVariableDeclaration;
import org.alice.tweedle.ast.TweedleArrayInitializer;
import org.alice.tweedle.ast.TweedleExpression;
import org.alice.tweedle.ast.TweedleLocalVariable;
import org.alice.tweedle.ast.ThisExpression;
import org.alice.tweedle.unlinked.TweedleUnlinkedParser;
import org.lgna.common.Resource;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalAccess;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Decoder {
  private final Map<String, AbstractType<?, ?, ?>> terminalTypesByName;
  private static final List<String> JAVA_TYPE_PACKAGES = List.of(
      "org.lgna.story.",
      "org.lgna.story.resources.",
      "org.lgna.common.resources.",
      "java.lang.");
  private static final Map<String, Class<?>> TWEEDLE_TYPE_ALIASES = Map.of(
      "WholeNumber", Integer.class,
      "DecimalNumber", Double.class,
      "TextString", String.class,
      "Boolean", Boolean.class,
      "Number", Number.class);

  Decoder(Set<AbstractDeclaration> terminals) {
    terminalTypesByName = terminals.stream()
        .filter(AbstractType.class::isInstance)
        .map(AbstractType.class::cast)
        .filter(type -> type.getName() != null)
        .collect(Collectors.toMap(AbstractType::getName, type -> type, (existing, replacement) -> existing));
  }

  Decoder() {
    this(new HashSet<>());
  }

  public AbstractNode decode(String document) {
    TweedleType tweedleType;
    try {
      tweedleType = new TweedleUnlinkedParser().parseType(document);
    } catch (TweedleLinkException e) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle type uses linked members that the AST decoder does not support.",
          e);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Unable to parse Tweedle type.", e);
    }
    if (tweedleType instanceof TweedleClass tweedleClass) {
      return decodeClass(tweedleClass);
    }
    throw new UnsupportedTweedleDecodeException("Only Tweedle class declarations can be decoded to AST nodes.");
  }

  public AbstractNode copy(String document) {
    return decode(document);
  }

  private NamedUserType decodeClass(TweedleClass tweedleClass) {
    NamedUserType type = userTypeNamed(tweedleClass.getName());
    type.name.setValue(tweedleClass.getName());
    type.superType.setValue(resolveType(tweedleClass.getSuperclassName(), "superclass"));
    for (TweedleField property : tweedleClass.getProperties()) {
      type.fields.add(decodeField(property));
    }
    for (TweedleMethod method : tweedleClass.getMethods()) {
      type.methods.add(decodeMethod(method, type.getDeclaredFields()));
    }
    for (TweedleConstructor constructor : tweedleClass.getConstructors()) {
      type.constructors.add(decodeConstructor(tweedleClass, constructor));
    }
    return type;
  }

  private NamedUserConstructor decodeConstructor(TweedleClass declaringClass, TweedleConstructor constructor) {
    if (!constructor.getName().equals(declaringClass.getName())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle constructor name does not match declaring class: " + constructor.getName());
    }
    if (!constructor.getOptionalParameters().isEmpty()) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle optional constructor parameters are not yet supported by the AST decoder: " + constructor.getName());
    }
    return new NamedUserConstructor(
        decodeRequiredParameters(constructor.getRequiredParameters(), "constructor parameter"),
        decodeConstructorBody(constructor));
  }

  private ConstructorBlockStatement decodeConstructorBody(TweedleConstructor constructor) {
    List<Statement> statements = new ArrayList<>();
    for (TweedleStatement statement : constructor.getBody()) {
      if (statement instanceof LocalVariableDeclaration localVariableDeclaration) {
        statements.add(decodeLocalDeclarationStatement(constructor.getName(), localVariableDeclaration));
      } else {
        throw unsupportedConstructorBody(constructor);
      }
    }
    if (statements.isEmpty()) {
      return new ConstructorBlockStatement();
    }
    return new ConstructorBlockStatement(
        new SuperConstructorInvocationStatement(),
        statements.toArray(Statement[]::new));
  }

  private UserParameter[] decodeRequiredParameters(List<TweedleRequiredParameter> parameters, String usage) {
    return parameters.stream()
        .map(parameter -> new UserParameter(parameter.getName(), resolveType(parameter.getType(), usage)))
        .toArray(UserParameter[]::new);
  }

  private UserMethod decodeMethod(TweedleMethod method, List<UserField> fields) {
    if (!method.getOptionalParameters().isEmpty()) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle optional method parameters are not yet supported by the AST decoder: " + method.getName());
    }
    AbstractType<?, ?, ?> returnType = resolveReturnType(method.getType());
    UserParameter[] requiredParameters = decodeRequiredParameters(method.getRequiredParameters(), "method parameter");
    return new UserMethod(
        method.getName(),
        returnType,
        requiredParameters,
        decodeMethodBody(method, returnType, requiredParameters, fields));
  }

  private BlockStatement decodeMethodBody(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] requiredParameters,
      List<UserField> fields) {
    if (method.getBody().isEmpty()) {
      if (returnType != JavaType.VOID_TYPE) {
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return values require a supported return statement: " + method.getName());
      }
      return new BlockStatement();
    }
    List<Statement> statements = new ArrayList<>();
    List<UserLocal> locals = new ArrayList<>();
    for (int i = 0; i < method.getBody().size(); i++) {
      TweedleStatement statement = method.getBody().get(i);
      if (statement instanceof LocalVariableDeclaration localVariableDeclaration) {
        LocalDeclarationStatement localStatement =
            decodeLocalDeclarationStatement(method.getName(), localVariableDeclaration);
        statements.add(localStatement);
        locals.add(localStatement.local.getValue());
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        statements.add(decodeMethodFieldAssignment(method, assignment, fields));
      } else if (statement instanceof org.alice.tweedle.ast.ReturnStatement returnStatement
          && i == method.getBody().size() - 1) {
        statements.add(decodeReturnStatement(method, returnType, requiredParameters, locals, fields, returnStatement));
      } else {
        throw unsupportedMethodBody(method);
      }
    }
    if (returnType != JavaType.VOID_TYPE
        && !(method.getBody().get(method.getBody().size() - 1) instanceof org.alice.tweedle.ast.ReturnStatement)) {
      throw unsupportedMethodBody(method);
    }
    return new BlockStatement(statements.toArray(Statement[]::new));
  }

  private LocalDeclarationStatement decodeLocalDeclarationStatement(
      String ownerName,
      LocalVariableDeclaration localVariableDeclaration) {
    TweedleLocalVariable tweedleLocal = localVariableDeclaration.getDeclaration();
    AbstractType<?, ?, ?> localType = resolveType(tweedleLocal.getType(), "local variable");
    TweedleExpression initializer = tweedleLocal.getInitializer();
    if (!(initializer instanceof TweedlePrimitiveValue<?> primitiveValue)) {
      throw unsupportedLocalInitializer(ownerName, tweedleLocal);
    }
    Expression astInitializer = primitiveLiteral(primitiveValue.getPrimitiveValue());
    if (!localType.isAssignableFrom(astInitializer.getType())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle local variable initializer type is not assignable to "
              + localType.getName() + ": " + ownerName + "." + tweedleLocal.getName());
    }
    return new LocalDeclarationStatement(
        new UserLocal(tweedleLocal.getName(), localType, localVariableDeclaration.isConstant()),
        astInitializer);
  }

  private Statement decodeMethodFieldAssignment(
      TweedleMethod method,
      org.alice.tweedle.ast.AssignmentExpression assignment,
      List<UserField> fields) {
    TweedleExpression value = assignment.getValueExp();
    if (!(value instanceof TweedlePrimitiveValue<?> primitiveValue)) {
      throw new UnsupportedTweedleDecodeException(
          "Non-literal Tweedle field assignment values are not yet supported by the AST decoder: " + method.getName());
    }
    Expression rhs = primitiveLiteral(primitiveValue.getPrimitiveValue());
    TweedleExpression assignee = assignment.getAssigneeExp();
    if (assignee instanceof IdentifierReference identifierReference) {
      UserField field = findField(fields, identifierReference.getName());
      if (field != null) {
        if (!field.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle field assignment value type is not assignable to "
                  + field.getValueType().getName() + ": " + method.getName() + "." + identifierReference.getName());
        }
        return AstUtilities.createFieldAssignmentStatement(field, rhs);
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle field assignment target is not a known field: " + method.getName() + "." + identifierReference.getName());
    }
    if (assignee instanceof org.alice.tweedle.ast.FieldAccess fieldAccess) {
      if (!(fieldAccess.getTarget() instanceof ThisExpression)) {
        throw new UnsupportedTweedleDecodeException(
            "Only this.field Tweedle assignment targets are supported by the AST decoder: "
                + method.getName() + "." + describeMemberAccess(fieldAccess));
      }
      UserField field = findField(fields, fieldAccess.getFieldName());
      if (field == null) {
        throw new UnsupportedTweedleDecodeException(
            "Tweedle this.field assignment target is not a known field: " + method.getName() + "." + fieldAccess.getFieldName());
      }
      if (!field.getValueType().isAssignableFrom(rhs.getType())) {
        throw new UnsupportedTweedleDecodeException(
            "Tweedle field assignment value type is not assignable to "
                + field.getValueType().getName() + ": " + method.getName() + ".this." + fieldAccess.getFieldName());
      }
      return AstUtilities.createFieldAssignmentStatement(field, rhs);
    }
    throw new UnsupportedTweedleDecodeException(
        "Unsupported Tweedle assignment target in method: " + method.getName());
  }

  private org.lgna.project.ast.ReturnStatement decodeReturnStatement(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] requiredParameters,
      List<UserLocal> locals,
      List<UserField> fields,
      org.alice.tweedle.ast.ReturnStatement returnStatement) {
    Expression expression =
        decodeMethodReturnExpression(method, returnType, requiredParameters, locals, fields, returnStatement.getExpression());
    return new org.lgna.project.ast.ReturnStatement(returnType, expression);
  }

  private Expression decodeMethodReturnExpression(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] requiredParameters,
      List<UserLocal> locals,
      List<UserField> fields,
      TweedleExpression returnExpression) {
    if (returnExpression instanceof TweedlePrimitiveValue<?> primitiveValue) {
      Expression expression = primitiveLiteral(primitiveValue.getPrimitiveValue());
      if (returnType.isAssignableFrom(expression.getType())) {
        return expression;
      }
      throw new UnsupportedTweedleDecodeException(
            "Tweedle method return expression type is not assignable to "
                + returnType.getName() + ": " + method.getName());
    }
    if (returnExpression instanceof IdentifierReference identifierReference) {
      UserLocal local = findLocal(locals, identifierReference.getName());
      if (local != null) {
        LocalAccess access = new LocalAccess(local);
        if (returnType.isAssignableFrom(access.getType())) {
          return access;
        }
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return identifier type is not assignable to "
                + returnType.getName() + ": " + method.getName() + "." + identifierReference.getName());
      }
      UserParameter parameter = findParameter(requiredParameters, identifierReference.getName());
      if (parameter != null) {
        ParameterAccess access = new ParameterAccess(parameter);
        if (returnType.isAssignableFrom(access.getType())) {
          return access;
        }
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return identifier type is not assignable to "
                + returnType.getName() + ": " + method.getName() + "." + identifierReference.getName());
      }
      UserField field = findField(fields, identifierReference.getName());
      if (field != null) {
        FieldAccess access = new FieldAccess(field);
        if (returnType.isAssignableFrom(access.getType())) {
          return access;
        }
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return identifier type is not assignable to "
                + returnType.getName() + ": " + method.getName() + "." + identifierReference.getName());
      }
      throw unsupportedMethodReturnIdentifier(method, identifierReference);
    }
    if (returnExpression instanceof org.alice.tweedle.ast.FieldAccess fieldAccess) {
      return decodeMethodReturnFieldAccess(method, returnType, fields, fieldAccess);
    }
    throw unsupportedMethodReturnExpression(method);
  }

  private Expression decodeMethodReturnFieldAccess(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      List<UserField> fields,
      org.alice.tweedle.ast.FieldAccess fieldAccess) {
    if (!(fieldAccess.getTarget() instanceof ThisExpression)) {
      throw unsupportedMethodReturnMemberExpression(method, fieldAccess);
    }
    UserField field = findField(fields, fieldAccess.getFieldName());
    if (field == null) {
      throw unsupportedMethodReturnMemberExpression(method, fieldAccess);
    }
    FieldAccess access = new FieldAccess(field);
    if (returnType.isAssignableFrom(access.getType())) {
      return access;
    }
    throw new UnsupportedTweedleDecodeException(
        "Tweedle method return member expression type is not assignable to "
            + returnType.getName() + ": " + method.getName() + ".this." + fieldAccess.getFieldName());
  }

  private UserLocal findLocal(List<UserLocal> locals, String name) {
    for (int i = locals.size() - 1; i >= 0; i--) {
      UserLocal local = locals.get(i);
      if (local.getName().equals(name)) {
        return local;
      }
    }
    return null;
  }

  private UserParameter findParameter(UserParameter[] parameters, String name) {
    for (UserParameter parameter : parameters) {
      if (parameter.getName().equals(name)) {
        return parameter;
      }
    }
    return null;
  }

  private UserField findField(List<UserField> fields, String name) {
    for (UserField field : fields) {
      if (field.getName().equals(name)) {
        return field;
      }
    }
    return null;
  }

  private AbstractType<?, ?, ?> resolveReturnType(TweedleType tweedleType) {
    if (tweedleType == TweedleVoidType.VOID) {
      return JavaType.VOID_TYPE;
    }
    return resolveType(tweedleType, "method return");
  }

  private UserField decodeField(TweedleField property) {
    AbstractType<?, ?, ?> valueType = resolveType(property.getType(), "field");
    Expression initializer = property.hasInitializer() ? decodeFieldInitializer(property, valueType) : null;
    return new UserField(property.getName(), valueType, initializer);
  }

  private Expression decodeFieldInitializer(TweedleField property, AbstractType<?, ?, ?> valueType) {
    TweedleExpression initializer = property.getInitializer();
    if (initializer instanceof TweedleNull) {
      return decodeNullFieldInitializer(property, valueType);
    }
    if (initializer instanceof TweedleArrayInitializer arrayInitializer) {
      return decodeArrayFieldInitializer(property, valueType, arrayInitializer);
    }
    if (isResourceType(valueType)) {
      throw unsupportedResourceFieldInitializer(property);
    }
    if (initializer instanceof TweedlePrimitiveValue<?> primitiveValue) {
      return primitiveLiteral(primitiveValue.getPrimitiveValue());
    }
    throw unsupportedFieldInitializer(property);
  }

  private Expression decodeArrayFieldInitializer(
      TweedleField property,
      AbstractType<?, ?, ?> valueType,
      TweedleArrayInitializer arrayInitializer) {
    if (!valueType.isArray()) {
      throw unsupportedFieldInitializer(property);
    }
    if (!arrayInitializer.hasElementInitializers()) {
      return decodeSizedArrayFieldInitializer(property, valueType, arrayInitializer);
    }

    AbstractType<?, ?, ?> componentType = valueType.getComponentType();
    List<Expression> elements = new ArrayList<>();
    for (TweedleExpression element : arrayInitializer.getElements()) {
      Expression elementExpression = decodeArrayInitializerElement(property, componentType, element);
      elements.add(elementExpression);
    }
    return AstUtilities.createArrayInstanceCreation(valueType, elements);
  }

  private Expression decodeSizedArrayFieldInitializer(
      TweedleField property,
      AbstractType<?, ?, ?> valueType,
      TweedleArrayInitializer arrayInitializer) {
    TweedleExpression size = arrayInitializer.getInitializeSize();
    if (!(size instanceof TweedlePrimitiveValue<?> primitiveValue)
        || !(primitiveValue.getPrimitiveValue() instanceof Integer length)
        || length < 0) {
      throw unsupportedArrayInitializerSize(property);
    }
    return new ArrayInstanceCreation(valueType, new Integer[] {length});
  }

  private Expression decodeArrayInitializerElement(
      TweedleField property,
      AbstractType<?, ?, ?> componentType,
      TweedleExpression element) {
    if (!(element instanceof TweedlePrimitiveValue<?> primitiveValue)) {
      throw unsupportedArrayInitializerElement(property);
    }
    Expression expression = primitiveLiteral(primitiveValue.getPrimitiveValue());
    if (!componentType.isAssignableFrom(expression.getType())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle array initializer element type is not assignable to "
              + componentType.getName() + ": " + property.getName());
    }
    return expression;
  }

  private Expression decodeNullFieldInitializer(TweedleField property, AbstractType<?, ?, ?> valueType) {
    if (isSupportedNullableField(property, valueType)) {
      return new NullLiteral();
    }
    throw new UnsupportedTweedleDecodeException(
        "Null initializer is not yet supported for Tweedle field type "
            + property.getType().getName() + ": " + property.getName());
  }

  private boolean isSupportedNullableField(TweedleField property, AbstractType<?, ?, ?> valueType) {
    return TWEEDLE_TYPE_ALIASES.containsKey(property.getType().getName())
        || valueType instanceof NamedUserType
        || valueType.isArray()
        || isResourceType(valueType);
  }

  private boolean isResourceType(AbstractType<?, ?, ?> valueType) {
    JavaType javaType = firstJavaTypeInHierarchy(valueType);
    return javaType != null && javaType.isAssignableTo(Resource.class);
  }

  private JavaType firstJavaTypeInHierarchy(AbstractType<?, ?, ?> valueType) {
    AbstractType<?, ?, ?> type = valueType;
    while (type != null && !(type instanceof JavaType)) {
      type = type.getSuperType();
    }
    return (JavaType) type;
  }

  private Expression primitiveLiteral(Object value) {
    if (value instanceof Integer integerValue) {
      return new IntegerLiteral(integerValue);
    }
    if (value instanceof Double doubleValue) {
      return new DoubleLiteral(doubleValue);
    }
    if (value instanceof String stringValue) {
      return new StringLiteral(stringValue);
    }
    if (value instanceof Boolean booleanValue) {
      return new BooleanLiteral(booleanValue);
    }
    throw new UnsupportedTweedleDecodeException("Unsupported Tweedle primitive initializer value: " + value);
  }

  private UnsupportedTweedleDecodeException unsupportedMethodBody(TweedleMethod method) {
    return new UnsupportedTweedleDecodeException(
        "Tweedle method bodies are not yet supported by the AST decoder: " + method.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedConstructorBody(TweedleConstructor constructor) {
    return new UnsupportedTweedleDecodeException(
        "Tweedle constructor bodies are not yet supported by the AST decoder: " + constructor.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedMethodReturnExpression(TweedleMethod method) {
    return new UnsupportedTweedleDecodeException(
        "Non-literal Tweedle method return expressions are not yet supported by the AST decoder: " + method.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedMethodReturnIdentifier(
      TweedleMethod method,
      IdentifierReference identifierReference) {
    return new UnsupportedTweedleDecodeException(
        "Only required-parameter, local-variable, or field Tweedle method return identifiers are supported by the AST decoder: "
            + method.getName() + "." + identifierReference.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedMethodReturnMemberExpression(
      TweedleMethod method,
      org.alice.tweedle.ast.FieldAccess fieldAccess) {
    return new UnsupportedTweedleDecodeException(
        "Only this.field Tweedle method return member expressions are supported by the AST decoder: "
            + method.getName() + "." + describeMemberAccess(fieldAccess));
  }

  private String describeMemberAccess(org.alice.tweedle.ast.FieldAccess fieldAccess) {
    TweedleExpression target = fieldAccess.getTarget();
    if (target instanceof ThisExpression) {
      return "this." + fieldAccess.getFieldName();
    }
    if (target instanceof IdentifierReference identifierReference) {
      return identifierReference.getName() + "." + fieldAccess.getFieldName();
    }
    return "<unsupported>." + fieldAccess.getFieldName();
  }

  private UnsupportedTweedleDecodeException unsupportedLocalInitializer(
      String ownerName,
      TweedleLocalVariable local) {
    return new UnsupportedTweedleDecodeException(
        "Non-literal Tweedle local variable initializers are not yet supported by the AST decoder: "
            + ownerName + "." + local.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedFieldInitializer(TweedleField property) {
    if (property.hasInitializer()) {
      return new UnsupportedTweedleDecodeException(
          "Non-literal Tweedle field initializers are not yet supported by the AST decoder: " + property.getName());
    }
    return new UnsupportedTweedleDecodeException(
        "Missing Tweedle field initializer: " + property.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedResourceFieldInitializer(TweedleField property) {
    return new UnsupportedTweedleDecodeException(
        "Tweedle resource field initializers are not yet supported by the AST decoder: " + property.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedArrayInitializerElement(TweedleField property) {
    return new UnsupportedTweedleDecodeException(
        "Non-literal Tweedle array initializer elements are not yet supported by the AST decoder: " + property.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedArrayInitializerSize(TweedleField property) {
    return new UnsupportedTweedleDecodeException(
        "Non-literal Tweedle array initializer sizes are not yet supported by the AST decoder: " + property.getName());
  }

  private AbstractType<?, ?, ?> resolveType(TweedleType tweedleType, String usage) {
    if (tweedleType instanceof TweedleArrayType arrayType) {
      TweedleType componentType = arrayType.getValueType();
      if (componentType == null) {
        throw unsupportedType(tweedleType.getName(), usage);
      }
      AbstractType<?, ?, ?> componentAstType = resolveType(componentType, usage);
      if (componentAstType == null) {
        throw unsupportedType(tweedleType.getName(), usage);
      }
      return componentAstType.getArrayType();
    }
    return resolveType(tweedleType == null ? null : tweedleType.getName(), usage);
  }

  private AbstractType<?, ?, ?> resolveType(String typeName, String usage) {
    if (typeName == null) {
      return null;
    }
    Class<?> aliasedClass = TWEEDLE_TYPE_ALIASES.get(typeName);
    if (aliasedClass != null) {
      return JavaType.getInstance(aliasedClass);
    }
    AbstractType<?, ?, ?> terminalType = terminalTypesByName.get(typeName);
    if (terminalType != null) {
      return terminalType;
    }
    for (String packageName : JAVA_TYPE_PACKAGES) {
      try {
        return JavaType.getInstance(Class.forName(packageName + typeName));
      } catch (ClassNotFoundException ignored) {
      }
    }
    throw unsupportedType(typeName, usage);
  }

  private UnsupportedTweedleDecodeException unsupportedType(String typeName, String usage) {
    return new UnsupportedTweedleDecodeException("Unsupported Tweedle " + usage + ": " + typeName);
  }

  private NamedUserType userTypeNamed(String name) {
    AbstractType<?, ?, ?> terminalType = terminalTypesByName.get(name);
    if (terminalType instanceof NamedUserType namedUserType) {
      return namedUserType;
    }
    return new NamedUserType();
  }
}
