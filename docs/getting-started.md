# Getting started

Use this guide to get RabbitHole building on a fresh machine.

## Contents

- [What you need](#what-you-need)
- [Clone the repository](#clone-the-repository)
- [Validate this checkout](#validate-this-checkout)
- [Build the project](#build-the-project)
- [Run tests](#run-tests)
- [Launch Alice](#launch-alice)
- [Coverage and quick diagnostics](#coverage-and-quick-diagnostics)
- [Everyday command list](#everyday-command-list)

## What you need

- Java 21
- Maven 3.9.9 or later
- Git
- Git LFS
- The `tweedle-lang` submodule
- Optional: Install4J 10 if you need installer builds

## Clone the repository

```bash
git clone --recurse-submodules https://github.com/rysweet/RabbitHole.git
cd RabbitHole
git submodule update --init tweedle-lang
git lfs pull
```

If you already cloned the repository without submodules, run this check before
you build:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar && echo "tweedle grammar present"
```

If the grammar directory is missing, initialize the submodule again:

```bash
git submodule update --init tweedle-lang
```

## Validate this checkout

RabbitHole includes an executable Getting Started validator that checks the
same setup path this guide documents:

```bash
./scripts/validate-getting-started.sh
```

Run it from anywhere inside the repository. The script resolves the Git root,
then verifies that the checkout has Git metadata, the required build tools, and
an initialized Tweedle grammar submodule. It does not create a second clone;
fresh-clone coverage comes from CI checkouts and local users running the
validator immediately after cloning.

The validator fails before Maven starts when `tweedle-lang` is missing,
uninitialized, or missing `tweedle-lang/Grammar`:

```text
tweedle-lang submodule is not initialized.
Run: git submodule update --init tweedle-lang
```

### Validation commands

| Command | Use it for | Behavior |
| --- | --- | --- |
| `./scripts/validate-getting-started.sh` | Default local or CI validation | Runs the headless lane. |
| `./scripts/validate-getting-started.sh --headless` | Explicit CI-safe validation | Same as the default lane. |
| `./scripts/validate-getting-started.sh --gui` | GUI validation | Runs the documented no-Sims GUI launch path with `java.awt.headless=false`. Requires either a real desktop display or an Xvfb display such as the headed Ubuntu CI lane. Exits non-zero if no GUI is available or the platform is blocked. |
| `./scripts/validate-getting-started.sh --all` | Local full validation | Runs headless validation, then runs GUI validation only when supported; unsupported GUI lanes are reported as skipped or blocked without failing after headless validation passes. |
| `./scripts/validate-getting-started.sh --help` | Usage reference | Prints supported flags and exits. |

For outside-in validation through the neutral Alice QA wrapper, run the same
checked-out validator from the repository root or one of its child directories:

```bash
python3 alice_qa.py getting-started validate --headless
```

Unknown flags are rejected with exit code 2 so CI does not accidentally run the
wrong lane.

### Headless lane

The headless lane is CI-safe and verifies CLI/docs-safe launch behavior with
`java.awt.headless=true`. It checks the documented no-Sims install path with the
same Maven flags users should run on machines without Sims assets or a desktop
display:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean install
```

It then probes the documented no-Sims launch command:

```bash
cd alice-ide
mvn -DincludeSims=false exec:java -Dalice-ide
```

For the headless validation probe, the script adds `-Djava.awt.headless=true`
to the same Maven launch path so local desktops do not open the IDE during the
headless lane:

```bash
cd alice-ide
mvn -DincludeSims=false -Djava.awt.headless=true exec:java -Dalice-ide
```

The launch probe is successful only when Alice stops at the expected desktop
boundary and reports:

```text
Alice desktop launch requires a graphical environment.
```

Any other launch result is a failure because it means the Getting Started
instructions no longer match the application behavior. This lane intentionally
does not prove that Ubuntu GUI/display-dependent behavior works; it proves that
the documented launch command fails safely and predictably when Java AWT is
headless.

### GUI lane

The GUI lane runs the same no-Sims launch path on a machine that can provide a
Java AWT display:

```bash
./scripts/validate-getting-started.sh --gui
```

Use this lane from a local desktop session with Java AWT display support.
Examples include a Linux desktop with a usable `DISPLAY` or Wayland bridge, a
Windows desktop session, or an Intel macOS desktop session. The headed Ubuntu
Xvfb CI lane runs this same GUI lane on an Xvfb-backed display and launches
with `-Djava.awt.headless=false` so it cannot silently fall back to headless
behavior.

If there is no graphical environment, explicit `--gui` must exit non-zero with
a clear message because the caller requested GUI validation. In `--all`, the
same unavailable GUI environment is reported as a skip after the headless lane
passes, and the command exits successfully.

### Headed Ubuntu Xvfb CI lane

The headed Ubuntu Xvfb workflow job is the CI version of the GUI lane. It uses
the shared Xvfb setup action to install Ubuntu's Xvfb tooling, resolve
`xvfb-run` to an absolute path exposed as an action output, and fail before
validation starts if `xvfb-run` is unavailable.

The job must remain separate from the headless validation path because an
Xvfb-wrapped `--headless` run still uses `java.awt.headless=true` and is not
equivalent to a headed GUI launch. The headed job runs the Getting Started GUI
validator through the reusable Xvfb harness with an explicit bounded startup
timeout, using the absolute `xvfb-run` path provided by the shared action. In
the workflow `run` block, that looks like this:

```bash
RABBITHOLE_LAUNCH_TIMEOUT_SECONDS=60 \
scripts/validate-gui-with-xvfb.sh \
  --timeout-seconds "${RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS:-7200}" \
  --expect success \
  --xvfb-run "${xvfb_run}" \
  -- \
  scripts/validate-getting-started.sh --gui
```

The workflow uses the shared action output, not a hard-coded filesystem path.
The validator first installs no-Sims artifacts with `java.awt.headless=false`
and tests skipped, then runs the GUI launch probe. The validator's
`RABBITHOLE_LAUNCH_TIMEOUT_SECONDS=60` setting keeps the GUI startup probe
bounded, while the harness timeout is a larger whole-command deadline so
dependency downloads or no-Sims installation do not consume the launch window. A
hung Alice startup is a CI failure, not a skipped GUI validation.

### macOS Apple Silicon GUI blocker

Desktop GUI launch on macOS Apple Silicon is treated as a known platform
blocker. The validator detects that platform and reports the GUI lane as
blocked. Explicit `--gui` must exit non-zero with that blocked result. `--all`
must report the blocked GUI lane and still exit successfully after headless
validation passes. This is not a Getting Started setup error, and this
validation feature does not fix the GUI blocker.

## Build the project

Build every Maven module and install the artifacts in your local Maven cache:

```bash
mvn compile install
```

If you want the no-Sims path used by the headless validation lane:

```bash
mvn -DincludeSims=false -Dinstall4j.skip clean install
```

## Run tests

Run the full test suite:

```bash
mvn test
```

Run the no-Sims, headless-friendly install lane used in CI:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean install
```

Run Checkstyle on the whole reactor:

```bash
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml
```

## Launch Alice

After a successful build, you can start the desktop IDE from the `alice-ide`
module:

```bash
cd alice-ide
mvn exec:java -Dalice-ide
```

If you built or installed with `-DincludeSims=false`, pass the same flag when
launching:

```bash
cd alice-ide
mvn -DincludeSims=false exec:java -Dalice-ide
```

Without that flag, Maven re-activates the Sims profile and tries to resolve the
nonfree Sims dependency that the no-Sims build intentionally skipped.

## Coverage and quick diagnostics

Generate the no-Sims JaCoCo report used by the modernization coverage lane:

```bash
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmaven.test.failure.ignore=true -Dmdep.skip=true -Pcoverage verify
python3 scripts/summarize-jacoco-coverage.py \
  --output coverage-summary.md \
  --evidence-manifest coverage-evidence-manifest.json \
  --target-aggregate-line-percent 70.0 \
  --min-aggregate-line-percent 8.0 \
  --min-module-line-percent core/ast=18.0 \
  --min-module-line-percent core/model-loading=10.0 \
  --min-module-line-percent core/story-api-migration=75.0 \
  --min-module-line-percent core/tweedle=50.0 \
  --min-module-line-percent core/scenegraph=10.0 \
  --min-module-line-percent netbeans=25.0
```

Key output files:

- `coverage-report/target/site/jacoco-aggregate/index.html`
- `coverage-summary.md`
- `coverage-evidence-manifest.json`

## Everyday command list

| Task | Command |
| --- | --- |
| Build everything | `mvn compile install` |
| Run all tests | `mvn test` |
| Run CI-like headless tests | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean test` |
| Preview GUI validation with local Xvfb | `RABBITHOLE_LAUNCH_TIMEOUT_SECONDS=60 scripts/validate-gui-with-xvfb.sh --timeout-seconds "${RABBITHOLE_XVFB_VALIDATION_TIMEOUT_SECONDS:-7200}" --expect success -- ./scripts/validate-getting-started.sh --gui` |
| Run Checkstyle | `mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml` |
| Generate coverage | `mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Dmaven.test.failure.ignore=true -Dmdep.skip=true -Pcoverage verify` |
| Start the IDE after a full build | `cd alice-ide && mvn exec:java -Dalice-ide` |
| Start the IDE after a no-Sims build | `cd alice-ide && mvn -DincludeSims=false exec:java -Dalice-ide` |
| Validate Getting Started, headless | `./scripts/validate-getting-started.sh --headless` |
| Validate Getting Started, GUI | `./scripts/validate-getting-started.sh --gui` |
