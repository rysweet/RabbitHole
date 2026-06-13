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

import edu.cmu.cs.dennisc.java.lang.ArrayUtilities;
import edu.cmu.cs.dennisc.java.lang.IterableUtilities;
import org.lgna.common.EachInTogetherRunnable;
import org.lgna.common.ThreadUtilities;
import org.lgna.project.ast.*;
import org.lgna.project.virtualmachine.events.*;

/**
 * Package-private delegate that owns all statement execution methods
 * extracted from {@link VirtualMachine}.
 *
 * @see VirtualMachine
 * @see VmExpressionEvaluator
 */
final class VmStatementExecutor {

  private final VirtualMachine vm;

  VmStatementExecutor(VirtualMachine vm) {
    this.vm = vm;
  }

  void execute(Statement statement) throws ReturnException {
    if (vm.isStopped) {
      return;
    }
    assert statement != null : vm;
    if (statement.isEnabled.getValue()) {
      StatementExecutionEvent statementEvent;
      VirtualMachineListener[] listeners;
      if (!vm.virtualMachineListeners.isEmpty()) {
        statementEvent = new StatementExecutionEvent(vm, statement);
        listeners = ArrayUtilities.createArray(vm.virtualMachineListeners, VirtualMachineListener.class);
      } else {
        statementEvent = null;
        listeners = null;
      }
      if ((statementEvent != null) && (listeners != null)) {
        for (VirtualMachineListener virtualMachineListener : listeners) {
          virtualMachineListener.statementExecuting(statementEvent);
        }
      }

      try {
        switch (statement) {
          case BlockStatement blockStatement -> executeBlockStatement(blockStatement, listeners);
          case ConditionalStatement conditional -> executeConditionalStatement(conditional, listeners);
          case Comment comment -> executeComment(comment, listeners);
          case CountLoop countLoop -> executeCountLoop(countLoop, listeners);
          case DoTogether doTogether -> executeDoTogether(doTogether, listeners);
          case DoInOrder order -> executeDoInOrder(order, listeners);
          case ExpressionStatement exp -> executeExpressionStatement(exp, listeners);
          case ForEachInArrayLoop iterableArray -> executeForEachInArrayLoop(iterableArray, listeners);
          case ForEachInIterableLoop iterableEach -> executeForEachInIterableLoop(iterableEach, listeners);
          case EachInArrayTogether arrayTogether -> executeEachInArrayTogether(arrayTogether, listeners);
          case EachInIterableTogether iterableTogetherTogether ->
              executeEachInIterableTogether(iterableTogetherTogether, listeners);
          case WhileLoop loop -> executeWhileLoop(loop, listeners);
          case LocalDeclarationStatement declarationStatement ->
              executeLocalDeclarationStatement(declarationStatement, listeners);
          case ReturnStatement returnStatement -> executeReturnStatement(returnStatement, listeners);

          // note: does not return.  throws ReturnException.
          default -> throw new RuntimeException();
        }
      } finally {
        if ((statementEvent != null) && (listeners != null)) {
          for (VirtualMachineListener virtualMachineListener : listeners) {
            virtualMachineListener.statementExecuted(statementEvent);
          }
        }
      }
    }
  }

  private void executeBlockStatement(BlockStatement blockStatement, VirtualMachineListener[] listeners) throws ReturnException {
    for (int i = 0, n = blockStatement.statements.size(); i < n; i++) {
      vm.execute(blockStatement.statements.get(i));
    }
  }

  private void executeConditionalStatement(ConditionalStatement conditionalStatement, VirtualMachineListener[] listeners) throws ReturnException {
    for (BooleanExpressionBodyPair booleanExpressionBodyPair : conditionalStatement.booleanExpressionBodyPairs) {
      if (vm.expressionEvaluator.evaluateBoolean(booleanExpressionBodyPair.expression.getValue(), "if condition is null")) {
        vm.execute(booleanExpressionBodyPair.body.getValue());
        return;
      }
    }
    vm.execute(conditionalStatement.elseBody.getValue());
  }

  private void executeComment(Comment comment, VirtualMachineListener[] listeners) {
  }

