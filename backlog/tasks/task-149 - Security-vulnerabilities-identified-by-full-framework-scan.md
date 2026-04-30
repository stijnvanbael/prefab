---
id: TASK-149
title: Security vulnerabilities identified by full framework scan
status: To Do
assignee: [ ]
created_date: '2026-04-30 09:20'
updated_date: '2026-04-30 09:20'
labels:
  - security
dependencies: [ ]
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A full security scan of the Prefab framework was performed on 2026-04-30. The scan covered:
- Static code analysis of all Java source files
- Dependency vulnerability check via GitHub Advisory Database
- Review of security configuration (Spring Security, OAuth2)
- Review of serialization and deserialization patterns
- Review of logging practices
- Review of example application configuration

**Dependency scan result**: No known CVEs were found in direct dependencies
(`commons-text 1.15.0`, `avro 1.11.4`, `commons-io 2.21.0`, `commons-compress 1.28.0`,
`jsqlparser 5.3`, `spring-boot 4.0.3`, `spring-cloud-aws 4.0.0`, `springdoc 3.0.2`).

**Code-level vulnerabilities** were found in four areas and are documented below as acceptance criteria.
Each criterion corresponds to one finding and its recommended fix.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 **[HIGH] VUL-001 – Unsafe class loading from SQS message Subject header (CWE-470)**

  **Location**: `core/src/main/java/be/appify/prefab/core/sns/SqsDeserializer.java:93`

  **Root cause**: `deserializeJson()` calls `Class.forName(typeName)` where `typeName` is read verbatim
  from the SNS envelope `Subject` field (line 81). An attacker who can publish to the backing SQS queue
  can set `Subject` to any fully-qualified class name, causing its static initialiser to execute during
  loading and allowing classpath enumeration.

  **Fix analysis**:
  The `SqsDeserializer` already has access to `SerializationRegistry`, which holds all event types that
  Prefab is aware of. The same approach used by `KafkaJsonTypeResolver` (an explicit `Map<String, Class<?>>`
  populated at startup) should be applied here.

  Concretely:
  1. Add a `Map<String, Class<?>> allowedTypes` field to `SqsDeserializer`, populated via a new
     `registerType(String typeName, Class<?> type)` method.
  2. In `deserializeJson()`, replace the unconstrained `Class.forName(typeName)` call with a lookup in
     `allowedTypes`. If the name is absent, throw `IllegalArgumentException` (do not fall back to
     `Class.forName`).
  3. The generated `SqsSubscriberConfiguration` (produced by `SnsSubscriberWriter`) must call
     `registerType(event.getClass().getName(), eventClass)` for each event type it subscribes to,
     mirroring what `KafkaConfiguration` already does for `KafkaJsonTypeResolver`.

- [ ] #2 **[HIGH] VUL-002 – Unsafe class loading from Pub/Sub message attribute (CWE-470)**

  **Location**: `core/src/main/java/be/appify/prefab/core/pubsub/PubSubUtil.java:168`

  **Root cause**: `consumeTyped()` calls `Class.forName(typeName)` where `typeName` is extracted from
  the Pub/Sub message attribute `"type"` (line 165). Any publisher that can write to the topic controls
  this value.

  **Fix analysis**:
  `PubSubUtil` is a Spring `@Component` with access to all registered beans. Apply the same allowlist
  pattern as VUL-001:
  1. Replace the `ConcurrentMap<String, Class<?>> messageTypes` cache (which is currently populated
     on first use) with a pre-populated allowlist.
  2. Add a `registerType(String typeName, Class<?> type)` method to `PubSubUtil`.
  3. The generated `PubSubSubscriberConfiguration` (produced by `PubSubSubscriberWriter`) already calls
     `pubSubUtil.subscribe(...)` per topic; extend that generated call to also invoke
     `pubSubUtil.registerType(EventClass.class.getName(), EventClass.class)` for each event class.
  4. In `consumeTyped()`, look up `typeName` in the pre-populated map; throw
     `IllegalArgumentException` instead of calling `Class.forName` for unknown names.

