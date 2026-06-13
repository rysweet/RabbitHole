import importlib.util
import json
import shutil
import sys
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "merge_ready_evidence.py"
SCRATCH_ROOT = REPO_ROOT / "target" / "test-merge-ready-evidence"
DOC_PATH = REPO_ROOT / "docs" / "reference" / "merge-ready-evidence-generator.md"


@lru_cache(maxsize=1)
def load_generator():
    if str(SCRIPT_PATH.parent) not in sys.path:
        sys.path.insert(0, str(SCRIPT_PATH.parent))
    spec = importlib.util.spec_from_file_location("merge_ready_evidence", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class FakeRunner:
    def __init__(self, checks=None, existing_body="Existing PR body") -> None:
        self.calls = []
        self.inputs = []
        self.checks = checks or [
            {"name": "Alice Test CI", "bucket": "pass", "state": "SUCCESS"},
            {"name": "Docs", "bucket": "pass", "state": "SUCCESS"},
        ]
        self.existing_body = existing_body

    def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
        module = load_generator()
        self.calls.append(tuple(args))
        if args[:3] == ["gh", "pr", "view"]:
            return module.CommandResult(
                tuple(args),
                0,
                json.dumps(
                    {
                        "number": 935,
                        "title": "Automate merge-ready evidence",
                        "url": "https://github.com/rysweet/RabbitHole/pull/935",
                        "body": self.existing_body,
                        "headRefName": "feat/issue-935-merge-ready-evidence",
                        "baseRefName": "develop",
                        "isDraft": True,
                        "mergeable": "MERGEABLE",
                        "reviewDecision": "APPROVED",
                        "headRefOid": "52a862f188000000000000000000000000000000",
                        "baseRefOid": "37194ffd3882df1d1c2af8c472e121047a7bc3c8",
                    }
                ),
                "",
            )
        if args[:3] == ["gh", "pr", "checks"]:
            return module.CommandResult(tuple(args), 0, json.dumps(self.checks), "")
        if args[:3] == ["gh", "pr", "edit"]:
            self.inputs.append(input_text)
            return module.CommandResult(tuple(args), 0, "", "")
        if args == ["git", "rev-parse", "HEAD"]:
            return module.CommandResult(
                tuple(args),
                0,
                "52a862f188000000000000000000000000000000\n",
                "",
            )
        if args == ["git", "rev-parse", "origin/develop"]:
            return module.CommandResult(
                tuple(args),
                0,
                "37194ffd3882df1d1c2af8c472e121047a7bc3c8\n",
                "",
            )
        if args == ["git", "status", "--porcelain"]:
            return module.CommandResult(tuple(args), 0, "", "")
        if args[:4] == ["/bin/scenario-evidence", "validate", "--directory", str(REPO_ROOT / "scenarios")]:
            return module.CommandResult(tuple(args), 0, "scenario schema valid\n", "")
        if args[:4] == ["/bin/scenario-evidence", "run", "--directory", str(REPO_ROOT / "scenarios")]:
            return module.CommandResult(tuple(args), 0, "scenario run passed\n", "")
        if args[:3] == ["git", "diff", "--name-status"]:
            return module.CommandResult(
                tuple(args),
                0,
                "\n".join(
                    [
                        "A\tscripts/generate-merge-ready-evidence.py",
                        "A\ttests/test_merge_ready_evidence.py",
                        "A\tdocs/reference/merge-ready-evidence-generator.md",
                    ]
                )
                + "\n",
                "",
            )
        raise AssertionError(f"unexpected command: {args}")


class MergeReadyEvidenceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_generator()
        self.scratch = SCRATCH_ROOT / self._testMethodName
        shutil.rmtree(self.scratch, ignore_errors=True)
        self.scratch.mkdir(parents=True)
        self.quality_file = self.scratch / "quality-audit.txt"
        self.quality_file.write_text(
            "QUALITY AUDIT: CLEAN\nNo high or critical findings remain.\n",
            encoding="utf-8",
        )

    def tearDown(self) -> None:
        shutil.rmtree(self.scratch, ignore_errors=True)

    def config(self, *, dry_run=True, patch=True, quality_file=None):
        return self.module.EvidenceConfig(
            root=REPO_ROOT,
            pr=935,
            repo=None,
            base_ref="origin/develop",
            quality_audit_file=quality_file or self.quality_file,
            scenario_command="scenario-evidence",
            scenario_directory=Path("scenarios"),
            scenario_name=None,
            scenario_config=None,
            scenario_strict=False,
            patch_pr_description=patch,
            dry_run=dry_run,
        )

    def test_dry_run_generates_all_sections_and_skips_pr_mutation(self) -> None:
        runner = FakeRunner()

        evidence = self.module.generate_evidence(
            self.config(dry_run=True),
            runner=runner,
            executable_resolver=lambda command: "/bin/scenario-evidence",
        )
        self.module.patch_pr_description(self.config(dry_run=True), evidence, runner)

        markdown = evidence["markdown"]
        self.assertIn("## Merge-ready evidence", markdown)
        self.assertIn("### Scenario evidence", markdown)
        self.assertIn("### Documentation impact checklist", markdown)
        self.assertIn("### Quality audit summary", markdown)
        self.assertIn("### CI status", markdown)
        self.assertIn("### Scope review", markdown)
        self.assertNotIn(("gh", "pr", "edit"), [call[:3] for call in runner.calls])

    def test_scope_review_fails_when_scenario_commands_dirty_worktree(self) -> None:
        class DirtyAfterScenarioRunner(FakeRunner):
            def __init__(self) -> None:
                super().__init__()
                self.status_calls = 0

            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args == ["git", "status", "--porcelain"]:
                    self.status_calls += 1
                    module = load_generator()
                    output = "" if self.status_calls == 1 else " M evidence.log\n"
                    return module.CommandResult(tuple(args), 0, output, "")
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        with self.assertRaisesRegex(self.module.EvidenceError, "uncommitted changes"):
            self.module.generate_evidence(
                self.config(),
                runner=DirtyAfterScenarioRunner(),
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_missing_quality_audit_evidence_fails_loudly(self) -> None:
        runner = FakeRunner()

        with self.assertRaisesRegex(
            self.module.EvidenceError,
            "quality-audit evidence is required",
        ):
            self.module.generate_evidence(
                self.config(quality_file=self.scratch / "missing.txt"),
                runner=runner,
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_non_passing_ci_blocks_merge_ready_evidence(self) -> None:
        runner = FakeRunner(checks=[{"name": "Alice Test CI", "bucket": "fail"}])

        with self.assertRaisesRegex(self.module.EvidenceError, "CI is not merge-ready"):
            self.module.generate_evidence(
                self.config(),
                runner=runner,
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_skipped_ci_blocks_merge_ready_evidence(self) -> None:
        runner = FakeRunner(checks=[{"name": "Alice Test CI", "bucket": "skipping"}])

        with self.assertRaisesRegex(self.module.EvidenceError, "CI is not merge-ready"):
            self.module.generate_evidence(
                self.config(),
                runner=runner,
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_quality_audit_without_clean_marker_fails_loudly(self) -> None:
        self.quality_file.write_text("Audit completed with no listed findings.\n", encoding="utf-8")
        runner = FakeRunner()

        with self.assertRaisesRegex(self.module.EvidenceError, "not clean"):
            self.module.generate_evidence(
                self.config(),
                runner=runner,
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_quoted_quality_audit_clean_marker_does_not_spoof_clean(self) -> None:
        self.quality_file.write_text(
            "Audit failed: missing 'QUALITY AUDIT: CLEAN'\n",
            encoding="utf-8",
        )

        with self.assertRaisesRegex(self.module.EvidenceError, "not clean"):
            self.module.generate_evidence(
                self.config(),
                runner=FakeRunner(),
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_clean_quality_audit_allows_severity_headings(self) -> None:
        self.quality_file.write_text(
            "QUALITY AUDIT: CLEAN\n## Critical Issues\nNone\n## High Issues\n0\n",
            encoding="utf-8",
        )
        runner = FakeRunner()

        evidence = self.module.generate_evidence(
            self.config(),
            runner=runner,
            executable_resolver=lambda command: "/bin/scenario-evidence",
        )

        self.assertIn("Clean marker present: yes", evidence["markdown"])

    def test_pr_description_patch_replaces_managed_section(self) -> None:
        existing_body = (
            "Intro\n\n"
            f"{self.module.SECTION_START}\nold evidence\n{self.module.SECTION_END}\n"
            "\nFooter"
        )
        runner = FakeRunner(existing_body=existing_body)
        config = self.config(dry_run=False, patch=True)

        evidence = self.module.generate_evidence(
            config,
            runner=runner,
            executable_resolver=lambda command: "/bin/scenario-evidence",
        )
        self.module.patch_pr_description(config, evidence, runner)

        edit_calls = [call for call in runner.calls if call[:3] == ("gh", "pr", "edit")]
        self.assertEqual(1, len(edit_calls))
        self.assertEqual(("gh", "pr", "edit", "935", "--body-file", "-"), edit_calls[0])
        patched_body = runner.inputs[0]
        self.assertIn("Intro", patched_body)
        self.assertIn("Footer", patched_body)
        self.assertIn("## Merge-ready evidence", patched_body)
        self.assertNotIn("old evidence", patched_body)

    def test_pr_description_patch_refuses_changed_body(self) -> None:
        class FreshBodyRunner(FakeRunner):
            def __init__(self) -> None:
                super().__init__(existing_body="stale body")
                self.view_calls = 0

            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args[:3] == ["gh", "pr", "view"]:
                    self.view_calls += 1
                    self.existing_body = "stale body" if self.view_calls == 1 else "fresh body"
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        runner = FreshBodyRunner()
        config = self.config(dry_run=False, patch=True)
        evidence = self.module.generate_evidence(
            config,
            runner=runner,
            executable_resolver=lambda command: "/bin/scenario-evidence",
        )

        with self.assertRaisesRegex(self.module.EvidenceError, "PR body changed"):
            self.module.patch_pr_description(config, evidence, runner)

    def test_pr_description_patch_refuses_changed_head(self) -> None:
        class ChangedHeadRunner(FakeRunner):
            def __init__(self) -> None:
                super().__init__()
                self.view_calls = 0

            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args[:3] == ["gh", "pr", "view"]:
                    self.view_calls += 1
                    result = super().__call__(args, cwd, allowed_exit_codes, input_text)
                    if self.view_calls > 1:
                        data = json.loads(result.stdout)
                        data["headRefOid"] = "changed0000000000000000000000000000000000"
                        module = load_generator()
                        return module.CommandResult(tuple(args), 0, json.dumps(data), "")
                    return result
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        runner = ChangedHeadRunner()
        config = self.config(dry_run=False, patch=True)
        evidence = self.module.generate_evidence(
            config,
            runner=runner,
            executable_resolver=lambda command: "/bin/scenario-evidence",
        )

        with self.assertRaisesRegex(self.module.EvidenceError, "PR head changed"):
            self.module.patch_pr_description(config, evidence, runner)

    def test_pr_description_patch_refuses_changed_base(self) -> None:
        class ChangedBaseRunner(FakeRunner):
            def __init__(self) -> None:
                super().__init__()
                self.view_calls = 0

            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args[:3] == ["gh", "pr", "view"]:
                    self.view_calls += 1
                    result = super().__call__(args, cwd, allowed_exit_codes, input_text)
                    if self.view_calls > 1:
                        data = json.loads(result.stdout)
                        data["baseRefOid"] = "changedbase000000000000000000000000000000"
                        module = load_generator()
                        return module.CommandResult(tuple(args), 0, json.dumps(data), "")
                    return result
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        runner = ChangedBaseRunner()
        config = self.config(dry_run=False, patch=True)
        evidence = self.module.generate_evidence(
            config,
            runner=runner,
            executable_resolver=lambda command: "/bin/scenario-evidence",
        )

        with self.assertRaisesRegex(self.module.EvidenceError, "PR base changed"):
            self.module.patch_pr_description(config, evidence, runner)

    def test_pr_description_patch_preserves_backslashes(self) -> None:
        existing_body = f"{self.module.SECTION_START}\nold\n{self.module.SECTION_END}\n"
        evidence_markdown = (
            f"{self.module.SECTION_START}\n"
            "## Merge-ready evidence\n"
            r"Path: C:\Users\runner\audit.txt"
            f"\n{self.module.SECTION_END}\n"
        )

        patched = self.module.patch_body(existing_body, evidence_markdown)

        self.assertIn(r"C:\Users\runner\audit.txt", patched)

    def test_pr_description_patch_rejects_malformed_markers(self) -> None:
        with self.assertRaisesRegex(ValueError, "managed evidence markers"):
            self.module.patch_body("Intro\n" + self.module.SECTION_START, "new evidence")

    def test_scope_review_fails_when_local_head_is_not_pr_head(self) -> None:
        class MismatchRunner(FakeRunner):
            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args == ["git", "rev-parse", "HEAD"]:
                    module = load_generator()
                    return module.CommandResult(tuple(args), 0, "different00000000000000000000000000000000\n", "")
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        with self.assertRaisesRegex(self.module.EvidenceError, "local HEAD does not match"):
            self.module.generate_evidence(
                self.config(),
                runner=MismatchRunner(),
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_scope_review_fails_when_config_base_differs_from_pr_base(self) -> None:
        config = self.module.EvidenceConfig(
            **{**self.config().__dict__, "base_ref": "origin/main"}
        )

        with self.assertRaisesRegex(self.module.EvidenceError, "does not match PR base"):
            self.module.generate_evidence(
                config,
                runner=FakeRunner(),
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_scope_review_fails_when_base_oid_differs_from_pr_base(self) -> None:
        class StaleBaseRunner(FakeRunner):
            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args == ["git", "rev-parse", "origin/develop"]:
                    module = load_generator()
                    return module.CommandResult(tuple(args), 0, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n", "")
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        with self.assertRaisesRegex(self.module.EvidenceError, "base ref does not match PR base commit"):
            self.module.generate_evidence(
                self.config(),
                runner=StaleBaseRunner(),
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_scope_review_fails_when_worktree_has_uncommitted_changes(self) -> None:
        class DirtyRunner(FakeRunner):
            def __call__(self, args, cwd, allowed_exit_codes=(0,), input_text=None):
                if args == ["git", "status", "--porcelain"]:
                    module = load_generator()
                    return module.CommandResult(tuple(args), 0, " M scripts/merge_ready_evidence.py\n", "")
                return super().__call__(args, cwd, allowed_exit_codes, input_text)

        with self.assertRaisesRegex(self.module.EvidenceError, "uncommitted changes"):
            self.module.generate_evidence(
                self.config(),
                runner=DirtyRunner(),
                executable_resolver=lambda command: "/bin/scenario-evidence",
            )

    def test_generated_markdown_sanitizes_markers_fences_and_secrets(self) -> None:
        classic_token = "gh" + "p_" + ("1" * 36)
        actions_token = "gh" + "s_" + ("1" * 36)
        private_key = (
            "-----BEGIN "
            + "PRIVATE KEY-----\nsecret\n-----END "
            + "PRIVATE KEY-----"
        )
        github_token_key = "GITHUB_" + "TOKEN"
        aws_secret_key = "AWS_" + "SECRET_ACCESS_KEY"
        basic_credential = "dXNl" + "cjpwYXNz"
        text = (
            f'{github_token_key}=abc123\napi_key = abc\n{{"{aws_secret_key}":"xyz"}}\n'
            f"Authorization: Basic {basic_credential}\nPRIVATE_KEY=raw\n--access-token flag-secret\n"
            f"{classic_token}\n{actions_token}\n{private_key}\n````text\n"
            + self.module.SECTION_END
        )
        sanitized = self.module.fenced_excerpt(text)

        self.assertIn("GITHUB_TOKEN=[REDACTED]", sanitized)
        self.assertIn("api_key = [REDACTED]", sanitized)
        self.assertIn(f'"{aws_secret_key}":"[REDACTED]"', sanitized)
        self.assertIn("--access-token [REDACTED]", sanitized)
        self.assertIn("Authorization: [REDACTED]", sanitized)
        self.assertIn("PRIVATE_KEY=[REDACTED]", sanitized)
        quoted_password = self.module.fenced_excerpt('PASSWORD=\"alpha beta gamma\"')
        self.assertIn("PASSWORD=[REDACTED]", quoted_password)
        self.assertNotIn("alpha beta gamma", quoted_password)
        spaced_password = self.module.fenced_excerpt("PASSWORD=alpha beta gamma")
        self.assertIn("PASSWORD=[REDACTED]", spaced_password)
        self.assertNotIn("alpha beta gamma", spaced_password)
        comma_key = self.module.fenced_excerpt("API_KEY=abc,def")
        self.assertIn("API_KEY=[REDACTED]", comma_key)
        self.assertNotIn("abc,def", comma_key)
        credential_url = self.module.fenced_excerpt(
            "SERVICE_URL=proto://alice:" + "s3" + "cr3t@example.invalid/prod"
        )
        self.assertIn("proto://[REDACTED]:[REDACTED]@example.invalid/prod", credential_url)
        self.assertNotIn("alice:" + "s3" + "cr3t", credential_url)
        connection_string = self.module.fenced_excerpt(
            "CONNECTION_STRING=DefaultEndpointsProtocol=https;Account" + "Key=abc123==;EndpointSuffix=example.invalid"
        )
        self.assertIn("Account" + "Key=[REDACTED]", connection_string)
        self.assertNotIn("abc123", connection_string)
        redis_url = self.module.fenced_excerpt("CACHE_URL=proto://:" + "pass" + "word@example.invalid/0")
        self.assertIn("proto://[REDACTED]@example.invalid/0", redis_url)
        self.assertNotIn("pass" + "word", redis_url)
        sql_connection = self.module.fenced_excerpt(
            "SQL=Server=db;User Id=alice;Pwd=" + "s3" + "cr3t;"
        )
        self.assertIn("Pwd=[REDACTED]", sql_connection)
        self.assertNotIn("s3" + "cr3t", sql_connection)
        sas = self.module.fenced_excerpt(
            "SharedAccess" + "Signature=sv=1&sig=" + "sec" + "ret"
        )
        self.assertIn("SharedAccess" + "Signature=[REDACTED]", sas)
        self.assertIn("sig=[REDACTED]", sas)
        self.assertNotIn("sec" + "ret", sas)
        signed_url = self.module.fenced_excerpt(
            "url=https://example.test/file?X-Amz-" + "Signature=abcdef" + "123456&Signature=abcdef" + "123456"
        )
        self.assertIn("X-Amz-Signature=[REDACTED]", signed_url)
        self.assertIn("Signature=[REDACTED]", signed_url)
        self.assertNotIn("abcdef" + "123456", signed_url)
        storage_key = self.module.fenced_excerpt("SharedAccess" + "Key=abc123==")
        self.assertIn("SharedAccessKey=[REDACTED]", storage_key)
        self.assertNotIn("abc123", storage_key)
        flag_secret = self.module.fenced_excerpt("--pass" + "word 'alpha beta gamma'")
        self.assertIn("--password [REDACTED]", flag_secret)
        self.assertNotIn("alpha beta gamma", flag_secret)
        access_key = self.module.fenced_excerpt("AWS_ACCESS_" + "KEY_ID=AKIAEXAMPLE")
        self.assertIn("AWS_ACCESS_" + "KEY_ID=[REDACTED]", access_key)
        self.assertNotIn("AKIAEXAMPLE", access_key)
        split_flag = self.module.fenced_excerpt("--access-" + "key AKIAEXAMPLE")
        self.assertIn("--access-key [REDACTED]", split_flag)
        self.assertNotIn("AKIAEXAMPLE", split_flag)
        self.assertNotIn(basic_credential, sanitized)
        self.assertNotIn(classic_token, sanitized)
        self.assertNotIn(actions_token, sanitized)
        self.assertNotIn(private_key, sanitized)
        self.assertNotIn(self.module.SECTION_END, sanitized)
        self.assertNotIn("`", sanitized)

    def test_command_text_redacts_split_secret_flags(self) -> None:
        rendered = self.module.command_text(
            ["tool", "--github-token", "abc123", "--client-secret=xyz"]
        )

        self.assertIn("--github-token [REDACTED]", rendered)
        self.assertIn("--client-secret=[REDACTED]", rendered)
        self.assertNotIn("abc123", rendered)
        self.assertNotIn("xyz", rendered)

    def test_reference_document_uses_neutral_language(self) -> None:
        text = DOC_PATH.read_text(encoding="utf-8")

        self.assertIn("scripts/generate-merge-ready-evidence.py", text)
        self.assertNotRegex(text.lower(), r"gadugi|copilot|amplihack")


if __name__ == "__main__":
    unittest.main()