  private void executeCountLoop(CountLoop countLoop, VirtualMachineListener[] listeners) throws ReturnException {
    UserLocal variable = countLoop.variable.getValue();
    UserLocal constant = countLoop.constant.getValue();
    vm.pushLocal(variable, -1);
    try {
      final int n = vm.expressionEvaluator.evaluateInt(countLoop.count.getValue(), "count expression is null");
      vm.pushLocal(constant, n);
      try {
        for (int i = 0; i < n; i++) {
          if (vm.isStopped) {
            return;
          }
          CountLoopIterationEvent countLoopIterationEvent;
          if (listeners != null) {
            countLoopIterationEvent = new CountLoopIterationEvent(vm, countLoop, i, n);
            for (VirtualMachineListener virtualMachineListener : listeners) {
              virtualMachineListener.countLoopIterating(countLoopIterationEvent);
            }
          } else {
            countLoopIterationEvent = null;
          }
          vm.setLocal(variable, i);
          vm.execute(countLoop.body.getValue());
          if (listeners != null) {
            for (VirtualMachineListener virtualMachineListener : listeners) {
              virtualMachineListener.countLoopIterated(countLoopIterationEvent);
            }
          }
        }
      } finally {
        vm.popLocal(constant);
      }
    } finally {
      vm.popLocal(variable);
    }
  }

  private void executeDoInOrder(DoInOrder doInOrder, VirtualMachineListener[] listeners) throws ReturnException {
    vm.execute(doInOrder.body.getValue());
  }

  private void executeDoTogether(DoTogether doTogether, VirtualMachineListener[] listeners) throws ReturnException {
    BlockStatement blockStatement = doTogether.body.getValue();
    //todo?
    switch (blockStatement.statements.size()) {
    case 0:
      break;
    case 1:
      vm.execute(blockStatement.statements.get(0));
      break;
    default:
      final Frame owner = vm.getFrameForThread(Thread.currentThread());
      Runnable[] runnables = new Runnable[blockStatement.statements.size()];
      for (int i = 0; i < runnables.length; i++) {
        final Statement statementI = blockStatement.statements.get(i);
        runnables[i] = new Runnable() {
          @Override
          public void run() {
            vm.pushCurrentThread(owner);
            try {
              vm.execute(statementI);
            } catch (ReturnException re) {
              //todo
            } finally {
              vm.popCurrentThread();
            }
          }
        };
      }
      ThreadUtilities.doTogether(runnables);
    }
  }

  private void executeExpressionStatement(ExpressionStatement expressionStatement, VirtualMachineListener[] listeners) {
    try {
      @SuppressWarnings("unused") Object unused = vm.evaluate(expressionStatement.expression.getValue());
    } catch (LgnaVmMethodInvocationException e) {
      vm.handleMethodInvocationException(e);
    }
  }

  void excecuteForEachLoop(AbstractForEachLoop forEachInLoop, Object[] array, VirtualMachineListener[] listeners) throws ReturnException {
    UserLocal item = forEachInLoop.item.getValue();
    BlockStatement blockStatement = forEachInLoop.body.getValue();
    vm.pushLocal(item, -1);
    try {
      int index = 0;
      for (Object o : array) {
        if (vm.isStopped) {
          return;
        }
        ForEachLoopIterationEvent forEachLoopIterationEvent;
        if (listeners != null) {
          forEachLoopIterationEvent = new ForEachLoopIterationEvent(vm, forEachInLoop, o, array, index);
          for (VirtualMachineListener virtualMachineListener : listeners) {
            virtualMachineListener.forEachLoopIterating(forEachLoopIterationEvent);
          }
        } else {
          forEachLoopIterationEvent = null;
        }

        vm.setLocal(item, o);
        vm.execute(blockStatement);
        if (listeners != null) {
          for (VirtualMachineListener virtualMachineListener : listeners) {
            virtualMachineListener.forEachLoopIterated(forEachLoopIterationEvent);
          }
        }
        index++;
      }
    } finally {
      vm.popLocal(item);
    }
  }

  private void executeForEachInArrayLoop(ForEachInArrayLoop forEachInArrayLoop, VirtualMachineListener[] listeners) throws ReturnException {
    Object[] array = vm.expressionEvaluator.evaluate(forEachInArrayLoop.array.getValue(), Object[].class);
    vm.checkNotNull(array, "for each array is null");
    excecuteForEachLoop(forEachInArrayLoop, array, listeners);
  }

  private void executeForEachInIterableLoop(ForEachInIterableLoop forEachInIterableLoop, VirtualMachineListener[] listeners) throws ReturnException {
    Iterable<?> iterable = vm.expressionEvaluator.evaluate(forEachInIterableLoop.iterable.getValue(), Iterable.class);
    vm.checkNotNull(iterable, "for each iterable is null");
    excecuteForEachLoop(forEachInIterableLoop, IterableUtilities.toArray(iterable), listeners);
  }

