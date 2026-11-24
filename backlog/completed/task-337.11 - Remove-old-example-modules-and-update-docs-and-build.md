---
id: TASK-337.11
title: Remove old example modules and update docs and build
status: Done
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-24 11:00'
labels:
  - examples
  - docs
dependencies:
  - TASK-337.08
  - TASK-337.09
  - TASK-337.10
parent_task_id: TASK-337
priority: medium
type: chore
ordinal: 182000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Once all variants are in place: delete `examples/{avro,kafka,mongodb,pubsub,sns-sqs,streams}` (and stray `pom.xml.versionsBackup` files), update `<modules>` in the root pom, `release.sh`/publishing exclusions, CI workflows, `readme.md`, the developer guide and any skill/agent docs that point at old example paths. Close TASK-153 and TASK-250 if their example code is gone.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No references to the old example modules remain (grep the repo)
- [x] #2 Full build including all variants passes in CI
- [x] #3 Example modules are still excluded from Maven Central publishing
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
**Removed**: `examples/{avro,kafka,mongodb,pubsub,sns-sqs,streams}` and the tracked `test/pom.xml.versionsBackup`.

**Updated references**: root `pom.xml` modules, `.github/dependabot.yml`, `benchmark/run.sh` (install exclusions),
`.idea/encodings.xml`, `CONTRIBUTING.md`, `readme.md` (the custom-projection and authorization examples now link to
`CustomerOrderHistory` and `Product` in the showcase), `backlog/docs/modules.md` (also lists `showcase-inmemory`),
`backlog/docs/extension-points.md`, `examples/README.md` (test-infrastructure paragraph) and decisions 002, 005 and
006, which pointed at the removed modules.

**Publishing**: no change needed. `release.sh` runs `mvn deploy` on the whole reactor; example modules stay out of
Maven Central because the showcase and `showcase-variant-parent` poms bind `central-publishing-maven-plugin`'s
`injected-central-publishing` execution to `phase none`, so the variants inherit it. CI (`ci.yml`) runs
`mvn verify` on the full reactor, so it covers every variant without a change.

**Left as is on purpose**: the Windows-path fixtures in `TestJavaFileWriterTest` (`examples/avro/...`) and the
`prefab.kafka-example` app name in `PostgresTestEnvironmentPostProcessorTest` are arbitrary strings, not references
to a module; archived and completed tasks keep their history; decisions 005 and 006 keep a one-line note that the
module they described is gone.

**Verification**: the five showcase modules compile and install. `showcase-inmemory` passes (92 tests, 5 skipped for
its declared capability gaps). The author session had no Docker, so the container-backed variants run only in CI.

**Closed with this task**: TASK-153 and TASK-250, whose example code no longer exists (see their notes).
<!-- SECTION:NOTES:END -->
