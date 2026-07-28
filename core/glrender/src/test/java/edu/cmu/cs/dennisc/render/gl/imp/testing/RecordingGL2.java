package edu.cmu.cs.dennisc.render.gl.imp.testing;

public class RecordingGL2 implements com.jogamp.opengl.GL2 {
  private final java.util.List<Call> calls = new java.util.ArrayList<>();
  private final java.util.Deque<Integer> errors = new java.util.ArrayDeque<>();

  public void addError(int error) { this.errors.addLast(error); }

  public java.util.List<Call> calls(String methodName) {
    java.util.List<Call> matching = new java.util.ArrayList<>();
    for (Call call : this.calls) { if (call.methodName.equals(methodName)) { matching.add(call); } }
    return matching;
  }

  public boolean wasCalledWith(String methodName, Object... expectedArgs) {
    for (Call call : calls(methodName)) { if (call.matches(expectedArgs)) { return true; } }
    return false;
  }

  public java.util.List<float[]> floatArrayCalls(String methodName) {
    java.util.List<float[]> values = new java.util.ArrayList<>();
    for (Call call : calls(methodName)) { values.add(call.floatArrayArgument()); }
    return values;
  }

  public float[] singleFloatArrayCall(String methodName) {
    java.util.List<float[]> values = floatArrayCalls(methodName);
    if (values.size() != 1) { throw new AssertionError("Expected one call for " + methodName + " but was " + values.size()); }
    return values.get(0);
  }

  public float[] lastFloatArrayCall(String methodName) {
    java.util.List<float[]> values = floatArrayCalls(methodName);
    return values.get(values.size() - 1);
  }

  public float[] singleFloatArgs(String methodName) {
    java.util.List<Call> matching = calls(methodName);
    if (matching.size() != 1) { throw new AssertionError("Expected one call for " + methodName + " but was " + matching.size()); }
    return matching.get(0).floatArgs();
  }

  public float[] lastFloatArgs(String methodName) {
    java.util.List<Call> matching = calls(methodName);
    return matching.get(matching.size() - 1).floatArgs();
  }

  public double[] lastDoubleArgs(String methodName) {
    java.util.List<Call> matching = calls(methodName);
    return matching.get(matching.size() - 1).doubleArgs();
  }

  public double[] doubleBufferArgument(String methodName) {
    Call call = calls(methodName).get(0);
    java.nio.DoubleBuffer buffer = ((java.nio.DoubleBuffer) call.args[0]).duplicate();
    buffer.position(0);
    double[] values = new double[buffer.remaining()];
    buffer.get(values);
    return values;
  }

  public float[] floatArrayArgument(String methodName, int secondArgument) {
    for (Call call : calls(methodName)) {
      if (((Number) call.args[1]).intValue() == secondArgument) {
        java.nio.FloatBuffer buffer = ((java.nio.FloatBuffer) call.args[2]).duplicate();
        buffer.position(0);
        float[] values = new float[buffer.remaining()];
        buffer.get(values);
        return values;
      }
    }
    throw new AssertionError("No matching call for " + methodName + " and " + secondArgument);
  }

  private void record(String methodName, Object[] args) { this.calls.add(new Call(methodName, cloneArgs(args))); }

