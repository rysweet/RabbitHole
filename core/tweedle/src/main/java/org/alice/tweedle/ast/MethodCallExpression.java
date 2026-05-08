package org.alice.tweedle.ast;

import org.alice.tweedle.TweedleValue;
import org.alice.tweedle.run.Frame;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MethodCallExpression extends MemberAccessExpression {

  private final String methodName;
  private final Map<String, TweedleExpression> arguments;
  private final boolean explicitTarget;

  public MethodCallExpression(TweedleExpression target, String methodName) {
    this(target, methodName, Collections.emptyMap(), true);
  }

  public MethodCallExpression(TweedleExpression target, String methodName, Map<String, TweedleExpression> arguments) {
    this(target, methodName, arguments, true);
  }

  public MethodCallExpression(TweedleExpression target, String methodName, Map<String, TweedleExpression> arguments, boolean explicitTarget) {
    super(target);
    this.methodName = methodName;
    this.arguments = arguments == null || arguments.isEmpty()
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(new HashMap<>(arguments));
    this.explicitTarget = explicitTarget;
  }

  @Override
  public TweedleValue evaluate(Frame frame) {
    evaluateTarget(frame);
    // TODO invoke the method on the target.
    return null;
  }

  public String getMethodName() {
    return methodName;
  }

  public Map<String, TweedleExpression> getArguments() {
    return arguments;
  }

  public boolean hasExplicitTarget() {
    return explicitTarget;
  }

  public TweedleExpression getArg(String argName) {
    return arguments.get(argName);
  }
}
