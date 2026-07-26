package org.alice.serialization.tweedle;

import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessageSeverity;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.lgna.project.ast.*;
import org.lgna.project.code.ProcessableNode;

import java.util.Map;

/**
 * Companion class that encapsulates argument-encoding logic extracted from
 * {@link TweedleEncoder}. Delegates back to the owning encoder for
 * super-class calls and protected formatting methods via package-private
 * bridge methods.
 */
class ArgumentEncoder {

  private final TweedleEncoder encoder;

  ArgumentEncoder(TweedleEncoder encoder) {
    this.encoder = encoder;
  }

  void processKeyedArgument(JavaKeyedArgument arg) {
    Expression expressionValue = arg.expression.getValue();
    if (expressionValue instanceof MethodInvocation methodInvocation) {
      AbstractMethod method = methodInvocation.method.getValue();
      AbstractType<?, ?, ?> factoryType = AstTypeResolutionHelpers.getKeywordFactoryType(arg);
      if (factoryType != null) {
        final String label = method.getName();
        encoder.forwardAppendString(TweedleEncoderData.methodParamsToRelabel.getOrDefault(label, label));
        encoder.forwardAppendString(": ");
        appendOneArgument(methodInvocation);
        return;
      }
    }
    encoder.processExpression(expressionValue);
  }

  private void appendOneArgument(MethodInvocation argumentOwner) {
    if (!argumentOwner.getVariableArgumentsProperty().isEmpty() || !argumentOwner.getKeyedArgumentsProperty().isEmpty() || argumentOwner.getRequiredArgumentsProperty().size() != 1) {
      Logger.errln("Expected a single argument.", argumentOwner);
    }
    if (!argumentOwner.getRequiredArgumentsProperty().isEmpty()) {
      final String methodName = argumentOwner.method.getValue().getName();
      Map<String, String> wrappedParams = TweedleEncoderData.optionalParamsToWrap.containsKey(methodName) ? TweedleEncoderData.optionalParamsToWrap : null;
      appendWrappedArg(argumentOwner.getRequiredArgumentsProperty().get(0), methodName, wrappedParams);
    }
  }

  void processArgument(AbstractParameter parameter, AbstractArgument argument) {
    final String parameterLabel = getParameterLabel(parameter);
    encoder.forwardAppendString(parameterLabel);
    encoder.forwardAppendString(": ");
    final Code parameterCode = parameter.getCode();
    Map<String, String> wrappedParams = parameterCode == null
        ? null
        : TweedleEncoderData.methodsWithWrappedArgs.get(parameterCode.getName());
    appendWrappedArg(argument, parameterLabel, wrappedParams);
  }

  private void appendWrappedArg(ProcessableNode argument, String parameterLabel, Map<String, String> wrappedParams) {
    String argStart = wrappedParams == null ? null : wrappedParams.get(parameterLabel);
    if (argStart != null) {
      encoder.forwardAppendString(argStart);
    }
    argument.process(encoder);
    if (argStart != null) {
      encoder.forwardAppendString(")");
    }
  }

  private String getParameterLabel(AbstractParameter parameter) {
    if (parameter instanceof JavaConstructorParameter constructorParameter) {
      String className = constructorParameter.getCode().getDeclaringType().getName();
      Map<String, String> paramLabelMap = TweedleEncoderData.constructorsWithRelabeledParams.get(className);
      if (paramLabelMap != null) {
        final String newLabel = paramLabelMap.get(parameter.getName());
        if (newLabel != null) {
          return newLabel;
        }
      }
    }
    String label = encoder.forwardIdentifierName(parameter);
    if (null != label) {
      return TweedleEncoderData.methodParamsToRelabel.getOrDefault(label, label);
    }
    if (parameter instanceof JavaMethodParameter methodParameter) {
      final String methodName = parameter.getCode().getName();
      if (TweedleEncoderData.methodsMissingParameterNames.containsKey(methodName)) {
        String[] paramNames = TweedleEncoderData.methodsMissingParameterNames.get(methodName);
        int i = parameterIndex(methodParameter);
        return paramNames[i];
      }
    }
    if (parameter instanceof JavaConstructorParameter) {
      String javaType = parameter.getCode().getDeclaringType().getName();
      if ("Double".equals(javaType)) {
        return "wholeNumber";
      }
    }
    final String paramType = parameter.getValueType().getName().toLowerCase();
    final String message = "Unable to read label from parameter on method: %s\nUsing the type as label: %s\nGenerated code may contain errors.".formatted(parameter.getCode().toString(), paramType);
    //TODO I18n
    // Reusable serialization code must not open Swing dialogs directly (see
    // docs/concepts/ui-prompt-boundary.md). Route through the UI prompt boundary
    // so the desktop IDE still surfaces the warning while headless/library
    // callers (corpus harness, batch export) stay silent instead of throwing
    // HeadlessException.
    UiPrompts.showMessage(new MessagePromptRequest(MessageSeverity.ERROR, "Unlabeled parameter", message));
    Logger.errln(message);
    return paramType;
  }

  private int parameterIndex(JavaMethodParameter parameter) {
    AbstractParameter[] parameters = parameter.getCode().getAllParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameter == parameters[i]) {
        return i;
      }
    }
    return -1;
  }
}
