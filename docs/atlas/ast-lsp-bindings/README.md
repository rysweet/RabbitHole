# AST + LSP Bindings

This layer is built from static package ownership plus Java import scanning across `core/*/src/main/java` and `netbeans/src/main/java`.
The approximation intentionally ignores JDK imports and avoids over-claiming where shared namespaces (especially `edu.cmu.cs.*`) span multiple modules.

## Static-approximation notes

- `core/util` is the lowest common package foundation for most other modules.
- `core/ide` is the heaviest cross-module consumer: it imports AST, Croquet, rendering, story API, Tweedle manifests, and model-loading entry points.
- `alice-ide/` is intentionally omitted from the main graph because the requested scan focused on `core/*` plus `netbeans`; it acts as a thin launcher over `core/ide`.

## Mermaid

```mermaid
flowchart LR
  util["core/util<br/>foundation packages"]
  tweedle["core/tweedle<br/>org.alice.tweedle.*"]
  ast["core/ast<br/>org.lgna.project.*"]
  scenegraph["core/scenegraph<br/>render + scenegraph API"]
  glrender["core/glrender<br/>render.gl.*"]
  storyapi["core/story-api<br/>org.lgna.story.*<br/>plus Jama.*"]
  croquet["core/croquet<br/>org.lgna.croquet.*"]
  ide["core/ide<br/>org.alice.ide.*<br/>org.alice.stageide.*"]
  modelloading["core/model-loading<br/>org.lgna.project.io.*"]
  netbeans["netbeans<br/>org.alice.netbeans.*"]

  ast --> util
  ast --> tweedle
  scenegraph --> util
  glrender --> scenegraph
  glrender --> util
  storyapi --> ast
  storyapi --> glrender
  storyapi --> scenegraph
  storyapi --> tweedle
  storyapi --> util
  croquet --> util
  ide --> ast
  ide --> croquet
  ide --> glrender
  ide --> modelloading
  ide --> scenegraph
  ide --> storyapi
  ide --> tweedle
  ide --> util
  modelloading --> ast
  modelloading --> glrender
  modelloading --> scenegraph
  modelloading --> storyapi
  modelloading --> tweedle
  modelloading --> util
  netbeans --> ast
  netbeans --> modelloading
  netbeans --> storyapi
  netbeans --> util
```

Source: [`ast-lsp-bindings.mmd`](./ast-lsp-bindings.mmd)

## Graphviz DOT

```dot
digraph ast_lsp_bindings {
  rankdir=LR;
  graph [fontname="Helvetica", labelloc=t, label="AST/LSP bindings (static approximation)"];
  node [shape=record, style="rounded,filled", fillcolor="#F8F9FA", color="#34495E", fontname="Helvetica"];
  edge [color="#5D6D7E", arrowsize=0.8];

  util [label="{core/util|foundation packages}"];
  tweedle [label="{core/tweedle|org.alice.tweedle.*}", fillcolor="#FCF3CF"];
  ast [label="{core/ast|org.lgna.project.*}", fillcolor="#D6EAF8"];
  scenegraph [label="{core/scenegraph|render + scenegraph API}"];
  glrender [label="{core/glrender|render.gl.*}"];
  storyapi [label="{core/story-api|org.lgna.story.* | bundled Jama.*}", fillcolor="#D5F5E3"];
  croquet [label="{core/croquet|org.lgna.croquet.*}"];
  ide [label="{core/ide|org.alice.ide.* | org.alice.stageide.*}", fillcolor="#FADBD8"];
  modelloading [label="{core/model-loading|org.lgna.project.io.*}"];
  netbeans [label="{netbeans|org.alice.netbeans.*}"];

  ast -> util;
  ast -> tweedle;
  scenegraph -> util;
  glrender -> scenegraph;
  glrender -> util;
  storyapi -> ast;
  storyapi -> glrender;
  storyapi -> scenegraph;
  storyapi -> tweedle;
  storyapi -> util;
  croquet -> util;
  ide -> ast;
  ide -> croquet;
  ide -> glrender;
  ide -> modelloading;
  ide -> scenegraph;
  ide -> storyapi;
  ide -> tweedle;
  ide -> util;
  modelloading -> ast;
  modelloading -> glrender;
  modelloading -> scenegraph;
  modelloading -> storyapi;
  modelloading -> tweedle;
  modelloading -> util;
  netbeans -> ast;
  netbeans -> modelloading;
  netbeans -> storyapi;
  netbeans -> util;
}
```

Source: [`ast-lsp-bindings.dot`](./ast-lsp-bindings.dot)
