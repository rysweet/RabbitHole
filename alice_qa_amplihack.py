#!/usr/bin/env python3
"""Small uvx-friendly command wrapper for Alice outside-in QA."""

from __future__ import annotations

import os
import subprocess
import sys
from collections.abc import Sequence
from pathlib import Path


USAGE = """usage:
  amplihack alice-scorecard [--root <dir>] [--output <path>]
  amplihack alice-qa validate
  amplihack alice-qa list
  amplihack alice-qa save-negative-contract
  amplihack alice-qa run <scenario-id-or-path> [--evidence-dir <dir>] [--timeout-seconds <seconds>] [--prepare-only]
  amplihack getting-started validate [--headless|--gui|--all|--help]
  amplihack archive-player-boundary verify
  amplihack tweedle-decode verify <simple-if-method-call|simple-if-boundaries|simple-if-player-archive>

Run from the Alice repository root or one of its child directories.
"""


TWEEDLE_DECODE_SCENARIOS = {
    "simple-if-method-call": {
        "description": "Tweedle simple-if body decodes a zero-argument this.method() call",
        "module": "core/ast",
        "tests": "TweedleEncoderDecoderTest#decodeClassWithSimpleIfMethodCallBodyCreatesConditionalMethodInvocation",
    },
    "simple-if-boundaries": {
        "description": "Tweedle simple-if body keeps unsupported neighboring statements rejected",
        "module": "core/ast",
        "tests": (
            "TweedleEncoderDecoderTest#"
            "decodeClassWithSimpleIfLogicalConditionAndMixedSupportedBodyCreatesOrderedStatements"
            "+decodeClassWithArgumentBearingThisMethodCallInIfBodyReportsUnsupportedBoundary"
            "+decodeClassWithArbitraryReceiverMethodCallInIfBodyReportsUnsupportedBoundary"
            "+decodeClassWithMethodCallInIfElseBodyReportsUnsupportedBoundary"
            "+decodeClassWithNestedIfInIfBodyReportsUnsupported"
        ),
    },
    "simple-if-player-archive": {
        "description": "JSON player archive Tweedle type decodes the simple-if method-call slice",
        "module": "core/story-api-migration",
        "tests": "IoUtilitiesTest#jsonPlayerTweedleSimpleIfMethodCallDecodesProgramType",
    },
}


ARCHIVE_PLAYER_BOUNDARY_TEST = "org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest"
NODE_OPTIONS_MEMORY_FLAG = "--max-old-space-size=32768"
TWEEDLE_SUBMODULE_COMMAND = ("git", "submodule", "update", "--init", "tweedle-lang")
ARCHIVE_PLAYER_BOUNDARY_COMMAND = (
    "mvn",
    "-DincludeSims=false",
    "-Dinstall4j.skip",
    "-pl",
    "core/story-api-migration",
    "-am",
    "-DfailIfNoTests=false",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    f"-Dtest={ARCHIVE_PLAYER_BOUNDARY_TEST}",
    "test",
    "-q",
)


def find_repo_root(start: Path) -> Path | None:
    """Return the nearest Alice checkout root at or above start."""
    candidate = start
    while True:
        runners = candidate / "qa" / "outside-in" / "alice-desktop" / "runners"
        if (runners / "validate-scenarios.sh").is_file() and (runners / "run-scenario.sh").is_file():
            return candidate
        if candidate.parent == candidate:
            return None
        candidate = candidate.parent


def run_from_repo(root: Path, command: Sequence[str], env: dict[str, str] | None = None) -> int:
    """Run a repository command and return its process exit code."""
    return subprocess.run(command, cwd=root, check=False, env=env).returncode


def node_options_env() -> dict[str, str] | None:
    """Return an environment with the saved Node memory flag when needed."""
    existing = os.environ.get("NODE_OPTIONS", "")
    if NODE_OPTIONS_MEMORY_FLAG in existing.split():
        return None
    env = dict(os.environ)
    env["NODE_OPTIONS"] = f"{existing} {NODE_OPTIONS_MEMORY_FLAG}".strip()
    return env


def report_result(label: str, returncode: int) -> int:
    """Print a stable PASS/FAIL line and preserve the command return code."""
    if returncode == 0:
        print(f"PASS: {label}")
    else:
        print(f"FAIL: {label}", file=sys.stderr)
    return returncode


def run_archive_player_boundary_verification(root: Path) -> int:
    """Run the bounded archive/player readiness verification sequence."""
    runners = root / "qa" / "outside-in" / "alice-desktop" / "runners"
    env = node_options_env()

    print("Running archive/player boundary scenario validation")
    scenario_result = run_from_repo(
        root,
        [str(runners / "validate-scenarios.sh")],
        env=env,
    )
    if scenario_result != 0:
        print("FAIL: archive/player boundary scenario validation failed", file=sys.stderr)
        return scenario_result

    print("Initializing Tweedle grammar submodule")
    submodule_result = run_from_repo(
        root,
        TWEEDLE_SUBMODULE_COMMAND,
        env=env,
    )
    if submodule_result != 0:
        print("FAIL: unable to initialize tweedle-lang submodule", file=sys.stderr)
        return submodule_result

    print("Running focused archive/player boundary characterization tests")
    test_result = run_from_repo(
        root,
        ARCHIVE_PLAYER_BOUNDARY_COMMAND,
        env=env,
    )
    return report_result("archive-player-boundary", test_result)


