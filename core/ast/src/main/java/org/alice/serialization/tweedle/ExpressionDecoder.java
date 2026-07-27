package org.alice.serialization.tweedle;

import org.alice.tweedle.TweedleMethod;
import org.alice.tweedle.ast.BinaryExpression;
import org.alice.tweedle.ast.BinaryNumericExpression;
import org.alice.tweedle.ast.AdditionExpression;
import org.alice.tweedle.ast.DivisionExpression;
import org.alice.tweedle.ast.EqualToExpression;
import org.alice.tweedle.ast.GreaterThanExpression;
import org.alice.tweedle.ast.GreaterThanOrEqualExpression;
import org.alice.tweedle.ast.IdentifierReference;
import org.alice.tweedle.ast.LessThanExpression;
import org.alice.tweedle.ast.LessThanOrEqualExpression;
import org.alice.tweedle.ast.LogicalAndExpression;
import org.alice.tweedle.ast.LogicalNotExpression;
import org.alice.tweedle.ast.LogicalOrExpression;
import org.alice.tweedle.ast.MultiplicationExpression;
import org.alice.tweedle.ast.NotEqualToExpression;
import org.alice.tweedle.ast.StringConcatenationExpression;
import org.alice.tweedle.ast.SubtractionExpression;
import org.alice.tweedle.ast.TweedleExpression;
import org.alice.tweedle.TweedlePrimitiveValue;
import org.alice.tweedle.ast.ThisExpression;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.ArithmeticInfixExpression;
import org.lgna.project.ast.ConditionalInfixExpression;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.LocalAccess;
import org.lgna.project.ast.LogicalComplement;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.StringConcatenation;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserParameter;

import java.util.List;

class ExpressionDecoder {
  private final Decoder decoder;
  private final MethodCallResolver methodCallResolver;

  ExpressionDecoder(Decoder decoder, MethodCallResolver methodCallResolver) {
    this.decoder = decoder;
    this.methodCallResolver = methodCallResolver;
  }

  Expression decodeValueExpression(
      String ownerName,
      TweedleExpression expr,
      UserParameter[] parameters,
      List<UserLocal> priorLocals,
      List<UserField> fields) {
    if (expr instanceof TweedlePrimitiveValue<?> primitiveValue) {
      return decoder.primitiveLiteral(primitiveValue.getPrimitiveValue());
    }
    if (expr instanceof IdentifierReference identifierReference) {
      String name = identifierReference.getName();
      UserLocal local = decoder.findLocal(priorLocals, name);
      if (local != null) {
        return new LocalAccess(local);
      }
      UserParameter parameter = decoder.findParameter(parameters, name);
      if (parameter != null) {
        return new ParameterAccess(parameter);
      }
      UserField field = decoder.findField(fields, name);
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

  StringConcatenation decodeStringConcatenationExpression(
      String ownerName,
      StringConcatenationExpression stringConcat,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression lhs = decodeValueExpression(ownerName, stringConcat.getLhs(), parameters, locals, fields);
    Expression rhs = decodeValueExpression(ownerName, stringConcat.getRhs(), parameters, locals, fields);
    return new StringConcatenation(lhs, rhs);
  }

  boolean isComparisonExpression(BinaryExpression expr) {
    return expr instanceof EqualToExpression
        || expr instanceof NotEqualToExpression
        || expr instanceof LessThanExpression
        || expr instanceof LessThanOrEqualExpression
        || expr instanceof GreaterThanExpression
        || expr instanceof GreaterThanOrEqualExpression;
  }

  RelationalInfixExpression decodeRelationalExpression(
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

  ConditionalInfixExpression decodeLogicalInfixExpression(
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

  LogicalComplement decodeLogicalNotExpression(
      String ownerName,
      LogicalNotExpression logicalNot,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression operand = decodeValueExpression(ownerName, logicalNot.getExpression(), parameters, locals, fields);
    return new LogicalComplement(operand);
  }

  ArithmeticInfixExpression decodeBinaryNumericExpression(
      String ownerName,
      BinaryNumericExpression<?> binaryNumeric,
      UserParameter[] parameters,
      List<UserLocal> locals,
      List<UserField> fields) {
    Expression lhs = decodeValueExpression(ownerName, binaryNumeric.getLhs(), parameters, locals, fields);
    Expression rhs = decodeValueExpression(ownerName, binaryNumeric.getRhs(), parameters, locals, fields);
    org.alice.tweedle.TweedleType resultTweedleType = binaryNumeric.getType();
    AbstractType<?, ?, ?> astResultType = decoder.resolveType(
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

  Expression decodeMethodReturnExpression(
      TweedleMethod method,
      AbstractType<?, ?, ?> returnType,
      UserParameter[] allParameters,
      List<UserLocal> locals,
      List<UserField> fields,
      TweedleExpression returnExpression) {
    if (returnExpression instanceof TweedlePrimitiveValue<?> primitiveValue) {
      Expression expression = decoder.primitiveLiteral(primitiveValue.getPrimitiveValue());
      if (returnType.isAssignableFrom(expression.getType())) {
        return expression;
      }
      throw new UnsupportedTweedleDecodeException(
            "Tweedle method return expression type is not assignable to "
                + returnType.getName() + ": " + method.getName());
    }
    if (returnExpression instanceof IdentifierReference identifierReference) {
      UserLocal local = decoder.findLocal(locals, identifierReference.getName());
      if (local != null) {
        LocalAccess access = new LocalAccess(local);
        if (returnType.isAssignableFrom(access.getType())) {
          return access;
        }
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return identifier type is not assignable to "
                + returnType.getName() + ": " + method.getName() + "." + identifierReference.getName());
      }
      UserParameter parameter = decoder.findParameter(allParameters, identifierReference.getName());
      if (parameter != null) {
        ParameterAccess access = new ParameterAccess(parameter);
        if (returnType.isAssignableFrom(access.getType())) {
          return access;
        }
        throw new UnsupportedTweedleDecodeException(
            "Tweedle method return identifier type is not assignable to "
                + returnType.getName() + ": " + method.getName() + "." + identifierReference.getName());
      }
      UserField field = decoder.findField(fields, identifierReference.getName());
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
    UserField field = decoder.findField(fields, fieldAccess.getFieldName());
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
            + method.getName() + "." + methodCallResolver.describeMemberAccess(fieldAccess));
  }
}
