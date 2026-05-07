package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleArrayType;
import org.alice.tweedle.TweedleClass;
import org.alice.tweedle.TweedleLinkException;
import org.alice.tweedle.TweedleField;
import org.alice.tweedle.TweedleMethod;
import org.alice.tweedle.TweedleNull;
import org.alice.tweedle.TweedlePrimitiveValue;
import org.alice.tweedle.TweedleType;
import org.alice.tweedle.TweedleVoidType;
import org.alice.tweedle.ast.TweedleArrayInitializer;
import org.alice.tweedle.ast.TweedleExpression;
import org.alice.tweedle.unlinked.TweedleUnlinkedParser;
import org.lgna.common.Resource;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserField;
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
    if (!tweedleClass.getConstructors().isEmpty()) {
      throw new UnsupportedTweedleDecodeException("Tweedle class constructors are not yet supported by the AST decoder.");
    }

    NamedUserType type = userTypeNamed(tweedleClass.getName());
    type.name.setValue(tweedleClass.getName());
    type.superType.setValue(resolveType(tweedleClass.getSuperclassName(), "superclass"));
    for (TweedleField property : tweedleClass.getProperties()) {
      type.fields.add(decodeField(property));
    }
    for (TweedleMethod method : tweedleClass.getMethods()) {
      type.methods.add(decodeMethod(method));
    }
    return type;
  }

  private UserMethod decodeMethod(TweedleMethod method) {
    if (!method.getRequiredParameters().isEmpty() || !method.getOptionalParameters().isEmpty()) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method parameters are not yet supported by the AST decoder: " + method.getName());
    }
    if (!method.getBody().isEmpty()) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method bodies are not yet supported by the AST decoder: " + method.getName());
    }
    if (method.getType() != TweedleVoidType.VOID) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method return values are not yet supported by the AST decoder: " + method.getName());
    }
    return new UserMethod(
        method.getName(),
        resolveReturnType(method.getType()),
        new UserParameter[] {},
        new BlockStatement());
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
