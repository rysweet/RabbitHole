package org.alice.ide.ast.export.type;

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.Assert.*;

public class TypeSummaryDataSourceTest {

  private static TypeSummary createTypeSummary() {
    return new TypeSummary(TypeSummary.CURRENT_VERSION, "TestType",
        Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
  }

  @Test
  public void getName_returnsFilename() {
    TypeSummaryDataSource dataSource = new TypeSummaryDataSource(createTypeSummary());

    assertEquals("typeSummary.xml", dataSource.getName());
  }

  @Test
  public void filename_constant_value() {
    assertEquals("typeSummary.xml", TypeSummaryDataSource.FILENAME);
  }

  @Test
  public void write_producesNonEmptyXml() throws Exception {
    TypeSummaryDataSource dataSource = new TypeSummaryDataSource(createTypeSummary());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    dataSource.write(outputStream);

    assertTrue(outputStream.size() > 0);
  }

  @Test
  public void write_producesValidXml() throws Exception {
    TypeSummaryDataSource dataSource = new TypeSummaryDataSource(createTypeSummary());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    dataSource.write(outputStream);

    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(document);
    assertNotNull(document.getDocumentElement());
    assertEquals("typeSummary", document.getDocumentElement().getTagName());
  }
}
