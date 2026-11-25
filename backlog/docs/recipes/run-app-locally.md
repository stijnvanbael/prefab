# Run an application locally without infrastructure

**Budget:** 750 tokens. **Verified example:** [showcase application](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/ShowcaseApplication.java), [local demo seed](../../../examples/showcase-inmemory/src/test/java/be/appify/prefab/example/showcase/ShowcaseLocalDemoData.java) and [runtime smoke test](../../../examples/showcase-inmemory/src/test/java/be/appify/prefab/example/showcase/LocalApplicationSmokeTest.java).

Add `prefab-test` with test scope, then from the application directory run:

```shell
mvn test-compile spring-boot:test-run
```

Spring Boot starts the main application using the test classpath. Prefab's in-memory repositories and event bus replace the database and broker. Events still use the platform serializers and reach the application's own handlers. State is temporary and disappears when the process exits.

Use the startup log's base URL to call an affected endpoint and open the available Swagger UI, OpenAPI JSON and AsyncAPI routes. The in-memory backend does not disable security. Configure local authentication for your app; the showcase uses HTTP Basic and Spring Boot's generated development password in its `local` profile. Keep production JWT security enabled.

To seed a local demo, add a test-scoped `ApplicationRunner` guarded by an app-specific profile such as `local-demo`; save records using generated repositories. The showcase variant's test-source auto-configuration seeds one category only when that profile is active. Start it with `mvn test-compile spring-boot:test-run -Dspring-boot.run.profiles=local,local-demo`. Do not put demo records or permissive security settings in production sources.
