package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleConstructor;
import org.alice.tweedle.TweedleMethod;
import org.alice.tweedle.TweedleStatement;
import org.alice.tweedle.ast.IdentifierReference;
import org.alice.tweedle.ast.LocalVariableDeclaration;
import org.alice.tweedle.ast.MethodCallExpression;
import org.alice.tweedle.ast.ThisExpression;
import org.alice.tweedle.ast.TweedleExpression;
import org.alice.tweedle.ast.TweedleLocalVariable;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.WhileLoop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class StatementDecoder {
  private static final String ARGUMENT_BEARING_EXPLICIT_THIS_METHOD_CALLS =
      "argument-bearing explicit this method calls";
  private final Decoder decoder;
  private final ExpressionDecoder expressionDecoder;

  StatementDecoder(Decoder decoder, ExpressionDecoder expressionDecoder) {
    this.decoder = decoder;
    this.expressionDecoder = expressionDecoder;
  }

  ConstructorBlockStatement decodeConstructorBody(
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
        statements.add(preserveEnabledState(statement, localStatement));
        locals.add(localStatement.local.getValue());
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        statements.add(preserveEnabledState(
            statement,
            decodeConstructorAssignmentStatement(constructor, assignment, parameters, locals, fields)));
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof MethodCallExpression methodCall) {
        statements.add(preserveEnabledState(
            statement,
            decodeSameClassMethodCallStatement(
                declaringType, constructor.getName(), methodCall, zeroArgumentMethods, parameters, locals, fields)));
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

  BlockStatement decodeMethodBody(
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
        statements.add(preserveEnabledState(statement, localStatement));
        locals.add(localStatement.local.getValue());
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof org.alice.tweedle.ast.AssignmentExpression assignment) {
        statements.add(preserveEnabledState(
            statement,
            decodeMethodAssignmentStatement(method, assignment, allParameters, locals, fields)));
      } else if (statement instanceof org.alice.tweedle.ast.ConditionalStatement conditionalStatement) {
        statements.add(preserveEnabledState(
            statement,
            decodeIfStatement(
                method, allParameters, locals, fields, declaringType, zeroArgumentMethods, conditionalStatement)));
      } else if (statement instanceof org.alice.tweedle.ast.WhileLoop whileLoop) {
        if (returnType != JavaType.VOID_TYPE) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle while loops are only supported in void methods by the AST decoder: " + method.getName());
        }
        statements.add(preserveEnabledState(
            statement,
            decodeWhileLoop(method, allParameters, locals, fields, whileLoop)));
      } else if (statement instanceof org.alice.tweedle.ast.ExpressionStatement expressionStatement
          && expressionStatement.getExpression() instanceof MethodCallExpression methodCall) {
        statements.add(preserveEnabledState(
            statement,
            decodeSameClassMethodCallStatement(
                declaringType, method.getName(), methodCall, zeroArgumentMethods, allParameters, locals, fields)));
      } else if (statement instanceof org.alice.tweedle.ast.ReturnStatement returnStatement
          && i == body.size() - 1) {
        statements.add(preserveEnabledState(
            statement,
            decodeReturnStatement(method, returnType, allParameters, locals, fields, returnStatement)));
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

  private Statement decodeSameClassMethodCallStatement(
      NamedUserType declaringType,
      String ownerName,
      MethodCallExpression methodCall,
      Map<String, UserMethod> zeroArgumentMethods,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    if (methodCall.hasExplicitTarget() && !(methodCall.getTarget() instanceof ThisExpression)) {
      throw unsupportedZeroArgumentThisMethodCall(ownerName, methodCall);
    }
    if (!methodCall.getArguments().isEmpty()) {
      return decodeArgumentBearingSameClassMethodCallStatement(
          declaringType, ownerName, methodCall, parameters, locals, fields);
    }
    UserMethod targetMethod = zeroArgumentMethods.get(methodCall.getMethodName());
    if (targetMethod == null) {
      throw unsupportedZeroArgumentThisMethodCall(ownerName, methodCall);
    }
    return AstUtilities.createMethodInvocationStatement(
        org.lgna.project.ast.ThisExpression.createInstanceThatCanExistWithoutAnAncestorType(declaringType),
        targetMethod);
  }

  private Statement decodeArgumentBearingSameClassMethodCallStatement(
      NamedUserType declaringType,
      String ownerName,
      MethodCallExpression methodCall,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Map<String, TweedleExpression> arguments = methodCall.getArguments();
    UserMethod targetMethod =
        resolveSameClassMethodByArgumentLabels(declaringType, methodCall.getMethodName(), arguments.keySet());
    if (targetMethod == null) {
      throw unsupportedArgumentBearingExplicitThisMethodCall(ownerName, methodCall);
    }
    List<UserParameter> requiredParameters = targetMethod.getRequiredParameters();
    Expression[] argumentExpressions = new Expression[requiredParameters.size()];
    for (int i = 0; i < requiredParameters.size(); i++) {
      UserParameter parameter = requiredParameters.get(i);
      TweedleExpression argumentExpression = arguments.get(parameter.getName());
      if (argumentExpression == null) {
        throw unsupportedArgumentBearingExplicitThisMethodCall(ownerName, methodCall);
      }
      Expression decoded =
          expressionDecoder.decodeValueExpression(ownerName, argumentExpression, parameters, locals, fields);
      if (!parameter.getValueType().isAssignableFrom(decoded.getType())) {
        throw unsupportedArgumentBearingExplicitThisMethodCall(ownerName, methodCall);
      }
      argumentExpressions[i] = decoded;
    }
    return AstUtilities.createMethodInvocationStatement(
        org.lgna.project.ast.ThisExpression.createInstanceThatCanExistWithoutAnAncestorType(declaringType),
        targetMethod,
        argumentExpressions);
  }

  private UserMethod resolveSameClassMethodByArgumentLabels(
      NamedUserType declaringType, String methodName, Set<String> argumentLabels) {
    UserMethod match = null;
    for (UserMethod candidate : declaringType.getDeclaredMethods()) {
      if (candidate.isStatic() || !candidate.getName().equals(methodName)) {
        continue;
      }
      List<UserParameter> requiredParameters = candidate.getRequiredParameters();
      if (requiredParameters.size() != argumentLabels.size()) {
        continue;
      }
      Set<String> parameterNames = new HashSet<>();
      for (UserParameter parameter : requiredParameters) {
        parameterNames.add(parameter.getName());
      }
      if (!parameterNames.equals(argumentLabels)) {
        continue;
      }
      if (match != null) {
        return null;
      }
      match = candidate;
    }
    return match;
  }

  private ConditionalStatement decodeIfStatement(
      TweedleMethod method,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields,
      NamedUserType declaringType,
      Map<String, UserMethod> zeroArgumentMethods,
      org.alice.tweedle.ast.ConditionalStatement conditional) {
    Expression condition = expressionDecoder.decodeValueExpression(method.getName(), conditional.getCondition(), parameters, locals, fields);
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
        decoded.add(preserveEnabledState(
            statement,
            decodeMethodAssignmentStatement(method, assignment, parameters, locals, fields)));
      } else if (expression instanceof MethodCallExpression methodCall) {
        decoded.add(preserveEnabledState(
            statement,
            decodeSameClassMethodCallStatement(
                declaringType, method.getName(), methodCall, zeroArgumentMethods, parameters, locals, fields)));
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
        decoded.add(preserveEnabledState(
            statement,
            decodeMethodAssignmentStatement(method, assignment, parameters, locals, fields)));
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
    Expression condition = expressionDecoder.decodeValueExpression(method.getName(), whileLoop.getRunCondition(), parameters, locals, fields);
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
        decoded.add(preserveEnabledState(
            statement,
            decodeMethodAssignmentStatement(method, assignment, parameters, locals, fields)));
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
    AbstractType<?, ?, ?> localType = decoder.resolveType(tweedleLocal.getType(), "local variable");
    TweedleExpression initializer = tweedleLocal.getInitializer();
    Expression astInitializer = expressionDecoder.decodeValueExpression(ownerName, initializer, parameters, priorLocals, fields);
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
      UserLocal local = decoder.findLocal(locals, name);
      if (local != null) {
        if (!local.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle local variable assignment value type is not assignable to "
                  + local.getValueType().getName() + ": " + method.getName() + "." + name);
        }
        return AstUtilities.createLocalAssignmentStatement(local, rhs);
      }
      UserField field = decoder.findField(fields, name);
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
                + method.getName() + "." + decoder.describeMemberAccess(fieldAccess));
      }
      UserField field = decoder.findField(fields, fieldAccess.getFieldName());
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

  Statement decodeConstructorAssignmentStatement(
      TweedleConstructor constructor,
      org.alice.tweedle.ast.AssignmentExpression assignment,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression rhs = decodeAssignmentRhs(constructor.getName(), assignment.getValueExp(), parameters, locals, fields);
    TweedleExpression assignee = assignment.getAssigneeExp();
    if (assignee instanceof IdentifierReference identifierReference) {
      String name = identifierReference.getName();
      UserLocal local = decoder.findLocal(locals, name);
      if (local != null) {
        if (!local.getValueType().isAssignableFrom(rhs.getType())) {
          throw new UnsupportedTweedleDecodeException(
              "Tweedle constructor local variable assignment value type is not assignable to "
                  + local.getValueType().getName() + ": " + constructor.getName() + "." + name);
        }
        return AstUtilities.createLocalAssignmentStatement(local, rhs);
      }
      UserField field = decoder.findField(fields, name);
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
                + constructor.getName() + "." + decoder.describeMemberAccess(fieldAccess));
      }
      UserField field = decoder.findField(fields, fieldAccess.getFieldName());
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
    return expressionDecoder.decodeValueExpression(ownerName, value, parameters, locals, fields);
  }

  private org.lgna.project.ast.ReturnStatement decodeReturnStatement(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] allParameters,
      List<UserLocal> locals,
      List<UserField> fields,
      org.alice.tweedle.ast.ReturnStatement returnStatement) {
    Expression expression =
        expressionDecoder.decodeMethodReturnExpression(method, returnType, allParameters, locals, fields, returnStatement.getExpression());
    return new org.lgna.project.ast.ReturnStatement(returnType, expression);
  }

  private static <T extends Statement> T preserveEnabledState(TweedleStatement source, T decoded) {
    decoded.isEnabled.setValue(source.isEnabled());
    return decoded;
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
            + ownerName + "." + decoder.describeMethodCall(methodCall));
  }

  private UnsupportedTweedleDecodeException unsupportedArgumentBearingExplicitThisMethodCall(
      String ownerName,
      MethodCallExpression methodCall) {
    return new UnsupportedTweedleDecodeException(
        "Tweedle " + ARGUMENT_BEARING_EXPLICIT_THIS_METHOD_CALLS + " are not supported by the AST decoder: "
            + ownerName + "." + decoder.describeMethodCall(methodCall));
  }

  private UnsupportedTweedleDecodeException unsupportedConstructorBody(TweedleConstructor constructor) {
    return new UnsupportedTweedleDecodeException(
        "Tweedle constructor bodies are not yet supported by the AST decoder: " + constructor.getName());
  }
}