- [ ] #3 **[MEDIUM] VUL-003 – Unsafe class loading from Avro schema full name (CWE-470)**

  **Location**: `core/src/main/java/be/appify/prefab/core/kafka/DynamicDeserializer.java:80`

  **Root cause**: `toEvent()` calls `Class.forName(resolveClassName(schema.getFullName()))` where the
  schema is embedded in the Kafka message payload. A malicious message with a crafted Avro schema can
  name any class on the classpath.

  **Fix analysis**:
  `DynamicDeserializer` already receives a `KafkaJsonTypeResolver` (line 42), whose `types` map
  (`Map<String, Class<?>>`) is populated via `registerType(topic, type)` for every known topic. The
  same registered types provide a natural allowlist for Avro class names.
  1. Expose a `Set<Class<?>> registeredTypes()` accessor on `KafkaJsonTypeResolver` (or pass the
     map reference directly to `DynamicDeserializer`).
  2. In `toEvent()`, after calling `resolveClassName(schema.getFullName())`, verify the resolved class
     name is among the registered types before calling `Class.forName`. If it is not, throw
     `IllegalArgumentException` with the offending full name.
  3. No generated code changes are required because `KafkaConfiguration` (generated by the processor)
     already calls `resolver.registerType(topic, EventClass.class)`.

- [ ] #4 **[MEDIUM] VUL-004 – CSRF protection unconditionally disabled (CWE-352)**

  **Location**: `security/src/main/java/be/appify/prefab/core/security/WebSecurityConfiguration.java:28`

  **Root cause**: `.csrf(CsrfConfigurer::disable)` removes all CSRF protection regardless of the
  request type. If any future endpoint uses session cookies the application becomes vulnerable to
  cross-site request forgery.

  **Fix analysis**:
  Prefab's default security posture is stateless OAuth2 (Bearer tokens from `oauth2ResourceServer`).
  CSRF is only needed for session-based flows. Two acceptable paths:
  - **Option A (recommended — stateless API)**: Keep CSRF disabled but add
    `.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))` to make the intent explicit and
    prevent accidental session creation. Add Javadoc explaining that disabling CSRF is safe because
    no session cookies are issued.
  - **Option B (mixed browser + API)**: Replace `CsrfConfigurer::disable` with a custom
    `CsrfTokenRequestAttributeHandler` that ignores CSRF for requests carrying an
    `Authorization: Bearer` header, leaving it active for browser sessions.

  The simplest correct fix is Option A. Add `SessionCreationPolicy.STATELESS` to the generated
  filter chain and document the reasoning in the Javadoc of `securityFilterChain`.

- [ ] #5 **[MEDIUM] VUL-005 – Sensitive message body logged on retry errors (CWE-532)**

  **Locations**:
  - `core/src/main/java/be/appify/prefab/core/sns/SqsUtil.java:245`
  - `core/src/main/java/be/appify/prefab/core/pubsub/PubSubUtil.java:151`

  **Root cause**: On every failed processing attempt the full raw message body is written to the log
  at WARN level. Payloads frequently contain PII (names, emails, financial data) that must not appear
  in log aggregation systems.

  **Fix analysis**:
  Replace the raw body argument with a truncated/redacted helper. A simple, reusable approach:
  1. Add a private static helper method `truncate(String body, int maxLength)` in both classes
     (or extract to a shared `MessageLogging` utility in `core/util`):
     ```java
     private static String truncate(String body, int maxLength) {
         if (body == null) return "<null>";
         return body.length() <= maxLength
             ? body
             : body.substring(0, maxLength) + "...[truncated " + (body.length() - maxLength) + " chars]";
     }
     ```
  2. In `SqsUtil.java:245` change `message.body()` to `truncate(message.body(), 200)`.
  3. In `PubSubUtil.java:151` change `pubsubMessage.getData().toStringUtf8()` to
     `truncate(pubsubMessage.getData().toStringUtf8(), 200)`.

  200 characters is enough for diagnostic context (topic, event type header) without exposing full
  payload content.

