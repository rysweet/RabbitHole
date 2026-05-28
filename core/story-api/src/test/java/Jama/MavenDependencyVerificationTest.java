package Jama;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Verifies that the JAMA library is loaded from the Maven dependency
 * (gov.nist.math:jama:1.0.3) rather than from vendored source files.
 * This test validates the dependency swap performed in issue #819.
 */
public class MavenDependencyVerificationTest {

  @Test
  public void matrixClassIsLoadableFromJar() {
    // The Maven dep should provide Jama.Matrix from a JAR on the classpath
    URL location = Matrix.class.getProtectionDomain().getCodeSource().getLocation();
    assertNotNull("Jama.Matrix should have a code source location", location);
    String path = location.toString();
    assertTrue("Jama.Matrix should be loaded from a JAR file, got: " + path,
        path.endsWith(".jar") || path.contains(".jar!"));
  }

  @Test
  public void allDecompositionClassesAccessible() {
    // Alice IK code uses Matrix, SingularValueDecomposition.
    // Verify all JAMA decomposition classes are accessible.
    assertNotNull(new Matrix(2, 2));
    assertNotNull(new Matrix(new double[][]{{4, 2}, {2, 3}}).chol());
    assertNotNull(new Matrix(new double[][]{{1, 2}, {3, 4}}).eig());
    assertNotNull(new Matrix(new double[][]{{1, 2}, {3, 4}}).lu());
    assertNotNull(new Matrix(new double[][]{{1, 2}, {3, 4}}).qr());
    assertNotNull(new Matrix(new double[][]{{1, 2}, {3, 4}}).svd());
  }

  @Test
  public void aliceIkApiSurface_matrixTimesAndSvd() {
    // Exercises the exact JAMA API surface used by Alice's IK solver:
    // Matrix construction, times(), minus(), SVD
    double[][] data = {
        {1.0, 2.0},
        {3.0, 4.0},
        {5.0, 6.0}
    };
    Matrix jacobian = new Matrix(data);

    assertEquals(3, jacobian.getRowDimension());
    assertEquals(2, jacobian.getColumnDimension());

    SingularValueDecomposition svd = jacobian.svd();
    assertNotNull(svd.getU());
    assertNotNull(svd.getS());
    assertNotNull(svd.getV());

    double[] singularValues = svd.getSingularValues();
    assertTrue(singularValues.length > 0);
    // Singular values should be non-negative and in descending order
    for (int i = 0; i < singularValues.length - 1; i++) {
      assertTrue(singularValues[i] >= singularValues[i + 1]);
    }

    // Verify SVD reconstruction: U * S * V^T ≈ original
    Matrix reconstructed = svd.getU().times(svd.getS().times(svd.getV().transpose()));
    double residual = reconstructed.minus(jacobian).normF();
    assertEquals(0.0, residual, 1.0e-10);
  }
}
