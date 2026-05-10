package org.lgna.project.ast;

import org.lgna.project.code.CodeOrganizer;
import org.junit.Test;

import static org.junit.Assert.*;

public class SourceCodeGeneratorTest {

  @Test
  public void repairsCachedCountNameBeforeForEachHeaderAndBodyEmission() {
    ForEachInArrayLoop loop = forEachLoop("COUNT__");
    UserLocal copy = new UserLocal("copy", String.class, true);
    loop.body.getValue().statements.add(new LocalDeclarationStatement(copy, new LocalAccess(loop.item.getValue())));

    String source = generate(loop);

    assertFalse(source, source.contains("COUNT__"));
    assertTrue(source, source.contains("for(String itemA : new String[]{\"red\", \"blue\"})"));
    assertTrue(source, source.contains("final String copy=itemA;"));
  }

  @Test
  public void preservesExplicitForEachItemName() {
    ForEachInArrayLoop loop = forEachLoop("item");
    UserLocal copy = new UserLocal("copy", String.class, true);
    loop.body.getValue().statements.add(new LocalDeclarationStatement(copy, new LocalAccess(loop.item.getValue())));

    String source = generate(loop);

    assertTrue(source, source.contains("for(String item : new String[]{\"red\", \"blue\"})"));
    assertTrue(source, source.contains("final String copy=item;"));
  }

  @Test
  public void characterizesRepresentativeStatementGoldenSnippets() {
    UserLocal greeting = new UserLocal("greeting", String.class, true);
    assertEquals(
        "final String greeting=\"hello\";",
        generate(new LocalDeclarationStatement(greeting, new StringLiteral("hello"))));

    ConditionalStatement conditional = AstUtilities.createConditionalStatement(new BooleanLiteral(true));
    conditional.booleanExpressionBodyPairs.get(0).body.getValue().statements.add(
        AstUtilities.createReturnStatement(String.class, new StringLiteral("yes")));
    conditional.elseBody.getValue().statements.add(
        AstUtilities.createReturnStatement(String.class, new StringLiteral("no")));
    assertEquals(
        "if(true){return \"yes\";} else{return \"no\";}",
        generate(conditional));

    CountLoop countLoop = AstUtilities.createCountLoop(new IntegerLiteral(3));
    countLoop.body.getValue().statements.add(new ExpressionStatement(new LocalAccess(countLoop.variable.getValue())));
    assertEquals(
        "for(Integer index_=0;index_<3;index_++){index_;}",
        generate(countLoop));
  }

  @Test
  public void characterizesDisabledStatementInsideEnabledBlock() {
    LocalDeclarationStatement disabledStatement = new LocalDeclarationStatement(
        new UserLocal("hidden", String.class, true),
        new StringLiteral("secret"));
    disabledStatement.isEnabled.setValue(false);
    BlockStatement block = new BlockStatement(
        disabledStatement,
        new LocalDeclarationStatement(
            new UserLocal("shown", String.class, true),
            new StringLiteral("visible")));

    assertEquals(
        "{\n/* disabled\nfinal String hidden=\"secret\";\n*/\nfinal String shown=\"visible\";}",
        generate(block));
  }

  @Test
  public void characterizesRepresentativeExpressionAndTypeGoldenSnippets() {
    assertEquals(
        "new String[]{\"red\", \"blue\"}",
        generate(AstUtilities.createArrayInstanceCreation(
            String[].class,
            new StringLiteral("red"),
            new StringLiteral("blue"))));

    assertEquals("String.class", generate(new TypeLiteral(String.class)));
  }

  @Test
  public void stringLiteralEscapesSpecialCharactersInGeneratedJavaSource() {
    StringLiteral literal = new StringLiteral("line1\n\t\"quote\"\\backslash");

    assertEquals(
        "\"line1\\n\\t\\\"quote\\\"\\\\backslash\"",
        generate(literal));
  }

