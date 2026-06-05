# GlResourceCache - GL Resource Lifecycle Delegate

`GlResourceCache` is the package-private delegate that keeps GL resource
lifecycle management focused inside the render implementation package.

## Design

`GlResourceCache` owns four mutable collections tracking OpenGL resources
for a single `RenderContext`:

| Field                       | Type                                    | Purpose                          |
| --------------------------- | --------------------------------------- | -------------------------------- |
| `displayListMap`            | `Map<GlrGeometry<?>, Integer>`          | Geometry → GL display-list IDs   |
| `textureBindingMap`         | `Map<GlrTexture<?>, ForgettableBinding>`| Texture → GL texture bindings    |
| `toBeForgottenDisplayLists` | `CopyOnWriteArrayList<Integer>`         | Deferred-deletion queue          |
| `toBeForgottenTextures`     | `CopyOnWriteArrayList<ForgettableBinding>` | Deferred-deletion queue       |

Plus the **static** `unusedTexturesListeners` list and `clearUnusedTextures` broadcast.

**Key constraints:**
- **Package-private** — only `RenderContext` instantiates it.
- **`UnusedTexturesListener` stays in `RenderContext`** — public nested interface; moving it would break external references.
- **Stateless w.r.t. GL** — never stores a `GL2` reference; receives the owning `RenderContext` as a parameter.
- **Identical synchronization** — same lock objects, same ordering, no new locks.

**Note:** `textureBindingMap` is never populated because the only historical
`put()` site remains disabled. Preserve that behavior unless a rendering
contract explicitly changes it.

## Listener removal contract

`addUnusedTexturesListener` registers a listener and
`removeUnusedTexturesListener` removes that same listener. Keep the methods
symmetrical so callers can manage listener lifetimes without leaking callbacks.

## Concurrency Model

Identical to original — no lock objects change, no ordering changes.

| Operation                    | Lock held                                     |
| ---------------------------- | --------------------------------------------- |
| `getDisplayListID`           | `synchronized(displayListMap)`                |
| `generateDisplayListID`      | `synchronized(displayListMap)` (map.put only) |
| `forgetGeometryAdapter`      | `synchronized(displayListMap)`                |
| `forgetTextureAdapter`       | `synchronized(textureBindingMap)`             |
| `actuallyForgetDisplayLists` | `synchronized(toBeForgottenDisplayLists)`     |
| `actuallyForgetTextures`     | `synchronized(toBeForgottenTextures)`         |
| Listener add/remove          | None (CopyOnWriteArrayList)                   |

**Reentrant lock:** `forgetAllGeometryAdapters` holds `displayListMap` lock
and calls `forgetGeometryAdapter` which re-acquires it. Java `synchronized`
is reentrant — do not refactor into a lock-free helper.

## Package layout

```
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/
├── Context.java
├── GlResourceCache.java
├── RenderContext.java
├── PickContext.java
└── RenderTargetImp.java
```

Callers (`GlrGeometry`, `GlrTexture`, `RenderTargetImp`, `GlrRenderTarget`)
use `RenderContext`'s unchanged public API — forwarding wrappers are invisible.
