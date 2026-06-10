# Architecture

RabbitHole keeps the Alice 3 codebase in separate Maven modules so parser,
runtime, rendering, and desktop UI work can move at different speeds.

## The big picture

Alice has three main layers:

1. **Language and project model** — Tweedle, AST, serialization, and migration.
2. **Runtime and scene model** — Story API, scene graph, model loading, and rendering.
3. **Desktop application** — Croquet UI, IDE workflows, NetBeans packaging, and the launcher.

## Core modules

| Module | What it owns | Why it matters |
| --- | --- | --- |
| `core/ast` | Alice abstract syntax tree classes and code generation helpers | Shared language model used by parsing, editing, and migration |
| `core/tweedle` | Tweedle parser, grammar integration, and language support | Keeps Alice source code readable, editable, and executable |
| `core/story-api` | Student-facing scene and object API | Defines the world model that lessons and projects use |
| `core/story-api-migration` | Project upgrade and compatibility logic | Keeps older projects opening in newer builds |
| `core/scenegraph` | Render-agnostic 3D scene data structures | Holds transforms, cameras, visuals, and geometry |
| `core/glrender` | JOGL-based rendering pipeline | Turns the scene graph into pixels when a real display is available |
| `core/model-loading` | Model import and resource decoding | Loads gallery assets and external model data |
| `core/resources` | Shared distribution resources | Images, starter data, and runtime assets |
| `core/croquet` | UI framework used by the desktop app | Provides actions, views, and interaction state |
| `core/ide` | Main Alice authoring workflows | Scene editor, code editor, save/export, and project actions |
| `core/util` | Shared utility code | Small helpers used across many modules |
| `alice-ide` | Desktop entry point | Assembles the IDE into a runnable application |
| `netbeans` | Packaged NetBeans plugin and distribution glue | Produces the packaged desktop product |
| `external/*` | Third-party libraries vendored into the reactor | Keeps key dependencies versioned with the build |

## How the modules relate

A typical flow looks like this:

```text
tweedle-lang grammar
  ↓
core/tweedle
  ↓
core/ast
  ↓
core/story-api and core/story-api-migration
  ↓
core/scenegraph and core/model-loading
  ↓
core/glrender
  ↓
core/croquet and core/ide
  ↓
alice-ide and netbeans
```

In practice, many modules share utilities and resources, but the direction is
mostly the same: language and project data feed runtime state, runtime state
feeds rendering, and the desktop layers sit on top.

## Build system overview

The root `pom.xml` is the reactor entry point. It pins shared versions,
declares Java 21, and lists the modules in build order.

Important build lanes:

| Lane | Command | Use |
| --- | --- | --- |
| Full reactor build | `mvn compile install` | Local development and packaging |
| Headless no-Sims test lane | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean test` | Fast validation without Sims assets |
| Coverage lane | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmaven.test.failure.ignore=true -Dmdep.skip=true -Pcoverage verify` | JaCoCo aggregate and per-module reports |
| NetBeans package lane | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -pl netbeans -am package -DskipTests` | Packaging checks |

## Why the modernization keeps splitting code

The modernization work tries to keep new seams small and reviewable. Large
legacy classes are usually characterized first, then split into helpers so one
file does not have to carry parsing, state management, rendering, and UI code
at the same time.
