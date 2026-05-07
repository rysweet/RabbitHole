# CI efficiency notes

This note records the current RabbitHole GitHub Actions shape and the measured
slowest checks, so future CI changes can improve time without weakening checks.

## Current checks

Pull requests to `develop` start these checks at the same time:

- Alice Checkstyle CI
- Alice Test CI
- Alice NetBeans Package CI
- Alice Coverage Reports
- GitGuardian Security Checks

The four repository-owned workflows do not wait on each other. They also cancel
older in-progress pull request runs from the same branch, while `develop` push
runs are kept for history.

## Recent timing sample

Measured from pull request runs created on 2026-05-07 at 04:37 UTC:

| Check | Main work step | Wall time |
| --- | --- | ---: |
| Alice Checkstyle CI | `mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml` | about 1 minute |
| Alice Test CI | `mvn -DincludeSims=false -Dinstall4j.skip clean test` | about 5.5 minutes |
| Alice NetBeans Package CI | `mvn -DincludeSims=false -Dinstall4j.skip -pl netbeans -am package -DskipTests` | about 8.5 minutes |
| Alice Coverage Reports | `mvn -DincludeSims=false -Dinstall4j.skip -Pcoverage verify` | about 10.5 minutes |

Because the checks already run side by side, total pull request wait time is
currently set by coverage. NetBeans packaging is the next longest check.

## Safe improvement targets

- Keep build, test, coverage, NetBeans packaging, and security checks required.
- Prefer changes that make coverage faster or split independent coverage work
  without reducing what is measured.
- Keep stale pull request cancellation, but do not cancel completed or `develop`
  runs.
- Re-measure before and after CI changes using `gh run view <run-id> --json jobs`.
