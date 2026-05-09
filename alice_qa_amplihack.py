#!/usr/bin/env python3
"""Small uvx-friendly command wrapper for Alice outside-in QA."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


USAGE = """usage:
  amplihack alice-scorecard [--root <dir>] [--output <path>]
  amplihack alice-qa validate
  amplihack alice-qa list
  amplihack alice-qa run <scenario-id-or-path> [--evidence-dir <dir>] [--timeout-seconds <seconds>] [--prepare-only]
  amplihack tweedle-decode verify <literal-arithmetic-return|literal-arithmetic-return-boundary|simple-if-method-call|simple-if-boundaries|simple-if-player-archive>

Run from the Alice repository root or one of its child directories.
"""


TWEEDLE_DECODE_SCENARIOS = {
    "literal-arithmetic-return": {
        "description": "JSON player archive Tweedle type decodes a literal arithmetic return method",
        "module": "core/story-api-migration",
        "tests": (
            "HistoricalArchiveRoundTripCharacterizationTest#"
            "generatedJsonPlayerArchiveDecodesProgramMethodReturningLiteralArithmetic"
        ),
    },
    "literal-arithmetic-return-boundary": {
        "description": "JSON player archive keeps identifier arithmetic return methods fail-closed",
        "module": "core/story-api-migration",
        "tests": (
            "HistoricalArchiveRoundTripCharacterizationTest#"
            "generatedJsonPlayerArchiveWithIdentifierArithmeticReturnIsRejectedWithoutPartialProgramDecode"
        ),
    },
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


def find_repo_root(start: Path) -> Path | None:
    for candidate in (start, *start.parents):
        runners = candidate / "qa" / "outside-in" / "alice-desktop" / "runners"
        if (runners / "validate-scenarios.sh").is_file() and (runners / "run-scenario.sh").is_file():
            return candidate
    return None


def run_from_repo(root: Path, command: list[str]) -> int:
    return subprocess.run(command, cwd=root, check=False).returncode


def run_tweedle_decode_verification(root: Path, scenario: str) -> int:
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
    submodule_result = subprocess.run(
        ["git", "submodule", "update", "--init", "tweedle-lang"],
        cwd=root,
        check=False,
    )
    if submodule_result.returncode != 0:
        print("FAIL: unable to initialize tweedle-lang submodule", file=sys.stderr)
        return submodule_result.returncode

    test_result = subprocess.run(
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
        cwd=root,
        check=False,
    )
    if test_result.returncode == 0:
        print(f"PASS: {scenario}")
    else:
        print(f"FAIL: {scenario}", file=sys.stderr)
    return test_result.returncode


def main(argv: list[str] | None = None) -> int:
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
        return run_from_repo(
            root,
            [
                sys.executable,
                str(root / "scripts" / "generate-modernization-scorecard.py"),
                *args[1:],
            ],
        )

    if args[0] == "tweedle-decode":
        if len(args) != 3 or args[1] != "verify":
            print("tweedle-decode usage: amplihack tweedle-decode verify <scenario>", file=sys.stderr)
            return 2
        return run_tweedle_decode_verification(root, args[2])

    if args[0] != "alice-qa":
        print(f"unknown command: {args[0]}", file=sys.stderr)
        print(USAGE, end="", file=sys.stderr)
        return 2

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
    if subcommand == "run":
        if len(args) < 3:
            print("alice-qa run requires a scenario id or path", file=sys.stderr)
            return 2
        return run_from_repo(root, [str(runners / "run-scenario.sh"), "run", *args[2:]])

    print(f"unknown alice-qa command: {subcommand}", file=sys.stderr)
    print(USAGE, end="", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
