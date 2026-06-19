package org.lgna.story.resourceutilities;

import edu.cmu.cs.dennisc.property.DoubleBufferProperty;
import edu.cmu.cs.dennisc.property.FloatBufferProperty;
import edu.cmu.cs.dennisc.scenegraph.WeightedMesh;
import org.alice.math.immutable.AffineMatrix4x4;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class StoryResourceUtilitiesBasicsTest {
  @Test
  public void pipelineExceptionTracksContextAndFormatsToString() {
    IllegalStateException cause = new IllegalStateException("bad row");
    PipelineException exception = new PipelineException("failure", "alpha", cause);
    exception.setSpreadsheetInfo("A", 4);
    exception.setColumnName("B");
    exception.setRowNumber(7);
    exception.setData("beta");

    assertEquals("beta", exception.getData());
    assertEquals("B", exception.getColumnName());
    assertEquals(7, exception.getRowNumber());
    assertSame(cause, exception.getCause());

    String text = exception.toString();
    assertTrue(text.contains("COL: B"));
    assertTrue(text.contains("ROW: 7"));
    assertTrue(text.contains("DATA: beta"));
    assertTrue(text.contains("failure"));
  }

  @Test
  public void pipelineExceptionSupportsSimpleAndCauseOnlyConstructors() {
    PipelineException simple = new PipelineException("simple message");
    assertEquals("simple message", simple.getMessage());
    assertNull(simple.getData());
    assertEquals(-1, simple.getRowNumber());

    RuntimeException cause = new RuntimeException("boom");
    PipelineException causeOnly = new PipelineException("payload", cause);
    assertEquals("payload", causeOnly.getData());
    assertSame(cause, causeOnly.getCause());
  }

  @Test
  public void modelLoadingExceptionPreservesCause() {
    IllegalArgumentException cause = new IllegalArgumentException("bad model");
    ModelLoadingException exception = new ModelLoadingException("message", cause);
    assertEquals("message", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  public void colladaTransformUtilitiesFlipArrays() {
    double[] transform = {
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12,
        13, 14, 15, 16
    };
    double[] flippedTransform = ColladaTransformUtilities.createFlippedRowMajorTransform(transform);
    assertArrayEquals(new double[]{1, -2, 3, -4, -5, 6, -7, 8, 9, -10, 11, -12, 13, 14, 15, 16}, flippedTransform, 0.0);

    assertArrayEquals(new double[]{-1, 2, -3, -4, 5, -6},
        ColladaTransformUtilities.createFlippedPoint3DoubleArray(new double[]{1, 2, 3, 4, 5, 6}), 0.0);
    assertArrayEquals(new float[]{-1f, 2f, -3f, -4f, 5f, -6f},
        ColladaTransformUtilities.createFlippedPoint3FloatArray(new float[]{1f, 2f, 3f, 4f, 5f, 6f}), 0.0f);
  }

  @Test
  public void utilitiesCreateDoubleBufferHandlesNullAndData() {
    assertNull(Utilities.createDoubleBuffer(null));

    DoubleBuffer buffer = Utilities.createDoubleBuffer(new double[]{1.5, 2.5, 3.5});
    assertNotNull(buffer);
    assertEquals(3, buffer.remaining());
    assertEquals(1.5, buffer.get(0), 0.0);
    assertEquals(2.5, buffer.get(1), 0.0);
    assertEquals(3.5, buffer.get(2), 0.0);
  }

  @Test
  public void modelExportDataSourcesWriteAndResolveRelativeNames() throws Exception {
    ModelExportDataSources.Writer writer = os -> os.write(new byte[]{1, 2, 3});
    edu.cmu.cs.dennisc.java.util.zip.DataSource dataSource = ModelExportDataSources.create("resource/path/model.glb", writer);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    dataSource.write(baos);

    assertArrayEquals(new byte[]{1, 2, 3}, baos.toByteArray());
    assertEquals("resource/path/model.glb", dataSource.getName());
    assertEquals("model.glb", ModelExportDataSources.structureFileNameRelativeTo("resource/path", dataSource));
  }

  @Test
  public void orientationSupportsAllUpAxisMappingsAndBufferTransforms() {
    Orientation xUp = Orientation.forUpAxis("X_UP");
    Orientation zUp = Orientation.forUpAxis("Z_UP");
    Orientation yUp = Orientation.forUpAxis("Y_UP");
    assertNotNull(xUp.orientMatrixToAlice(AffineMatrix4x4.IDENTITY));
    assertNotNull(zUp.orientMatrixToAlice(AffineMatrix4x4.IDENTITY));
    assertNotNull(yUp.orientMatrixToAlice(AffineMatrix4x4.IDENTITY));

    Orientation alice = Orientation.forAlice();
    DoubleBufferProperty verticesProperty = new DoubleBufferProperty(new WeightedMesh(), (DoubleBuffer) null);
    double[] orientedVertices = alice.orientVertices(new float[]{1f, 2f, 3f}, verticesProperty);
    assertArrayEquals(new double[]{-1.0, 2.0, -3.0}, orientedVertices, 0.000001);
    assertArrayEquals(orientedVertices, read(verticesProperty.getValue()), 0.000001);

    FloatBufferProperty normalsProperty = new FloatBufferProperty(new WeightedMesh(), (FloatBuffer) null);
    alice.orientNormals(new float[]{1f, 2f, 3f}, normalsProperty);
    assertArrayEquals(new float[]{-1f, 2f, -3f}, read(normalsProperty.getValue()), 0.000001f);
  }

  @Test
  public void aliceModelImportDataStoresTypedProofPath() {
    Path sourceFile = Path.of("source.dae");
    AliceModelImportData importData = new AliceModelImportData(
        sourceFile,
        new edu.cmu.cs.dennisc.scenegraph.SkeletonVisual(),
        Path.of("proof.glb"),
        Path.of("proof.a3r"),
        Optional.empty(),
        0,
        0);

    assertEquals(sourceFile, importData.getSourceFile());
  }

  private static double[] read(DoubleBuffer buffer) {
    double[] values = new double[buffer.remaining()];
    for (int i = 0; i < values.length; i++) {
      values[i] = buffer.get(i);
    }
    return values;
  }

  private static float[] read(FloatBuffer buffer) {
    float[] values = new float[buffer.remaining()];
    for (int i = 0; i < values.length; i++) {
      values[i] = buffer.get(i);
    }
    return values;
  }
}
