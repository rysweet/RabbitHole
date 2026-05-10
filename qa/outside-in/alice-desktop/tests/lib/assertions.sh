#!/usr/bin/env bash
# qa/outside-in/alice-desktop/tests/lib/assertions.sh

failures=0

create_scratch_root() {
  local base_dir=$1
  local scratch_base=${ALICE_QA_TEST_SCRATCH_DIR:-$base_dir/.test-scratch}
  local attempt path run_name

  mkdir -p "$scratch_base" || return 1
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    run_name=$(date -u +%Y%m%dT%H%M%S%NZ)-$$-${RANDOM:-0}-$attempt
    path="$scratch_base/$run_name"
    if mkdir "$path"; then
      printf '%s\n' "$path"
      return 0
    fi
  done

  printf 'failed to create unique scratch directory under %s\n' "$scratch_base" >&2
  return 1
}

fail() {
  printf 'not ok - %s\n' "$1" >&2
  failures=$((failures + 1))
}

pass() {
  printf 'ok - %s\n' "$1"
}

assert_success() {
  local status=$1
  local label=$2
  if [ "$status" -eq 0 ]; then
    pass "$label"
  else
    fail "$label (exit $status)"
  fi
}

assert_failure() {
  local status=$1
  local label=$2
  if [ "$status" -ne 0 ]; then
    pass "$label"
  else
    fail "$label (expected failure)"
  fi
}

assert_exit_code() {
  local actual=$1
  local expected=$2
  local label=$3
  if [ "$actual" -eq "$expected" ]; then
    pass "$label"
  else
    fail "$label (expected exit $expected, got $actual)"
  fi
}

assert_file_exists() {
  local path=$1
  local label=$2
  if [ -f "$path" ]; then
    pass "$label"
  else
    fail "$label (missing $path)"
  fi
}

assert_contains() {
  local path=$1
  local pattern=$2
  local label=$3
  if [ -f "$path" ] && grep -Eq -- "$pattern" "$path"; then
    pass "$label"
  else
    fail "$label (pattern not found: $pattern)"
  fi
}

assert_not_contains() {
  local path=$1
  local pattern=$2
  local label=$3
  if [ -f "$path" ] && ! grep -Eq -- "$pattern" "$path"; then
    pass "$label"
  else
    fail "$label (unexpected pattern found: $pattern)"
  fi
}

assert_literal_in_file() {
  local path=$1
  local token=$2
  local label=$3
  if [ -f "$path" ] && grep -Fq -- "$token" "$path"; then
    pass "$label"
  else
    fail "$label (missing literal: $token)"
  fi
}

assert_literal_absent_from_file() {
  local path=$1
  local token=$2
  local label=$3
  if [ -f "$path" ] && ! grep -Fq -- "$token" "$path"; then
    pass "$label"
  else
    fail "$label (unexpected literal: $token)"
  fi
}

assert_pattern_absent_from_file() {
  local path=$1
  local pattern=$2
  local label=$3
  if [ -f "$path" ] && ! grep -Eq -- "$pattern" "$path"; then
    pass "$label"
  else
    fail "$label (unexpected pattern: $pattern)"
  fi
}

assert_exact_count_in_file() {
  local path=$1
  local token=$2
  local expected=$3
  local label=$4
  local actual
  if [ ! -f "$path" ]; then
    fail "$label (missing $path)"
    return
  fi
  actual=$(grep -Fc -- "$token" "$path")
  if [ "$actual" -eq "$expected" ]; then
    pass "$label"
  else
    fail "$label (expected $expected, got $actual)"
  fi
}

single_child_dir() {
  local parent=$1
  local found=
  local candidate

  if [ ! -d "$parent" ]; then
    printf 'missing directory: %s\n' "$parent" >&2
    return 1
  fi

  for candidate in "$parent"/*; do
    [ -d "$candidate" ] || continue
    if [ -n "$found" ]; then
      printf 'multiple child directories under %s\n' "$parent" >&2
      return 1
    fi
    found=$candidate
  done

  if [ -z "$found" ]; then
    printf 'no child directories under %s\n' "$parent" >&2
    return 1
  fi
  printf '%s\n' "$found"
}

finish() {
  if [ "$failures" -eq 0 ]; then
    exit 0
  fi
  printf '%s assertion(s) failed\n' "$failures" >&2
  exit 1
}
