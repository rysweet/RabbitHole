package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleClass;
import org.alice.tweedle.TweedleLinkException;
import org.alice.tweedle.TweedleField;
import org.alice.tweedle.TweedleNull;
import org.alice.tweedle.TweedlePrimitiveValue;
import org.alice.tweedle.TweedleType;
import org.alice.tweedle.ast.TweedleExpression;
import org.alice.tweedle.unlinked.TweedleUnlinkedParser;
import org.lgna.common.Resource;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserField;

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
    if (!tweedleClass.getMethods().isEmpty() || !tweedleClass.getConstructors().isEmpty()) {
      throw new UnsupportedTweedleDecodeException("Tweedle class methods and constructors are not yet supported by the AST decoder.");
    }

    NamedUserType type = userTypeNamed(tweedleClass.getName());
    type.name.setValue(tweedleClass.getName());
    type.superType.setValue(resolveType(tweedleClass.getSuperclassName(), "superclass"));
    for (TweedleField property : tweedleClass.getProperties()) {
      type.fields.add(decodeField(property));
    }
    return type;
  }

  private UserField decodeField(TweedleField property) {
    AbstractType<?, ?, ?> valueType = resolveType(property.getType().getName(), "field");
    Expression initializer = property.hasInitializer() ? decodeFieldInitializer(property, valueType) : null;
    return new UserField(property.getName(), valueType, initializer);
  }

  private Expression decodeFieldInitializer(TweedleField property, AbstractType<?, ?, ?> valueType) {
    TweedleExpression initializer = property.getInitializer();
    if (initializer instanceof TweedleNull) {
      return decodeNullFieldInitializer(property, valueType);
    }
    if (valueType != null && valueType.isAssignableTo(Resource.class)) {
      throw unsupportedResourceFieldInitializer(property);
    }
    if (initializer instanceof TweedlePrimitiveValue<?> primitiveValue) {
      return primitiveLiteral(primitiveValue.getPrimitiveValue());
    }
    throw unsupportedFieldInitializer(property);
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
    return "TextString".equals(property.getType().getName())
        || valueType != null && valueType.isAssignableTo(Resource.class);
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
    throw new UnsupportedTweedleDecodeException("Unsupported Tweedle " + usage + ": " + typeName);
  }

  private NamedUserType userTypeNamed(String name) {
    AbstractType<?, ?, ?> terminalType = terminalTypesByName.get(name);
    if (terminalType instanceof NamedUserType namedUserType) {
      return namedUserType;
    }
    return new NamedUserType();
  }
}
