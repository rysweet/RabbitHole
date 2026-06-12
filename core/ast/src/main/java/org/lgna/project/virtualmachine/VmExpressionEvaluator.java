/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.lgna.project.virtualmachine;

import edu.cmu.cs.dennisc.java.util.Maps;
import edu.cmu.cs.dennisc.java.util.logging.Logger;
import edu.cmu.cs.dennisc.print.PrintUtilities;
import org.lgna.project.ast.*;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

/**
 * Package-private delegate that owns all expression evaluation methods
 * extracted from {@link VirtualMachine}.
 *
 * @see VirtualMachine
 * @see VmStatementExecutor
 */
final class VmExpressionEvaluator {

  private final VirtualMachine vm;
  private final VmMethodInvocationEvaluator methodInvocationEvaluator;

  VmExpressionEvaluator(VirtualMachine vm) {
    this.vm = vm;
    this.methodInvocationEvaluator = new VmMethodInvocationEvaluator(vm, this);
  }

  Object evaluate(Expression expression) {
    if (expression == null) {
      throw new NullPointerException();
    }
    Object rv = switch (expression) {
      case AssignmentExpression assignmentExpression -> evaluateAssignmentExpression(assignmentExpression);
      case BooleanLiteral bool -> evaluateBooleanLiteral(bool);
      case InstanceCreation creation -> creation.evaluate(vm);
      case ArrayInstanceCreation arrayCreation -> evaluateArrayInstanceCreation(arrayCreation);
      case ArrayLength length -> evaluateArrayLength(length);
      case ArrayAccess array -> evaluateArrayAccess(array);
      case FieldAccess field -> evaluateFieldAccess(field);
      case LocalAccess local -> evaluateLocalAccess(local);
      case ArithmeticInfixExpression math -> evaluateArithmeticInfixExpression(math);
      case BitwiseInfixExpression bitwise -> evaluateBitwiseInfixExpression(bitwise);
      case ConditionalInfixExpression conditional -> evaluateConditionalInfixExpression(conditional);
      case RelationalInfixExpression relational -> evaluateRelationalInfixExpression(relational);
      case ShiftInfixExpression infixExpression -> evaluateShiftInfixExpression(infixExpression);
      case LogicalComplement complement -> evaluateLogicalComplement(complement);
      case MethodInvocation invocation -> methodInvocationEvaluator.evaluate(invocation);
      case NullLiteral nullLiteral -> evaluateNullLiteral(nullLiteral);
      case StringConcatenation concatenation -> evaluateStringConcatenation(concatenation);
      case DoubleLiteral doubleLiteral -> evaluateDoubleLiteral(doubleLiteral);
      case FloatLiteral floatLiteral -> evaluateFloatLiteral(floatLiteral);
      case IntegerLiteral integerLiteral -> evaluateIntegerLiteral(integerLiteral);
      case ParameterAccess access -> evaluateParameterAccess(access);
      case StringLiteral stringLiteral -> evaluateStringLiteral(stringLiteral);
      case ThisExpression thisExpression -> evaluateThisExpression(thisExpression);
      case TypeExpression typeExpression -> evaluateTypeExpression(typeExpression);
      case TypeLiteral typeLiteral -> evaluateTypeLiteral(typeLiteral);
      case ResourceExpression resourceExpression -> evaluateResourceExpression(resourceExpression);
      case LambdaExpression lambdaExpression -> evaluateLambdaExpression(lambdaExpression);
      default -> throw new RuntimeException(expression.getClass().getName());
    };
    if (!vm.virtualMachineListeners.isEmpty()) {
      ExpressionEvaluationEvent expressionEvaluationEvent = new ExpressionEvaluationEvent(vm, expression, rv);
      for (VirtualMachineListener virtualMachineListener : vm.virtualMachineListeners) {
        virtualMachineListener.expressionEvaluated(expressionEvaluationEvent);
      }
    }
    return rv;
  }

  final <E> E evaluate(Expression expression, Class<E> cls) {
    Object value = this.evaluate(expression);
    if (cls.isArray()) {
      if (value instanceof UserArrayInstance userArrayInstance) {
        //todo
        value = userArrayInstance.getValues();
      }
    }
    return cls.cast(value);
  }