  @Test
  public void characterizesSpecialPrimitiveLiteralNames() {
    assertEquals("Integer.MAX_VALUE", generateInt(Integer.MAX_VALUE));
    assertEquals("Integer.MIN_VALUE", generateInt(Integer.MIN_VALUE));
    assertEquals("Float.NaN", generateFloat(Float.NaN));
    assertEquals("Float.POSITIVE_INFINITY", generateFloat(Float.POSITIVE_INFINITY));
    assertEquals("Float.NEGATIVE_INFINITY", generateFloat(Float.NEGATIVE_INFINITY));
    assertEquals("Double.NaN", generateDouble(Double.NaN));
    assertEquals("Double.POSITIVE_INFINITY", generateDouble(Double.POSITIVE_INFINITY));
    assertEquals("Double.NEGATIVE_INFINITY", generateDouble(Double.NEGATIVE_INFINITY));
  }

  @Test
  public void characterizesRepresentativeMemberAndClassGoldenSnippet() {
    UserField message = new UserField("message", String.class, new StringLiteral("hi"));
    message.accessLevel.setValue(AccessLevel.PRIVATE);
    message.finalVolatileOrNeither.setValue(FieldModifierFinalVolatileOrNeither.FINAL);

    UserParameter prefix = new UserParameter("prefix", String.class);
    UserMethod greet = new UserMethod(
        "greet",
        String.class,
        new UserParameter[] {prefix},
        new BlockStatement(AstUtilities.createReturnStatement(
            String.class,
            new StringConcatenation(new ParameterAccess(prefix), new FieldAccess(new ThisExpression(), message)))));

    NamedUserType greeter = new NamedUserType(
        "Greeter",
        null,
        Object.class,
        new NamedUserConstructor[] {new NamedUserConstructor(new UserParameter[] {}, new ConstructorBlockStatement())},
        new UserMethod[] {greet},
        new UserField[] {message});

    assertEquals(
        "class Greeter extends Object{public Greeter(){super();}public String greet(String prefix){return prefix + this.message;}public String getMessage(){return this.message;}private final String message=\"hi\";}",
        generate(greeter));
  }

  @Test
  public void characterizesWhileLoopWithBooleanCondition() {
    WhileLoop loop = new WhileLoop(new BooleanLiteral(false), new BlockStatement());
    assertEquals("while (false){}", generate(loop));
  }

  @Test
  public void characterizesNullLiteral() {
    assertEquals("null", generate(new NullLiteral()));
  }

  @Test
  public void characterizesLogicalComplement() {
    assertEquals("!true", generate(new LogicalComplement(new BooleanLiteral(true))));
  }

  @Test
  public void characterizesArithmeticInfixExpression() {
    assertEquals(
        "3+4",
        generate(new ArithmeticInfixExpression(
            new IntegerLiteral(3), ArithmeticInfixExpression.Operator.PLUS, new IntegerLiteral(4), Integer.class)));
  }

  @Test
  public void characterizesRelationalInfixExpression() {
    assertEquals(
        "3<10",
        generate(new RelationalInfixExpression(
            new IntegerLiteral(3), RelationalInfixExpression.Operator.LESS, new IntegerLiteral(10),
            Integer.class, Integer.class)));
  }

  @Test
  public void characterizesConditionalAndInfixExpression() {
    assertEquals(
        "true&&false",
        generate(new ConditionalInfixExpression(
            new BooleanLiteral(true), ConditionalInfixExpression.Operator.AND, new BooleanLiteral(false))));
  }

  @Test
  public void characterizesConditionalOrInfixExpression() {
    assertEquals(
        "true||false",
        generate(new ConditionalInfixExpression(
            new BooleanLiteral(true), ConditionalInfixExpression.Operator.OR, new BooleanLiteral(false))));
  }

  @Test
  public void characterizesLogicalOrPrecedenceOverAnd() {
    assertEquals(
        "true&&false||true",
        generate(new ConditionalInfixExpression(
            new ConditionalInfixExpression(
                new BooleanLiteral(true), ConditionalInfixExpression.Operator.AND, new BooleanLiteral(false)),
            ConditionalInfixExpression.Operator.OR,
            new BooleanLiteral(true))));
  }

