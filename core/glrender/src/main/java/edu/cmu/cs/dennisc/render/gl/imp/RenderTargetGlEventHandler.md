# RenderTargetGlEventHandler - GL Event Listener Delegate

`RenderTargetGlEventHandler` is the package-private delegate that owns JOGL
event callbacks for `RenderTargetImp`.

## Design

`RenderTargetGlEventHandler` implements `GLEventListener` and owns the
lifecycle callbacks that JOGL invokes on the GL thread. It delegates back to
`RenderTargetImp` for rendering, event firing, and state queries.

### Responsibilities

| Callback       | What it does                                                         |
| -------------- | -------------------------------------------------------------------- |
| `init`         | Obtains `GL2`, updates conformance test, sets debug GL, stores dimensions, fires `RenderTargetInitializeEvent` |
| `display`      | Lazy-initializes if needed, corrects zero-sized drawables, sets GL on render context, calls `performRender()` |
| `reshape`      | Stores new drawable and screen dimensions, fires `RenderTargetResizeEvent` |
| `dispose`      | Logs via `Logger.todo` (placeholder — matches original behavior)     |

### What stays in RenderTargetImp

| Concern                    | Methods                                                              |
| -------------------------- | -------------------------------------------------------------------- |
| Camera management          | `addSgCamera`, `removeSgCamera`, `clearSgCameras`, `getSgCamera*`, `getCameraAtAwtPoint` |
| Render pipeline            | `performRender()` (private)                                          |
| Listener dispatch          | `fireInitialized`, `fireCleared`, `fireRendered`, `fireResized`, `fireDisplayChanged` |
| Buffer capture             | `createBufferedImageForUseAsColorBuffer*`, `getColorBuffer*`, `createFloatBufferForUseAsDepthBuffer` |
| Display tasks              | `addDisplayTask`                                                     |
| Texture management         | `forgetAllCachedItems`, `clearUnusedTextures`                        |
| Public accessors           | `getRenderTarget`, `getSynchronous*`, `getAsynchronous*`             |
| Listener registration      | `addRenderTargetListener`, `removeRenderTargetListener`              |

## Field Access

The handler needs access to several `RenderTargetImp` fields. These are
widened from `private` to package-private:

| Field                                     | Type                                  | Accessed by handler for         |
| ----------------------------------------- | ------------------------------------- | ------------------------------- |
| `renderContext`                            | `RenderContext`                        | GL setup, null-check            |
| `drawable`                                 | `GLAutoDrawable`                       | Assertion checks                |
| `drawableWidth` / `drawableHeight`         | `int`                                  | Dimension tracking              |
| `screenWidth` / `screenHeight`             | `int`                                  | Screen-size tracking            |
| `isDisplayIgnoredDueToPreviousException`   | `boolean`                              | Not accessed — stays private    |

The handler also calls these package-private methods on `RenderTargetImp`:

| Method                | Purpose                              |
| --------------------- | ------------------------------------ |
| `performRender()`     | Widened from private → package-private |
| `fireInitialized(e)`  | Widened from private → package-private |
| `fireResized(e)`      | Widened from private → package-private |
| `getRenderTarget()`   | Already public                        |

## Concurrency Model

Identical to original — no lock objects change, no ordering changes.

| Operation                | Thread           | Synchronization                       |
| ------------------------ | ---------------- | ------------------------------------- |
| `init` / `display` / `reshape` / `dispose` | GL thread (JOGL) | Single-threaded by JOGL contract |
| `performRender()`        | GL thread         | Called only from `display` callback    |
| `fireInitialized/Resized`| GL thread         | Listeners on `CopyOnWriteArrayList`    |
| `startListening` / `stopListening` | EDT    | Guards `isListening` flag              |

The handler introduces no new threads, locks, or shared state.

## Wiring

```java
final RenderTargetGlEventHandler glEventHandler = new RenderTargetGlEventHandler(this);
```

