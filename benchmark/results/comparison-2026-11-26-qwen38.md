# Prefab benchmark comparison

| | qwen3.8:27b / none<br>`20261126-204151-qwen38-sandboxed-none` | qwen3.8:27b / cheat-sheet<br>`20261126-155010-qwen38-sandboxed-cheat-sheet` | qwen3.8:27b / recipes<br>`20261126-165026-qwen38-sandboxed-recipes` | qwen3.8:27b / local-run<br>`20261126-172744-qwen38-sandboxed-local-run` | qwen3.8:27b / full-docs<br>`20261126-182614-qwen38-sandboxed-full-docs` |
|---|---|---|---|---|---|
| **Pass rate** | **6/6** 100% (95% CI 61-100%) | **6/6** 100% (95% CI 61-100%) | **6/6** 100% (95% CI 61-100%) | **6/6** 100% (95% CI 61-100%) | **6/6** 100% (95% CI 61-100%) |
| Cost | $12,21 | $5,70 | $3,66 | $5,29 | $10,55 |
| Median turns | 72 | 31 | 33 | 31 | 24 |
| Median time to first green build | 9m36s (6/6 attempts) | 3m44s (6/6 attempts) | 2m27s (6/6 attempts) | 2m36s (6/6 attempts) | 9m52s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) | 0 (in 0 attempts) | 0 (in 0 attempts) | 0 (in 0 attempts) | 0 (in 0 attempts) |
| Pass rate, quick tier | 6/6 100% (95% CI 61-100%) | 6/6 100% (95% CI 61-100%) | 6/6 100% (95% CI 61-100%) | 6/6 100% (95% CI 61-100%) | 6/6 100% (95% CI 61-100%) |
| DOC_GAP | 0 | 0 | 0 | 0 | 0 |
| MISLEADING_DIAGNOSTIC | 0 | 0 | 0 | 0 | 0 |
| API_MISUSE | 0 | 0 | 0 | 0 | 0 |
| PLAIN_SPRING_FALLBACK | 0 | 0 | 0 | 0 | 0 |
| TEST_OR_INFRASTRUCTURE | 0 | 0 | 0 | 0 | 0 |
| events-multicast | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-publish-on-create | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| mixin-derived-query | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-create-and-get | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-parent-child | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-validation | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
