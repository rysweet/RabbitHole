package org.alice.ide.croquet.models.html;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.story.SScene;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class HtmlProjectWriterExtendedTest {

  @Test
  public void writeTypeEscapesSpecialCharactersInTitle() throws IOException {
    HtmlProjectWriter writer = new HtmlProjectWriter();
    NamedUserType type = createSceneType("Scene & <Test>", false);

    String output = writeType(writer, type);

    assertTrue(output.contains("<title>Scene &amp; &lt;Test&gt;</title>"));
    assertTrue(output.contains("class=\"alice-class\""));
  }

  @Test
  public void writeTypeWithEmptyTypeStillProducesHtmlScaffold() throws IOException {
    HtmlProjectWriter writer = new HtmlProjectWriter();
    NamedUserType type = createSceneType("EmptyScene", false);

    String output = writeType(writer, type);

    assertTrue(output.contains("<html"));
    assertTrue(output.contains("<body"));
    assertTrue(output.contains("class=\"alice-class\""));
  }

  @Test
  public void writeDeclarationForNonProcessableDeclarationWritesWrapperAndTitle() throws IOException {
    HtmlProjectWriter writer = new HtmlProjectWriter();
    UserParameter parameter = new UserParameter();
    parameter.name.setValue("speed & power");
    parameter.valueType.setValue(JavaType.getInstance(Double.class));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writer.writeDeclaration(outputStream, parameter);
    String output = outputStream.toString(StandardCharsets.UTF_8);

    assertTrue(output.contains("<title>speed &amp; power</title>"));
    assertTrue(output.contains("class=\"alice-method\""));
  }

  @Test
  public void writeDeclarationForStaticMainMethodKeepsTitleHeadlessSafe() throws IOException {
    HtmlProjectWriter writer = new HtmlProjectWriter();
    UserMethod mainMethod = createStaticMainMethod("main");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writer.writeDeclaration(outputStream, mainMethod);
    String output = outputStream.toString(StandardCharsets.UTF_8);

    assertTrue(output.contains("<title>main</title>"));
    assertTrue(output.contains("class=\"alice-method\""));
  }

  @Test
  public void writeTypeIncludesEmbeddedCss() throws IOException {
    HtmlProjectWriter writer = new HtmlProjectWriter();
    NamedUserType type = createSceneType("StyledScene", false);

    String output = writeType(writer, type);

    assertTrue(output.contains(".alice-generated-svg"));
    assertTrue(output.contains(".alice-disabled"));
  }

  private String writeType(HtmlProjectWriter writer, NamedUserType type) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writer.writeType(outputStream, type);
    return outputStream.toString(StandardCharsets.UTF_8);
  }

  private NamedUserType createSceneType(String name, boolean includeStaticMain) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(SScene.class));
    if (includeStaticMain) {
      type.methods.add(createStaticMainMethod("main"));
    }
    return type;
  }

  private UserMethod createStaticMainMethod(String name) {
    UserMethod method = new UserMethod();
    method.name.setValue(name);
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.body.setValue(new BlockStatement());
    method.isStatic.setValue(true);
    method.managementLevel.setValue(ManagementLevel.NONE);
    return method;
  }
}
