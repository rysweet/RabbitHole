package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleArrayType;
import org.alice.tweedle.TweedleClass;
import org.alice.tweedle.TweedleConstructor;
import org.alice.tweedle.TweedleLinkException;
import org.alice.tweedle.TweedleField;
import org.alice.tweedle.TweedleMethod;
import org.alice.tweedle.TweedleNull;
import org.alice.tweedle.TweedleOptionalParameter;
import org.alice.tweedle.TweedlePrimitiveValue;
import org.alice.tweedle.TweedleRequiredParameter;
import org.alice.tweedle.TweedleStatement;
import org.alice.tweedle.TweedleType;
import org.alice.tweedle.TweedleVoidType;
import org.alice.tweedle.ast.AdditionExpression;
import org.alice.tweedle.ast.BinaryExpression;
import org.alice.tweedle.ast.BinaryNumericExpression;
import org.alice.tweedle.ast.DivisionExpression;
import org.alice.tweedle.ast.EqualToExpression;
import org.alice.tweedle.ast.GreaterThanExpression;
import org.alice.tweedle.ast.GreaterThanOrEqualExpression;
import org.alice.tweedle.ast.IdentifierReference;
import org.alice.tweedle.ast.LessThanExpression;
import org.alice.tweedle.ast.LessThanOrEqualExpression;
import org.alice.tweedle.ast.LocalVariableDeclaration;
import org.alice.tweedle.ast.LogicalAndExpression;
import org.alice.tweedle.ast.LogicalNotExpression;
import org.alice.tweedle.ast.LogicalOrExpression;
import org.alice.tweedle.ast.MethodCallExpression;
import org.alice.tweedle.ast.MultiplicationExpression;
import org.alice.tweedle.ast.NotEqualToExpression;
import org.alice.tweedle.ast.StringConcatenationExpression;
import org.alice.tweedle.ast.SubtractionExpression;
import org.alice.tweedle.ast.TweedleArrayInitializer;
import org.alice.tweedle.ast.TweedleExpression;
import org.alice.tweedle.ast.TweedleLocalVariable;
import org.alice.tweedle.ast.ThisExpression;
import org.alice.tweedle.unlinked.TweedleUnlinkedParser;
import org.lgna.common.Resource;
import org.lgna.project.ast.AbstractDeclaration;
import org.lgna.project.ast.AbstractNode;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.ConditionalInfixExpression;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.StringConcatenation;
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
import org.lgna.project.ast.LogicalComplement;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.WhileLoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Decoder {
  private static final String ARGUMENT_BEARING_EXPLICIT_THIS_METHOD_CALLS =
      "argument-bearing explicit this method calls";
  private final Map<String, AbstractType<?, ?, ?>> terminalTypesByName;
  private final boolean allowLiteralArithmeticFieldInitializers;
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
    this(terminals, true);
  }

  Decoder(Set<AbstractDeclaration> terminals, boolean allowLiteralArithmeticFieldInitializers) {
    this.allowLiteralArithmeticFieldInitializers = allowLiteralArithmeticFieldInitializers;
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
      userMethod.body.setValue(decodeMethodBody(
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
        decodeConstructorBody(constructor, allParameters, fields, declaringType, zeroArgumentMethods));
  }

  private ConstructorBlockStatement decodeConstructorBody(
      TweedleConstructor constructor,
      UserParameter[] parameters,
      List<UserField> fields,
      NamedUserType declaringType,
      Map<String, UserMethod> zeroArgumentMethods) {
    List<TweedleStatement> body = constructor.getBody();
    List<Statement> statements = new ArrayList<>(body.size());
    List<UserLocal> locals = new ArrayList<>();
    for (TweedleStatement statement : body) {
      if (statement instanceof LocalVariableDeclaration localVariableDeclaration) {
        LocalDeclarationStatement localStatement =
            decodeLocalDeclarationStatement(constructor.getName(), localVariableDeclaration, parameters, locals, fields);
        statements.add(localStatement);
        locals.add(localStatement.local.getValue());
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        statements.add(decodeConstructorAssignmentStatement(constructor, assignment, parameters, locals, fields));
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof MethodCallExpression methodCall) {
        statements.add(decodeZeroArgumentSameClassMethodCallStatement(
            declaringType, constructor.getName(), methodCall, zeroArgumentMethods));
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
    UserParameter[] allParameters = decodeAllParameters(method.getRequiredParameters(), method.getOptionalParameters(), "method parameter");
    return new UserMethod(
        method.getName(),
        returnType,
        allParameters,
        new BlockStatement());
  }

  private BlockStatement decodeMethodBody(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] allParameters,
      List<UserField> fields,
      NamedUserType declaringType,
      Map<String, UserMethod> zeroArgumentMethods) {
    List<TweedleStatement> body = method.getBody();
    if (body.isEmpty()) {
      if (returnType != JavaType.VOID_TYPE) {
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return values require a supported return statement: " + method.getName());
      }
      return new BlockStatement();
    }
    List<Statement> statements = new ArrayList<>(body.size());
    List<UserLocal> locals = new ArrayList<>();
    for (int i = 0; i < body.size(); i++) {
      TweedleStatement statement = body.get(i);
      if (statement instanceof LocalVariableDeclaration localVariableDeclaration) {
        LocalDeclarationStatement localStatement =
            decodeLocalDeclarationStatement(method.getName(), localVariableDeclaration, allParameters, locals, fields);
        statements.add(localStatement);
        locals.add(localStatement.local.getValue());
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        statements.add(decodeMethodAssignmentStatement(method, assignment, allParameters, locals, fields));
      } else if (statement instanceof org.alice.tweedle.ast.ConditionalStatement conditionalStatement) {
        statements.add(decodeIfStatement(
            method, allParameters, locals, fields, declaringType, zeroArgumentMethods, conditionalStatement));
      } else if (statement instanceof org.alice.tweedle.ast.WhileLoop whileLoop) {
        if (returnType != JavaType.VOID_TYPE) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle while loops are only supported in void methods by the AST decoder: " + method.getName());
        }
        statements.add(decodeWhileLoop(method, allParameters, locals, fields, whileLoop));
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof MethodCallExpression methodCall) {
        statements.add(decodeZeroArgumentSameClassMethodCallStatement(
            declaringType, method.getName(), methodCall, zeroArgumentMethods));
      } else if (statement instanceof org.alice.tweedle.ast.ReturnStatement returnStatement
          && i == body.size() - 1) {
        statements.add(decodeReturnStatement(method, returnType, allParameters, locals, fields, returnStatement));
      } else {
        throw unsupportedMethodBody(method);
      }
    }
    if (returnType != JavaType.VOID_TYPE
        && !(body.get(body.size() - 1) instanceof org.alice.tweedle.ast.ReturnStatement)) {
      throw unsupportedMethodBody(method);
    }
    return new BlockStatement(statements.toArray(Statement[]::new));
  }

  private Statement decodeZeroArgumentSameClassMethodCallStatement(
      NamedUserType declaringType,
      String ownerName,
      MethodCallExpression methodCall,
      Map<String, UserMethod> zeroArgumentMethods) {
    boolean hasArguments = !methodCall.getArguments().isEmpty();
    if (methodCall.hasExplicitTarget()) {
      if (!(methodCall.getTarget() instanceof ThisExpression)) {
        throw unsupportedZeroArgumentThisMethodCall(ownerName, methodCall);
      }
      if (hasArguments) {
        throw unsupportedArgumentBearingExplicitThisMethodCall(ownerName, methodCall);
      }
    } else if (hasArguments) {
      throw unsupportedZeroArgumentThisMethodCall(ownerName, methodCall);
    }
    UserMethod targetMethod = zeroArgumentMethods.get(methodCall.getMethodName());
    if (targetMethod == null) {
      throw unsupportedZeroArgumentThisMethodCall(ownerName, methodCall);
    }
    return AstUtilities.createMethodInvocationStatement(
        org.lgna.project.ast.ThisExpression.createInstanceThatCanExistWithoutAnAncestorType(declaringType),
        targetMethod);
  }

  private ConditionalStatement decodeIfStatement(
      TweedleMethod method,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields,
      NamedUserType declaringType,
      Map<String, UserMethod> zeroArgumentMethods,
      org.alice.tweedle.ast.ConditionalStatement conditional) {
    Expression condition = decodeValueExpression(method.getName(), conditional.getCondition(), parameters, locals, fields);
    if (!JavaType.BOOLEAN_OBJECT_TYPE.isAssignableFrom(condition.getType())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle if condition must be a Boolean expression: " + method.getName());
    }
    List<TweedleStatement> thenBlock = conditional.getThenBlock();
    List<TweedleStatement> elseBlock = conditional.getElseBlock();
    BlockStatement thenBody;
    BlockStatement elseBody;
    if (elseBlock.isEmpty()) {
      thenBody = decodeSimpleIfBody(method, parameters, locals, fields, declaringType, zeroArgumentMethods, thenBlock);
      elseBody = new BlockStatement();
    } else {
      thenBody = decodeAssignmentOnlyConditionalBranchBody(method, parameters, locals, fields, thenBlock);
      elseBody = decodeAssignmentOnlyConditionalBranchBody(method, parameters, locals, fields, elseBlock);
    }
    return new ConditionalStatement(
        new BooleanExpressionBodyPair[]{new BooleanExpressionBodyPair(condition, thenBody)},
        elseBody);
  }

  private BlockStatement decodeSimpleIfBody(
      TweedleMethod method,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields,
      NamedUserType declaringType,
      Map<String, UserMethod> zeroArgumentMethods,
      List<TweedleStatement> statements) {
    List<Statement> decoded = new ArrayList<>(statements.size());
    for (TweedleStatement statement : statements) {
      if (!(statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement)) {
        throw unsupportedSimpleIfBody(method);
      }
      TweedleExpression expression = expressionStatement.getExpression();
      if (expression instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        decoded.add(decodeMethodAssignmentStatement(method, assignment, parameters, locals, fields));
      } else if (expression instanceof MethodCallExpression methodCall) {
        decoded.add(decodeZeroArgumentSameClassMethodCallStatement(
            declaringType, method.getName(), methodCall, zeroArgumentMethods));
      } else {
        throw unsupportedSimpleIfBody(method);
      }
    }
    return new BlockStatement(decoded.toArray(Statement[]::new));
  }

  private BlockStatement decodeAssignmentOnlyConditionalBranchBody(
      TweedleMethod method,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields,
      List<TweedleStatement> statements) {
    List<Statement> decoded = new ArrayList<>(statements.size());
    for (TweedleStatement statement : statements) {
      if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        decoded.add(decodeMethodAssignmentStatement(method, assignment, parameters, locals, fields));
      } else {
        throw new UnsupportedTweedleDecodeException(
            "Only assignment statements are supported in Tweedle if/else bodies by the AST decoder: "
                + method.getName());
      }
    }
    return new BlockStatement(decoded.toArray(Statement[]::new));
  }

  private WhileLoop decodeWhileLoop(
      TweedleMethod method,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields,
      org.alice.tweedle.ast.WhileLoop whileLoop) {
    Expression condition = decodeValueExpression(method.getName(), whileLoop.getRunCondition(), parameters, locals, fields);
    if (!JavaType.BOOLEAN_OBJECT_TYPE.isAssignableFrom(condition.getType())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle while condition must be a Boolean expression: " + method.getName());
    }
    BlockStatement body = decodeWhileLoopBody(method, parameters, locals, fields, whileLoop.getStatements());
    return new WhileLoop(condition, body);
  }

  private BlockStatement decodeWhileLoopBody(
      TweedleMethod method,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields,
      List<TweedleStatement> statements) {
    List<Statement> decoded = new ArrayList<>(statements.size());
    for (TweedleStatement statement : statements) {
      if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        decoded.add(decodeMethodAssignmentStatement(method, assignment, parameters, locals, fields));
      } else {
        throw new UnsupportedTweedleDecodeException(
            "Only assignment statements are supported in Tweedle while loop bodies by the AST decoder: "
                + method.getName());
      }
    }
    return new BlockStatement(decoded.toArray(Statement[]::new));
  }

  private LocalDeclarationStatement decodeLocalDeclarationStatement(
      String ownerName,
      LocalVariableDeclaration localVariableDeclaration,
      UserParameter[] parameters,
      List<UserLocal> priorLocals,
      List<UserField> fields) {
    TweedleLocalVariable tweedleLocal = localVariableDeclaration.getDeclaration();
    AbstractType<?, ?, ?> localType = resolveType(tweedleLocal.getType(), "local variable");
    TweedleExpression initializer = tweedleLocal.getInitializer();
    Expression astInitializer = decodeValueExpression(ownerName, initializer, parameters, priorLocals, fields);
    if (!localType.isAssignableFrom(astInitializer.getType())) {
      throw new UnsupportedTweedleDecodeException(
          "Tweedle local variable initializer type is not assignable to "
              + localType.getName() + ": " + ownerName + "." + tweedleLocal.getName());
    }
    return new LocalDeclarationStatement(
        new UserLocal(tweedleLocal.getName(), localType, localVariableDeclaration.isConstant()),
        astInitializer);
  }

  private Statement decodeMethodAssignmentStatement(
      TweedleMethod method,
      org.alice.tweedle.ast.AssignmentExpression assignment,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression rhs = decodeAssignmentRhs(method.getName(), assignment.getValueExp(), parameters, locals, fields);
    TweedleExpression assignee = assignment.getAssigneeExp();
    if (assignee instanceof IdentifierReference identifierReference) {
      String name = identifierReference.getName();
      UserLocal local = findLocal(locals, name);
      if (local != null) {
        if (!local.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle local variable assignment value type is not assignable to "
                  + local.getValueType().getName() + ": " + method.getName() + "." + name);
        }
        return AstUtilities.createLocalAssignmentStatement(local, rhs);
      }
      UserField field = findField(fields, name);
      if (field != null) {
        if (!field.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle field assignment value type is not assignable to "
                  + field.getValueType().getName() + ": " + method.getName() + "." + name);
        }
        return AstUtilities.createFieldAssignmentStatement(field, rhs);
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle assignment target is not a known local or field: " + method.getName() + "." + name);
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

  private Statement decodeConstructorAssignmentStatement(
      TweedleConstructor constructor,
      org.alice.tweedle.ast.AssignmentExpression assignment,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression rhs = decodeAssignmentRhs(constructor.getName(), assignment.getValueExp(), parameters, locals, fields);
    TweedleExpression assignee = assignment.getAssigneeExp();
    if (assignee instanceof IdentifierReference identifierReference) {
      String name = identifierReference.getName();
      UserLocal local = findLocal(locals, name);
      if (local != null) {
        if (!local.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle constructor local variable assignment value type is not assignable to "
                  + local.getValueType().getName() + ": " + constructor.getName() + "." + name);
        }
        return AstUtilities.createLocalAssignmentStatement(local, rhs);
      }
      UserField field = findField(fields, name);
      if (field != null) {
        if (!field.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle constructor field assignment value type is not assignable to "
                  + field.getValueType().getName() + ": " + constructor.getName() + "." + name);
        }
        return AstUtilities.createFieldAssignmentStatement(field, rhs);
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle constructor assignment target is not a known local or field: " + constructor.getName() + "." + name);
    }
    if (assignee instanceof org.alice.tweedle.ast.FieldAccess fieldAccess) {
      if (!(fieldAccess.getTarget() instanceof ThisExpression)) {
        throw new UnsupportedTweedleDecodeException(
            "Only this.field Tweedle constructor assignment targets are supported by the AST decoder: "
                + constructor.getName() + "." + describeMemberAccess(fieldAccess));
      }
      UserField field = findField(fields, fieldAccess.getFieldName());
      if (field == null) {
        throw new UnsupportedTweedleDecodeException(
            "Tweedle constructor this.field assignment target is not a known field: " + constructor.getName() + "." + fieldAccess.getFieldName());
      }
      if (!field.getValueType().isAssignableFrom(rhs.getType())) {
        throw new UnsupportedTweedleDecodeException(
            "Tweedle constructor field assignment value type is not assignable to "
                + field.getValueType().getName() + ": " + constructor.getName() + ".this." + fieldAccess.getFieldName());
      }
      return AstUtilities.createFieldAssignmentStatement(field, rhs);
    }
    throw new UnsupportedTweedleDecodeException(
        "Unsupported Tweedle assignment target in constructor: " + constructor.getName());
  }

  private Expression decodeAssignmentRhs(
      String ownerName,
      TweedleExpression value,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    return decodeValueExpression(ownerName, value, parameters, locals, fields);
  }

  private org.lgna.project.ast.ReturnStatement decodeReturnStatement(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] allParameters,
      List<UserLocal> locals,
      List<UserField> fields,
      org.alice.tweedle.ast.ReturnStatement returnStatement) {
    Expression expression =
        decodeMethodReturnExpression(method, returnType, allParameters, locals, fields, returnStatement.getExpression());
    return new org.lgna.project.ast.ReturnStatement(returnType, expression);
  }

  private Expression decodeValueExpression(
      String ownerName,
      TweedleExpression expr,
      UserParameter[] parameters,
      List<UserLocal> priorLocals,
      List<UserField> fields) {
    if (expr instanceof TweedlePrimitiveValue<?> primitiveValue) {
      return primitiveLiteral(primitiveValue.getPrimitiveValue());
    }
    if (expr instanceof IdentifierReference identifierReference) {
      String name = identifierReference.getName();
      UserLocal local = findLocal(priorLocals, name);
      if (local != null) {
        return new LocalAccess(local);
      }
      UserParameter parameter = findParameter(parameters, name);
      if (parameter != null) {
        return new ParameterAccess(parameter);
      }
      UserField field = findField(fields, name);
      if (field != null) {
        return new FieldAccess(field);
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle value expression identifier is not a known local, parameter, or field: "
              + ownerName + "." + name);
    }
    if (expr instanceof BinaryExpression binaryExpr && isComparisonExpression(binaryExpr)) {
      return decodeRelationalExpression(ownerName, binaryExpr, parameters, priorLocals, fields);
    }
    if (expr instanceof LogicalAndExpression<?> logicalAnd) {
      return decodeLogicalInfixExpression(ownerName, logicalAnd, ConditionalInfixExpression.Operator.AND, parameters, priorLocals, fields);
    }
    if (expr instanceof LogicalOrExpression<?> logicalOr) {
      return decodeLogicalInfixExpression(ownerName, logicalOr, ConditionalInfixExpression.Operator.OR, parameters, priorLocals, fields);
    }
    if (expr instanceof LogicalNotExpression logicalNot) {
      return decodeLogicalNotExpression(ownerName, logicalNot, parameters, priorLocals, fields);
    }
    if (expr instanceof BinaryNumericExpression<?> binaryNumeric) {
      return decodeBinaryNumericExpression(ownerName, binaryNumeric, parameters, priorLocals, fields);
    }
    if (expr instanceof StringConcatenationExpression stringConcat) {
      return decodeStringConcatenationExpression(ownerName, stringConcat, parameters, priorLocals, fields);
    }
    throw new UnsupportedTweedleDecodeException(
        "Unsupported Tweedle value expression (only primitive literals, identifier references, "
            + "arithmetic binary expressions, string concatenation, comparison expressions, "
            + "and logical expressions are supported): " + ownerName);
  }

  private StringConcatenation decodeStringConcatenationExpression(
      String ownerName,
      StringConcatenationExpression stringConcat,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression lhs = decodeValueExpression(ownerName, stringConcat.getLhs(), parameters, locals, fields);
    Expression rhs = decodeValueExpression(ownerName, stringConcat.getRhs(), parameters, locals, fields);
    return new StringConcatenation(lhs, rhs);
  }

  private boolean isComparisonExpression(BinaryExpression expr) {
    return expr instanceof EqualToExpression
        || expr instanceof NotEqualToExpression
        || expr instanceof LessThanExpression
        || expr instanceof LessThanOrEqualExpression
        || expr instanceof GreaterThanExpression
        || expr instanceof GreaterThanOrEqualExpression;
  }

  private RelationalInfixExpression decodeRelationalExpression(
      String ownerName,
      BinaryExpression binaryExpr,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression lhs = decodeValueExpression(ownerName, binaryExpr.getLhs(), parameters, locals, fields);
    Expression rhs = decodeValueExpression(ownerName, binaryExpr.getRhs(), parameters, locals, fields);
    RelationalInfixExpression.Operator operator = relationalOperator(ownerName, binaryExpr);
    return new RelationalInfixExpression(lhs, operator, rhs, lhs.getType(), rhs.getType());
  }

  private RelationalInfixExpression.Operator relationalOperator(String ownerName, BinaryExpression expr) {
    if (expr instanceof EqualToExpression) {
      return RelationalInfixExpression.Operator.EQUALS;
    }
    if (expr instanceof NotEqualToExpression) {
      return RelationalInfixExpression.Operator.NOT_EQUALS;
    }
    if (expr instanceof LessThanExpression) {
      return RelationalInfixExpression.Operator.LESS;
    }
    if (expr instanceof LessThanOrEqualExpression) {
      return RelationalInfixExpression.Operator.LESS_EQUALS;
    }
    if (expr instanceof GreaterThanExpression) {
      return RelationalInfixExpression.Operator.GREATER;
    }
    if (expr instanceof GreaterThanOrEqualExpression) {
      return RelationalInfixExpression.Operator.GREATER_EQUALS;
    }
    throw new UnsupportedTweedleDecodeException(
        "Unsupported Tweedle comparison operator " + expr.getClass().getSimpleName() + ": " + ownerName);
  }

  private ConditionalInfixExpression decodeLogicalInfixExpression(
      String ownerName,
      BinaryExpression binaryExpr,
      ConditionalInfixExpression.Operator operator,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression lhs = decodeValueExpression(ownerName, binaryExpr.getLhs(), parameters, locals, fields);
    Expression rhs = decodeValueExpression(ownerName, binaryExpr.getRhs(), parameters, locals, fields);
    return new ConditionalInfixExpression(lhs, operator, rhs);
  }

  private LogicalComplement decodeLogicalNotExpression(
      String ownerName,
      LogicalNotExpression logicalNot,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression operand = decodeValueExpression(ownerName, logicalNot.getExpression(), parameters, locals, fields);
    return new LogicalComplement(operand);
  }

  private ArithmeticInfixExpression decodeBinaryNumericExpression(
      String ownerName,
      BinaryNumericExpression<?> binaryNumeric,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression lhs = decodeValueExpression(ownerName, binaryNumeric.getLhs(), parameters, locals, fields);
    Expression rhs = decodeValueExpression(ownerName, binaryNumeric.getRhs(), parameters, locals, fields);
    org.alice.tweedle.TweedleType resultTweedleType = binaryNumeric.getType();
    AbstractType<?, ?, ?> astResultType = resolveType(
        resultTweedleType != null ? resultTweedleType.getName() : null, "arithmetic expression");
    ArithmeticInfixExpression.Operator operator = arithmeticOperator(ownerName, binaryNumeric, resultTweedleType);
    return new ArithmeticInfixExpression(lhs, operator, rhs, astResultType);
  }

  private ArithmeticInfixExpression.Operator arithmeticOperator(
      String ownerName,
      BinaryNumericExpression<?> binaryNumeric,
      org.alice.tweedle.TweedleType resultType) {
    if (binaryNumeric instanceof AdditionExpression) {
      return ArithmeticInfixExpression.Operator.PLUS;
    }
    if (binaryNumeric instanceof SubtractionExpression) {
      return ArithmeticInfixExpression.Operator.MINUS;
    }
    if (binaryNumeric instanceof MultiplicationExpression) {
      return ArithmeticInfixExpression.Operator.TIMES;
    }
    if (binaryNumeric instanceof DivisionExpression) {
      String typeName = resultType != null ? resultType.getName() : null;
      if ("WholeNumber".equals(typeName)) {
        return ArithmeticInfixExpression.Operator.INTEGER_DIVIDE;
      }
      if ("DecimalNumber".equals(typeName)) {
        return ArithmeticInfixExpression.Operator.REAL_DIVIDE;
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle division expression has ambiguous numeric type "
              + "(both operands must be explicitly WholeNumber or DecimalNumber): " + ownerName);
    }
    throw new UnsupportedTweedleDecodeException(
        "Unsupported Tweedle arithmetic operator " + binaryNumeric.getClass().getSimpleName()
            + ": " + ownerName);
  }

  private Expression decodeMethodReturnExpression(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] allParameters,
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
      UserParameter parameter = findParameter(allParameters, identifierReference.getName());
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
    if (returnExpression instanceof StringConcatenationExpression stringConcat) {
      StringConcatenation concat = decodeStringConcatenationExpression(
          method.getName(), stringConcat, allParameters, locals, fields);
      if (returnType.isAssignableFrom(concat.getType())) {
        return concat;
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method return string concatenation type is not assignable to "
              + returnType.getName() + ": " + method.getName());
    }
    if (returnExpression instanceof BinaryExpression binaryExpr && isComparisonExpression(binaryExpr)) {
      RelationalInfixExpression comparison =
          decodeRelationalExpression(method.getName(), binaryExpr, allParameters, locals, fields);
      if (returnType.isAssignableFrom(comparison.getType())) {
        return comparison;
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method return comparison expression type is not assignable to "
              + returnType.getName() + ": " + method.getName());
    }
    if (returnExpression instanceof LogicalAndExpression<?> logicalAnd) {
      ConditionalInfixExpression conditional =
          decodeLogicalInfixExpression(method.getName(), logicalAnd, ConditionalInfixExpression.Operator.AND, allParameters, locals, fields);
      if (returnType.isAssignableFrom(conditional.getType())) {
        return conditional;
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method return logical && expression type is not assignable to "
              + returnType.getName() + ": " + method.getName());
    }
    if (returnExpression instanceof LogicalOrExpression<?> logicalOr) {
      ConditionalInfixExpression conditional =
          decodeLogicalInfixExpression(method.getName(), logicalOr, ConditionalInfixExpression.Operator.OR, allParameters, locals, fields);
      if (returnType.isAssignableFrom(conditional.getType())) {
        return conditional;
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method return logical || expression type is not assignable to "
              + returnType.getName() + ": " + method.getName());
    }
    if (returnExpression instanceof LogicalNotExpression logicalNot) {
      LogicalComplement complement =
          decodeLogicalNotExpression(method.getName(), logicalNot, allParameters, locals, fields);
      if (returnType.isAssignableFrom(complement.getType())) {
        return complement;
      }
      throw new UnsupportedTweedleDecodeException(
          "Tweedle method return logical ! expression type is not assignable to "
              + returnType.getName() + ": " + method.getName());
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

  private Map<String, UserMethod> zeroArgumentMethodsByName(List<TweedleMethod> tweedleMethods, List<UserMethod> userMethods) {
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
    if (allowLiteralArithmeticFieldInitializers
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
    Expression expression = decodeBinaryNumericExpression(
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

  private UnsupportedTweedleDecodeException unsupportedSimpleIfBody(TweedleMethod method) {
    return new UnsupportedTweedleDecodeException(
        "Only assignment statements, explicit zero-argument this-method calls, "
            + "and implicit zero-argument same-class method calls are supported "
            + "in Tweedle simple if bodies by the AST decoder: " + method.getName());
  }

  private UnsupportedTweedleDecodeException unsupportedZeroArgumentThisMethodCall(
      String ownerName,
      MethodCallExpression methodCall) {
    return new UnsupportedTweedleDecodeException(
        "Only explicit zero-argument this-method calls or implicit zero-argument same-class method calls "
            + "declared on the current Tweedle type "
            + "are supported by the AST decoder: "
            + ownerName + "." + describeMethodCall(methodCall));
  }

  private UnsupportedTweedleDecodeException unsupportedArgumentBearingExplicitThisMethodCall(
      String ownerName,
      MethodCallExpression methodCall) {
    return new UnsupportedTweedleDecodeException(
        "Tweedle " + ARGUMENT_BEARING_EXPLICIT_THIS_METHOD_CALLS + " are not supported by the AST decoder: "
            + ownerName + "." + describeMethodCall(methodCall));
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

  private String describeMethodCall(MethodCallExpression methodCall) {
    if (!methodCall.hasExplicitTarget()) {
      return methodCall.getMethodName();
    }
    TweedleExpression target = methodCall.getTarget();
    if (target instanceof ThisExpression) {
      return "this." + methodCall.getMethodName();
    }
    if (target instanceof IdentifierReference identifierReference) {
      return identifierReference.getName() + "." + methodCall.getMethodName();
    }
    if (target instanceof org.alice.tweedle.ast.FieldAccess fieldAccess) {
      return describeMemberAccess(fieldAccess) + "." + methodCall.getMethodName();
    }
    if (target instanceof MethodCallExpression targetMethodCall) {
      return describeMethodCall(targetMethodCall) + "." + methodCall.getMethodName();
    }
    return "<unsupported>." + methodCall.getMethodName();
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
