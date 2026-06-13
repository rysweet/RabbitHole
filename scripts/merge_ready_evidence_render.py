"""Rendering and sanitization helpers for merge-ready evidence."""

from __future__ import annotations

import re
from typing import Optional


SECTION_START = "<!-- merge-ready-evidence:start -->"
SECTION_END = "<!-- merge-ready-evidence:end -->"
SECRET_RE = re.compile(
    r"(?i)([\"']?[A-Z0-9_]*(?:TOKEN|PASSWORD|SECRET|ACCESS[_-]?KEY|API[_-]?KEY|PRIVATE[_-]?KEY|AUTHORIZATION)[A-Z0-9_]*[\"']?\s*[:=]\s*)(\"[^\"]*\"|'[^']*'|[^\n\r]+)"
)
SECRET_FLAG_RE = re.compile(
    r"(?i)(--[A-Z0-9-]*(?:token|password|secret|access-?key|api-?key|private-?key|authorization)[A-Z0-9-]*(?:=|\s+))(\"[^\"]*\"|'[^']*'|\S+)"
)
SECRET_FLAG_ARG_RE = re.compile(
    r"(?i)^--[A-Z0-9-]*(?:token|password|secret|access-?key|api-?key|private-?key|authorization)[A-Z0-9-]*$"
)
GITHUB_TOKEN_RE = re.compile(
    r"\b(?:(?:ghp|ghs|ghu|gho|ghr)_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,})\b"
)
PEM_PRIVATE_KEY_RE = re.compile(
    r"-----BEGIN [A-Z ]*PRIVATE KEY-----.*?-----END [A-Z ]*PRIVATE KEY-----",
    re.DOTALL,
)
CREDENTIAL_URL_RE = re.compile(
    r"(?i)\b([a-z][a-z0-9+.-]*://)(?:([^/\s:@]*):)?([^@\s/]+)@"
)
ACCOUNT_KEY_RE = re.compile(r"(?i)(AccountKey=)[^;\s]+")
CONNECTION_SECRET_RE = re.compile(
    r"(?i)\b(Pwd|Password|SharedAccessKey|SharedAccessSignature|X-Amz-Signature|X-Goog-Signature|Signature|sig)=([^;\s&]+)"
)


class PatchBodyError(ValueError):
    """Raised when an existing PR body has malformed managed evidence markers."""


def redact_text(text: str) -> str:
    text = PEM_PRIVATE_KEY_RE.sub("[REDACTED PRIVATE KEY]", text)
    text = CREDENTIAL_URL_RE.sub(redact_credential_url, text)
    text = ACCOUNT_KEY_RE.sub(r"\1[REDACTED]", text)
    text = CONNECTION_SECRET_RE.sub(lambda match: f"{match.group(1)}=[REDACTED]", text)
    text = GITHUB_TOKEN_RE.sub("[REDACTED]", text)
    text = re.sub(r"(?i)\bBearer\s+[A-Za-z0-9._~+/=-]+", "Bearer [REDACTED]", text)
    text = re.sub(
        r"(?i)([\"']?AUTHORIZATION[\"']?\s*[:=]\s*[\"']?)[^\"'\n\r,}]+",
        r"\1[REDACTED]",
        text,
    )
    text = SECRET_RE.sub(redact_key_value_secret, text)
    return SECRET_FLAG_RE.sub(lambda match: f"{match.group(1)}[REDACTED]", text)


def redact_credential_url(match: re.Match[str]) -> str:
    username = match.group(2)
    if username:
        return f"{match.group(1)}[REDACTED]:[REDACTED]@"
    return f"{match.group(1)}[REDACTED]@"


def redact_key_value_secret(match: re.Match[str]) -> str:
    value = match.group(2)
    if value.startswith('"'):
        return f'{match.group(1)}"[REDACTED]"'
    if value.startswith("'"):
        return f"{match.group(1)}'[REDACTED]'"
    return f"{match.group(1)}[REDACTED]"


def is_secret_flag_arg(text: str) -> bool:
    return bool(SECRET_FLAG_ARG_RE.match(text))


def sanitize_evidence_text(text: str) -> str:
    sanitized = redact_text(text)
    sanitized = sanitized.replace(SECTION_START, "[managed-section-start-redacted]")
    sanitized = sanitized.replace(SECTION_END, "[managed-section-end-redacted]")
    return sanitized.replace("`", "ˋ")


