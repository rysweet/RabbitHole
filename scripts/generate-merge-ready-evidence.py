#!/usr/bin/env python3
"""Command wrapper for RabbitHole merge-ready evidence generation."""

import argparse
import sys
from pathlib import Path

from merge_ready_evidence import (
    EvidenceConfig,
    EvidenceError,
    generate_evidence,
    patch_pr_description,
    run_command,
)


def positive_int(value: str) -> int:
    try:
        parsed = int(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("--pr must be a positive integer") from exc
    if parsed <= 0:
        raise argparse.ArgumentTypeError("--pr must be a positive integer")
    return parsed


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="repository root")
    parser.add_argument("--pr", type=positive_int, required=True, help="pull request number")
    parser.add_argument("--repo", help="GitHub repository in OWNER/REPO form")
    parser.add_argument("--base-ref", default="origin/develop", help="base ref for scope review")
    parser.add_argument(
        "--quality-audit-file",
        type=Path,
        required=True,
        help="path to quality-audit output collected before merge readiness",
    )
    parser.add_argument("--scenario-command", default="gadugi-test")
    parser.add_argument("--scenario-directory", type=Path, default=Path("scenarios"))
    parser.add_argument("--scenario-name")
    parser.add_argument("--scenario-config", type=Path)
    parser.add_argument("--scenario-strict", action="store_true")
    parser.add_argument("--patch-pr-description", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    return parser


def main(argv=None) -> int:
    args = build_parser().parse_args(argv)
    root = args.root.resolve()
    quality_file = args.quality_audit_file
    if not quality_file.is_absolute():
        quality_file = root / quality_file
    config = EvidenceConfig(
        root=root,
        pr=args.pr,
        repo=args.repo,
        base_ref=args.base_ref,
        quality_audit_file=quality_file,
        scenario_command=args.scenario_command,
        scenario_directory=args.scenario_directory,
        scenario_name=args.scenario_name,
        scenario_config=args.scenario_config,
        scenario_strict=args.scenario_strict,
        patch_pr_description=args.patch_pr_description,
        dry_run=args.dry_run,
    )
    try:
        evidence = generate_evidence(config)
        print(evidence["markdown"], end="")
        if config.patch_pr_description:
            patch_pr_description(config, evidence, run_command)
    except EvidenceError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
