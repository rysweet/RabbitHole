# JavaFX Xvfb Launcher Reference

This document describes the JavaFX Xvfb launcher contract for Alice JavaFX tests
and packaged-launcher checks. The command-line examples, shared Java test
helper, and static characterization coverage use the same resilient prefix.

## Command contract

Maintained Markdown examples and the Java test launcher use this prefix:

```text
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac"
```

The prefix means:

| Argument | Purpose |
| --- | --- |
| `xvfb-run` | Starts the command under a temporary Xvfb display. |
| `--auto-servernum` | Chooses an available display number instead of assuming `:99`. |
| `-s "-screen 0 1024x768x24 -ac"` | Configures the X server with a 1024x768 24-bit screen and disables access control for the temporary test display. |

Keep `-ac` inside the `-s` server-argument string. It is an X server argument,
not an `xvfb-run` option.

Do not add the short `-a` option beside `--auto-servernum`. They request the
same display-number behavior, and the launcher helper intentionally omits the
redundant `-a`.

## Usage

Use the resilient prefix when running tests that bootstrap Swing, JavaFX, or
JOGL code:

```bash
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am test
```

Run coverage verification the same way:

```bash
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false verify
```

Pure headless tests do not require Xvfb, but this wrapper is safe for mixed test
sets where some tests create desktop UI infrastructure.

## Launcher helper API

`ProjectCodeGeneratorStandaloneProjectTest` exposes a package-private,
path-aware helper for test launcher construction:

```java
javaFxXvfbRunPrefix(Path xvfbRun)
```

The helper returns the shared argument prefix as process arguments:

```text
/resolved/path/to/xvfb-run
--auto-servernum
-s
-screen 0 1024x768x24 -ac
```

Accepting `Path xvfbRun` preserves the behavior that resolves the executable
from `PATH` before building the command. The helper normalizes that path instead
of hard-coding the literal `xvfb-run` executable name.

Both JavaFX launcher paths build on this helper:

- `runJarWithJavaFxModulesUnderXvfb(...)` appends the Java executable and JavaFX
  module arguments after the shared prefix.
- `xvfbRunStartsJava(...)` appends the Java executable and preflight arguments
  after the same shared prefix.

Because both paths start from the same helper, the packaged-launcher command and
the preflight command should not drift.

## Configuration

No environment variable is required for the Xvfb launcher contract. Configure the
display through the fixed argument prefix, not through shell aliases or local
defaults.

The launcher command is built as a process argument list instead of a shell
string. Do not add quoting into individual arguments except for interactive shell
examples in documentation.

## Characterization tests

The JavaFX Xvfb characterization tests verify command construction without
requiring a live X server. They assert that:

- the shared prefix starts with the normalized resolved `xvfb-run` path
- `--auto-servernum` is present
- redundant `-a` is absent
- `-s` is present
- the server argument is `-screen 0 1024x768x24 -ac`
- both Java launch paths preserve the shared prefix before appending Java
  arguments

Use these tests when changing launcher construction so command resilience stays
covered without making local test runs depend on Xvfb availability.
