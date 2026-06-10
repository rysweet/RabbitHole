package edu.cmu.cs.dennisc.render.gl.structural;



import edu.cmu.cs.dennisc.render.gl.ClassLoadingSweepSupport;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlrenderHotspotClassLoadingSweepTest {



  @Test
  public void loadsGlrenderCoverageHotspots() {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepClasses(List.of(
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrSkeletonVisual",
        "edu.cmu.cs.dennisc.render.gl.imp.SynchronousPicker$ActualPicker",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrOldMesh",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrThoughtBubble",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrVisual",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrScene",
        "edu.cmu.cs.dennisc.render.gl.imp.GlTessellationRenderer",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrAbstractTransformable",
        "edu.cmu.cs.dennisc.render.gl.imp.RenderContext",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrSpeechBubble",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrIndexedPolygonArray",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrScalable",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrPlanarReflector",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrCustomTexture",
        "edu.cmu.cs.dennisc.render.gl.imp.Graphics2D",
        "edu.cmu.cs.dennisc.render.gl.imp.RenderTargetImp",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.GlrTexturedAppearance",
        "edu.cmu.cs.dennisc.render.gl.GlrRenderFactory"
    ));
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", failures=" + stats.getFailures();

    assertEquals(summary, 18, stats.getAttemptedClassCount());
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
  }
}
