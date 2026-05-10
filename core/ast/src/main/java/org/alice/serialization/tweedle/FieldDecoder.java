package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleField;
import org.alice.tweedle.TweedleNull;
import org.alice.tweedle.TweedlePrimitiveValue;
import org.alice.tweedle.ast.BinaryNumericExpression;
import org.alice.tweedle.ast.TweedleArrayInitializer;
import org.alice.tweedle.ast.TweedleExpression;
import org.lgna.common.Resource;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserParameter;

import java.util.ArrayList;
import java.util.List;

class FieldDecoder {
  private final Decoder decoder;
  private final ExpressionDecoder expressionDecoder;

  FieldDecoder(Decoder decoder, ExpressionDecoder expressionDecoder) {
    this.decoder = decoder;
    this.expressionDecoder = expressionDecoder;
  }

  UserField decodeField(TweedleField property) {
    AbstractType<?, ?, ?> valueType = decoder.resolveType(property.getType(), "field");
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
      return decoder.primitiveLiteral(primitiveValue.getPrimitiveValue());
    }
    if (decoder.isLiteralArithmeticAllowed()
        && initializer instanceof BinaryNumericExpression<?> binaryNumeric
        && isLiteralOnlyArithmeticExpression(binaryNumeric)) {
      return decodeLiteralArithmeticFieldInitializer(property, valueType, binaryNumeric);
    }
    throw unsupportedFieldInitializer(property);
  }

  private Expression decodeLiteralArithmeticFieldInitializer(
      TweedleField property,
      AbstractType<?, ?, ?> valueType,
      BinaryNumericExpression<?> binaryNumeric) {
    Expression expression = expressionDecoder.decodeBinaryNumericExpression(
        property.getName(),
        binaryNumeric,
        new UserParameter[0],
        List.of(),
        List.of());
    if (!valueType.isAssignableFrom(expression.getType())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle field initializer type is not assignable to "
              + valueType.getName() + ": " + property.getName());
    }
    return expression;
  }

  private boolean isLiteralOnlyArithmeticExpression(TweedleExpression expression) {
    if (expression instanceof TweedlePrimitiveValue<?> primitiveValue) {
      Object value = primitiveValue.getPrimitiveValue();
      return value instanceof Integer || value instanceof Double;
    }
    if (expression instanceof BinaryNumericExpression<?> binaryNumeric) {
      return isLiteralOnlyArithmeticExpression(binaryNumeric.getLhs())
          && isLiteralOnlyArithmeticExpression(binaryNumeric.getRhs());
    }
    return false;
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
    Expression expression = decoder.primitiveLiteral(primitiveValue.getPrimitiveValue());
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
    return Decoder.TWEEDLE_TYPE_ALIASES.containsKey(property.getType().getName())
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
        "Tweedle resource field initializer with a non-null value is not yet supported by the AST decoder "
            + "because no archive resource manifest or binding context is available. "
            + "Only null resource field initializers can be decoded without archive resource context: "
            + property.getType().getName() + " " + property.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedArrayInitializerElement(TweedleField property) {
    return new UnsupportedTweedleDecodeException(
        "Non-literal Tweedle array initializer elements are not yet supported by the AST decoder: " + property.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedArrayInitializerSize(TweedleField property) {
    return new UnsupportedTweedleDecodeException(
        "Non-literal Tweedle array initializer sizes are not yet supported by the AST decoder: " + property.getName());
  }
}
