package org.alice.ide.croquet.codecs;

import edu.cmu.cs.dennisc.codec.InputStreamBinaryDecoder;
import edu.cmu.cs.dennisc.codec.OutputStreamBinaryEncoder;
import org.alice.ide.ProjectStack;
import org.alice.ide.croquet.models.numberpad.IntegerModel;
import org.junit.Test;
import org.lgna.croquet.ItemCodec;
import org.lgna.project.Project;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import static org.junit.Assert.*;

public class CodecUtilitiesExtendedTest {

  @Test
  public void stringCodecRoundTripPreservesSpecialCharacters() {
    String value = "alpha & beta <gamma>";

    String decoded = roundTrip(StringCodec.SINGLETON, value);

    assertEquals(value, decoded);
  }

  @Test
  public void localeCodecRoundTripPreservesLanguageCountryAndVariant() {
    Locale locale = Locale.of("en", "US", "POSIX");

    Locale decoded = roundTrip(LocaleCodec.SINGLETON, locale);

    assertEquals(locale.getLanguage(), decoded.getLanguage());
    assertEquals(locale.getCountry(), decoded.getCountry());
    assertEquals(locale.getVariant(), decoded.getVariant());
  }

  @Test
  public void localeCodecRoundTripSupportsNull() {
    assertNull(roundTrip(LocaleCodec.SINGLETON, null));
  }

  @Test
  public void nodeCodecRoundTripReturnsSameInstanceWhenGlobalMapContainsNode() {
    IntegerLiteral literal = new IntegerLiteral(17);
    NodeCodec<IntegerLiteral> codec = NodeCodec.getInstance(IntegerLiteral.class);
    NodeCodec.addNodeToGlobalMap(literal);
    try {
      IntegerLiteral decoded = roundTrip(codec, literal);
      assertSame(literal, decoded);
    } finally {
      NodeCodec.removeNodeFromGlobalMap(literal);
    }
  }

  @Test
  public void nodeCodecRoundTripCanResolveNodeFromProjectStack() {
    NamedUserType programType = new NamedUserType();
    programType.name.setValue("Program");
    UserField field = new UserField();
    field.name.setValue("score");
    field.valueType.setValue(JavaType.getInstance(String.class));
    programType.fields.add(field);
    Project project = new Project(programType, Project.SceneCameraType.WindowCamera);
    ProjectStack.pushProject(project);
    try {
      UserField decoded = roundTrip(NodeCodec.getInstance(UserField.class), field);
      assertSame(field, decoded);
    } finally {
      ProjectStack.popAndCheckProject(project);
    }
  }

  @Test
  public void nodeCodecRoundTripSupportsNull() {
    assertNull(roundTrip(NodeCodec.getInstance(IntegerLiteral.class), null));
  }

  @Test
  public void singletonCodecRoundTripUsesGetInstanceMethod() {
    SingletonCodec<IntegerModel> codec = SingletonCodec.getInstance(IntegerModel.class);

    IntegerModel decoded = roundTrip(codec, IntegerModel.getInstance());

    assertSame(IntegerModel.getInstance(), decoded);
  }

  @Test
  public void singletonCodecRoundTripSupportsNull() {
    SingletonCodec<IntegerModel> codec = SingletonCodec.getInstance(IntegerModel.class);

    assertNull(roundTrip(codec, null));
  }

  private <T> T roundTrip(ItemCodec<T> codec, T value) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamBinaryEncoder encoder = new OutputStreamBinaryEncoder(outputStream);
    codec.encodeValue(encoder, value);
    encoder.flush();
    InputStreamBinaryDecoder decoder = new InputStreamBinaryDecoder(new ByteArrayInputStream(outputStream.toByteArray()));
    return codec.decodeValue(decoder);
  }
}
