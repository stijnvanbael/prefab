---
id: TASK-258
title: Refactor WriterTests for flexible output assertions
status: In Progress
assignee: []
created_date: '2025-07-01 10:00'
updated_date: '2026-07-01 06:44'
labels:
  - "\U0001F9EAtesting"
dependencies: []
ordinal: 0
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace strict exact-match assertions in all WriterTest classes across the project with more lenient, context-aware assertions that verify the essence of the generated output and protect against regressions without being overly brittle.

Current issue: Many tests use `contentsAsUtf8String().isEqualTo(contentsOf(...))` which breaks when formatting or minor implementation details change, even though the functional correctness is preserved. This makes tests maintenance-heavy and prone to false positives.

Goal: Adopt flexible assertion patterns that:
- Verify the presence of critical elements (class names, method signatures, annotations, SQL structures, etc.)
- Check logical correctness without enforcing exact formatting
- Protect against actual regressions while allowing safe refactoring
- Maintain descriptive, readable test names and comments that explain *what* is being verified

Scope: 25 WriterTest files across all modules:
- annotation-processor: ~15 tests
- kafka: ~3 tests
- pubsub: ~2 tests
- sns-sqs: ~2 tests
- terraform: ~1 test
- avro-processor: ~3 tests
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
<!-- SECTION:ACCEPTANCE_CRITERIA:BEGIN -->
- [ ] #1 All 25 WriterTest files have been reviewed
- [ ] #2 Exact-match assertions (`isEqualTo(contentsOf(...))`) replaced with contextual checks (`contains()`, regex matches, structured verification)
- [ ] #3 Tests still verify correct class/method generation, proper annotations, SQL DDL/DML structures, event schema fields, etc.
- [ ] #4 Test method names and comments clearly describe what essence is being protected
- [ ] #5 All tests pass reliably with no flakiness
- [ ] #6 No regression: previously passing scenarios still pass
- [ ] #7 Code follows SOLID principles: assertions are specific, focused, and test one aspect of correctness
<!-- SECTION:ACCEPTANCE_CRITERIA:END -->
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Refactoring Strategy

### Phase 1: Analyze and Plan
1. Examine all 25 *WriterTest files to identify assertion patterns
2. Classify assertions into categories (exact match, contains, regex, structured)
3. Create assertion helper methods where needed for reusable patterns

### Phase 2: Core Modules (annotation-processor)
1. **DbMigrationWriterTest.java** (620 lines) - SQL DDL/DML assertions
   - Replace `isEqualTo(contentsOf(...))` with targeted `contains()` checks
   - Verify table structures, column definitions, index creation without exact formatting

2. **RestWriterTest.java** (446 lines) - REST controller/service generation
   - Verify class generation, method signatures, annotations without exact whitespace
   - Check service injection, repository method calls exist

3. **BuilderWriterTest.java** - Builder pattern generation
   - Verify builder class structure and methods

4. **EventHandlerWriterTest.java** - Event handler generation
   - Verify event handler methods and routing

5. Other annotation-processor tests:
   - PolymorphicConverterWriterTest
   - PolymorphicRestWriterTest
   - AsyncCommitWriterTest
   - AutocompleteRepositoryWriterTest
   - EventSchemaDocumentationWriterTest
   - MongoMigrationWriterTest
   - MongoIndexWriterTest
   - TestClientWriterTest
   - TestJavaFileWriterTest
   - JavaFileWriterTest

### Phase 3: Event/Messaging Modules
1. **Kafka**: KafkaEventTypeRegistrarWriterTest, KafkaConsumerWriterTest, KafkaConsumerConfigWriterTest
2. **PubSub**: PubSubEventTypeRegistrarWriterTest, PubSubSubscriberWriterTest
3. **SNS-SQS**: SqsEventTypeRegistrarWriterTest, SqsSubscriberWriterTest

### Phase 4: Additional Modules
1. **Avro**: EventSchemaFactoryWriterTest, GenericRecordToEventConverterWriterTest, EventToGenericRecordConverterWriterTest
2. **Terraform**: GcpTerraformWriterTest

### Assertion Patterns to Adopt

**Instead of:**
```java
.isEqualTo(contentsOf("path/expected.java"))
```

**Use patterns like:**
```java
.contains("class ClassName")
.contains("public void methodName()")
.contains("@Annotation")
.matchesPattern("pattern")  // for complex patterns
```

### Testing
- Run full test suite after each module
- Commit after each major module completion
- Verify no regressions: all tests should still pass
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
<!-- SECTION:IMPLEMENTATION_NOTES:BEGIN -->
<!-- To be filled in after task completion -->
<!-- SECTION:IMPLEMENTATION_NOTES:END -->

## Comments

<!-- SECTION:COMMENTS:BEGIN -->
See user request context: "I want to replace all assertions that expect the exact output with more lenient assertions that still verify the essence and protect from regressions to make tests more flexible."

Key files to explore first:
- DbMigrationWriterTest.java (uses many exact-match assertions)
- EventSchemaFactoryWriterTest.java (Kafka, PubSub, SQS event type registrars)
- RestWriterTest.java and related REST tests
- AvroEventSchemaFactoryWriterTest.java
<!-- SECTION:COMMENTS:END -->

Starting implementation with analysis phase. Identified key files and assertion patterns. Ready to begin with DbMigrationWriterTest as first target.
<!-- SECTION:NOTES:END -->
