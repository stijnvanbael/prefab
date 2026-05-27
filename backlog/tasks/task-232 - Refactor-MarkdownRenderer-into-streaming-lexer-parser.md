---
id: TASK-232
title: Refactor MarkdownRenderer into streaming lexer parser
status: Done
assignee: []
created_date: '2026-05-27 05:39'
updated_date: '2026-05-27 05:51'
labels:
  - renderer
  - markdown
  - streaming
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/mind-forge/src/main/java/be/appify/ai/mindforge/infrastructure/MarkdownRenderer.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/mind-forge/src/test/java/be/appify/ai/mindforge/infrastructure/MarkdownRendererTest.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace ad-hoc regex-based MarkdownRenderer logic with a streaming lexer/parser that preserves parser state across streamed chunks and supports markdown features with code fence handling plus basic Java highlighting in fenced java blocks.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Renderer parses markdown incrementally across chunk boundaries
- [x] #2 Code fences toggle correctly and render with visible block delimiters
- [x] #3 Basic Java keyword highlighting works inside fenced java blocks
- [x] #4 Existing markdown behaviors for headers, bullets, bold, italic, inline code, and links still work
- [x] #5 Unit tests cover streaming chunk scenarios and pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Follow-up requested: current implementation still considered not a true lexer/parser. Refactoring to explicit tokenization + parsing pipeline.

Completed follow-up refactor to explicit lexer/parser stages: lexInlineLine -> parseInlineTokens and lexJavaLine -> renderJavaTokens.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refactored MarkdownRenderer into a stateful streaming lexer/parser with chunk buffering and parser state across calls.

Added code-fence parsing with explicit block markers and maintained in/out code-block state.

Implemented basic Java keyword highlighting inside fenced java blocks.

Added flush() API and integrated it in CliClient to emit pending partial lines after stream completion.

Expanded MarkdownRendererTest with streaming chunk boundary and java highlighting coverage; test suite passes.

Follow-up completed: MarkdownRenderer now uses explicit token types and parser state transitions rather than direct character formatting logic.
<!-- SECTION:FINAL_SUMMARY:END -->
