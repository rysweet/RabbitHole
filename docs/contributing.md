# Contributing

Keep changes small, tested, and easy to review.

## Basic process

1. Start from a fresh branch.
2. If you are changing behavior, add or update characterization tests first.
3. Make one focused change at a time.
4. Run local validation before you ask for review.
5. Send the change through your normal team or COE review path before merge.

## Install the local pre-push hook

RabbitHole keeps a `pre-push` hook in `hooks/`.

```bash
cp hooks/pre-push .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

That hook runs Git LFS checks and Maven Checkstyle before a push.

## Required checks

Run these from the repository root:

```bash
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true clean test
```

Use targeted module tests when you are iterating, then finish with the broader
validation lane before review.

## Checkstyle rules

Checkstyle is part of the normal contribution flow. The repository-wide config
lives in `checkstyle.xml`, and CI runs the same command used by the pre-push
hook.

## Module size guidance

Modernization work favors small helpers over giant classes. Many refactoring
docs and contract tests use a **sub-500-line target** for extracted helpers so
one file does not carry too many responsibilities.

A good rule of thumb:

- keep new helpers focused on one job
- split large files once they become hard to test or review
- prefer another small package-private helper over adding one more mixed concern

## Test expectations

- Add tests for any behavior change.
- Prefer headless-safe tests when possible.
- Use explicit `Assume` guards for display-dependent tests.
- Do not widen a change without widening the matching docs and test coverage.

## Documentation expectations

Put contributor-facing documentation in `docs/` and link it from
`docs/index.md`. If your change alters a workflow, command, or contract,
update the matching docs in the same branch.

Durable docs should describe the supported RabbitHole behavior, not the branch
or local investigation that produced it. Keep generated evidence, scratch
reports, local tool traces, and time-bound progress notes out of Git unless the
content has been rewritten as a maintained reference. See
[Repository hygiene](repository-hygiene.md) for the cleanup rules and checks.

## Merge-ready evidence

Before moving a reviewed pull request out of draft, generate the maintained
merge-ready evidence section:

```bash
python3 scripts/generate-merge-ready-evidence.py \
  --pr <pull-request-number> \
  --scenario-directory <scenario-directory> \
  --quality-audit-file <quality-audit-output> \
  --patch-pr-description
```

Use `--dry-run` to preview the generated section without editing the pull
request. The command prints evidence to standard output and updates the pull
request description when patching is enabled; do not commit generated evidence
files or local command transcripts.
