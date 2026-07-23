---
title: Pin and Audit Dependencies
description: How to audit RabbitHole's external dependency sources and pin any mutable reference to an exact, immutable identifier.
last_updated: 2026-07-23
review_schedule: quarterly
owner: modernization
doc_type: howto
related:
  - ../concepts/supply-chain-dependency-pinning.md
  - ../reference/dependency-pinning-audit.md
---

# Pin and Audit Dependencies

Use this guide when adding a new dependency, plugin, GitHub Action, or submodule,
or when re-running the supply-chain audit. The goal is that every external build
input resolves to an exact, immutable identifier.

Background: [Supply-Chain Dependency Pinning](../concepts/supply-chain-dependency-pinning.md).
Current inventory: [Dependency Pinning Audit](../reference/dependency-pinning-audit.md).

## Prerequisites

Run commands from the repository root. Initialize the grammar submodule before
any Maven build.

```bash
git submodule update --init tweedle-lang
export NODE_OPTIONS=--max-old-space-size=32768
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

Work on a feature branch off `develop`. Never push to the `upstream-source`
remote; push only to `origin`.

```bash
git checkout develop && git pull --ff-only origin develop
git checkout -b <your-branch>
```

## 1. Scan Maven for mutable versions

A dependency or plugin is compliant when it declares an exact `<version>`. Flag
anything using a range (`[..]`, `(..)`), `LATEST`, or `RELEASE`.

```bash
grep -rn "LATEST\|RELEASE\|\[.*,.*\]\|(.*,.*)" --include=pom.xml .
```

Cross-check each hit against the
[allowlist](../reference/dependency-pinning-audit.md#allowlist--version-like-strings-that-are-intentionally-not-pinned).
Legitimate matches you should **not** change:

- `-SNAPSHOT` on `org.alice` reactor modules,
- the Enforcer `requireMavenVersion` range (e.g. `[3.9.9,4)`),
- the m2e `lifecycle-mapping` filter (e.g. `[0.1,)`),
- NetBeans identifiers such as `RELEASE180`.

To pin a real finding, set an exact version in the centralized
`<dependencyManagement>` or `<pluginManagement>` block of the root `pom.xml` so
every module inherits it. Do not add versions to `org.alice` reactor siblings.

## 2. List every GitHub Action reference

```bash
grep -rn "uses:" .github/workflows/ .github/actions/
```

Any `uses: owner/repo@vN` (a tag) must be pinned. A `uses: ./.github/...` local
reference is already immutable and needs no SHA.

> This check is performed manually today. An automated CI lint that rejects
> non-SHA `uses: …@vN` references is [planned follow-up](../reference/dependency-pinning-audit.md#planned-hardening--not-yet-implemented),
> not a current gate — do not assume CI will catch an unpinned tag for you.

## 3. Resolve a tag to its commit SHA

Pin to the 40-character commit SHA that the version tag points to. If the tag is
an *annotated* tag, dereference it to the underlying commit.

```bash
resolve() {
  local repo=$1 tag=$2
  local json
  json=$(gh api "repos/$repo/git/ref/tags/$tag")
  local type sha
  type=$(echo "$json" | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["type"])')
  sha=$(echo "$json" | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["sha"])')
  if [ "$type" = "tag" ]; then
    sha=$(gh api "repos/$repo/git/tags/$sha" \
      | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["sha"])')
  fi
  echo "$repo -> $sha"
}

resolve actions/checkout v4
resolve actions/setup-java v4
```

Record the exact semver (e.g. `v4.4.0`) that the resolved SHA corresponds to so
you can add it as a trailing comment.

## 4. Apply the pin

Replace the mutable tag with the SHA and append a `# vX.Y.Z` comment. Use `|` as
the `sed` delimiter so the `#` in the comment does not clash. Anchor on the tag
so the edit is idempotent and does not re-pin an already-pinned reference.

```bash
for f in .github/workflows/*.yml; do
  sed -i \
    -e 's|actions/checkout@v4$|actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0|g' \
    "$f"
done
```

The result reads:

```yaml
uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
```

Leave `uses: ./.github/actions/setup-xvfb` unchanged.

## 5. Validate the build under JDK 21

```bash
mvn -Dmaven.repo.local=/home/azureuser/.m2/repository \
    -Dlicense.skipAggregateDownloadLicenses=true \
    -Dinstall4j.skip \
    -Dcheckstyle.skip \
    -Dmdep.skip=true \
    -Djava.awt.headless=true \
    clean install
```

Require `BUILD SUCCESS` with **0 failures / 0 errors**. If you changed only
workflow YAML, the [Testing](../testing.md) suite result must be unchanged from
the prior run (the reference audit observed 58272 run / 2236 skipped). Confirm
the change scope:

```bash
git status --porcelain
git diff --stat
```

## 6. Commit and open the pull request

Every commit must carry both trailers. Push to `origin` only, and base the PR on
`develop`.

```bash
git add .github/workflows/*.yml pom.xml
git commit -F - <<'EOF'
ci: pin dependencies to immutable identifiers

<what changed and why>

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
Copilot-Session: <session-id>
EOF

git push -u origin <your-branch>
gh pr create --base develop --head <your-branch> \
  --title "Supply-chain: pin dependencies to immutable identifiers" \
  --body-file <audit-summary>
```

Include the audit summary (findings table plus the pinned SHA/version table) in
the PR body so reviewers can verify pins without leaving the pull request.

## Verify a pin later

To confirm a SHA still matches the version its comment claims:

```bash
gh api repos/actions/checkout/git/ref/tags/v4.4.0 \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["sha"])'
```

If the SHA no longer matches, the upstream tag was moved — investigate before
trusting it.
