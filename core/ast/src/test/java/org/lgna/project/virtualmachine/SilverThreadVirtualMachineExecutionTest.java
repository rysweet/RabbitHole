package org.lgna.project.virtualmachine;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread test proving the VM recursive method-invocation execution path
 * with listener events. Covers the Run step of the student journey (issue #491).
 *
 * <p>Headless scope: no JavaFX, no gallery assets, no scene graph, no display.
 */
public class SilverThreadVirtualMachineExecutionTest {

  // --- Core silver-thread tests ---

  @Test
  public void vmExecutesMethodInvocationAndFiresExpectedEvents() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    UserMethod helperMethod = createStaticMethod("helperMethod",
        new BlockStatement(new Comment("helper executed")));
    type.methods.add(helperMethod);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), helperMethod);
    ExpressionStatement callStatement = new ExpressionStatement(call);
    UserMethod entryMethod = createStaticMethod("entryMethod",
        new BlockStatement(callStatement));
    type.methods.add(entryMethod);

    vm.ENTRY_POINT_invoke(null, entryMethod);

    List<String> expected = Arrays.asList(
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executing:BlockStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement",
        "executed:ExpressionStatement",
        "executed:BlockStatement");

    assertEquals("VM should fire 8 statement events for recursive method invocation",
        expected, listener.statementEvents);
  }

  @Test
  public void vmExecutionCompletesWithoutExceptions() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();

    NamedUserType type = createProgramType();

    UserMethod helperMethod = createStaticMethod("helperMethod",
        new BlockStatement(new Comment("helper executed")));
    type.methods.add(helperMethod);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), helperMethod);
    ExpressionStatement callStatement = new ExpressionStatement(call);
    UserMethod entryMethod = createStaticMethod("entryMethod",
        new BlockStatement(callStatement));
    type.methods.add(entryMethod);

    // Must not throw
    vm.ENTRY_POINT_invoke(null, entryMethod);
  }

  // --- Edge case tests ---

  @Test
  public void vmExecutesEmptyHelperBodyAndFiresSixEvents() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    UserMethod helperMethod = createStaticMethod("emptyHelper",
        new BlockStatement());
    type.methods.add(helperMethod);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), helperMethod);
    ExpressionStatement callStatement = new ExpressionStatement(call);
    UserMethod entryMethod = createStaticMethod("entryMethod",
        new BlockStatement(callStatement));
    type.methods.add(entryMethod);

    vm.ENTRY_POINT_invoke(null, entryMethod);

    List<String> expected = Arrays.asList(
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executing:BlockStatement",
        "executed:BlockStatement",
        "executed:ExpressionStatement",
        "executed:BlockStatement");

    assertEquals("Empty helper body should produce 6 events (no Comment pair)",
        expected, listener.statementEvents);
  }

  @Test
  public void vmExecutesMultipleSequentialCallsInOneBody() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    UserMethod helperA = createStaticMethod("helperA",
        new BlockStatement(new Comment("A")));
    UserMethod helperB = createStaticMethod("helperB",
        new BlockStatement(new Comment("B")));
    type.methods.add(helperA);
    type.methods.add(helperB);

    ExpressionStatement callA = new ExpressionStatement(
        new MethodInvocation(new NullLiteral(), helperA));
    ExpressionStatement callB = new ExpressionStatement(
        new MethodInvocation(new NullLiteral(), helperB));
    UserMethod entryMethod = createStaticMethod("entryMethod",
        new BlockStatement(callA, callB));
    type.methods.add(entryMethod);

    vm.ENTRY_POINT_invoke(null, entryMethod);

    // Outer Block + ExprStmtA + helperA(Block+Comment) + ExprStmtB + helperB(Block+Comment)
    List<String> expected = Arrays.asList(
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executing:BlockStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement",
        "executed:ExpressionStatement",
        "executing:ExpressionStatement",
        "executing:BlockStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement",
        "executed:ExpressionStatement",
        "executed:BlockStatement");

    assertEquals("Two sequential calls should produce 14 events",
        expected, listener.statementEvents);
  }

  @Test
  public void vmExecutesChainedCallsThreeLevelsDeep() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    // Level 3: leaf method with Comment
    UserMethod leaf = createStaticMethod("leaf",
        new BlockStatement(new Comment("leaf")));
    type.methods.add(leaf);

    // Level 2: middle calls leaf
    UserMethod middle = createStaticMethod("middle",
        new BlockStatement(new ExpressionStatement(
            new MethodInvocation(new NullLiteral(), leaf))));
    type.methods.add(middle);

    // Level 1: entry calls middle
    UserMethod entry = createStaticMethod("entry",
        new BlockStatement(new ExpressionStatement(
            new MethodInvocation(new NullLiteral(), middle))));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);

    // entry Block, ExprStmt, middle Block, ExprStmt, leaf Block, Comment — then unwind
    List<String> expected = Arrays.asList(
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executing:BlockStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement",
        "executed:ExpressionStatement",
        "executed:BlockStatement",
        "executed:ExpressionStatement",
        "executed:BlockStatement");

    assertEquals("Three-level call chain should produce 12 events",
        expected, listener.statementEvents);
  }

  // --- isValid guard edge case ---

  @Test
  public void vmSkipsInvalidMethodInvocationButStillFiresExpressionStatementEvents() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    // Helper NOT added to type → isValid() returns false (getDeclaringType() == null)
    UserMethod orphanHelper = createStaticMethod("orphanHelper",
        new BlockStatement(new Comment("should not execute")));
    // Deliberately NOT adding orphanHelper to type

    assertFalse("Orphan method should be invalid",
        orphanHelper.isValid());

    MethodInvocation invalidCall = new MethodInvocation(new NullLiteral(), orphanHelper);
    assertFalse("MethodInvocation targeting invalid method should itself be invalid",
        invalidCall.isValid());

    ExpressionStatement callStatement = new ExpressionStatement(invalidCall);
    UserMethod entryMethod = createStaticMethod("entryMethod",
        new BlockStatement(callStatement));
    type.methods.add(entryMethod);

    // Should not throw — VM logs severe and skips
    vm.ENTRY_POINT_invoke(null, entryMethod);

    // Only the entry body events fire; helper body is never entered
    List<String> expected = Arrays.asList(
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executed:ExpressionStatement",
        "executed:BlockStatement");

    assertEquals("Invalid method invocation should be skipped, yielding only 4 events",
        expected, listener.statementEvents);
  }

  // --- Listener lifecycle tests ---

  @Test
  public void removedListenerReceivesNoFurtherEvents() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();
    UserMethod helperMethod = createStaticMethod("helperMethod",
        new BlockStatement(new Comment("helper")));
    type.methods.add(helperMethod);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), helperMethod);
    UserMethod entryMethod = createStaticMethod("entryMethod",
        new BlockStatement(new ExpressionStatement(call)));
    type.methods.add(entryMethod);

    // First execution: listener records events
    vm.ENTRY_POINT_invoke(null, entryMethod);
    int firstRunCount = listener.statementEvents.size();
    assertEquals("First run should produce 8 events", 8, firstRunCount);

    // Remove listener and run again
    vm.removeVirtualMachineListener(listener);
    vm.ENTRY_POINT_invoke(null, entryMethod);

    assertEquals("After removal, no new events should be recorded",
        firstRunCount, listener.statementEvents.size());
  }

  @Test
  public void methodInvocationIsValidWhenMethodOwnedByType() {
    NamedUserType type = createProgramType();
    UserMethod helper = createStaticMethod("helper",
        new BlockStatement(new Comment("check")));

    assertFalse("Method not yet in type should be invalid", helper.isValid());

    type.methods.add(helper);
    assertTrue("Method in type should be valid", helper.isValid());

    MethodInvocation invocation = new MethodInvocation(new NullLiteral(), helper);
    assertTrue("MethodInvocation with valid method should be valid", invocation.isValid());
  }

  @Test
  public void expressionEvaluatedEventFirsForMethodInvocationExpression() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();
    UserMethod helper = createStaticMethod("helper",
        new BlockStatement(new Comment("expr-eval")));
    type.methods.add(helper);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), helper);
    UserMethod entry = createStaticMethod("entry",
        new BlockStatement(new ExpressionStatement(call)));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);

    // The VM evaluates the NullLiteral expression and fires expressionEvaluated
    assertTrue("At least one expressionEvaluated event should fire for the NullLiteral target",
        listener.expressionEvaluatedCount > 0);
  }

  // --- Helpers ---

  private static NamedUserType createProgramType() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("VMExecutionProgram");
    type.superType.setValue(JavaType.OBJECT_TYPE);
    return type;
  }

  private static UserMethod createStaticMethod(String name, BlockStatement body) {
    UserMethod method = new UserMethod(name, Void.TYPE, new UserParameter[0], body);
    method.isStatic.setValue(true);
    return method;
  }

  private static class RecordingVirtualMachineListener implements VirtualMachineListener {
    final List<String> statementEvents = new ArrayList<>();
    int expressionEvaluatedCount = 0;

    @Override
    public void statementExecuting(StatementExecutionEvent event) {
      statementEvents.add("executing:" + event.getStatement().getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent event) {
      statementEvents.add("executed:" + event.getStatement().getClass().getSimpleName());
    }

    @Override
    public void expressionEvaluated(ExpressionEvaluationEvent event) {
      expressionEvaluatedCount++;
    }

    @Override
    public void whileLoopIterating(WhileLoopIterationEvent e) {}

    @Override
    public void whileLoopIterated(WhileLoopIterationEvent e) {}

    @Override
    public void countLoopIterating(CountLoopIterationEvent e) {}

    @Override
    public void countLoopIterated(CountLoopIterationEvent e) {}

    @Override
    public void forEachLoopIterating(ForEachLoopIterationEvent e) {}

    @Override
    public void forEachLoopIterated(ForEachLoopIterationEvent e) {}

    @Override
    public void eachInTogetherItemExecuting(EachInTogetherItemEvent e) {}

    @Override
    public void eachInTogetherItemExecuted(EachInTogetherItemEvent e) {}
  }
}