  // Package-private: VmStatementExecutor accesses these via vm.expressionEvaluator
  boolean evaluateBoolean(Expression expression, String nullExceptionMessage) {
    Object value = this.evaluate(expression);
    vm.checkNotNull(value, nullExceptionMessage);
    if (value instanceof Boolean b) {
      return b;
    } else {
      throw new LgnaVmClassCastException(vm, Boolean.class, value.getClass());
    }
  }

  // Package-private: VmStatementExecutor accesses these via vm.expressionEvaluator
  int evaluateInt(Expression expression, String nullExceptionMessage) {
    Object value = this.evaluate(expression);
    vm.checkNotNull(value, nullExceptionMessage);
    if (value instanceof Integer integer) {
      return integer;
    } else {
      throw new LgnaVmClassCastException(vm, Integer.class, value.getClass());
    }
  }

  private Object evaluateArgument(AbstractArgument argument) {
    assert argument != null;
    Expression expression = argument.expression.getValue();
    assert expression != null;
    if (expression instanceof LambdaExpression lambdaExpression) {
      return this.EPIC_HACK_evaluateLambdaExpression(lambdaExpression, argument);
    } else {
      return this.evaluate(expression);
    }
  }

  Object[] evaluateArguments(AbstractCode code, NodeListProperty<SimpleArgument> arguments, NodeListProperty<SimpleArgument> variableArguments, NodeListProperty<JavaKeyedArgument> keyedArguments) {
    //todo: when variable length and keyed parameters are offered in the IDE (User) this code will need to be updated
    List<? extends AbstractParameter> requiredParameters = code.getRequiredParameters();
    AbstractParameter variableParameter = code.getVariableLengthParameter();
    AbstractParameter keyedParameter = code.getKeyedParameter();

    final int REQUIRED_N = arguments.size();
    assert requiredParameters.size() == REQUIRED_N : code.getName() + " " + requiredParameters.size() + " " + arguments.size();

    int length = REQUIRED_N;
    if (variableParameter != null) {
      length += 1;
    }
    if (keyedParameter != null) {
      length += 1;
    }
    Object[] rv = new Object[length];
    int rvIndex;
    for (rvIndex = 0; rvIndex < REQUIRED_N; rvIndex++) {
      rv[rvIndex] = this.evaluateArgument(arguments.get(rvIndex));
    }
    if (variableParameter != null) {
      final int VARIABLE_N = variableArguments.size();
      JavaType variableArrayType = variableParameter.getValueType().getFirstEncounteredJavaType();
      assert variableArrayType.isArray();
      Class<?> componentCls = variableArrayType.getComponentType().getClassReflectionProxy().getReification();
      Object array = Array.newInstance(componentCls, VARIABLE_N);
      for (int i = 0; i < VARIABLE_N; i++) {
        //todo: support primitive types
        Array.set(array, i, this.evaluateArgument(variableArguments.get(rvIndex)));
      }
      rv[rvIndex] = array;
    }
    if (keyedParameter != null) {
      final int KEYED_N = keyedArguments.size();
      JavaType keyedArrayType = keyedParameter.getValueType().getFirstEncounteredJavaType();
      assert keyedArrayType.isArray();
      Class<?> componentCls = keyedArrayType.getComponentType().getClassReflectionProxy().getReification();
      Object array = Array.newInstance(componentCls, KEYED_N);
      for (int i = 0; i < KEYED_N; i++) {
        //todo: support primitive types
        Array.set(array, i, this.evaluateArgument(keyedArguments.get(i)));
      }
      rv[rvIndex] = array;
    }
    return rv;
  }

  private Object evaluateAssignmentExpression(AssignmentExpression assignmentExpression) {
    Expression leftHandExpression = assignmentExpression.leftHandSide.getValue();
    Expression rightHandExpression = assignmentExpression.rightHandSide.getValue();
    Object rightHandValue = this.evaluate(rightHandExpression);
    if (assignmentExpression.operator.getValue() == AssignmentExpression.Operator.ASSIGN) {
      if (leftHandExpression instanceof FieldAccess fieldAccess) {
        vm.set(fieldAccess.field.getValue(), this.evaluate(fieldAccess.expression.getValue()), rightHandValue);
      } else if (leftHandExpression instanceof LocalAccess localAccess) {
        vm.setLocal(localAccess.local.getValue(), rightHandValue);
      } else if (leftHandExpression instanceof ArrayAccess arrayAccess) {
        vm.setItemAtIndex(arrayAccess.arrayType.getValue(), this.evaluate(arrayAccess.array.getValue()), this.evaluateInt(arrayAccess.index.getValue(), "array index is null"), rightHandValue);
      } else {
        PrintUtilities.println("todo: evaluateActual", assignmentExpression.leftHandSide.getValue(), rightHandValue);
      }
    } else {
      PrintUtilities.println("todo: evaluateActual", assignmentExpression);
    }
    return null;
  }

