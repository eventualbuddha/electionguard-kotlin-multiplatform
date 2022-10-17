# ElectionGuard-Kotlin-Multiplatform
ElectionGuard-Kotlin-Multiplatform (EKM) is an experimental attempt to create a multiplatform Kotlin implementation of 
[ElectionGuard](https://github.com/microsoft/electionguard), capable of running
"everywhere" (Android / JVM, iOS / Unix native, and eventually JavaScript-in-browser).

Note that EKM is *not* compatible with ElectionGuard-Python or any other
ElectionGuard implementation. EKM is, however, intended to have full feature-parity
with the Python codebase.

EKM is available under an MIT-style open source license.

*Table of contents*:
- [Incompatibilities with ElectionGuard 1.0](#incompatibilities-with-electionguard-10)
- [Cool features of EKM](#cool-features-of-ekm)
- [What about JavaScript?](#what-about-javascript)
- [API differences from ElectionGuard-Python](#api-differences-from-electionguard-python)
- [Protobuf Serialization](#protobuf-serialization)
- [Workflow and Command Line Interfaces](#workflow)
- [Input Validation](#input-validation)
- [Authors](#authors)

## Incompatibilities with ElectionGuard 1.0

- EKM uses an optimized encoding of an encrypted ElGamal counter, proposed by [Pereira](https://perso.uclouvain.be/olivier.pereira/). Where 
  ElectionGuard 1.0 defines $\mathrm{Encrypt}(g^a, r, m) = \left(g^r, g^{ar}g^m\right)$,
  EKM instead defines $\mathrm{Encrypt}(g^a, r, m) = \left(g^r, g^{a(r + m)}\right)$.
  This allows for one fewer exponentiation per encryption. EKM includes corresponding
  changes in its Chaum-Pedersen proofs and discrete-log engine to support this.

- EKM further optimizes the Chaum-Pedersen proofs with a space-optimization from [Boneh and Shoup](http://toc.cryptobook.us/) that allows
  the larger elements-mod-p to be elided from the proofs because they can be recomputed by the verifier.

- EKM includes a Chaum-Pedersen "range proof", which is a proof that a ciphertext
  corresponds to a plaintext from 0 to a given constant. These are a generalization of the
  earlier 0-or-1 disjunctive proofs. The size of the proof will be
  linear with respect to the size of the constant, and when the constant is "1", 
  the proof will be the same size as the original disjunctive proof.

The above changes are consistent with changes that have been proposed for ElectionGuard 2.0. The following
changes are not:

- EKM does not use JSON for serialization. Instead, it uses [Protocol Buffers](https://en.wikipedia.org/wiki/Protocol_Buffers), a binary format
  that takes roughly half the space of JSON for the same information. EKM includes `.proto` files for all
  the relevant data formats, which could be adopted by other implementations,
  making it easier for future ElectionGuard implementations to have compatible
  data serialization.

- EKM changes how hashes are computed, defining the result of a hash function as
  a 256-bit unsigned integer rather than an element-mod-q. This simplifies the code 
  in a variety of ways. The `UInt256` type is used in a number of other contexts,
  like HMAC keys, allowing those implementations to be independent of the ElGamal group parameters.

## Cool features of EKM

EKM uses Kotlin's "multiplatform" features to support JVM platforms, including
Android, and also to support "native" platforms, including iOS. The primary
difference between these is how big integers are handled. On the JVM, we
use [java.math.BigInteger](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/math/BigInteger.html).
On native platforms, we use [Microsoft HACL*](https://www.microsoft.com/en-us/research/publication/hacl-a-verified-modern-cryptographic-library/). 
With both platforms, we use a variety of optimizations: 

- Pereira's "pow-radix" precomputation, used for common bases like
  the group generator or a public key, replaces modular exponentiation with
  table lookups and multiplies. We support three table sizes. Batch computations
  might then use larger tables and gain greater speedups, while memory-limited
  computations would use smaller tables.


- In this table, we transform the numbers to [Montgomery form](https://en.wikipedia.org/wiki/Montgomery_modular_multiplication), allowing us
  to avoid an expensive modulo operation after each multiply. HACL* has native support
  for this transformation, resulting in significant speedups.
  We also get modest speedups within the JVM.

- ElectionGuard defines two sets of global parameters: using either 3072-bit
  or 4096-bit modular arithmetic. EKM supports both sets of parameters as well as a
  "tiny" set of 32-bit parameters, used to radically speed up the unit tests.

- All the code in EKM is thread-safe, and it's mostly functional. This
  means that you can easily use libraries like [Java Streams](https://docs.oracle.com/javase/tutorial/collections/streams/parallelism.html)
  to do parallel operations on a multicore computer. Similarly, for a voting
  machine, you might take advantage of [Kotlin's coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
  to run an encryption in the background without creating lag on the UI thread.

## What about JavaScript?

We tried to include JavaScript, which you can see at the tag [BEFORE_REMOVING_JS](https://github.com/danwallach/electionguard-kotlin-multiplatform/releases/tag/BEFORE_REMOVING_JS). This
used a big integer library called [kt-math](https://github.com/gciatto/kt-math),
but the performance was not acceptable.

Instead, please check out [ElectionGuard-TypeScript](https://github.com/danwallach/ElectionGuard-TypeScript),
which is fully compatible with ElectionGuard 1.0 and runs very efficiently,
taking advantage of the built-in `bigint` type of modern JavaScript engines.
If we were going to bring back Kotlin/JS as an EKM target platform, we'd probably borrow the "core"
cryptographic classes from ElectionGuard-TypeScript, and build up the rest
of the ballot abstractions in Kotlin.

We note that the Kotlin team is actively developing a [WebAssembly backend](https://youtrack.jetbrains.com/issue/KT-46773).
If this ultimately supports foreign function calls to C functions, then the
"native" version of EKM, including HACL* for big integer arithmetic, could potentially run in a JavaScript WASM engine.
Alternatively, the Kotlin team is working on portable support for [BigInteger and
BigDecimal](https://youtrack.jetbrains.com/issue/KT-20912/BigDecimalBigInteger-types-in-Kotlin-stdlib), which we
could use here when it's ready.

## API differences from ElectionGuard-Python

The biggest and most notable difference is the use of [GroupContext](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/kotlin/electionguard/core/GroupCommon.kt#L117) 
instances. A `GroupContext` provides
all the necessary state to do computation with a group, replacing a series of global variables, in 
the Python code, with instance variables inside the group context. You get a group context by calling 
[productionGroup()](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/kotlin/electionguard/core/Group.kt#L11)
with an optional parameter specifying how much precomputation (and memory use) you're willing to tolerate
in response for more acceleration of the cryptographic primitives. There's also a [tinyGroup()](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonTest/kotlin/electionguard/core/TinyGroup.kt#L23), only
available to unit tests, that operates with 32-bit primes rather than the original 256 or 4096-bit primes. This 
allows the unit tests to run radically faster, and in some cases even discover corner case bugs that would
be unlikely to manifest if the tests were operating with a production group.

As a general rule, we try to use Kotlin's language features to make the code
simpler and cleaner. For example, we use Kotlin's operator overloading such
that math operations on `ElementModP` and `ElementModQ` can be written with
infix notation rather than function calls. We also implemented many 
[extension functions](https://kotlinlang.org/docs/extensions.html), so if you have a value of any EKM type and you type
a period, the IDE's autocomplete menu should offer you a variety of useful
methods.

## Protobuf Serialization
* [Protobuf serialization](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/ProtoSerializationSpec2.md)
* [Election Record serialization for private classes](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/ProtoSerializationPrivate.md)
* [Election Record protobuf directory and file layout](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/ElectionRecord.md)
* [Protobuf serialization (ver 1) and comparison with JSON](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/ProtoSerializationSpec1.md)

## Workflow
* [Workflow and Command Line Programs](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/CommandLineInterface.md)

## Input Validation
* [Input Validation](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/InputValidation.md)

## Authors
- [John Caron](https://github.com/JohnLCaron)
- [Dan S. Wallach](https://www.cs.rice.edu/~dwallach/)