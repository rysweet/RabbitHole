# Repository hygiene

RabbitHole documentation describes durable Alice behavior, build contracts, and
contribution workflows. Temporary investigation notes, local evidence, local
tool traces, tool-specific hook bundles, and time-bound progress narratives stay
out of the committed repository unless their content is promoted into a stable
user-facing document.

This contract keeps the repository readable after a branch lands: a reader
should not need to know which branch, pull request, local tool, or investigation
session produced a document in order to use it.

## What belongs in the repository

| Surface | Durable content |
| --- | --- |
| `README.md` | Project overview, build prerequisites, common commands, and links to maintained documentation. |
| `docs/` | User-facing guides, architecture references, stable testing contracts, and maintained API or command references. |
| `qa/outside-in/` | Source-controlled scenario definitions, schemas, runners, and checked-in contracts for Alice outside-in QA. |
| `tests/` | Executable contracts that protect behavior, documentation shape, command surfaces, and hygiene rules. |
| `.github/workflows/` | Repository CI workflows and reusable validation lanes. |
| Scripts and module docs | Maintained commands and module-local references that remain accurate across branches. |

Generated outputs belong outside Git unless they are intentionally small,
reviewed, deterministic fixtures required by tests. Examples include generated
logs, local evidence folders, scratch summaries, branch-specific review notes,
and local command transcripts.

## Documentation rules

Durable documentation uses stable language:

- Describe the supported behavior, command, or contract in present tense.
- Link to maintained docs instead of embedding one-off progress notes.
- Refer to generic issues and pull requests only when explaining the normal
  contribution workflow.
- Avoid concrete tracking numbers, branch names, local paths, date-stamped
  reports, and temporary tool names.
- Replace migration or cleanup narratives with the finished contract they
  produced.
- Keep "how to run it" instructions separate from local evidence created by a
  particular run.

When a document is useful but written as a point-in-time narrative, rewrite it
as a stable reference. When the document only records temporary investigation
state, delete it.

## Repository cleanup workflow

Use this workflow before merging documentation or QA hygiene changes:

1. Inventory tracked files with `git ls-files`.
2. Classify matches as durable documentation, source-controlled QA contract,
   generated output, local tool configuration, or temporary investigation note.
3. Delete generated outputs, local workflow traces, scratch reports, and
   tool-specific hook bundles. Inspect scratch scripts as text; do not execute
   them to decide whether they are durable.
4. Rewrite durable docs so they describe the finished RabbitHole behavior
   without branch-local framing.
5. Rename retained QA tooling to neutral Alice or RabbitHole names.
6. Update `mkdocs.yml`, `docs/index.md`, and README links after deleting or
   renaming pages.
7. Run the documentation hygiene tests and the affected docs or QA checks.

The cleanup is intentionally tracked-file based. Do not run broad destructive
filesystem commands over untracked local work.

## Neutral Alice QA command surface

Repository-owned QA helpers use Alice or RabbitHole names. The neutral wrapper
is `alice_qa.py`; it delegates to maintained scripts, preserves their exit
codes, and does not add a second build system.

```bash
python3 alice_qa.py getting-started validate --headless
python3 alice_qa.py alice-qa validate
python3 alice_qa.py alice-qa run alice-desktop-launch
python3 alice_qa.py alice-qa save-negative-contract
```

Use the direct scripts when you do not need the wrapper:

```bash
./scripts/validate-getting-started.sh --headless
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-launch
```

The wrapper is a convenience entry point for repository-local QA, not a separate
source of truth. Its documented subcommands and tests must be updated in the
same change whenever a delegated runner is added, renamed, or removed.

## Hygiene tests

The documentation hygiene tests enforce the repository contract:

```bash
python3 -m pytest \
  tests/test_point_in_time_docs_removal.py \
  tests/test_alice_qa.py \
  tests/test_alice_qa_docs_contract.py
```

These tests allow ordinary contribution language such as "issues", "pull
requests", "GitHub", "CI", and "review". They reject committed temporary
artifact directories, local tool traces, concrete tracking-number
references in durable docs, stale navigation links, and tool-branded QA names.

For documentation-only changes, also run:

```bash
mkdocs build --strict
```

For outside-in QA documentation or scenario changes, run:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

## Example: rewriting an unstable note

Unstable note:

```text
A local investigation deleted generated output and renamed a QA helper while
checking documentation navigation.
```

Durable documentation:

```text
Generated output is not committed. The desktop QA wrapper is named
`alice_qa.py`, and documentation navigation links only to maintained pages.
```

The durable version says what the repository guarantees after the change. It
does not depend on investigation history, review state, or a local run.

## Configuration

Repository hygiene is enforced through three maintained surfaces:

| Surface | Responsibility |
| --- | --- |
| `.gitignore` | Keeps local evidence, generated logs, assistant workspaces, and QA output out of Git. |
| `tests/test_point_in_time_docs_removal.py` | Rejects non-durable tracked artifacts and point-in-time documentation patterns. |
| `tests/test_alice_qa_docs_contract.py` | Keeps the documented Alice QA command surface aligned with the neutral wrapper and outside-in scripts. |

Update all three together when a new generated-output directory, QA wrapper, or
documentation category is added.
