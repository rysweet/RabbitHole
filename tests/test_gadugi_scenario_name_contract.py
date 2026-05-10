"""TDD contract tests for gadugi-test scenario name compatibility.

Every scenario YAML in qa/outside-in/alice-desktop/scenarios/ must carry
a ``name`` field (matching ``title``), plus ``steps`` and ``agents`` arrays,
so that ``gadugi-test validate`` reports 0 invalid files.

These tests define the contract BEFORE implementation — they should fail
if any scenario is missing the required gadugi-test fields, and pass once
all 30 files are fixed.
"""

import json
import os
import shutil
import subprocess
import unittest
from pathlib import Path

try:
    import yaml
except ImportError:
    yaml = None  # type: ignore[assignment]

REPO_ROOT = Path(__file__).resolve().parents[1]
SCENARIOS_DIR = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios"
SCHEMA_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "schema" / "scenario.schema.json"
VALIDATOR_PATH = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners" / "validate-scenarios.sh"
GADUGI_BASE = REPO_ROOT / "qa" / "outside-in" / "alice-desktop"


def scenario_files():
    return sorted(SCENARIOS_DIR.glob("*.yaml"))


_yaml_cache: dict[Path, dict] = {}


def load_yaml(path: Path) -> dict:
    assert yaml is not None, "PyYAML is required for these tests"
    if path not in _yaml_cache:
        _yaml_cache[path] = yaml.safe_load(path.read_text(encoding="utf-8"))
    return _yaml_cache[path]


@unittest.skipIf(yaml is None, "PyYAML not installed")
class TestEveryScenarioHasNameField(unittest.TestCase):
    """Contract: every scenario YAML must have a ``name`` field."""

    def test_all_scenarios_have_name_field(self) -> None:
        missing = []
        for path in scenario_files():
            data = load_yaml(path)
            if "name" not in data:
                missing.append(path.name)
        self.assertEqual(missing, [], f"Scenarios missing 'name': {missing}")

    def test_name_is_nonempty_string(self) -> None:
        for path in scenario_files():
            data = load_yaml(path)
            name = data.get("name")
            with self.subTest(file=path.name):
                self.assertIsInstance(name, str, f"{path.name}: name must be a string")
                self.assertGreater(len(name), 0, f"{path.name}: name must be non-empty")


@unittest.skipIf(yaml is None, "PyYAML not installed")
class TestNameMatchesTitle(unittest.TestCase):
    """Contract: ``name`` must equal ``title`` in every scenario."""

    def test_name_equals_title_in_all_scenarios(self) -> None:
        mismatches = []
        for path in scenario_files():
            data = load_yaml(path)
            name = data.get("name", "")
            title = data.get("title", "")
            if name != title:
                mismatches.append(f"{path.name}: name={name!r} != title={title!r}")
        self.assertEqual(mismatches, [], f"Mismatches:\n" + "\n".join(mismatches))


@unittest.skipIf(yaml is None, "PyYAML not installed")
class TestGadugiRequiredFields(unittest.TestCase):
    """Contract: every scenario must have ``steps`` and ``agents`` arrays."""

    def test_all_scenarios_have_steps(self) -> None:
        missing = []
        for path in scenario_files():
            data = load_yaml(path)
            if "steps" not in data:
                missing.append(path.name)
        self.assertEqual(missing, [], f"Scenarios missing 'steps': {missing}")

    def test_all_scenarios_have_agents(self) -> None:
        missing = []
        for path in scenario_files():
            data = load_yaml(path)
            if "agents" not in data:
                missing.append(path.name)
        self.assertEqual(missing, [], f"Scenarios missing 'agents': {missing}")

    def test_steps_is_a_list(self) -> None:
        for path in scenario_files():
            data = load_yaml(path)
            with self.subTest(file=path.name):
                self.assertIsInstance(data.get("steps"), list, f"{path.name}: steps must be a list")

    def test_agents_is_a_list(self) -> None:
        for path in scenario_files():
            data = load_yaml(path)
            with self.subTest(file=path.name):
                self.assertIsInstance(data.get("agents"), list, f"{path.name}: agents must be a list")


@unittest.skipIf(yaml is None, "PyYAML not installed")
class TestScenarioCount(unittest.TestCase):
    """Guard: we expect exactly 30 scenario files."""

    def test_scenario_count(self) -> None:
        count = len(scenario_files())
        self.assertEqual(count, 30, f"Expected 30 scenarios, found {count}")