  private Object evaluateBooleanLiteral(BooleanLiteral booleanLiteral) {
    return booleanLiteral.value.getValue();
  }

  private Object evaluateArrayInstanceCreation(ArrayInstanceCreation arrayInstanceCreation) {
    Object[] values = new Object[arrayInstanceCreation.expressions.size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = this.evaluate(arrayInstanceCreation.expressions.get(i));
    }
    int[] lengths = new int[arrayInstanceCreation.lengths.size()];
    for (int i = 0; i < lengths.length; i++) {
      lengths[i] = arrayInstanceCreation.lengths.get(i);
    }
    return vm.createArrayInstance(arrayInstanceCreation.arrayType.getValue(), lengths, values);
  }

  private Object evaluateArrayAccess(ArrayAccess arrayAccess) {
    return vm.getItemAtIndex(arrayAccess.arrayType.getValue(), this.evaluate(arrayAccess.array.getValue()), this.evaluateInt(arrayAccess.index.getValue(), "array index is null"));
  }

  private Integer evaluateArrayLength(ArrayLength arrayLength) {
    return vm.getArrayLength(this.evaluate(arrayLength.array.getValue()));
  }

  private Object evaluateFieldAccess(FieldAccess fieldAccess) {
    Object o = fieldAccess.field.getValue();
    if (o instanceof AbstractField field) {
      Object value = this.evaluate(fieldAccess.expression.getValue());
      return vm.get(field, value);
    } else {
      Logger.errln("field access field is not a field", o);
      Node node = fieldAccess;
      while (node != null) {
        Logger.errln("   ", node);
        node = node.getParent();
      }
      return null;
    }
  }

  private Object evaluateLocalAccess(LocalAccess localAccess) {
    return vm.getLocal(localAccess.local.getValue());
  }

  private Object evaluateArithmeticInfixExpression(ArithmeticInfixExpression arithmeticInfixExpression) {
    Number leftOperand = (Number) this.evaluate(arithmeticInfixExpression.leftOperand.getValue());
    Number rightOperand = (Number) this.evaluate(arithmeticInfixExpression.rightOperand.getValue());
    return arithmeticInfixExpression.operator.getValue().operate(leftOperand, rightOperand);
  }

  private Object evaluateBitwiseInfixExpression(BitwiseInfixExpression bitwiseInfixExpression) {
    Object leftOperand = this.evaluate(bitwiseInfixExpression.leftOperand.getValue());
    Object rightOperand = this.evaluate(bitwiseInfixExpression.rightOperand.getValue());
    return bitwiseInfixExpression.operator.getValue().operate(leftOperand, rightOperand);
  }

  private Boolean evaluateConditionalInfixExpression(ConditionalInfixExpression conditionalInfixExpression) {
    ConditionalInfixExpression.Operator operator = conditionalInfixExpression.operator.getValue();
    Boolean leftOperand = (Boolean) this.evaluate(conditionalInfixExpression.leftOperand.getValue());
    if (operator == ConditionalInfixExpression.Operator.AND) {
      if (leftOperand) {
        return (Boolean) this.evaluate(conditionalInfixExpression.rightOperand.getValue());
      } else {
        return false;
      }
    } else if (operator == ConditionalInfixExpression.Operator.OR) {
      if (leftOperand) {
        return true;
      } else {
        return (Boolean) this.evaluate(conditionalInfixExpression.rightOperand.getValue());
      }
    } else {

      return false;
    }
  }

  private Boolean evaluateRelationalInfixExpression(RelationalInfixExpression relationalInfixExpression) {
    Object leftOperand = UserInstance.getJavaInstanceIfNecessary(this.evaluate(relationalInfixExpression.leftOperand.getValue()));
    Object rightOperand = UserInstance.getJavaInstanceIfNecessary(this.evaluate(relationalInfixExpression.rightOperand.getValue()));
    if (leftOperand != null) {
      if (rightOperand != null) {
        return relationalInfixExpression.operator.getValue().operate(leftOperand, rightOperand);
      } else {
        throw new LgnaVmNullPointerException("right operand is null.", vm);
      }
    } else {
      if (rightOperand != null) {
        throw new LgnaVmNullPointerException("left operand is null.", vm);
      } else {
        throw new LgnaVmNullPointerException("left and right operands are both null.", vm);
      }
    }
  }

