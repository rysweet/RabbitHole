# Tutorial: Trace a Player Archive Unsupported This-Call Diagnostic

This tutorial walks through the JSON player archive diagnostic for an unsupported
argument-bearing explicit `this` Tweedle method call.

The goal is to prove this behavior:

```text
A JSON .a3w archive that declares Program as its player type fails closed when
Program contains `this.helper(value: 1)` inside a `caller` method, and the
IOException names Program plus the unsupported decoder reason and call-site
context.
```

## 1. Start with the unsupported call

Use a small Tweedle program:

```java
class Program extends SProgram {
  void caller() {
    this.helper(value: 1);
  }

  void helper(WholeNumber value) {
  }
}
```

The important syntax is the labeled argument on explicit `this`:

```java
this.helper(value: 1);
```

That call remains unsupported. The bounded stable decoder reason is:

```text
Tweedle argument-bearing explicit this method calls
```

The separate call-site context is:

```text
caller.this.helper
```

## 2. Add one decodable sibling

Use a simple sibling type:

```java
class DecodedSibling extends SScene {
}
```

The sibling is not a success claim for the archive. It only gives the archive
reader useful context to report alongside the unsupported `Program` type.

## 3. Package both types in a JSON player archive

Create a temporary `.a3w` archive with:

```text
version.txt
manifest.json
src/Program.twe
src/DecodedSibling.twe
```

The manifest names `Program` as the player type and references both Tweedle
sources as manifest-declared type references.

## 4. Read through IoUtilities

Read through the public production API:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

The archive reader fails closed because the expected player type is unsupported.
It does not return a partially decoded `Project`.

## 5. Check the diagnostic

Assert stable message fragments:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Program"));
assertTrue(message.contains("DecodedSibling"));
assertTrue(message.contains("unsupported"));
assertTrue(message.contains("argument-bearing explicit this method calls"));
assertTrue(message.contains("caller.this.helper"));
```

Those assertions prove that the archive failure identifies the expected program
type, preserves decoded sibling context, names the unsupported type set, and
surfaces the decoder reason plus the call-site context.

## 6. Avoid broader claims

Do not add tutorial text or tests that imply support for:

- argument-bearing calls;
- labeled argument binding;
- optional parameters;
- overload resolution;
- non-`this` targets;
- implicit receiver calls;
- full Tweedle/player decode.

The finished behavior is clearer failure reporting for an unsupported boundary,
not new method-call decode support.