  void excecuteEachInTogether(final AbstractEachInTogether eachInTogether, final Object[] array, final VirtualMachineListener[] listeners) throws ReturnException {
    final UserLocal item = eachInTogether.item.getValue();
    final BlockStatement blockStatement = eachInTogether.body.getValue();

    switch (array.length) {
    case 0:
      break;
    case 1:
      Object value = array[0];
      vm.pushLocal(item, value);
      try {
        EachInTogetherItemEvent eachInTogetherEvent;
        if (listeners != null) {
          eachInTogetherEvent = new EachInTogetherItemEvent(vm, eachInTogether, value, array);
          for (VirtualMachineListener virtualMachineListener : listeners) {
            virtualMachineListener.eachInTogetherItemExecuting(eachInTogetherEvent);
          }
        } else {
          eachInTogetherEvent = null;
        }
        vm.execute(blockStatement);
        if (listeners != null) {
          for (VirtualMachineListener virtualMachineListener : listeners) {
            virtualMachineListener.eachInTogetherItemExecuted(eachInTogetherEvent);
          }
        }
      } finally {
        vm.popLocal(item);
      }
      break;
    default:
      final Frame owner = vm.getFrameForThread(Thread.currentThread());
      ThreadUtilities.eachInTogether(new EachInTogetherRunnable<Object>() {
        @Override
        public void run(Object value) {
          vm.pushCurrentThread(owner);
          try {
            vm.pushLocal(item, value);
            try {
              EachInTogetherItemEvent eachInTogetherEvent;
              if (listeners != null) {
                eachInTogetherEvent = new EachInTogetherItemEvent(vm, eachInTogether, value, array);
                for (VirtualMachineListener virtualMachineListener : listeners) {
                  virtualMachineListener.eachInTogetherItemExecuting(eachInTogetherEvent);
                }
              } else {
                eachInTogetherEvent = null;
              }
              vm.execute(blockStatement);
              if (listeners != null) {
                for (VirtualMachineListener virtualMachineListener : listeners) {
                  virtualMachineListener.eachInTogetherItemExecuted(eachInTogetherEvent);
                }
              }
            } catch (ReturnException re) {
              //todo
            } finally {
              vm.popLocal(item);
            }
          } finally {
            vm.popCurrentThread();
          }
        }
      }, array);
    }
  }

  private void executeEachInArrayTogether(EachInArrayTogether eachInArrayTogether, VirtualMachineListener[] listeners) throws ReturnException {
    Object[] array = vm.expressionEvaluator.evaluate(eachInArrayTogether.array.getValue(), Object[].class);
    vm.checkNotNull(array, "each in together array is null");
    excecuteEachInTogether(eachInArrayTogether, array, listeners);
  }

  private void executeEachInIterableTogether(EachInIterableTogether eachInIterableTogether, VirtualMachineListener[] listeners) throws ReturnException {
    Iterable<?> iterable = vm.expressionEvaluator.evaluate(eachInIterableTogether.iterable.getValue(), Iterable.class);
    vm.checkNotNull(iterable, "each in together iterable is null");
    excecuteEachInTogether(eachInIterableTogether, IterableUtilities.toArray(iterable), listeners);
  }

  private void executeReturnStatement(ReturnStatement returnStatement, VirtualMachineListener[] listeners) throws ReturnException {
    Object returnValue = vm.evaluate(returnStatement.expression.getValue());
    throw new ReturnException(returnValue);
  }

  private void executeWhileLoop(WhileLoop whileLoop, VirtualMachineListener[] listeners) throws ReturnException {
    int i = 0;
    while (!vm.isStopped && vm.expressionEvaluator.evaluateBoolean(whileLoop.conditional.getValue(), "while condition is null")) {
      WhileLoopIterationEvent whileLoopIterationEvent;
      if (listeners != null) {
        whileLoopIterationEvent = new WhileLoopIterationEvent(vm, whileLoop, i);
        for (VirtualMachineListener virtualMachineListener : listeners) {
          virtualMachineListener.whileLoopIterating(whileLoopIterationEvent);
        }
      } else {
        whileLoopIterationEvent = null;
      }
      vm.execute(whileLoop.body.getValue());
      if (listeners != null) {
        for (VirtualMachineListener virtualMachineListener : listeners) {
          virtualMachineListener.whileLoopIterated(whileLoopIterationEvent);
        }
      }
      i++;
    }
  }

  private void executeLocalDeclarationStatement(LocalDeclarationStatement localDeclarationStatement, VirtualMachineListener[] listeners) {
    Object value;
    try {
      value = vm.evaluate(localDeclarationStatement.initializer.getValue());
    } catch (LgnaVmMethodInvocationException e) {
      value = vm.handleMethodInvocationException(e);
    }
    vm.pushLocal(localDeclarationStatement.local.getValue(), value);
    //handle pop on exit of owning block statement
  }
}
