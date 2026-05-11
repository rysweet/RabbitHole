package org.alice.ide;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.CountLoop;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.Statement;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.virtualmachine.ReleaseVirtualMachine;
import org.lgna.project.virtualmachine.events.CountLoopIterationEvent;
import org.lgna.project.virtualmachine.events.EachInTogetherItemEvent;
import org.lgna.project.virtualmachine.events.ExpressionEvaluationEvent;
import org.lgna.project.virtualmachine.events.ForEachLoopIterationEvent;
import org.lgna.project.virtualmachine.events.StatementExecutionEvent;
import org.lgna.project.virtualmachine.events.VirtualMachineListener;
import org.lgna.project.virtualmachine.events.WhileLoopIterationEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Silver thread Test C: event listener dispatch fires once via the
 * {@link ReleaseVirtualMachine} headlessly. Simulates the student program
 * pattern where {@code initializeEventListeners} dispatches to
 * {@code myListener} which contains a {@link CountLoop}.
 *
 * <p>Uses pure {@link UserMethod}-only VM execution (no JavaMethod
 * reflection, no real SScene/SceneImp). The VM's
 * {@code ENTRY_POINT_invoke} drives the call chain and fires
 * {@link VirtualMachineListener} events for each statement and
 * count-loop iteration.
 *
 * <p>Headless: no JavaFX, no 3D rendering, no gallery assets, no display.
 *
 * @see SilverThreadStudentProgramSaveReadbackTest
 * @see SilverThreadStudentProgramCodegenTest
 */
public class SilverThreadStudentProgramEventDispatchTest {

  // ── Core: dispatch fires listener once with CountLoop iterations ─────

