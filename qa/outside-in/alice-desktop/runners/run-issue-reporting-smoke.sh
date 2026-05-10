#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
git submodule update --init tweedle-lang 2>/dev/null
exec mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/issue-reporting -am \
  -Dtest=org.lgna.issue.IssueSubmissionProgressWorkerTest test -q
