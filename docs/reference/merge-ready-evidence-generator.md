# Merge-ready evidence generator

Use the merge-ready evidence generator before converting a reviewed pull request
out of draft. The command gathers the evidence that reviewers need in one
managed pull request description section instead of committing local reports.

```bash
python3 scripts/generate-merge-ready-evidence.py \
  --pr <pull-request-number> \
  --scenario-directory <scenario-directory> \
  --quality-audit-file <quality-audit-output> \
  --patch-pr-description
```

Use `--dry-run` to print the managed section without editing the pull request.
The command fails loudly when required evidence is unavailable or not clean:

- scenario validation and scenario run evidence
- quality audit evidence
- GitHub check results
- local scope review against the configured base ref
- pull request access for description patching

The generated section includes a documentation impact checklist, quality audit
summary, check results, changed-file scope review, and scenario evidence. Keep
the generated text in the pull request description. Do not add generated
evidence files, local logs, or branch-specific reports to Git.