class TestSchemaAcceptsGadugiFields(unittest.TestCase):
    """Contract: scenario.schema.json must declare name, steps, agents."""

    def setUp(self) -> None:
        self.schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))

    def test_schema_has_name_property(self) -> None:
        self.assertIn("name", self.schema["properties"])

    def test_schema_name_is_string_with_minlength(self) -> None:
        name_prop = self.schema["properties"]["name"]
        self.assertEqual(name_prop["type"], "string")
        self.assertEqual(name_prop["minLength"], 1)

    def test_schema_has_steps_property(self) -> None:
        self.assertIn("steps", self.schema["properties"])

    def test_schema_steps_is_array(self) -> None:
        self.assertEqual(self.schema["properties"]["steps"]["type"], "array")

    def test_schema_has_agents_property(self) -> None:
        self.assertIn("agents", self.schema["properties"])

    def test_schema_agents_is_array(self) -> None:
        self.assertEqual(self.schema["properties"]["agents"]["type"], "array")

    def test_name_is_not_required(self) -> None:
        """name is optional in the JSON schema — only gadugi-test requires it."""
        required = self.schema.get("required", [])
        self.assertNotIn("name", required)

    def test_steps_is_not_required(self) -> None:
        required = self.schema.get("required", [])
        self.assertNotIn("steps", required)

    def test_agents_is_not_required(self) -> None:
        required = self.schema.get("required", [])
        self.assertNotIn("agents", required)


class TestValidatorAllowsGadugiFields(unittest.TestCase):
    """Contract: validate-scenarios.sh allowed_top must include gadugi fields."""

    def setUp(self) -> None:
        self.validator_text = VALIDATOR_PATH.read_text(encoding="utf-8")

    def test_allowed_top_includes_name(self) -> None:
        self.assertIn('"name"', self.validator_text)

    def test_allowed_top_includes_steps(self) -> None:
        self.assertIn('"steps"', self.validator_text)

    def test_allowed_top_includes_agents(self) -> None:
        self.assertIn('"agents"', self.validator_text)


class TestValidateScenariosShPasses(unittest.TestCase):
    """Integration: validate-scenarios.sh must pass on the current catalog."""

    def test_validate_scenarios_exits_zero(self) -> None:
        result = subprocess.run(
            ["bash", str(VALIDATOR_PATH)],
            cwd=str(REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=60,
            env={**os.environ, "NODE_OPTIONS": "--max-old-space-size=32768"},
        )
        self.assertEqual(
            0,
            result.returncode,
            f"validate-scenarios.sh failed:\nstdout: {result.stdout}\nstderr: {result.stderr}",
        )


@unittest.skipUnless(shutil.which("gadugi-test"), "gadugi-test not installed")
class TestGadugiTestValidate(unittest.TestCase):
    """Integration: gadugi-test validate must report 0 invalid files."""

    _result: subprocess.CompletedProcess | None = None

    @classmethod
    def setUpClass(cls) -> None:
        cls._result = subprocess.run(
            ["gadugi-test", "validate", "scenarios/"],
            cwd=str(GADUGI_BASE),
            capture_output=True,
            text=True,
            timeout=60,
        )

    def test_gadugi_validate_exits_zero(self) -> None:
        assert self._result is not None
        self.assertEqual(
            0,
            self._result.returncode,
            f"gadugi-test validate failed:\n{self._result.stdout}\n{self._result.stderr}",
        )

    def test_gadugi_reports_zero_invalid(self) -> None:
        assert self._result is not None
        combined = self._result.stdout + self._result.stderr
        self.assertIn("Invalid files: 0", combined)

    def test_gadugi_reports_30_valid(self) -> None:
        assert self._result is not None
        combined = self._result.stdout + self._result.stderr
        self.assertIn("Valid files: 30", combined)


@unittest.skipIf(yaml is None, "PyYAML not installed")
class TestFieldOrdering(unittest.TestCase):
    """Convention: name appears after id in the YAML file."""

    def test_name_line_follows_id_line(self) -> None:
        for path in scenario_files():
            lines = path.read_text(encoding="utf-8").splitlines()
            id_line = name_line = None
            for i, line in enumerate(lines):
                if line.startswith("id:"):
                    id_line = i
                elif line.startswith("name:"):
                    name_line = i
            with self.subTest(file=path.name):
                self.assertIsNotNone(id_line, f"{path.name}: missing id line")
                self.assertIsNotNone(name_line, f"{path.name}: missing name line")
                assert id_line is not None and name_line is not None
                self.assertGreater(
                    name_line, id_line,
                    f"{path.name}: name (line {name_line}) should follow id (line {id_line})",
                )


if __name__ == "__main__":
    unittest.main()
