# Prefab benchmark comparison

| | claude-haiku-5-5 / none<br>`20261121-173455-claude-haiku-5-5-none` | claude-haiku-5-5 / full-docs<br>`20261121-175404-claude-haiku-5-5-full-docs` | claude-sonnet-5-5 / full-docs<br>`20261121-180102-claude-sonnet-5-5-full-docs` |
|---|---|---|---|
| **Pass rate** | **8/8** 100% (95% CI 68-100%) | **7/8** 88% (95% CI 53-98%) | **6/8** 75% (95% CI 41-93%) |
| Cost | $0.96 | $1.74 | $3.99 |
| Median turns | 43 | 12 | 6 |
| DOC_GAP | 0 | 0 | 0 |
| MISLEADING_DIAGNOSTIC | 0 | 0 | 0 |
| API_MISUSE | 0 | 1 | 1 |
| PLAIN_SPRING_FALLBACK | 0 | 0 | 0 |
| TEST_OR_INFRASTRUCTURE | 0 | 0 | 1 |
| binary-upload-download | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-multicast | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| events-sealed-hierarchy | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| list-range-filter | PASS 1/1 | PASS 1/1 | FAIL 0/1 infra |
| persistence-evolve-schema | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| rest-value-object | PASS 1/1 | PASS 1/1 | PASS 1/1 |
| security-public-endpoints | PASS 1/1 | FAIL 0/1 api | FAIL 0/1 api |
| testing-domain-events-unit | PASS 1/1 | PASS 1/1 | PASS 1/1 |
