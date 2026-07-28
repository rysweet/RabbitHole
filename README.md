# Alice 3

[Alice](https://www.alice.org) is an innovative block-based programming environment that makes it easy to create animations, build interactive narratives, or program simple games in 3D.

### Latest Released Build:

[![](https://img.shields.io/badge/3.9.1.0-green.svg)](https://www.alice.org/get-alice/alice-3/)

# Alice Project Feedback Survey
Thank you for visiting! We are gathering feedback on our latest builds. 
Please take 5 minutes to fill out our [Feedback Questionnaire](https://forms.gle/qNyV8VVZj9nvVtV56).

## Building Alice 3 from the source

Download and install the following build tools
* [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
  * Set $JAVA_HOME accordingly, and add $JAVA_HOME/bin to your PATH
* [Apache Maven](https://maven.apache.org/install.html) 3.9.9 or later
* git
* [git-lfs](https://help.github.com/en/articles/installing-git-large-file-storage)
* [Install4J 10](https://www.ej-technologies.com/products/install4j/overview.html) (Only required to build the installers)

---

Clone this RabbitHole repository into a local directory, `${alice3}`

    cd ${alice3}
    git clone --recurse-submodules https://github.com/rysweet/RabbitHole.git
    
Alice 3 uses a submodule for the Tweedle language, the internal representation of Alice code.
If you do not use the `--recurse-submodules` flag above it can be pulled in explicitly.

    git submodule init
    git submodule update

Quick diagnostic for worktrees or checkouts that were cloned without submodules:

    git submodule status tweedle-lang
    test -d tweedle-lang/Grammar && echo "tweedle grammar present"

If the grammar directory is missing, run:

    git submodule update --init tweedle-lang

Maven builds that reach `core/tweedle` require `tweedle-lang/Grammar/TweedleLexer.g4` and
`tweedle-lang/Grammar/TweedleParser.g4`. Missing generated Tweedle parser classes usually
mean the submodule was not initialized in the current checkout or worktree.

To ensure the lfs files are available locally:

    git lfs pull 

Compile the code, build the jars, and install them in the local mvn repository.

    mvn compile install

The install step will also build the NetBeans plugin in `{alice3}/netbeans/target/`

If you want to use Install4J to build the installers add a flag to use the buildInstaller profile:

    mvn -DbuildInstaller=true install

More information about configuring Install4j can be found [here](https://www.ej-technologies.com/resources/install4j/help/doc/cli/maven.html)

## Executing and testing

The Getting Started validator checks that this checkout follows the documented
setup path. Run it from the repository root:

    ./scripts/validate-getting-started.sh

The same validator is exposed through the neutral Alice QA wrapper for
outside-in validation of the checkout under review:

    python3 alice_qa.py getting-started validate --headless

The CI-safe headless lane checks CLI/docs-safe launch behavior with
`java.awt.headless=true`: Git checkout state, the initialized `tweedle-lang`
grammar submodule, the documented open-asset Maven install command, and the
default Alice launch command up to the expected GUI-required boundary. The headed
Ubuntu Xvfb lane checks GUI/display-dependent behavior on Ubuntu without a
physical display by running the GUI lane through
`scripts/validate-gui-with-xvfb.sh`, backed by the shared Xvfb action, with
`java.awt.headless=false` and a bounded startup timeout.

On a desktop with a real graphical environment, run the same GUI lane explicitly:

    ./scripts/validate-getting-started.sh --gui

Use `--all` to run the headless lane and attempt the GUI lane when the platform
supports it. macOS Apple Silicon desktop GUI launch is treated as a known
platform blocker, so explicit `--gui` validation exits non-zero with a blocked
result on that platform. `--all` reports the blocked GUI lane without failing
after headless validation passes. See [Getting started](docs/getting-started.md#validate-this-checkout)
and [Testing](docs/testing.md#getting-started-validation-lanes) for the
headless validation lane, headed Xvfb validation lane, skip rules, and failure
semantics.

After successfully compiling and installing the Alice jars into the mvn
repository, you can launch the Alice IDE.

    cd alice-ide
    mvn exec:java -Dalice-ide                         # default open assets
    mvn -DincludeSims=true exec:java -Dalice-ide       # optional Sims assets

Run unit tests

    cd ${alice3}
    mvn test

Generate open-asset aggregate and per-module coverage reports:

    cd ${alice3}
    mvn -Dinstall4j.skip -Pcoverage verify
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

The aggregate HTML report is written to `coverage-report/target/site/jacoco-aggregate/index.html`.
Per-module HTML reports are written under each module's `target/site/jacoco/index.html` when JaCoCo
produces module-level data. CI uploads those reports, `coverage-summary.md`, and
`coverage-evidence-manifest.json` as the `alice-coverage-evidence-open-assets` artifact for pull
requests. CI enforces an 8.0% aggregate open-asset line coverage floor plus conservative module floors
for covered modernization areas; raise them as characterization coverage grows toward the 70%
mission target. The 70% target is claimable only from measured aggregate JaCoCo data, not from
module-only evidence.

Outside-in desktop acceptance scenarios live in `qa/outside-in/alice-desktop/`. See the [documentation index](docs/index.md) for usage, scenario schema, evidence expectations, and configuration.

    qa/outside-in/alice-desktop/runners/validate-scenarios.sh
    qa/outside-in/alice-desktop/runners/run-scenario.sh list

### Runtime issue reporting credentials

Direct JIRA issue submission does not embed credentials in source. Configure deployment or
local runtime with either VM properties:

    -Dalice.jira.username=<username> -Dalice.jira.password=<password>

or environment variables:

    ALICE_JIRA_USERNAME=<username>
    ALICE_JIRA_PASSWORD=<password>

If credentials are absent, Alice reports the configuration problem in the issue submission
progress and leaves the application running instead of crashing.

## Documentation

Repository documentation starts at [docs/index.md](docs/index.md).

## Installing Git Hooks

The hooks directory contains Git hooks, that should be placed in .git/hooks

Here's an example command to copy the pre-push hook:

    cp hooks/pre-push .git/hooks/pre-push

The `pre-commit` hook runs the test suite (headless, in parallel) before each
commit so headless-only failures are caught locally instead of in CI:

    cp hooks/pre-commit .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit

It delegates to `scripts/run-headless-tests.sh`; bypass it for work-in-progress
commits with `git commit --no-verify`.

## IDE
**IntelliJ IDEA** is suggested for coding/building Alice 3. There is a free community edition
or JetBrains offers a product pack for students using an edu email address [here](https://www.jetbrains.com/community/education/#students
).

### Build with IntelliJ

**Alice 3 builds with Java 21** which should be installed and selected in place of IntelliJ's bundled JDK.

IntelliJ IDEA has a bundled maven.
* The location is set in:
  * *File -> Settings -> Build, Execution, Deployment -> Build Tools -> Maven*
* To build with maven using **build project** in the IDE first check the "Delegate IDE build/run actions to Maven" option in
  * *File -> Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner*

### Run with IntelliJ
After the project is built successfully. You can create a Run/Debug Configuration and run the project.
In the **Run/Debug Configurations** window, create a new **Application** configuration and enter lines as bellow.
![New Application Configuration](docs/images/IDELaunchAlice.png)
The working directory should be the root directory where you checked the Alice 3 project out, `${alice3}`.

The VM options are:

    -ea
    -splash:"./installer/installerFiles/SplashScreen.png"
    -Xmx1024m
    -Dswing.aatext=true
    -Dorg.alice.ide.rootDirectory="./core/resources/target/distribution"
    -Dcom.apple.mrj.application.apple.menu.about.name=Alice3
    -Dedu.cmu.cs.dennisc.java.util.logging.Logger.Level=WARNING
    -Dorg.alice.ide.internalTesting=true
    -Dorg.lgna.croquet.Element.isIdCheckDesired=true
    -Djogamp.gluegen.UseTempJarCache=false
    -Dorg.alice.stageide.isCrashDetectionDesired=false
    -Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider
    --add-opens=java.base/java.io=ALL-UNNAMED
    --add-opens=java.desktop/sun.awt=ALL-UNNAMED
    --add-opens=java.base/java.time=ALL-UNNAMED

Then the project should be ready to run.

Final note: If you were delegating IDE build/run actions to Maven in the previous step, please uncheck that option. Or you might run into some graphics errors running Alice.

## Open assets by default and optional Sims assets

RabbitHole defaults to the redistributable open 3D asset path. A fresh compile,
package, install, or IDE launch does not require Sims/nonfree artifacts.

    cd ${alice3}
    mvn -Dinstall4j.skip clean package
Or:

    cd ${alice3}
    mvn clean install

To opt into the legacy Sims assets, pass `-DincludeSims=true` during build and
launch:

    cd alice-ide
    mvn -DincludeSims=true exec:java -Dalice-ide

`alice-ide/pom.xml` declares its dependency on `org.alice.nonfree:ide-nonfree`
inside the same `includeSims` profile (whose activation is
`<value>true</value>`). If the launch command omits the flag, Maven keeps the
open-asset default and does not resolve `ide-nonfree:9.1.0-SNAPSHOT`.

## How to contribute

We appreciate contributions from the Alice community.

To make it easier to merge in new work, when submitting PRs please:
* Keep each one small and focused
* Make individual commits of smaller chunks with clear descriptions
* Follow the established coding style

### Development tools

This repository previously included a number of projects that were experiments, test beds, and development aids.

They have been relocated to the [Alice 3 Tools](https://github.com/TheAliceProject/alice3-tools) repo.
(NB If you are looking for historic versions of any of those projects, the richer git history is in this repo. The history did not migrate due to the way filter-branch was applied.)