```java
class RenderTargetGlEventHandler implements GLEventListener {
  private final RenderTargetImp rtImp;

  RenderTargetGlEventHandler(RenderTargetImp rtImp) {
    this.rtImp = rtImp;
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    assert drawable == rtImp.drawable;
    GL2 gl = drawable.getGL().getGL2();
    ConformanceTestResults.SINGLETON.updateRenderInformationIfNecessary(gl);
    final boolean USE_DEBUG_GL = false;
    if (USE_DEBUG_GL && !(gl instanceof DebugGL2)) {
      gl = new DebugGL2(gl);
      Logger.info("using debug gl: ", gl);
      drawable.setGL(gl);
    }
    int w = GlDrawableUtils.getGlDrawableWidth(drawable);
    int h = GlDrawableUtils.getGlDrawableHeight(drawable);
    rtImp.drawableWidth = w;
    rtImp.drawableHeight = h;
    rtImp.screenWidth = GlDrawableUtils.getGLJPanelWidth(drawable);
    rtImp.screenHeight = GlDrawableUtils.getGLJPanelHeight(drawable);
    rtImp.renderContext.setGL(gl);
    rtImp.fireInitialized(new RenderTargetInitializeEvent(rtImp.getRenderTarget(), w, h));
  }

  @Override
  public void display(GLAutoDrawable drawable) {
    assert drawable == rtImp.drawable;
    GL2 gl = drawable.getGL().getGL2();
    if (rtImp.renderContext.gl == null) {
      init(drawable);
      Logger.outln("note: initialize necessary from display");
    }
    if (rtImp.drawableWidth <= 0 || rtImp.drawableHeight <= 0) {
      int nextW = GlDrawableUtils.getGlDrawableWidth(drawable);
      int nextH = GlDrawableUtils.getGlDrawableHeight(drawable);
      int nextSW = GlDrawableUtils.getGLJPanelWidth(drawable);
      int nextSH = GlDrawableUtils.getGLJPanelHeight(drawable);
      if (rtImp.drawableWidth != nextW || rtImp.drawableHeight != nextH) {
        Logger.severe(rtImp.drawableWidth, rtImp.drawableHeight, nextW, nextH);
        rtImp.drawableWidth = nextW;
        rtImp.drawableHeight = nextH;
        rtImp.screenWidth = nextSW;
        rtImp.screenHeight = nextSH;
      }
    }
    rtImp.renderContext.setGL(gl);
    rtImp.performRender();
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    assert drawable == rtImp.drawable;
    rtImp.drawableWidth = width;
    rtImp.drawableHeight = height;
    rtImp.screenWidth = GlDrawableUtils.getGLJPanelWidth(drawable);
    rtImp.screenHeight = GlDrawableUtils.getGLJPanelHeight(drawable);
    rtImp.fireResized(new RenderTargetResizeEvent(rtImp.getRenderTarget(), width, height));
  }

  @Override
  public void dispose(GLAutoDrawable drawable) {
    Logger.todo(drawable);
  }
}
```

### Wiring in RenderTargetImp

The `startListening` and `stopListening` methods register and remove the same
handler instance:

```java
private void startListening(GLAutoDrawable drawable) {
    if (drawable != null) {
        this.isListening = true;
        this.drawable = drawable;
        this.drawable.addGLEventListener(this.glEventHandler);
    }
}

private void stopListening(GLAutoDrawable drawable) {
    // ... same guard logic ...
    drawable.removeGLEventListener(this.glEventHandler);
    // ...
}
```

## Configuration

No new configuration. The `USE_DEBUG_GL` compile-time constant moves
unchanged into the handler's `init()`.

## Package layout

```
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/
├── GlResourceCache.java
├── GlResourceCache.md
├── RenderContext.java
├── RenderTargetGlEventHandler.java
├── RenderTargetGlEventHandler.md
└── RenderTargetImp.java
```

## Invariants

| Invariant | Rationale |
| --- | --- |
| Package-private access only | Only render implementation classes in this package use the widened fields and methods. |
| GL callback behavior stays in JOGL order | `init`, `display`, `reshape`, and `dispose` follow the `GLEventListener` contract. |
| Lazy initialization remains guarded | `display()` initializes through `init()` when `renderContext.gl` is null. |

## Security

- **Fail-closed rendering:** Exception handler in `performRender()` stays in `RenderTargetImp`, still sets `isDisplayIgnoredDueToPreviousException` and rethrows.
- **Thread safety:** No new shared state. JOGL's single-GL-thread contract unchanged.
- **Zero new attack surface:** Package-private class, no reflection, no new I/O.
