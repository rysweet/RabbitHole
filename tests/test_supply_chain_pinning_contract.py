"""Supply-chain pinning contract (TDD, Step 7).

These tests define the contract for the supply-chain security audit and
version-pinning work on branch ``rysweet/supply-chain-pinning``:

  Surface A -- GitHub Actions
    * Every *external* ``uses:`` reference in ``.github/workflows/*.yml`` and in
      the local composite action must be pinned to a full 40-char lowercase-hex
      commit SHA (never a mutable tag such as ``@v4``, ``@main`` or ``@latest``).
    * Each pinned reference must carry a trailing ``# vX.Y.Z`` version comment so
      humans can read the intended version.
    * The in-repo composite reference ``./.github/actions/setup-xvfb`` is a local
      path and is intentionally exempt from SHA pinning.

  Surface B -- Maven
    * No ``<dependency>`` or plugin ``<version>`` may use a version range,
      ``LATEST`` or ``RELEASE``. In-repo ``org.alice`` reactor modules may keep
      their ``-SNAPSHOT`` versions (they are built from source in this reactor).

Written test-first: they encode the expected end-state and guard against
regression (a future edit that reintroduces a mutable tag or a dynamic version
must fail these tests).
"""

import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
WORKFLOW_DIR = REPO_ROOT / ".github" / "workflows"
ACTIONS_DIR = REPO_ROOT / ".github" / "actions"

# Directories that are not part of *this* checkout's source of truth.
_EXCLUDED_PATH_PARTS = {"target", "worktrees", ".git"}

SHA_RE = re.compile(r"^[0-9a-f]{40}$")
# Matches:  uses: owner/repo@<ref>   (optionally with a trailing "# comment")
USES_RE = re.compile(
    r"""uses:\s*
        (?P<ref>\S+)                 # the action reference
        (?:\s*\#\s*(?P<comment>.+?))?  # optional trailing version comment
        \s*$""",
    re.VERBOSE,
)
VERSION_COMMENT_RE = re.compile(r"^v\d+(\.\d+){0,2}\b")


def _iter_workflow_files():
    files = sorted(WORKFLOW_DIR.glob("*.yml")) + sorted(WORKFLOW_DIR.glob("*.yaml"))
    return files


def _iter_composite_action_files():
    if not ACTIONS_DIR.exists():
        return []
    return sorted(ACTIONS_DIR.glob("**/action.yml")) + sorted(
        ACTIONS_DIR.glob("**/action.yaml")
    )


def _iter_uses(path):
    """Yield (lineno, ref, comment) for every ``uses:`` line in *path*."""
    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        stripped = raw.strip().lstrip("-").strip()
        if not stripped.startswith("uses:"):
            continue
        m = USES_RE.match(stripped)
        if not m:
            yield lineno, None, None
            continue
        yield lineno, m.group("ref"), (m.group("comment") or "").strip() or None


def _is_local_ref(ref):
    return ref.startswith("./") or ref.startswith("../")


