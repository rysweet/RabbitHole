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
