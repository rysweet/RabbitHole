#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/test-ci-jogamp-dependency-contract.sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
# shellcheck source=qa/outside-in/alice-desktop/tests/lib/assertions.sh
. "$SCRIPT_DIR/lib/assertions.sh"

ci_settings="$REPO_ROOT/.github/maven/jogamp-ci-settings.xml"
test_workflow="$REPO_ROOT/.github/workflows/alice-test-ci.yml"
netbeans_workflow="$REPO_ROOT/.github/workflows/alice-netbeans-package-ci.yml"
pom="$REPO_ROOT/pom.xml"

assert_file_exists "$ci_settings" "CI has a checked-in JogAmp Maven settings file"
assert_literal_in_file "$test_workflow" ".github/maven/jogamp-ci-settings.xml" "headed Xvfb CI uses the JogAmp reliability settings"
assert_literal_in_file "$netbeans_workflow" ".github/maven/jogamp-ci-settings.xml" "NetBeans package CI uses the JogAmp reliability settings"

if [ -f "$ci_settings" ]; then
  assert_contains "$ci_settings" '<settings' "JogAmp CI settings is a Maven settings file"
  assert_contains "$ci_settings" 'https://repo.maven.apache.org/maven2|https://repo1.maven.org/maven2' "JogAmp CI settings keeps Maven Central available"
  assert_contains "$ci_settings" 'jogamp|JOGL|GlueGen|org\.jogamp' "JogAmp CI settings documents the GL dependency mitigation"
  assert_pattern_absent_from_file "$ci_settings" 'http://[^< ]+' "JogAmp CI settings uses HTTPS repositories only"
  assert_pattern_absent_from_file "$ci_settings" 'checksumPolicy>[[:space:]]*ignore|ssl\.insecure|maven\.wagon\.http\.ssl\.allowall|--no-check-certificate' "JogAmp CI settings does not bypass artifact integrity checks"
fi

assert_contains "$pom" '<jogl.version>[^<]+</jogl.version>' "root pom remains the JOGL version source of truth"
assert_contains "$pom" '<gluegen.version>[^<]+</gluegen.version>' "root pom remains the GlueGen version source of truth"
assert_literal_in_file "$pom" "<groupId>org.jogamp.jogl</groupId>" "root pom still declares the JOGL group"
assert_literal_in_file "$pom" "<artifactId>jogl-all</artifactId>" "root pom still declares the JOGL artifact"
assert_literal_in_file "$pom" "<groupId>org.jogamp.gluegen</groupId>" "root pom still declares the GlueGen group"
assert_literal_in_file "$pom" "<artifactId>gluegen-rt</artifactId>" "root pom still declares the GlueGen artifact"

finish
