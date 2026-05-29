---
id: TASK-233
title: Extend Java syntax highlighting in MarkdownRenderer
status: Done
assignee: []
created_date: '2026-05-27 05:55'
updated_date: '2026-05-27 05:57'
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
Add highlighting support for Java annotations, string literals, number literals, and method names in fenced java code blocks within MarkdownRenderer.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Annotations are colorized in fenced java blocks
- [x] #2 String and number literals are colorized in fenced java blocks
- [x] #3 Method names are colorized in fenced java blocks
- [x] #4 Existing MarkdownRenderer tests continue to pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Extended Java code lexer to emit explicit tokens for annotations, string literals, number literals, identifiers, and symbols.

Added parser-side method-name highlighting by identifying identifier tokens followed by '(' with optional whitespace.

Added ANSI colors for new token classes and preserved keyword highlighting behavior.

Added MarkdownRendererTest coverage for annotation/string/number/method-name highlighting in fenced java blocks; tests pass with mvn -Dtest=MarkdownRendererTest test.
<!-- SECTION:FINAL_SUMMARY:END -->
