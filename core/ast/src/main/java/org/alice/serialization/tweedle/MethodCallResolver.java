package org.alice.serialization.tweedle;

import org.alice.tweedle.ast.IdentifierReference;
import org.alice.tweedle.ast.MethodCallExpression;
import org.alice.tweedle.ast.ThisExpression;
import org.alice.tweedle.ast.TweedleExpression;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared method-call boundary logic for the Tweedle decoders.
 *
 * <p>Centralises the target-shape description used to build
 * unsupported-boundary messages ({@link #describeMethodCall} /
 * {@link #describeMemberAccess}) and the same-class argument-label method
 * resolution ({@link #resolveSameClassMethodByArgumentLabels}). {@link
 * StatementDecoder}, {@link ExpressionDecoder}, and {@link Decoder} share a
 * single instance so that a future method-call decode gap — currently handled
 * only for statements — is closed in one place rather than three.
 *
 * <p>Every method here is a pure function of its arguments; the resolver holds
 * no state and is safe to share by reference.
 */
class MethodCallResolver {

  String describeMethodCall(MethodCallExpression methodCall) {
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

  String describeMemberAccess(org.alice.tweedle.ast.FieldAccess fieldAccess) {
    TweedleExpression target = fieldAccess.getTarget();
    if (target instanceof ThisExpression) {
      return "this." + fieldAccess.getFieldName();
    }
    if (target instanceof IdentifierReference identifierReference) {
      return identifierReference.getName() + "." + fieldAccess.getFieldName();
    }
    return "<unsupported>." + fieldAccess.getFieldName();
  }

  UserMethod resolveSameClassMethodByArgumentLabels(
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
}
