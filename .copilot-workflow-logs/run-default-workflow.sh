#!/usr/bin/env bash
set -o pipefail
amplihack recipe run default-workflow \
  -c task_description="Implement the next real UI step after PR #202 for RabbitHole modernization. Prefer a small tested step toward Save-menu completion or live desktop invocation if possible. If blocked, add a concrete implementation/test seam that materially advances completion. Avoid decoder and coverage/hotspot files. Do not overclaim live desktop invocation, desktop edit command completion, Save-menu completion, first-lesson completion, rendering, grading, or full UI automation." \
  -c repo_path=.
