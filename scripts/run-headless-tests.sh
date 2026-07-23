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
# Parallelism (fastest wall-clock, chosen strategy = process isolation):
#   - mvn -T 1C            : build/test reactor modules in parallel (1 thread/core).
#   - surefire.forkCount=1C: up to one test JVM per core.
#   - surefire.reuseForks=false: a fresh JVM per test class (strong isolation).
#     NOTE: strong isolation maximizes speed but HIDES cross-test shared-static
#     state bugs in this gate. CI runs surefire with its default reused fork, which
#     still surfaces those. Do not rely on this gate to catch static-poisoning bugs.
#
# Usage:
#   scripts/run-headless-tests.sh                 # full reactor (default)
#   scripts/run-headless-tests.sh -pl core/ide -am  # scope to a module + deps
#   Any extra arguments are passed straight through to Maven.
#
# Overrides (environment variables):
#   MAVEN_FORK_COUNT     default 1C   (surefire.forkCount)
#   MAVEN_REACTOR_THREADS default 1C  (mvn -T value)
#   SKIP_SUBMODULE_INIT  set to 1 to skip the tweedle-lang submodule init
#
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

FORK_COUNT="${MAVEN_FORK_COUNT:-1C}"
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
  -Dsurefire.reuseForks=false
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
