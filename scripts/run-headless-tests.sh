#!/usr/bin/env bash
#
# Single source of truth for running the Alice test suite the same way CI does:
# headless, and with maximum process-level parallelism.
#
# Used by both the local git hooks (hooks/pre-commit) and, where desired, CI, so
# the two cannot drift apart. The pre-commit gate exists because the historical
# local hook only ran Checkstyle -- no tests -- which let headless-only failures
# reach CI unnoticed.
#
# Environment mirroring:
#   - Forces -Djava.awt.headless=true so GraphicsEnvironment.isHeadless() matches
#     the CI runner even on a developer machine that has a display attached.
#
# Parallelism (maximum wall-clock throughput while still mirroring CI):
#   - mvn -T 1C            : build/test reactor modules in parallel (1 thread/core).
#   - surefire.forkCount=1C: up to one test JVM per core (process-level parallelism).
#   - surefire.reuseForks=true: each fork JVM is reused across the test classes it
#     runs. This is what CI does, so the gate reproduces CI behavior -- including
#     cross-test shared-static state bugs (e.g. static-initializer poisoning), which
#     is the bug class that first motivated this gate.
#
#     Measured on core/ide (~70% of all tests, 11067 tests): reuseForks=true is
#     ~2.7x FASTER wall-clock than reuseForks=false, because these tests are very
#     fine-grained and a fresh JVM per class is dominated by JVM/classload startup.
#     reuseForks=false ALSO hides the static-poisoning bugs CI catches, so it is
#     wrong for a CI-mirroring gate on both counts. Override only if you have a
#     specific isolation need (MAVEN_REUSE_FORKS=false).
#
# Usage:
#   scripts/run-headless-tests.sh                 # full reactor (default)
#   scripts/run-headless-tests.sh -pl core/ide -am  # scope to a module + deps
#   Any extra arguments are passed straight through to Maven.
#
# Overrides (environment variables):
#   MAVEN_FORK_COUNT     default 1C    (surefire.forkCount)
#   MAVEN_REUSE_FORKS    default true  (surefire.reuseForks; mirrors CI)
#   MAVEN_REACTOR_THREADS default 1C   (mvn -T value)
#   SKIP_SUBMODULE_INIT  set to 1 to skip the tweedle-lang submodule init
#
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

FORK_COUNT="${MAVEN_FORK_COUNT:-1C}"
REUSE_FORKS="${MAVEN_REUSE_FORKS:-true}"
REACTOR_THREADS="${MAVEN_REACTOR_THREADS:-1C}"

# Repo guardrail: the Tweedle grammar submodule must be initialized before broad
# Maven validation, otherwise generated parser classes are missing.
if [[ "${SKIP_SUBMODULE_INIT:-0}" != "1" ]]; then
  if [[ ! -d tweedle-lang/Grammar ]]; then
    echo "[run-headless-tests] Initializing tweedle-lang submodule..." >&2
    git submodule update --init tweedle-lang
  fi
fi

MVN_ARGS=(
  -T "${REACTOR_THREADS}"
  -Djava.awt.headless=true
  -Dsurefire.forkCount="${FORK_COUNT}"
  -Dsurefire.reuseForks="${REUSE_FORKS}"
  -Dcheckstyle.skip
  -Dinstall4j.skip
)

# Mirror CI dependency resolution (jogamp mirrors) when the settings file exists.
CI_SETTINGS=".github/maven/jogamp-ci-settings.xml"
if [[ -f "${CI_SETTINGS}" ]]; then
  MVN_ARGS+=(--settings "${CI_SETTINGS}")
fi

echo "[run-headless-tests] mvn ${MVN_ARGS[*]} test $*" >&2
exec mvn "${MVN_ARGS[@]}" test "$@"
