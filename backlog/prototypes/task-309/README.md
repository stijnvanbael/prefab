# TASK-309 use-case prototype

This standalone, non-reactor prototype tests one opt-in vertical-slice operation: importing a batch of products.
The workflow and its local transaction remain developer-authored; `UseCaseProcessor` generates only the Spring
HTTP adapter. The test compiles the generated adapter with the JDK compiler and checks its route and delegation.

Run from the repository root:

```shell
mvn -f backlog/prototypes/task-309/pom.xml test
```

This is deliberately not registered in the root Maven reactor and is not a production module. It is not a proposed
public API; it demonstrates the minimal generator boundary for comparison with the aggregate-centric DDD output.
