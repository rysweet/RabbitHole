# Forbidden pattern inventory

Use `scripts/inventory-forbidden-patterns.py` to inventory tracked source files
for modernization follow-up candidates. The script is for triage: it reports
candidate locations and known false positives, but owners still decide which
findings require code changes.

```bash
python3 scripts/inventory-forbidden-patterns.py --format markdown
python3 scripts/inventory-forbidden-patterns.py --format json
```

The scanner reports:

- broad `Throwable` catch blocks
- `printStackTrace()` calls
- `System.exit(...)` calls
- catch blocks that log and then continue without an explicit flow change
- `TODO` and `HACK` markers

Java code-region patterns ignore strings, line comments, block comments, and
text blocks. `TODO` and `HACK` markers intentionally include comments because
comment markers are part of the inventory.

Do not commit generated inventory output. Put current snapshots in issue or pull
request comments, and keep durable cleanup guidance in maintained docs.
