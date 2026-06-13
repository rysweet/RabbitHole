#!/usr/bin/env python3
"""Inventory pre-existing forbidden-pattern candidates from tracked files."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import Counter
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable


SCHEMA_VERSION = 1
SOURCE_SUFFIXES = {
    ".java",
    ".xml",
    ".properties",
    ".md",
    ".py",
    ".sh",
    ".yml",
    ".yaml",
    ".txt",
}
SCANNER_PATHS = {
    "scripts/inventory-forbidden-patterns.py",
    "tests/test_forbidden_pattern_inventory.py",
}
PATTERNS = {
    "broad-throwable-catch": re.compile(
        r"\bcatch\s*\(\s*(?:final\s+)?(?:java\.lang\.)?Throwable\b"
    ),
    "print-stack-trace": re.compile(r"\.printStackTrace\s*\("),
    "todo-hack-marker": re.compile(r"\b(?:TODO|HACK)\b"),
}
LOG_CALL_RE = re.compile(
    r"(?:\b(?:LOGGER|logger|log|Log)\s*\.\s*"
    r"(?:debug|error|fatal|info|log|severe|warn|warning)\s*\()"
    r"|(?:\bLogger\s*\.\s*"
    r"(?:debug|errln|error|fatal|info|log|outln|severe|throwable|warn|warning)\s*\()"
    r"|(?:\bSystem\.(?:err|out)\.println\s*\()"
)
COMMENT_OR_TEXT_PREFIX_RE = re.compile(
    r"^\s*(?://|/\*|\*|#|<!--|\*|\"|')"
)
JAVA_CATCH_RE = re.compile(r"\bcatch\s*\(")
FOCUS_RULES = (
    ("vm-exception-formatting", ("exceptionformatter", "/vm/", "virtualmachine")),
    ("scene-editor", ("sceneeditor", "scene-editor", "scene editor")),
    ("program-imp", ("programimp", "program imp")),
    ("migration-registry", ("story-api-migration", "migrationregistry", "migration")),
    ("project-loading", ("projectloader", "project loading", "/project/io/", "loader")),
)


@dataclass(frozen=True)
class Finding:
    pattern: str
    severity: str
    disposition: str
    reason: str
    owner_module: str
    path: str
    line: int
    focus_areas: tuple[str, ...]
    text: str


def markdown_cell(value: object) -> str:
    text = str(value).replace("\r\n", "\n").replace("\r", "\n")
    text = " ".join(part.strip() for part in text.split("\n"))
    return text.replace("|", r"\|")


def tracked_repo_files(root: Path) -> tuple[str, ...]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise ValueError(
            f"git ls-files failed while collecting tracked files (exit {result.returncode})"
        )
    return tuple(line for line in result.stdout.splitlines() if line)


def resolve_repo_root(path: Path) -> Path:
    candidate = path.resolve()
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        cwd=candidate,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        return Path(result.stdout.strip()).resolve()
    return candidate


def source_kind(path: str) -> str:
    if "/src/main/java/" in path:
        return "production-java"
    if "/src/test/java/" in path or path.startswith("tests/"):
        return "test"
    if path.startswith("docs/") or path.endswith(".md"):
        return "documentation"
    if path.endswith((".properties", ".xml", ".yml", ".yaml", ".txt")):
        return "configuration-or-resource"
    if path.startswith("scripts/") or path.endswith((".py", ".sh")):
        return "tooling"
    return "other"


def owner_module(path: str) -> str:
    parts = Path(path).parts
    if len(parts) >= 2 and parts[0] in {"core", "core-nonfree", "external"}:
        return f"{parts[0]}/{parts[1]}"
    if parts and parts[0] in {"alice-ide", "netbeans", "installer", "scripts", "tests"}:
        return parts[0]
    return parts[0] if parts else "."


def focus_areas_for(path: str, text: str = "") -> tuple[str, ...]:
    haystack = f"{path}\n{text}".lower()
    areas = [
        name
        for name, needles in FOCUS_RULES
        if any(needle in haystack for needle in needles)
    ]
    return tuple(areas)


def is_palette_placeholder(path: str, line: str) -> bool:
    return (
        path.startswith("netbeans/src/main/resources/org/alice/netbeans/palette/")
        and "HINT_html-" in line
        and "TODO" in line
    )


def classify(pattern: str, path: str, line: str) -> tuple[str, str, str]:
    kind = source_kind(path)
    stripped = line.strip()
    if pattern != "todo-hack-marker" and (
        kind in {"documentation", "configuration-or-resource"}
        or COMMENT_OR_TEXT_PREFIX_RE.search(stripped)
    ):
        return "false-positive", "none", "documentation, resource, or commented example"
    if pattern == "todo-hack-marker" and is_palette_placeholder(path, line):
        return "false-positive", "none", "NetBeans palette user-code placeholder"
    if kind == "test":
        return "candidate", "medium", "test code requires review before cleanup"
    if kind == "production-java":
        severity = "high" if pattern != "todo-hack-marker" else "medium"
        return "candidate", severity, "production Java source"
    if pattern == "todo-hack-marker":
        return "candidate", "low", f"{kind} marker requires owner triage"
    return "candidate", "medium", f"{kind} usage requires owner triage"


def scan_line_patterns(path: str, lines: list[str], patterns: Iterable[str] | None = None) -> list[Finding]:
    selected_patterns = set(patterns) if patterns is not None else set(PATTERNS)
    findings: list[Finding] = []
    for line_number, line in enumerate(lines, start=1):
        for pattern, regex in PATTERNS.items():
            if pattern not in selected_patterns:
                continue
            if not regex.search(line):
                continue
            disposition, severity, reason = classify(pattern, path, line)
            findings.append(
                Finding(
                    pattern=pattern,
                    severity=severity,
                    disposition=disposition,
                    reason=reason,
                    owner_module=owner_module(path),
                    path=path,
                    line=line_number,
                    focus_areas=focus_areas_for(path, line),
                    text=line.strip(),
                )
            )
    return findings


def brace_delta(text: str) -> int:
    return text.count("{") - text.count("}")


def strip_block_comments(text: str) -> str:
    return re.sub(r"/\*.*?\*/", lambda match: "\n" * match.group(0).count("\n"), text, flags=re.DOTALL)


def mask_string_literals(text: str) -> str:
    chars: list[str] = []
    quote: str | None = None
    escaped = False
    for char in text:
        if quote is None:
            if char in {'"', "'"}:
                quote = char
                chars.append(char)
            else:
                chars.append(char)
            continue
        if escaped:
            escaped = False
            chars.append(" ")
        elif char == "\\":
            escaped = True
            chars.append(" ")
        elif char == quote:
            quote = None
            chars.append(char)
        elif char == "\n":
            chars.append("\n")
        else:
            chars.append(" ")
    return "".join(chars)


def strip_line_comments(text: str) -> str:
    lines = []
    for line in text.splitlines():
        masked = mask_string_literals(line)
        comment_index = masked.find("//")
        lines.append(line[:comment_index] if comment_index >= 0 else line)
    return "\n".join(lines)


def find_matching(text: str, start: int, open_char: str, close_char: str) -> int:
    depth = 0
    for index in range(start, len(text)):
        char = text[index]
        if char == open_char:
            depth += 1
        elif char == close_char:
            depth -= 1
            if depth == 0:
                return index
    return -1


def catch_body(block_lines: list[str]) -> str:
    text = "\n".join(block_lines)
    catch_start = text.find("catch")
    catch_text = text[catch_start:] if catch_start >= 0 else text
    open_index = catch_text.find("{")
    if open_index < 0:
        return catch_text
    close_index = find_matching(catch_text, open_index, "{", "}")
    if close_index < 0:
        return catch_text[open_index + 1 :]
    return catch_text[open_index + 1 : close_index]


def has_top_level_flow_change(text: str) -> bool:
    depth = 0
    statement = []
    for char in text:
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
        if depth == 0:
            statement.append(char)
            if char == ";":
                stripped = "".join(statement).strip()
                if re.match(r"^(?:throw|return|break|continue)\b|^System\.exit\s*\(", stripped):
                    return True
                statement = []
    stripped = "".join(statement).strip()
    return bool(re.match(r"^(?:throw|return|break|continue)\b|^System\.exit\s*\(", stripped))


def full_if_else_always_exits(text: str) -> bool:
    stripped = text.strip()
    if not stripped.startswith("if"):
        return False
    condition_start = stripped.find("(")
    if condition_start < 0:
        return False
    condition_end = find_matching(stripped, condition_start, "(", ")")
    if condition_end < 0:
        return False
    true_start = stripped.find("{", condition_end)
    if true_start < 0:
        return False
    true_end = find_matching(stripped, true_start, "{", "}")
    if true_end < 0:
        return False
    remainder = stripped[true_end + 1 :].strip()
    if not remainder.startswith("else"):
        return False
    false_start = remainder.find("{")
    if false_start < 0:
        return False
    false_end = find_matching(remainder, false_start, "{", "}")
    if false_end < 0 or remainder[false_end + 1 :].strip():
        return False
    return fragment_always_exits(stripped[true_start + 1 : true_end]) and fragment_always_exits(remainder[false_start + 1 : false_end])


def fragment_always_exits(text: str) -> bool:
    masked = mask_string_literals(text)
    return has_top_level_flow_change(masked) or full_if_else_always_exits(masked)


def iter_catch_blocks(lines: list[str]) -> Iterable[tuple[int, list[str]]]:
    line_index = 0
    while line_index < len(lines):
        line = lines[line_index]
        if not JAVA_CATCH_RE.search(line):
            line_index += 1
            continue
        catch_matches = list(JAVA_CATCH_RE.finditer(line))
        if len(catch_matches) > 1:
            for match_index, match in enumerate(catch_matches):
                next_start = (
                    catch_matches[match_index + 1].start()
                    if match_index + 1 < len(catch_matches)
                    else len(line)
                )
                block_lines = [line[match.start():next_start]]
                depth = brace_delta(block_lines[0])
                scan_index = line_index + 1
                while depth > 0 and scan_index < len(lines):
                    next_line = lines[scan_index]
                    if depth <= 1 and JAVA_CATCH_RE.search(next_line):
                        break
                    block_lines.append(next_line)
                    depth += brace_delta(next_line)
                    scan_index += 1
                yield line_index + 1, block_lines
            line_index += 1
            continue
        block_lines = [line]
        catch_start = line.find("catch")
        catch_fragment = line[catch_start:] if catch_start >= 0 else line
        depth = brace_delta(catch_fragment)
        started = "{" in catch_fragment
        scan_index = line_index + 1
        while scan_index < len(lines):
            if started and depth <= 0:
                break
            next_line = lines[scan_index]
            if started and depth <= 1 and JAVA_CATCH_RE.search(next_line):
                break
            block_lines.append(next_line)
            started = started or "{" in next_line
            depth += brace_delta(next_line)
            scan_index += 1
            if started and depth <= 0:
                break
        yield line_index + 1, block_lines
        line_index = max(scan_index, line_index + 1)


def has_unconditional_flow_change(block_lines: list[str]) -> bool:
    return fragment_always_exits(catch_body(block_lines))


def has_full_conditional_flow_change(block_lines: list[str]) -> bool:
    return False


def scan_log_and_continue(path: str, lines: list[str]) -> list[Finding]:
    if not path.endswith(".java"):
        return []
    findings: list[Finding] = []
    for line_number, block_lines in iter_catch_blocks(lines):
        block_text = "\n".join(block_lines)
        if not LOG_CALL_RE.search(block_text) or has_unconditional_flow_change(block_lines) or has_full_conditional_flow_change(block_lines):
            continue
        disposition, severity, reason = classify("log-and-continue", path, block_lines[0])
        if disposition != "false-positive" and source_kind(path) == "production-java":
            severity = "high"
            reason = "catch block logs without an explicit throw, return, break, or continue"
        findings.append(
            Finding(
                pattern="log-and-continue",
                severity=severity,
                disposition=disposition,
                reason=reason,
                owner_module=owner_module(path),
                path=path,
                line=line_number,
                focus_areas=focus_areas_for(path, block_text),
                text=block_lines[0].strip(),
            )
        )
    return findings


def scan_text(path: str, text: str) -> list[Finding]:
    original_lines = text.splitlines()
    code_lines = strip_line_comments(strip_block_comments(text)).splitlines()
    return (
        scan_line_patterns(path, original_lines, patterns=("todo-hack-marker",))
        + scan_line_patterns(path, code_lines, patterns=("broad-throwable-catch", "print-stack-trace"))
        + scan_log_and_continue(path, code_lines)
    )


def collect_findings(root: Path, paths: Iterable[str] | None = None) -> list[Finding]:
    selected_paths = tuple(paths) if paths is not None else tracked_repo_files(root)
    findings: list[Finding] = []
    for path in selected_paths:
        if path in SCANNER_PATHS or Path(path).suffix not in SOURCE_SUFFIXES:
            continue
        absolute_path = root / path
        if not absolute_path.is_file():
            continue
        try:
            text = absolute_path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            text = absolute_path.read_text(encoding="utf-8", errors="replace")
        findings.extend(scan_text(path, text))
    return sorted(findings, key=lambda item: (item.path, item.line, item.pattern))


def summarize(findings: list[Finding]) -> dict[str, object]:
    candidates = [finding for finding in findings if finding.disposition == "candidate"]
    false_positives = [
        finding for finding in findings if finding.disposition == "false-positive"
    ]
    focus_counter: Counter[str] = Counter(
        area for finding in candidates for area in finding.focus_areas
    )
    return {
        "totalFindings": len(findings),
        "candidates": len(candidates),
        "falsePositives": len(false_positives),
        "byPattern": dict(sorted(Counter(f.pattern for f in candidates).items())),
        "bySeverity": dict(sorted(Counter(f.severity for f in candidates).items())),
        "byOwnerModule": dict(sorted(Counter(f.owner_module for f in candidates).items())),
        "byFocusArea": dict(sorted(focus_counter.items())),
    }


def inventory_document(findings: list[Finding]) -> dict[str, object]:
    return {
        "schemaVersion": SCHEMA_VERSION,
        "summary": summarize(findings),
        "findings": [asdict(finding) for finding in findings],
    }


def render_summary_table(title: str, rows: dict[str, object]) -> list[str]:
    lines = [f"## {title}", "", "| Category | Count |", "| --- | ---: |"]
    for key, value in rows.items():
        lines.append(f"| {markdown_cell(key)} | {value} |")
    if not rows:
        lines.append("| None | 0 |")
    lines.append("")
    return lines


def render_findings_table(title: str, findings: list[Finding], limit: int) -> list[str]:
    lines = [
        f"## {title}",
        "",
        "| Severity | Pattern | Owner module | Location | Reason |",
        "| --- | --- | --- | --- | --- |",
    ]
    for finding in findings[:limit]:
        location = f"{finding.path}:{finding.line}"
        lines.append(
            "| "
            + " | ".join(
                markdown_cell(value)
                for value in (
                    finding.severity,
                    finding.pattern,
                    finding.owner_module,
                    location,
                    finding.reason,
                )
            )
            + " |"
        )
    if not findings:
        lines.append("| None | None | None | None | None |")
    elif len(findings) > limit:
        lines.append(f"| ... | ... | ... | ... | {len(findings) - limit} more omitted |")
    lines.append("")
    return lines


def render_markdown(findings: list[Finding], limit: int) -> str:
    document = inventory_document(findings)
    summary = document["summary"]
    candidates = [finding for finding in findings if finding.disposition == "candidate"]
    false_positives = [
        finding for finding in findings if finding.disposition == "false-positive"
    ]
    high_confidence = [finding for finding in candidates if finding.severity == "high"]
    lines = [
        "# Forbidden Pattern Inventory",
        "",
        "Generated from tracked repository files by `scripts/inventory-forbidden-patterns.py`.",
        "Use this output in issue or pull-request comments; do not commit point-in-time inventories.",
        "",
        f"- Total findings: {summary['totalFindings']}",
        f"- Candidate findings: {summary['candidates']}",
        f"- False positives: {summary['falsePositives']}",
        "",
    ]
    lines.extend(render_summary_table("Candidate findings by severity", summary["bySeverity"]))
    lines.extend(render_summary_table("Candidate findings by owner module", summary["byOwnerModule"]))
    lines.extend(render_summary_table("Candidate findings by pattern", summary["byPattern"]))
    lines.extend(render_summary_table("Recent-work focus areas", summary["byFocusArea"]))
    lines.extend(render_findings_table("High-confidence follow-up candidates", high_confidence, limit))
    lines.extend(render_findings_table("False positives", false_positives, limit))
    return "\n".join(lines).rstrip() + "\n"


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Inventory tracked forbidden-pattern candidates for issue/PR triage."
    )
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="repository root")
    parser.add_argument(
        "--format",
        choices=("json", "markdown"),
        default="markdown",
        help="output format",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=50,
        help="maximum rows in each markdown findings table",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    findings = collect_findings(resolve_repo_root(args.root))
    if args.format == "json":
        print(json.dumps(inventory_document(findings), indent=2, sort_keys=True))
    else:
        print(render_markdown(findings, args.limit), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
