# Prefab examples

`showcase` is a shop application (catalog, customers, orders, invoices, sales analytics) that shows Prefab on the
reference stack: **Postgres + Kafka (JSON)**. The `showcase-<technology>` modules are variants: they contain only a
pom, configuration and a capabilities file, and run the *same* sources and tests on another technology.
See [decision-007](../backlog/decisions/decision-007%20-%20One-showcase-recompiled-per-technology-variant.md) for how
that works and why.

## Technology coverage

Not every feature works on every stack. A variant lists what it lacks in
`src/test/resources/showcase-capabilities.yml`; every gap carries a reason and the backlog task that closes it:

```yaml
variant: showcase-pubsub
unsupported:
  STREAMS:
    reason: no Pub/Sub backend for Prefab Streams
    task: TASK-341
```

- A test that needs a capability declares `@RequiresCapability(Capability.STREAMS)`. On a variant without it, the test
  is skipped with a message like
  `showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-341)`.
- Capabilities whose code lives in optional source folders (streams, Avro) have no compiled tests on variants that lack
  them. `CapabilityGapsTest` reports one skipped test per unsupported capability instead, so gaps show up in every build.
- An unknown capability, or a gap without a reason or backlog task, fails the build.

| Symbol | Meaning |
|:---:|---|
| ✅ | supported, covered by the showcase tests |
| ❌ | gap, linked to the backlog task that tracks it |

`CoverageMatrixTest` fails when this table differs from the capability files. On a mismatch it prints the expected
table, so after changing a capability file, paste that between the markers.

<!-- coverage-matrix:begin -->
| Capability | showcase | showcase-mongodb |
| --- | :---: | :---: |
| FULL_TEXT_SEARCH | ✅ | ✅ |
| RANGE_FILTER | ✅ | ✅ |
| POLYMORPHIC_AGGREGATES | ✅ | ✅ |
| BINARY_DOWNLOAD | ✅ | ✅ |
| ORDERED_DELIVERY | ✅ | ✅ |
| STREAMS | ✅ | ✅ |
| AVRO | ✅ | ✅ |
| SECURITY | ✅ | ✅ |
| OPENAPI | ✅ | ✅ |
| ASYNCAPI | ✅ | ✅ |
<!-- coverage-matrix:end -->

## Adding a variant

1. Create `examples/showcase-<technology>` with a pom (parent `showcase-variant-parent`, only the technology
   dependencies), `application.yml`, `application-test.yml` and `showcase-capabilities.yml`.
2. Register the module in the root `pom.xml` and add a `Showcase tests (<technology>)` run configuration in `.run/`.
3. Run `CoverageMatrixTest` and paste the expected table into this file.
