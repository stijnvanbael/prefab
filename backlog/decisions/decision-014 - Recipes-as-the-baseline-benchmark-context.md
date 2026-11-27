---
id: decision-014
title: Recipes as the baseline benchmark context
date: '2026-11-27'
status: accepted
related_tasks:
  - TASK-392
  - TASK-346
  - TASK-401
---

## Context

The agent benchmark compares documentation setups ("contexts") to decide which guidance helps agents build Prefab
applications. Until now new guidance was compared against `cheat-sheet` or `full-docs`, and `none` served as the
reference.

TASK-392 recorded all five contexts on the local baseline model, Qwen 3.8 27B, with six quick tasks and one attempt
each. Every context passed 6/6. `recipes` (the cheat sheet plus on-demand verified recipes) had the lowest median
agent time (5m30s against 8m32s for `cheat-sheet`, 7m53s for `local-run` and 17m52s for `full-docs`), the fewest
input tokens (3.2M against 4.7M to 14.9M) and was the fastest setup on five of six tasks against both `cheat-sheet` and
`local-run`.

That is not a statistically clear win: one attempt per cell, quick tasks only, a pass rate that does not separate the
setups, and machine load that varied between runs. Five of six tasks favouring one setup happens by chance about 11%
of the time if the setups were equal.

## Decision

1. `recipes` is the **baseline context**: new aids and documentation changes are measured against it, not against
   `cheat-sheet` or `full-docs`.
2. `recipes` contains the cheat sheet, so adopting it loses nothing the cheat sheet gave; the decision rests on
   "never worse, usually faster and cheaper", not on a significant pass-rate difference.
3. `none` stays a reference recorded once per model. `cheat-sheet` and `full-docs` are kept as contexts for
   comparisons but are no longer the default yardstick.
4. The decision is revisited if `--repeat 3` runs or the feature and brownfield tiers (TASK-389) show `recipes` behind
   another setup.

## Consequences

- Benchmark documentation and new benchmark tasks compare against `recipes`. TASK-401 investigates aids combined with
  recipes on that basis.
- The CLI default `--context none` is unchanged; the baseline is a convention for comparisons, not a default that
  would silently change existing commands.
- Recipes have to stay verified and current (TASK-346): the baseline is only as good as the recipes it loads.
