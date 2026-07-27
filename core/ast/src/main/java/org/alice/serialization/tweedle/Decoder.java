package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleArrayType;
import org.alice.tweedle.TweedleClass;
import org.alice.tweedle.TweedleConstructor;
import org.alice.tweedle.TweedleLinkException;
import org.alice.tweedle.TweedleField;
import org.alice.tweedle.TweedleMethod;
import org.alice.tweedle.TweedleOptionalParameter;
import org.alice.tweedle.TweedleRequiredParameter;
import org.alice.tweedle.TweedleType;
import org.alice.tweedle.TweedleVoidType;
import org.alice.tweedle.unlinked.TweedleUnlinkedParser;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thin coordinator that parses Tweedle source into AST nodes.
 * Delegates expression, statement, and field decoding to
 * {@link ExpressionDecoder}, {@link StatementDecoder}, and {@link FieldDecoder}.
 */
public class Decoder {
  private final Map<String, AbstractType<?, ?, ?>> terminalTypesByName;
  private final boolean allowLiteralArithmeticFieldInitializers;
  private static final List<String> JAVA_TYPE_PACKAGES = List.of(
      "org.lgna.story.",
      "org.lgna.story.resources.",
      // Gallery model resource enums (e.g. TerrainResource, WaterTankResource) live in
      // per-category subpackages of org.lgna.story.resources; the Tweedle encoder emits them by
      // simple name, so the decoder must search these subpackages to resolve resource-typed
      // method parameters and fields.
      "org.lgna.story.resources.aircraft.",
      "org.lgna.story.resources.biped.",
      "org.lgna.story.resources.fish.",
      "org.lgna.story.resources.flyer.",
      "org.lgna.story.resources.marinemammal.",
      "org.lgna.story.resources.prop.",
      "org.lgna.story.resources.quadruped.",
      "org.lgna.story.resources.slitherer.",
      "org.lgna.story.resources.train.",
      "org.lgna.story.resources.watercraft.",
      "org.lgna.common.resources.",
      "java.lang.");
  static final Map<String, Class<?>> TWEEDLE_TYPE_ALIASES = Map.of(
      "WholeNumber", Integer.class,
      "DecimalNumber", Double.class,
      "TextString", String.class,
      "Boolean", Boolean.class,
      "Number", Number.class);

  private final ExpressionDecoder expressionDecoder;
  private final StatementDecoder statementDecoder;
  private final FieldDecoder fieldDecoder;
  private final MethodCallResolver methodCallResolver;

  Decoder(Set<AbstractDeclaration> terminals) {
    this(terminals, true);
  }

  Decoder(Set<AbstractDeclaration> terminals, boolean allowLiteralArithmeticFieldInitializers) {
    this.allowLiteralArithmeticFieldInitializers = allowLiteralArithmeticFieldInitializers;
    terminalTypesByName = terminals.stream()
        .filter(AbstractType.class::isInstance)
        .map(AbstractType.class::cast)
        .filter(type -> type.getName() != null)
        .collect(Collectors.toMap(AbstractType::getName, type -> type, (existing, replacement) -> existing));
    this.methodCallResolver = new MethodCallResolver();
    this.expressionDecoder = new ExpressionDecoder(this, methodCallResolver);
    this.statementDecoder = new StatementDecoder(this, expressionDecoder, methodCallResolver);
    this.fieldDecoder = new FieldDecoder(this, expressionDecoder);
  }

  Decoder() {
    this(new HashSet<>());
  }