def run_tweedle_decode_verification(root: Path, scenario: str) -> int:
    """Run the focused Tweedle decode verification scenario."""
    selected = TWEEDLE_DECODE_SCENARIOS.get(scenario)
    if selected is None:
        valid = ", ".join(sorted(TWEEDLE_DECODE_SCENARIOS))
        print(f"unknown tweedle-decode scenario: {scenario}", file=sys.stderr)
        print(f"valid scenarios: {valid}", file=sys.stderr)
        return 2

    description = selected["description"]
    module = selected["module"]
    test_selector = selected["tests"]
    print(f"Running Tweedle decode scenario: {description}")
    env = node_options_env()
    submodule_result = run_from_repo(root, TWEEDLE_SUBMODULE_COMMAND, env=env)
    if submodule_result != 0:
        print("FAIL: unable to initialize tweedle-lang submodule", file=sys.stderr)
        return submodule_result

    test_result = run_from_repo(
        root,
        [
            "mvn",
            "-pl",
            module,
            "-am",
            "-DfailIfNoTests=false",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            f"-Dtest={test_selector}",
            "test",
            "-q",
        ],
        env=env,
    )
    return report_result(scenario, test_result)


def run_alice_scorecard(root: Path, args: Sequence[str]) -> int:
    """Delegate scorecard generation to the repository script."""
    return run_from_repo(
        root,
        [
            sys.executable,
            str(root / "scripts" / "generate-modernization-scorecard.py"),
            *args[1:],
        ],
    )


def run_alice_qa(root: Path, args: Sequence[str]) -> int:
    """Delegate Alice desktop QA subcommands to the checked-in runners."""
    if len(args) == 1 or args[1] in {"-h", "--help", "help"}:
        print(USAGE, end="")
        return 0

    runners = root / "qa" / "outside-in" / "alice-desktop" / "runners"
    subcommand = args[1]
    if subcommand == "validate":
        if len(args) != 2:
            print("alice-qa validate does not accept extra arguments", file=sys.stderr)
            return 2
        return run_from_repo(root, [str(runners / "validate-scenarios.sh")])
    if subcommand == "list":
        if len(args) != 2:
            print("alice-qa list does not accept extra arguments", file=sys.stderr)
            return 2
        return run_from_repo(root, [str(runners / "run-scenario.sh"), "list"])
    if subcommand == "save-negative-contract":
        if len(args) != 2:
            print("alice-qa save-negative-contract does not accept extra arguments", file=sys.stderr)
            return 2
        return run_from_repo(
            root,
            [
                "bash",
                str(root / "qa" / "outside-in" / "alice-desktop" / "tests" / "test-save-menu-dialog-negative-artifact-contract.sh"),
            ],
        )
    if subcommand == "run":
        if len(args) < 3:
            print("alice-qa run requires a scenario id or path", file=sys.stderr)
            return 2
        return run_from_repo(root, [str(runners / "run-scenario.sh"), "run", *args[2:]])

    print(f"unknown alice-qa command: {subcommand}", file=sys.stderr)
    print(USAGE, end="", file=sys.stderr)
    return 2


def run_getting_started_validation(root: Path, args: Sequence[str]) -> int:
    """Delegate Getting Started validation to the checked-in executable."""
    if len(args) == 1 or args[1] in {"-h", "--help", "help"}:
        return run_from_repo(root, [str(root / "scripts" / "validate-getting-started.sh"), "--help"])
    if args[1] != "validate":
        print(
            "getting-started usage: amplihack getting-started validate [--headless|--gui|--all|--help]",
            file=sys.stderr,
        )
        return 2
    return run_from_repo(root, [str(root / "scripts" / "validate-getting-started.sh"), *args[2:]])


def main(argv: list[str] | None = None) -> int:
    """Dispatch amplihack command wrapper arguments."""
    args = list(sys.argv[1:] if argv is None else argv)
    if not args or args[0] in {"-h", "--help", "help"}:
        print(USAGE, end="")
        return 0

    root = find_repo_root(Path.cwd().resolve())
    if root is None:
        print(
            "amplihack alice-qa must be run from an Alice repository checkout",
            file=sys.stderr,
        )
        return 2

    if args[0] == "alice-scorecard":
        return run_alice_scorecard(root, args)

    if args[0] == "tweedle-decode":
        if len(args) != 3 or args[1] != "verify":
            print("tweedle-decode usage: amplihack tweedle-decode verify <scenario>", file=sys.stderr)
            return 2
        return run_tweedle_decode_verification(root, args[2])

    if args[0] == "archive-player-boundary":
        if len(args) != 2 or args[1] != "verify":
            print("archive-player-boundary usage: amplihack archive-player-boundary verify", file=sys.stderr)
            return 2
        return run_archive_player_boundary_verification(root)

    if args[0] == "getting-started":
        return run_getting_started_validation(root, args)

    if args[0] == "alice-qa":
        return run_alice_qa(root, args)

    print(f"unknown command: {args[0]}", file=sys.stderr)
    print(USAGE, end="", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