  private static Object[] cloneArgs(Object[] args) {
    Object[] cloned = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      cloned[i] = cloneArg(args[i]);
    }
    return cloned;
  }

  private static Object cloneArg(Object arg) {
    if (arg instanceof java.nio.FloatBuffer buffer) {
      java.nio.FloatBuffer duplicate = buffer.duplicate();
      duplicate.position(0);
      java.nio.FloatBuffer copy = java.nio.FloatBuffer.allocate(duplicate.remaining());
      copy.put(duplicate);
      copy.flip();
      return copy;
    }
    if (arg instanceof java.nio.DoubleBuffer buffer) {
      java.nio.DoubleBuffer duplicate = buffer.duplicate();
      duplicate.position(0);
      java.nio.DoubleBuffer copy = java.nio.DoubleBuffer.allocate(duplicate.remaining());
      copy.put(duplicate);
      copy.flip();
      return copy;
    }
    return arg;
  }

  @Override public String toString() { return "RecordingGL2"; }
  @Override public int hashCode() { return System.identityHashCode(this); }
  @Override public boolean equals(Object obj) { return this == obj; }

  public static final class Call {
    private final String methodName;
    private final Object[] args;

    private Call(String methodName, Object[] args) { this.methodName = methodName; this.args = args; }

    private boolean matches(Object... expectedArgs) {
      if (this.args.length != expectedArgs.length) { return false; }
      for (int i = 0; i < expectedArgs.length; i++) {
        Object actual = this.args[i];
        Object expected = expectedArgs[i];
        if (actual instanceof Number && expected instanceof Number) {
          if (Double.compare(((Number) actual).doubleValue(), ((Number) expected).doubleValue()) != 0) { return false; }
        } else if ((actual == null && expected != null) || (actual != null && !actual.equals(expected))) {
          return false;
        }
      }
      return true;
    }

    private float[] floatArrayArgument() {
      for (Object arg : this.args) {
        if (arg instanceof java.nio.FloatBuffer buffer) {
          java.nio.FloatBuffer duplicate = buffer.duplicate();
          duplicate.position(0);
          float[] values = new float[duplicate.remaining()];
          duplicate.get(values);
          return values;
        }
      }
      throw new AssertionError("No FloatBuffer argument recorded for " + this.methodName);
    }

    private float[] floatArgs() {
      float[] values = new float[this.args.length];
      for (int i = 0; i < this.args.length; i++) { values[i] = ((Number) this.args[i]).floatValue(); }
      return values;
    }

    private double[] doubleArgs() {
      double[] values = new double[this.args.length];
      for (int i = 0; i < this.args.length; i++) { values[i] = ((Number) this.args[i]).doubleValue(); }
      return values;
    }
  }

  // Generated from com.jogamp.opengl.GL2; run GenerateRecordingGL2 after JOGL upgrades.
  @Override public int getBoundBuffer(int p0) {
    record("getBoundBuffer", new Object[] {p0});
    return 0;
  }

  @Override public int getBoundFramebuffer(int p0) {
    record("getBoundFramebuffer", new Object[] {p0});
    return 0;
  }

  @Override public com.jogamp.opengl.GLBufferStorage getBufferStorage(int p0) {
    record("getBufferStorage", new Object[] {p0});
    return null;
  }

  @Override public com.jogamp.opengl.GLContext getContext() {
    record("getContext", new Object[] {});
    return null;
  }

  @Override public int getDefaultDrawBuffer() {
    record("getDefaultDrawBuffer", new Object[] {});
    return 0;
  }

  @Override public int getDefaultDrawFramebuffer() {
    record("getDefaultDrawFramebuffer", new Object[] {});
    return 0;
  }

  @Override public int getDefaultReadBuffer() {
    record("getDefaultReadBuffer", new Object[] {});
    return 0;
  }

  @Override public int getDefaultReadFramebuffer() {
    record("getDefaultReadFramebuffer", new Object[] {});
    return 0;
  }

  @Override public com.jogamp.opengl.GL getDownstreamGL() {
    record("getDownstreamGL", new Object[] {});
    return null;
  }

  @Override public java.lang.Object getExtension(java.lang.String p0) {
    record("getExtension", new Object[] {p0});
    return null;
  }

  @Override public com.jogamp.opengl.GL getGL() {
    record("getGL", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL2 getGL2() {
    record("getGL2", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL2ES1 getGL2ES1() {
    record("getGL2ES1", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL2ES2 getGL2ES2() {
    record("getGL2ES2", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL2ES3 getGL2ES3() {
    record("getGL2ES3", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL2GL3 getGL2GL3() {
    record("getGL2GL3", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL3 getGL3() {
    record("getGL3", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL3ES3 getGL3ES3() {
    record("getGL3ES3", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL3bc getGL3bc() {
    record("getGL3bc", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL4 getGL4() {
    record("getGL4", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL4ES3 getGL4ES3() {
    record("getGL4ES3", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL4bc getGL4bc() {
    record("getGL4bc", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GLES1 getGLES1() {
    record("getGLES1", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GLES2 getGLES2() {
    record("getGLES2", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GLES3 getGLES3() {
    record("getGLES3", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GLProfile getGLProfile() {
    record("getGLProfile", new Object[] {});
    return null;
  }

  @Override public int getMaxRenderbufferSamples() {
    record("getMaxRenderbufferSamples", new Object[] {});
    return 0;
  }

  @Override public java.lang.Object getPlatformGLExtensions() {
    record("getPlatformGLExtensions", new Object[] {});
    return null;
  }

  @Override public com.jogamp.opengl.GL getRootGL() {
    record("getRootGL", new Object[] {});
    return null;
  }

  @Override public int getSwapInterval() {
    record("getSwapInterval", new Object[] {});
    return 0;
  }

  @Override public void glAccum(int p0, float p1) {
    record("glAccum", new Object[] {p0, p1});
  }

  @Override public boolean glAcquireKeyedMutexWin32EXT(int p0, long p1, int p2) {
    record("glAcquireKeyedMutexWin32EXT", new Object[] {p0, p1, p2});
    return false;
  }

  @Override public void glActiveShaderProgram(int p0, int p1) {
    record("glActiveShaderProgram", new Object[] {p0, p1});
  }

  @Override public void glActiveStencilFaceEXT(int p0) {
    record("glActiveStencilFaceEXT", new Object[] {p0});
  }

  @Override public void glActiveTexture(int p0) {
    record("glActiveTexture", new Object[] {p0});
  }

  @Override public void glAlphaFunc(int p0, float p1) {
    record("glAlphaFunc", new Object[] {p0, p1});
  }

  @Override public void glAlphaToCoverageDitherControlNV(int p0) {
    record("glAlphaToCoverageDitherControlNV", new Object[] {p0});
  }

  @Override public void glApplyFramebufferAttachmentCMAAINTEL() {
    record("glApplyFramebufferAttachmentCMAAINTEL", new Object[] {});
  }

  @Override public void glApplyTextureEXT(int p0) {
    record("glApplyTextureEXT", new Object[] {p0});
  }

  @Override public boolean glAreTexturesResident(int p0, int[] p1, int p2, byte[] p3, int p4) {
    record("glAreTexturesResident", new Object[] {p0, p1, p2, p3, p4});
    return false;
  }

  @Override public boolean glAreTexturesResident(int p0, java.nio.IntBuffer p1, java.nio.ByteBuffer p2) {
    record("glAreTexturesResident", new Object[] {p0, p1, p2});
    return false;
  }

  @Override public void glArrayElement(int p0) {
    record("glArrayElement", new Object[] {p0});
  }

  @Override public int glAsyncCopyBufferSubDataNVX(int p0, int[] p1, int p2, long[] p3, int p4, int p5, int p6, int p7, int p8, long p9, long p10, long p11, int p12, int[] p13, int p14, long[] p15, int p16) {
    record("glAsyncCopyBufferSubDataNVX", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16});
    return 0;
  }

  @Override public int glAsyncCopyBufferSubDataNVX(int p0, java.nio.IntBuffer p1, java.nio.LongBuffer p2, int p3, int p4, int p5, int p6, long p7, long p8, long p9, int p10, java.nio.IntBuffer p11, java.nio.LongBuffer p12) {
    record("glAsyncCopyBufferSubDataNVX", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12});
    return 0;
  }

  @Override public int glAsyncCopyImageSubDataNVX(int p0, int[] p1, int p2, long[] p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12, int p13, int p14, int p15, int p16, int p17, int p18, int p19, int p20, int p21, int p22, int[] p23, int p24, long[] p25, int p26) {
    record("glAsyncCopyImageSubDataNVX", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26});
    return 0;
  }

  @Override public int glAsyncCopyImageSubDataNVX(int p0, java.nio.IntBuffer p1, java.nio.LongBuffer p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12, int p13, int p14, int p15, int p16, int p17, int p18, int p19, int p20, java.nio.IntBuffer p21, java.nio.LongBuffer p22) {
    record("glAsyncCopyImageSubDataNVX", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22});
    return 0;
  }

  @Override public void glAttachObjectARB(long p0, long p1) {
    record("glAttachObjectARB", new Object[] {p0, p1});
  }

  @Override public void glAttachShader(int p0, int p1) {
    record("glAttachShader", new Object[] {p0, p1});
  }

  @Override public void glBegin(int p0) {
    record("glBegin", new Object[] {p0});
  }

  @Override public void glBeginConditionalRender(int p0, int p1) {
    record("glBeginConditionalRender", new Object[] {p0, p1});
  }

  @Override public void glBeginConditionalRenderNVX(int p0) {
    record("glBeginConditionalRenderNVX", new Object[] {p0});
  }

  @Override public void glBeginOcclusionQueryNV(int p0) {
    record("glBeginOcclusionQueryNV", new Object[] {p0});
  }

  @Override public void glBeginPerfQueryINTEL(int p0) {
    record("glBeginPerfQueryINTEL", new Object[] {p0});
  }

  @Override public void glBeginQuery(int p0, int p1) {
    record("glBeginQuery", new Object[] {p0, p1});
  }

  @Override public void glBeginQueryIndexed(int p0, int p1, int p2) {
    record("glBeginQueryIndexed", new Object[] {p0, p1, p2});
  }

  @Override public void glBeginTransformFeedback(int p0) {
    record("glBeginTransformFeedback", new Object[] {p0});
  }

  @Override public void glBeginVertexShaderEXT() {
    record("glBeginVertexShaderEXT", new Object[] {});
  }

  @Override public void glBeginVideoCaptureNV(int p0) {
    record("glBeginVideoCaptureNV", new Object[] {p0});
  }

  @Override public void glBindAttribLocation(int p0, int p1, java.lang.String p2) {
    record("glBindAttribLocation", new Object[] {p0, p1, p2});
  }

  @Override public void glBindBuffer(int p0, int p1) {
    record("glBindBuffer", new Object[] {p0, p1});
  }

  @Override public void glBindBufferBase(int p0, int p1, int p2) {
    record("glBindBufferBase", new Object[] {p0, p1, p2});
  }

  @Override public void glBindBufferRange(int p0, int p1, int p2, long p3, long p4) {
    record("glBindBufferRange", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glBindFragDataLocation(int p0, int p1, java.lang.String p2) {
    record("glBindFragDataLocation", new Object[] {p0, p1, p2});
  }

  @Override public void glBindFramebuffer(int p0, int p1) {
    record("glBindFramebuffer", new Object[] {p0, p1});
  }

  @Override public void glBindImageTexture(int p0, int p1, int p2, boolean p3, int p4, int p5, int p6) {
    record("glBindImageTexture", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public int glBindLightParameterEXT(int p0, int p1) {
    record("glBindLightParameterEXT", new Object[] {p0, p1});
    return 0;
  }

  @Override public int glBindMaterialParameterEXT(int p0, int p1) {
    record("glBindMaterialParameterEXT", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glBindMultiTextureEXT(int p0, int p1, int p2) {
    record("glBindMultiTextureEXT", new Object[] {p0, p1, p2});
  }

  @Override public int glBindParameterEXT(int p0) {
    record("glBindParameterEXT", new Object[] {p0});
    return 0;
  }

  @Override public void glBindProgramARB(int p0, int p1) {
    record("glBindProgramARB", new Object[] {p0, p1});
  }

  @Override public void glBindProgramPipeline(int p0) {
    record("glBindProgramPipeline", new Object[] {p0});
  }

  @Override public void glBindRenderbuffer(int p0, int p1) {
    record("glBindRenderbuffer", new Object[] {p0, p1});
  }

  @Override public int glBindTexGenParameterEXT(int p0, int p1, int p2) {
    record("glBindTexGenParameterEXT", new Object[] {p0, p1, p2});
    return 0;
  }

  @Override public void glBindTexture(int p0, int p1) {
    record("glBindTexture", new Object[] {p0, p1});
  }

  @Override public int glBindTextureUnitParameterEXT(int p0, int p1) {
    record("glBindTextureUnitParameterEXT", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glBindTransformFeedback(int p0, int p1) {
    record("glBindTransformFeedback", new Object[] {p0, p1});
  }

  @Override public void glBindTransformFeedbackNV(int p0, int p1) {
    record("glBindTransformFeedbackNV", new Object[] {p0, p1});
  }

  @Override public void glBindVertexArray(int p0) {
    record("glBindVertexArray", new Object[] {p0});
  }

  @Override public void glBindVertexShaderEXT(int p0) {
    record("glBindVertexShaderEXT", new Object[] {p0});
  }

  @Override public void glBindVideoCaptureStreamBufferNV(int p0, int p1, int p2, long p3) {
    record("glBindVideoCaptureStreamBufferNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBindVideoCaptureStreamTextureNV(int p0, int p1, int p2, int p3, int p4) {
    record("glBindVideoCaptureStreamTextureNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glBitmap(int p0, int p1, float p2, float p3, float p4, float p5, byte[] p6, int p7) {
    record("glBitmap", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glBitmap(int p0, int p1, float p2, float p3, float p4, float p5, java.nio.ByteBuffer p6) {
    record("glBitmap", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glBitmap(int p0, int p1, float p2, float p3, float p4, float p5, long p6) {
    record("glBitmap", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glBlendBarrier() {
    record("glBlendBarrier", new Object[] {});
  }

  @Override public void glBlendColor(float p0, float p1, float p2, float p3) {
    record("glBlendColor", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBlendEquation(int p0) {
    record("glBlendEquation", new Object[] {p0});
  }

  @Override public void glBlendEquationIndexedAMD(int p0, int p1) {
    record("glBlendEquationIndexedAMD", new Object[] {p0, p1});
  }

  @Override public void glBlendEquationSeparate(int p0, int p1) {
    record("glBlendEquationSeparate", new Object[] {p0, p1});
  }

  @Override public void glBlendEquationSeparateIndexedAMD(int p0, int p1, int p2) {
    record("glBlendEquationSeparateIndexedAMD", new Object[] {p0, p1, p2});
  }

  @Override public void glBlendEquationSeparatei(int p0, int p1, int p2) {
    record("glBlendEquationSeparatei", new Object[] {p0, p1, p2});
  }

  @Override public void glBlendEquationi(int p0, int p1) {
    record("glBlendEquationi", new Object[] {p0, p1});
  }

  @Override public void glBlendFunc(int p0, int p1) {
    record("glBlendFunc", new Object[] {p0, p1});
  }

  @Override public void glBlendFuncIndexedAMD(int p0, int p1, int p2) {
    record("glBlendFuncIndexedAMD", new Object[] {p0, p1, p2});
  }

  @Override public void glBlendFuncSeparate(int p0, int p1, int p2, int p3) {
    record("glBlendFuncSeparate", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBlendFuncSeparateIndexedAMD(int p0, int p1, int p2, int p3, int p4) {
    record("glBlendFuncSeparateIndexedAMD", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glBlendFuncSeparatei(int p0, int p1, int p2, int p3, int p4) {
    record("glBlendFuncSeparatei", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glBlendFunci(int p0, int p1, int p2) {
    record("glBlendFunci", new Object[] {p0, p1, p2});
  }

  @Override public void glBlitFramebuffer(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) {
    record("glBlitFramebuffer", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glBlitFramebufferLayerEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11) {
    record("glBlitFramebufferLayerEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glBlitFramebufferLayersEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) {
    record("glBlitFramebufferLayersEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glBufferAddressRangeNV(int p0, int p1, long p2, long p3) {
    record("glBufferAddressRangeNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBufferData(int p0, long p1, java.nio.Buffer p2, int p3) {
    record("glBufferData", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBufferPageCommitmentARB(int p0, long p1, long p2, boolean p3) {
    record("glBufferPageCommitmentARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBufferParameteri(int p0, int p1, int p2) {
    record("glBufferParameteri", new Object[] {p0, p1, p2});
  }

  @Override public void glBufferStorageExternalEXT(int p0, long p1, long p2, java.nio.Buffer p3, int p4) {
    record("glBufferStorageExternalEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glBufferStorageMemEXT(int p0, long p1, int p2, long p3) {
    record("glBufferStorageMemEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glBufferSubData(int p0, long p1, long p2, java.nio.Buffer p3) {
    record("glBufferSubData", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glCallCommandListNV(int p0) {
    record("glCallCommandListNV", new Object[] {p0});
  }

  @Override public void glCallList(int p0) {
    record("glCallList", new Object[] {p0});
  }

  @Override public void glCallLists(int p0, int p1, java.nio.Buffer p2) {
    record("glCallLists", new Object[] {p0, p1, p2});
  }

  @Override public int glCheckFramebufferStatus(int p0) {
    record("glCheckFramebufferStatus", new Object[] {p0});
    return 0;
  }

  @Override public int glCheckNamedFramebufferStatusEXT(int p0, int p1) {
    record("glCheckNamedFramebufferStatusEXT", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glClampColor(int p0, int p1) {
    record("glClampColor", new Object[] {p0, p1});
  }

  @Override public void glClear(int p0) {
    record("glClear", new Object[] {p0});
  }

  @Override public void glClearAccum(float p0, float p1, float p2, float p3) {
    record("glClearAccum", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearBufferData(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glClearBufferData", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glClearBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, java.nio.Buffer p6) {
    record("glClearBufferSubData", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glClearBufferfi(int p0, int p1, float p2, int p3) {
    record("glClearBufferfi", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearBufferfv(int p0, int p1, float[] p2, int p3) {
    record("glClearBufferfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearBufferfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glClearBufferfv", new Object[] {p0, p1, p2});
  }

  @Override public void glClearBufferiv(int p0, int p1, int[] p2, int p3) {
    record("glClearBufferiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearBufferiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glClearBufferiv", new Object[] {p0, p1, p2});
  }

  @Override public void glClearBufferuiv(int p0, int p1, int[] p2, int p3) {
    record("glClearBufferuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearBufferuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glClearBufferuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glClearColor(float p0, float p1, float p2, float p3) {
    record("glClearColor", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearColorIi(int p0, int p1, int p2, int p3) {
    record("glClearColorIi", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearColorIui(int p0, int p1, int p2, int p3) {
    record("glClearColorIui", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glClearDepth(double p0) {
    record("glClearDepth", new Object[] {p0});
  }

  @Override public void glClearDepthf(float p0) {
    record("glClearDepthf", new Object[] {p0});
  }

  @Override public void glClearIndex(float p0) {
    record("glClearIndex", new Object[] {p0});
  }

  @Override public void glClearNamedBufferData(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glClearNamedBufferData", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, java.nio.Buffer p6) {
    record("glClearNamedBufferSubData", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glClearStencil(int p0) {
    record("glClearStencil", new Object[] {p0});
  }

  @Override public void glClientActiveTexture(int p0) {
    record("glClientActiveTexture", new Object[] {p0});
  }

  @Override public void glClientAttribDefaultEXT(int p0) {
    record("glClientAttribDefaultEXT", new Object[] {p0});
  }

  @Override public void glClientWaitSemaphoreui64NVX(int p0, int[] p1, int p2, long[] p3, int p4) {
    record("glClientWaitSemaphoreui64NVX", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glClientWaitSemaphoreui64NVX(int p0, java.nio.IntBuffer p1, java.nio.LongBuffer p2) {
    record("glClientWaitSemaphoreui64NVX", new Object[] {p0, p1, p2});
  }

  @Override public void glClipPlane(int p0, double[] p1, int p2) {
    record("glClipPlane", new Object[] {p0, p1, p2});
  }

  @Override public void glClipPlane(int p0, java.nio.DoubleBuffer p1) {
    record("glClipPlane", new Object[] {p0, p1});
  }

  @Override public void glClipPlanef(int p0, float[] p1, int p2) {
    record("glClipPlanef", new Object[] {p0, p1, p2});
  }

  @Override public void glClipPlanef(int p0, java.nio.FloatBuffer p1) {
    record("glClipPlanef", new Object[] {p0, p1});
  }

  @Override public void glColor3b(byte p0, byte p1, byte p2) {
    record("glColor3b", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3bv(byte[] p0, int p1) {
    record("glColor3bv", new Object[] {p0, p1});
  }

  @Override public void glColor3bv(java.nio.ByteBuffer p0) {
    record("glColor3bv", new Object[] {p0});
  }

  @Override public void glColor3d(double p0, double p1, double p2) {
    record("glColor3d", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3dv(double[] p0, int p1) {
    record("glColor3dv", new Object[] {p0, p1});
  }

  @Override public void glColor3dv(java.nio.DoubleBuffer p0) {
    record("glColor3dv", new Object[] {p0});
  }

  @Override public void glColor3f(float p0, float p1, float p2) {
    record("glColor3f", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3fv(float[] p0, int p1) {
    record("glColor3fv", new Object[] {p0, p1});
  }

  @Override public void glColor3fv(java.nio.FloatBuffer p0) {
    record("glColor3fv", new Object[] {p0});
  }

  @Override public void glColor3h(short p0, short p1, short p2) {
    record("glColor3h", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3hv(java.nio.ShortBuffer p0) {
    record("glColor3hv", new Object[] {p0});
  }

  @Override public void glColor3hv(short[] p0, int p1) {
    record("glColor3hv", new Object[] {p0, p1});
  }

  @Override public void glColor3i(int p0, int p1, int p2) {
    record("glColor3i", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3iv(int[] p0, int p1) {
    record("glColor3iv", new Object[] {p0, p1});
  }

  @Override public void glColor3iv(java.nio.IntBuffer p0) {
    record("glColor3iv", new Object[] {p0});
  }

  @Override public void glColor3s(short p0, short p1, short p2) {
    record("glColor3s", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3sv(java.nio.ShortBuffer p0) {
    record("glColor3sv", new Object[] {p0});
  }

  @Override public void glColor3sv(short[] p0, int p1) {
    record("glColor3sv", new Object[] {p0, p1});
  }

  @Override public void glColor3ub(byte p0, byte p1, byte p2) {
    record("glColor3ub", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3ubv(byte[] p0, int p1) {
    record("glColor3ubv", new Object[] {p0, p1});
  }

  @Override public void glColor3ubv(java.nio.ByteBuffer p0) {
    record("glColor3ubv", new Object[] {p0});
  }

  @Override public void glColor3ui(int p0, int p1, int p2) {
    record("glColor3ui", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3uiv(int[] p0, int p1) {
    record("glColor3uiv", new Object[] {p0, p1});
  }

  @Override public void glColor3uiv(java.nio.IntBuffer p0) {
    record("glColor3uiv", new Object[] {p0});
  }

  @Override public void glColor3us(short p0, short p1, short p2) {
    record("glColor3us", new Object[] {p0, p1, p2});
  }

  @Override public void glColor3usv(java.nio.ShortBuffer p0) {
    record("glColor3usv", new Object[] {p0});
  }

  @Override public void glColor3usv(short[] p0, int p1) {
    record("glColor3usv", new Object[] {p0, p1});
  }

  @Override public void glColor4b(byte p0, byte p1, byte p2, byte p3) {
    record("glColor4b", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4bv(byte[] p0, int p1) {
    record("glColor4bv", new Object[] {p0, p1});
  }

  @Override public void glColor4bv(java.nio.ByteBuffer p0) {
    record("glColor4bv", new Object[] {p0});
  }

  @Override public void glColor4d(double p0, double p1, double p2, double p3) {
    record("glColor4d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4dv(double[] p0, int p1) {
    record("glColor4dv", new Object[] {p0, p1});
  }

  @Override public void glColor4dv(java.nio.DoubleBuffer p0) {
    record("glColor4dv", new Object[] {p0});
  }

  @Override public void glColor4f(float p0, float p1, float p2, float p3) {
    record("glColor4f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4fv(float[] p0, int p1) {
    record("glColor4fv", new Object[] {p0, p1});
  }

  @Override public void glColor4fv(java.nio.FloatBuffer p0) {
    record("glColor4fv", new Object[] {p0});
  }

  @Override public void glColor4h(short p0, short p1, short p2, short p3) {
    record("glColor4h", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4hv(java.nio.ShortBuffer p0) {
    record("glColor4hv", new Object[] {p0});
  }

  @Override public void glColor4hv(short[] p0, int p1) {
    record("glColor4hv", new Object[] {p0, p1});
  }

  @Override public void glColor4i(int p0, int p1, int p2, int p3) {
    record("glColor4i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4iv(int[] p0, int p1) {
    record("glColor4iv", new Object[] {p0, p1});
  }

  @Override public void glColor4iv(java.nio.IntBuffer p0) {
    record("glColor4iv", new Object[] {p0});
  }

  @Override public void glColor4s(short p0, short p1, short p2, short p3) {
    record("glColor4s", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4sv(java.nio.ShortBuffer p0) {
    record("glColor4sv", new Object[] {p0});
  }

  @Override public void glColor4sv(short[] p0, int p1) {
    record("glColor4sv", new Object[] {p0, p1});
  }

  @Override public void glColor4ub(byte p0, byte p1, byte p2, byte p3) {
    record("glColor4ub", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4ubv(byte[] p0, int p1) {
    record("glColor4ubv", new Object[] {p0, p1});
  }

  @Override public void glColor4ubv(java.nio.ByteBuffer p0) {
    record("glColor4ubv", new Object[] {p0});
  }

  @Override public void glColor4ui(int p0, int p1, int p2, int p3) {
    record("glColor4ui", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4uiv(int[] p0, int p1) {
    record("glColor4uiv", new Object[] {p0, p1});
  }

  @Override public void glColor4uiv(java.nio.IntBuffer p0) {
    record("glColor4uiv", new Object[] {p0});
  }

  @Override public void glColor4us(short p0, short p1, short p2, short p3) {
    record("glColor4us", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColor4usv(java.nio.ShortBuffer p0) {
    record("glColor4usv", new Object[] {p0});
  }

  @Override public void glColor4usv(short[] p0, int p1) {
    record("glColor4usv", new Object[] {p0, p1});
  }

  @Override public void glColorFormatNV(int p0, int p1, int p2) {
    record("glColorFormatNV", new Object[] {p0, p1, p2});
  }

  @Override public void glColorMask(boolean p0, boolean p1, boolean p2, boolean p3) {
    record("glColorMask", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColorMaskIndexed(int p0, boolean p1, boolean p2, boolean p3, boolean p4) {
    record("glColorMaskIndexed", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glColorMaski(int p0, boolean p1, boolean p2, boolean p3, boolean p4) {
    record("glColorMaski", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glColorMaterial(int p0, int p1) {
    record("glColorMaterial", new Object[] {p0, p1});
  }

  @Override public void glColorPointer(com.jogamp.opengl.GLArrayData p0) {
    record("glColorPointer", new Object[] {p0});
  }

  @Override public void glColorPointer(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glColorPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColorPointer(int p0, int p1, int p2, long p3) {
    record("glColorPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColorSubTable(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glColorSubTable", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glColorSubTable(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glColorSubTable", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glColorTable(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glColorTable", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glColorTable(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glColorTable", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glColorTableParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glColorTableParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColorTableParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glColorTableParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glColorTableParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glColorTableParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glColorTableParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glColorTableParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glCommandListSegmentsNV(int p0, int p1) {
    record("glCommandListSegmentsNV", new Object[] {p0, p1});
  }

  @Override public void glCompileCommandListNV(int p0) {
    record("glCompileCommandListNV", new Object[] {p0});
  }

  @Override public void glCompileShader(int p0) {
    record("glCompileShader", new Object[] {p0});
  }

  @Override public void glCompileShaderARB(long p0) {
    record("glCompileShaderARB", new Object[] {p0});
  }

  @Override public void glCompressedMultiTexImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glCompressedMultiTexImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCompressedMultiTexImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glCompressedMultiTexImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCompressedMultiTexImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glCompressedMultiTexImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glCompressedMultiTexSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glCompressedMultiTexSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCompressedMultiTexSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glCompressedMultiTexSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glCompressedMultiTexSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, java.nio.Buffer p11) {
    record("glCompressedMultiTexSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glCompressedTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.Buffer p6) {
    record("glCompressedTexImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCompressedTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glCompressedTexImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCompressedTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glCompressedTexImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCompressedTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {
    record("glCompressedTexImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCompressedTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glCompressedTexImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCompressedTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {
    record("glCompressedTexImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCompressedTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.Buffer p6) {
    record("glCompressedTexSubImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCompressedTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glCompressedTexSubImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCompressedTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glCompressedTexSubImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCompressedTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {
    record("glCompressedTexSubImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCompressedTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.Buffer p10) {
    record("glCompressedTexSubImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glCompressedTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {
    record("glCompressedTexSubImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glCompressedTextureImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glCompressedTextureImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCompressedTextureImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glCompressedTextureImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCompressedTextureImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glCompressedTextureImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glCompressedTextureSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glCompressedTextureSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCompressedTextureSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glCompressedTextureSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glCompressedTextureSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, java.nio.Buffer p11) {
    record("glCompressedTextureSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glConservativeRasterParameterfNV(int p0, float p1) {
    record("glConservativeRasterParameterfNV", new Object[] {p0, p1});
  }

  @Override public void glConservativeRasterParameteriNV(int p0, int p1) {
    record("glConservativeRasterParameteriNV", new Object[] {p0, p1});
  }

  @Override public void glConvolutionFilter1D(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glConvolutionFilter1D", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glConvolutionFilter1D(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glConvolutionFilter1D", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glConvolutionFilter2D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.Buffer p6) {
    record("glConvolutionFilter2D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glConvolutionFilter2D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glConvolutionFilter2D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glConvolutionParameterf(int p0, int p1, float p2) {
    record("glConvolutionParameterf", new Object[] {p0, p1, p2});
  }

  @Override public void glConvolutionParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glConvolutionParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glConvolutionParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glConvolutionParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glConvolutionParameteri(int p0, int p1, int p2) {
    record("glConvolutionParameteri", new Object[] {p0, p1, p2});
  }

  @Override public void glConvolutionParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glConvolutionParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glConvolutionParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glConvolutionParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glCopyBufferSubData(int p0, int p1, long p2, long p3, long p4) {
    record("glCopyBufferSubData", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glCopyColorSubTable(int p0, int p1, int p2, int p3, int p4) {
    record("glCopyColorSubTable", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glCopyColorTable(int p0, int p1, int p2, int p3, int p4) {
    record("glCopyColorTable", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glCopyConvolutionFilter1D(int p0, int p1, int p2, int p3, int p4) {
    record("glCopyConvolutionFilter1D", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glCopyConvolutionFilter2D(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glCopyConvolutionFilter2D", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glCopyImageSubData(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12, int p13, int p14) {
    record("glCopyImageSubData", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14});
  }

  @Override public void glCopyImageSubDataNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12, int p13, int p14) {
    record("glCopyImageSubDataNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14});
  }

  @Override public void glCopyMultiTexImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    record("glCopyMultiTexImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCopyMultiTexImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    record("glCopyMultiTexImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCopyMultiTexSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glCopyMultiTexSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCopyMultiTexSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    record("glCopyMultiTexSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCopyMultiTexSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) {
    record("glCopyMultiTexSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glCopyPixels(int p0, int p1, int p2, int p3, int p4) {
    record("glCopyPixels", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glCopyTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glCopyTexImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCopyTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    record("glCopyTexImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCopyTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glCopyTexSubImage1D", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glCopyTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    record("glCopyTexSubImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCopyTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    record("glCopyTexSubImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCopyTextureImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    record("glCopyTextureImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glCopyTextureImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    record("glCopyTextureImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCopyTextureSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glCopyTextureSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glCopyTextureSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    record("glCopyTextureSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glCopyTextureSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) {
    record("glCopyTextureSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glCoverageModulationNV(int p0) {
    record("glCoverageModulationNV", new Object[] {p0});
  }

  @Override public void glCoverageModulationTableNV(int p0, float[] p1, int p2) {
    record("glCoverageModulationTableNV", new Object[] {p0, p1, p2});
  }

  @Override public void glCoverageModulationTableNV(int p0, java.nio.FloatBuffer p1) {
    record("glCoverageModulationTableNV", new Object[] {p0, p1});
  }

  @Override public void glCreateCommandListsNV(int p0, int[] p1, int p2) {
    record("glCreateCommandListsNV", new Object[] {p0, p1, p2});
  }

  @Override public void glCreateCommandListsNV(int p0, java.nio.IntBuffer p1) {
    record("glCreateCommandListsNV", new Object[] {p0, p1});
  }

  @Override public void glCreateMemoryObjectsEXT(int p0, int[] p1, int p2) {
    record("glCreateMemoryObjectsEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glCreateMemoryObjectsEXT(int p0, java.nio.IntBuffer p1) {
    record("glCreateMemoryObjectsEXT", new Object[] {p0, p1});
  }

  @Override public void glCreatePerfQueryINTEL(int p0, int[] p1, int p2) {
    record("glCreatePerfQueryINTEL", new Object[] {p0, p1, p2});
  }

  @Override public void glCreatePerfQueryINTEL(int p0, java.nio.IntBuffer p1) {
    record("glCreatePerfQueryINTEL", new Object[] {p0, p1});
  }

  @Override public int glCreateProgram() {
    record("glCreateProgram", new Object[] {});
    return 0;
  }

  @Override public long glCreateProgramObjectARB() {
    record("glCreateProgramObjectARB", new Object[] {});
    return 0L;
  }

  @Override public int glCreateProgressFenceNVX() {
    record("glCreateProgressFenceNVX", new Object[] {});
    return 0;
  }

  @Override public int glCreateShader(int p0) {
    record("glCreateShader", new Object[] {p0});
    return 0;
  }

  @Override public long glCreateShaderObjectARB(int p0) {
    record("glCreateShaderObjectARB", new Object[] {p0});
    return 0L;
  }

  @Override public int glCreateShaderProgramv(int p0, int p1, java.lang.String[] p2) {
    record("glCreateShaderProgramv", new Object[] {p0, p1, p2});
    return 0;
  }

  @Override public void glCreateStatesNV(int p0, int[] p1, int p2) {
    record("glCreateStatesNV", new Object[] {p0, p1, p2});
  }

  @Override public void glCreateStatesNV(int p0, java.nio.IntBuffer p1) {
    record("glCreateStatesNV", new Object[] {p0, p1});
  }

  @Override public void glCullFace(int p0) {
    record("glCullFace", new Object[] {p0});
  }

  @Override public void glCullParameterdvEXT(int p0, double[] p1, int p2) {
    record("glCullParameterdvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glCullParameterdvEXT(int p0, java.nio.DoubleBuffer p1) {
    record("glCullParameterdvEXT", new Object[] {p0, p1});
  }

  @Override public void glCullParameterfvEXT(int p0, float[] p1, int p2) {
    record("glCullParameterfvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glCullParameterfvEXT(int p0, java.nio.FloatBuffer p1) {
    record("glCullParameterfvEXT", new Object[] {p0, p1});
  }

  @Override public void glCurrentPaletteMatrixARB(int p0) {
    record("glCurrentPaletteMatrixARB", new Object[] {p0});
  }

  @Override public void glDebugMessageControl(int p0, int p1, int p2, int p3, int[] p4, int p5, boolean p6) {
    record("glDebugMessageControl", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glDebugMessageControl(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4, boolean p5) {
    record("glDebugMessageControl", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDebugMessageEnableAMD(int p0, int p1, int p2, int[] p3, int p4, boolean p5) {
    record("glDebugMessageEnableAMD", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDebugMessageEnableAMD(int p0, int p1, int p2, java.nio.IntBuffer p3, boolean p4) {
    record("glDebugMessageEnableAMD", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDebugMessageInsert(int p0, int p1, int p2, int p3, int p4, java.lang.String p5) {
    record("glDebugMessageInsert", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDebugMessageInsertAMD(int p0, int p1, int p2, int p3, java.lang.String p4) {
    record("glDebugMessageInsertAMD", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDeleteBuffers(int p0, int[] p1, int p2) {
    record("glDeleteBuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteBuffers(int p0, java.nio.IntBuffer p1) {
    record("glDeleteBuffers", new Object[] {p0, p1});
  }

  @Override public void glDeleteCommandListsNV(int p0, int[] p1, int p2) {
    record("glDeleteCommandListsNV", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteCommandListsNV(int p0, java.nio.IntBuffer p1) {
    record("glDeleteCommandListsNV", new Object[] {p0, p1});
  }

  @Override public void glDeleteFramebuffers(int p0, int[] p1, int p2) {
    record("glDeleteFramebuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteFramebuffers(int p0, java.nio.IntBuffer p1) {
    record("glDeleteFramebuffers", new Object[] {p0, p1});
  }

  @Override public void glDeleteLists(int p0, int p1) {
    record("glDeleteLists", new Object[] {p0, p1});
  }

  @Override public void glDeleteMemoryObjectsEXT(int p0, int[] p1, int p2) {
    record("glDeleteMemoryObjectsEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteMemoryObjectsEXT(int p0, java.nio.IntBuffer p1) {
    record("glDeleteMemoryObjectsEXT", new Object[] {p0, p1});
  }

  @Override public void glDeleteNamesAMD(int p0, int p1, int[] p2, int p3) {
    record("glDeleteNamesAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glDeleteNamesAMD(int p0, int p1, java.nio.IntBuffer p2) {
    record("glDeleteNamesAMD", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteObjectARB(long p0) {
    record("glDeleteObjectARB", new Object[] {p0});
  }

  @Override public void glDeleteOcclusionQueriesNV(int p0, int[] p1, int p2) {
    record("glDeleteOcclusionQueriesNV", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteOcclusionQueriesNV(int p0, java.nio.IntBuffer p1) {
    record("glDeleteOcclusionQueriesNV", new Object[] {p0, p1});
  }

  @Override public void glDeletePerfQueryINTEL(int p0) {
    record("glDeletePerfQueryINTEL", new Object[] {p0});
  }

  @Override public void glDeleteProgram(int p0) {
    record("glDeleteProgram", new Object[] {p0});
  }

  @Override public void glDeleteProgramPipelines(int p0, int[] p1, int p2) {
    record("glDeleteProgramPipelines", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteProgramPipelines(int p0, java.nio.IntBuffer p1) {
    record("glDeleteProgramPipelines", new Object[] {p0, p1});
  }

  @Override public void glDeleteProgramsARB(int p0, int[] p1, int p2) {
    record("glDeleteProgramsARB", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteProgramsARB(int p0, java.nio.IntBuffer p1) {
    record("glDeleteProgramsARB", new Object[] {p0, p1});
  }

  @Override public void glDeleteQueries(int p0, int[] p1, int p2) {
    record("glDeleteQueries", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteQueries(int p0, java.nio.IntBuffer p1) {
    record("glDeleteQueries", new Object[] {p0, p1});
  }

  @Override public void glDeleteQueryResourceTagNV(int p0, int[] p1, int p2) {
    record("glDeleteQueryResourceTagNV", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteQueryResourceTagNV(int p0, java.nio.IntBuffer p1) {
    record("glDeleteQueryResourceTagNV", new Object[] {p0, p1});
  }

  @Override public void glDeleteRenderbuffers(int p0, int[] p1, int p2) {
    record("glDeleteRenderbuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteRenderbuffers(int p0, java.nio.IntBuffer p1) {
    record("glDeleteRenderbuffers", new Object[] {p0, p1});
  }

  @Override public void glDeleteSemaphoresEXT(int p0, int[] p1, int p2) {
    record("glDeleteSemaphoresEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteSemaphoresEXT(int p0, java.nio.IntBuffer p1) {
    record("glDeleteSemaphoresEXT", new Object[] {p0, p1});
  }

  @Override public void glDeleteShader(int p0) {
    record("glDeleteShader", new Object[] {p0});
  }

  @Override public void glDeleteStatesNV(int p0, int[] p1, int p2) {
    record("glDeleteStatesNV", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteStatesNV(int p0, java.nio.IntBuffer p1) {
    record("glDeleteStatesNV", new Object[] {p0, p1});
  }

  @Override public void glDeleteTextures(int p0, int[] p1, int p2) {
    record("glDeleteTextures", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteTextures(int p0, java.nio.IntBuffer p1) {
    record("glDeleteTextures", new Object[] {p0, p1});
  }

  @Override public void glDeleteTransformFeedbacks(int p0, int[] p1, int p2) {
    record("glDeleteTransformFeedbacks", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteTransformFeedbacks(int p0, java.nio.IntBuffer p1) {
    record("glDeleteTransformFeedbacks", new Object[] {p0, p1});
  }

  @Override public void glDeleteTransformFeedbacksNV(int p0, int[] p1, int p2) {
    record("glDeleteTransformFeedbacksNV", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteTransformFeedbacksNV(int p0, java.nio.IntBuffer p1) {
    record("glDeleteTransformFeedbacksNV", new Object[] {p0, p1});
  }

  @Override public void glDeleteVertexArrays(int p0, int[] p1, int p2) {
    record("glDeleteVertexArrays", new Object[] {p0, p1, p2});
  }

  @Override public void glDeleteVertexArrays(int p0, java.nio.IntBuffer p1) {
    record("glDeleteVertexArrays", new Object[] {p0, p1});
  }

  @Override public void glDeleteVertexShaderEXT(int p0) {
    record("glDeleteVertexShaderEXT", new Object[] {p0});
  }

  @Override public void glDepthBoundsEXT(double p0, double p1) {
    record("glDepthBoundsEXT", new Object[] {p0, p1});
  }

  @Override public void glDepthFunc(int p0) {
    record("glDepthFunc", new Object[] {p0});
  }

  @Override public void glDepthMask(boolean p0) {
    record("glDepthMask", new Object[] {p0});
  }

  @Override public void glDepthRange(double p0, double p1) {
    record("glDepthRange", new Object[] {p0, p1});
  }

  @Override public void glDepthRangef(float p0, float p1) {
    record("glDepthRangef", new Object[] {p0, p1});
  }

  @Override public void glDetachObjectARB(long p0, long p1) {
    record("glDetachObjectARB", new Object[] {p0, p1});
  }

  @Override public void glDetachShader(int p0, int p1) {
    record("glDetachShader", new Object[] {p0, p1});
  }

  @Override public void glDisable(int p0) {
    record("glDisable", new Object[] {p0});
  }

  @Override public void glDisableClientState(int p0) {
    record("glDisableClientState", new Object[] {p0});
  }

  @Override public void glDisableClientStateIndexedEXT(int p0, int p1) {
    record("glDisableClientStateIndexedEXT", new Object[] {p0, p1});
  }

  @Override public void glDisableClientStateiEXT(int p0, int p1) {
    record("glDisableClientStateiEXT", new Object[] {p0, p1});
  }

  @Override public void glDisableIndexed(int p0, int p1) {
    record("glDisableIndexed", new Object[] {p0, p1});
  }

  @Override public void glDisableVariantClientStateEXT(int p0) {
    record("glDisableVariantClientStateEXT", new Object[] {p0});
  }

  @Override public void glDisableVertexArrayAttribEXT(int p0, int p1) {
    record("glDisableVertexArrayAttribEXT", new Object[] {p0, p1});
  }

  @Override public void glDisableVertexArrayEXT(int p0, int p1) {
    record("glDisableVertexArrayEXT", new Object[] {p0, p1});
  }

  @Override public void glDisableVertexAttribAPPLE(int p0, int p1) {
    record("glDisableVertexAttribAPPLE", new Object[] {p0, p1});
  }

  @Override public void glDisableVertexAttribArray(int p0) {
    record("glDisableVertexAttribArray", new Object[] {p0});
  }

  @Override public void glDisableVertexAttribArrayARB(int p0) {
    record("glDisableVertexAttribArrayARB", new Object[] {p0});
  }

  @Override public void glDisablei(int p0, int p1) {
    record("glDisablei", new Object[] {p0, p1});
  }

  @Override public void glDrawArrays(int p0, int p1, int p2) {
    record("glDrawArrays", new Object[] {p0, p1, p2});
  }

  @Override public void glDrawArraysInstanced(int p0, int p1, int p2, int p3) {
    record("glDrawArraysInstanced", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glDrawArraysInstancedBaseInstance(int p0, int p1, int p2, int p3, int p4) {
    record("glDrawArraysInstancedBaseInstance", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawBuffer(int p0) {
    record("glDrawBuffer", new Object[] {p0});
  }

  @Override public void glDrawBuffers(int p0, int[] p1, int p2) {
    record("glDrawBuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glDrawBuffers(int p0, java.nio.IntBuffer p1) {
    record("glDrawBuffers", new Object[] {p0, p1});
  }

  @Override public void glDrawBuffersATI(int p0, int[] p1, int p2) {
    record("glDrawBuffersATI", new Object[] {p0, p1, p2});
  }

  @Override public void glDrawBuffersATI(int p0, java.nio.IntBuffer p1) {
    record("glDrawBuffersATI", new Object[] {p0, p1});
  }

  @Override public void glDrawCommandsAddressNV(int p0, java.nio.LongBuffer p1, java.nio.IntBuffer p2, int p3) {
    record("glDrawCommandsAddressNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glDrawCommandsAddressNV(int p0, long[] p1, int p2, int[] p3, int p4, int p5) {
    record("glDrawCommandsAddressNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDrawCommandsNV(int p0, int p1, com.jogamp.common.nio.PointerBuffer p2, int[] p3, int p4, int p5) {
    record("glDrawCommandsNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDrawCommandsNV(int p0, int p1, com.jogamp.common.nio.PointerBuffer p2, java.nio.IntBuffer p3, int p4) {
    record("glDrawCommandsNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawCommandsStatesAddressNV(java.nio.LongBuffer p0, java.nio.IntBuffer p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, int p4) {
    record("glDrawCommandsStatesAddressNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawCommandsStatesAddressNV(long[] p0, int p1, int[] p2, int p3, int[] p4, int p5, int[] p6, int p7, int p8) {
    record("glDrawCommandsStatesAddressNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glDrawCommandsStatesNV(int p0, com.jogamp.common.nio.PointerBuffer p1, int[] p2, int p3, int[] p4, int p5, int[] p6, int p7, int p8) {
    record("glDrawCommandsStatesNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glDrawCommandsStatesNV(int p0, com.jogamp.common.nio.PointerBuffer p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, int p5) {
    record("glDrawCommandsStatesNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDrawElements(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glDrawElements", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glDrawElements(int p0, int p1, int p2, long p3) {
    record("glDrawElements", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glDrawElementsInstanced(int p0, int p1, int p2, java.nio.Buffer p3, int p4) {
    record("glDrawElementsInstanced", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawElementsInstanced(int p0, int p1, int p2, long p3, int p4) {
    record("glDrawElementsInstanced", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawElementsInstancedBaseInstance(int p0, int p1, int p2, long p3, int p4, int p5) {
    record("glDrawElementsInstancedBaseInstance", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDrawElementsInstancedBaseVertexBaseInstance(int p0, int p1, int p2, long p3, int p4, int p5, int p6) {
    record("glDrawElementsInstancedBaseVertexBaseInstance", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glDrawPixels(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glDrawPixels", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawPixels(int p0, int p1, int p2, int p3, long p4) {
    record("glDrawPixels", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glDrawRangeElements(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glDrawRangeElements", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDrawRangeElements(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glDrawRangeElements", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glDrawTextureNV(int p0, int p1, float p2, float p3, float p4, float p5, float p6, float p7, float p8, float p9, float p10) {
    record("glDrawTextureNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glDrawTransformFeedback(int p0, int p1) {
    record("glDrawTransformFeedback", new Object[] {p0, p1});
  }

  @Override public void glDrawTransformFeedbackNV(int p0, int p1) {
    record("glDrawTransformFeedbackNV", new Object[] {p0, p1});
  }

  @Override public void glDrawTransformFeedbackStream(int p0, int p1, int p2) {
    record("glDrawTransformFeedbackStream", new Object[] {p0, p1, p2});
  }

  @Override public void glEdgeFlag(boolean p0) {
    record("glEdgeFlag", new Object[] {p0});
  }

  @Override public void glEdgeFlagFormatNV(int p0) {
    record("glEdgeFlagFormatNV", new Object[] {p0});
  }

  @Override public void glEdgeFlagPointer(int p0, java.nio.Buffer p1) {
    record("glEdgeFlagPointer", new Object[] {p0, p1});
  }

  @Override public void glEdgeFlagPointer(int p0, long p1) {
    record("glEdgeFlagPointer", new Object[] {p0, p1});
  }

  @Override public void glEdgeFlagv(byte[] p0, int p1) {
    record("glEdgeFlagv", new Object[] {p0, p1});
  }

  @Override public void glEdgeFlagv(java.nio.ByteBuffer p0) {
    record("glEdgeFlagv", new Object[] {p0});
  }

  @Override public void glEnable(int p0) {
    record("glEnable", new Object[] {p0});
  }

  @Override public void glEnableClientState(int p0) {
    record("glEnableClientState", new Object[] {p0});
  }

  @Override public void glEnableClientStateIndexedEXT(int p0, int p1) {
    record("glEnableClientStateIndexedEXT", new Object[] {p0, p1});
  }

  @Override public void glEnableClientStateiEXT(int p0, int p1) {
    record("glEnableClientStateiEXT", new Object[] {p0, p1});
  }

  @Override public void glEnableIndexed(int p0, int p1) {
    record("glEnableIndexed", new Object[] {p0, p1});
  }

  @Override public void glEnableVariantClientStateEXT(int p0) {
    record("glEnableVariantClientStateEXT", new Object[] {p0});
  }

  @Override public void glEnableVertexArrayAttribEXT(int p0, int p1) {
    record("glEnableVertexArrayAttribEXT", new Object[] {p0, p1});
  }

  @Override public void glEnableVertexArrayEXT(int p0, int p1) {
    record("glEnableVertexArrayEXT", new Object[] {p0, p1});
  }

  @Override public void glEnableVertexAttribAPPLE(int p0, int p1) {
    record("glEnableVertexAttribAPPLE", new Object[] {p0, p1});
  }

  @Override public void glEnableVertexAttribArray(int p0) {
    record("glEnableVertexAttribArray", new Object[] {p0});
  }

  @Override public void glEnableVertexAttribArrayARB(int p0) {
    record("glEnableVertexAttribArrayARB", new Object[] {p0});
  }

  @Override public void glEnablei(int p0, int p1) {
    record("glEnablei", new Object[] {p0, p1});
  }

  @Override public void glEnd() {
    record("glEnd", new Object[] {});
  }

  @Override public void glEndConditionalRender() {
    record("glEndConditionalRender", new Object[] {});
  }

  @Override public void glEndConditionalRenderNVX() {
    record("glEndConditionalRenderNVX", new Object[] {});
  }

  @Override public void glEndList() {
    record("glEndList", new Object[] {});
  }

  @Override public void glEndOcclusionQueryNV() {
    record("glEndOcclusionQueryNV", new Object[] {});
  }

  @Override public void glEndPerfQueryINTEL(int p0) {
    record("glEndPerfQueryINTEL", new Object[] {p0});
  }

  @Override public void glEndQuery(int p0) {
    record("glEndQuery", new Object[] {p0});
  }

  @Override public void glEndQueryIndexed(int p0, int p1) {
    record("glEndQueryIndexed", new Object[] {p0, p1});
  }

  @Override public void glEndTransformFeedback() {
    record("glEndTransformFeedback", new Object[] {});
  }

  @Override public void glEndVertexShaderEXT() {
    record("glEndVertexShaderEXT", new Object[] {});
  }

  @Override public void glEndVideoCaptureNV(int p0) {
    record("glEndVideoCaptureNV", new Object[] {p0});
  }

  @Override public void glEvalCoord1d(double p0) {
    record("glEvalCoord1d", new Object[] {p0});
  }

  @Override public void glEvalCoord1dv(double[] p0, int p1) {
    record("glEvalCoord1dv", new Object[] {p0, p1});
  }

  @Override public void glEvalCoord1dv(java.nio.DoubleBuffer p0) {
    record("glEvalCoord1dv", new Object[] {p0});
  }

  @Override public void glEvalCoord1f(float p0) {
    record("glEvalCoord1f", new Object[] {p0});
  }

  @Override public void glEvalCoord1fv(float[] p0, int p1) {
    record("glEvalCoord1fv", new Object[] {p0, p1});
  }

  @Override public void glEvalCoord1fv(java.nio.FloatBuffer p0) {
    record("glEvalCoord1fv", new Object[] {p0});
  }

  @Override public void glEvalCoord2d(double p0, double p1) {
    record("glEvalCoord2d", new Object[] {p0, p1});
  }

  @Override public void glEvalCoord2dv(double[] p0, int p1) {
    record("glEvalCoord2dv", new Object[] {p0, p1});
  }

  @Override public void glEvalCoord2dv(java.nio.DoubleBuffer p0) {
    record("glEvalCoord2dv", new Object[] {p0});
  }

  @Override public void glEvalCoord2f(float p0, float p1) {
    record("glEvalCoord2f", new Object[] {p0, p1});
  }

  @Override public void glEvalCoord2fv(float[] p0, int p1) {
    record("glEvalCoord2fv", new Object[] {p0, p1});
  }

  @Override public void glEvalCoord2fv(java.nio.FloatBuffer p0) {
    record("glEvalCoord2fv", new Object[] {p0});
  }

  @Override public void glEvalMapsNV(int p0, int p1) {
    record("glEvalMapsNV", new Object[] {p0, p1});
  }

  @Override public void glEvalMesh1(int p0, int p1, int p2) {
    record("glEvalMesh1", new Object[] {p0, p1, p2});
  }

  @Override public void glEvalMesh2(int p0, int p1, int p2, int p3, int p4) {
    record("glEvalMesh2", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glEvalPoint1(int p0) {
    record("glEvalPoint1", new Object[] {p0});
  }

  @Override public void glEvalPoint2(int p0, int p1) {
    record("glEvalPoint2", new Object[] {p0, p1});
  }

  @Override public void glExtractComponentEXT(int p0, int p1, int p2) {
    record("glExtractComponentEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glFeedbackBuffer(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glFeedbackBuffer", new Object[] {p0, p1, p2});
  }

  @Override public void glFinish() {
    record("glFinish", new Object[] {});
  }

  @Override public void glFinishTextureSUNX() {
    record("glFinishTextureSUNX", new Object[] {});
  }

  @Override public void glFlush() {
    record("glFlush", new Object[] {});
  }

  @Override public void glFlushMappedBufferRange(int p0, long p1, long p2) {
    record("glFlushMappedBufferRange", new Object[] {p0, p1, p2});
  }

  @Override public void glFlushMappedNamedBufferRangeEXT(int p0, long p1, long p2) {
    record("glFlushMappedNamedBufferRangeEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glFlushPixelDataRangeNV(int p0) {
    record("glFlushPixelDataRangeNV", new Object[] {p0});
  }

  @Override public void glFlushVertexArrayRangeAPPLE(int p0, java.nio.Buffer p1) {
    record("glFlushVertexArrayRangeAPPLE", new Object[] {p0, p1});
  }

  @Override public void glFogCoordFormatNV(int p0, int p1) {
    record("glFogCoordFormatNV", new Object[] {p0, p1});
  }

  @Override public void glFogCoordPointer(int p0, int p1, java.nio.Buffer p2) {
    record("glFogCoordPointer", new Object[] {p0, p1, p2});
  }

  @Override public void glFogCoordPointer(int p0, int p1, long p2) {
    record("glFogCoordPointer", new Object[] {p0, p1, p2});
  }

  @Override public void glFogCoordd(double p0) {
    record("glFogCoordd", new Object[] {p0});
  }

  @Override public void glFogCoorddv(double[] p0, int p1) {
    record("glFogCoorddv", new Object[] {p0, p1});
  }

  @Override public void glFogCoorddv(java.nio.DoubleBuffer p0) {
    record("glFogCoorddv", new Object[] {p0});
  }

  @Override public void glFogCoordf(float p0) {
    record("glFogCoordf", new Object[] {p0});
  }

  @Override public void glFogCoordfv(float[] p0, int p1) {
    record("glFogCoordfv", new Object[] {p0, p1});
  }

  @Override public void glFogCoordfv(java.nio.FloatBuffer p0) {
    record("glFogCoordfv", new Object[] {p0});
  }

  @Override public void glFogCoordh(short p0) {
    record("glFogCoordh", new Object[] {p0});
  }

  @Override public void glFogCoordhv(java.nio.ShortBuffer p0) {
    record("glFogCoordhv", new Object[] {p0});
  }

  @Override public void glFogCoordhv(short[] p0, int p1) {
    record("glFogCoordhv", new Object[] {p0, p1});
  }

  @Override public void glFogf(int p0, float p1) {
    record("glFogf", new Object[] {p0, p1});
  }

  @Override public void glFogfv(int p0, float[] p1, int p2) {
    record("glFogfv", new Object[] {p0, p1, p2});
  }

  @Override public void glFogfv(int p0, java.nio.FloatBuffer p1) {
    record("glFogfv", new Object[] {p0, p1});
  }

  @Override public void glFogi(int p0, int p1) {
    record("glFogi", new Object[] {p0, p1});
  }

  @Override public void glFogiv(int p0, int[] p1, int p2) {
    record("glFogiv", new Object[] {p0, p1, p2});
  }

  @Override public void glFogiv(int p0, java.nio.IntBuffer p1) {
    record("glFogiv", new Object[] {p0, p1});
  }

  @Override public void glFrameTerminatorGREMEDY() {
    record("glFrameTerminatorGREMEDY", new Object[] {});
  }

  @Override public void glFramebufferDrawBufferEXT(int p0, int p1) {
    record("glFramebufferDrawBufferEXT", new Object[] {p0, p1});
  }

  @Override public void glFramebufferDrawBuffersEXT(int p0, int p1, int[] p2, int p3) {
    record("glFramebufferDrawBuffersEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glFramebufferDrawBuffersEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glFramebufferDrawBuffersEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glFramebufferFetchBarrierEXT() {
    record("glFramebufferFetchBarrierEXT", new Object[] {});
  }

  @Override public void glFramebufferParameteri(int p0, int p1, int p2) {
    record("glFramebufferParameteri", new Object[] {p0, p1, p2});
  }

  @Override public void glFramebufferReadBufferEXT(int p0, int p1) {
    record("glFramebufferReadBufferEXT", new Object[] {p0, p1});
  }

  @Override public void glFramebufferRenderbuffer(int p0, int p1, int p2, int p3) {
    record("glFramebufferRenderbuffer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glFramebufferSamplePositionsfvAMD(int p0, int p1, int p2, float[] p3, int p4) {
    record("glFramebufferSamplePositionsfvAMD", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glFramebufferSamplePositionsfvAMD(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glFramebufferSamplePositionsfvAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glFramebufferTexture1D(int p0, int p1, int p2, int p3, int p4) {
    record("glFramebufferTexture1D", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glFramebufferTexture2D(int p0, int p1, int p2, int p3, int p4) {
    record("glFramebufferTexture2D", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glFramebufferTexture3D(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glFramebufferTexture3D", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glFramebufferTextureEXT(int p0, int p1, int p2, int p3) {
    record("glFramebufferTextureEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glFramebufferTextureFaceEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glFramebufferTextureFaceEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glFramebufferTextureLayer(int p0, int p1, int p2, int p3, int p4) {
    record("glFramebufferTextureLayer", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glFramebufferTextureMultiviewOVR(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glFramebufferTextureMultiviewOVR", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glFrontFace(int p0) {
    record("glFrontFace", new Object[] {p0});
  }

  @Override public void glFrustum(double p0, double p1, double p2, double p3, double p4, double p5) {
    record("glFrustum", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glFrustumf(float p0, float p1, float p2, float p3, float p4, float p5) {
    record("glFrustumf", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGenBuffers(int p0, int[] p1, int p2) {
    record("glGenBuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glGenBuffers(int p0, java.nio.IntBuffer p1) {
    record("glGenBuffers", new Object[] {p0, p1});
  }

  @Override public void glGenFramebuffers(int p0, int[] p1, int p2) {
    record("glGenFramebuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glGenFramebuffers(int p0, java.nio.IntBuffer p1) {
    record("glGenFramebuffers", new Object[] {p0, p1});
  }

  @Override public int glGenLists(int p0) {
    record("glGenLists", new Object[] {p0});
    return 0;
  }

  @Override public void glGenNamesAMD(int p0, int p1, int[] p2, int p3) {
    record("glGenNamesAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGenNamesAMD(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGenNamesAMD", new Object[] {p0, p1, p2});
  }

  @Override public void glGenOcclusionQueriesNV(int p0, int[] p1, int p2) {
    record("glGenOcclusionQueriesNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGenOcclusionQueriesNV(int p0, java.nio.IntBuffer p1) {
    record("glGenOcclusionQueriesNV", new Object[] {p0, p1});
  }

  @Override public void glGenProgramPipelines(int p0, int[] p1, int p2) {
    record("glGenProgramPipelines", new Object[] {p0, p1, p2});
  }

  @Override public void glGenProgramPipelines(int p0, java.nio.IntBuffer p1) {
    record("glGenProgramPipelines", new Object[] {p0, p1});
  }

  @Override public void glGenProgramsARB(int p0, int[] p1, int p2) {
    record("glGenProgramsARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGenProgramsARB(int p0, java.nio.IntBuffer p1) {
    record("glGenProgramsARB", new Object[] {p0, p1});
  }

  @Override public void glGenQueries(int p0, int[] p1, int p2) {
    record("glGenQueries", new Object[] {p0, p1, p2});
  }

  @Override public void glGenQueries(int p0, java.nio.IntBuffer p1) {
    record("glGenQueries", new Object[] {p0, p1});
  }

  @Override public void glGenQueryResourceTagNV(int p0, int[] p1, int p2) {
    record("glGenQueryResourceTagNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGenQueryResourceTagNV(int p0, java.nio.IntBuffer p1) {
    record("glGenQueryResourceTagNV", new Object[] {p0, p1});
  }

  @Override public void glGenRenderbuffers(int p0, int[] p1, int p2) {
    record("glGenRenderbuffers", new Object[] {p0, p1, p2});
  }

  @Override public void glGenRenderbuffers(int p0, java.nio.IntBuffer p1) {
    record("glGenRenderbuffers", new Object[] {p0, p1});
  }

  @Override public void glGenSemaphoresEXT(int p0, int[] p1, int p2) {
    record("glGenSemaphoresEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGenSemaphoresEXT(int p0, java.nio.IntBuffer p1) {
    record("glGenSemaphoresEXT", new Object[] {p0, p1});
  }

  @Override public int glGenSymbolsEXT(int p0, int p1, int p2, int p3) {
    record("glGenSymbolsEXT", new Object[] {p0, p1, p2, p3});
    return 0;
  }

  @Override public void glGenTextures(int p0, int[] p1, int p2) {
    record("glGenTextures", new Object[] {p0, p1, p2});
  }

  @Override public void glGenTextures(int p0, java.nio.IntBuffer p1) {
    record("glGenTextures", new Object[] {p0, p1});
  }

  @Override public void glGenTransformFeedbacks(int p0, int[] p1, int p2) {
    record("glGenTransformFeedbacks", new Object[] {p0, p1, p2});
  }

  @Override public void glGenTransformFeedbacks(int p0, java.nio.IntBuffer p1) {
    record("glGenTransformFeedbacks", new Object[] {p0, p1});
  }

  @Override public void glGenTransformFeedbacksNV(int p0, int[] p1, int p2) {
    record("glGenTransformFeedbacksNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGenTransformFeedbacksNV(int p0, java.nio.IntBuffer p1) {
    record("glGenTransformFeedbacksNV", new Object[] {p0, p1});
  }

  @Override public void glGenVertexArrays(int p0, int[] p1, int p2) {
    record("glGenVertexArrays", new Object[] {p0, p1, p2});
  }

  @Override public void glGenVertexArrays(int p0, java.nio.IntBuffer p1) {
    record("glGenVertexArrays", new Object[] {p0, p1});
  }

  @Override public int glGenVertexShadersEXT(int p0) {
    record("glGenVertexShadersEXT", new Object[] {p0});
    return 0;
  }

  @Override public void glGenerateMipmap(int p0) {
    record("glGenerateMipmap", new Object[] {p0});
  }

  @Override public void glGenerateMultiTexMipmapEXT(int p0, int p1) {
    record("glGenerateMultiTexMipmapEXT", new Object[] {p0, p1});
  }

  @Override public void glGenerateTextureMipmapEXT(int p0, int p1) {
    record("glGenerateTextureMipmapEXT", new Object[] {p0, p1});
  }

  @Override public void glGetActiveAtomicCounterBufferiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetActiveAtomicCounterBufferiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetActiveAtomicCounterBufferiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetActiveAtomicCounterBufferiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetActiveAttrib(int p0, int p1, int p2, int[] p3, int p4, int[] p5, int p6, int[] p7, int p8, byte[] p9, int p10) {
    record("glGetActiveAttrib", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glGetActiveAttrib(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.ByteBuffer p6) {
    record("glGetActiveAttrib", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetActiveUniform(int p0, int p1, int p2, int[] p3, int p4, int[] p5, int p6, int[] p7, int p8, byte[] p9, int p10) {
    record("glGetActiveUniform", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glGetActiveUniform(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.ByteBuffer p6) {
    record("glGetActiveUniform", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetActiveUniformARB(long p0, int p1, int p2, int[] p3, int p4, int[] p5, int p6, int[] p7, int p8, byte[] p9, int p10) {
    record("glGetActiveUniformARB", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glGetActiveUniformARB(long p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.ByteBuffer p6) {
    record("glGetActiveUniformARB", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetActiveUniformBlockName(int p0, int p1, int p2, int[] p3, int p4, byte[] p5, int p6) {
    record("glGetActiveUniformBlockName", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetActiveUniformBlockName(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.ByteBuffer p4) {
    record("glGetActiveUniformBlockName", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetActiveUniformBlockiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetActiveUniformBlockiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetActiveUniformBlockiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetActiveUniformBlockiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetActiveUniformName(int p0, int p1, int p2, int[] p3, int p4, byte[] p5, int p6) {
    record("glGetActiveUniformName", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetActiveUniformName(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.ByteBuffer p4) {
    record("glGetActiveUniformName", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetActiveUniformsiv(int p0, int p1, int[] p2, int p3, int p4, int[] p5, int p6) {
    record("glGetActiveUniformsiv", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetActiveUniformsiv(int p0, int p1, java.nio.IntBuffer p2, int p3, java.nio.IntBuffer p4) {
    record("glGetActiveUniformsiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetAttachedObjectsARB(long p0, int p1, int[] p2, int p3, long[] p4, int p5) {
    record("glGetAttachedObjectsARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetAttachedObjectsARB(long p0, int p1, java.nio.IntBuffer p2, java.nio.LongBuffer p3) {
    record("glGetAttachedObjectsARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetAttachedShaders(int p0, int p1, int[] p2, int p3, int[] p4, int p5) {
    record("glGetAttachedShaders", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetAttachedShaders(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3) {
    record("glGetAttachedShaders", new Object[] {p0, p1, p2, p3});
  }

  @Override public int glGetAttribLocation(int p0, java.lang.String p1) {
    record("glGetAttribLocation", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glGetBooleanIndexedv(int p0, int p1, byte[] p2, int p3) {
    record("glGetBooleanIndexedv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetBooleanIndexedv(int p0, int p1, java.nio.ByteBuffer p2) {
    record("glGetBooleanIndexedv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetBooleani_v(int p0, int p1, byte[] p2, int p3) {
    record("glGetBooleani_v", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetBooleani_v(int p0, int p1, java.nio.ByteBuffer p2) {
    record("glGetBooleani_v", new Object[] {p0, p1, p2});
  }

  @Override public void glGetBooleanv(int p0, byte[] p1, int p2) {
    record("glGetBooleanv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetBooleanv(int p0, java.nio.ByteBuffer p1) {
    record("glGetBooleanv", new Object[] {p0, p1});
  }

  @Override public void glGetBufferParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetBufferParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetBufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetBufferParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetBufferParameterui64vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetBufferParameterui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetBufferParameterui64vNV(int p0, int p1, long[] p2, int p3) {
    record("glGetBufferParameterui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetBufferSubData(int p0, long p1, long p2, java.nio.Buffer p3) {
    record("glGetBufferSubData", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetClipPlane(int p0, double[] p1, int p2) {
    record("glGetClipPlane", new Object[] {p0, p1, p2});
  }

  @Override public void glGetClipPlane(int p0, java.nio.DoubleBuffer p1) {
    record("glGetClipPlane", new Object[] {p0, p1});
  }

  @Override public void glGetClipPlanef(int p0, float[] p1, int p2) {
    record("glGetClipPlanef", new Object[] {p0, p1, p2});
  }

  @Override public void glGetClipPlanef(int p0, java.nio.FloatBuffer p1) {
    record("glGetClipPlanef", new Object[] {p0, p1});
  }

  @Override public void glGetColorTable(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glGetColorTable", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetColorTable(int p0, int p1, int p2, long p3) {
    record("glGetColorTable", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetColorTableParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glGetColorTableParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetColorTableParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetColorTableParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetColorTableParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetColorTableParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetColorTableParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetColorTableParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public int glGetCommandHeaderNV(int p0, int p1) {
    record("glGetCommandHeaderNV", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glGetCompressedMultiTexImageEXT(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glGetCompressedMultiTexImageEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetCompressedTexImage(int p0, int p1, java.nio.Buffer p2) {
    record("glGetCompressedTexImage", new Object[] {p0, p1, p2});
  }

  @Override public void glGetCompressedTexImage(int p0, int p1, long p2) {
    record("glGetCompressedTexImage", new Object[] {p0, p1, p2});
  }

  @Override public void glGetCompressedTextureImageEXT(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glGetCompressedTextureImageEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetConvolutionFilter(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glGetConvolutionFilter", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetConvolutionFilter(int p0, int p1, int p2, long p3) {
    record("glGetConvolutionFilter", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetConvolutionParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glGetConvolutionParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetConvolutionParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetConvolutionParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetConvolutionParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetConvolutionParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetConvolutionParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetConvolutionParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetCoverageModulationTableNV(int p0, float[] p1, int p2) {
    record("glGetCoverageModulationTableNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetCoverageModulationTableNV(int p0, java.nio.FloatBuffer p1) {
    record("glGetCoverageModulationTableNV", new Object[] {p0, p1});
  }

  @Override public int glGetDebugMessageLog(int p0, int p1, int[] p2, int p3, int[] p4, int p5, int[] p6, int p7, int[] p8, int p9, int[] p10, int p11, byte[] p12, int p13) {
    record("glGetDebugMessageLog", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13});
    return 0;
  }

  @Override public int glGetDebugMessageLog(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.IntBuffer p6, java.nio.ByteBuffer p7) {
    record("glGetDebugMessageLog", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
    return 0;
  }

  @Override public int glGetDebugMessageLogAMD(int p0, int p1, int[] p2, int p3, int[] p4, int p5, int[] p6, int p7, int[] p8, int p9, byte[] p10, int p11) {
    record("glGetDebugMessageLogAMD", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
    return 0;
  }

  @Override public int glGetDebugMessageLogAMD(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.ByteBuffer p6) {
    record("glGetDebugMessageLogAMD", new Object[] {p0, p1, p2, p3, p4, p5, p6});
    return 0;
  }

  @Override public void glGetDoubleIndexedvEXT(int p0, int p1, double[] p2, int p3) {
    record("glGetDoubleIndexedvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetDoubleIndexedvEXT(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetDoubleIndexedvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetDoublei_vEXT(int p0, int p1, double[] p2, int p3) {
    record("glGetDoublei_vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetDoublei_vEXT(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetDoublei_vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetDoublev(int p0, double[] p1, int p2) {
    record("glGetDoublev", new Object[] {p0, p1, p2});
  }

  @Override public void glGetDoublev(int p0, java.nio.DoubleBuffer p1) {
    record("glGetDoublev", new Object[] {p0, p1});
  }

  @Override public int glGetError() {
    record("glGetError", new Object[] {});
    return this.errors.isEmpty() ? com.jogamp.opengl.GL.GL_NO_ERROR : this.errors.removeFirst();
  }

  @Override public void glGetFirstPerfQueryIdINTEL(int[] p0, int p1) {
    record("glGetFirstPerfQueryIdINTEL", new Object[] {p0, p1});
  }

  @Override public void glGetFirstPerfQueryIdINTEL(java.nio.IntBuffer p0) {
    record("glGetFirstPerfQueryIdINTEL", new Object[] {p0});
  }

  @Override public void glGetFloatIndexedvEXT(int p0, int p1, float[] p2, int p3) {
    record("glGetFloatIndexedvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetFloatIndexedvEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetFloatIndexedvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetFloati_vEXT(int p0, int p1, float[] p2, int p3) {
    record("glGetFloati_vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetFloati_vEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetFloati_vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetFloatv(int p0, float[] p1, int p2) {
    record("glGetFloatv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetFloatv(int p0, java.nio.FloatBuffer p1) {
    record("glGetFloatv", new Object[] {p0, p1});
  }

  @Override public int glGetFragDataLocation(int p0, java.lang.String p1) {
    record("glGetFragDataLocation", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glGetFramebufferAttachmentParameteriv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetFramebufferAttachmentParameteriv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetFramebufferAttachmentParameteriv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetFramebufferAttachmentParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetFramebufferParameterfvAMD(int p0, int p1, int p2, int p3, int p4, float[] p5, int p6) {
    record("glGetFramebufferParameterfvAMD", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetFramebufferParameterfvAMD(int p0, int p1, int p2, int p3, int p4, java.nio.FloatBuffer p5) {
    record("glGetFramebufferParameterfvAMD", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetFramebufferParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetFramebufferParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetFramebufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetFramebufferParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetFramebufferParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetFramebufferParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetFramebufferParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetFramebufferParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public int glGetGraphicsResetStatus() {
    record("glGetGraphicsResetStatus", new Object[] {});
    return 0;
  }

  @Override public long glGetHandleARB(int p0) {
    record("glGetHandleARB", new Object[] {p0});
    return 0L;
  }

  @Override public void glGetHistogram(int p0, boolean p1, int p2, int p3, java.nio.Buffer p4) {
    record("glGetHistogram", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetHistogram(int p0, boolean p1, int p2, int p3, long p4) {
    record("glGetHistogram", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetHistogramParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glGetHistogramParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetHistogramParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetHistogramParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetHistogramParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetHistogramParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetHistogramParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetHistogramParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetInfoLogARB(long p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetInfoLogARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetInfoLogARB(long p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetInfoLogARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetInteger64v(int p0, java.nio.LongBuffer p1) {
    record("glGetInteger64v", new Object[] {p0, p1});
  }

  @Override public void glGetInteger64v(int p0, long[] p1, int p2) {
    record("glGetInteger64v", new Object[] {p0, p1, p2});
  }

  @Override public void glGetIntegerIndexedv(int p0, int p1, int[] p2, int p3) {
    record("glGetIntegerIndexedv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetIntegerIndexedv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetIntegerIndexedv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetIntegeri_v(int p0, int p1, int[] p2, int p3) {
    record("glGetIntegeri_v", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetIntegeri_v(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetIntegeri_v", new Object[] {p0, p1, p2});
  }

  @Override public void glGetIntegerui64i_vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetIntegerui64i_vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetIntegerui64i_vNV(int p0, int p1, long[] p2, int p3) {
    record("glGetIntegerui64i_vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetIntegerui64vNV(int p0, java.nio.LongBuffer p1) {
    record("glGetIntegerui64vNV", new Object[] {p0, p1});
  }

  @Override public void glGetIntegerui64vNV(int p0, long[] p1, int p2) {
    record("glGetIntegerui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetIntegerv(int p0, int[] p1, int p2) {
    record("glGetIntegerv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetIntegerv(int p0, java.nio.IntBuffer p1) {
    record("glGetIntegerv", new Object[] {p0, p1});
  }

  @Override public void glGetInternalformati64v(int p0, int p1, int p2, int p3, java.nio.LongBuffer p4) {
    record("glGetInternalformati64v", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetInternalformati64v(int p0, int p1, int p2, int p3, long[] p4, int p5) {
    record("glGetInternalformati64v", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetInternalformativ(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glGetInternalformativ", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetInternalformativ(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glGetInternalformativ", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetInvariantBooleanvEXT(int p0, int p1, byte[] p2, int p3) {
    record("glGetInvariantBooleanvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetInvariantBooleanvEXT(int p0, int p1, java.nio.ByteBuffer p2) {
    record("glGetInvariantBooleanvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetInvariantFloatvEXT(int p0, int p1, float[] p2, int p3) {
    record("glGetInvariantFloatvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetInvariantFloatvEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetInvariantFloatvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetInvariantIntegervEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetInvariantIntegervEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetInvariantIntegervEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetInvariantIntegervEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetLightfv(int p0, int p1, float[] p2, int p3) {
    record("glGetLightfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetLightfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetLightfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetLightiv(int p0, int p1, int[] p2, int p3) {
    record("glGetLightiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetLightiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetLightiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetLocalConstantBooleanvEXT(int p0, int p1, byte[] p2, int p3) {
    record("glGetLocalConstantBooleanvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetLocalConstantBooleanvEXT(int p0, int p1, java.nio.ByteBuffer p2) {
    record("glGetLocalConstantBooleanvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetLocalConstantFloatvEXT(int p0, int p1, float[] p2, int p3) {
    record("glGetLocalConstantFloatvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetLocalConstantFloatvEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetLocalConstantFloatvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetLocalConstantIntegervEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetLocalConstantIntegervEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetLocalConstantIntegervEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetLocalConstantIntegervEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMapAttribParameterfvNV(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetMapAttribParameterfvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMapAttribParameterfvNV(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetMapAttribParameterfvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapAttribParameterivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetMapAttribParameterivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMapAttribParameterivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetMapAttribParameterivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapControlPointsNV(int p0, int p1, int p2, int p3, int p4, boolean p5, java.nio.Buffer p6) {
    record("glGetMapControlPointsNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetMapParameterfvNV(int p0, int p1, float[] p2, int p3) {
    record("glGetMapParameterfvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapParameterfvNV(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetMapParameterfvNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMapParameterivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetMapParameterivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapParameterivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetMapParameterivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMapdv(int p0, int p1, double[] p2, int p3) {
    record("glGetMapdv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapdv(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetMapdv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMapfv(int p0, int p1, float[] p2, int p3) {
    record("glGetMapfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetMapfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMapiv(int p0, int p1, int[] p2, int p3) {
    record("glGetMapiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMapiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetMapiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMaterialfv(int p0, int p1, float[] p2, int p3) {
    record("glGetMaterialfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMaterialfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetMaterialfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMaterialiv(int p0, int p1, int[] p2, int p3) {
    record("glGetMaterialiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMaterialiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetMaterialiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMemoryObjectParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetMemoryObjectParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMemoryObjectParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetMemoryObjectParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMinmax(int p0, boolean p1, int p2, int p3, java.nio.Buffer p4) {
    record("glGetMinmax", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMinmax(int p0, boolean p1, int p2, int p3, long p4) {
    record("glGetMinmax", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMinmaxParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glGetMinmaxParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMinmaxParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetMinmaxParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMinmaxParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetMinmaxParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMinmaxParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetMinmaxParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMultiTexEnvfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetMultiTexEnvfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexEnvfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetMultiTexEnvfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexEnvivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetMultiTexEnvivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexEnvivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetMultiTexEnvivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexGendvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glGetMultiTexGendvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexGendvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glGetMultiTexGendvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexGenfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetMultiTexGenfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexGenfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetMultiTexGenfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexGenivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetMultiTexGenivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexGenivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetMultiTexGenivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexImageEXT(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glGetMultiTexImageEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetMultiTexLevelParameterfvEXT(int p0, int p1, int p2, int p3, float[] p4, int p5) {
    record("glGetMultiTexLevelParameterfvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetMultiTexLevelParameterfvEXT(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
    record("glGetMultiTexLevelParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexLevelParameterivEXT(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glGetMultiTexLevelParameterivEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetMultiTexLevelParameterivEXT(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glGetMultiTexLevelParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexParameterIivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetMultiTexParameterIivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexParameterIivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetMultiTexParameterIivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexParameterIuivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetMultiTexParameterIuivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexParameterIuivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetMultiTexParameterIuivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexParameterfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetMultiTexParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexParameterfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetMultiTexParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultiTexParameterivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetMultiTexParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetMultiTexParameterivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetMultiTexParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultisamplefv(int p0, int p1, float[] p2, int p3) {
    record("glGetMultisamplefv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultisamplefv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetMultisamplefv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetMultisamplefvNV(int p0, int p1, float[] p2, int p3) {
    record("glGetMultisamplefvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetMultisamplefvNV(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetMultisamplefvNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetNamedBufferParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetNamedBufferParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedBufferParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetNamedBufferParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetNamedBufferParameterui64vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetNamedBufferParameterui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetNamedBufferParameterui64vNV(int p0, int p1, long[] p2, int p3) {
    record("glGetNamedBufferParameterui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedBufferSubDataEXT(int p0, long p1, long p2, java.nio.Buffer p3) {
    record("glGetNamedBufferSubDataEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedFramebufferAttachmentParameterivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetNamedFramebufferAttachmentParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetNamedFramebufferAttachmentParameterivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetNamedFramebufferAttachmentParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedFramebufferParameterfvAMD(int p0, int p1, int p2, int p3, int p4, float[] p5, int p6) {
    record("glGetNamedFramebufferParameterfvAMD", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetNamedFramebufferParameterfvAMD(int p0, int p1, int p2, int p3, int p4, java.nio.FloatBuffer p5) {
    record("glGetNamedFramebufferParameterfvAMD", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetNamedFramebufferParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetNamedFramebufferParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedFramebufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetNamedFramebufferParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetNamedProgramLocalParameterIivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetNamedProgramLocalParameterIivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetNamedProgramLocalParameterIivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetNamedProgramLocalParameterIivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedProgramLocalParameterIuivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetNamedProgramLocalParameterIuivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetNamedProgramLocalParameterIuivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetNamedProgramLocalParameterIuivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedProgramLocalParameterdvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glGetNamedProgramLocalParameterdvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetNamedProgramLocalParameterdvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glGetNamedProgramLocalParameterdvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedProgramLocalParameterfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetNamedProgramLocalParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetNamedProgramLocalParameterfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetNamedProgramLocalParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedProgramStringEXT(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glGetNamedProgramStringEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedProgramivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetNamedProgramivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetNamedProgramivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetNamedProgramivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedRenderbufferParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetNamedRenderbufferParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetNamedRenderbufferParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetNamedRenderbufferParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetNextPerfQueryIdINTEL(int p0, int[] p1, int p2) {
    record("glGetNextPerfQueryIdINTEL", new Object[] {p0, p1, p2});
  }

  @Override public void glGetNextPerfQueryIdINTEL(int p0, java.nio.IntBuffer p1) {
    record("glGetNextPerfQueryIdINTEL", new Object[] {p0, p1});
  }

  @Override public void glGetObjectLabel(int p0, int p1, int p2, int[] p3, int p4, byte[] p5, int p6) {
    record("glGetObjectLabel", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetObjectLabel(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.ByteBuffer p4) {
    record("glGetObjectLabel", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetObjectParameterfvARB(long p0, int p1, float[] p2, int p3) {
    record("glGetObjectParameterfvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetObjectParameterfvARB(long p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetObjectParameterfvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetObjectParameterivAPPLE(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetObjectParameterivAPPLE", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetObjectParameterivAPPLE(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetObjectParameterivAPPLE", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetObjectParameterivARB(long p0, int p1, int[] p2, int p3) {
    record("glGetObjectParameterivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetObjectParameterivARB(long p0, int p1, java.nio.IntBuffer p2) {
    record("glGetObjectParameterivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetObjectPtrLabel(java.nio.Buffer p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetObjectPtrLabel", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetObjectPtrLabel(java.nio.Buffer p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetObjectPtrLabel", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetOcclusionQueryivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetOcclusionQueryivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetOcclusionQueryivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetOcclusionQueryivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetOcclusionQueryuivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetOcclusionQueryuivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetOcclusionQueryuivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetOcclusionQueryuivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPerfCounterInfoINTEL(int p0, int p1, int p2, byte[] p3, int p4, int p5, byte[] p6, int p7, int[] p8, int p9, int[] p10, int p11, int[] p12, int p13, int[] p14, int p15, long[] p16, int p17) {
    record("glGetPerfCounterInfoINTEL", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17});
  }

  @Override public void glGetPerfCounterInfoINTEL(int p0, int p1, int p2, java.nio.ByteBuffer p3, int p4, java.nio.ByteBuffer p5, java.nio.IntBuffer p6, java.nio.IntBuffer p7, java.nio.IntBuffer p8, java.nio.IntBuffer p9, java.nio.LongBuffer p10) {
    record("glGetPerfCounterInfoINTEL", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glGetPerfQueryDataINTEL(int p0, int p1, int p2, java.nio.Buffer p3, int[] p4, int p5) {
    record("glGetPerfQueryDataINTEL", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetPerfQueryDataINTEL(int p0, int p1, int p2, java.nio.Buffer p3, java.nio.IntBuffer p4) {
    record("glGetPerfQueryDataINTEL", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetPerfQueryIdByNameINTEL(byte[] p0, int p1, int[] p2, int p3) {
    record("glGetPerfQueryIdByNameINTEL", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetPerfQueryIdByNameINTEL(java.nio.ByteBuffer p0, java.nio.IntBuffer p1) {
    record("glGetPerfQueryIdByNameINTEL", new Object[] {p0, p1});
  }

  @Override public void glGetPerfQueryInfoINTEL(int p0, int p1, byte[] p2, int p3, int[] p4, int p5, int[] p6, int p7, int[] p8, int p9, int[] p10, int p11) {
    record("glGetPerfQueryInfoINTEL", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glGetPerfQueryInfoINTEL(int p0, int p1, java.nio.ByteBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.IntBuffer p6) {
    record("glGetPerfQueryInfoINTEL", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetPixelMapfv(int p0, float[] p1, int p2) {
    record("glGetPixelMapfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPixelMapfv(int p0, java.nio.FloatBuffer p1) {
    record("glGetPixelMapfv", new Object[] {p0, p1});
  }

  @Override public void glGetPixelMapfv(int p0, long p1) {
    record("glGetPixelMapfv", new Object[] {p0, p1});
  }

  @Override public void glGetPixelMapuiv(int p0, int[] p1, int p2) {
    record("glGetPixelMapuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPixelMapuiv(int p0, java.nio.IntBuffer p1) {
    record("glGetPixelMapuiv", new Object[] {p0, p1});
  }

  @Override public void glGetPixelMapuiv(int p0, long p1) {
    record("glGetPixelMapuiv", new Object[] {p0, p1});
  }

  @Override public void glGetPixelMapusv(int p0, java.nio.ShortBuffer p1) {
    record("glGetPixelMapusv", new Object[] {p0, p1});
  }

  @Override public void glGetPixelMapusv(int p0, long p1) {
    record("glGetPixelMapusv", new Object[] {p0, p1});
  }

  @Override public void glGetPixelMapusv(int p0, short[] p1, int p2) {
    record("glGetPixelMapusv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPixelTransformParameterfvEXT(int p0, int p1, float[] p2, int p3) {
    record("glGetPixelTransformParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetPixelTransformParameterfvEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetPixelTransformParameterfvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPixelTransformParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetPixelTransformParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetPixelTransformParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetPixelTransformParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPointeri_vEXT(int p0, int p1, com.jogamp.common.nio.PointerBuffer p2) {
    record("glGetPointeri_vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetPolygonStipple(byte[] p0, int p1) {
    record("glGetPolygonStipple", new Object[] {p0, p1});
  }

  @Override public void glGetPolygonStipple(java.nio.ByteBuffer p0) {
    record("glGetPolygonStipple", new Object[] {p0});
  }

  @Override public void glGetPolygonStipple(long p0) {
    record("glGetPolygonStipple", new Object[] {p0});
  }

  @Override public void glGetProgramBinary(int p0, int p1, int[] p2, int p3, int[] p4, int p5, java.nio.Buffer p6) {
    record("glGetProgramBinary", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glGetProgramBinary(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3, java.nio.Buffer p4) {
    record("glGetProgramBinary", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetProgramEnvParameterIivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramEnvParameterIivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramEnvParameterIivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramEnvParameterIivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramEnvParameterIuivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramEnvParameterIuivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramEnvParameterIuivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramEnvParameterIuivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramEnvParameterdvARB(int p0, int p1, double[] p2, int p3) {
    record("glGetProgramEnvParameterdvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramEnvParameterdvARB(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetProgramEnvParameterdvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramEnvParameterfvARB(int p0, int p1, float[] p2, int p3) {
    record("glGetProgramEnvParameterfvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramEnvParameterfvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetProgramEnvParameterfvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramInfoLog(int p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetProgramInfoLog", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetProgramInfoLog(int p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetProgramInfoLog", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramLocalParameterIivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramLocalParameterIivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramLocalParameterIivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramLocalParameterIivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramLocalParameterIuivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramLocalParameterIuivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramLocalParameterIuivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramLocalParameterIuivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramLocalParameterdvARB(int p0, int p1, double[] p2, int p3) {
    record("glGetProgramLocalParameterdvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramLocalParameterdvARB(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetProgramLocalParameterdvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramLocalParameterfvARB(int p0, int p1, float[] p2, int p3) {
    record("glGetProgramLocalParameterfvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramLocalParameterfvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetProgramLocalParameterfvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramPipelineInfoLog(int p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetProgramPipelineInfoLog", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetProgramPipelineInfoLog(int p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetProgramPipelineInfoLog", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramPipelineiv(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramPipelineiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramPipelineiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramPipelineiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramStringARB(int p0, int p1, java.nio.Buffer p2) {
    record("glGetProgramStringARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramSubroutineParameteruivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramSubroutineParameteruivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramSubroutineParameteruivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramSubroutineParameteruivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramiv(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetProgramivARB(int p0, int p1, int[] p2, int p3) {
    record("glGetProgramivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetProgramivARB(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetProgramivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryIndexediv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetQueryIndexediv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetQueryIndexediv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetQueryIndexediv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjecti64v(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetQueryObjecti64v", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryObjecti64v(int p0, int p1, long[] p2, int p3) {
    record("glGetQueryObjecti64v", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjecti64vEXT(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetQueryObjecti64vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryObjecti64vEXT(int p0, int p1, long[] p2, int p3) {
    record("glGetQueryObjecti64vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjectiv(int p0, int p1, int[] p2, int p3) {
    record("glGetQueryObjectiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjectiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetQueryObjectiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryObjectui64v(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetQueryObjectui64v", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryObjectui64v(int p0, int p1, long[] p2, int p3) {
    record("glGetQueryObjectui64v", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjectui64vEXT(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetQueryObjectui64vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryObjectui64vEXT(int p0, int p1, long[] p2, int p3) {
    record("glGetQueryObjectui64vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjectuiv(int p0, int p1, int[] p2, int p3) {
    record("glGetQueryObjectuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryObjectuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetQueryObjectuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetQueryiv(int p0, int p1, int[] p2, int p3) {
    record("glGetQueryiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetQueryiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetQueryiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetRenderbufferParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetRenderbufferParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetRenderbufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetRenderbufferParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetSamplerParameterIiv(int p0, int p1, int[] p2, int p3) {
    record("glGetSamplerParameterIiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetSamplerParameterIiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetSamplerParameterIiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetSamplerParameterIuiv(int p0, int p1, int[] p2, int p3) {
    record("glGetSamplerParameterIuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetSamplerParameterIuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetSamplerParameterIuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetSemaphoreParameterui64vEXT(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetSemaphoreParameterui64vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetSemaphoreParameterui64vEXT(int p0, int p1, long[] p2, int p3) {
    record("glGetSemaphoreParameterui64vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetSeparableFilter(int p0, int p1, int p2, java.nio.Buffer p3, java.nio.Buffer p4, java.nio.Buffer p5) {
    record("glGetSeparableFilter", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetSeparableFilter(int p0, int p1, int p2, long p3, long p4, long p5) {
    record("glGetSeparableFilter", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetShaderInfoLog(int p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetShaderInfoLog", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetShaderInfoLog(int p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetShaderInfoLog", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetShaderPrecisionFormat(int p0, int p1, int[] p2, int p3, int[] p4, int p5) {
    record("glGetShaderPrecisionFormat", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetShaderPrecisionFormat(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3) {
    record("glGetShaderPrecisionFormat", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetShaderSource(int p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetShaderSource", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetShaderSource(int p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetShaderSource", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetShaderSourceARB(long p0, int p1, int[] p2, int p3, byte[] p4, int p5) {
    record("glGetShaderSourceARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetShaderSourceARB(long p0, int p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
    record("glGetShaderSourceARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetShaderiv(int p0, int p1, int[] p2, int p3) {
    record("glGetShaderiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetShaderiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetShaderiv", new Object[] {p0, p1, p2});
  }

  @Override public short glGetStageIndexNV(int p0) {
    record("glGetStageIndexNV", new Object[] {p0});
    return (short) 0;
  }

  @Override public java.lang.String glGetString(int p0) {
    record("glGetString", new Object[] {p0});
    return null;
  }

  @Override public java.lang.String glGetStringi(int p0, int p1) {
    record("glGetStringi", new Object[] {p0, p1});
    return null;
  }

  @Override public void glGetTexEnvfv(int p0, int p1, float[] p2, int p3) {
    record("glGetTexEnvfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexEnvfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetTexEnvfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexEnviv(int p0, int p1, int[] p2, int p3) {
    record("glGetTexEnviv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexEnviv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetTexEnviv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexGendv(int p0, int p1, double[] p2, int p3) {
    record("glGetTexGendv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexGendv(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetTexGendv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexGenfv(int p0, int p1, float[] p2, int p3) {
    record("glGetTexGenfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexGenfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetTexGenfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexGeniv(int p0, int p1, int[] p2, int p3) {
    record("glGetTexGeniv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexGeniv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetTexGeniv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexImage(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glGetTexImage", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTexImage(int p0, int p1, int p2, int p3, long p4) {
    record("glGetTexImage", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTexLevelParameterfv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetTexLevelParameterfv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTexLevelParameterfv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetTexLevelParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexLevelParameteriv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetTexLevelParameteriv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTexLevelParameteriv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetTexLevelParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexParameterIiv(int p0, int p1, int[] p2, int p3) {
    record("glGetTexParameterIiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexParameterIiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetTexParameterIiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexParameterIuiv(int p0, int p1, int[] p2, int p3) {
    record("glGetTexParameterIuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexParameterIuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetTexParameterIuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glGetTexParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetTexParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTexParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glGetTexParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTexParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetTexParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetTextureImageEXT(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glGetTextureImageEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetTextureLevelParameterfvEXT(int p0, int p1, int p2, int p3, float[] p4, int p5) {
    record("glGetTextureLevelParameterfvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetTextureLevelParameterfvEXT(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
    record("glGetTextureLevelParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTextureLevelParameterivEXT(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glGetTextureLevelParameterivEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetTextureLevelParameterivEXT(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glGetTextureLevelParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTextureParameterIivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetTextureParameterIivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTextureParameterIivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetTextureParameterIivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTextureParameterIuivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetTextureParameterIuivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTextureParameterIuivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetTextureParameterIuivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTextureParameterfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetTextureParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTextureParameterfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetTextureParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTextureParameterivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetTextureParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetTextureParameterivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetTextureParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetTransformFeedbackVarying(int p0, int p1, int p2, int[] p3, int p4, int[] p5, int p6, int[] p7, int p8, byte[] p9, int p10) {
    record("glGetTransformFeedbackVarying", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glGetTransformFeedbackVarying(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, java.nio.ByteBuffer p6) {
    record("glGetTransformFeedbackVarying", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public int glGetUniformBlockIndex(int p0, java.lang.String p1) {
    record("glGetUniformBlockIndex", new Object[] {p0, p1});
    return 0;
  }

  @Override public int glGetUniformBufferSizeEXT(int p0, int p1) {
    record("glGetUniformBufferSizeEXT", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glGetUniformIndices(int p0, int p1, java.lang.String[] p2, int[] p3, int p4) {
    record("glGetUniformIndices", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetUniformIndices(int p0, int p1, java.lang.String[] p2, java.nio.IntBuffer p3) {
    record("glGetUniformIndices", new Object[] {p0, p1, p2, p3});
  }

  @Override public int glGetUniformLocation(int p0, java.lang.String p1) {
    record("glGetUniformLocation", new Object[] {p0, p1});
    return 0;
  }

  @Override public int glGetUniformLocationARB(long p0, java.lang.String p1) {
    record("glGetUniformLocationARB", new Object[] {p0, p1});
    return 0;
  }

  @Override public long glGetUniformOffsetEXT(int p0, int p1) {
    record("glGetUniformOffsetEXT", new Object[] {p0, p1});
    return 0L;
  }

  @Override public void glGetUniformfv(int p0, int p1, float[] p2, int p3) {
    record("glGetUniformfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUniformfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetUniformfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUniformfvARB(long p0, int p1, float[] p2, int p3) {
    record("glGetUniformfvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUniformfvARB(long p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetUniformfvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUniformiv(int p0, int p1, int[] p2, int p3) {
    record("glGetUniformiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUniformiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetUniformiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUniformivARB(long p0, int p1, int[] p2, int p3) {
    record("glGetUniformivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUniformivARB(long p0, int p1, java.nio.IntBuffer p2) {
    record("glGetUniformivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUniformui64vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetUniformui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUniformui64vNV(int p0, int p1, long[] p2, int p3) {
    record("glGetUniformui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUniformuiv(int p0, int p1, int[] p2, int p3) {
    record("glGetUniformuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUniformuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetUniformuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUnsignedBytei_vEXT(int p0, int p1, byte[] p2, int p3) {
    record("glGetUnsignedBytei_vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetUnsignedBytei_vEXT(int p0, int p1, java.nio.ByteBuffer p2) {
    record("glGetUnsignedBytei_vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUnsignedBytevEXT(int p0, byte[] p1, int p2) {
    record("glGetUnsignedBytevEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetUnsignedBytevEXT(int p0, java.nio.ByteBuffer p1) {
    record("glGetUnsignedBytevEXT", new Object[] {p0, p1});
  }

  @Override public void glGetVariantBooleanvEXT(int p0, int p1, byte[] p2, int p3) {
    record("glGetVariantBooleanvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVariantBooleanvEXT(int p0, int p1, java.nio.ByteBuffer p2) {
    record("glGetVariantBooleanvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVariantFloatvEXT(int p0, int p1, float[] p2, int p3) {
    record("glGetVariantFloatvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVariantFloatvEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetVariantFloatvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVariantIntegervEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetVariantIntegervEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVariantIntegervEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVariantIntegervEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexArrayIntegeri_vEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetVertexArrayIntegeri_vEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetVertexArrayIntegeri_vEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetVertexArrayIntegeri_vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexArrayIntegervEXT(int p0, int p1, int[] p2, int p3) {
    record("glGetVertexArrayIntegervEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexArrayIntegervEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVertexArrayIntegervEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexArrayPointeri_vEXT(int p0, int p1, int p2, com.jogamp.common.nio.PointerBuffer p3) {
    record("glGetVertexArrayPointeri_vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexArrayPointervEXT(int p0, int p1, com.jogamp.common.nio.PointerBuffer p2) {
    record("glGetVertexArrayPointervEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribIiv(int p0, int p1, int[] p2, int p3) {
    record("glGetVertexAttribIiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribIiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVertexAttribIiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribIuiv(int p0, int p1, int[] p2, int p3) {
    record("glGetVertexAttribIuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribIuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVertexAttribIuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribLdv(int p0, int p1, double[] p2, int p3) {
    record("glGetVertexAttribLdv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribLdv(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetVertexAttribLdv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribLi64vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetVertexAttribLi64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribLi64vNV(int p0, int p1, long[] p2, int p3) {
    record("glGetVertexAttribLi64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribLui64vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glGetVertexAttribLui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribLui64vNV(int p0, int p1, long[] p2, int p3) {
    record("glGetVertexAttribLui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribdv(int p0, int p1, double[] p2, int p3) {
    record("glGetVertexAttribdv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribdv(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetVertexAttribdv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribdvARB(int p0, int p1, double[] p2, int p3) {
    record("glGetVertexAttribdvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribdvARB(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glGetVertexAttribdvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribfv(int p0, int p1, float[] p2, int p3) {
    record("glGetVertexAttribfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetVertexAttribfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribfvARB(int p0, int p1, float[] p2, int p3) {
    record("glGetVertexAttribfvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribfvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetVertexAttribfvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribiv(int p0, int p1, int[] p2, int p3) {
    record("glGetVertexAttribiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVertexAttribiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVertexAttribivARB(int p0, int p1, int[] p2, int p3) {
    record("glGetVertexAttribivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVertexAttribivARB(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVertexAttribivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glGetVideoCaptureStreamdvNV(int p0, int p1, int p2, double[] p3, int p4) {
    record("glGetVideoCaptureStreamdvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetVideoCaptureStreamdvNV(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glGetVideoCaptureStreamdvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVideoCaptureStreamfvNV(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetVideoCaptureStreamfvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetVideoCaptureStreamfvNV(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetVideoCaptureStreamfvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVideoCaptureStreamivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetVideoCaptureStreamivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetVideoCaptureStreamivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetVideoCaptureStreamivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVideoCaptureivNV(int p0, int p1, int[] p2, int p3) {
    record("glGetVideoCaptureivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetVideoCaptureivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetVideoCaptureivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glGetnColorTable(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glGetnColorTable", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnCompressedTexImage(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glGetnCompressedTexImage", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnConvolutionFilter(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glGetnConvolutionFilter", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnHistogram(int p0, boolean p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glGetnHistogram", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetnMapdv(int p0, int p1, int p2, double[] p3, int p4) {
    record("glGetnMapdv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnMapdv(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glGetnMapdv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnMapfv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetnMapfv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnMapfv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetnMapfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnMapiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetnMapiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnMapiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetnMapiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnMinmax(int p0, boolean p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glGetnMinmax", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetnPixelMapfv(int p0, int p1, float[] p2, int p3) {
    record("glGetnPixelMapfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnPixelMapfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glGetnPixelMapfv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetnPixelMapuiv(int p0, int p1, int[] p2, int p3) {
    record("glGetnPixelMapuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnPixelMapuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glGetnPixelMapuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetnPixelMapusv(int p0, int p1, java.nio.ShortBuffer p2) {
    record("glGetnPixelMapusv", new Object[] {p0, p1, p2});
  }

  @Override public void glGetnPixelMapusv(int p0, int p1, short[] p2, int p3) {
    record("glGetnPixelMapusv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnPolygonStipple(int p0, byte[] p1, int p2) {
    record("glGetnPolygonStipple", new Object[] {p0, p1, p2});
  }

  @Override public void glGetnPolygonStipple(int p0, java.nio.ByteBuffer p1) {
    record("glGetnPolygonStipple", new Object[] {p0, p1});
  }

  @Override public void glGetnSeparableFilter(int p0, int p1, int p2, int p3, java.nio.Buffer p4, int p5, java.nio.Buffer p6, java.nio.Buffer p7) {
    record("glGetnSeparableFilter", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glGetnTexImage(int p0, int p1, int p2, int p3, int p4, java.nio.Buffer p5) {
    record("glGetnTexImage", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glGetnUniformdv(int p0, int p1, int p2, double[] p3, int p4) {
    record("glGetnUniformdv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnUniformdv(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glGetnUniformdv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnUniformfv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glGetnUniformfv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnUniformfv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glGetnUniformfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnUniformiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetnUniformiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnUniformiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetnUniformiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glGetnUniformuiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glGetnUniformuiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glGetnUniformuiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glGetnUniformuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glHint(int p0, int p1) {
    record("glHint", new Object[] {p0, p1});
  }

  @Override public void glHistogram(int p0, int p1, int p2, boolean p3) {
    record("glHistogram", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glImportMemoryFdEXT(int p0, long p1, int p2, int p3) {
    record("glImportMemoryFdEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glImportSemaphoreFdEXT(int p0, int p1, int p2) {
    record("glImportSemaphoreFdEXT", new Object[] {p0, p1, p2});
  }

  @Override public long glImportSyncEXT(int p0, long p1, int p2) {
    record("glImportSyncEXT", new Object[] {p0, p1, p2});
    return 0L;
  }

  @Override public void glIndexFormatNV(int p0, int p1) {
    record("glIndexFormatNV", new Object[] {p0, p1});
  }

  @Override public void glIndexFuncEXT(int p0, float p1) {
    record("glIndexFuncEXT", new Object[] {p0, p1});
  }

  @Override public void glIndexMask(int p0) {
    record("glIndexMask", new Object[] {p0});
  }

  @Override public void glIndexMaterialEXT(int p0, int p1) {
    record("glIndexMaterialEXT", new Object[] {p0, p1});
  }

  @Override public void glIndexPointer(int p0, int p1, java.nio.Buffer p2) {
    record("glIndexPointer", new Object[] {p0, p1, p2});
  }

  @Override public void glIndexd(double p0) {
    record("glIndexd", new Object[] {p0});
  }

  @Override public void glIndexdv(double[] p0, int p1) {
    record("glIndexdv", new Object[] {p0, p1});
  }

  @Override public void glIndexdv(java.nio.DoubleBuffer p0) {
    record("glIndexdv", new Object[] {p0});
  }

  @Override public void glIndexf(float p0) {
    record("glIndexf", new Object[] {p0});
  }

  @Override public void glIndexfv(float[] p0, int p1) {
    record("glIndexfv", new Object[] {p0, p1});
  }

  @Override public void glIndexfv(java.nio.FloatBuffer p0) {
    record("glIndexfv", new Object[] {p0});
  }

  @Override public void glIndexi(int p0) {
    record("glIndexi", new Object[] {p0});
  }

  @Override public void glIndexiv(int[] p0, int p1) {
    record("glIndexiv", new Object[] {p0, p1});
  }

  @Override public void glIndexiv(java.nio.IntBuffer p0) {
    record("glIndexiv", new Object[] {p0});
  }

  @Override public void glIndexs(short p0) {
    record("glIndexs", new Object[] {p0});
  }

  @Override public void glIndexsv(java.nio.ShortBuffer p0) {
    record("glIndexsv", new Object[] {p0});
  }

  @Override public void glIndexsv(short[] p0, int p1) {
    record("glIndexsv", new Object[] {p0, p1});
  }

  @Override public void glIndexub(byte p0) {
    record("glIndexub", new Object[] {p0});
  }

  @Override public void glIndexubv(byte[] p0, int p1) {
    record("glIndexubv", new Object[] {p0, p1});
  }

  @Override public void glIndexubv(java.nio.ByteBuffer p0) {
    record("glIndexubv", new Object[] {p0});
  }

  @Override public void glInitNames() {
    record("glInitNames", new Object[] {});
  }

  @Override public void glInsertComponentEXT(int p0, int p1, int p2) {
    record("glInsertComponentEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glInterleavedArrays(int p0, int p1, java.nio.Buffer p2) {
    record("glInterleavedArrays", new Object[] {p0, p1, p2});
  }

  @Override public void glInterleavedArrays(int p0, int p1, long p2) {
    record("glInterleavedArrays", new Object[] {p0, p1, p2});
  }

  @Override public void glInvalidateBufferData(int p0) {
    record("glInvalidateBufferData", new Object[] {p0});
  }

  @Override public void glInvalidateBufferSubData(int p0, long p1, long p2) {
    record("glInvalidateBufferSubData", new Object[] {p0, p1, p2});
  }

  @Override public void glInvalidateFramebuffer(int p0, int p1, int[] p2, int p3) {
    record("glInvalidateFramebuffer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glInvalidateFramebuffer(int p0, int p1, java.nio.IntBuffer p2) {
    record("glInvalidateFramebuffer", new Object[] {p0, p1, p2});
  }

  @Override public void glInvalidateSubFramebuffer(int p0, int p1, int[] p2, int p3, int p4, int p5, int p6, int p7) {
    record("glInvalidateSubFramebuffer", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glInvalidateSubFramebuffer(int p0, int p1, java.nio.IntBuffer p2, int p3, int p4, int p5, int p6) {
    record("glInvalidateSubFramebuffer", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glInvalidateTexImage(int p0, int p1) {
    record("glInvalidateTexImage", new Object[] {p0, p1});
  }

  @Override public void glInvalidateTexSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    record("glInvalidateTexSubImage", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public boolean glIsBuffer(int p0) {
    record("glIsBuffer", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsBufferResidentNV(int p0) {
    record("glIsBufferResidentNV", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsCommandListNV(int p0) {
    record("glIsCommandListNV", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsEnabled(int p0) {
    record("glIsEnabled", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsEnabledIndexed(int p0, int p1) {
    record("glIsEnabledIndexed", new Object[] {p0, p1});
    return false;
  }

  @Override public boolean glIsEnabledi(int p0, int p1) {
    record("glIsEnabledi", new Object[] {p0, p1});
    return false;
  }

  @Override public boolean glIsFramebuffer(int p0) {
    record("glIsFramebuffer", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsList(int p0) {
    record("glIsList", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsMemoryObjectEXT(int p0) {
    record("glIsMemoryObjectEXT", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsNameAMD(int p0, int p1) {
    record("glIsNameAMD", new Object[] {p0, p1});
    return false;
  }

  @Override public boolean glIsNamedBufferResidentNV(int p0) {
    record("glIsNamedBufferResidentNV", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsOcclusionQueryNV(int p0) {
    record("glIsOcclusionQueryNV", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsProgram(int p0) {
    record("glIsProgram", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsProgramARB(int p0) {
    record("glIsProgramARB", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsProgramPipeline(int p0) {
    record("glIsProgramPipeline", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsQuery(int p0) {
    record("glIsQuery", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsRenderbuffer(int p0) {
    record("glIsRenderbuffer", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsSemaphoreEXT(int p0) {
    record("glIsSemaphoreEXT", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsShader(int p0) {
    record("glIsShader", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsStateNV(int p0) {
    record("glIsStateNV", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsTexture(int p0) {
    record("glIsTexture", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsTransformFeedback(int p0) {
    record("glIsTransformFeedback", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsTransformFeedbackNV(int p0) {
    record("glIsTransformFeedbackNV", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsVariantEnabledEXT(int p0, int p1) {
    record("glIsVariantEnabledEXT", new Object[] {p0, p1});
    return false;
  }

  @Override public boolean glIsVertexArray(int p0) {
    record("glIsVertexArray", new Object[] {p0});
    return false;
  }

  @Override public boolean glIsVertexAttribEnabledAPPLE(int p0, int p1) {
    record("glIsVertexAttribEnabledAPPLE", new Object[] {p0, p1});
    return false;
  }

  @Override public void glLGPUCopyImageSubDataNVX(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12, int p13, int p14, int p15, int p16) {
    record("glLGPUCopyImageSubDataNVX", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16});
  }

  @Override public void glLGPUInterlockNVX() {
    record("glLGPUInterlockNVX", new Object[] {});
  }

  @Override public void glLGPUNamedBufferSubDataNVX(int p0, int p1, long p2, long p3, java.nio.Buffer p4) {
    record("glLGPUNamedBufferSubDataNVX", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glLightModelf(int p0, float p1) {
    record("glLightModelf", new Object[] {p0, p1});
  }

  @Override public void glLightModelfv(int p0, float[] p1, int p2) {
    record("glLightModelfv", new Object[] {p0, p1, p2});
  }

  @Override public void glLightModelfv(int p0, java.nio.FloatBuffer p1) {
    record("glLightModelfv", new Object[] {p0, p1});
  }

  @Override public void glLightModeli(int p0, int p1) {
    record("glLightModeli", new Object[] {p0, p1});
  }

  @Override public void glLightModeliv(int p0, int[] p1, int p2) {
    record("glLightModeliv", new Object[] {p0, p1, p2});
  }

  @Override public void glLightModeliv(int p0, java.nio.IntBuffer p1) {
    record("glLightModeliv", new Object[] {p0, p1});
  }

  @Override public void glLightf(int p0, int p1, float p2) {
    record("glLightf", new Object[] {p0, p1, p2});
  }

  @Override public void glLightfv(int p0, int p1, float[] p2, int p3) {
    record("glLightfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glLightfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glLightfv", new Object[] {p0, p1, p2});
  }

  @Override public void glLighti(int p0, int p1, int p2) {
    record("glLighti", new Object[] {p0, p1, p2});
  }

  @Override public void glLightiv(int p0, int p1, int[] p2, int p3) {
    record("glLightiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glLightiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glLightiv", new Object[] {p0, p1, p2});
  }

  @Override public void glLineStipple(int p0, short p1) {
    record("glLineStipple", new Object[] {p0, p1});
  }

  @Override public void glLineWidth(float p0) {
    record("glLineWidth", new Object[] {p0});
  }

  @Override public void glLinkProgram(int p0) {
    record("glLinkProgram", new Object[] {p0});
  }

  @Override public void glLinkProgramARB(long p0) {
    record("glLinkProgramARB", new Object[] {p0});
  }

  @Override public void glListBase(int p0) {
    record("glListBase", new Object[] {p0});
  }

  @Override public void glListDrawCommandsStatesClientNV(int p0, int p1, com.jogamp.common.nio.PointerBuffer p2, int[] p3, int p4, int[] p5, int p6, int[] p7, int p8, int p9) {
    record("glListDrawCommandsStatesClientNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glListDrawCommandsStatesClientNV(int p0, int p1, com.jogamp.common.nio.PointerBuffer p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5, int p6) {
    record("glListDrawCommandsStatesClientNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glLoadIdentity() {
    record("glLoadIdentity", new Object[] {});
  }

  @Override public void glLoadMatrixd(double[] p0, int p1) {
    record("glLoadMatrixd", new Object[] {p0, p1});
  }

  @Override public void glLoadMatrixd(java.nio.DoubleBuffer p0) {
    record("glLoadMatrixd", new Object[] {p0});
  }

  @Override public void glLoadMatrixf(float[] p0, int p1) {
    record("glLoadMatrixf", new Object[] {p0, p1});
  }

  @Override public void glLoadMatrixf(java.nio.FloatBuffer p0) {
    record("glLoadMatrixf", new Object[] {p0});
  }

  @Override public void glLoadName(int p0) {
    record("glLoadName", new Object[] {p0});
  }

  @Override public void glLoadTransposeMatrixd(double[] p0, int p1) {
    record("glLoadTransposeMatrixd", new Object[] {p0, p1});
  }

  @Override public void glLoadTransposeMatrixd(java.nio.DoubleBuffer p0) {
    record("glLoadTransposeMatrixd", new Object[] {p0});
  }

  @Override public void glLoadTransposeMatrixf(float[] p0, int p1) {
    record("glLoadTransposeMatrixf", new Object[] {p0, p1});
  }

  @Override public void glLoadTransposeMatrixf(java.nio.FloatBuffer p0) {
    record("glLoadTransposeMatrixf", new Object[] {p0});
  }

  @Override public void glLockArraysEXT(int p0, int p1) {
    record("glLockArraysEXT", new Object[] {p0, p1});
  }

  @Override public void glLogicOp(int p0) {
    record("glLogicOp", new Object[] {p0});
  }

  @Override public void glMakeBufferNonResidentNV(int p0) {
    record("glMakeBufferNonResidentNV", new Object[] {p0});
  }

  @Override public void glMakeBufferResidentNV(int p0, int p1) {
    record("glMakeBufferResidentNV", new Object[] {p0, p1});
  }

  @Override public void glMakeNamedBufferNonResidentNV(int p0) {
    record("glMakeNamedBufferNonResidentNV", new Object[] {p0});
  }

  @Override public void glMakeNamedBufferResidentNV(int p0, int p1) {
    record("glMakeNamedBufferResidentNV", new Object[] {p0, p1});
  }

  @Override public void glMap1d(int p0, double p1, double p2, int p3, int p4, double[] p5, int p6) {
    record("glMap1d", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMap1d(int p0, double p1, double p2, int p3, int p4, java.nio.DoubleBuffer p5) {
    record("glMap1d", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMap1f(int p0, float p1, float p2, int p3, int p4, float[] p5, int p6) {
    record("glMap1f", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMap1f(int p0, float p1, float p2, int p3, int p4, java.nio.FloatBuffer p5) {
    record("glMap1f", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMap2d(int p0, double p1, double p2, int p3, int p4, double p5, double p6, int p7, int p8, double[] p9, int p10) {
    record("glMap2d", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glMap2d(int p0, double p1, double p2, int p3, int p4, double p5, double p6, int p7, int p8, java.nio.DoubleBuffer p9) {
    record("glMap2d", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glMap2f(int p0, float p1, float p2, int p3, int p4, float p5, float p6, int p7, int p8, float[] p9, int p10) {
    record("glMap2f", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glMap2f(int p0, float p1, float p2, int p3, int p4, float p5, float p6, int p7, int p8, java.nio.FloatBuffer p9) {
    record("glMap2f", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public java.nio.ByteBuffer glMapBuffer(int p0, int p1) {
    record("glMapBuffer", new Object[] {p0, p1});
    return null;
  }

  @Override public java.nio.ByteBuffer glMapBufferRange(int p0, long p1, long p2, int p3) {
    record("glMapBufferRange", new Object[] {p0, p1, p2, p3});
    return null;
  }

  @Override public void glMapControlPointsNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, boolean p7, java.nio.Buffer p8) {
    record("glMapControlPointsNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glMapGrid1d(int p0, double p1, double p2) {
    record("glMapGrid1d", new Object[] {p0, p1, p2});
  }

  @Override public void glMapGrid1f(int p0, float p1, float p2) {
    record("glMapGrid1f", new Object[] {p0, p1, p2});
  }

  @Override public void glMapGrid2d(int p0, double p1, double p2, int p3, double p4, double p5) {
    record("glMapGrid2d", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMapGrid2f(int p0, float p1, float p2, int p3, float p4, float p5) {
    record("glMapGrid2f", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public java.nio.ByteBuffer glMapNamedBufferEXT(int p0, int p1) {
    record("glMapNamedBufferEXT", new Object[] {p0, p1});
    return null;
  }

  @Override public java.nio.ByteBuffer glMapNamedBufferRangeEXT(int p0, long p1, long p2, int p3) {
    record("glMapNamedBufferRangeEXT", new Object[] {p0, p1, p2, p3});
    return null;
  }

  @Override public void glMapParameterfvNV(int p0, int p1, float[] p2, int p3) {
    record("glMapParameterfvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMapParameterfvNV(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glMapParameterfvNV", new Object[] {p0, p1, p2});
  }

  @Override public void glMapParameterivNV(int p0, int p1, int[] p2, int p3) {
    record("glMapParameterivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMapParameterivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glMapParameterivNV", new Object[] {p0, p1, p2});
  }

  @Override public java.nio.ByteBuffer glMapTexture2DINTEL(int p0, int p1, int p2, int[] p3, int p4, int[] p5, int p6) {
    record("glMapTexture2DINTEL", new Object[] {p0, p1, p2, p3, p4, p5, p6});
    return null;
  }

  @Override public java.nio.ByteBuffer glMapTexture2DINTEL(int p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4) {
    record("glMapTexture2DINTEL", new Object[] {p0, p1, p2, p3, p4});
    return null;
  }

  @Override public void glMapVertexAttrib1dAPPLE(int p0, int p1, double p2, double p3, int p4, int p5, double[] p6, int p7) {
    record("glMapVertexAttrib1dAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glMapVertexAttrib1dAPPLE(int p0, int p1, double p2, double p3, int p4, int p5, java.nio.DoubleBuffer p6) {
    record("glMapVertexAttrib1dAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMapVertexAttrib1fAPPLE(int p0, int p1, float p2, float p3, int p4, int p5, float[] p6, int p7) {
    record("glMapVertexAttrib1fAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glMapVertexAttrib1fAPPLE(int p0, int p1, float p2, float p3, int p4, int p5, java.nio.FloatBuffer p6) {
    record("glMapVertexAttrib1fAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMapVertexAttrib2dAPPLE(int p0, int p1, double p2, double p3, int p4, int p5, double p6, double p7, int p8, int p9, double[] p10, int p11) {
    record("glMapVertexAttrib2dAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glMapVertexAttrib2dAPPLE(int p0, int p1, double p2, double p3, int p4, int p5, double p6, double p7, int p8, int p9, java.nio.DoubleBuffer p10) {
    record("glMapVertexAttrib2dAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glMapVertexAttrib2fAPPLE(int p0, int p1, float p2, float p3, int p4, int p5, float p6, float p7, int p8, int p9, float[] p10, int p11) {
    record("glMapVertexAttrib2fAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glMapVertexAttrib2fAPPLE(int p0, int p1, float p2, float p3, int p4, int p5, float p6, float p7, int p8, int p9, java.nio.FloatBuffer p10) {
    record("glMapVertexAttrib2fAPPLE", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glMaterialf(int p0, int p1, float p2) {
    record("glMaterialf", new Object[] {p0, p1, p2});
  }

  @Override public void glMaterialfv(int p0, int p1, float[] p2, int p3) {
    record("glMaterialfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMaterialfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glMaterialfv", new Object[] {p0, p1, p2});
  }

  @Override public void glMateriali(int p0, int p1, int p2) {
    record("glMateriali", new Object[] {p0, p1, p2});
  }

  @Override public void glMaterialiv(int p0, int p1, int[] p2, int p3) {
    record("glMaterialiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMaterialiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glMaterialiv", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixFrustumEXT(int p0, double p1, double p2, double p3, double p4, double p5, double p6) {
    record("glMatrixFrustumEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMatrixIndexPointerARB(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glMatrixIndexPointerARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMatrixIndexPointerARB(int p0, int p1, int p2, long p3) {
    record("glMatrixIndexPointerARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMatrixIndexubvARB(int p0, byte[] p1, int p2) {
    record("glMatrixIndexubvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixIndexubvARB(int p0, java.nio.ByteBuffer p1) {
    record("glMatrixIndexubvARB", new Object[] {p0, p1});
  }

  @Override public void glMatrixIndexuivARB(int p0, int[] p1, int p2) {
    record("glMatrixIndexuivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixIndexuivARB(int p0, java.nio.IntBuffer p1) {
    record("glMatrixIndexuivARB", new Object[] {p0, p1});
  }

  @Override public void glMatrixIndexusvARB(int p0, java.nio.ShortBuffer p1) {
    record("glMatrixIndexusvARB", new Object[] {p0, p1});
  }

  @Override public void glMatrixIndexusvARB(int p0, short[] p1, int p2) {
    record("glMatrixIndexusvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixLoadIdentityEXT(int p0) {
    record("glMatrixLoadIdentityEXT", new Object[] {p0});
  }

  @Override public void glMatrixLoadTransposedEXT(int p0, double[] p1, int p2) {
    record("glMatrixLoadTransposedEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixLoadTransposedEXT(int p0, java.nio.DoubleBuffer p1) {
    record("glMatrixLoadTransposedEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixLoadTransposefEXT(int p0, float[] p1, int p2) {
    record("glMatrixLoadTransposefEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixLoadTransposefEXT(int p0, java.nio.FloatBuffer p1) {
    record("glMatrixLoadTransposefEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixLoaddEXT(int p0, double[] p1, int p2) {
    record("glMatrixLoaddEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixLoaddEXT(int p0, java.nio.DoubleBuffer p1) {
    record("glMatrixLoaddEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixLoadfEXT(int p0, float[] p1, int p2) {
    record("glMatrixLoadfEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixLoadfEXT(int p0, java.nio.FloatBuffer p1) {
    record("glMatrixLoadfEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixMode(int p0) {
    record("glMatrixMode", new Object[] {p0});
  }

  @Override public void glMatrixMultTransposedEXT(int p0, double[] p1, int p2) {
    record("glMatrixMultTransposedEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixMultTransposedEXT(int p0, java.nio.DoubleBuffer p1) {
    record("glMatrixMultTransposedEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixMultTransposefEXT(int p0, float[] p1, int p2) {
    record("glMatrixMultTransposefEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixMultTransposefEXT(int p0, java.nio.FloatBuffer p1) {
    record("glMatrixMultTransposefEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixMultdEXT(int p0, double[] p1, int p2) {
    record("glMatrixMultdEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixMultdEXT(int p0, java.nio.DoubleBuffer p1) {
    record("glMatrixMultdEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixMultfEXT(int p0, float[] p1, int p2) {
    record("glMatrixMultfEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMatrixMultfEXT(int p0, java.nio.FloatBuffer p1) {
    record("glMatrixMultfEXT", new Object[] {p0, p1});
  }

  @Override public void glMatrixOrthoEXT(int p0, double p1, double p2, double p3, double p4, double p5, double p6) {
    record("glMatrixOrthoEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMatrixPopEXT(int p0) {
    record("glMatrixPopEXT", new Object[] {p0});
  }

  @Override public void glMatrixPushEXT(int p0) {
    record("glMatrixPushEXT", new Object[] {p0});
  }

  @Override public void glMatrixRotatedEXT(int p0, double p1, double p2, double p3, double p4) {
    record("glMatrixRotatedEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMatrixRotatefEXT(int p0, float p1, float p2, float p3, float p4) {
    record("glMatrixRotatefEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMatrixScaledEXT(int p0, double p1, double p2, double p3) {
    record("glMatrixScaledEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMatrixScalefEXT(int p0, float p1, float p2, float p3) {
    record("glMatrixScalefEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMatrixTranslatedEXT(int p0, double p1, double p2, double p3) {
    record("glMatrixTranslatedEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMatrixTranslatefEXT(int p0, float p1, float p2, float p3) {
    record("glMatrixTranslatefEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMaxShaderCompilerThreadsKHR(int p0) {
    record("glMaxShaderCompilerThreadsKHR", new Object[] {p0});
  }

  @Override public void glMemoryBarrier(int p0) {
    record("glMemoryBarrier", new Object[] {p0});
  }

  @Override public void glMemoryObjectParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glMemoryObjectParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMemoryObjectParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glMemoryObjectParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMinSampleShading(float p0) {
    record("glMinSampleShading", new Object[] {p0});
  }

  @Override public void glMinmax(int p0, int p1, boolean p2) {
    record("glMinmax", new Object[] {p0, p1, p2});
  }

  @Override public void glMultMatrixd(double[] p0, int p1) {
    record("glMultMatrixd", new Object[] {p0, p1});
  }

  @Override public void glMultMatrixd(java.nio.DoubleBuffer p0) {
    record("glMultMatrixd", new Object[] {p0});
  }

  @Override public void glMultMatrixf(float[] p0, int p1) {
    record("glMultMatrixf", new Object[] {p0, p1});
  }

  @Override public void glMultMatrixf(java.nio.FloatBuffer p0) {
    record("glMultMatrixf", new Object[] {p0});
  }

  @Override public void glMultTransposeMatrixd(double[] p0, int p1) {
    record("glMultTransposeMatrixd", new Object[] {p0, p1});
  }

  @Override public void glMultTransposeMatrixd(java.nio.DoubleBuffer p0) {
    record("glMultTransposeMatrixd", new Object[] {p0});
  }

  @Override public void glMultTransposeMatrixf(float[] p0, int p1) {
    record("glMultTransposeMatrixf", new Object[] {p0, p1});
  }

  @Override public void glMultTransposeMatrixf(java.nio.FloatBuffer p0) {
    record("glMultTransposeMatrixf", new Object[] {p0});
  }

  @Override public void glMultiDrawArrays(int p0, int[] p1, int p2, int[] p3, int p4, int p5) {
    record("glMultiDrawArrays", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMultiDrawArrays(int p0, java.nio.IntBuffer p1, java.nio.IntBuffer p2, int p3) {
    record("glMultiDrawArrays", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiDrawArraysIndirectAMD(int p0, java.nio.Buffer p1, int p2, int p3) {
    record("glMultiDrawArraysIndirectAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiDrawArraysIndirectBindlessCountNV(int p0, java.nio.Buffer p1, int p2, int p3, int p4, int p5) {
    record("glMultiDrawArraysIndirectBindlessCountNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMultiDrawArraysIndirectBindlessNV(int p0, java.nio.Buffer p1, int p2, int p3, int p4) {
    record("glMultiDrawArraysIndirectBindlessNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiDrawElements(int p0, java.nio.IntBuffer p1, int p2, com.jogamp.common.nio.PointerBuffer p3, int p4) {
    record("glMultiDrawElements", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiDrawElementsIndirectAMD(int p0, int p1, java.nio.Buffer p2, int p3, int p4) {
    record("glMultiDrawElementsIndirectAMD", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiDrawElementsIndirectBindlessCountNV(int p0, int p1, java.nio.Buffer p2, int p3, int p4, int p5, int p6) {
    record("glMultiDrawElementsIndirectBindlessCountNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMultiDrawElementsIndirectBindlessNV(int p0, int p1, java.nio.Buffer p2, int p3, int p4, int p5) {
    record("glMultiDrawElementsIndirectBindlessNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMultiTexBufferEXT(int p0, int p1, int p2, int p3) {
    record("glMultiTexBufferEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord1bOES(int p0, byte p1) {
    record("glMultiTexCoord1bOES", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1bvOES(int p0, byte[] p1, int p2) {
    record("glMultiTexCoord1bvOES", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord1bvOES(int p0, java.nio.ByteBuffer p1) {
    record("glMultiTexCoord1bvOES", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1d(int p0, double p1) {
    record("glMultiTexCoord1d", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1dv(int p0, double[] p1, int p2) {
    record("glMultiTexCoord1dv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord1dv(int p0, java.nio.DoubleBuffer p1) {
    record("glMultiTexCoord1dv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1f(int p0, float p1) {
    record("glMultiTexCoord1f", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1fv(int p0, float[] p1, int p2) {
    record("glMultiTexCoord1fv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord1fv(int p0, java.nio.FloatBuffer p1) {
    record("glMultiTexCoord1fv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1h(int p0, short p1) {
    record("glMultiTexCoord1h", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1hv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord1hv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1hv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord1hv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord1i(int p0, int p1) {
    record("glMultiTexCoord1i", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1iv(int p0, int[] p1, int p2) {
    record("glMultiTexCoord1iv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord1iv(int p0, java.nio.IntBuffer p1) {
    record("glMultiTexCoord1iv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1s(int p0, short p1) {
    record("glMultiTexCoord1s", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1sv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord1sv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord1sv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord1sv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2bOES(int p0, byte p1, byte p2) {
    record("glMultiTexCoord2bOES", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2bvOES(int p0, byte[] p1, int p2) {
    record("glMultiTexCoord2bvOES", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2bvOES(int p0, java.nio.ByteBuffer p1) {
    record("glMultiTexCoord2bvOES", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord2d(int p0, double p1, double p2) {
    record("glMultiTexCoord2d", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2dv(int p0, double[] p1, int p2) {
    record("glMultiTexCoord2dv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2dv(int p0, java.nio.DoubleBuffer p1) {
    record("glMultiTexCoord2dv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord2f(int p0, float p1, float p2) {
    record("glMultiTexCoord2f", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2fv(int p0, float[] p1, int p2) {
    record("glMultiTexCoord2fv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2fv(int p0, java.nio.FloatBuffer p1) {
    record("glMultiTexCoord2fv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord2h(int p0, short p1, short p2) {
    record("glMultiTexCoord2h", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2hv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord2hv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord2hv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord2hv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2i(int p0, int p1, int p2) {
    record("glMultiTexCoord2i", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2iv(int p0, int[] p1, int p2) {
    record("glMultiTexCoord2iv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2iv(int p0, java.nio.IntBuffer p1) {
    record("glMultiTexCoord2iv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord2s(int p0, short p1, short p2) {
    record("glMultiTexCoord2s", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord2sv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord2sv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord2sv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord2sv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord3bOES(int p0, byte p1, byte p2, byte p3) {
    record("glMultiTexCoord3bOES", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord3bvOES(int p0, byte[] p1, int p2) {
    record("glMultiTexCoord3bvOES", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord3bvOES(int p0, java.nio.ByteBuffer p1) {
    record("glMultiTexCoord3bvOES", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord3d(int p0, double p1, double p2, double p3) {
    record("glMultiTexCoord3d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord3dv(int p0, double[] p1, int p2) {
    record("glMultiTexCoord3dv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord3dv(int p0, java.nio.DoubleBuffer p1) {
    record("glMultiTexCoord3dv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord3f(int p0, float p1, float p2, float p3) {
    record("glMultiTexCoord3f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord3fv(int p0, float[] p1, int p2) {
    record("glMultiTexCoord3fv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord3fv(int p0, java.nio.FloatBuffer p1) {
    record("glMultiTexCoord3fv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord3h(int p0, short p1, short p2, short p3) {
    record("glMultiTexCoord3h", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord3hv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord3hv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord3hv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord3hv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord3i(int p0, int p1, int p2, int p3) {
    record("glMultiTexCoord3i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord3iv(int p0, int[] p1, int p2) {
    record("glMultiTexCoord3iv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord3iv(int p0, java.nio.IntBuffer p1) {
    record("glMultiTexCoord3iv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord3s(int p0, short p1, short p2, short p3) {
    record("glMultiTexCoord3s", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexCoord3sv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord3sv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord3sv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord3sv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord4bOES(int p0, byte p1, byte p2, byte p3, byte p4) {
    record("glMultiTexCoord4bOES", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexCoord4bvOES(int p0, byte[] p1, int p2) {
    record("glMultiTexCoord4bvOES", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord4bvOES(int p0, java.nio.ByteBuffer p1) {
    record("glMultiTexCoord4bvOES", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord4d(int p0, double p1, double p2, double p3, double p4) {
    record("glMultiTexCoord4d", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexCoord4dv(int p0, double[] p1, int p2) {
    record("glMultiTexCoord4dv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord4dv(int p0, java.nio.DoubleBuffer p1) {
    record("glMultiTexCoord4dv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord4f(int p0, float p1, float p2, float p3, float p4) {
    record("glMultiTexCoord4f", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexCoord4fv(int p0, float[] p1, int p2) {
    record("glMultiTexCoord4fv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord4fv(int p0, java.nio.FloatBuffer p1) {
    record("glMultiTexCoord4fv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord4h(int p0, short p1, short p2, short p3, short p4) {
    record("glMultiTexCoord4h", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexCoord4hv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord4hv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord4hv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord4hv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord4i(int p0, int p1, int p2, int p3, int p4) {
    record("glMultiTexCoord4i", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexCoord4iv(int p0, int[] p1, int p2) {
    record("glMultiTexCoord4iv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoord4iv(int p0, java.nio.IntBuffer p1) {
    record("glMultiTexCoord4iv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord4s(int p0, short p1, short p2, short p3, short p4) {
    record("glMultiTexCoord4s", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexCoord4sv(int p0, java.nio.ShortBuffer p1) {
    record("glMultiTexCoord4sv", new Object[] {p0, p1});
  }

  @Override public void glMultiTexCoord4sv(int p0, short[] p1, int p2) {
    record("glMultiTexCoord4sv", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexCoordPointerEXT(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glMultiTexCoordPointerEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexEnvfEXT(int p0, int p1, int p2, float p3) {
    record("glMultiTexEnvfEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexEnvfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glMultiTexEnvfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexEnvfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glMultiTexEnvfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexEnviEXT(int p0, int p1, int p2, int p3) {
    record("glMultiTexEnviEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexEnvivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMultiTexEnvivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexEnvivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMultiTexEnvivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexGendEXT(int p0, int p1, int p2, double p3) {
    record("glMultiTexGendEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexGendvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glMultiTexGendvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexGendvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glMultiTexGendvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexGenfEXT(int p0, int p1, int p2, float p3) {
    record("glMultiTexGenfEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexGenfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glMultiTexGenfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexGenfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glMultiTexGenfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexGeniEXT(int p0, int p1, int p2, int p3) {
    record("glMultiTexGeniEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexGenivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMultiTexGenivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexGenivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMultiTexGenivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glMultiTexImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glMultiTexImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glMultiTexImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glMultiTexImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.Buffer p10) {
    record("glMultiTexImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glMultiTexParameterIivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMultiTexParameterIivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexParameterIivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMultiTexParameterIivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexParameterIuivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMultiTexParameterIuivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexParameterIuivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMultiTexParameterIuivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexParameterfEXT(int p0, int p1, int p2, float p3) {
    record("glMultiTexParameterfEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexParameterfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glMultiTexParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexParameterfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glMultiTexParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexParameteriEXT(int p0, int p1, int p2, int p3) {
    record("glMultiTexParameteriEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexParameterivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMultiTexParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMultiTexParameterivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMultiTexParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMultiTexRenderbufferEXT(int p0, int p1, int p2) {
    record("glMultiTexRenderbufferEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glMultiTexSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glMultiTexSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glMultiTexSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glMultiTexSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glMultiTexSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, java.nio.Buffer p11) {
    record("glMultiTexSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glMulticastBarrierNV() {
    record("glMulticastBarrierNV", new Object[] {});
  }

  @Override public void glMulticastBlitFramebufferNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11) {
    record("glMulticastBlitFramebufferNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glMulticastBufferSubDataNV(int p0, int p1, long p2, long p3, java.nio.Buffer p4) {
    record("glMulticastBufferSubDataNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastCopyBufferSubDataNV(int p0, int p1, int p2, int p3, long p4, long p5, long p6) {
    record("glMulticastCopyBufferSubDataNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glMulticastCopyImageSubDataNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12, int p13, int p14, int p15, int p16) {
    record("glMulticastCopyImageSubDataNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16});
  }

  @Override public void glMulticastFramebufferSampleLocationsfvNV(int p0, int p1, int p2, int p3, float[] p4, int p5) {
    record("glMulticastFramebufferSampleLocationsfvNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glMulticastFramebufferSampleLocationsfvNV(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
    record("glMulticastFramebufferSampleLocationsfvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastGetQueryObjecti64vNV(int p0, int p1, int p2, java.nio.LongBuffer p3) {
    record("glMulticastGetQueryObjecti64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastGetQueryObjecti64vNV(int p0, int p1, int p2, long[] p3, int p4) {
    record("glMulticastGetQueryObjecti64vNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastGetQueryObjectivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMulticastGetQueryObjectivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastGetQueryObjectivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMulticastGetQueryObjectivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastGetQueryObjectui64vNV(int p0, int p1, int p2, java.nio.LongBuffer p3) {
    record("glMulticastGetQueryObjectui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastGetQueryObjectui64vNV(int p0, int p1, int p2, long[] p3, int p4) {
    record("glMulticastGetQueryObjectui64vNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastGetQueryObjectuivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMulticastGetQueryObjectuivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastGetQueryObjectuivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMulticastGetQueryObjectuivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastScissorArrayvNVX(int p0, int p1, int p2, int[] p3, int p4) {
    record("glMulticastScissorArrayvNVX", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastScissorArrayvNVX(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glMulticastScissorArrayvNVX", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastViewportArrayvNVX(int p0, int p1, int p2, float[] p3, int p4) {
    record("glMulticastViewportArrayvNVX", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glMulticastViewportArrayvNVX(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glMulticastViewportArrayvNVX", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastViewportPositionWScaleNVX(int p0, int p1, float p2, float p3) {
    record("glMulticastViewportPositionWScaleNVX", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glMulticastWaitSyncNV(int p0, int p1) {
    record("glMulticastWaitSyncNV", new Object[] {p0, p1});
  }

  @Override public void glNamedBufferDataEXT(int p0, long p1, java.nio.Buffer p2, int p3) {
    record("glNamedBufferDataEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedBufferPageCommitmentARB(int p0, long p1, long p2, boolean p3) {
    record("glNamedBufferPageCommitmentARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedBufferPageCommitmentEXT(int p0, long p1, long p2, boolean p3) {
    record("glNamedBufferPageCommitmentEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedBufferStorageEXT(int p0, long p1, java.nio.Buffer p2, int p3) {
    record("glNamedBufferStorageEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedBufferStorageExternalEXT(int p0, long p1, long p2, java.nio.Buffer p3, int p4) {
    record("glNamedBufferStorageExternalEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedBufferStorageMemEXT(int p0, long p1, int p2, long p3) {
    record("glNamedBufferStorageMemEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedBufferSubDataEXT(int p0, long p1, long p2, java.nio.Buffer p3) {
    record("glNamedBufferSubDataEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedCopyBufferSubDataEXT(int p0, int p1, long p2, long p3, long p4) {
    record("glNamedCopyBufferSubDataEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedFramebufferParameteri(int p0, int p1, int p2) {
    record("glNamedFramebufferParameteri", new Object[] {p0, p1, p2});
  }

  @Override public void glNamedFramebufferRenderbufferEXT(int p0, int p1, int p2, int p3) {
    record("glNamedFramebufferRenderbufferEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedFramebufferSamplePositionsfvAMD(int p0, int p1, int p2, float[] p3, int p4) {
    record("glNamedFramebufferSamplePositionsfvAMD", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedFramebufferSamplePositionsfvAMD(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glNamedFramebufferSamplePositionsfvAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedFramebufferTexture1DEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glNamedFramebufferTexture1DEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedFramebufferTexture2DEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glNamedFramebufferTexture2DEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedFramebufferTexture3DEXT(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glNamedFramebufferTexture3DEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedFramebufferTextureEXT(int p0, int p1, int p2, int p3) {
    record("glNamedFramebufferTextureEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedFramebufferTextureFaceEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glNamedFramebufferTextureFaceEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedFramebufferTextureLayerEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glNamedFramebufferTextureLayerEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedFramebufferTextureMultiviewOVR(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glNamedFramebufferTextureMultiviewOVR", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedProgramLocalParameter4dEXT(int p0, int p1, int p2, double p3, double p4, double p5, double p6) {
    record("glNamedProgramLocalParameter4dEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glNamedProgramLocalParameter4dvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glNamedProgramLocalParameter4dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramLocalParameter4dvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glNamedProgramLocalParameter4dvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedProgramLocalParameter4fEXT(int p0, int p1, int p2, float p3, float p4, float p5, float p6) {
    record("glNamedProgramLocalParameter4fEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glNamedProgramLocalParameter4fvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glNamedProgramLocalParameter4fvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramLocalParameter4fvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glNamedProgramLocalParameter4fvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedProgramLocalParameterI4iEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glNamedProgramLocalParameterI4iEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glNamedProgramLocalParameterI4ivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glNamedProgramLocalParameterI4ivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramLocalParameterI4ivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glNamedProgramLocalParameterI4ivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedProgramLocalParameterI4uiEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glNamedProgramLocalParameterI4uiEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glNamedProgramLocalParameterI4uivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glNamedProgramLocalParameterI4uivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramLocalParameterI4uivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glNamedProgramLocalParameterI4uivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedProgramLocalParameters4fvEXT(int p0, int p1, int p2, int p3, float[] p4, int p5) {
    record("glNamedProgramLocalParameters4fvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedProgramLocalParameters4fvEXT(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
    record("glNamedProgramLocalParameters4fvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramLocalParametersI4ivEXT(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glNamedProgramLocalParametersI4ivEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedProgramLocalParametersI4ivEXT(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glNamedProgramLocalParametersI4ivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramLocalParametersI4uivEXT(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glNamedProgramLocalParametersI4uivEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedProgramLocalParametersI4uivEXT(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glNamedProgramLocalParametersI4uivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedProgramStringEXT(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glNamedProgramStringEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNamedRenderbufferStorageEXT(int p0, int p1, int p2, int p3) {
    record("glNamedRenderbufferStorageEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glNamedRenderbufferStorageMultisampleAdvancedAMD(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glNamedRenderbufferStorageMultisampleAdvancedAMD", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedRenderbufferStorageMultisampleCoverageEXT(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glNamedRenderbufferStorageMultisampleCoverageEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glNamedRenderbufferStorageMultisampleEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glNamedRenderbufferStorageMultisampleEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glNewList(int p0, int p1) {
    record("glNewList", new Object[] {p0, p1});
  }

  @Override public void glNormal3b(byte p0, byte p1, byte p2) {
    record("glNormal3b", new Object[] {p0, p1, p2});
  }

  @Override public void glNormal3bv(byte[] p0, int p1) {
    record("glNormal3bv", new Object[] {p0, p1});
  }

  @Override public void glNormal3bv(java.nio.ByteBuffer p0) {
    record("glNormal3bv", new Object[] {p0});
  }

  @Override public void glNormal3d(double p0, double p1, double p2) {
    record("glNormal3d", new Object[] {p0, p1, p2});
  }

  @Override public void glNormal3dv(double[] p0, int p1) {
    record("glNormal3dv", new Object[] {p0, p1});
  }

  @Override public void glNormal3dv(java.nio.DoubleBuffer p0) {
    record("glNormal3dv", new Object[] {p0});
  }

  @Override public void glNormal3f(float p0, float p1, float p2) {
    record("glNormal3f", new Object[] {p0, p1, p2});
  }

  @Override public void glNormal3fv(float[] p0, int p1) {
    record("glNormal3fv", new Object[] {p0, p1});
  }

  @Override public void glNormal3fv(java.nio.FloatBuffer p0) {
    record("glNormal3fv", new Object[] {p0});
  }

  @Override public void glNormal3h(short p0, short p1, short p2) {
    record("glNormal3h", new Object[] {p0, p1, p2});
  }

  @Override public void glNormal3hv(java.nio.ShortBuffer p0) {
    record("glNormal3hv", new Object[] {p0});
  }

  @Override public void glNormal3hv(short[] p0, int p1) {
    record("glNormal3hv", new Object[] {p0, p1});
  }

  @Override public void glNormal3i(int p0, int p1, int p2) {
    record("glNormal3i", new Object[] {p0, p1, p2});
  }

  @Override public void glNormal3iv(int[] p0, int p1) {
    record("glNormal3iv", new Object[] {p0, p1});
  }

  @Override public void glNormal3iv(java.nio.IntBuffer p0) {
    record("glNormal3iv", new Object[] {p0});
  }

  @Override public void glNormal3s(short p0, short p1, short p2) {
    record("glNormal3s", new Object[] {p0, p1, p2});
  }

  @Override public void glNormal3sv(java.nio.ShortBuffer p0) {
    record("glNormal3sv", new Object[] {p0});
  }

  @Override public void glNormal3sv(short[] p0, int p1) {
    record("glNormal3sv", new Object[] {p0, p1});
  }

  @Override public void glNormalFormatNV(int p0, int p1) {
    record("glNormalFormatNV", new Object[] {p0, p1});
  }

  @Override public void glNormalPointer(com.jogamp.opengl.GLArrayData p0) {
    record("glNormalPointer", new Object[] {p0});
  }

  @Override public void glNormalPointer(int p0, int p1, java.nio.Buffer p2) {
    record("glNormalPointer", new Object[] {p0, p1, p2});
  }

  @Override public void glNormalPointer(int p0, int p1, long p2) {
    record("glNormalPointer", new Object[] {p0, p1, p2});
  }

  @Override public void glObjectLabel(int p0, int p1, int p2, byte[] p3, int p4) {
    record("glObjectLabel", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glObjectLabel(int p0, int p1, int p2, java.nio.ByteBuffer p3) {
    record("glObjectLabel", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glObjectPtrLabel(java.nio.Buffer p0, int p1, byte[] p2, int p3) {
    record("glObjectPtrLabel", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glObjectPtrLabel(java.nio.Buffer p0, int p1, java.nio.ByteBuffer p2) {
    record("glObjectPtrLabel", new Object[] {p0, p1, p2});
  }

  @Override public int glObjectPurgeableAPPLE(int p0, int p1, int p2) {
    record("glObjectPurgeableAPPLE", new Object[] {p0, p1, p2});
    return 0;
  }

  @Override public int glObjectUnpurgeableAPPLE(int p0, int p1, int p2) {
    record("glObjectUnpurgeableAPPLE", new Object[] {p0, p1, p2});
    return 0;
  }

  @Override public void glOrtho(double p0, double p1, double p2, double p3, double p4, double p5) {
    record("glOrtho", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glOrthof(float p0, float p1, float p2, float p3, float p4, float p5) {
    record("glOrthof", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glPNTrianglesfATI(int p0, float p1) {
    record("glPNTrianglesfATI", new Object[] {p0, p1});
  }

  @Override public void glPNTrianglesiATI(int p0, int p1) {
    record("glPNTrianglesiATI", new Object[] {p0, p1});
  }

  @Override public void glPassThrough(float p0) {
    record("glPassThrough", new Object[] {p0});
  }

  @Override public void glPauseTransformFeedback() {
    record("glPauseTransformFeedback", new Object[] {});
  }

  @Override public void glPauseTransformFeedbackNV() {
    record("glPauseTransformFeedbackNV", new Object[] {});
  }

  @Override public void glPixelDataRangeNV(int p0, int p1, java.nio.Buffer p2) {
    record("glPixelDataRangeNV", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapfv(int p0, int p1, float[] p2, int p3) {
    record("glPixelMapfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glPixelMapfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glPixelMapfv", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapfv(int p0, int p1, long p2) {
    record("glPixelMapfv", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapuiv(int p0, int p1, int[] p2, int p3) {
    record("glPixelMapuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glPixelMapuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glPixelMapuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapuiv(int p0, int p1, long p2) {
    record("glPixelMapuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapusv(int p0, int p1, java.nio.ShortBuffer p2) {
    record("glPixelMapusv", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapusv(int p0, int p1, long p2) {
    record("glPixelMapusv", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelMapusv(int p0, int p1, short[] p2, int p3) {
    record("glPixelMapusv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glPixelStoref(int p0, float p1) {
    record("glPixelStoref", new Object[] {p0, p1});
  }

  @Override public void glPixelStorei(int p0, int p1) {
    record("glPixelStorei", new Object[] {p0, p1});
  }

  @Override public void glPixelTransferf(int p0, float p1) {
    record("glPixelTransferf", new Object[] {p0, p1});
  }

  @Override public void glPixelTransferi(int p0, int p1) {
    record("glPixelTransferi", new Object[] {p0, p1});
  }

  @Override public void glPixelTransformParameterfEXT(int p0, int p1, float p2) {
    record("glPixelTransformParameterfEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelTransformParameterfvEXT(int p0, int p1, float[] p2, int p3) {
    record("glPixelTransformParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glPixelTransformParameterfvEXT(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glPixelTransformParameterfvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelTransformParameteriEXT(int p0, int p1, int p2) {
    record("glPixelTransformParameteriEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelTransformParameterivEXT(int p0, int p1, int[] p2, int p3) {
    record("glPixelTransformParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glPixelTransformParameterivEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glPixelTransformParameterivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glPixelZoom(float p0, float p1) {
    record("glPixelZoom", new Object[] {p0, p1});
  }

  @Override public void glPointParameterf(int p0, float p1) {
    record("glPointParameterf", new Object[] {p0, p1});
  }

  @Override public void glPointParameterfv(int p0, float[] p1, int p2) {
    record("glPointParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glPointParameterfv(int p0, java.nio.FloatBuffer p1) {
    record("glPointParameterfv", new Object[] {p0, p1});
  }

  @Override public void glPointParameteri(int p0, int p1) {
    record("glPointParameteri", new Object[] {p0, p1});
  }

  @Override public void glPointParameteriv(int p0, int[] p1, int p2) {
    record("glPointParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glPointParameteriv(int p0, java.nio.IntBuffer p1) {
    record("glPointParameteriv", new Object[] {p0, p1});
  }

  @Override public void glPointSize(float p0) {
    record("glPointSize", new Object[] {p0});
  }

  @Override public void glPolygonMode(int p0, int p1) {
    record("glPolygonMode", new Object[] {p0, p1});
  }

  @Override public void glPolygonOffset(float p0, float p1) {
    record("glPolygonOffset", new Object[] {p0, p1});
  }

  @Override public void glPolygonOffsetClampEXT(float p0, float p1, float p2) {
    record("glPolygonOffsetClampEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glPolygonStipple(byte[] p0, int p1) {
    record("glPolygonStipple", new Object[] {p0, p1});
  }

  @Override public void glPolygonStipple(java.nio.ByteBuffer p0) {
    record("glPolygonStipple", new Object[] {p0});
  }

  @Override public void glPolygonStipple(long p0) {
    record("glPolygonStipple", new Object[] {p0});
  }

  @Override public void glPopAttrib() {
    record("glPopAttrib", new Object[] {});
  }

  @Override public void glPopClientAttrib() {
    record("glPopClientAttrib", new Object[] {});
  }

  @Override public void glPopDebugGroup() {
    record("glPopDebugGroup", new Object[] {});
  }

  @Override public void glPopMatrix() {
    record("glPopMatrix", new Object[] {});
  }

  @Override public void glPopName() {
    record("glPopName", new Object[] {});
  }

  @Override public void glPrimitiveRestartIndex(int p0) {
    record("glPrimitiveRestartIndex", new Object[] {p0});
  }

  @Override public void glPrimitiveRestartIndexNV(int p0) {
    record("glPrimitiveRestartIndexNV", new Object[] {p0});
  }

  @Override public void glPrimitiveRestartNV() {
    record("glPrimitiveRestartNV", new Object[] {});
  }

  @Override public void glPrioritizeTextures(int p0, int[] p1, int p2, float[] p3, int p4) {
    record("glPrioritizeTextures", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glPrioritizeTextures(int p0, java.nio.IntBuffer p1, java.nio.FloatBuffer p2) {
    record("glPrioritizeTextures", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramBinary(int p0, int p1, java.nio.Buffer p2, int p3) {
    record("glProgramBinary", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramBufferParametersIivNV(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glProgramBufferParametersIivNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramBufferParametersIivNV(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glProgramBufferParametersIivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramBufferParametersIuivNV(int p0, int p1, int p2, int p3, int[] p4, int p5) {
    record("glProgramBufferParametersIuivNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramBufferParametersIuivNV(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
    record("glProgramBufferParametersIuivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramBufferParametersfvNV(int p0, int p1, int p2, int p3, float[] p4, int p5) {
    record("glProgramBufferParametersfvNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramBufferParametersfvNV(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
    record("glProgramBufferParametersfvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramEnvParameter4dARB(int p0, int p1, double p2, double p3, double p4, double p5) {
    record("glProgramEnvParameter4dARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramEnvParameter4dvARB(int p0, int p1, double[] p2, int p3) {
    record("glProgramEnvParameter4dvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramEnvParameter4dvARB(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glProgramEnvParameter4dvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramEnvParameter4fARB(int p0, int p1, float p2, float p3, float p4, float p5) {
    record("glProgramEnvParameter4fARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramEnvParameter4fvARB(int p0, int p1, float[] p2, int p3) {
    record("glProgramEnvParameter4fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramEnvParameter4fvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glProgramEnvParameter4fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramEnvParameterI4iNV(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glProgramEnvParameterI4iNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramEnvParameterI4ivNV(int p0, int p1, int[] p2, int p3) {
    record("glProgramEnvParameterI4ivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramEnvParameterI4ivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glProgramEnvParameterI4ivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramEnvParameterI4uiNV(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glProgramEnvParameterI4uiNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramEnvParameterI4uivNV(int p0, int p1, int[] p2, int p3) {
    record("glProgramEnvParameterI4uivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramEnvParameterI4uivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glProgramEnvParameterI4uivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramEnvParameters4fvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glProgramEnvParameters4fvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramEnvParameters4fvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glProgramEnvParameters4fvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramEnvParametersI4ivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramEnvParametersI4ivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramEnvParametersI4ivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramEnvParametersI4ivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramEnvParametersI4uivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramEnvParametersI4uivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramEnvParametersI4uivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramEnvParametersI4uivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParameter4dARB(int p0, int p1, double p2, double p3, double p4, double p5) {
    record("glProgramLocalParameter4dARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramLocalParameter4dvARB(int p0, int p1, double[] p2, int p3) {
    record("glProgramLocalParameter4dvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParameter4dvARB(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glProgramLocalParameter4dvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramLocalParameter4fARB(int p0, int p1, float p2, float p3, float p4, float p5) {
    record("glProgramLocalParameter4fARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramLocalParameter4fvARB(int p0, int p1, float[] p2, int p3) {
    record("glProgramLocalParameter4fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParameter4fvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glProgramLocalParameter4fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramLocalParameterI4iNV(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glProgramLocalParameterI4iNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramLocalParameterI4ivNV(int p0, int p1, int[] p2, int p3) {
    record("glProgramLocalParameterI4ivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParameterI4ivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glProgramLocalParameterI4ivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramLocalParameterI4uiNV(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glProgramLocalParameterI4uiNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramLocalParameterI4uivNV(int p0, int p1, int[] p2, int p3) {
    record("glProgramLocalParameterI4uivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParameterI4uivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glProgramLocalParameterI4uivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramLocalParameters4fvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glProgramLocalParameters4fvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramLocalParameters4fvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glProgramLocalParameters4fvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParametersI4ivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramLocalParametersI4ivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramLocalParametersI4ivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramLocalParametersI4ivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramLocalParametersI4uivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramLocalParametersI4uivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramLocalParametersI4uivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramLocalParametersI4uivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramParameteri(int p0, int p1, int p2) {
    record("glProgramParameteri", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramStringARB(int p0, int p1, int p2, java.lang.String p3) {
    record("glProgramStringARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramSubroutineParametersuivNV(int p0, int p1, int[] p2, int p3) {
    record("glProgramSubroutineParametersuivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramSubroutineParametersuivNV(int p0, int p1, java.nio.IntBuffer p2) {
    record("glProgramSubroutineParametersuivNV", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniform1d(int p0, int p1, double p2) {
    record("glProgramUniform1d", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniform1dEXT(int p0, int p1, double p2) {
    record("glProgramUniform1dEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniform1dv(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform1dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform1dv(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform1dv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform1dvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform1dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform1dvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform1dvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform1f(int p0, int p1, float p2) {
    record("glProgramUniform1f", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniform1fv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glProgramUniform1fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform1fv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glProgramUniform1fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform1i(int p0, int p1, int p2) {
    record("glProgramUniform1i", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniform1iv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform1iv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform1iv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform1iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform1ui(int p0, int p1, int p2) {
    record("glProgramUniform1ui", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniform1uiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform1uiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform1uiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform1uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2d(int p0, int p1, double p2, double p3) {
    record("glProgramUniform2d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2dEXT(int p0, int p1, double p2, double p3) {
    record("glProgramUniform2dEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2dv(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform2dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform2dv(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform2dv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2dvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform2dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform2dvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform2dvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2f(int p0, int p1, float p2, float p3) {
    record("glProgramUniform2f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2fv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glProgramUniform2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform2fv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glProgramUniform2fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2i(int p0, int p1, int p2, int p3) {
    record("glProgramUniform2i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2iv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform2iv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform2iv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform2iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2ui(int p0, int p1, int p2, int p3) {
    record("glProgramUniform2ui", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform2uiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform2uiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform2uiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform2uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform3d(int p0, int p1, double p2, double p3, double p4) {
    record("glProgramUniform3d", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3dEXT(int p0, int p1, double p2, double p3, double p4) {
    record("glProgramUniform3dEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3dv(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform3dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3dv(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform3dv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform3dvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform3dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3dvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform3dvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform3f(int p0, int p1, float p2, float p3, float p4) {
    record("glProgramUniform3f", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3fv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glProgramUniform3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3fv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glProgramUniform3fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform3i(int p0, int p1, int p2, int p3, int p4) {
    record("glProgramUniform3i", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3iv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform3iv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3iv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform3iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform3ui(int p0, int p1, int p2, int p3, int p4) {
    record("glProgramUniform3ui", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3uiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform3uiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform3uiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform3uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform4d(int p0, int p1, double p2, double p3, double p4, double p5) {
    record("glProgramUniform4d", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniform4dEXT(int p0, int p1, double p2, double p3, double p4, double p5) {
    record("glProgramUniform4dEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniform4dv(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform4dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform4dv(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform4dv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform4dvEXT(int p0, int p1, int p2, double[] p3, int p4) {
    record("glProgramUniform4dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform4dvEXT(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glProgramUniform4dvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform4f(int p0, int p1, float p2, float p3, float p4, float p5) {
    record("glProgramUniform4f", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniform4fv(int p0, int p1, int p2, float[] p3, int p4) {
    record("glProgramUniform4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform4fv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glProgramUniform4fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform4i(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glProgramUniform4i", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniform4iv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform4iv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform4iv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform4iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniform4ui(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glProgramUniform4ui", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniform4uiv(int p0, int p1, int p2, int[] p3, int p4) {
    record("glProgramUniform4uiv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniform4uiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glProgramUniform4uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniformMatrix2dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix2dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix2dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix2dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix2dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix2fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2x3dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix2x3dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2x3dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix2x3dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2x3dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix2x3dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2x3dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix2x3dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2x3fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix2x3fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2x3fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix2x3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2x4dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix2x4dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2x4dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix2x4dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2x4dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix2x4dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2x4dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix2x4dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix2x4fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix2x4fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix2x4fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix2x4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix3dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix3dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix3dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix3dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix3fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3x2dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix3x2dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3x2dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix3x2dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3x2dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix3x2dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3x2dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix3x2dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3x2fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix3x2fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3x2fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix3x2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3x4dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix3x4dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3x4dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix3x4dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3x4dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix3x4dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3x4dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix3x4dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix3x4fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix3x4fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix3x4fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix3x4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix4dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix4dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix4dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix4dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix4fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4x2dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix4x2dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4x2dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix4x2dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4x2dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix4x2dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4x2dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix4x2dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4x2fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix4x2fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4x2fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix4x2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4x3dv(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix4x3dv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4x3dv(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix4x3dv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4x3dvEXT(int p0, int p1, int p2, boolean p3, double[] p4, int p5) {
    record("glProgramUniformMatrix4x3dvEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4x3dvEXT(int p0, int p1, int p2, boolean p3, java.nio.DoubleBuffer p4) {
    record("glProgramUniformMatrix4x3dvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformMatrix4x3fv(int p0, int p1, int p2, boolean p3, float[] p4, int p5) {
    record("glProgramUniformMatrix4x3fv", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glProgramUniformMatrix4x3fv(int p0, int p1, int p2, boolean p3, java.nio.FloatBuffer p4) {
    record("glProgramUniformMatrix4x3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramUniformui64NV(int p0, int p1, long p2) {
    record("glProgramUniformui64NV", new Object[] {p0, p1, p2});
  }

  @Override public void glProgramUniformui64vNV(int p0, int p1, int p2, java.nio.LongBuffer p3) {
    record("glProgramUniformui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glProgramUniformui64vNV(int p0, int p1, int p2, long[] p3, int p4) {
    record("glProgramUniformui64vNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glProgramVertexLimitNV(int p0, int p1) {
    record("glProgramVertexLimitNV", new Object[] {p0, p1});
  }

  @Override public void glProvokingVertex(int p0) {
    record("glProvokingVertex", new Object[] {p0});
  }

  @Override public void glProvokingVertexEXT(int p0) {
    record("glProvokingVertexEXT", new Object[] {p0});
  }

  @Override public void glPushAttrib(int p0) {
    record("glPushAttrib", new Object[] {p0});
  }

  @Override public void glPushClientAttrib(int p0) {
    record("glPushClientAttrib", new Object[] {p0});
  }

  @Override public void glPushClientAttribDefaultEXT(int p0) {
    record("glPushClientAttribDefaultEXT", new Object[] {p0});
  }

  @Override public void glPushDebugGroup(int p0, int p1, int p2, byte[] p3, int p4) {
    record("glPushDebugGroup", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glPushDebugGroup(int p0, int p1, int p2, java.nio.ByteBuffer p3) {
    record("glPushDebugGroup", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glPushMatrix() {
    record("glPushMatrix", new Object[] {});
  }

  @Override public void glPushName(int p0) {
    record("glPushName", new Object[] {p0});
  }

  @Override public void glQueryCounter(int p0, int p1) {
    record("glQueryCounter", new Object[] {p0, p1});
  }

  @Override public int glQueryMatrixxOES(int[] p0, int p1, int[] p2, int p3) {
    record("glQueryMatrixxOES", new Object[] {p0, p1, p2, p3});
    return 0;
  }

  @Override public int glQueryMatrixxOES(java.nio.IntBuffer p0, java.nio.IntBuffer p1) {
    record("glQueryMatrixxOES", new Object[] {p0, p1});
    return 0;
  }

  @Override public void glQueryObjectParameteruiAMD(int p0, int p1, int p2, int p3) {
    record("glQueryObjectParameteruiAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public int glQueryResourceNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glQueryResourceNV", new Object[] {p0, p1, p2, p3, p4});
    return 0;
  }

  @Override public int glQueryResourceNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glQueryResourceNV", new Object[] {p0, p1, p2, p3});
    return 0;
  }

  @Override public void glQueryResourceTagNV(int p0, byte[] p1, int p2) {
    record("glQueryResourceTagNV", new Object[] {p0, p1, p2});
  }

  @Override public void glQueryResourceTagNV(int p0, java.nio.ByteBuffer p1) {
    record("glQueryResourceTagNV", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2d(double p0, double p1) {
    record("glRasterPos2d", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2dv(double[] p0, int p1) {
    record("glRasterPos2dv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2dv(java.nio.DoubleBuffer p0) {
    record("glRasterPos2dv", new Object[] {p0});
  }

  @Override public void glRasterPos2f(float p0, float p1) {
    record("glRasterPos2f", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2fv(float[] p0, int p1) {
    record("glRasterPos2fv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2fv(java.nio.FloatBuffer p0) {
    record("glRasterPos2fv", new Object[] {p0});
  }

  @Override public void glRasterPos2i(int p0, int p1) {
    record("glRasterPos2i", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2iv(int[] p0, int p1) {
    record("glRasterPos2iv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2iv(java.nio.IntBuffer p0) {
    record("glRasterPos2iv", new Object[] {p0});
  }

  @Override public void glRasterPos2s(short p0, short p1) {
    record("glRasterPos2s", new Object[] {p0, p1});
  }

  @Override public void glRasterPos2sv(java.nio.ShortBuffer p0) {
    record("glRasterPos2sv", new Object[] {p0});
  }

  @Override public void glRasterPos2sv(short[] p0, int p1) {
    record("glRasterPos2sv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos3d(double p0, double p1, double p2) {
    record("glRasterPos3d", new Object[] {p0, p1, p2});
  }

  @Override public void glRasterPos3dv(double[] p0, int p1) {
    record("glRasterPos3dv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos3dv(java.nio.DoubleBuffer p0) {
    record("glRasterPos3dv", new Object[] {p0});
  }

  @Override public void glRasterPos3f(float p0, float p1, float p2) {
    record("glRasterPos3f", new Object[] {p0, p1, p2});
  }

  @Override public void glRasterPos3fv(float[] p0, int p1) {
    record("glRasterPos3fv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos3fv(java.nio.FloatBuffer p0) {
    record("glRasterPos3fv", new Object[] {p0});
  }

  @Override public void glRasterPos3i(int p0, int p1, int p2) {
    record("glRasterPos3i", new Object[] {p0, p1, p2});
  }

  @Override public void glRasterPos3iv(int[] p0, int p1) {
    record("glRasterPos3iv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos3iv(java.nio.IntBuffer p0) {
    record("glRasterPos3iv", new Object[] {p0});
  }

  @Override public void glRasterPos3s(short p0, short p1, short p2) {
    record("glRasterPos3s", new Object[] {p0, p1, p2});
  }

  @Override public void glRasterPos3sv(java.nio.ShortBuffer p0) {
    record("glRasterPos3sv", new Object[] {p0});
  }

  @Override public void glRasterPos3sv(short[] p0, int p1) {
    record("glRasterPos3sv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos4d(double p0, double p1, double p2, double p3) {
    record("glRasterPos4d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRasterPos4dv(double[] p0, int p1) {
    record("glRasterPos4dv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos4dv(java.nio.DoubleBuffer p0) {
    record("glRasterPos4dv", new Object[] {p0});
  }

  @Override public void glRasterPos4f(float p0, float p1, float p2, float p3) {
    record("glRasterPos4f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRasterPos4fv(float[] p0, int p1) {
    record("glRasterPos4fv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos4fv(java.nio.FloatBuffer p0) {
    record("glRasterPos4fv", new Object[] {p0});
  }

  @Override public void glRasterPos4i(int p0, int p1, int p2, int p3) {
    record("glRasterPos4i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRasterPos4iv(int[] p0, int p1) {
    record("glRasterPos4iv", new Object[] {p0, p1});
  }

  @Override public void glRasterPos4iv(java.nio.IntBuffer p0) {
    record("glRasterPos4iv", new Object[] {p0});
  }

  @Override public void glRasterPos4s(short p0, short p1, short p2, short p3) {
    record("glRasterPos4s", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRasterPos4sv(java.nio.ShortBuffer p0) {
    record("glRasterPos4sv", new Object[] {p0});
  }

  @Override public void glRasterPos4sv(short[] p0, int p1) {
    record("glRasterPos4sv", new Object[] {p0, p1});
  }

  @Override public void glRasterSamplesEXT(int p0, boolean p1) {
    record("glRasterSamplesEXT", new Object[] {p0, p1});
  }

  @Override public void glReadBuffer(int p0) {
    record("glReadBuffer", new Object[] {p0});
  }

  @Override public void glReadPixels(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.Buffer p6) {
    record("glReadPixels", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glReadPixels(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glReadPixels", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glReadnPixels", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glRectd(double p0, double p1, double p2, double p3) {
    record("glRectd", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectdv(double[] p0, int p1, double[] p2, int p3) {
    record("glRectdv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectdv(java.nio.DoubleBuffer p0, java.nio.DoubleBuffer p1) {
    record("glRectdv", new Object[] {p0, p1});
  }

  @Override public void glRectf(float p0, float p1, float p2, float p3) {
    record("glRectf", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectfv(float[] p0, int p1, float[] p2, int p3) {
    record("glRectfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectfv(java.nio.FloatBuffer p0, java.nio.FloatBuffer p1) {
    record("glRectfv", new Object[] {p0, p1});
  }

  @Override public void glRecti(int p0, int p1, int p2, int p3) {
    record("glRecti", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectiv(int[] p0, int p1, int[] p2, int p3) {
    record("glRectiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectiv(java.nio.IntBuffer p0, java.nio.IntBuffer p1) {
    record("glRectiv", new Object[] {p0, p1});
  }

  @Override public void glRects(short p0, short p1, short p2, short p3) {
    record("glRects", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRectsv(java.nio.ShortBuffer p0, java.nio.ShortBuffer p1) {
    record("glRectsv", new Object[] {p0, p1});
  }

  @Override public void glRectsv(short[] p0, int p1, short[] p2, int p3) {
    record("glRectsv", new Object[] {p0, p1, p2, p3});
  }

  @Override public boolean glReleaseKeyedMutexWin32EXT(int p0, long p1) {
    record("glReleaseKeyedMutexWin32EXT", new Object[] {p0, p1});
    return false;
  }

  @Override public void glReleaseShaderCompiler() {
    record("glReleaseShaderCompiler", new Object[] {});
  }

  @Override public void glRenderGpuMaskNV(int p0) {
    record("glRenderGpuMaskNV", new Object[] {p0});
  }

  @Override public int glRenderMode(int p0) {
    record("glRenderMode", new Object[] {p0});
    return 0;
  }

  @Override public void glRenderbufferStorage(int p0, int p1, int p2, int p3) {
    record("glRenderbufferStorage", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRenderbufferStorageMultisample(int p0, int p1, int p2, int p3, int p4) {
    record("glRenderbufferStorageMultisample", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glRenderbufferStorageMultisampleAdvancedAMD(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glRenderbufferStorageMultisampleAdvancedAMD", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glRenderbufferStorageMultisampleCoverageNV(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glRenderbufferStorageMultisampleCoverageNV", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glResetHistogram(int p0) {
    record("glResetHistogram", new Object[] {p0});
  }

  @Override public void glResetMinmax(int p0) {
    record("glResetMinmax", new Object[] {p0});
  }

  @Override public void glResumeTransformFeedback() {
    record("glResumeTransformFeedback", new Object[] {});
  }

  @Override public void glResumeTransformFeedbackNV() {
    record("glResumeTransformFeedbackNV", new Object[] {});
  }

  @Override public void glRotated(double p0, double p1, double p2, double p3) {
    record("glRotated", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glRotatef(float p0, float p1, float p2, float p3) {
    record("glRotatef", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSampleCoverage(float p0, boolean p1) {
    record("glSampleCoverage", new Object[] {p0, p1});
  }

  @Override public void glSampleMaskIndexedNV(int p0, int p1) {
    record("glSampleMaskIndexedNV", new Object[] {p0, p1});
  }

  @Override public void glSampleMaski(int p0, int p1) {
    record("glSampleMaski", new Object[] {p0, p1});
  }

  @Override public void glSamplerParameterIiv(int p0, int p1, int[] p2, int p3) {
    record("glSamplerParameterIiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSamplerParameterIiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glSamplerParameterIiv", new Object[] {p0, p1, p2});
  }

  @Override public void glSamplerParameterIuiv(int p0, int p1, int[] p2, int p3) {
    record("glSamplerParameterIuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSamplerParameterIuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glSamplerParameterIuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glScaled(double p0, double p1, double p2) {
    record("glScaled", new Object[] {p0, p1, p2});
  }

  @Override public void glScalef(float p0, float p1, float p2) {
    record("glScalef", new Object[] {p0, p1, p2});
  }

  @Override public void glScissor(int p0, int p1, int p2, int p3) {
    record("glScissor", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSecondaryColor3b(byte p0, byte p1, byte p2) {
    record("glSecondaryColor3b", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3bv(byte[] p0, int p1) {
    record("glSecondaryColor3bv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3bv(java.nio.ByteBuffer p0) {
    record("glSecondaryColor3bv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3d(double p0, double p1, double p2) {
    record("glSecondaryColor3d", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3dv(double[] p0, int p1) {
    record("glSecondaryColor3dv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3dv(java.nio.DoubleBuffer p0) {
    record("glSecondaryColor3dv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3f(float p0, float p1, float p2) {
    record("glSecondaryColor3f", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3fv(float[] p0, int p1) {
    record("glSecondaryColor3fv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3fv(java.nio.FloatBuffer p0) {
    record("glSecondaryColor3fv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3h(short p0, short p1, short p2) {
    record("glSecondaryColor3h", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3hv(java.nio.ShortBuffer p0) {
    record("glSecondaryColor3hv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3hv(short[] p0, int p1) {
    record("glSecondaryColor3hv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3i(int p0, int p1, int p2) {
    record("glSecondaryColor3i", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3iv(int[] p0, int p1) {
    record("glSecondaryColor3iv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3iv(java.nio.IntBuffer p0) {
    record("glSecondaryColor3iv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3s(short p0, short p1, short p2) {
    record("glSecondaryColor3s", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3sv(java.nio.ShortBuffer p0) {
    record("glSecondaryColor3sv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3sv(short[] p0, int p1) {
    record("glSecondaryColor3sv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3ub(byte p0, byte p1, byte p2) {
    record("glSecondaryColor3ub", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3ubv(byte[] p0, int p1) {
    record("glSecondaryColor3ubv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3ubv(java.nio.ByteBuffer p0) {
    record("glSecondaryColor3ubv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3ui(int p0, int p1, int p2) {
    record("glSecondaryColor3ui", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3uiv(int[] p0, int p1) {
    record("glSecondaryColor3uiv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColor3uiv(java.nio.IntBuffer p0) {
    record("glSecondaryColor3uiv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3us(short p0, short p1, short p2) {
    record("glSecondaryColor3us", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColor3usv(java.nio.ShortBuffer p0) {
    record("glSecondaryColor3usv", new Object[] {p0});
  }

  @Override public void glSecondaryColor3usv(short[] p0, int p1) {
    record("glSecondaryColor3usv", new Object[] {p0, p1});
  }

  @Override public void glSecondaryColorFormatNV(int p0, int p1, int p2) {
    record("glSecondaryColorFormatNV", new Object[] {p0, p1, p2});
  }

  @Override public void glSecondaryColorPointer(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glSecondaryColorPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSecondaryColorPointer(int p0, int p1, int p2, long p3) {
    record("glSecondaryColorPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSelectBuffer(int p0, java.nio.IntBuffer p1) {
    record("glSelectBuffer", new Object[] {p0, p1});
  }

  @Override public void glSemaphoreParameterui64vEXT(int p0, int p1, java.nio.LongBuffer p2) {
    record("glSemaphoreParameterui64vEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glSemaphoreParameterui64vEXT(int p0, int p1, long[] p2, int p3) {
    record("glSemaphoreParameterui64vEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSeparableFilter2D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.Buffer p6, java.nio.Buffer p7) {
    record("glSeparableFilter2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glSeparableFilter2D(int p0, int p1, int p2, int p3, int p4, int p5, long p6, long p7) {
    record("glSeparableFilter2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glSetInvariantEXT(int p0, int p1, java.nio.Buffer p2) {
    record("glSetInvariantEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glSetLocalConstantEXT(int p0, int p1, java.nio.Buffer p2) {
    record("glSetLocalConstantEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glSetMultisamplefvAMD(int p0, int p1, float[] p2, int p3) {
    record("glSetMultisamplefvAMD", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSetMultisamplefvAMD(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glSetMultisamplefvAMD", new Object[] {p0, p1, p2});
  }

  @Override public void glShadeModel(int p0) {
    record("glShadeModel", new Object[] {p0});
  }

  @Override public void glShaderBinary(int p0, int[] p1, int p2, int p3, java.nio.Buffer p4, int p5) {
    record("glShaderBinary", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glShaderBinary(int p0, java.nio.IntBuffer p1, int p2, java.nio.Buffer p3, int p4) {
    record("glShaderBinary", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glShaderOp1EXT(int p0, int p1, int p2) {
    record("glShaderOp1EXT", new Object[] {p0, p1, p2});
  }

  @Override public void glShaderOp2EXT(int p0, int p1, int p2, int p3) {
    record("glShaderOp2EXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glShaderOp3EXT(int p0, int p1, int p2, int p3, int p4) {
    record("glShaderOp3EXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glShaderSource(int p0, int p1, java.lang.String[] p2, int[] p3, int p4) {
    record("glShaderSource", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glShaderSource(int p0, int p1, java.lang.String[] p2, java.nio.IntBuffer p3) {
    record("glShaderSource", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glShaderSourceARB(long p0, int p1, java.lang.String[] p2, int[] p3, int p4) {
    record("glShaderSourceARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glShaderSourceARB(long p0, int p1, java.lang.String[] p2, java.nio.IntBuffer p3) {
    record("glShaderSourceARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glSignalSemaphoreEXT(int p0, int p1, int[] p2, int p3, int p4, int[] p5, int p6, int[] p7, int p8) {
    record("glSignalSemaphoreEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glSignalSemaphoreEXT(int p0, int p1, java.nio.IntBuffer p2, int p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5) {
    record("glSignalSemaphoreEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glSignalSemaphoreui64NVX(int p0, int p1, int[] p2, int p3, long[] p4, int p5) {
    record("glSignalSemaphoreui64NVX", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glSignalSemaphoreui64NVX(int p0, int p1, java.nio.IntBuffer p2, java.nio.LongBuffer p3) {
    record("glSignalSemaphoreui64NVX", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glStateCaptureNV(int p0, int p1) {
    record("glStateCaptureNV", new Object[] {p0, p1});
  }

  @Override public void glStencilClearTagEXT(int p0, int p1) {
    record("glStencilClearTagEXT", new Object[] {p0, p1});
  }

  @Override public void glStencilFunc(int p0, int p1, int p2) {
    record("glStencilFunc", new Object[] {p0, p1, p2});
  }

  @Override public void glStencilFuncSeparate(int p0, int p1, int p2, int p3) {
    record("glStencilFuncSeparate", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glStencilMask(int p0) {
    record("glStencilMask", new Object[] {p0});
  }

  @Override public void glStencilMaskSeparate(int p0, int p1) {
    record("glStencilMaskSeparate", new Object[] {p0, p1});
  }

  @Override public void glStencilOp(int p0, int p1, int p2) {
    record("glStencilOp", new Object[] {p0, p1, p2});
  }

  @Override public void glStencilOpSeparate(int p0, int p1, int p2, int p3) {
    record("glStencilOpSeparate", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glStencilOpValueAMD(int p0, int p1) {
    record("glStencilOpValueAMD", new Object[] {p0, p1});
  }

  @Override public void glStringMarkerGREMEDY(int p0, java.nio.Buffer p1) {
    record("glStringMarkerGREMEDY", new Object[] {p0, p1});
  }

  @Override public void glSwizzleEXT(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glSwizzleEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glSyncTextureINTEL(int p0) {
    record("glSyncTextureINTEL", new Object[] {p0});
  }

  @Override public void glTessellationFactorAMD(float p0) {
    record("glTessellationFactorAMD", new Object[] {p0});
  }

  @Override public void glTessellationModeAMD(int p0) {
    record("glTessellationModeAMD", new Object[] {p0});
  }

  @Override public void glTexBuffer(int p0, int p1, int p2) {
    record("glTexBuffer", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord1bOES(byte p0) {
    record("glTexCoord1bOES", new Object[] {p0});
  }

  @Override public void glTexCoord1bvOES(byte[] p0, int p1) {
    record("glTexCoord1bvOES", new Object[] {p0, p1});
  }

  @Override public void glTexCoord1bvOES(java.nio.ByteBuffer p0) {
    record("glTexCoord1bvOES", new Object[] {p0});
  }

  @Override public void glTexCoord1d(double p0) {
    record("glTexCoord1d", new Object[] {p0});
  }

  @Override public void glTexCoord1dv(double[] p0, int p1) {
    record("glTexCoord1dv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord1dv(java.nio.DoubleBuffer p0) {
    record("glTexCoord1dv", new Object[] {p0});
  }

  @Override public void glTexCoord1f(float p0) {
    record("glTexCoord1f", new Object[] {p0});
  }

  @Override public void glTexCoord1fv(float[] p0, int p1) {
    record("glTexCoord1fv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord1fv(java.nio.FloatBuffer p0) {
    record("glTexCoord1fv", new Object[] {p0});
  }

  @Override public void glTexCoord1h(short p0) {
    record("glTexCoord1h", new Object[] {p0});
  }

  @Override public void glTexCoord1hv(java.nio.ShortBuffer p0) {
    record("glTexCoord1hv", new Object[] {p0});
  }

  @Override public void glTexCoord1hv(short[] p0, int p1) {
    record("glTexCoord1hv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord1i(int p0) {
    record("glTexCoord1i", new Object[] {p0});
  }

  @Override public void glTexCoord1iv(int[] p0, int p1) {
    record("glTexCoord1iv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord1iv(java.nio.IntBuffer p0) {
    record("glTexCoord1iv", new Object[] {p0});
  }

  @Override public void glTexCoord1s(short p0) {
    record("glTexCoord1s", new Object[] {p0});
  }

  @Override public void glTexCoord1sv(java.nio.ShortBuffer p0) {
    record("glTexCoord1sv", new Object[] {p0});
  }

  @Override public void glTexCoord1sv(short[] p0, int p1) {
    record("glTexCoord1sv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2bOES(byte p0, byte p1) {
    record("glTexCoord2bOES", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2bvOES(byte[] p0, int p1) {
    record("glTexCoord2bvOES", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2bvOES(java.nio.ByteBuffer p0) {
    record("glTexCoord2bvOES", new Object[] {p0});
  }

  @Override public void glTexCoord2d(double p0, double p1) {
    record("glTexCoord2d", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2dv(double[] p0, int p1) {
    record("glTexCoord2dv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2dv(java.nio.DoubleBuffer p0) {
    record("glTexCoord2dv", new Object[] {p0});
  }

  @Override public void glTexCoord2f(float p0, float p1) {
    record("glTexCoord2f", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2fv(float[] p0, int p1) {
    record("glTexCoord2fv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2fv(java.nio.FloatBuffer p0) {
    record("glTexCoord2fv", new Object[] {p0});
  }

  @Override public void glTexCoord2h(short p0, short p1) {
    record("glTexCoord2h", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2hv(java.nio.ShortBuffer p0) {
    record("glTexCoord2hv", new Object[] {p0});
  }

  @Override public void glTexCoord2hv(short[] p0, int p1) {
    record("glTexCoord2hv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2i(int p0, int p1) {
    record("glTexCoord2i", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2iv(int[] p0, int p1) {
    record("glTexCoord2iv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2iv(java.nio.IntBuffer p0) {
    record("glTexCoord2iv", new Object[] {p0});
  }

  @Override public void glTexCoord2s(short p0, short p1) {
    record("glTexCoord2s", new Object[] {p0, p1});
  }

  @Override public void glTexCoord2sv(java.nio.ShortBuffer p0) {
    record("glTexCoord2sv", new Object[] {p0});
  }

  @Override public void glTexCoord2sv(short[] p0, int p1) {
    record("glTexCoord2sv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord3bOES(byte p0, byte p1, byte p2) {
    record("glTexCoord3bOES", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord3bvOES(byte[] p0, int p1) {
    record("glTexCoord3bvOES", new Object[] {p0, p1});
  }

  @Override public void glTexCoord3bvOES(java.nio.ByteBuffer p0) {
    record("glTexCoord3bvOES", new Object[] {p0});
  }

  @Override public void glTexCoord3d(double p0, double p1, double p2) {
    record("glTexCoord3d", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord3dv(double[] p0, int p1) {
    record("glTexCoord3dv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord3dv(java.nio.DoubleBuffer p0) {
    record("glTexCoord3dv", new Object[] {p0});
  }

  @Override public void glTexCoord3f(float p0, float p1, float p2) {
    record("glTexCoord3f", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord3fv(float[] p0, int p1) {
    record("glTexCoord3fv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord3fv(java.nio.FloatBuffer p0) {
    record("glTexCoord3fv", new Object[] {p0});
  }

  @Override public void glTexCoord3h(short p0, short p1, short p2) {
    record("glTexCoord3h", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord3hv(java.nio.ShortBuffer p0) {
    record("glTexCoord3hv", new Object[] {p0});
  }

  @Override public void glTexCoord3hv(short[] p0, int p1) {
    record("glTexCoord3hv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord3i(int p0, int p1, int p2) {
    record("glTexCoord3i", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord3iv(int[] p0, int p1) {
    record("glTexCoord3iv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord3iv(java.nio.IntBuffer p0) {
    record("glTexCoord3iv", new Object[] {p0});
  }

  @Override public void glTexCoord3s(short p0, short p1, short p2) {
    record("glTexCoord3s", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoord3sv(java.nio.ShortBuffer p0) {
    record("glTexCoord3sv", new Object[] {p0});
  }

  @Override public void glTexCoord3sv(short[] p0, int p1) {
    record("glTexCoord3sv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord4bOES(byte p0, byte p1, byte p2, byte p3) {
    record("glTexCoord4bOES", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoord4bvOES(byte[] p0, int p1) {
    record("glTexCoord4bvOES", new Object[] {p0, p1});
  }

  @Override public void glTexCoord4bvOES(java.nio.ByteBuffer p0) {
    record("glTexCoord4bvOES", new Object[] {p0});
  }

  @Override public void glTexCoord4d(double p0, double p1, double p2, double p3) {
    record("glTexCoord4d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoord4dv(double[] p0, int p1) {
    record("glTexCoord4dv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord4dv(java.nio.DoubleBuffer p0) {
    record("glTexCoord4dv", new Object[] {p0});
  }

  @Override public void glTexCoord4f(float p0, float p1, float p2, float p3) {
    record("glTexCoord4f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoord4fv(float[] p0, int p1) {
    record("glTexCoord4fv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord4fv(java.nio.FloatBuffer p0) {
    record("glTexCoord4fv", new Object[] {p0});
  }

  @Override public void glTexCoord4h(short p0, short p1, short p2, short p3) {
    record("glTexCoord4h", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoord4hv(java.nio.ShortBuffer p0) {
    record("glTexCoord4hv", new Object[] {p0});
  }

  @Override public void glTexCoord4hv(short[] p0, int p1) {
    record("glTexCoord4hv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord4i(int p0, int p1, int p2, int p3) {
    record("glTexCoord4i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoord4iv(int[] p0, int p1) {
    record("glTexCoord4iv", new Object[] {p0, p1});
  }

  @Override public void glTexCoord4iv(java.nio.IntBuffer p0) {
    record("glTexCoord4iv", new Object[] {p0});
  }

  @Override public void glTexCoord4s(short p0, short p1, short p2, short p3) {
    record("glTexCoord4s", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoord4sv(java.nio.ShortBuffer p0) {
    record("glTexCoord4sv", new Object[] {p0});
  }

  @Override public void glTexCoord4sv(short[] p0, int p1) {
    record("glTexCoord4sv", new Object[] {p0, p1});
  }

  @Override public void glTexCoordFormatNV(int p0, int p1, int p2) {
    record("glTexCoordFormatNV", new Object[] {p0, p1, p2});
  }

  @Override public void glTexCoordPointer(com.jogamp.opengl.GLArrayData p0) {
    record("glTexCoordPointer", new Object[] {p0});
  }

  @Override public void glTexCoordPointer(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glTexCoordPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexCoordPointer(int p0, int p1, int p2, long p3) {
    record("glTexCoordPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexEnvf(int p0, int p1, float p2) {
    record("glTexEnvf", new Object[] {p0, p1, p2});
  }

  @Override public void glTexEnvfv(int p0, int p1, float[] p2, int p3) {
    record("glTexEnvfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexEnvfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glTexEnvfv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexEnvi(int p0, int p1, int p2) {
    record("glTexEnvi", new Object[] {p0, p1, p2});
  }

  @Override public void glTexEnviv(int p0, int p1, int[] p2, int p3) {
    record("glTexEnviv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexEnviv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glTexEnviv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexGend(int p0, int p1, double p2) {
    record("glTexGend", new Object[] {p0, p1, p2});
  }

  @Override public void glTexGendv(int p0, int p1, double[] p2, int p3) {
    record("glTexGendv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexGendv(int p0, int p1, java.nio.DoubleBuffer p2) {
    record("glTexGendv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexGenf(int p0, int p1, float p2) {
    record("glTexGenf", new Object[] {p0, p1, p2});
  }

  @Override public void glTexGenfv(int p0, int p1, float[] p2, int p3) {
    record("glTexGenfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexGenfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glTexGenfv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexGeni(int p0, int p1, int p2) {
    record("glTexGeni", new Object[] {p0, p1, p2});
  }

  @Override public void glTexGeniv(int p0, int p1, int[] p2, int p3) {
    record("glTexGeniv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexGeniv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glTexGeniv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glTexImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTexImage1D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {
    record("glTexImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glTexImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTexImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {
    record("glTexImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTexImage2DMultisample(int p0, int p1, int p2, int p3, int p4, boolean p5) {
    record("glTexImage2DMultisample", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glTexImage2DMultisampleCoverageNV(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
    record("glTexImage2DMultisampleCoverageNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glTexImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glTexImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9) {
    record("glTexImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glTexImage3DMultisample(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
    record("glTexImage3DMultisample", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexImage3DMultisampleCoverageNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, boolean p7) {
    record("glTexImage3DMultisampleCoverageNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTexPageCommitmentARB(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, boolean p8) {
    record("glTexPageCommitmentARB", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTexParameterIiv(int p0, int p1, int[] p2, int p3) {
    record("glTexParameterIiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexParameterIiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glTexParameterIiv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexParameterIuiv(int p0, int p1, int[] p2, int p3) {
    record("glTexParameterIuiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexParameterIuiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glTexParameterIuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexParameterf(int p0, int p1, float p2) {
    record("glTexParameterf", new Object[] {p0, p1, p2});
  }

  @Override public void glTexParameterfv(int p0, int p1, float[] p2, int p3) {
    record("glTexParameterfv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glTexParameterfv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexParameteri(int p0, int p1, int p2) {
    record("glTexParameteri", new Object[] {p0, p1, p2});
  }

  @Override public void glTexParameteriv(int p0, int p1, int[] p2, int p3) {
    record("glTexParameteriv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glTexParameteriv", new Object[] {p0, p1, p2});
  }

  @Override public void glTexRenderbufferNV(int p0, int p1) {
    record("glTexRenderbufferNV", new Object[] {p0, p1});
  }

  @Override public void glTexStorage1D(int p0, int p1, int p2, int p3) {
    record("glTexStorage1D", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTexStorage2D(int p0, int p1, int p2, int p3, int p4) {
    record("glTexStorage2D", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glTexStorage2DMultisample(int p0, int p1, int p2, int p3, int p4, boolean p5) {
    record("glTexStorage2DMultisample", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glTexStorage3D(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glTexStorage3D", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glTexStorage3DMultisample(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
    record("glTexStorage3DMultisample", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexStorageMem2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glTexStorageMem2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexStorageMem2DMultisampleEXT(int p0, int p1, int p2, int p3, int p4, boolean p5, int p6, long p7) {
    record("glTexStorageMem2DMultisampleEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTexStorageMem3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {
    record("glTexStorageMem3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTexStorageMem3DMultisampleEXT(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6, int p7, long p8) {
    record("glTexStorageMem3DMultisampleEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTexStorageSparseAMD(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glTexStorageSparseAMD", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.Buffer p6) {
    record("glTexSubImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glTexSubImage1D", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glTexSubImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTexSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {
    record("glTexSubImage2D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.Buffer p10) {
    record("glTexSubImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glTexSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {
    record("glTexSubImage3D", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glTextureBufferEXT(int p0, int p1, int p2, int p3) {
    record("glTextureBufferEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureBufferRangeEXT(int p0, int p1, int p2, int p3, long p4, long p5) {
    record("glTextureBufferRangeEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glTextureImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.Buffer p8) {
    record("glTextureImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTextureImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {
    record("glTextureImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTextureImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glTextureImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glTextureImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9) {
    record("glTextureImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glTextureImage2DMultisampleCoverageNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, boolean p7) {
    record("glTextureImage2DMultisampleCoverageNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureImage2DMultisampleNV(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
    record("glTextureImage2DMultisampleNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTextureImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.Buffer p10) {
    record("glTextureImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glTextureImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {
    record("glTextureImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10});
  }

  @Override public void glTextureImage3DMultisampleCoverageNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, boolean p8) {
    record("glTextureImage3DMultisampleCoverageNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTextureImage3DMultisampleNV(int p0, int p1, int p2, int p3, int p4, int p5, int p6, boolean p7) {
    record("glTextureImage3DMultisampleNV", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureLightEXT(int p0) {
    record("glTextureLightEXT", new Object[] {p0});
  }

  @Override public void glTextureMaterialEXT(int p0, int p1) {
    record("glTextureMaterialEXT", new Object[] {p0, p1});
  }

  @Override public void glTextureNormalEXT(int p0) {
    record("glTextureNormalEXT", new Object[] {p0});
  }

  @Override public void glTexturePageCommitmentEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, boolean p8) {
    record("glTexturePageCommitmentEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTextureParameterIivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glTextureParameterIivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glTextureParameterIivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glTextureParameterIivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureParameterIuivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glTextureParameterIuivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glTextureParameterIuivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glTextureParameterIuivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureParameterfEXT(int p0, int p1, int p2, float p3) {
    record("glTextureParameterfEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureParameterfvEXT(int p0, int p1, int p2, float[] p3, int p4) {
    record("glTextureParameterfvEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glTextureParameterfvEXT(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glTextureParameterfvEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureParameteriEXT(int p0, int p1, int p2, int p3) {
    record("glTextureParameteriEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureParameterivEXT(int p0, int p1, int p2, int[] p3, int p4) {
    record("glTextureParameterivEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glTextureParameterivEXT(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glTextureParameterivEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTextureRangeAPPLE(int p0, int p1, java.nio.Buffer p2) {
    record("glTextureRangeAPPLE", new Object[] {p0, p1, p2});
  }

  @Override public void glTextureRenderbufferEXT(int p0, int p1, int p2) {
    record("glTextureRenderbufferEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glTextureStorage1DEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glTextureStorage1DEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glTextureStorage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glTextureStorage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glTextureStorage2DMultisampleEXT(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
    record("glTextureStorage2DMultisampleEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTextureStorage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6) {
    record("glTextureStorage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTextureStorage3DMultisampleEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, boolean p7) {
    record("glTextureStorage3DMultisampleEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureStorageMem2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glTextureStorageMem2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glTextureStorageMem2DMultisampleEXT(int p0, int p1, int p2, int p3, int p4, boolean p5, int p6, long p7) {
    record("glTextureStorageMem2DMultisampleEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureStorageMem3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {
    record("glTextureStorageMem3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureStorageMem3DMultisampleEXT(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6, int p7, long p8) {
    record("glTextureStorageMem3DMultisampleEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glTextureStorageSparseAMD(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    record("glTextureStorageSparseAMD", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.Buffer p7) {
    record("glTextureSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureSubImage1DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {
    record("glTextureSubImage1DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glTextureSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.Buffer p9) {
    record("glTextureSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glTextureSubImage2DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9) {
    record("glTextureSubImage2DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override public void glTextureSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, java.nio.Buffer p11) {
    record("glTextureSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glTextureSubImage3DEXT(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, long p11) {
    record("glTextureSubImage3DEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11});
  }

  @Override public void glTransformFeedbackVaryings(int p0, int p1, java.lang.String[] p2, int p3) {
    record("glTransformFeedbackVaryings", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glTranslated(double p0, double p1, double p2) {
    record("glTranslated", new Object[] {p0, p1, p2});
  }

  @Override public void glTranslatef(float p0, float p1, float p2) {
    record("glTranslatef", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform(com.jogamp.opengl.GLUniformData p0) {
    record("glUniform", new Object[] {p0});
  }

  @Override public void glUniform1f(int p0, float p1) {
    record("glUniform1f", new Object[] {p0, p1});
  }

  @Override public void glUniform1fARB(int p0, float p1) {
    record("glUniform1fARB", new Object[] {p0, p1});
  }

  @Override public void glUniform1fv(int p0, int p1, float[] p2, int p3) {
    record("glUniform1fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform1fv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform1fv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform1fvARB(int p0, int p1, float[] p2, int p3) {
    record("glUniform1fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform1fvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform1fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform1i(int p0, int p1) {
    record("glUniform1i", new Object[] {p0, p1});
  }

  @Override public void glUniform1iARB(int p0, int p1) {
    record("glUniform1iARB", new Object[] {p0, p1});
  }

  @Override public void glUniform1iv(int p0, int p1, int[] p2, int p3) {
    record("glUniform1iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform1iv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform1iv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform1ivARB(int p0, int p1, int[] p2, int p3) {
    record("glUniform1ivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform1ivARB(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform1ivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform1ui(int p0, int p1) {
    record("glUniform1ui", new Object[] {p0, p1});
  }

  @Override public void glUniform1uiv(int p0, int p1, int[] p2, int p3) {
    record("glUniform1uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform1uiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform1uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2f(int p0, float p1, float p2) {
    record("glUniform2f", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2fARB(int p0, float p1, float p2) {
    record("glUniform2fARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2fv(int p0, int p1, float[] p2, int p3) {
    record("glUniform2fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform2fv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform2fv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2fvARB(int p0, int p1, float[] p2, int p3) {
    record("glUniform2fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform2fvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform2fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2i(int p0, int p1, int p2) {
    record("glUniform2i", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2iARB(int p0, int p1, int p2) {
    record("glUniform2iARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2iv(int p0, int p1, int[] p2, int p3) {
    record("glUniform2iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform2iv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform2iv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2ivARB(int p0, int p1, int[] p2, int p3) {
    record("glUniform2ivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform2ivARB(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform2ivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2ui(int p0, int p1, int p2) {
    record("glUniform2ui", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform2uiv(int p0, int p1, int[] p2, int p3) {
    record("glUniform2uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform2uiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform2uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform3f(int p0, float p1, float p2, float p3) {
    record("glUniform3f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3fARB(int p0, float p1, float p2, float p3) {
    record("glUniform3fARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3fv(int p0, int p1, float[] p2, int p3) {
    record("glUniform3fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3fv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform3fv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform3fvARB(int p0, int p1, float[] p2, int p3) {
    record("glUniform3fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3fvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform3fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform3i(int p0, int p1, int p2, int p3) {
    record("glUniform3i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3iARB(int p0, int p1, int p2, int p3) {
    record("glUniform3iARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3iv(int p0, int p1, int[] p2, int p3) {
    record("glUniform3iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3iv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform3iv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform3ivARB(int p0, int p1, int[] p2, int p3) {
    record("glUniform3ivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3ivARB(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform3ivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform3ui(int p0, int p1, int p2, int p3) {
    record("glUniform3ui", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3uiv(int p0, int p1, int[] p2, int p3) {
    record("glUniform3uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform3uiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform3uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform4f(int p0, float p1, float p2, float p3, float p4) {
    record("glUniform4f", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniform4fARB(int p0, float p1, float p2, float p3, float p4) {
    record("glUniform4fARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniform4fv(int p0, int p1, float[] p2, int p3) {
    record("glUniform4fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform4fv(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform4fv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform4fvARB(int p0, int p1, float[] p2, int p3) {
    record("glUniform4fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform4fvARB(int p0, int p1, java.nio.FloatBuffer p2) {
    record("glUniform4fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform4i(int p0, int p1, int p2, int p3, int p4) {
    record("glUniform4i", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniform4iARB(int p0, int p1, int p2, int p3, int p4) {
    record("glUniform4iARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniform4iv(int p0, int p1, int[] p2, int p3) {
    record("glUniform4iv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform4iv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform4iv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform4ivARB(int p0, int p1, int[] p2, int p3) {
    record("glUniform4ivARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform4ivARB(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform4ivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glUniform4ui(int p0, int p1, int p2, int p3, int p4) {
    record("glUniform4ui", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniform4uiv(int p0, int p1, int[] p2, int p3) {
    record("glUniform4uiv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniform4uiv(int p0, int p1, java.nio.IntBuffer p2) {
    record("glUniform4uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glUniformBlockBinding(int p0, int p1, int p2) {
    record("glUniformBlockBinding", new Object[] {p0, p1, p2});
  }

  @Override public void glUniformBufferEXT(int p0, int p1, int p2) {
    record("glUniformBufferEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glUniformMatrix2fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix2fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix2fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix2fvARB(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix2fvARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix2fvARB(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix2fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix2x3fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix2x3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix2x3fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix2x3fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix2x4fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix2x4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix2x4fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix2x4fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix3fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix3fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix3fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix3fvARB(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix3fvARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix3fvARB(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix3fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix3x2fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix3x2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix3x2fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix3x2fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix3x4fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix3x4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix3x4fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix3x4fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix4fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix4fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix4fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix4fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix4fvARB(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix4fvARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix4fvARB(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix4fvARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix4x2fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix4x2fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix4x2fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix4x2fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformMatrix4x3fv(int p0, int p1, boolean p2, float[] p3, int p4) {
    record("glUniformMatrix4x3fv", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glUniformMatrix4x3fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    record("glUniformMatrix4x3fv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUniformui64NV(int p0, long p1) {
    record("glUniformui64NV", new Object[] {p0, p1});
  }

  @Override public void glUniformui64vNV(int p0, int p1, java.nio.LongBuffer p2) {
    record("glUniformui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glUniformui64vNV(int p0, int p1, long[] p2, int p3) {
    record("glUniformui64vNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glUnlockArraysEXT() {
    record("glUnlockArraysEXT", new Object[] {});
  }

  @Override public boolean glUnmapBuffer(int p0) {
    record("glUnmapBuffer", new Object[] {p0});
    return false;
  }

  @Override public boolean glUnmapNamedBufferEXT(int p0) {
    record("glUnmapNamedBufferEXT", new Object[] {p0});
    return false;
  }

  @Override public void glUnmapTexture2DINTEL(int p0, int p1) {
    record("glUnmapTexture2DINTEL", new Object[] {p0, p1});
  }

  @Override public void glUploadGpuMaskNVX(int p0) {
    record("glUploadGpuMaskNVX", new Object[] {p0});
  }

  @Override public void glUseProgram(int p0) {
    record("glUseProgram", new Object[] {p0});
  }

  @Override public void glUseProgramObjectARB(long p0) {
    record("glUseProgramObjectARB", new Object[] {p0});
  }

  @Override public void glUseProgramStages(int p0, int p1, int p2) {
    record("glUseProgramStages", new Object[] {p0, p1, p2});
  }

  @Override public void glVDPAUFiniNV() {
    record("glVDPAUFiniNV", new Object[] {});
  }

  @Override public void glVDPAUGetSurfaceivNV(long p0, int p1, int p2, int[] p3, int p4, int[] p5, int p6) {
    record("glVDPAUGetSurfaceivNV", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glVDPAUGetSurfaceivNV(long p0, int p1, int p2, java.nio.IntBuffer p3, java.nio.IntBuffer p4) {
    record("glVDPAUGetSurfaceivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVDPAUInitNV(java.nio.Buffer p0, java.nio.Buffer p1) {
    record("glVDPAUInitNV", new Object[] {p0, p1});
  }

  @Override public boolean glVDPAUIsSurfaceNV(long p0) {
    record("glVDPAUIsSurfaceNV", new Object[] {p0});
    return false;
  }

  @Override public void glVDPAUMapSurfacesNV(int p0, com.jogamp.common.nio.PointerBuffer p1) {
    record("glVDPAUMapSurfacesNV", new Object[] {p0, p1});
  }

  @Override public long glVDPAURegisterOutputSurfaceNV(java.nio.Buffer p0, int p1, int p2, int[] p3, int p4) {
    record("glVDPAURegisterOutputSurfaceNV", new Object[] {p0, p1, p2, p3, p4});
    return 0L;
  }

  @Override public long glVDPAURegisterOutputSurfaceNV(java.nio.Buffer p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glVDPAURegisterOutputSurfaceNV", new Object[] {p0, p1, p2, p3});
    return 0L;
  }

  @Override public long glVDPAURegisterVideoSurfaceNV(java.nio.Buffer p0, int p1, int p2, int[] p3, int p4) {
    record("glVDPAURegisterVideoSurfaceNV", new Object[] {p0, p1, p2, p3, p4});
    return 0L;
  }

  @Override public long glVDPAURegisterVideoSurfaceNV(java.nio.Buffer p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glVDPAURegisterVideoSurfaceNV", new Object[] {p0, p1, p2, p3});
    return 0L;
  }

  @Override public long glVDPAURegisterVideoSurfaceWithPictureStructureNV(java.nio.Buffer p0, int p1, int p2, int[] p3, int p4, boolean p5) {
    record("glVDPAURegisterVideoSurfaceWithPictureStructureNV", new Object[] {p0, p1, p2, p3, p4, p5});
    return 0L;
  }

  @Override public long glVDPAURegisterVideoSurfaceWithPictureStructureNV(java.nio.Buffer p0, int p1, int p2, java.nio.IntBuffer p3, boolean p4) {
    record("glVDPAURegisterVideoSurfaceWithPictureStructureNV", new Object[] {p0, p1, p2, p3, p4});
    return 0L;
  }

  @Override public void glVDPAUSurfaceAccessNV(long p0, int p1) {
    record("glVDPAUSurfaceAccessNV", new Object[] {p0, p1});
  }

  @Override public void glVDPAUUnmapSurfacesNV(int p0, com.jogamp.common.nio.PointerBuffer p1) {
    record("glVDPAUUnmapSurfacesNV", new Object[] {p0, p1});
  }

  @Override public void glVDPAUUnregisterSurfaceNV(long p0) {
    record("glVDPAUUnregisterSurfaceNV", new Object[] {p0});
  }

  @Override public void glValidateProgram(int p0) {
    record("glValidateProgram", new Object[] {p0});
  }

  @Override public void glValidateProgramARB(long p0) {
    record("glValidateProgramARB", new Object[] {p0});
  }

  @Override public void glValidateProgramPipeline(int p0) {
    record("glValidateProgramPipeline", new Object[] {p0});
  }

  @Override public void glVariantPointerEXT(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glVariantPointerEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVariantPointerEXT(int p0, int p1, int p2, long p3) {
    record("glVariantPointerEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVariantbvEXT(int p0, byte[] p1, int p2) {
    record("glVariantbvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantbvEXT(int p0, java.nio.ByteBuffer p1) {
    record("glVariantbvEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantdvEXT(int p0, double[] p1, int p2) {
    record("glVariantdvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantdvEXT(int p0, java.nio.DoubleBuffer p1) {
    record("glVariantdvEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantfvEXT(int p0, float[] p1, int p2) {
    record("glVariantfvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantfvEXT(int p0, java.nio.FloatBuffer p1) {
    record("glVariantfvEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantivEXT(int p0, int[] p1, int p2) {
    record("glVariantivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantivEXT(int p0, java.nio.IntBuffer p1) {
    record("glVariantivEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantsvEXT(int p0, java.nio.ShortBuffer p1) {
    record("glVariantsvEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantsvEXT(int p0, short[] p1, int p2) {
    record("glVariantsvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantubvEXT(int p0, byte[] p1, int p2) {
    record("glVariantubvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantubvEXT(int p0, java.nio.ByteBuffer p1) {
    record("glVariantubvEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantuivEXT(int p0, int[] p1, int p2) {
    record("glVariantuivEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVariantuivEXT(int p0, java.nio.IntBuffer p1) {
    record("glVariantuivEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantusvEXT(int p0, java.nio.ShortBuffer p1) {
    record("glVariantusvEXT", new Object[] {p0, p1});
  }

  @Override public void glVariantusvEXT(int p0, short[] p1, int p2) {
    record("glVariantusvEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex2bOES(byte p0, byte p1) {
    record("glVertex2bOES", new Object[] {p0, p1});
  }

  @Override public void glVertex2bvOES(byte[] p0, int p1) {
    record("glVertex2bvOES", new Object[] {p0, p1});
  }

  @Override public void glVertex2bvOES(java.nio.ByteBuffer p0) {
    record("glVertex2bvOES", new Object[] {p0});
  }

  @Override public void glVertex2d(double p0, double p1) {
    record("glVertex2d", new Object[] {p0, p1});
  }

  @Override public void glVertex2dv(double[] p0, int p1) {
    record("glVertex2dv", new Object[] {p0, p1});
  }

  @Override public void glVertex2dv(java.nio.DoubleBuffer p0) {
    record("glVertex2dv", new Object[] {p0});
  }

  @Override public void glVertex2f(float p0, float p1) {
    record("glVertex2f", new Object[] {p0, p1});
  }

  @Override public void glVertex2fv(float[] p0, int p1) {
    record("glVertex2fv", new Object[] {p0, p1});
  }

  @Override public void glVertex2fv(java.nio.FloatBuffer p0) {
    record("glVertex2fv", new Object[] {p0});
  }

  @Override public void glVertex2h(short p0, short p1) {
    record("glVertex2h", new Object[] {p0, p1});
  }

  @Override public void glVertex2hv(java.nio.ShortBuffer p0) {
    record("glVertex2hv", new Object[] {p0});
  }

  @Override public void glVertex2hv(short[] p0, int p1) {
    record("glVertex2hv", new Object[] {p0, p1});
  }

  @Override public void glVertex2i(int p0, int p1) {
    record("glVertex2i", new Object[] {p0, p1});
  }

  @Override public void glVertex2iv(int[] p0, int p1) {
    record("glVertex2iv", new Object[] {p0, p1});
  }

  @Override public void glVertex2iv(java.nio.IntBuffer p0) {
    record("glVertex2iv", new Object[] {p0});
  }

  @Override public void glVertex2s(short p0, short p1) {
    record("glVertex2s", new Object[] {p0, p1});
  }

  @Override public void glVertex2sv(java.nio.ShortBuffer p0) {
    record("glVertex2sv", new Object[] {p0});
  }

  @Override public void glVertex2sv(short[] p0, int p1) {
    record("glVertex2sv", new Object[] {p0, p1});
  }

  @Override public void glVertex3bOES(byte p0, byte p1, byte p2) {
    record("glVertex3bOES", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex3bvOES(byte[] p0, int p1) {
    record("glVertex3bvOES", new Object[] {p0, p1});
  }

  @Override public void glVertex3bvOES(java.nio.ByteBuffer p0) {
    record("glVertex3bvOES", new Object[] {p0});
  }

  @Override public void glVertex3d(double p0, double p1, double p2) {
    record("glVertex3d", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex3dv(double[] p0, int p1) {
    record("glVertex3dv", new Object[] {p0, p1});
  }

  @Override public void glVertex3dv(java.nio.DoubleBuffer p0) {
    record("glVertex3dv", new Object[] {p0});
  }

  @Override public void glVertex3f(float p0, float p1, float p2) {
    record("glVertex3f", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex3fv(float[] p0, int p1) {
    record("glVertex3fv", new Object[] {p0, p1});
  }

  @Override public void glVertex3fv(java.nio.FloatBuffer p0) {
    record("glVertex3fv", new Object[] {p0});
  }

  @Override public void glVertex3h(short p0, short p1, short p2) {
    record("glVertex3h", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex3hv(java.nio.ShortBuffer p0) {
    record("glVertex3hv", new Object[] {p0});
  }

  @Override public void glVertex3hv(short[] p0, int p1) {
    record("glVertex3hv", new Object[] {p0, p1});
  }

  @Override public void glVertex3i(int p0, int p1, int p2) {
    record("glVertex3i", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex3iv(int[] p0, int p1) {
    record("glVertex3iv", new Object[] {p0, p1});
  }

  @Override public void glVertex3iv(java.nio.IntBuffer p0) {
    record("glVertex3iv", new Object[] {p0});
  }

  @Override public void glVertex3s(short p0, short p1, short p2) {
    record("glVertex3s", new Object[] {p0, p1, p2});
  }

  @Override public void glVertex3sv(java.nio.ShortBuffer p0) {
    record("glVertex3sv", new Object[] {p0});
  }

  @Override public void glVertex3sv(short[] p0, int p1) {
    record("glVertex3sv", new Object[] {p0, p1});
  }

  @Override public void glVertex4bOES(byte p0, byte p1, byte p2, byte p3) {
    record("glVertex4bOES", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertex4bvOES(byte[] p0, int p1) {
    record("glVertex4bvOES", new Object[] {p0, p1});
  }

  @Override public void glVertex4bvOES(java.nio.ByteBuffer p0) {
    record("glVertex4bvOES", new Object[] {p0});
  }

  @Override public void glVertex4d(double p0, double p1, double p2, double p3) {
    record("glVertex4d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertex4dv(double[] p0, int p1) {
    record("glVertex4dv", new Object[] {p0, p1});
  }

  @Override public void glVertex4dv(java.nio.DoubleBuffer p0) {
    record("glVertex4dv", new Object[] {p0});
  }

  @Override public void glVertex4f(float p0, float p1, float p2, float p3) {
    record("glVertex4f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertex4fv(float[] p0, int p1) {
    record("glVertex4fv", new Object[] {p0, p1});
  }

  @Override public void glVertex4fv(java.nio.FloatBuffer p0) {
    record("glVertex4fv", new Object[] {p0});
  }

  @Override public void glVertex4h(short p0, short p1, short p2, short p3) {
    record("glVertex4h", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertex4hv(java.nio.ShortBuffer p0) {
    record("glVertex4hv", new Object[] {p0});
  }

  @Override public void glVertex4hv(short[] p0, int p1) {
    record("glVertex4hv", new Object[] {p0, p1});
  }

  @Override public void glVertex4i(int p0, int p1, int p2, int p3) {
    record("glVertex4i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertex4iv(int[] p0, int p1) {
    record("glVertex4iv", new Object[] {p0, p1});
  }

  @Override public void glVertex4iv(java.nio.IntBuffer p0) {
    record("glVertex4iv", new Object[] {p0});
  }

  @Override public void glVertex4s(short p0, short p1, short p2, short p3) {
    record("glVertex4s", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertex4sv(java.nio.ShortBuffer p0) {
    record("glVertex4sv", new Object[] {p0});
  }

  @Override public void glVertex4sv(short[] p0, int p1) {
    record("glVertex4sv", new Object[] {p0, p1});
  }

  @Override public void glVertexArrayBindVertexBufferEXT(int p0, int p1, int p2, long p3, int p4) {
    record("glVertexArrayBindVertexBufferEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexArrayColorOffsetEXT(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glVertexArrayColorOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexArrayEdgeFlagOffsetEXT(int p0, int p1, int p2, long p3) {
    record("glVertexArrayEdgeFlagOffsetEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexArrayFogCoordOffsetEXT(int p0, int p1, int p2, int p3, long p4) {
    record("glVertexArrayFogCoordOffsetEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexArrayIndexOffsetEXT(int p0, int p1, int p2, int p3, long p4) {
    record("glVertexArrayIndexOffsetEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexArrayMultiTexCoordOffsetEXT(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glVertexArrayMultiTexCoordOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glVertexArrayNormalOffsetEXT(int p0, int p1, int p2, int p3, long p4) {
    record("glVertexArrayNormalOffsetEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexArrayParameteriAPPLE(int p0, int p1) {
    record("glVertexArrayParameteriAPPLE", new Object[] {p0, p1});
  }

  @Override public void glVertexArrayRangeAPPLE(int p0, java.nio.Buffer p1) {
    record("glVertexArrayRangeAPPLE", new Object[] {p0, p1});
  }

  @Override public void glVertexArraySecondaryColorOffsetEXT(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glVertexArraySecondaryColorOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexArrayTexCoordOffsetEXT(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glVertexArrayTexCoordOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexArrayVertexAttribBindingEXT(int p0, int p1, int p2) {
    record("glVertexArrayVertexAttribBindingEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexArrayVertexAttribDivisorEXT(int p0, int p1, int p2) {
    record("glVertexArrayVertexAttribDivisorEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexArrayVertexAttribFormatEXT(int p0, int p1, int p2, int p3, boolean p4, int p5) {
    record("glVertexArrayVertexAttribFormatEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexArrayVertexAttribIFormatEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glVertexArrayVertexAttribIFormatEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexArrayVertexAttribIOffsetEXT(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glVertexArrayVertexAttribIOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glVertexArrayVertexAttribLFormatEXT(int p0, int p1, int p2, int p3, int p4) {
    record("glVertexArrayVertexAttribLFormatEXT", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexArrayVertexAttribLOffsetEXT(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    record("glVertexArrayVertexAttribLOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6});
  }

  @Override public void glVertexArrayVertexAttribOffsetEXT(int p0, int p1, int p2, int p3, int p4, boolean p5, int p6, long p7) {
    record("glVertexArrayVertexAttribOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override public void glVertexArrayVertexBindingDivisorEXT(int p0, int p1, int p2) {
    record("glVertexArrayVertexBindingDivisorEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexArrayVertexOffsetEXT(int p0, int p1, int p2, int p3, int p4, long p5) {
    record("glVertexArrayVertexOffsetEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexAttrib1d(int p0, double p1) {
    record("glVertexAttrib1d", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1dARB(int p0, double p1) {
    record("glVertexAttrib1dARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1dv(int p0, double[] p1, int p2) {
    record("glVertexAttrib1dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib1dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib1dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1dvARB(int p0, double[] p1, int p2) {
    record("glVertexAttrib1dvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib1dvARB(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib1dvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1f(int p0, float p1) {
    record("glVertexAttrib1f", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1fARB(int p0, float p1) {
    record("glVertexAttrib1fARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1fv(int p0, float[] p1, int p2) {
    record("glVertexAttrib1fv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib1fv(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib1fv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1fvARB(int p0, float[] p1, int p2) {
    record("glVertexAttrib1fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib1fvARB(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib1fvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1h(int p0, short p1) {
    record("glVertexAttrib1h", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1hv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib1hv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1hv(int p0, short[] p1, int p2) {
    record("glVertexAttrib1hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib1s(int p0, short p1) {
    record("glVertexAttrib1s", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1sARB(int p0, short p1) {
    record("glVertexAttrib1sARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1sv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib1sv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1sv(int p0, short[] p1, int p2) {
    record("glVertexAttrib1sv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib1svARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib1svARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib1svARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib1svARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2d(int p0, double p1, double p2) {
    record("glVertexAttrib2d", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2dARB(int p0, double p1, double p2) {
    record("glVertexAttrib2dARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2dv(int p0, double[] p1, int p2) {
    record("glVertexAttrib2dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib2dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2dvARB(int p0, double[] p1, int p2) {
    record("glVertexAttrib2dvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2dvARB(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib2dvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2f(int p0, float p1, float p2) {
    record("glVertexAttrib2f", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2fARB(int p0, float p1, float p2) {
    record("glVertexAttrib2fARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2fv(int p0, float[] p1, int p2) {
    record("glVertexAttrib2fv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2fv(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib2fv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2fvARB(int p0, float[] p1, int p2) {
    record("glVertexAttrib2fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2fvARB(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib2fvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2h(int p0, short p1, short p2) {
    record("glVertexAttrib2h", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2hv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib2hv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2hv(int p0, short[] p1, int p2) {
    record("glVertexAttrib2hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2s(int p0, short p1, short p2) {
    record("glVertexAttrib2s", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2sARB(int p0, short p1, short p2) {
    record("glVertexAttrib2sARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2sv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib2sv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2sv(int p0, short[] p1, int p2) {
    record("glVertexAttrib2sv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib2svARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib2svARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib2svARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib2svARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3d(int p0, double p1, double p2, double p3) {
    record("glVertexAttrib3d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3dARB(int p0, double p1, double p2, double p3) {
    record("glVertexAttrib3dARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3dv(int p0, double[] p1, int p2) {
    record("glVertexAttrib3dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib3dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3dvARB(int p0, double[] p1, int p2) {
    record("glVertexAttrib3dvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3dvARB(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib3dvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3f(int p0, float p1, float p2, float p3) {
    record("glVertexAttrib3f", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3fARB(int p0, float p1, float p2, float p3) {
    record("glVertexAttrib3fARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3fv(int p0, float[] p1, int p2) {
    record("glVertexAttrib3fv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3fv(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib3fv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3fvARB(int p0, float[] p1, int p2) {
    record("glVertexAttrib3fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3fvARB(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib3fvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3h(int p0, short p1, short p2, short p3) {
    record("glVertexAttrib3h", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3hv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib3hv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3hv(int p0, short[] p1, int p2) {
    record("glVertexAttrib3hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3s(int p0, short p1, short p2, short p3) {
    record("glVertexAttrib3s", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3sARB(int p0, short p1, short p2, short p3) {
    record("glVertexAttrib3sARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttrib3sv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib3sv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3sv(int p0, short[] p1, int p2) {
    record("glVertexAttrib3sv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib3svARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib3svARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib3svARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib3svARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4Nbv(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4Nbv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4Nbv(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4Nbv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4NbvARB(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4NbvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4NbvARB(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4NbvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4Niv(int p0, int[] p1, int p2) {
    record("glVertexAttrib4Niv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4Niv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4Niv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4NivARB(int p0, int[] p1, int p2) {
    record("glVertexAttrib4NivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4NivARB(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4NivARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4Nsv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4Nsv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4Nsv(int p0, short[] p1, int p2) {
    record("glVertexAttrib4Nsv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4NsvARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4NsvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4NsvARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib4NsvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4Nub(int p0, byte p1, byte p2, byte p3, byte p4) {
    record("glVertexAttrib4Nub", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4NubARB(int p0, byte p1, byte p2, byte p3, byte p4) {
    record("glVertexAttrib4NubARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4Nubv(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4Nubv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4Nubv(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4Nubv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4NubvARB(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4NubvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4NubvARB(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4NubvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4Nuiv(int p0, int[] p1, int p2) {
    record("glVertexAttrib4Nuiv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4Nuiv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4Nuiv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4NuivARB(int p0, int[] p1, int p2) {
    record("glVertexAttrib4NuivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4NuivARB(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4NuivARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4Nusv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4Nusv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4Nusv(int p0, short[] p1, int p2) {
    record("glVertexAttrib4Nusv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4NusvARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4NusvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4NusvARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib4NusvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4bv(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4bv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4bv(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4bv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4bvARB(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4bvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4bvARB(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4bvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4d(int p0, double p1, double p2, double p3, double p4) {
    record("glVertexAttrib4d", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4dARB(int p0, double p1, double p2, double p3, double p4) {
    record("glVertexAttrib4dARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4dv(int p0, double[] p1, int p2) {
    record("glVertexAttrib4dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib4dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4dvARB(int p0, double[] p1, int p2) {
    record("glVertexAttrib4dvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4dvARB(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttrib4dvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4f(int p0, float p1, float p2, float p3, float p4) {
    record("glVertexAttrib4f", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4fARB(int p0, float p1, float p2, float p3, float p4) {
    record("glVertexAttrib4fARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4fv(int p0, float[] p1, int p2) {
    record("glVertexAttrib4fv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4fv(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib4fv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4fvARB(int p0, float[] p1, int p2) {
    record("glVertexAttrib4fvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4fvARB(int p0, java.nio.FloatBuffer p1) {
    record("glVertexAttrib4fvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4h(int p0, short p1, short p2, short p3, short p4) {
    record("glVertexAttrib4h", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4hv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4hv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4hv(int p0, short[] p1, int p2) {
    record("glVertexAttrib4hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4iv(int p0, int[] p1, int p2) {
    record("glVertexAttrib4iv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4iv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4iv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4ivARB(int p0, int[] p1, int p2) {
    record("glVertexAttrib4ivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4ivARB(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4ivARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4s(int p0, short p1, short p2, short p3, short p4) {
    record("glVertexAttrib4s", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4sARB(int p0, short p1, short p2, short p3, short p4) {
    record("glVertexAttrib4sARB", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttrib4sv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4sv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4sv(int p0, short[] p1, int p2) {
    record("glVertexAttrib4sv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4svARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4svARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4svARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib4svARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4ubv(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4ubv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4ubv(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4ubv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4ubvARB(int p0, byte[] p1, int p2) {
    record("glVertexAttrib4ubvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4ubvARB(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttrib4ubvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4uiv(int p0, int[] p1, int p2) {
    record("glVertexAttrib4uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4uiv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4uiv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4uivARB(int p0, int[] p1, int p2) {
    record("glVertexAttrib4uivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4uivARB(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttrib4uivARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4usv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4usv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4usv(int p0, short[] p1, int p2) {
    record("glVertexAttrib4usv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttrib4usvARB(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttrib4usvARB", new Object[] {p0, p1});
  }

  @Override public void glVertexAttrib4usvARB(int p0, short[] p1, int p2) {
    record("glVertexAttrib4usvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribDivisor(int p0, int p1) {
    record("glVertexAttribDivisor", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribFormatNV(int p0, int p1, int p2, boolean p3, int p4) {
    record("glVertexAttribFormatNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribI1i(int p0, int p1) {
    record("glVertexAttribI1i", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI1iv(int p0, int[] p1, int p2) {
    record("glVertexAttribI1iv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI1iv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI1iv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI1ui(int p0, int p1) {
    record("glVertexAttribI1ui", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI1uiv(int p0, int[] p1, int p2) {
    record("glVertexAttribI1uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI1uiv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI1uiv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI2i(int p0, int p1, int p2) {
    record("glVertexAttribI2i", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI2iv(int p0, int[] p1, int p2) {
    record("glVertexAttribI2iv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI2iv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI2iv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI2ui(int p0, int p1, int p2) {
    record("glVertexAttribI2ui", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI2uiv(int p0, int[] p1, int p2) {
    record("glVertexAttribI2uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI2uiv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI2uiv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI3i(int p0, int p1, int p2, int p3) {
    record("glVertexAttribI3i", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribI3iv(int p0, int[] p1, int p2) {
    record("glVertexAttribI3iv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI3iv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI3iv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI3ui(int p0, int p1, int p2, int p3) {
    record("glVertexAttribI3ui", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribI3uiv(int p0, int[] p1, int p2) {
    record("glVertexAttribI3uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI3uiv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI3uiv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4bv(int p0, byte[] p1, int p2) {
    record("glVertexAttribI4bv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI4bv(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttribI4bv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4i(int p0, int p1, int p2, int p3, int p4) {
    record("glVertexAttribI4i", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribI4iv(int p0, int[] p1, int p2) {
    record("glVertexAttribI4iv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI4iv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI4iv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4sv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttribI4sv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4sv(int p0, short[] p1, int p2) {
    record("glVertexAttribI4sv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI4ubv(int p0, byte[] p1, int p2) {
    record("glVertexAttribI4ubv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI4ubv(int p0, java.nio.ByteBuffer p1) {
    record("glVertexAttribI4ubv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4ui(int p0, int p1, int p2, int p3, int p4) {
    record("glVertexAttribI4ui", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribI4uiv(int p0, int[] p1, int p2) {
    record("glVertexAttribI4uiv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribI4uiv(int p0, java.nio.IntBuffer p1) {
    record("glVertexAttribI4uiv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4usv(int p0, java.nio.ShortBuffer p1) {
    record("glVertexAttribI4usv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribI4usv(int p0, short[] p1, int p2) {
    record("glVertexAttribI4usv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribIFormatNV(int p0, int p1, int p2, int p3) {
    record("glVertexAttribIFormatNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribIPointer(int p0, int p1, int p2, int p3, java.nio.Buffer p4) {
    record("glVertexAttribIPointer", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribIPointer(int p0, int p1, int p2, int p3, long p4) {
    record("glVertexAttribIPointer", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribL1d(int p0, double p1) {
    record("glVertexAttribL1d", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL1dv(int p0, double[] p1, int p2) {
    record("glVertexAttribL1dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL1dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttribL1dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL1i64NV(int p0, long p1) {
    record("glVertexAttribL1i64NV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL1i64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL1i64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL1i64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL1i64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL1ui64NV(int p0, long p1) {
    record("glVertexAttribL1ui64NV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL1ui64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL1ui64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL1ui64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL1ui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL2d(int p0, double p1, double p2) {
    record("glVertexAttribL2d", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL2dv(int p0, double[] p1, int p2) {
    record("glVertexAttribL2dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL2dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttribL2dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL2i64NV(int p0, long p1, long p2) {
    record("glVertexAttribL2i64NV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL2i64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL2i64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL2i64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL2i64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL2ui64NV(int p0, long p1, long p2) {
    record("glVertexAttribL2ui64NV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL2ui64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL2ui64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL2ui64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL2ui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL3d(int p0, double p1, double p2, double p3) {
    record("glVertexAttribL3d", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribL3dv(int p0, double[] p1, int p2) {
    record("glVertexAttribL3dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL3dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttribL3dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL3i64NV(int p0, long p1, long p2, long p3) {
    record("glVertexAttribL3i64NV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribL3i64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL3i64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL3i64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL3i64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL3ui64NV(int p0, long p1, long p2, long p3) {
    record("glVertexAttribL3ui64NV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribL3ui64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL3ui64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL3ui64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL3ui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL4d(int p0, double p1, double p2, double p3, double p4) {
    record("glVertexAttribL4d", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribL4dv(int p0, double[] p1, int p2) {
    record("glVertexAttribL4dv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL4dv(int p0, java.nio.DoubleBuffer p1) {
    record("glVertexAttribL4dv", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL4i64NV(int p0, long p1, long p2, long p3, long p4) {
    record("glVertexAttribL4i64NV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribL4i64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL4i64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL4i64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL4i64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribL4ui64NV(int p0, long p1, long p2, long p3, long p4) {
    record("glVertexAttribL4ui64NV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribL4ui64vNV(int p0, java.nio.LongBuffer p1) {
    record("glVertexAttribL4ui64vNV", new Object[] {p0, p1});
  }

  @Override public void glVertexAttribL4ui64vNV(int p0, long[] p1, int p2) {
    record("glVertexAttribL4ui64vNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribLFormatNV(int p0, int p1, int p2, int p3) {
    record("glVertexAttribLFormatNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribLPointer(int p0, int p1, int p2, int p3, long p4) {
    record("glVertexAttribLPointer", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVertexAttribParameteriAMD(int p0, int p1, int p2) {
    record("glVertexAttribParameteriAMD", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribPointer(com.jogamp.opengl.GLArrayData p0) {
    record("glVertexAttribPointer", new Object[] {p0});
  }

  @Override public void glVertexAttribPointer(int p0, int p1, int p2, boolean p3, int p4, java.nio.Buffer p5) {
    record("glVertexAttribPointer", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexAttribPointer(int p0, int p1, int p2, boolean p3, int p4, long p5) {
    record("glVertexAttribPointer", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexAttribPointerARB(int p0, int p1, int p2, boolean p3, int p4, java.nio.Buffer p5) {
    record("glVertexAttribPointerARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexAttribPointerARB(int p0, int p1, int p2, boolean p3, int p4, long p5) {
    record("glVertexAttribPointerARB", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glVertexAttribs1hv(int p0, int p1, java.nio.ShortBuffer p2) {
    record("glVertexAttribs1hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribs1hv(int p0, int p1, short[] p2, int p3) {
    record("glVertexAttribs1hv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribs2hv(int p0, int p1, java.nio.ShortBuffer p2) {
    record("glVertexAttribs2hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribs2hv(int p0, int p1, short[] p2, int p3) {
    record("glVertexAttribs2hv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribs3hv(int p0, int p1, java.nio.ShortBuffer p2) {
    record("glVertexAttribs3hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribs3hv(int p0, int p1, short[] p2, int p3) {
    record("glVertexAttribs3hv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexAttribs4hv(int p0, int p1, java.nio.ShortBuffer p2) {
    record("glVertexAttribs4hv", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexAttribs4hv(int p0, int p1, short[] p2, int p3) {
    record("glVertexAttribs4hv", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexBlendARB(int p0) {
    record("glVertexBlendARB", new Object[] {p0});
  }

  @Override public void glVertexFormatNV(int p0, int p1, int p2) {
    record("glVertexFormatNV", new Object[] {p0, p1, p2});
  }

  @Override public void glVertexPointer(com.jogamp.opengl.GLArrayData p0) {
    record("glVertexPointer", new Object[] {p0});
  }

  @Override public void glVertexPointer(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glVertexPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexPointer(int p0, int p1, int p2, long p3) {
    record("glVertexPointer", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexWeightPointerEXT(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glVertexWeightPointerEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexWeightPointerEXT(int p0, int p1, int p2, long p3) {
    record("glVertexWeightPointerEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVertexWeightfEXT(float p0) {
    record("glVertexWeightfEXT", new Object[] {p0});
  }

  @Override public void glVertexWeightfvEXT(float[] p0, int p1) {
    record("glVertexWeightfvEXT", new Object[] {p0, p1});
  }

  @Override public void glVertexWeightfvEXT(java.nio.FloatBuffer p0) {
    record("glVertexWeightfvEXT", new Object[] {p0});
  }

  @Override public void glVertexWeighth(short p0) {
    record("glVertexWeighth", new Object[] {p0});
  }

  @Override public void glVertexWeighthv(java.nio.ShortBuffer p0) {
    record("glVertexWeighthv", new Object[] {p0});
  }

  @Override public void glVertexWeighthv(short[] p0, int p1) {
    record("glVertexWeighthv", new Object[] {p0, p1});
  }

  @Override public int glVideoCaptureNV(int p0, int[] p1, int p2, long[] p3, int p4) {
    record("glVideoCaptureNV", new Object[] {p0, p1, p2, p3, p4});
    return 0;
  }

  @Override public int glVideoCaptureNV(int p0, java.nio.IntBuffer p1, java.nio.LongBuffer p2) {
    record("glVideoCaptureNV", new Object[] {p0, p1, p2});
    return 0;
  }

  @Override public void glVideoCaptureStreamParameterdvNV(int p0, int p1, int p2, double[] p3, int p4) {
    record("glVideoCaptureStreamParameterdvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVideoCaptureStreamParameterdvNV(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    record("glVideoCaptureStreamParameterdvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVideoCaptureStreamParameterfvNV(int p0, int p1, int p2, float[] p3, int p4) {
    record("glVideoCaptureStreamParameterfvNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVideoCaptureStreamParameterfvNV(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
    record("glVideoCaptureStreamParameterfvNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glVideoCaptureStreamParameterivNV(int p0, int p1, int p2, int[] p3, int p4) {
    record("glVideoCaptureStreamParameterivNV", new Object[] {p0, p1, p2, p3, p4});
  }

  @Override public void glVideoCaptureStreamParameterivNV(int p0, int p1, int p2, java.nio.IntBuffer p3) {
    record("glVideoCaptureStreamParameterivNV", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glViewport(int p0, int p1, int p2, int p3) {
    record("glViewport", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glWaitSemaphoreEXT(int p0, int p1, int[] p2, int p3, int p4, int[] p5, int p6, int[] p7, int p8) {
    record("glWaitSemaphoreEXT", new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override public void glWaitSemaphoreEXT(int p0, int p1, java.nio.IntBuffer p2, int p3, java.nio.IntBuffer p4, java.nio.IntBuffer p5) {
    record("glWaitSemaphoreEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glWaitSemaphoreui64NVX(int p0, int p1, int[] p2, int p3, long[] p4, int p5) {
    record("glWaitSemaphoreui64NVX", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public void glWaitSemaphoreui64NVX(int p0, int p1, java.nio.IntBuffer p2, java.nio.LongBuffer p3) {
    record("glWaitSemaphoreui64NVX", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glWeightPointerARB(int p0, int p1, int p2, java.nio.Buffer p3) {
    record("glWeightPointerARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glWeightPointerARB(int p0, int p1, int p2, long p3) {
    record("glWeightPointerARB", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glWeightbvARB(int p0, byte[] p1, int p2) {
    record("glWeightbvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightbvARB(int p0, java.nio.ByteBuffer p1) {
    record("glWeightbvARB", new Object[] {p0, p1});
  }

  @Override public void glWeightdvARB(int p0, double[] p1, int p2) {
    record("glWeightdvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightdvARB(int p0, java.nio.DoubleBuffer p1) {
    record("glWeightdvARB", new Object[] {p0, p1});
  }

  @Override public void glWeightfvARB(int p0, float[] p1, int p2) {
    record("glWeightfvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightfvARB(int p0, java.nio.FloatBuffer p1) {
    record("glWeightfvARB", new Object[] {p0, p1});
  }

  @Override public void glWeightivARB(int p0, int[] p1, int p2) {
    record("glWeightivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightivARB(int p0, java.nio.IntBuffer p1) {
    record("glWeightivARB", new Object[] {p0, p1});
  }

  @Override public void glWeightsvARB(int p0, java.nio.ShortBuffer p1) {
    record("glWeightsvARB", new Object[] {p0, p1});
  }

  @Override public void glWeightsvARB(int p0, short[] p1, int p2) {
    record("glWeightsvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightubvARB(int p0, byte[] p1, int p2) {
    record("glWeightubvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightubvARB(int p0, java.nio.ByteBuffer p1) {
    record("glWeightubvARB", new Object[] {p0, p1});
  }

  @Override public void glWeightuivARB(int p0, int[] p1, int p2) {
    record("glWeightuivARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWeightuivARB(int p0, java.nio.IntBuffer p1) {
    record("glWeightuivARB", new Object[] {p0, p1});
  }

  @Override public void glWeightusvARB(int p0, java.nio.ShortBuffer p1) {
    record("glWeightusvARB", new Object[] {p0, p1});
  }

  @Override public void glWeightusvARB(int p0, short[] p1, int p2) {
    record("glWeightusvARB", new Object[] {p0, p1, p2});
  }

  @Override public void glWindowPos2d(double p0, double p1) {
    record("glWindowPos2d", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2dv(double[] p0, int p1) {
    record("glWindowPos2dv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2dv(java.nio.DoubleBuffer p0) {
    record("glWindowPos2dv", new Object[] {p0});
  }

  @Override public void glWindowPos2f(float p0, float p1) {
    record("glWindowPos2f", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2fv(float[] p0, int p1) {
    record("glWindowPos2fv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2fv(java.nio.FloatBuffer p0) {
    record("glWindowPos2fv", new Object[] {p0});
  }

  @Override public void glWindowPos2i(int p0, int p1) {
    record("glWindowPos2i", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2iv(int[] p0, int p1) {
    record("glWindowPos2iv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2iv(java.nio.IntBuffer p0) {
    record("glWindowPos2iv", new Object[] {p0});
  }

  @Override public void glWindowPos2s(short p0, short p1) {
    record("glWindowPos2s", new Object[] {p0, p1});
  }

  @Override public void glWindowPos2sv(java.nio.ShortBuffer p0) {
    record("glWindowPos2sv", new Object[] {p0});
  }

  @Override public void glWindowPos2sv(short[] p0, int p1) {
    record("glWindowPos2sv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos3d(double p0, double p1, double p2) {
    record("glWindowPos3d", new Object[] {p0, p1, p2});
  }

  @Override public void glWindowPos3dv(double[] p0, int p1) {
    record("glWindowPos3dv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos3dv(java.nio.DoubleBuffer p0) {
    record("glWindowPos3dv", new Object[] {p0});
  }

  @Override public void glWindowPos3f(float p0, float p1, float p2) {
    record("glWindowPos3f", new Object[] {p0, p1, p2});
  }

  @Override public void glWindowPos3fv(float[] p0, int p1) {
    record("glWindowPos3fv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos3fv(java.nio.FloatBuffer p0) {
    record("glWindowPos3fv", new Object[] {p0});
  }

  @Override public void glWindowPos3i(int p0, int p1, int p2) {
    record("glWindowPos3i", new Object[] {p0, p1, p2});
  }

  @Override public void glWindowPos3iv(int[] p0, int p1) {
    record("glWindowPos3iv", new Object[] {p0, p1});
  }

  @Override public void glWindowPos3iv(java.nio.IntBuffer p0) {
    record("glWindowPos3iv", new Object[] {p0});
  }

  @Override public void glWindowPos3s(short p0, short p1, short p2) {
    record("glWindowPos3s", new Object[] {p0, p1, p2});
  }

  @Override public void glWindowPos3sv(java.nio.ShortBuffer p0) {
    record("glWindowPos3sv", new Object[] {p0});
  }

  @Override public void glWindowPos3sv(short[] p0, int p1) {
    record("glWindowPos3sv", new Object[] {p0, p1});
  }

  @Override public void glWindowRectanglesEXT(int p0, int p1, int[] p2, int p3) {
    record("glWindowRectanglesEXT", new Object[] {p0, p1, p2, p3});
  }

  @Override public void glWindowRectanglesEXT(int p0, int p1, java.nio.IntBuffer p2) {
    record("glWindowRectanglesEXT", new Object[] {p0, p1, p2});
  }

  @Override public void glWriteMaskEXT(int p0, int p1, int p2, int p3, int p4, int p5) {
    record("glWriteMaskEXT", new Object[] {p0, p1, p2, p3, p4, p5});
  }

  @Override public boolean hasBasicFBOSupport() {
    record("hasBasicFBOSupport", new Object[] {});
    return false;
  }

  @Override public boolean hasFullFBOSupport() {
    record("hasFullFBOSupport", new Object[] {});
    return false;
  }

  @Override public boolean hasGLSL() {
    record("hasGLSL", new Object[] {});
    return false;
  }

  @Override public boolean isExtensionAvailable(java.lang.String p0) {
    record("isExtensionAvailable", new Object[] {p0});
    return false;
  }

  @Override public boolean isFunctionAvailable(java.lang.String p0) {
    record("isFunctionAvailable", new Object[] {p0});
    return false;
  }

  @Override public boolean isGL() {
    record("isGL", new Object[] {});
    return false;
  }

  @Override public boolean isGL2() {
    record("isGL2", new Object[] {});
    return false;
  }

  @Override public boolean isGL2ES1() {
    record("isGL2ES1", new Object[] {});
    return false;
  }

  @Override public boolean isGL2ES2() {
    record("isGL2ES2", new Object[] {});
    return false;
  }

  @Override public boolean isGL2ES3() {
    record("isGL2ES3", new Object[] {});
    return false;
  }

  @Override public boolean isGL2GL3() {
    record("isGL2GL3", new Object[] {});
    return false;
  }

  @Override public boolean isGL3() {
    record("isGL3", new Object[] {});
    return false;
  }

  @Override public boolean isGL3ES3() {
    record("isGL3ES3", new Object[] {});
    return false;
  }

  @Override public boolean isGL3bc() {
    record("isGL3bc", new Object[] {});
    return false;
  }

  @Override public boolean isGL3core() {
    record("isGL3core", new Object[] {});
    return false;
  }

  @Override public boolean isGL4() {
    record("isGL4", new Object[] {});
    return false;
  }

  @Override public boolean isGL4ES3() {
    record("isGL4ES3", new Object[] {});
    return false;
  }

  @Override public boolean isGL4bc() {
    record("isGL4bc", new Object[] {});
    return false;
  }

  @Override public boolean isGL4core() {
    record("isGL4core", new Object[] {});
    return false;
  }

  @Override public boolean isGLES() {
    record("isGLES", new Object[] {});
    return false;
  }

  @Override public boolean isGLES1() {
    record("isGLES1", new Object[] {});
    return false;
  }

  @Override public boolean isGLES2() {
    record("isGLES2", new Object[] {});
    return false;
  }

  @Override public boolean isGLES2Compatible() {
    record("isGLES2Compatible", new Object[] {});
    return false;
  }

  @Override public boolean isGLES3() {
    record("isGLES3", new Object[] {});
    return false;
  }

  @Override public boolean isGLES31Compatible() {
    record("isGLES31Compatible", new Object[] {});
    return false;
  }

  @Override public boolean isGLES32Compatible() {
    record("isGLES32Compatible", new Object[] {});
    return false;
  }

  @Override public boolean isGLES3Compatible() {
    record("isGLES3Compatible", new Object[] {});
    return false;
  }

  @Override public boolean isGLcore() {
    record("isGLcore", new Object[] {});
    return false;
  }

  @Override public boolean isNPOTTextureAvailable() {
    record("isNPOTTextureAvailable", new Object[] {});
    return false;
  }

  @Override public boolean isPBOPackBound() {
    record("isPBOPackBound", new Object[] {});
    return false;
  }

  @Override public boolean isPBOUnpackBound() {
    record("isPBOUnpackBound", new Object[] {});
    return false;
  }

  @Override public boolean isTextureFormatBGRA8888Available() {
    record("isTextureFormatBGRA8888Available", new Object[] {});
    return false;
  }

  @Override public boolean isVBOArrayBound() {
    record("isVBOArrayBound", new Object[] {});
    return false;
  }

  @Override public boolean isVBOElementArrayBound() {
    record("isVBOElementArrayBound", new Object[] {});
    return false;
  }

  @Override public com.jogamp.opengl.GLBufferStorage mapBuffer(int p0, int p1) {
    record("mapBuffer", new Object[] {p0, p1});
    return null;
  }

  @Override public com.jogamp.opengl.GLBufferStorage mapBufferRange(int p0, long p1, long p2, int p3) {
    record("mapBufferRange", new Object[] {p0, p1, p2, p3});
    return null;
  }

  @Override public com.jogamp.opengl.GLBufferStorage mapNamedBufferEXT(int p0, int p1) {
    record("mapNamedBufferEXT", new Object[] {p0, p1});
    return null;
  }

  @Override public com.jogamp.opengl.GLBufferStorage mapNamedBufferRangeEXT(int p0, long p1, long p2, int p3) {
    record("mapNamedBufferRangeEXT", new Object[] {p0, p1, p2, p3});
    return null;
  }

  @Override public void setSwapInterval(int p0) {
    record("setSwapInterval", new Object[] {p0});
  }

}
