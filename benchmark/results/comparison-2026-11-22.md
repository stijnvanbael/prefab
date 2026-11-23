# Prefab benchmark comparison

| | claude-haiku-5-5 / none<br>`20261122-082637-claude-haiku-5-5-none` | claude-haiku-5-5 / full-docs<br>`20261122-091726-claude-haiku-5-5-full-docs` | claude-haiku-5-5 / full-docs<br>`20261122-235409-claude-haiku-5-5-full-docs` | claude-haiku-5-5 / docs-folder<br>`20261122-191511-claude-haiku-5-5-docs-folder` | claude-sonnet-5-5 / full-docs<br>`20261122-195459-claude-sonnet-5-5-full-docs` |
|---|---|---|---|---|---|
| **Pass rate** | **30/32** 94% (95% CI 80-98%) | **31/32** 97% (95% CI 84-99%) | **31/32** 97% (95% CI 84-99%) | **29/32** 91% (95% CI 76-97%) | **32/32** 100% (95% CI 89-100%) |
| Cost | $2.12 | $6.42 | $6.65 | $0.77 | $15.08 |
| Median turns | 24 | 13 | 13 | 18 | 6 |
| DOC_GAP | 0 | 0 | 0 | 1 | 0 |
| MISLEADING_DIAGNOSTIC | 0 | 0 | 0 | 0 | 0 |
| API_MISUSE | 0 | 0 | 0 | 0 | 0 |
| PLAIN_SPRING_FALLBACK | 2 | 0 | 1 | 1 | 0 |
| TEST_OR_INFRASTRUCTURE | 0 | 1 | 0 | 1 | 0 |
| audit-trail | PASS 1/1 | FAIL 0/1 infra | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| avro-avsc-contract | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| avro-publish-and-handle | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| binary-upload-download | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| diagnostic-fix-build | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-create-from-event | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-multicast | FAIL 0/1 spring | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-publish-on-create | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-sealed-hierarchy | PASS 1/1 | PASS 1/1 | PASS 1/1 | FAIL 0/1 doc | PASS 1/1 |
| events-update-by-reference | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| list-autocomplete | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| list-filters | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| list-full-text-search | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| list-paged-sorted | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| list-range-filter | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| mixin-custom-sql | PASS 1/1 | PASS 1/1 | PASS 1/1 | FAIL 0/1 infra | PASS 1/1 |
| mixin-derived-query | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| mongodb-crud | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| persistence-evolve-schema | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-computed-field | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-create-and-get | FAIL 0/1 spring | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-custom-action | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-parent-child | PASS 1/1 | PASS 1/1 | FAIL 0/1 spring | FAIL 0/1 spring | PASS 1/1 |
| rest-update-and-delete | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-validation | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-value-object | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| security-public-endpoints | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| security-role-based | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| tenancy-isolation | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| testing-domain-events-unit | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| testing-integration-tests | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| view-model | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 | PASS 1/1 |
