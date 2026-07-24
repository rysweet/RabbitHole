# Automated dependency updates

RabbitHole keeps its pinned dependencies and pinned GitHub Actions current with
[Dependabot version updates][dependabot-docs]. A single configuration file,
`.github/dependabot.yml`, tells Dependabot which ecosystems to scan, how often to
check for new releases, and who reviews the resulting pull requests. This keeps
build inputs from rotting so that security fixes and compatible upgrades surface
on a predictable cadence instead of accumulating silently.

Version updates never merge on their own. Every update arrives as a pull request
that a reviewer approves and that required CI must pass before it lands.

## Configuration file

The configuration lives at `.github/dependabot.yml` and uses Dependabot
configuration schema version 2:

```yaml
version: 2
updates:
  # Maven reactor dependencies at the repository root.
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    reviewers:
      - "rysweet"
    groups:
      maven-minor-patch:
        update-types:
          - "minor"
          - "patch"

  # GitHub Actions used by workflows and the composite setup-xvfb action.
  - package-ecosystem: "github-actions"
    directories:
      - "/"
      - "/.github/actions/setup-xvfb"
    schedule:
      interval: "weekly"
    reviewers:
      - "rysweet"

  # Python (mkdocs) documentation dependencies.
  - package-ecosystem: "pip"
    directory: "/docs"
    schedule:
      interval: "weekly"
    reviewers:
      - "rysweet"
```

## Tracked ecosystems

| Ecosystem | Location | Schedule | Notable settings |
| --- | --- | --- | --- |
| `maven` | `/` | Weekly | Minor and patch updates grouped into a single `maven-minor-patch` pull request; up to 10 open pull requests. |
| `github-actions` | `/` and `/.github/actions/setup-xvfb` | Weekly | Scans workflow `uses:` references and the composite `setup-xvfb` action. |
| `pip` | `/docs` | Weekly | Tracks the mkdocs documentation build dependencies. |

### Maven reactor dependencies

Dependabot scans the Maven reactor from the repository root (`/`) once a week.
Minor and patch upgrades are collected into a single grouped pull request named
`maven-minor-patch` so routine version drift lands as one reviewable change
instead of many. Major upgrades continue to arrive as individual pull requests
because they are more likely to require code changes. Up to 10 update pull
requests may be open at once.

Internal `org.alice` reactor modules use `-SNAPSHOT` versions and are not
external releases, so Dependabot does not propose updates for them.

### GitHub Actions

Dependabot keeps pinned GitHub Actions current in two locations:

- `/` — the actions referenced by workflows under `.github/workflows/`.
- `/.github/actions/setup-xvfb` — the actions referenced by the composite
  `setup-xvfb` action.

Dependabot does not pin actions to commit SHAs on its own. As reviewer guidance,
prefer pinning third-party actions to a commit SHA so the resolved version stays
reproducible; this is a manual review practice, not an enforced Dependabot rule.

### Python documentation dependencies

Dependabot scans `/docs` weekly for the Python packages used to build the mkdocs
documentation site. Until a Python requirements manifest (for example
`docs/requirements.txt`) is present, this block is valid and simply produces no
pull requests; it begins proposing updates automatically once a manifest lands.

## Review workflow

1. Dependabot opens a pull request against `develop` — the repository's default
   branch — when a tracked dependency or action has a newer version. Because
   `develop` is the default branch, no explicit `target-branch` is required; if
   the default branch ever changes, add `target-branch: "develop"` to each
   ecosystem block to keep update pull requests targeting `develop`.
2. The configured reviewer (`rysweet`) is requested on every update pull request.
3. Required CI — including checkstyle, tests, and coverage — must pass before the
   update can merge, acting as the supply-chain quality gate.
4. A human approves and merges. There is no auto-merge for dependency bumps.

## Adjusting the configuration

Edit `.github/dependabot.yml` to change the behavior:

- **Change cadence** — set `schedule.interval` to `daily`, `weekly`, or
  `monthly` per ecosystem.
- **Add an ecosystem** — append a new entry under `updates` with its
  `package-ecosystem` and `directory` (or `directories`).
- **Adjust grouping** — add or edit a key under `groups` to bundle related
  updates into one pull request.
- **Change reviewers** — edit the `reviewers` list for an ecosystem.
- **Limit noise** — tune `open-pull-requests-limit` for an ecosystem.

After editing, validate that the file still parses before committing:

```bash
python3 -c 'import yaml; yaml.safe_load(open(".github/dependabot.yml"))'
```

[dependabot-docs]: https://docs.github.com/code-security/dependabot/dependabot-version-updates/configuration-options-for-the-dependabot.yml-file