def fenced_excerpt(text: str, max_lines: int = 12) -> str:
    lines = sanitize_evidence_text(text).splitlines()[:max_lines]
    if not lines:
        lines = ["(no output)"]
    if len(text.splitlines()) > max_lines:
        lines.append("...")
    return "\n".join(lines)


def render_scenario(scenario: dict) -> list[str]:
    return [
        "### Scenario evidence",
        "",
        f"- Validate command: `{sanitize_evidence_text(scenario['validate_command'])}`",
        "```text",
        fenced_excerpt(scenario["validate_output"]),
        "```",
        f"- Run command: `{sanitize_evidence_text(scenario['run_command'])}`",
        "```text",
        fenced_excerpt(scenario["run_output"]),
        "```",
        "",
    ]


def render_docs_impact(docs: dict) -> list[str]:
    return [
        "### Documentation impact checklist",
        "",
        f"- Outcome: **{docs['outcome']}**",
        f"- Documentation changed: {'yes' if docs['docs_changed'] else 'no'}",
        f"- Documentation likely required: {'yes' if docs['docs_likely_required'] else 'no'}",
        f"- Rationale: {sanitize_evidence_text(docs['rationale'])}",
        "",
    ]


def render_quality(quality: dict) -> list[str]:
    lines = [
        "### Quality audit summary",
        "",
        f"- Evidence file: `{sanitize_evidence_text(quality['path'])}`",
        f"- Clean marker present: {'yes' if quality['clean'] else 'no'}",
        "",
    ]
    lines.extend(f"- {sanitize_evidence_text(line)}" for line in quality["summary_lines"])
    lines.append("")
    return lines


def render_ci(checks: list[dict]) -> list[str]:
    lines = ["### CI status", ""]
    for check in checks:
        name = sanitize_evidence_text(check.get("name", "(unnamed)"))
        state = sanitize_evidence_text(str(check.get("bucket") or check.get("state")))
        lines.append(f"- {name}: {state}")
    lines.append("")
    return lines


def render_scope(scope: dict) -> list[str]:
    lines = ["### Scope review", "", f"- Base ref: `{sanitize_evidence_text(scope['base_ref'])}`", ""]
    for entry in scope["changed_files"]:
        previous = entry.get("previous_path", "")
        prefix = sanitize_evidence_text(previous + " → " if previous else "")
        lines.append(f"- {sanitize_evidence_text(entry['status'])} {prefix}{sanitize_evidence_text(entry['path'])}")
    lines.append("")
    return lines


def render_markdown(evidence: dict) -> str:
    pr = evidence["pr"]
    lines = [
        SECTION_START,
        "## Merge-ready evidence",
        "",
        f"Generated: {evidence['generated_at']}",
        f"PR: {sanitize_evidence_text(pr['url'])}",
        f"Branch: `{sanitize_evidence_text(pr['headRefName'])}` → `{sanitize_evidence_text(pr['baseRefName'])}`",
        "",
    ]
    lines.extend(render_scenario(evidence["scenario"]))
    lines.extend(render_docs_impact(evidence["docs_impact"]))
    lines.extend(render_quality(evidence["quality_audit"]))
    lines.extend(render_ci(evidence["ci_status"]))
    lines.extend(render_scope(evidence["scope_review"]))
    lines.append(SECTION_END)
    return "\n".join(lines) + "\n"


def patch_body(existing_body: Optional[str], evidence_markdown: str) -> str:
    body = existing_body or ""
    start_count = body.count(SECTION_START)
    end_count = body.count(SECTION_END)
    if start_count or end_count:
        if start_count != 1 or end_count != 1:
            raise PatchBodyError("PR body has duplicate or incomplete managed evidence markers")
        start = body.index(SECTION_START)
        end = body.index(SECTION_END)
        if end < start:
            raise PatchBodyError("PR body has reversed managed evidence markers")
        end += len(SECTION_END)
        return body[:start] + evidence_markdown.strip() + body[end:]
    separator = "\n\n" if body.strip() else ""
    return body.rstrip() + separator + evidence_markdown.strip() + "\n"
