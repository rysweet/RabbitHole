package edu.cmu.cs.dennisc.render.gl.structural;



import edu.cmu.cs.dennisc.render.gl.ClassLoadingSweepSupport;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlrenderAdditionalClassLoadingSweepTest {



  @Test
  public void loadsRemainingGlrenderAndTextRenderingClasses() {
    ClassLoadingSweepSupport.SweepStats stats = ClassLoadingSweepSupport.sweepClasses(List.of(
        "edu.cmu.cs.dennisc.render.gl.Animator",
        "edu.cmu.cs.dennisc.render.gl.GlrOffscreenRenderTarget",
        "edu.cmu.cs.dennisc.render.gl.GlrOnscreenRenderTarget",
        "edu.cmu.cs.dennisc.render.gl.GlrRenderTarget",
        "edu.cmu.cs.dennisc.render.gl.TextureBinding",
        "edu.cmu.cs.dennisc.render.gl.imp.DisplayTask",
        "edu.cmu.cs.dennisc.render.gl.imp.GlImageRenderer",
        "edu.cmu.cs.dennisc.render.gl.imp.GlPrimitiveShapeRenderer",
        "edu.cmu.cs.dennisc.render.gl.imp.GlResourceCache",
        "edu.cmu.cs.dennisc.render.gl.imp.GlTessellationRenderer",
        "edu.cmu.cs.dennisc.render.gl.imp.GlTextRenderer",
        "edu.cmu.cs.dennisc.render.gl.imp.GlrAsynchronousImageCapturer",
        "edu.cmu.cs.dennisc.render.gl.imp.GlrAsynchronousPicker",
        "edu.cmu.cs.dennisc.render.gl.imp.GlrImageBuffer",
        "edu.cmu.cs.dennisc.render.gl.imp.Graphics2D",
        "edu.cmu.cs.dennisc.render.gl.imp.ImageCaptureDisplayTask",
        "edu.cmu.cs.dennisc.render.gl.imp.IsFrameBufferIntact",
        "edu.cmu.cs.dennisc.render.gl.imp.NativeOffscreenDrawable",
        "edu.cmu.cs.dennisc.render.gl.imp.OffscreenDrawable",
        "edu.cmu.cs.dennisc.render.gl.imp.PickAllDisplayTask",
        "edu.cmu.cs.dennisc.render.gl.imp.PickDisplayTask",
        "edu.cmu.cs.dennisc.render.gl.imp.PickFrontMostDisplayTask",
        "edu.cmu.cs.dennisc.render.gl.imp.Pixels",
        "edu.cmu.cs.dennisc.render.gl.imp.ReferencedObject",
        "edu.cmu.cs.dennisc.render.gl.imp.RenderTargetGlEventHandler",
        "edu.cmu.cs.dennisc.render.gl.imp.SelectionBufferInfo",
        "edu.cmu.cs.dennisc.render.gl.imp.SoftwareOffscreenDrawable",
        "edu.cmu.cs.dennisc.render.gl.imp.SynchronousPicker",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.SkeletonWeightProcessor",
        "edu.cmu.cs.dennisc.render.joglrenderer.CharSequenceIterator",
        "edu.cmu.cs.dennisc.render.joglrenderer.CharacterCache",
        "edu.cmu.cs.dennisc.render.joglrenderer.DebugListener",
        "edu.cmu.cs.dennisc.render.joglrenderer.Manager",
        "edu.cmu.cs.dennisc.render.joglrenderer.TextData",
        "edu.cmu.cs.dennisc.render.joglrenderer.TextRendererGlyph",
        "edu.cmu.cs.dennisc.render.joglrenderer.TextRendererGlyphProducer",
        "edu.cmu.cs.dennisc.render.joglrenderer.TextRendererPipeline",
        "edu.cmu.cs.dennisc.render.joglrenderer.TextRendererProperties",
        "edu.cmu.cs.dennisc.render.joglrenderer.TextRendererQuadRenderer",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrBubble",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrMainTitle",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrOvertitle",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrShapeEnclosedText",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrSpeechBubble",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrSubtitle",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrText",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrThoughtBubble",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.graphics.GlrTitle",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.adorn.GlrAdornment",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.adorn.GlrPivotFigure",
        "edu.cmu.cs.dennisc.render.gl.imp.adapters.adorn.GlrStickFigure"
    ));
    String summary = "attempted=" + stats.getAttemptedClassCount()
        + ", loaded=" + stats.getLoadedClassCount()
        + ", instantiated=" + stats.getInstantiatedClassCount()
        + ", enums=" + stats.getEnumExerciseCount()
        + ", staticFields=" + stats.getStaticFieldAccessCount()
        + ", staticMethods=" + stats.getStaticMethodCallCount()
        + ", failures=" + stats.getFailures();

    assertEquals(summary, 51, stats.getAttemptedClassCount());
    assertEquals(summary, stats.getAttemptedClassCount(), stats.getLoadedClassCount());
    assertTrue(summary, stats.getFailures().isEmpty());
    assertEquals(summary, 0, stats.getInstantiatedClassCount());
    assertEquals(summary, 0, stats.getEnumExerciseCount());
    assertEquals(summary, 0, stats.getStaticFieldAccessCount());
    assertEquals(summary, 0, stats.getStaticMethodCallCount());
  }
}