  @Test
  public void characterizesArrayAccess() {
    UserLocal items = new UserLocal("items", String[].class, false);
    assertEquals(
        "items[0]",
        generate(new ArrayAccess(String[].class, new LocalAccess(items), new IntegerLiteral(0))));
  }

  @Test
  public void characterizesArrayLength() {
    UserLocal items = new UserLocal("items", String[].class, false);
    assertEquals("items.length", generate(new ArrayLength(new LocalAccess(items))));
  }

  @Test
  public void characterizesExpressionStatementAssignmentAndStaticMethodCall() {
    UserLocal count = new UserLocal("count", Integer.class, false);
    assertEquals(
        "count=4;",
        generate(new ExpressionStatement(new AssignmentExpression(
            JavaType.getInstance(Integer.class),
            new LocalAccess(count),
            AssignmentExpression.Operator.ASSIGN,
            new IntegerLiteral(4)))));

    JavaMethod valueOf = JavaMethod.getInstance(String.class, "valueOf", int.class);
    assertEquals(
        "String.valueOf(7);",
        generate(new ExpressionStatement(new MethodInvocation(
            new TypeExpression(String.class),
            valueOf,
            new SimpleArgument(valueOf.getRequiredParameters().get(0), new IntegerLiteral(7))))));
  }

  @Test
  public void characterizesDirectFieldAccessAndInstanceMethodCall() {
    UserField message = new UserField("message", String.class, new StringLiteral("hi"));
    assertEquals("this.message", generate(new FieldAccess(new ThisExpression(), message)));

    JavaMethod trim = JavaMethod.getInstance(String.class, "trim");
    assertEquals(
        "\" padded \".trim();",
        generate(new ExpressionStatement(new MethodInvocation(new StringLiteral(" padded "), trim))));
  }

  @Test
  public void characterizesIterableForEachLoopHeaderAndBody() {
    UserLocal item = new UserLocal("item", String.class, true);
    UserLocal items = new UserLocal("items", Iterable.class, false);
    ForEachInIterableLoop loop = new ForEachInIterableLoop(
        item,
        new LocalAccess(items),
        new BlockStatement());
    UserLocal copy = new UserLocal("copy", String.class, true);
    loop.body.getValue().statements.add(new LocalDeclarationStatement(copy, new LocalAccess(loop.item.getValue())));

    assertEquals(
        "for(String item : items){final String copy=item;}",
        generate(loop));
  }

  @Test
  public void characterizesDoTogetherRunnableFallbackSnippet() {
    DoTogether doTogether = new DoTogether(new BlockStatement(
        new LocalDeclarationStatement(
            new UserLocal("left", String.class, true),
            new StringLiteral("L")),
        new LocalDeclarationStatement(
            new UserLocal("right", String.class, true),
            new StringLiteral("R"))));

    assertEquals(
        "ThreadUtilities.doTogether(new Runnable(){public void run(){final String left=\"L\";}},new Runnable(){public void run(){final String right=\"R\";}});",
        generate(doTogether));
  }

  private static ForEachInArrayLoop forEachLoop(String itemName) {
    return new ForEachInArrayLoop(
        new UserLocal(itemName, String.class, true),
        AstUtilities.createArrayInstanceCreation(
            String[].class,
            new StringLiteral("red"),
            new StringLiteral("blue")),
        new BlockStatement());
  }

  private static String generate(Statement statement) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder().build();
    statement.process(generator);
    return generator.getText();
  }

  private static String generate(Expression expression) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder().build();
    generator.processExpression(expression);
    return generator.getText();
  }

  private static String generate(NamedUserType type) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder()
        .addDefaultCodeOrganizerDefinition(CodeOrganizer.defaultCodeOrganizer)
        .build();
    type.process(generator);
    return generator.getText();
  }

  private static String generateInt(int value) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder().build();
    generator.processInt(value);
    return generator.getText();
  }

  private static String generateFloat(float value) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder().build();
    generator.processFloat(value);
    return generator.getText();
  }

  private static String generateDouble(double value) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Builder().build();
    generator.processDouble(value);
    return generator.getText();
  }
}
