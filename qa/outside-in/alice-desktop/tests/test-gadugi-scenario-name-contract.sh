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

# ── 1. Every scenario YAML has a name field ──────────────────────────────

python3 - "$SCENARIOS_DIR" >"$tmp_root/name-field.out" 2>"$tmp_root/name-field.err" <<'PY'
from pathlib import Path
import sys
import yaml

scenarios_dir = Path(sys.argv[1])
errors = []
for path in sorted(scenarios_dir.glob("*.yaml")):
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    if "name" not in data:
        errors.append(f"{path.name}: missing 'name' field")
if errors:
    raise AssertionError("Scenarios missing name field:\n" + "\n".join(errors))
print(f"all {len(list(scenarios_dir.glob('*.yaml')))} scenarios have a name field")
PY
assert_success "$?" "every scenario YAML has a name field"

# ── 2. name matches title in every scenario ──────────────────────────────

python3 - "$SCENARIOS_DIR" >"$tmp_root/name-title.out" 2>"$tmp_root/name-title.err" <<'PY'
from pathlib import Path
import sys
import yaml

scenarios_dir = Path(sys.argv[1])
errors = []
for path in sorted(scenarios_dir.glob("*.yaml")):
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    name = data.get("name", "")
    title = data.get("title", "")
    if name != title:
        errors.append(f"{path.name}: name={name!r} != title={title!r}")
if errors:
    raise AssertionError("Scenarios with name/title mismatch:\n" + "\n".join(errors))
print("all scenario name fields match their title fields")
PY
assert_success "$?" "scenario name matches title in every file"

# ── 3. Every scenario YAML has steps and agents fields ───────────────────

python3 - "$SCENARIOS_DIR" >"$tmp_root/steps-agents.out" 2>"$tmp_root/steps-agents.err" <<'PY'
from pathlib import Path
import sys
import yaml

scenarios_dir = Path(sys.argv[1])
errors = []
for path in sorted(scenarios_dir.glob("*.yaml")):
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    if "steps" not in data:
        errors.append(f"{path.name}: missing 'steps' field")
    if "agents" not in data:
        errors.append(f"{path.name}: missing 'agents' field")
if errors:
    raise AssertionError("Scenarios missing gadugi-test fields:\n" + "\n".join(errors))
print("all scenarios have steps and agents fields")
PY
assert_success "$?" "every scenario YAML has steps and agents fields"

# ── 4. name field appears after id and before workflow ───────────────────

python3 - "$SCENARIOS_DIR" >"$tmp_root/field-order.out" 2>"$tmp_root/field-order.err" <<'PY'
from pathlib import Path
import sys

scenarios_dir = Path(sys.argv[1])
errors = []
for path in sorted(scenarios_dir.glob("*.yaml")):
    lines = path.read_text(encoding="utf-8").splitlines()
    id_line = name_line = title_line = None
    for i, line in enumerate(lines):
        if line.startswith("id:"):
            id_line = i
        elif line.startswith("name:"):
            name_line = i
        elif line.startswith("title:"):
            title_line = i
    if id_line is None or name_line is None:
        errors.append(f"{path.name}: missing id or name line")
        continue
    if name_line <= id_line:
        errors.append(f"{path.name}: name (line {name_line}) must appear after id (line {id_line})")
if errors:
    raise AssertionError("Field ordering issues:\n" + "\n".join(errors))
print("name field is correctly positioned after id in all scenarios")
PY
assert_success "$?" "name field appears after id line in every scenario"

# ── 5. Schema accepts name, steps, and agents properties ─────────────────

python3 - "$SCHEMA" >"$tmp_root/schema-props.out" 2>"$tmp_root/schema-props.err" <<'PY'
import json
import sys

schema = json.load(open(sys.argv[1], encoding="utf-8"))
properties = set(schema.get("properties", {}).keys())
errors = []
for field in ("name", "steps", "agents"):
    if field not in properties:
        errors.append(f"schema properties must include '{field}'")
if errors:
    raise AssertionError("\n".join(errors))

name_prop = schema["properties"]["name"]
if name_prop.get("type") != "string":
    raise AssertionError("schema name property must be type string")
if name_prop.get("minLength") != 1:
    raise AssertionError("schema name property must have minLength 1")

for field in ("steps", "agents"):
    if schema["properties"][field].get("type") != "array":
        raise AssertionError(f"schema {field} property must be type array")

print("schema accepts name, steps, and agents properties")
PY
assert_success "$?" "schema declares name, steps, and agents properties"

# ── 6. validate-scenarios.sh allowed_top includes gadugi fields ──────────

python3 - "$VALIDATOR" >"$tmp_root/allowed-top.out" 2>"$tmp_root/allowed-top.err" <<'PY'
import sys

validator = open(sys.argv[1], encoding="utf-8").read()
errors = []
for field in ("name", "steps", "agents"):
    if f'"{field}"' not in validator:
        errors.append(f"validate-scenarios.sh must include '{field}' in allowed_top")
if errors:
    raise AssertionError("\n".join(errors))
print("validate-scenarios.sh allowed_top includes gadugi fields")
PY
assert_success "$?" "validate-scenarios.sh allowed_top includes name, steps, agents"

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
