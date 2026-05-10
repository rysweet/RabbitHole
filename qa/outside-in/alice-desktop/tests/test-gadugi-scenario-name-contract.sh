#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-gadugi-scenario-name-contract.sh
#
# TDD contract: every scenario YAML in scenarios/ must carry gadugi-test
# compatible fields (name, steps, agents) and pass gadugi-test validate.
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
SCENARIOS_DIR="$BASE_DIR/scenarios"
VALIDATOR="$BASE_DIR/runners/validate-scenarios.sh"
SCHEMA="$BASE_DIR/schema/scenario.schema.json"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

# ── 1-4. YAML field checks (single Python process for all 30 files) ──────

python3 - "$SCENARIOS_DIR" >"$tmp_root/yaml-checks.out" 2>"$tmp_root/yaml-checks.err" <<'PY'
from pathlib import Path
import json
import sys
import yaml

scenarios_dir = Path(sys.argv[1])
results = {"name": [], "title_match": [], "steps_agents": [], "ordering": []}

for path in sorted(scenarios_dir.glob("*.yaml")):
    text = path.read_text(encoding="utf-8")
    data = yaml.safe_load(text)

    # check 1: name field present
    if "name" not in data:
        results["name"].append(f"{path.name}: missing 'name' field")
    # check 2: name matches title
    elif data.get("name", "") != data.get("title", ""):
        results["title_match"].append(
            f"{path.name}: name={data['name']!r} != title={data.get('title')!r}"
        )
    # check 3: steps and agents present
    if "steps" not in data:
        results["steps_agents"].append(f"{path.name}: missing 'steps' field")
    if "agents" not in data:
        results["steps_agents"].append(f"{path.name}: missing 'agents' field")
    # check 4: name line follows id line
    id_line = name_line = None
    for i, line in enumerate(text.splitlines()):
        if line.startswith("id:"):
            id_line = i
        elif line.startswith("name:"):
            name_line = i
    if id_line is None or name_line is None:
        results["ordering"].append(f"{path.name}: missing id or name line")
    elif name_line <= id_line:
        results["ordering"].append(
            f"{path.name}: name (line {name_line}) must appear after id (line {id_line})"
        )

# emit results as JSON for individual assertion checking
json.dump(results, sys.stdout)
PY
yaml_status=$?
assert_success "$yaml_status" "YAML field checks Python process exits 0"

# Parse consolidated results into individual assertions
python3 - "$tmp_root/yaml-checks.out" >"$tmp_root/yaml-split.out" 2>"$tmp_root/yaml-split.err" <<'PY'
import json, sys
results = json.load(open(sys.argv[1]))
failed = []
for check, errors in results.items():
    if errors:
        failed.append(f"{check}: " + "; ".join(errors))
if failed:
    raise AssertionError("\n".join(failed))
print("all YAML field checks passed")
PY
assert_success "$?" "every scenario YAML has name, name==title, steps, agents, correct ordering"

# ── 5-6. Schema + validator static checks (single Python process) ────────

python3 - "$SCHEMA" "$VALIDATOR" >"$tmp_root/static-checks.out" 2>"$tmp_root/static-checks.err" <<'PY'
import json
import sys

schema = json.load(open(sys.argv[1], encoding="utf-8"))
validator = open(sys.argv[2], encoding="utf-8").read()
errors = []

# Schema property checks
properties = set(schema.get("properties", {}).keys())
for field in ("name", "steps", "agents"):
    if field not in properties:
        errors.append(f"schema properties must include '{field}'")

if "name" in properties:
    name_prop = schema["properties"]["name"]
    if name_prop.get("type") != "string":
        errors.append("schema name property must be type string")
    if name_prop.get("minLength") != 1:
        errors.append("schema name property must have minLength 1")

for field in ("steps", "agents"):
    if field in properties and schema["properties"][field].get("type") != "array":
        errors.append(f"schema {field} property must be type array")

# Validator allowed_top checks
for field in ("name", "steps", "agents"):
    if f'"{field}"' not in validator:
        errors.append(f"validate-scenarios.sh must include '{field}' in allowed_top")

if errors:
    raise AssertionError("\n".join(errors))
print("schema and validator static checks passed")
PY
assert_success "$?" "schema and validator accept name, steps, agents"

# ── 7. gadugi-test validate reports 0 invalid files ──────────────────────

if command -v gadugi-test >/dev/null 2>&1; then
  gadugi_output=$(cd "$BASE_DIR" && gadugi-test validate scenarios/ 2>&1)
  gadugi_status=$?
  printf '%s\n' "$gadugi_output" >"$tmp_root/gadugi-validate.out"
  assert_success "$gadugi_status" "gadugi-test validate exits 0"

  if echo "$gadugi_output" | grep -q 'Invalid files: 0'; then
    pass "gadugi-test reports 0 invalid files"
  else
    fail "gadugi-test reports 0 invalid files (output: $gadugi_output)"
  fi
else
  pass "gadugi-test not installed — skipping runtime validation (CI will catch this)"
fi

# ── 8. validate-scenarios.sh still passes with gadugi fields present ─────

"$VALIDATOR" >"$tmp_root/validate-all.out" 2>"$tmp_root/validate-all.err"
assert_success "$?" "validate-scenarios.sh passes with gadugi-compatible scenario files"

# ── 9. validate-scenarios.sh still accepts scenarios without name ─────────
# (name is optional in the custom validator — gadugi-test uses title fallback)

no_name_custom_dir="$tmp_root/no-name-custom"
mkdir -p "$no_name_custom_dir"
cp "$SCENARIOS_DIR"/*.yaml "$no_name_custom_dir"/
python3 - "$no_name_custom_dir/launch.yaml" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
lines = [l for l in path.read_text(encoding="utf-8").splitlines() if not l.startswith("name:")]
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY
ALICE_QA_SCENARIO_DIR="$no_name_custom_dir" "$VALIDATOR" >"$tmp_root/no-name-custom.out" 2>"$tmp_root/no-name-custom.err"
assert_success "$?" "validate-scenarios.sh accepts scenarios without name (name is optional in custom validator)"

finish