  @Test
  public void dispatchFiresListenerOnceWithThreeCountLoopIterations() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    // myListener: CountLoop(3) { Comment("iteration") }
    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(3));
    countLoop.body.getValue().statements.add(new Comment("iteration"));
    UserMethod myListener = createStaticMethod("myListener",
        new BlockStatement(countLoop));
    type.methods.add(myListener);

    // initializeEventListeners calls myListener once
    MethodInvocation call = new MethodInvocation(new NullLiteral(), myListener);
    ExpressionStatement callStmt = new ExpressionStatement(call);
    UserMethod initListeners = createStaticMethod("initializeEventListeners",
        new BlockStatement(callStmt));
    type.methods.add(initListeners);

    vm.ENTRY_POINT_invoke(null, initListeners);

    assertEquals("CountLoop must fire 3 countLoopIterating events",
        3, listener.countLoopIteratingCount);
    assertEquals("CountLoop must fire 3 countLoopIterated events",
        3, listener.countLoopIteratedCount);
  }

  @Test
  public void dispatchedMethodBodyExecutesExactlyOnce() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    // myListener: Comment("proof of single dispatch")
    UserMethod myListener = createStaticMethod("myListener",
        new BlockStatement(new Comment("proof of single dispatch")));
    type.methods.add(myListener);

    // initializeEventListeners calls myListener once
    MethodInvocation call = new MethodInvocation(new NullLiteral(), myListener);
    UserMethod initListeners = createStaticMethod("initializeEventListeners",
        new BlockStatement(new ExpressionStatement(call)));
    type.methods.add(initListeners);

    vm.ENTRY_POINT_invoke(null, initListeners);

    // Count how many times "Comment" was executed (myListener's body Comment)
    long commentExecutingCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment"))
        .count();
    assertEquals("Comment in myListener must execute exactly once",
        1, commentExecutingCount);
  }

  // ── CountLoop body executes correct number of times ──────────────────

  @Test
  public void countLoopBodyCommentExecutesThreeTimes() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(3));
    countLoop.body.getValue().statements.add(new Comment("loop body marker"));
    UserMethod entry = createStaticMethod("entry", new BlockStatement(countLoop));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment"))
        .count();
    assertEquals("Comment inside CountLoop(3) must execute 3 times",
        3, commentCount);
  }

  // ── Full event sequence for dispatch + CountLoop ─────────────────────

  @Test
  public void fullEventSequenceMatchesExpectedPattern() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    // myListener: CountLoop(2) { Comment("tick") }
    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(2));
    countLoop.body.getValue().statements.add(new Comment("tick"));
    UserMethod myListener = createStaticMethod("myListener",
        new BlockStatement(countLoop));
    type.methods.add(myListener);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), myListener);
    UserMethod entry = createStaticMethod("entry",
        new BlockStatement(new ExpressionStatement(call)));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);

    // Expected sequence:
    // entry: Block executing
    //   ExpressionStatement executing (the call)
    //     myListener: Block executing
    //       CountLoop executing
    //         iter 0: Block executing, Comment executing/executed, Block executed
    //         iter 1: Block executing, Comment executing/executed, Block executed
    //       CountLoop executed
    //     myListener: Block executed
    //   ExpressionStatement executed
    // entry: Block executed

    List<String> events = listener.statementEvents;
    assertEquals("first event must be entry BlockStatement executing",
        "executing:BlockStatement", events.get(0));
    assertEquals("second event must be ExpressionStatement executing",
        "executing:ExpressionStatement", events.get(1));
    assertEquals("third event must be myListener BlockStatement executing",
        "executing:BlockStatement", events.get(2));
    assertEquals("fourth event must be CountLoop executing",
        "executing:CountLoop", events.get(3));

    // Verify the unwinding end
    String lastEvent = events.get(events.size() - 1);
    assertEquals("last event must be entry BlockStatement executed",
        "executed:BlockStatement", lastEvent);

    assertEquals("CountLoop iterations must fire 2 iterating events",
        2, listener.countLoopIteratingCount);
  }

  // ── Zero-iteration CountLoop fires no iteration events ───────────────

  @Test
  public void zeroIterationCountLoopFiresNoIterationEvents() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(0));
    countLoop.body.getValue().statements.add(new Comment("should not execute"));
    UserMethod entry = createStaticMethod("entry", new BlockStatement(countLoop));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);

    assertEquals("zero-count loop must fire 0 iterating events",
        0, listener.countLoopIteratingCount);
    assertEquals("zero-count loop must fire 0 iterated events",
        0, listener.countLoopIteratedCount);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment"))
        .count();
    assertEquals("Comment in zero-count body must never execute",
        0, commentCount);
  }

  // ── Chained dispatch: A → B → CountLoop ──────────────────────────────

  @Test
  public void chainedDispatchFiresCountLoopThroughTwoLevels() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    // innerHandler: CountLoop(2) { Comment("inner") }
    CountLoop innerLoop = AstUtilities.createCountLoop(new IntegerLiteral(2));
    innerLoop.body.getValue().statements.add(new Comment("inner"));
    UserMethod innerHandler = createStaticMethod("innerHandler",
        new BlockStatement(innerLoop));
    type.methods.add(innerHandler);

    // outerDispatch calls innerHandler
    MethodInvocation callInner = new MethodInvocation(new NullLiteral(), innerHandler);
    UserMethod outerDispatch = createStaticMethod("outerDispatch",
        new BlockStatement(new ExpressionStatement(callInner)));
    type.methods.add(outerDispatch);

    // entry calls outerDispatch
    MethodInvocation callOuter = new MethodInvocation(new NullLiteral(), outerDispatch);
    UserMethod entry = createStaticMethod("entry",
        new BlockStatement(new ExpressionStatement(callOuter)));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);

    assertEquals("CountLoop through 2-level chain must still fire 2 iterating events",
        2, listener.countLoopIteratingCount);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment"))
        .count();
    assertEquals("Comment in inner loop must execute 2 times",
        2, commentCount);
  }

  // ── Listener removal stops event recording ───────────────────────────

  @Test
  public void removedListenerStopsReceivingCountLoopEvents() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    RecordingListener listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);

    NamedUserType type = createProgramType();

    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(3));
    countLoop.body.getValue().statements.add(new Comment("tick"));
    UserMethod entry = createStaticMethod("entry", new BlockStatement(countLoop));
    type.methods.add(entry);

    vm.ENTRY_POINT_invoke(null, entry);
    assertEquals("first run: 3 iterations", 3, listener.countLoopIteratingCount);

    vm.removeVirtualMachineListener(listener);
    vm.ENTRY_POINT_invoke(null, entry);

    assertEquals("after removal: still 3 (no new events)",
        3, listener.countLoopIteratingCount);
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  private static NamedUserType createProgramType() {
    NamedUserType type = new NamedUserType();
    type.name.setValue("StudentProgram");
    type.superType.setValue(JavaType.OBJECT_TYPE);
    return type;
  }

  private static UserMethod createStaticMethod(String name, BlockStatement body) {
    UserMethod method = new UserMethod(name, Void.TYPE, new UserParameter[0], body);
    method.isStatic.setValue(true);
    return method;
  }

  // ── Recording listener ───────────────────────────────────────────────

  private static class RecordingListener implements VirtualMachineListener {
    final List<String> statementEvents = new ArrayList<>();
    int countLoopIteratingCount = 0;
    int countLoopIteratedCount = 0;
    int expressionEvaluatedCount = 0;

    @Override
    public void statementExecuting(StatementExecutionEvent event) {
      Statement stmt = event.getStatement();
      statementEvents.add("executing:" + stmt.getClass().getSimpleName());
    }

    @Override
    public void statementExecuted(StatementExecutionEvent event) {
      Statement stmt = event.getStatement();
      statementEvents.add("executed:" + stmt.getClass().getSimpleName());
    }

    @Override
    public void expressionEvaluated(ExpressionEvaluationEvent event) {
      expressionEvaluatedCount++;
    }

    @Override
    public void countLoopIterating(CountLoopIterationEvent event) {
      countLoopIteratingCount++;
    }

    @Override
    public void countLoopIterated(CountLoopIterationEvent event) {
      countLoopIteratedCount++;
    }

    @Override
    public void whileLoopIterating(WhileLoopIterationEvent e) {}

    @Override
    public void whileLoopIterated(WhileLoopIterationEvent e) {}

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
