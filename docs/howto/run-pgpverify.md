# Verify PGP signatures of Maven artifacts (`-Ppgpverify`)

Maven repository checksums prove **integrity** (the bytes were not corrupted in
transit) but not **authenticity** (the bytes came from the expected publisher).
Issue [#980](https://github.com/rysweet/RabbitHole/issues/980) tracks adding PGP
signature verification to close that gap.

This repository wires in the
[`pgpverify-maven-plugin`](https://www.simplify4u.org/pgpverify-maven-plugin/)
through an **opt-in, non-blocking** profile so the default build is never
affected.

## What is configured

- A dedicated `pgpverify` Maven profile in the root `pom.xml`.
- The plugin's `check` goal bound to the `verify` phase.
- A **non-failing (warn) posture** by default:
  `failNoSignature`, `failNoKey`, and `failWeakSignature` are all `false`.
- A keys map at [`.mvn/pgp-keys-map.list`](../../.mvn/pgp-keys-map.list) that
  starts permissive so unsigned `org.alice` `-SNAPSHOT` artifacts and
  as-yet-untrusted third-party artifacts do not fail the build.

Because the profile is only active with `-Ppgpverify`, the default build command
is unchanged and continues to reach `BUILD SUCCESS` with the baseline test
totals.

## How to run it

Initialize the Tweedle grammar submodule first (required by the reactor build):

```bash
git submodule update --init tweedle-lang
```

Run verification as part of `verify`:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  mvn -Ppgpverify -Djava.awt.headless=true verify
```

Or run only the signature check without the rest of the lifecycle:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  mvn -Ppgpverify org.simplify4u.plugins:pgpverify-maven-plugin:check
```

The check downloads `.asc` signatures from the configured repositories and
resolves keys from public keyservers, so it needs network access. In the current
warn posture, missing signatures, missing keys, or keyserver flakiness are logged
as warnings and never fail the build.

## Path to a fail-closed posture

The permissive default is deliberate: building a complete trust map for every
transitive dependency across the five external repositories
(`jogamp.org`, `org.alice`, `org.alice.external`, `thirdparty-releases`,
`atlassian-public`) is a substantial, incremental effort. To harden verification
over time:

1. **Enumerate real key fingerprints.** Run `-Ppgpverify` and review the plugin
   output / generated report to collect the actual signing-key fingerprints per
   artifact.
2. **Tighten `.mvn/pgp-keys-map.list`.** Replace the trailing wildcard
   `* = <key>, noKey, noSig` rule with explicit
   `groupId:artifactId = <fingerprint>` entries. Keep the `org.alice`,
   `org.lgna`, and `edu.cmu.cs.dennisc` reactor artifacts as `noSig` (they are
   unsigned by design; do not change their versions).
3. **Flip the flags to fail-closed.** Set `failNoSignature`, `failNoKey`, and
   `failWeakSignature` to `true` in the `pgpverify` profile once the keys map is
   comprehensive.
4. **Promote to the default build.** When the fail-closed configuration is stable
   in CI under `-Ppgpverify`, move the plugin out of the profile into the default
   `<build><plugins>` so every build verifies authenticity.

## Related

- Checksum integrity enforcement: issue #979.
- Plugin docs: <https://www.simplify4u.org/pgpverify-maven-plugin/>
- Keys map format:
  <https://www.simplify4u.org/pgpverify-maven-plugin/keysmap-format.html>