  private Object evaluateShiftInfixExpression(ShiftInfixExpression shiftInfixExpression) {
    Object leftOperand = this.evaluate(shiftInfixExpression.leftOperand.getValue());
    Object rightOperand = this.evaluate(shiftInfixExpression.rightOperand.getValue());
    return shiftInfixExpression.operator.getValue().operate(leftOperand, rightOperand);
  }

  private Object evaluateLogicalComplement(LogicalComplement logicalComplement) {
    Boolean operand = this.evaluateBoolean(logicalComplement.operand.getValue(), "logical complement expression is null");
    return !operand;
  }

  private String evaluateStringConcatenation(StringConcatenation stringConcatenation) {
    Object leftOperand = this.evaluate(stringConcatenation.leftOperand.getValue());
    Object rightOperand = this.evaluate(stringConcatenation.rightOperand.getValue());
    return String.valueOf(leftOperand).concat(String.valueOf(rightOperand));
  }

  private Object evaluateNullLiteral(NullLiteral nullLiteral) {
    return null;
  }

  private Object evaluateDoubleLiteral(DoubleLiteral doubleLiteral) {
    return doubleLiteral.value.getValue();
  }

  private Object evaluateFloatLiteral(FloatLiteral floatLiteral) {
    return floatLiteral.value.getValue();
  }

  private Object evaluateIntegerLiteral(IntegerLiteral integerLiteral) {
    return integerLiteral.value.getValue();
  }

  private Object evaluateParameterAccess(ParameterAccess parameterAccess) {
    return vm.lookup(parameterAccess.parameter.getValue());
  }

  private Object evaluateStringLiteral(StringLiteral stringLiteral) {
    return stringLiteral.value.getValue();
  }

  private Object evaluateThisExpression(ThisExpression thisExpression) {
    Object rv = vm.getThis();
    assert rv != null;
    return rv;
  }

  private Object evaluateTypeExpression(TypeExpression typeExpression) {
    return typeExpression.value.getValue();
  }

  private Object evaluateTypeLiteral(TypeLiteral typeLiteral) {
    return typeLiteral.value.getValue();
  }

  private Object evaluateResourceExpression(ResourceExpression resourceExpression) {
    return resourceExpression.resource.getValue();
  }

  Object EPIC_HACK_evaluateLambdaExpression(LambdaExpression lambdaExpression, AbstractArgument argument) {
    Lambda lambda = lambdaExpression.value.getValue();

    AbstractType<?, ?, ?> type = argument.parameter.getValue().getValueType();
    if (type instanceof JavaType javaType) {

      UserInstance thisInstance = vm.getThis();
      assert thisInstance != null;
      Class<?> interfaceCls = javaType.getClassReflectionProxy().getReification();
      Class<?> adapterCls = vm.mapAbstractClsToAdapterCls.get(interfaceCls);
      assert adapterCls != null : interfaceCls;
      Class<?>[] parameterTypes = {LambdaContext.class, Lambda.class, UserInstance.class};
      Object[] arguments = {new LambdaContext() {
        @Override
        public void invokeEntryPoint(Lambda lambda, AbstractMethod singleAbstractMethod, UserInstance thisInstance, Object... arguments) {
          assert thisInstance != null;
          if (lambda instanceof UserLambda userLambda) {
            Map<AbstractParameter, Object> map = Maps.newHashMap();
            for (int i = 0; i < arguments.length; i++) {
              map.put(userLambda.requiredParameters.get(i), arguments[i]);
            }
            vm.pushLambdaFrame(thisInstance, userLambda, singleAbstractMethod, map);
            try {
              vm.execute(userLambda.body.getValue());
            } catch (ReturnException re) {
              Logger.todo("handle return");
              assert false : re;
            } finally {
              vm.popFrame();
            }
          }
        }
      }, lambda, thisInstance};
      try {
        Constructor<?> cnstrctr = adapterCls.getDeclaredConstructor(parameterTypes);
        return cnstrctr.newInstance(arguments);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("todo");
    }
  }

  private Object evaluateLambdaExpression(LambdaExpression lambdaExpression) {
    throw new RuntimeException("todo");
  }
}
