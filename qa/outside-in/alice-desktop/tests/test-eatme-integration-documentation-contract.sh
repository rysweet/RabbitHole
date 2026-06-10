#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-eatme-integration-documentation-contract.sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

docs_index="$REPO_ROOT/docs/index.md"
readiness_howto="$REPO_ROOT/docs/howto/verify-ci-gui-eatme-xvfb-readiness.md"
validation_reference="$REPO_ROOT/docs/reference/ci-gui-eatme-xvfb-validation.md"
reopen_doc="$REPO_ROOT/docs/tools-eatme-reopen-project.md"

assert_file_exists "$reopen_doc" "Eatme reopen tool documentation is checked in"
assert_literal_in_file "$docs_index" "./tools-eatme-reopen-project.md" "Eatme reopen tool documentation is discoverable from docs index"
assert_literal_in_file "$docs_index" "./howto/verify-ci-gui-eatme-xvfb-readiness.md" "Eatme verification how-to is discoverable from docs index"
assert_literal_in_file "$docs_index" "./reference/ci-gui-eatme-xvfb-validation.md" "Eatme validation reference is discoverable from docs index"

for wrapper in \
  eatme-place-object \
  eatme-edit-procedure \
  eatme-run-world \
  eatme-save-project \
  eatme-reopen-project
do
  tool_path="$REPO_ROOT/tools/$wrapper"
  assert_file_exists "$tool_path" "$wrapper wrapper is checked in"
  assert_contains "$tool_path" 'alice-ide/target/lib is missing' "$wrapper fails clearly when Alice has not been packaged"
  assert_contains "$tool_path" 'alice-ide/target/\*:alice-ide/target/lib/\*' "$wrapper uses the packaged Alice classpath"
  assert_contains "$tool_path" 'org\.alice\.tools\.Eatme[A-Za-z]+' "$wrapper launches a repository-owned Eatme Java entry point"
  assert_contains "$tool_path" '"\$@"' "$wrapper forwards arguments without eval or reparsing"
  assert_literal_in_file "$readiness_howto" "tools/$wrapper" "$wrapper is covered by the Eatme readiness how-to"
  assert_literal_in_file "$validation_reference" "tools/$wrapper" "$wrapper is covered by the Eatme validation reference"
done

assert_contains "$reopen_doc" 'Schema: `eatme\.alice-project-reopen-result/v1`' "reopen doc names stdout result schema"
assert_contains "$reopen_doc" 'Schema: `eatme\.alice-project-reopen-artifact/v1`' "reopen doc names bounded reopen evidence schema"
assert_contains "$reopen_doc" 'Schema: `eatme\.alice-project-reopen-state/v1`' "reopen doc names reopened state evidence schema"
assert_contains "$reopen_doc" 'mvn -DincludeSims=false -Dinstall4j\.skip clean package -DskipTests' "reopen doc states the package precondition"
assert_contains "$validation_reference" 'Do not claim full first-lesson completion|Do not claim full first-lesson completion, grading, creative assessment' "Eatme evidence boundaries prevent overclaiming"

finish
