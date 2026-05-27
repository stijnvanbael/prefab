---
id: TASK-234
title: Add Java comment highlighting in MarkdownRenderer
status: Done
assignee: []
created_date: '2026-05-27 06:15'
updated_date: '2026-05-27 06:16'
labels:
  - renderer
  - syntax-highlighting
  - java
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
Extend Java syntax highlighting in fenced java blocks to support // line comments and /* */ block comments, including multi-line block comment state.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Line comments are colorized inside fenced java blocks
- [x] #2 Block comments are colorized inside fenced java blocks
- [x] #3 Multi-line block comment state is preserved across streamed lines
- [x] #4 Existing MarkdownRenderer tests remain green
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added Java comment token type and gray coloring for both // line comments and /* */ block comments in fenced java blocks.

Implemented multi-line block comment state tracking across streamed lines via inJavaBlockComment.

Ensured comment tokens are rendered with precedence over other Java token styles.

Added two tests for line/block comment highlighting and multi-line block comment state; MarkdownRenderer test suite passes.
<!-- SECTION:FINAL_SUMMARY:END -->
