# Modernization corpus manifest

[`modernization-corpus-manifest.json`](./modernization-corpus-manifest.json)
is a small, text-only evidence manifest for representative Alice corpus
coverage. It is intentionally independent of Git LFS and intentionally does not
check in `.a3p`, `.a3w`, `.a3c`, or media payloads.

This manifest is representative evidence only. It is not full historical archive
coverage and must not be described as a replacement for the original Alice
corpus or a complete compatibility archive.

Each entry names the expected generated fixture path, gives a plain description,
and lists generated-fixture expectations that are protected by focused
characterization tests. The paths describe expected fixture shapes; they do not
require the binary fixture files to exist in the repository.

The modernization scorecard treats the manifest as present corpus evidence only
when every entry has:

1. a non-empty repository-relative `path`;
2. a non-empty `description`;
3. a non-empty `generatedFixtureExpectations` list of non-empty strings.

Update the manifest when representative generated fixture coverage changes, and
keep the coverage statement explicit that the scope remains representative and
not full historical archive coverage.
