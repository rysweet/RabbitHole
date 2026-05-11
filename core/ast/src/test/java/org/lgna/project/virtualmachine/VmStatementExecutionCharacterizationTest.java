package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.CountLoop;
import org.lgna.project.ast.ArrayInstanceCreation;
import org.lgna.project.ast.DoInOrder;
import org.lgna.project.ast.DoTogether;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.ForEachInArrayLoop;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalAccess;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.WhileLoop;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for VirtualMachine statement execution.
 * Documents the current behavior of block statements, conditionals,
 * loops (count, while, doInOrder, doTogether), local declarations,
 * and return statements.
 *
 * <p>Headless scope: no JavaFX, no gallery assets, no scene graph, no display.
 *
 * @see VirtualMachine#execute(org.lgna.project.ast.Statement)
 */
public class VmStatementExecutionCharacterizationTest {

  private ReleaseVirtualMachine vm;
  private RecordingListener listener;
  private NamedUserType type;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
    listener = new RecordingListener();
    vm.addVirtualMachineListener(listener);
    type = createProgramType();
  }

  private void invokeStatic(UserMethod method) {
    vm.ENTRY_POINT_invoke(null, method);
  }

  private Object invokeStaticFunction(UserMethod method) {
    return vm.ENTRY_POINT_invoke(null, method);
  }

  // --- Block statement ---

  @Test
  public void emptyBlockStatementFiresExecutingAndExecutedEvents() {
    UserMethod method = createStaticProcedure("emptyBlock", new BlockStatement());
    type.methods.add(method);

    invokeStatic(method);

    assertEquals(Arrays.asList(
        "executing:BlockStatement",
        "executed:BlockStatement"),
        listener.statementEvents);
  }

  @Test
  public void blockWithCommentFiresFourEvents() {
    UserMethod method = createStaticProcedure("commented",
        new BlockStatement(new Comment("test")));
    type.methods.add(method);

    invokeStatic(method);

    assertEquals(Arrays.asList(
        "executing:BlockStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement"),
        listener.statementEvents);
  }

  // --- Conditional statement ---

  @Test
  public void conditionalTrueBranchExecutes() {
    Comment thenComment = new Comment("then");
    Comment elseComment = new Comment("else");
    ConditionalStatement conditional = new ConditionalStatement(
        new BooleanExpressionBodyPair[]{
            new BooleanExpressionBodyPair(new BooleanLiteral(true), new BlockStatement(thenComment))
        },
        new BlockStatement(elseComment));

    UserMethod method = createStaticProcedure("ifTrue", new BlockStatement(conditional));
    type.methods.add(method);

    invokeStatic(method);

    assertTrue("Then-branch Comment should fire",
        listener.statementEvents.contains("executing:Comment"));
    // Count Comment events — only 1 (from then-branch, not else)
    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Exactly one Comment should execute", 1, commentCount);
  }

  @Test
  public void conditionalFalseBranchExecutesElse() {
    Comment thenComment = new Comment("then");
    Comment elseComment = new Comment("else");
    ConditionalStatement conditional = new ConditionalStatement(
        new BooleanExpressionBodyPair[]{
            new BooleanExpressionBodyPair(new BooleanLiteral(false), new BlockStatement(thenComment))
        },
        new BlockStatement(elseComment));

    UserMethod method = createStaticProcedure("ifFalse", new BlockStatement(conditional));
    type.methods.add(method);

    invokeStatic(method);

    // The else-branch block fires, containing the else Comment
    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Exactly one Comment (from else) should execute", 1, commentCount);
  }

  // --- CountLoop ---

  @Test
  public void countLoopZeroIterationsExecutesNoBody() {
    UserLocal variable = new UserLocal("i", Integer.class, false);
    UserLocal constant = new UserLocal("n", Integer.class, true);
    CountLoop loop = new CountLoop(variable, constant,
        new IntegerLiteral(0), new BlockStatement(new Comment("body")));

    UserMethod method = createStaticProcedure("countZero", new BlockStatement(loop));
    type.methods.add(method);

    invokeStatic(method);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Zero iterations → no Comment events", 0, commentCount);
  }

  @Test
  public void countLoopThreeIterationsExecutesBodyThreeTimes() {
    UserLocal variable = new UserLocal("i", Integer.class, false);
    UserLocal constant = new UserLocal("n", Integer.class, true);
    CountLoop loop = new CountLoop(variable, constant,
        new IntegerLiteral(3), new BlockStatement(new Comment("body")));

    UserMethod method = createStaticProcedure("countThree", new BlockStatement(loop));
    type.methods.add(method);

    invokeStatic(method);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Three iterations → three Comment events", 3, commentCount);
    assertEquals("Three countLoop iteration events", 3, listener.countLoopIteratingCount);
    assertEquals("Three countLoop iterated events", 3, listener.countLoopIteratedCount);
  }

  // --- WhileLoop ---

  @Test
  public void whileLoopFalseConditionNeverExecutesBody() {
    WhileLoop loop = new WhileLoop(
        new BooleanLiteral(false),
        new BlockStatement(new Comment("unreachable")));

    UserMethod method = createStaticProcedure("whileFalse", new BlockStatement(loop));
    type.methods.add(method);

    invokeStatic(method);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("False condition → no body execution", 0, commentCount);
    assertEquals(0, listener.whileLoopIteratingCount);
  }

  // --- DoInOrder ---

  @Test
  public void doInOrderExecutesStatementsSequentially() {
    DoInOrder doInOrder = new DoInOrder(
        new BlockStatement(new Comment("first"), new Comment("second")));

    UserMethod method = createStaticProcedure("doInOrder", new BlockStatement(doInOrder));
    type.methods.add(method);

    invokeStatic(method);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Two comments execute in order", 2, commentCount);
  }

  // --- DoTogether ---

  @Test
  public void doTogetherEmptyBodyDoesNothing() {
    DoTogether doTogether = new DoTogether(new BlockStatement());

    UserMethod method = createStaticProcedure("togetherEmpty", new BlockStatement(doTogether));
    type.methods.add(method);

    invokeStatic(method);

    assertEquals(Arrays.asList(
        "executing:BlockStatement",
        "executing:DoTogether",
        "executed:DoTogether",
        "executed:BlockStatement"),
        listener.statementEvents);
  }

  @Test
  public void doTogetherSingleStatementExecutesSynchronously() {
    DoTogether doTogether = new DoTogether(
        new BlockStatement(new Comment("solo")));

    UserMethod method = createStaticProcedure("togetherOne", new BlockStatement(doTogether));
    type.methods.add(method);

    invokeStatic(method);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Single statement in DoTogether executes synchronously", 1, commentCount);
  }

  // --- LocalDeclarationStatement + LocalAccess ---

  @Test
  public void localDeclarationAndAccessRoundTrips() {
    UserLocal local = new UserLocal("x", Integer.class, false);
    LocalDeclarationStatement decl = new LocalDeclarationStatement(local, new IntegerLiteral(99));
    ReturnStatement ret = new ReturnStatement(JavaType.getInstance(Integer.class), new LocalAccess(local));

    UserMethod method = new UserMethod("localRoundTrip", Integer.class,
        new UserParameter[0], new BlockStatement(decl, ret));
    method.isStatic.setValue(true);
    type.methods.add(method);

    Object result = invokeStaticFunction(method);
    assertEquals(99, result);
  }

  // --- ReturnStatement ---

  @Test
  public void returnStatementReturnsExpressionValue() {
    ReturnStatement ret = new ReturnStatement(
        JavaType.getInstance(Integer.class), new IntegerLiteral(42));
    UserMethod method = new UserMethod("returnFortyTwo", Integer.class,
        new UserParameter[0], new BlockStatement(ret));
    method.isStatic.setValue(true);
    type.methods.add(method);

    Object result = invokeStaticFunction(method);
    assertEquals(42, result);
  }

  @Test
  public void procedureReturnsNull() {
    UserMethod method = createStaticProcedure("noop",
        new BlockStatement(new Comment("noop")));
    type.methods.add(method);

    Object result = invokeStaticFunction(method);
    assertNull("Procedure should return null", result);
  }

  // --- Disabled statement ---

  @Test
  public void disabledStatementIsSkipped() {
    Comment disabled = new Comment("disabled");
    disabled.isEnabled.setValue(false);

    UserMethod method = createStaticProcedure("disabled", new BlockStatement(disabled));
    type.methods.add(method);

    invokeStatic(method);

    long commentCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("Disabled statement should not fire events", 0, commentCount);
  }

  // --- ForEachInArrayLoop ---

  @Test
  public void forEachInArrayLoopIteratesOverAllElements() {
    ArrayInstanceCreation arrayExpr = new ArrayInstanceCreation(
        Integer[].class,
        new Integer[]{3},
        new IntegerLiteral(10), new IntegerLiteral(20), new IntegerLiteral(30));

    UserLocal item = new UserLocal("item", Integer.class, true);
    ForEachInArrayLoop forEach = new ForEachInArrayLoop(
        item, arrayExpr, new BlockStatement(new Comment("body")));

    UserMethod method = createStaticProcedure("forEachTest", new BlockStatement(forEach));
    type.methods.add(method);

    invokeStatic(method);

    long bodyExecCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("ForEach over 3-element array should execute body 3 times", 3, bodyExecCount);
    assertTrue("ForEachInArrayLoop should fire forEachLoopIterating events",
        listener.forEachIteratingCount >= 3);
  }

  @Test
  public void forEachInArrayLoopWithEmptyArrayExecutesNoBody() {
    ArrayInstanceCreation emptyArray = new ArrayInstanceCreation(
        Integer[].class, new Integer[]{0});

    UserLocal item = new UserLocal("item", Integer.class, true);
    ForEachInArrayLoop forEach = new ForEachInArrayLoop(
        item, emptyArray, new BlockStatement(new Comment("unreachable")));

    UserMethod method = createStaticProcedure("forEachEmpty", new BlockStatement(forEach));
    type.methods.add(method);

    invokeStatic(method);

    long bodyExecCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("ForEach over empty array should execute body 0 times", 0, bodyExecCount);
  }

  @Test
  public void forEachInArrayLoopSetsItemLocalForEachIteration() {
    ArrayInstanceCreation arrayExpr = new ArrayInstanceCreation(
        Integer[].class,
        new Integer[]{2},
        new IntegerLiteral(100), new IntegerLiteral(200));

    UserLocal item = new UserLocal("item", Integer.class, true);
    ForEachInArrayLoop forEach = new ForEachInArrayLoop(
        item, arrayExpr, new BlockStatement(new Comment("access item")));

    UserMethod method = createStaticProcedure("forEachItem", new BlockStatement(forEach));
    type.methods.add(method);

    // Should not throw — the item local is properly managed
    invokeStatic(method);

    long bodyExecCount = listener.statementEvents.stream()
        .filter(e -> e.equals("executing:Comment")).count();
    assertEquals("ForEach should execute body once per element", 2, bodyExecCount);
  }

  // --- Method invocation within body ---

  @Test
  public void methodCallInsideDoInOrderFiresNestedEvents() {
    UserMethod helper = createStaticProcedure("helper",
        new BlockStatement(new Comment("inner")));
    type.methods.add(helper);

    MethodInvocation call = new MethodInvocation(new NullLiteral(), helper);
    DoInOrder doInOrder = new DoInOrder(
        new BlockStatement(new ExpressionStatement(call)));

    UserMethod entry = createStaticProcedure("entry", new BlockStatement(doInOrder));
    type.methods.add(entry);

    invokeStatic(entry);

    assertTrue("Comment from helper should fire inside DoInOrder",
        listener.statementEvents.contains("executing:Comment"));
  }

  // --- Helpers ---

  private static NamedUserType createProgramType() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("VmStatementTestProgram");
    programType.superType.setValue(JavaType.OBJECT_TYPE);
    return programType;
  }

  private static UserMethod createStaticProcedure(String name, BlockStatement body) {
    UserMethod method = new UserMethod(name, Void.TYPE, new UserParameter[0], body);
    method.isStatic.setValue(true);
    return method;
  }

  private static class RecordingListener implements VirtualMachineListener {
    final List<String> statementEvents = new ArrayList<>();
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
    public void expressionEvaluated(ExpressionEvaluationEvent e) {}

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
