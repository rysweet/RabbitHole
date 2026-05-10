# Run the Silver Thread Tweedle Decoder Round-Trip Test

Use this guide to run and review the Tweedle decoder round-trip silver thread
test for Alice. The test proves that Tweedle encode→decode preserves AST
structural identity for all `NamedUserType` declarations in a real `.a3p`
starter project, entirely headlessly.

For the full contract, see the [Silver Thread Tweedle Decoder Round-Trip Test
reference](../reference/silver-thread-tweedle-decoder-round-trip-test.md).

## Before you start

Confirm the test class and test resource exist:

```bash
test -f core/ide/src/test/java/org/alice/ide/SilverThreadTweedleDecoderRoundTripTest.java && echo "Test class OK" || echo "Test class MISSING"
test -f core/ide/src/test/resources/starters/indiaMinimum.a3p && echo "Starter OK" || echo "Starter MISSING"
```

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the focused test

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadTweedleDecoderRoundTripTest \
  test
```

This command does not require a display server, JavaFX toolkit, or network
access. Do not add `xvfb-run`, Swing automation, or timeout-based success
criteria.

## What this test proves

The existing silver thread tests (`SilverThreadLaunchBuildRunTest`,
`SilverThreadSaveRoundTripTest`) verify `.a3p` archive round-trips — the binary
serialization path. This test exercises the **Tweedle source code** round-trip:

1. Load `indiaMinimum.a3p` from the test classpath.
2. For each `NamedUserType` in the project, encode it to Tweedle source via
   `TweedleEncoderDecoder.encode(type)`.
3. Decode the Tweedle source back to a `NamedUserType` via
   `TweedleEncoderDecoder.decode(tweedleSource, terminals)`.
4. Assert structural equality between the encoded and decoded ASTs.

The Encoder renames Java types to Tweedle types (`Double` → `DecimalNumber`,
`Integer` → `WholeNumber`, etc.), so the test compares the decoded AST against
the **encoded** Tweedle source structure, not the original pre-encode Java AST.

## Review the test

Review `allNamedUserTypesRoundTripThroughTweedleEncoderDecoder` for these
assertions:

| Step | Assertion | Meaning |
| --- | --- | --- |
| Load project | Project is non-null. | Real `.a3p` archive parsed successfully. |
| Encode each type | Tweedle source is non-null and non-empty. | Encoder produced output for each `NamedUserType`. |
| Decode each type | Decoded node is a `NamedUserType`. | Decoder parsed the Tweedle source back to a typed AST node. |
| Class name | Decoded class name matches encoded class name. | Type identity preserved through the round-trip. |
| Method count | Decoded method count matches encoded method count. | No methods lost or duplicated. |
| Method names | Decoded method names match encoded names (set equality). | Method identity preserved; order is not required. |
| Field count | Decoded field count matches encoded field count. | No fields lost or duplicated. |
| Constructor presence | Decoded constructor presence matches encoded. | Constructor shape preserved. |
| Non-empty body | At least one method has statements in its body. | Decoder produced non-trivial method bodies. |
| At least one success | At least one type completed the full round-trip. | Fail-safe against total decoder gaps. |

## Handle UnsupportedTweedleDecodeException

Some `NamedUserType` declarations in a real starter project may use AST
features the Decoder does not yet support (complex field initializers, resource
references, advanced control flow). The test catches
`UnsupportedTweedleDecodeException` per-type and documents the gap without
failing the overall test.

If you see per-type exceptions in the test output, this is expected behavior —
the test is characterizing which types can round-trip today and documenting
which ones cannot.

## Keep the claim narrow

Cite this test only for the headless Tweedle encode→decode structural
round-trip. Do not cite it as evidence for:

- Full Tweedle language support
- `.a3p` archive binary round-trip (owned by `SilverThreadLaunchBuildRunTest`)
- Production save path (owned by `SilverThreadSaveRoundTripTest`)
- 3D rendering correctness
- Drag-and-drop code tile UI
- Gallery or model resource loading
- JavaFX display or scene rendering
- Lesson completion, grading, or assessment
- VM execution events
- Deep AST equality (UUID, object identity, supertype resolution)

Adjacent claims are owned by separate documents:

| Claim | Document |
| --- | --- |
| Headless create → build → run → save → reopen | [Run the Silver Thread Launch-Build-Run Test](./run-silver-thread-launch-build-run-test.md). |
| Production save round-trip | [Run the Silver Thread Save Round-Trip Test](./run-silver-thread-save-round-trip-test.md). |
| Tweedle decode coverage (unit-level) | [Decode Coverage Characterization](../reference/decode-coverage-characterization.md). |
| Silver-thread aggregate status | [Silver-Thread Status Report](../reference/silver-thread-status-report.md). |
