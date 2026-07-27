# Tweedle inherited / library method-call resolution — module spec

> **Status: SPECIFICATION + PINNED DESIRED BEHAVIOR.** This document specifies a
> decoder capability that is **not yet implemented**. It is the design record for
> closing the single largest argument-bearing gap in the Tweedle decode census
> (see [`tweedle-decode-gaps.md`](./tweedle-decode-gaps.md)). The desired behavior
> is pinned by `@Ignore`'d / characterization tests (see
> [Pinned tests](#pinned-tests)); it makes **no** corpus-coverage claim — the
> corpus is 0 % bounded today and stays so until this capability *and* the
> entangled gallery-node-resolution gap are both closed.

## 1. Problem

`docs/tweedle-decode-gaps.md` records that **all 168** argument-bearing decode
fallbacks in the curriculum corpus are the *same* call:

```tweedle
this.setJointedModelResource(resource: <galleryNode>)
```

`StatementDecoder.resolveSameClassMethodByArgumentLabels` (shipped in #1007)
resolves an argument-bearing `this`-call only against methods **declared on the
current user type**. `setJointedModelResource` is **not** declared on the user
type — it is an **inherited story-API library method** on the gallery-model
supertype (e.g. a `org.lgna.story.*` model base), and its `resource:` argument is
a **gallery-node resource** value. Same-class resolution therefore finds zero
candidates and the decoder raises the loud fallback
`unsupportedArgumentBearingExplicitThisMethodCall`.

Closing this gap requires two capabilities that today do not exist in the
headless decoder:

1. **Inherited / library method resolution** — walk the supertype /
   linked-story-API hierarchy, not just the current type.
2. **Resource / gallery-node argument decoding** — decode a resource-typed
   argument into an AST resource expression.

The second capability is the **same** capability whose absence makes the five
`UNREADABLE` corpus projects fail (`ResourceTypeHelper.createInstanceCreation` /
gallery-node resolution), and it is blocked outside the IDE by the headless
gallery-resolution wall. **This spec covers the resolution algorithm and the
decode contract; it does not claim the headless gallery-resolution wall is
removable.**

## 2. Resolution algorithm

Given an argument-bearing method call `target.methodName(label₁: e₁, …, labelₙ: eₙ)`
where `target` is `this` (explicit or implicit) and `declaringType` is the user
type whose body is being decoded:

1. **Same-class first (existing, unchanged).** Attempt
   `resolveSameClassMethodByArgumentLabels(declaringType, methodName, {label₁…labelₙ})`.
   If it returns a unique match, bind and return (current #1007 behavior).
2. **Ascend the hierarchy (new).** If no same-class match, walk the supertype
   chain of `declaringType`:
   `declaringType.getSuperType()`, then *its* supertype, … up to (and including)
   the story-API library base types. At each level collect **non-static**
   candidate methods whose `getName()` equals `methodName`.
3. **Label-set match at each level (new).** For each candidate, require that its
   **required-parameter name-set** equals the call's **argument-label set**
   `{label₁…labelₙ}` exactly (same cardinality, same names). This reuses the
   #1007 label-set-match rule, now applied to inherited/library methods.
4. **Nearest-unique wins.** Resolution stops at the **first (nearest) hierarchy
   level that yields exactly one label-set match**. That single method is the
   resolution result. See the safety property below for the ambiguity rules.
5. **Decode and bind arguments** using the [resource-arg decode
   contract](#4-resourcegallery-node-argument-decode-contract), then emit a
   `MethodInvocation` whose target is a `ThisExpression` for `declaringType` and
   whose method is the resolved (possibly inherited) method.
6. **Loud fallback (unchanged).** If no level yields a unique label-set match, or
   any argument fails to decode / is not assignable to its parameter, the decoder
   still throws `unsupportedArgumentBearingExplicitThisMethodCall`. Resolution is
   **never** a silent best-effort.

## 3. Label-set-match safety property

> **Safety property.** A call is resolved to a method **M** iff **M** is the
> *unique* method, at the *nearest* hierarchy level containing any name match,
> whose required-parameter name-set is exactly equal to the call's argument-label
> set. If the nearest matching level contains **two or more** methods with that
> same label set (an overload the label set cannot disambiguate), resolution
> **fails loudly** rather than guessing.

Rationale and invariants:

- **No silent narrowing.** Tweedle emits **named** arguments; the label set is
  the only structural key available (Tweedle does not re-emit static parameter
  types at the call site). Equality of the *whole* name-set — not subset — means
  a call can never bind to a method that requires a different set of parameters.
- **Nearest-level shadowing.** A subtype method with a matching label set
  **shadows** a supertype method with the same label set; resolution never
  reaches past a level that already uniquely matches. This mirrors normal
  override/shadowing semantics.
- **Ambiguity ⇒ loud fallback.** Two same-level candidates with identical label
  sets are indistinguishable from Tweedle's named-argument encoding, so the
  decoder must not pick one. This preserves the #1007 invariant that ambiguous
  resolution returns `null` → loud fallback.
- **Behavior-compatibility.** Because step 1 (same-class) is tried first and
  unchanged, every call that resolves today resolves identically; the new steps
  only ever convert a *former loud fallback* into a resolved invocation, never
  change an already-resolved call.

## 4. Resource / gallery-node argument decode contract

For each resolved required parameter `p` of method **M** with corresponding
argument expression `eᵢ` (matched by label `p.getName()`):

1. **Non-resource arguments** decode exactly as today via
   `ExpressionDecoder.decodeValueExpression` and must satisfy
   `p.getValueType().isAssignableFrom(decoded.getType())`.
2. **Resource-typed parameters** (parameter value type is a gallery-model
   resource type — e.g. a `org.lgna.story.resources.*` enum such as
   `TerrainResource`, or a gallery-node resource) require decoding a
   **gallery-node argument** into an AST resource expression
   (`ResourceExpression` / an instance-creation produced by the story-API
   resource helper, i.e. the `ResourceTypeHelper.createInstanceCreation` path).
3. **Contract — completeness or loud fallback.** The decoded resource argument
   MUST resolve to a concrete gallery node. If the gallery node cannot be
   resolved (the headless gallery-resolution wall: no node exists for the
   referenced art-gallery model resource), the decode MUST raise the loud
   fallback — it MUST NOT emit a `null`, placeholder, or partially-bound
   resource. This preserves the **"never worse than today"** contract: an archive
   whose gallery node cannot be resolved routes through its in-archive XML
   payload rather than decoding to a lossy AST.
4. **Entanglement note.** Because (3) depends on gallery-node availability, this
   contract is **not satisfiable in the pure-headless census environment** for
   the corpus's `setJointedModelResource(resource:)` calls. That is why closing
   inherited resolution alone flips **no** corpus type onto the bounded path, and
   why this spec makes no corpus-coverage claim.

## 5. Pinned tests

The desired behavior above is pinned by tests that keep CI green:

- **`core/ast` — `TweedleEncoderDecoderTest`:**
  - a **green characterization** test pinning that the inherited
    `this.setJointedModelResource(resource: …)` shape currently raises the loud
    `unsupportedArgumentBearingExplicitThisMethodCall` fallback (today's boundary);
  - an **`@Ignore`'d** test pinning the *desired* resolution of the same call to a
    `MethodInvocation` bound to the inherited `setJointedModelResource` method.
- **`core/story-api-migration` — `HistoricalArchiveRoundTripCharacterizationTest`:**
  - an **`@Ignore`'d** archive round-trip pin expressing the desired end-to-end
    decode of a program/scene type that calls the inherited resource setter.

The `@Ignore`'d tests carry a reason string referencing this spec so the desired
behavior is discoverable and executable the moment the capability lands.

## 6. Relationship to the census

See [`tweedle-decode-gaps.md` §3 "Argument-bearing explicit `this` method
calls"](./tweedle-decode-gaps.md) for the census evidence that all 168
arg-bearing fallbacks are this single inherited call, and for the entangled
`UNREADABLE` gallery-resolution gap this capability shares a root cause with.
