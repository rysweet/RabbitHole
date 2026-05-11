package org.lgna.project.virtualmachine;

import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.JavaConstructor;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.virtualmachine.events.CountLoopIterationEvent;
import org.lgna.project.virtualmachine.events.EachInTogetherItemEvent;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.ForEachLoopIterationEvent;
import org.lgna.project.virtualmachine.events.StatementExecutionEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;
import org.lgna.project.virtualmachine.events.WhileLoopIterationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared test utilities for VirtualMachine characterization tests.
 * Provides factory methods for AST construction and a recording listener.
 */
final class VmTestSupport {

  private VmTestSupport() {}

  /** Creates a NamedUserType backed by OBJECT_TYPE with no constructor chain. */
  static NamedUserType createProgramType(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.OBJECT_TYPE);
    return type;
  }

  /** Creates a NamedUserType backed by Object.class with a full constructor chain. */
  static NamedUserType createTypeWithConstructor(String name) {
    return createTypeWithJavaSuperclass(name, Object.class);
  }

  /** Creates a NamedUserType backed by the given Java superclass with a constructor chain. */
  static NamedUserType createTypeWithJavaSuperclass(String name, Class<?> superclass) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(superclass));

    JavaConstructor superConstructor = JavaConstructor.getInstance(superclass);
    SuperConstructorInvocationStatement superCall =
        new SuperConstructorInvocationStatement(superConstructor);
    ConstructorBlockStatement constructorBody = new ConstructorBlockStatement(superCall);
    NamedUserConstructor constructor =
        new NamedUserConstructor(new UserParameter[0], constructorBody);
    type.constructors.add(constructor);

    return type;
  }

  /** Creates a static void UserMethod (procedure). */
  static UserMethod createStaticProcedure(String name, BlockStatement body) {
    UserMethod method = new UserMethod(name, Void.TYPE, new UserParameter[0], body);
    method.isStatic.setValue(true);
    return method;
  }

  /**
   * VirtualMachineListener that records statement execution events and loop iteration counts.
   * Use the public fields to assert against expected behavior.
   */
  static class RecordingListener implements VirtualMachineListener {
    final List<String> statementEvents = new ArrayList<>();
    int expressionEvaluatedCount = 0;
    int countLoopIteratingCount = 0;
    int countLoopIteratedCount = 0;
    int whileLoopIteratingCount = 0;
    int forEachIteratingCount = 0;

    @Override
    public void statementExecuting(StatementExecutionEvent e) {
      statementEvents.add("executing:" + e.getStatement().getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent e) {
      statementEvents.add("executed:" + e.getStatement().getClass().getSimpleName());
    }

    @Override
    public void expressionEvaluated(ExpressionEvaluationEvent e) {
      expressionEvaluatedCount++;
    }

    @Override
    public void whileLoopIterating(WhileLoopIterationEvent e) { whileLoopIteratingCount++; }

    @Override
    public void whileLoopIterated(WhileLoopIterationEvent e) {}

    @Override
    public void countLoopIterating(CountLoopIterationEvent e) { countLoopIteratingCount++; }

    @Override
    public void countLoopIterated(CountLoopIterationEvent e) { countLoopIteratedCount++; }

    @Override
    public void forEachLoopIterating(ForEachLoopIterationEvent e) { forEachIteratingCount++; }

    @Override
    public void forEachLoopIterated(ForEachLoopIterationEvent e) {}

    @Override
    public void eachInTogetherItemExecuting(EachInTogetherItemEvent e) {}

    @Override
    public void eachInTogetherItemExecuted(EachInTogetherItemEvent e) {}
  }
}
