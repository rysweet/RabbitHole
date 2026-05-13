/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package edu.cmu.cs.dennisc.render.gl.imp;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import edu.cmu.cs.dennisc.print.PrintUtilities;

import java.awt.*;
import java.awt.geom.PathIterator;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL2.GL_LINE_STIPPLE;

/**
 * Delegate for tessellation-based draw/fill(Shape) operations.
 * Extracted from Graphics2D to reduce class size.
 */
/*package-private*/ final class GlTessellationRenderer {
  static final double FLATNESS = 0.01;
  static final Stroke LINE_STROKE = new BasicStroke(0);

  private final Graphics2D graphics2D;

  GlTessellationRenderer(Graphics2D graphics2D) {
    assert graphics2D != null;
    this.graphics2D = graphics2D;
  }

  void fill(PathIterator pi) {

    class MyTessAdapter implements GLUtessellatorCallback {
      private GL2 gl;

      public MyTessAdapter(GL2 gl) {
        this.gl = gl;
      }

      @Override
      public void begin(int primitiveType) {
        this.gl.glBegin(primitiveType);
      }

      @Override
      public void beginData(int primitiveType, Object data) {
      }

      @Override
      public void vertex(Object data) {
        double[] a = (double[]) data;
        this.gl.glVertex2d(a[0], a[1]);
      }

      @Override
      public void vertexData(Object arg0, Object arg1) {
      }

      @Override
      public void end() {
        this.gl.glEnd();
      }

      @Override
      public void endData(Object arg0) {
      }

      @Override
      public void edgeFlag(boolean value) {
      }

      @Override
      public void edgeFlagData(boolean arg0, Object arg1) {
      }

      @Override
      public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
        assert outData != null;
        assert outData.length > 0;
        double[] out = new double[3];
        out[0] = coords[0];
        out[1] = coords[1];
        out[2] = coords[2];
        outData[0] = out;
      }

      @Override
      public void combineData(double[] arg0, Object[] arg1, float[] arg2, Object[] arg3, Object arg4) {
      }

      @Override
      public void error(int n) {

      }

      @Override
      public void errorData(int n, Object data) {
        PrintUtilities.println("tesselator error");
        PrintUtilities.println("\tn:", n);
        try {
          PrintUtilities.println("\tgluErrorString:", GlTessellationRenderer.this.graphics2D.renderContext.glu.gluErrorString(n));
        } catch (ArrayIndexOutOfBoundsException aioobe) {
          PrintUtilities.println("\tgluErrorString: unknown");
        }
        PrintUtilities.println("\tdata:", data);
      }
    }

    GLUtessellatorCallback adapter = new MyTessAdapter(graphics2D.renderContext.gl);
    GLUtessellator tesselator = GLU.gluNewTess();
    try {
      GLU.gluTessCallback(tesselator, GLU.GLU_TESS_BEGIN, adapter);
      GLU.gluTessCallback(tesselator, GLU.GLU_TESS_VERTEX, adapter);
      GLU.gluTessCallback(tesselator, GLU.GLU_TESS_END, adapter);
      GLU.gluTessCallback(tesselator, GLU.GLU_TESS_EDGE_FLAG, adapter);
      GLU.gluTessCallback(tesselator, GLU.GLU_TESS_COMBINE, adapter);
      GLU.gluTessCallback(tesselator, GLU.GLU_TESS_ERROR, adapter);

      double[] segment = new double[6];

      GLU.gluBeginPolygon(tesselator);
      try {
        while (!pi.isDone()) {
          double[] xyz = new double[3];
          switch (pi.currentSegment(segment)) {
          case PathIterator.SEG_MOVETO:
            GLU.gluTessBeginContour(tesselator);
            //note: no break
          case PathIterator.SEG_LINETO:
            xyz[0] = segment[0];
            xyz[1] = segment[1];
            GLU.gluTessVertex(tesselator, xyz, 0, xyz);
            break;
          case PathIterator.SEG_CLOSE:
            GLU.gluTessEndContour(tesselator);
            break;

          case PathIterator.SEG_QUADTO:
            throw new RuntimeException("SEG_QUADTO: should not occur when shape.getPathIterator is passed a flatness argument");
          case PathIterator.SEG_CUBICTO:
            throw new RuntimeException("SEG_CUBICTO: should not occur when shape.getPathIterator is passed a flatness argument");
          default:
            throw new RuntimeException("unhandled segment: should not occur");
          }
          pi.next();
        }
      } finally {
        GLU.gluTessEndPolygon(tesselator);
      }
    } finally {
      GLU.gluDeleteTess(tesselator);
    }
  }

  void draw(Shape s) {
    Stroke currentStroke = graphics2D.getStrokeField();
    if (currentStroke instanceof BasicStroke basicStroke) {
      if (basicStroke.getDashArray() != null) {
        //todo
        graphics2D.renderContext.gl.glLineStipple(1, (short) 0x00FF);
        graphics2D.renderContext.gl.glEnable(GL_LINE_STIPPLE);
      }

      Shape outlinesShape = LINE_STROKE.createStrokedShape(s);
      PathIterator pi = outlinesShape.getPathIterator(null, FLATNESS);
      float[] segment = new float[6];
      graphics2D.renderContext.gl.glLineWidth(basicStroke.getLineWidth());
      try {
        while (!pi.isDone()) {
          switch (pi.currentSegment(segment)) {
          case PathIterator.SEG_MOVETO:
            graphics2D.renderContext.gl.glBegin(GL_LINE_STRIP);
            //note: no break
          case PathIterator.SEG_LINETO:
            graphics2D.renderContext.gl.glVertex2f(segment[0], segment[1]);
            break;
          case PathIterator.SEG_CLOSE:
            graphics2D.renderContext.gl.glEnd();
            break;

          case PathIterator.SEG_QUADTO:
            throw new RuntimeException("SEG_QUADTO: should not occur when shape.getPathIterator is passed a flatness argument");
          case PathIterator.SEG_CUBICTO:
            throw new RuntimeException("SEG_CUBICTO: should not occur when shape.getPathIterator is passed a flatness argument");
          default:
            throw new RuntimeException("unhandled segment: should not occur");
          }
          pi.next();
        }
      } finally {
        graphics2D.renderContext.gl.glDisable(GL_LINE_STIPPLE);
        graphics2D.renderContext.gl.glLineWidth(1);
      }
    } else {
      //todo: investigate
      Shape outlinesShape = currentStroke.createStrokedShape(s);
      PathIterator pi = outlinesShape.getPathIterator(null, FLATNESS);
      fill(pi);
    }
  }

  void fill(Shape s) {
    fill(s.getPathIterator(null, FLATNESS));
  }
}
