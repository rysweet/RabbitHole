package org.lgna.project.virtualmachine;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.UserMethod;
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

/**
 * Headless scope: verifies virtual machine listener dispatch for a small story method body only;
 * it does not start JavaFX, load gallery assets, or render a scene.
 */
public class VirtualMachineHeadlessRuntimeEventTest {

  @Test
  public void headlessStaticStoryMethodNotifiesListenerAroundBlockAndCommentStatements() {
    ReleaseVirtualMachine virtualMachine = new ReleaseVirtualMachine();
    RecordingVirtualMachineListener listener = new RecordingVirtualMachineListener();
    virtualMachine.addVirtualMachineListener(listener);

    Comment runtimeStep = new Comment("headless runtime step");
    UserMethod storyMethod = new UserMethod(
        "runHeadlessStoryStep",
        Void.TYPE,
        new org.lgna.project.ast.UserParameter[0],
        new BlockStatement(runtimeStep));
    storyMethod.isStatic.setValue(true);

    virtualMachine.ENTRY_POINT_invoke(null, storyMethod);

    assertEquals(
        Arrays.asList(
            "executing:BlockStatement",
            "executing:Comment",
            "executed:Comment",
            "executed:BlockStatement"),
        listener.statementEvents);

    virtualMachine.removeVirtualMachineListener(listener);
    virtualMachine.ENTRY_POINT_invoke(null, storyMethod);

    assertEquals(
        Arrays.asList(
            "executing:BlockStatement",
            "executing:Comment",
            "executed:Comment",
            "executed:BlockStatement"),
        listener.statementEvents);
  }

  private static class RecordingVirtualMachineListener implements VirtualMachineListener {
    private final List<String> statementEvents = new ArrayList<>();

    @Override
    public void statementExecuting(StatementExecutionEvent statementExecutionEvent) {
      statementEvents.add("executing:" + statementExecutionEvent.getStatement().getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent statementExecutionEvent) {
      statementEvents.add("executed:" + statementExecutionEvent.getStatement().getClass().getSimpleName());
    }

    @Override
    public void whileLoopIterating(WhileLoopIterationEvent whileLoopIterationEvent) {
    }

    @Override
    public void whileLoopIterated(WhileLoopIterationEvent whileLoopIterationEvent) {
    }

    @Override
    public void countLoopIterating(CountLoopIterationEvent countLoopIterationEvent) {
    }

    @Override
    public void countLoopIterated(CountLoopIterationEvent countLoopIterationEvent) {
    }

    @Override
    public void forEachLoopIterating(ForEachLoopIterationEvent forEachLoopIterationEvent) {
    }

    @Override
    public void forEachLoopIterated(ForEachLoopIterationEvent forEachLoopIterationEvent) {
    }

    @Override
    public void eachInTogetherItemExecuting(EachInTogetherItemEvent eachInTogetherItemEvent) {
    }

    @Override
    public void eachInTogetherItemExecuted(EachInTogetherItemEvent eachInTogetherItemEvent) {
    }

    @Override
    public void expressionEvaluated(ExpressionEvaluationEvent expressionEvaluationEvent) {
    }
  }
}
