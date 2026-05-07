#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-root-directory-prep.sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PREP="$BASE_DIR/runners/prepare-root-directory.py"
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

tmp_root=$(create_scratch_root "$SCRIPT_DIR") || exit 1
trap 'rm -rf "$tmp_root"' EXIT

fake_repo="$tmp_root/fake-repo"
mkdir -p "$fake_repo/alice-ide" "$fake_repo/core/resources/target/distribution"
cat > "$fake_repo/alice-ide/pom.xml" <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <build>
    <plugins>
      <plugin>
        <configuration>
          <systemProperties>
            <systemProperty>
              <key>org.alice.ide.rootDirectory</key>
              <value>../core/resources/target/distribution</value>
            </systemProperty>
          </systemProperties>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML

ready_output="$tmp_root/root-directory-ready.json"
python3 "$PREP" --repo-root "$fake_repo" --alice-cwd alice-ide --output "$ready_output" --log "$tmp_root/ready.log" --no-execute
status=$?
assert_success "$status" "rootDirectory prep accepts an existing prepared distribution"
assert_contains "$ready_output" '"status": "ready"' "ready artifact records prepared state"
assert_contains "$ready_output" '"blocker": "none"' "ready artifact does not name a blocker"
assert_contains "$ready_output" '"configuredRootDirectory": "../core/resources/target/distribution"' "ready artifact records Maven rootDirectory property"
assert_contains "$ready_output" '"distributionExists": true' "ready artifact proves distribution path exists"
assert_contains "$ready_output" '"prepAttempted": false' "ready artifact avoids unnecessary Maven prep"

missing_repo="$tmp_root/missing-repo"
mkdir -p "$missing_repo/alice-ide"
cp "$fake_repo/alice-ide/pom.xml" "$missing_repo/alice-ide/pom.xml"
missing_output="$tmp_root/root-directory-missing.json"
python3 "$PREP" --repo-root "$missing_repo" --alice-cwd alice-ide --output "$missing_output" --log "$tmp_root/missing.log" --no-execute
status=$?
assert_exit_code "$status" 2 "rootDirectory prep plainly blocks when distribution is missing and execution is disabled"
assert_contains "$missing_output" '"status": "blocked"' "missing artifact records blocked status"
assert_contains "$missing_output" '"blocker": "core-resources-distribution-missing"' "missing artifact names exact missing distribution artifact"
assert_contains "$missing_output" 'core/resources/target/distribution' "missing artifact names distribution path"
assert_contains "$missing_output" '"mavenPhase": "process-resources"' "missing artifact names Maven phase to prepare distribution"
assert_contains "$missing_output" '"prepAttempted": false' "missing artifact does not pretend Maven prep ran"

unset_repo="$tmp_root/unset-repo"
mkdir -p "$unset_repo/alice-ide"
cat > "$unset_repo/alice-ide/pom.xml" <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <build />
</project>
XML
unset_output="$tmp_root/root-directory-unset.json"
python3 "$PREP" --repo-root "$unset_repo" --alice-cwd alice-ide --output "$unset_output" --log "$tmp_root/unset.log" --no-execute
status=$?
assert_exit_code "$status" 2 "rootDirectory prep blocks when alice-ide exec rootDirectory is not configured"
assert_contains "$unset_output" '"blocker": "root-directory-property-missing"' "unset artifact names missing JVM property"
assert_contains "$unset_output" 'org\.alice\.ide\.rootDirectory' "unset artifact names exact property"
assert_contains "$unset_output" '\.\./core/resources/target/distribution' "unset artifact names required property value"

finish