class GitHubActionsPinningContract(unittest.TestCase):
    def test_workflow_directory_exists(self):
        self.assertTrue(
            WORKFLOW_DIR.is_dir(), f"missing workflow dir: {WORKFLOW_DIR}"
        )

    def test_at_least_one_external_action_is_referenced(self):
        found = False
        for wf in _iter_workflow_files():
            for _, ref, _ in _iter_uses(wf):
                if ref and not _is_local_ref(ref):
                    found = True
        self.assertTrue(found, "expected external actions in workflows to pin")

    def test_every_external_action_is_pinned_to_full_sha(self):
        offenders = []
        for src in _iter_workflow_files() + _iter_composite_action_files():
            for lineno, ref, _ in _iter_uses(src):
                if ref is None or _is_local_ref(ref):
                    continue
                if "@" not in ref:
                    offenders.append(f"{src}:{lineno} no @ref -> {ref}")
                    continue
                pinned = ref.rsplit("@", 1)[1]
                if not SHA_RE.match(pinned):
                    offenders.append(
                        f"{src.relative_to(REPO_ROOT)}:{lineno} not a 40-char "
                        f"SHA -> {ref}"
                    )
        self.assertEqual([], offenders, "unpinned external actions:\n" + "\n".join(offenders))

    def test_no_mutable_tag_references(self):
        mutable = re.compile(r"@(v\d+|latest|main|master|develop|HEAD)\b", re.I)
        offenders = []
        for src in _iter_workflow_files() + _iter_composite_action_files():
            for lineno, ref, _ in _iter_uses(src):
                if ref is None or _is_local_ref(ref):
                    continue
                if mutable.search(ref):
                    offenders.append(f"{src.relative_to(REPO_ROOT)}:{lineno} -> {ref}")
        self.assertEqual([], offenders, "mutable tag refs:\n" + "\n".join(offenders))

    def test_every_pinned_action_has_version_comment(self):
        offenders = []
        for src in _iter_workflow_files() + _iter_composite_action_files():
            for lineno, ref, comment in _iter_uses(src):
                if ref is None or _is_local_ref(ref):
                    continue
                if comment is None or not VERSION_COMMENT_RE.match(comment):
                    offenders.append(
                        f"{src.relative_to(REPO_ROOT)}:{lineno} -> {ref} "
                        f"(comment={comment!r})"
                    )
        self.assertEqual(
            [], offenders, "pinned actions missing '# vX.Y.Z' comment:\n" + "\n".join(offenders)
        )

    def test_local_composite_reference_is_exempt_and_present(self):
        # The in-repo composite action is referenced by local path (no SHA).
        local_refs = []
        for wf in _iter_workflow_files():
            for _, ref, _ in _iter_uses(wf):
                if ref and _is_local_ref(ref):
                    local_refs.append(ref)
        self.assertIn(
            "./.github/actions/setup-xvfb",
            local_refs,
            "expected local composite action reference to remain a path (unpinned)",
        )


class MavenVersionPinningContract(unittest.TestCase):
    POM_NS = "{http://maven.apache.org/POM/4.0.0}"
    DYNAMIC_RE = re.compile(r"(LATEST|RELEASE|[\[\(].*,.*[\]\)])")

    @classmethod
    def _poms(cls):
        for pom in REPO_ROOT.rglob("pom.xml"):
            parts = set(pom.relative_to(REPO_ROOT).parts)
            if parts & _EXCLUDED_PATH_PARTS:
                continue
            yield pom

    def _tag(self, name):
        return f"{self.POM_NS}{name}"

    def _artifact_version_nodes(self, pom):
        """Yield (groupId, artifactId, version_text) for dependencies & plugins."""
        try:
            root = ET.parse(pom).getroot()
        except ET.ParseError as exc:  # pragma: no cover - defensive
            self.fail(f"unparseable pom {pom}: {exc}")
        for elem in root.iter():
            if elem.tag not in (self._tag("dependency"), self._tag("plugin")):
                continue
            gid = elem.findtext(self._tag("groupId"), default="")
            aid = elem.findtext(self._tag("artifactId"), default="")
            ver = elem.findtext(self._tag("version"))
            if ver is not None:
                yield gid.strip(), aid.strip(), ver.strip()

    def test_at_least_one_pom_found(self):
        self.assertTrue(list(self._poms()), "expected to discover pom.xml files")

    def test_no_dependency_or_plugin_uses_dynamic_version(self):
        offenders = []
        for pom in self._poms():
            for gid, aid, ver in self._artifact_version_nodes(pom):
                # In-repo reactor modules may keep SNAPSHOT versions.
                if gid.startswith("org.alice") and ver.endswith("-SNAPSHOT"):
                    continue
                if self.DYNAMIC_RE.search(ver):
                    offenders.append(
                        f"{pom.relative_to(REPO_ROOT)} -> {gid}:{aid} = {ver}"
                    )
        self.assertEqual(
            [], offenders, "dynamic dependency/plugin versions:\n" + "\n".join(offenders)
        )

    def test_no_dependency_or_plugin_version_is_empty(self):
        offenders = []
        for pom in self._poms():
            for gid, aid, ver in self._artifact_version_nodes(pom):
                if ver == "":
                    offenders.append(f"{pom.relative_to(REPO_ROOT)} -> {gid}:{aid} (empty)")
        self.assertEqual(
            [], offenders, "empty <version> elements:\n" + "\n".join(offenders)
        )


if __name__ == "__main__":  # pragma: no cover
    unittest.main(verbosity=2)
