---
id: TASK-231
title: Fix hanging prompt stream in MindForgeApi
status: Done
assignee:
  - Copilot
created_date: '2026-05-26 16:41'
updated_date: '2026-05-26 17:00'
labels: []
milestone: cli-with-file-capabilities
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/mind-forge/src/main/java/be/appify/ai/mindforge/application/MindForgeApi.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/mind-forge/src/main/java/be/appify/ai/mindforge/application/TokenStreamAdapter.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/mind-forge/src/main/java/be/appify/ai/mindforge/application/Assistant.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/mind-forge/src/main/java/be/appify/ai/mindforge/application/AgentConfiguration.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate and fix the prompt streaming flow so chat responses emitted through the application API produce tokens for callers and always complete when the model finishes or errors. The current behavior is that the stream returned by `src/main/java/be/appify/ai/mindforge/application/MindForgeApi.java` yields no items and never completes, which blocks CLI consumers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Calling the application prompt API returns a stream that emits model output to its consumer.
- [x] #2 The returned stream completes after the model finishes successfully.
- [x] #3 The returned stream completes after an error instead of hanging indefinitely.
- [x] #4 Automated tests cover the streaming adapter or API behavior for both success and failure cases.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Inspect the current streaming flow around `MindForgeApi`, `TokenStreamAdapter`, and `MessageStream` to confirm where production and completion events are lost.
2. Update the adapter so it registers callbacks and then starts the LangChain4j `TokenStream`, ensuring the model request actually begins.
3. Harden `MessageStream` so a consumer waiting for the next item can observe completion even when the producer completes without enqueueing another message.
4. Add unit tests for successful streaming output and error completion behavior to prevent regressions.
5. Run the relevant Maven tests and record the outcome in the task notes and acceptance criteria.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Confirmed two independent causes for the hanging prompt stream: `TokenStreamAdapter` registered handlers but never called `TokenStream.start()`, and `MessageStream` could block forever if completion happened after the consumer checked `completionFuture` but before `queue.take()` woke up.

Updated `TokenStreamAdapter` to start the LangChain4j stream after wiring callbacks and to emit the final full message only as a fallback when no partial tokens were received, avoiding duplicate full-response output after successful token streaming.

Hardened `MessageStream` by polling with a short timeout and checking for completion between waits so the returned `Stream` always terminates after success or error.

Added unit tests for successful partial-token streaming, fallback full-message completion, error completion, and the `MessageStream` completion race. Verified with `mvn test` and a second focused `mvn -Dtest=MessageStreamTest,TokenStreamAdapterTest test` run; both passed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed the hanging prompt stream exposed by `MindForgeApi.prompt`.

What changed:
- `TokenStreamAdapter` now starts the LangChain4j `TokenStream` after registering its callbacks, so streaming actually begins.
- The adapter now treats the final `onCompleteResponse` text as a fallback only when no partial chunks were emitted, which avoids printing the full response twice after streamed tokens.
- `MessageStream` no longer blocks indefinitely on producer completion races; it periodically checks for completion while waiting for more messages and cleanly terminates the consumer stream once the queue is drained.

Why:
- The previous implementation never invoked `TokenStream.start()`, so no callbacks fired and callers observed an empty, non-terminating stream.
- Even if the token stream completed, `MessageStream` could still hang if completion happened after a consumer decided to block on `queue.take()`.

Tests:
- Added `TokenStreamAdapterTest` covering successful partial streaming, final-message fallback, and error completion.
- Added `MessageStreamTest` covering completion without any final queued message.
- Verified with `mvn test` and a second focused regression run for the new tests.

Risk / follow-up:
- The fix keeps the current public API intact. If you later want lower completion latency than the current short polling interval in `MessageStream`, we can replace it with an explicit completion sentinel approach.
<!-- SECTION:FINAL_SUMMARY:END -->