  public AbstractNode decode(String document) {
    TweedleUnlinkedParser parser = new TweedleUnlinkedParser();
    if (parser.sourceContainsComments(document)) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle comments are not yet supported by the AST decoder.");
    }
    TweedleType tweedleType;
    try {
      tweedleType = parser.parseType(document);
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
      type.fields.add(fieldDecoder.decodeField(property));
    }
    List<UserField> fields = type.getDeclaredFields();
    List<TweedleMethod> tweedleMethods = tweedleClass.getMethods();
    List<UserMethod> userMethods = new ArrayList<>(tweedleMethods.size());
    for (TweedleMethod method : tweedleMethods) {
      UserMethod userMethod = decodeMethodSignature(method);
      type.methods.add(userMethod);
      userMethods.add(userMethod);
    }
    Map<String, UserMethod> zeroArgumentMethods = zeroArgumentMethodsByName(tweedleMethods, userMethods);
    for (int i = 0; i < tweedleMethods.size(); i++) {
      TweedleMethod tweedleMethod = tweedleMethods.get(i);
      UserMethod userMethod = userMethods.get(i);
      userMethod.body.setValue(statementDecoder.decodeMethodBody(
          tweedleMethod,
          userMethod.getReturnType(),
          userMethod.getRequiredParameters().toArray(UserParameter[]::new),
          fields,
          type,
          zeroArgumentMethods));
    }
    for (TweedleConstructor constructor : tweedleClass.getConstructors()) {
      type.constructors.add(decodeConstructor(tweedleClass, constructor, fields, type, zeroArgumentMethods));
    }
    return type;
  }

  private NamedUserConstructor decodeConstructor(
      TweedleClass declaringClass,
      TweedleConstructor constructor,
      List<UserField> fields,
      NamedUserType declaringType,
      Map<String, UserMethod> zeroArgumentMethods) {
    if (!constructor.getName().equals(declaringClass.getName())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle constructor name does not match declaring class: " + constructor.getName());
    }
    UserParameter[] allParameters = decodeAllParameters(
        constructor.getRequiredParameters(), constructor.getOptionalParameters(), "constructor parameter");
    return new NamedUserConstructor(
        allParameters,
        statementDecoder.decodeConstructorBody(constructor, allParameters, fields, declaringType, zeroArgumentMethods));
  }

  private UserParameter[] decodeAllParameters(
      List<TweedleRequiredParameter> required,
      List<TweedleOptionalParameter> optional,
      String usage) {
    List<UserParameter> all = new ArrayList<>();
    for (TweedleRequiredParameter p : required) {
      all.add(new UserParameter(p.getName(), resolveType(p.getType(), usage)));
    }
    for (TweedleOptionalParameter p : optional) {
      all.add(new UserParameter(p.getName(), resolveType(p.getType(), usage)));
    }
    return all.toArray(UserParameter[]::new);
  }

  private UserMethod decodeMethodSignature(TweedleMethod method) {
    AbstractType<?, ?, ?> returnType = resolveReturnType(method.getType());
    UserParameter[] allParameters = decodeAllParameters(
        method.getRequiredParameters(), method.getOptionalParameters(), "method parameter");
    UserMethod userMethod = new UserMethod(
        method.getName(),
        returnType,
        allParameters,
        new BlockStatement());
    userMethod.isStatic.setValue(method.isStatic());
    return userMethod;
  }

  // -- Package-private services used by delegates --

  boolean isLiteralArithmeticAllowed() {
    return allowLiteralArithmeticFieldInitializers;
  }

  UserLocal findLocal(List<UserLocal> locals, String name) {
    for (int i = locals.size() - 1; i >= 0; i--) {
      UserLocal local = locals.get(i);
      if (local.getName().equals(name)) {
        return local;
      }
    }
    return null;
  }

  UserParameter findParameter(UserParameter[] parameters, String name) {
    for (UserParameter parameter : parameters) {
      if (parameter.getName().equals(name)) {
        return parameter;
      }
    }
    return null;
  }

  UserField findField(List<UserField> fields, String name) {
    for (UserField field : fields) {
      if (field.getName().equals(name)) {
        return field;
      }
    }
    return null;
  }

  Expression primitiveLiteral(Object value) {
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

  AbstractType<?, ?, ?> resolveReturnType(TweedleType tweedleType) {
    if (tweedleType == TweedleVoidType.VOID) {
      return JavaType.VOID_TYPE;
    }
    return resolveType(tweedleType, "method return");
  }

  AbstractType<?, ?, ?> resolveType(TweedleType tweedleType, String usage) {
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

  AbstractType<?, ?, ?> resolveType(String typeName, String usage) {
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

  // -- Private helpers --

  private Map<String, UserMethod> zeroArgumentMethodsByName(
      List<TweedleMethod> tweedleMethods, List<UserMethod> userMethods) {
    Map<String, UserMethod> methodsByName = null;
    for (int i = 0; i < tweedleMethods.size(); i++) {
      TweedleMethod tweedleMethod = tweedleMethods.get(i);
      UserMethod userMethod = userMethods.get(i);
      if (!tweedleMethod.isStatic()
          && tweedleMethod.getRequiredParameters().isEmpty()
          && tweedleMethod.getOptionalParameters().isEmpty()
          && userMethod.getRequiredParameters().isEmpty()) {
        if (methodsByName == null) {
          methodsByName = new HashMap<>();
        }
        if (methodsByName.put(userMethod.getName(), userMethod) != null) {
          throw new UnsupportedTweedleDecodeException(
              "Duplicate zero-argument Tweedle methods are not supported by the AST decoder: "
                  + userMethod.getName());
        }
      }
    }
    return methodsByName != null ? methodsByName : Map.of();
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
