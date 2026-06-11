package org.alice.ide.croquet.models.project.stats.croquet;

import org.alice.ide.croquet.models.ui.formatter.FormatterState;
import org.alice.ide.formatter.AliceFormatter;
import org.alice.ide.formatter.Formatter;
import org.alice.ide.testing.ProjectContextTestCase;
import org.alice.ide.testing.TestIdeBootstrap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.BooleanExpressionBodyPair;
import org.lgna.project.ast.BooleanLiteral;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.ConditionalStatement;
import org.lgna.project.ast.CountLoop;
import org.lgna.project.ast.DoTogether;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.WhileLoop;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StatisticsComputationBehaviorTest extends ProjectContextTestCase {
  private Formatter previousFormatter;

  @Before
  public void rememberFormatter() {
    previousFormatter = FormatterState.getInstance().getValue();
    FormatterState.getInstance().setValueTransactionlessly(AliceFormatter.getInstance());
  }

  @After
  public void restoreFormatter() {
    if (previousFormatter != null) {
      FormatterState.getInstance().setValueTransactionlessly(previousFormatter);
    }
  }

  @Test
  public void methodFrequencyComposite_countsEnabledInvocations_andAppliesVisibilityFilters() {
    trimFixtureMethods();
    fixture.sceneProcedure.body.getValue().statements.add(invocationOf(fixture.sceneProcedure));
    fixture.sceneProcedure.body.getValue().statements.add(invocationOf(fixture.sceneFunction));
    ExpressionStatement disabledInvocation = invocationOf(fixture.sceneFunction);
    disabledInvocation.isEnabled.setValue(false);
    fixture.sceneProcedure.body.getValue().statements.add(disabledInvocation);
    fixture.sceneFunction.body.getValue().statements.add(0, invocationOf(fixture.sceneProcedure));

    StatisticsMethodFrequencyTabComposite composite = TestIdeBootstrap.onEdt(StatisticsMethodFrequencyTabComposite::new);

    List<UserMethod> methods = toMethods(composite.getUserMethodList());
    assertTrue(methods.contains(fixture.sceneFunction));
    assertTrue(methods.contains(fixture.sceneProcedure));
    assertTrue(methods.indexOf(fixture.sceneFunction) < methods.indexOf(fixture.sceneProcedure));
    assertEquals(6, composite.getMaximum().intValue());
    assertEquals(2, composite.getCount(fixture.sceneProcedure, null));
    assertEquals(1, composite.getCount(fixture.sceneProcedure, fixture.sceneFunction));
    assertEquals(1, composite.getCount(fixture.sceneProcedure, fixture.sceneProcedure));
    assertEquals(List.of("score", "storyAction"), composite.getLeftColVals(fixture.sceneProcedure));
    assertEquals(List.of(1, 1), composite.getRightColVals(fixture.sceneProcedure));

    TestIdeBootstrap.onEdt(() -> {
      composite.getShowFunctionsState().setValueTransactionlessly(false);
      return null;
    });

    assertEquals(List.of("storyAction"), composite.getLeftColVals(fixture.sceneProcedure));
    assertEquals(List.of(1), composite.getRightColVals(fixture.sceneProcedure));
    assertEquals(2, composite.getSize(fixture.sceneProcedure));
  }

  @Test
  public void flowControlComposite_countsProjectConstructs_andFramePublishesBothTabs() {
    trimFixtureMethods();
    BlockStatement ifBody = new BlockStatement(new Comment("if branch"));
    ConditionalStatement conditional = new ConditionalStatement(
        new BooleanExpressionBodyPair[] {new BooleanExpressionBodyPair(new BooleanLiteral(true), ifBody)},
        new BlockStatement());
    CountLoop countLoop = new CountLoop(
        new UserLocal("i", JavaType.INTEGER_OBJECT_TYPE, false),
        new UserLocal("count", JavaType.INTEGER_OBJECT_TYPE, true),
        new IntegerLiteral(3),
        new BlockStatement(new LocalDeclarationStatement(
            new UserLocal("value", Object.class, false),
            new NullLiteral())));
    DoTogether disabledDoTogether = new DoTogether();
    disabledDoTogether.isEnabled.setValue(false);
    fixture.sceneProcedure.body.getValue().statements.add(conditional);
    fixture.sceneProcedure.body.getValue().statements.add(countLoop);
    fixture.sceneProcedure.body.getValue().statements.add(disabledDoTogether);
    fixture.sceneFunction.body.getValue().statements.add(0, new WhileLoop(new BooleanLiteral(false), new BlockStatement()));

    StatisticsFlowControlFrequencyComposite flowComposite = TestIdeBootstrap.onEdt(StatisticsFlowControlFrequencyComposite::new);

    List<UserMethod> methods = toMethods(flowComposite.getUserMethodList());
    assertSame(StatisticsFlowControlFrequencyComposite.root, methods.get(0));
    assertTrue(methods.contains(fixture.sceneFunction));
    assertTrue(methods.contains(fixture.sceneProcedure));
    assertTrue(methods.indexOf(fixture.sceneFunction) < methods.indexOf(fixture.sceneProcedure));
    assertEquals(1, flowComposite.getCount(StatisticsFlowControlFrequencyComposite.root, ConditionalStatement.class));
    assertEquals(1, flowComposite.getCount(StatisticsFlowControlFrequencyComposite.root, CountLoop.class));
    assertEquals(1, flowComposite.getCount(StatisticsFlowControlFrequencyComposite.root, WhileLoop.class));
    assertEquals(1, flowComposite.getCount(StatisticsFlowControlFrequencyComposite.root, LocalDeclarationStatement.class));
    assertEquals(1, flowComposite.getCount(StatisticsFlowControlFrequencyComposite.root, ReturnStatement.class));
    assertEquals(0, flowComposite.getCount(StatisticsFlowControlFrequencyComposite.root, DoTogether.class));
    assertEquals(1, flowComposite.getMaximum(new Class[] {
        ConditionalStatement.class,
        CountLoop.class,
        WhileLoop.class,
        LocalDeclarationStatement.class,
        ReturnStatement.class
    }));

    StatisticsFrameComposite frameComposite = TestIdeBootstrap.onEdt(
        () -> new StatisticsFrameComposite(TestIdeBootstrap.getDocumentFrame()));
    assertEquals(2, frameComposite.getTabState().getItemCount());
    assertTrue(frameComposite.getTabState().getItemAt(0) instanceof StatisticsFlowControlFrequencyComposite);
    assertTrue(frameComposite.getTabState().getItemAt(1) instanceof StatisticsMethodFrequencyTabComposite);
  }

  private void trimFixtureMethods() {
    fixture.sceneType.methods.clear();
    fixture.sceneType.methods.add(fixture.sceneProcedure);
    fixture.sceneType.methods.add(fixture.sceneFunction);
  }

  private static List<UserMethod> toMethods(org.lgna.croquet.MutableDataSingleSelectListState<UserMethod> state) {
    List<UserMethod> methods = new ArrayList<>();
    for (int i = 0; i < state.getItemCount(); i++) {
      methods.add(state.getItemAt(i));
    }
    return methods;
  }

  private static ExpressionStatement invocationOf(UserMethod method) {
    return AstUtilities.createMethodInvocationStatement(new ThisExpression(), method);
  }
}