- [ ] #6 **[LOW] VUL-006 – Missing HTTP security response headers (CWE-693)**

  **Location**: `security/src/main/java/be/appify/prefab/core/security/WebSecurityConfiguration.java`

  **Root cause**: The filter chain does not explicitly configure security headers. Spring Security 6
  adds `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control`, and `X-XSS-Protection` by
  default, but `Content-Security-Policy` and `Referrer-Policy` require explicit opt-in and are absent.

  **Fix analysis**:
  Add a `.headers()` customiser to the existing filter chain:
  ```java
  .headers(headers -> headers
      .contentSecurityPolicy(csp -> csp.policyDirectives(
          "default-src 'self'; frame-ancestors 'none'"))
      .referrerPolicy(rp -> rp.policy(
          ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
  )
  ```
  The CSP `default-src 'self'` is a safe, restrictive starting point for a REST API that serves no
  inline scripts or styles. `frame-ancestors 'none'` reinforces `X-Frame-Options: DENY`.
  Adopters who embed a Swagger UI will need to loosen the policy; document this in Javadoc.

- [ ] #7 **[LOW] VUL-007 – Hardcoded fallback AWS credentials in example configuration (CWE-287)**

  **Location**: `examples/sns-sqs/src/main/resources/application.yml` lines 10–11

  **Root cause**: `${AWS_ACCESS_KEY_ID:localAccessKey}` and `${AWS_SECRET_ACCESS_KEY:localSecretKey}`
  include literal fallback values. When the environment variables are absent the application starts with
  known static credentials, which may silently authenticate against LocalStack or a real AWS endpoint
  with predictable keys.

  **Fix analysis**:
  The test infrastructure already handles LocalStack credentials correctly: `SnsTestAutoConfiguration`
  registers `spring.cloud.aws.credentials.access-key` and `.secret-key` dynamically from the
  LocalStack container via `DynamicPropertyRegistrar`, so the fallback values are never needed during
  tests.

  Changes required:
  1. In `application.yml`, change to `access-key: ${AWS_ACCESS_KEY_ID}` and
     `secret-key: ${AWS_SECRET_ACCESS_KEY}` (remove fallback literals).
  2. Add a Spring profile `local` (e.g. `application-local.yml`) that provides
     `access-key: test` / `secret-key: test` explicitly for running the example against a local
     LocalStack instance without exporting environment variables. Document this in the example README.

- [ ] #8 **[LOW] VUL-008 – Default security configuration uses oauth2Login (browser flow) only (CWE-306)**

  **Location**: `security/src/main/java/be/appify/prefab/core/security/WebSecurityConfiguration.java:31`

  **Root cause**: `.oauth2Login(withDefaults())` configures the authorisation-code redirect flow for
  browser users. There is no `.oauth2ResourceServer(...)` configuration, so REST clients that send
  `Authorization: Bearer <token>` receive a 302 redirect to the IdP login page instead of a 401
  response. This silently breaks API authentication and may mislead security scanners.

  **Fix analysis**:
  Add `oauth2ResourceServer` with JWT support to the default filter chain:
  ```java
  .oauth2ResourceServer(rs -> rs.jwt(withDefaults()))
  ```
  This adds Spring Security's `BearerTokenAuthenticationFilter`, which handles `Authorization: Bearer`
  requests and returns `401 Unauthorized` on failure instead of redirecting.

  The combined configuration (`oauth2Login` + `oauth2ResourceServer`) is the standard pattern for
  applications that serve both a browser UI and a programmatic API. Adopters who want only one mode
  can override the bean (the existing `@ConditionalOnMissingBean` ensures this).

  Also add `spring-boot-starter-oauth2-resource-server` as a dependency in `security/pom.xml` if it
  is not already transitively present (check `spring-boot-starter-oauth2-client` transitive deps;
  add explicitly if absent to avoid relying on a transitive dependency).

<!-- AC:END -->
