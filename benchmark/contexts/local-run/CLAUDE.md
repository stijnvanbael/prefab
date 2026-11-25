When `prefab-test` is a test dependency, verify completed work against the running application as well as tests:
run `mvn test-compile spring-boot:test-run`, use the base URL and docs routes printed at startup, and make an HTTP
request to an affected endpoint. The in-memory backend needs no Docker; application security remains enabled.
