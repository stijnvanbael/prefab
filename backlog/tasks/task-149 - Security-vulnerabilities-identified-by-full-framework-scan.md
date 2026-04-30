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
  `SqsDeserializer.java:93` calls `Class.forName(typeName)` where `typeName` is taken directly from the
  SNS envelope `Subject` field — a value controlled by the message producer.
  An attacker who can publish to the SQS queue can supply an arbitrary fully-qualified class name, triggering
  static initialisers during class loading and probing the classpath.
  **Fix**: maintain an allowlist of permitted event types (e.g. the types registered in `SerializationRegistry`)
  and reject any `typeName` not in that list before calling `Class.forName`.

- [ ] #2 **[HIGH] VUL-002 – Unsafe class loading from Pub/Sub message attribute (CWE-470)**
  `PubSubUtil.java:168` calls `Class.forName(typeName)` where `typeName` is taken from the
  Pub/Sub message attribute `"type"` — a value controlled by the message publisher.
  Same risk as VUL-001.
  **Fix**: same allowlist approach; validate `typeName` against the set of types registered
  in `SerializationRegistry` before resolving the class.

- [ ] #3 **[MEDIUM] VUL-003 – Unsafe class loading from Avro schema full name (CWE-470)**
  `DynamicDeserializer.java:80` calls `Class.forName(resolveClassName(schema.getFullName()))` where the
  schema comes from the Avro message payload.
  A malicious Kafka message with a crafted Avro schema could trigger loading of unexpected classes.
  **Fix**: verify that `resolveClassName(schema.getFullName())` resolves to a class that has been explicitly
  registered via `KafkaJsonTypeResolver.registerType`, rejecting any unrecognised full name.

- [ ] #4 **[MEDIUM] VUL-004 – CSRF protection unconditionally disabled (CWE-352)**
  `WebSecurityConfiguration.java:28` calls `.csrf(CsrfConfigurer::disable)` with no condition.
  This removes CSRF protection for all endpoints, including any that may use session cookies.
  **Fix**: enable CSRF for session-based flows; restrict the disable to stateless API consumers by checking
  the request header (`X-Requested-With`, custom header, or presence of `Authorization: Bearer`).
  Alternatively, document in Javadoc why disabling CSRF is safe for this configuration and add a comment.

- [ ] #5 **[MEDIUM] VUL-005 – Sensitive message body logged on retry errors (CWE-532)**
  `SqsUtil.java:245` logs `message.body()` at WARN level on every retry attempt.
  `PubSubUtil.java:151` logs `pubsubMessage.getData().toStringUtf8()` at WARN level on every retry.
  These payloads may contain PII, financial data, or other sensitive information.
  **Fix**: replace full body logging with a truncated or hashed representation, e.g.
  log only the first 100 characters or the SHA-256 hash of the body, to aid debugging without
  exposing sensitive content.

- [ ] #6 **[LOW] VUL-006 – Missing HTTP security response headers (CWE-693)**
  `WebSecurityConfiguration.java` does not configure Spring Security's header defaults
  (`X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`,
  `Content-Security-Policy`, `Referrer-Policy`).
  Spring Security enables some defaults automatically, but `Content-Security-Policy` and
  `Referrer-Policy` must be opted in explicitly.
  **Fix**: call `.headers(headers -> headers.defaultsDisabled()...)` or rely on Spring Security
  defaults and add `.contentSecurityPolicy(...)` and `.referrerPolicy(...)` as appropriate.
  Document the choices in Javadoc.

- [ ] #7 **[LOW] VUL-007 – Hardcoded fallback AWS credentials in example configuration (CWE-287)**
  `examples/sns-sqs/src/main/resources/application.yml` specifies
  `access-key: ${AWS_ACCESS_KEY_ID:localAccessKey}` and
  `secret-key: ${AWS_SECRET_ACCESS_KEY:localSecretKey}`.
  The fallback values `localAccessKey` / `localSecretKey` are static strings that will be used when
  the environment variables are absent, which could silently permit unauthenticated requests in
  environments where the AWS SDK falls back gracefully.
  **Fix**: remove the fallback literal values; let the application fail fast if the environment
  variables are missing, or configure Spring profiles so the LocalStack/test profile provides the
  credentials explicitly.

- [ ] #8 **[LOW] VUL-008 – Default security configuration uses oauth2Login (browser flow) only (CWE-306)**
  `WebSecurityConfiguration.java:31` configures `.oauth2Login(withDefaults())` which handles the
  OAuth2 authorisation-code flow for browser clients but does not configure
  `.oauth2ResourceServer(...)` for stateless REST clients that send `Authorization: Bearer <token>`
  headers.
  As a result, API clients using access tokens will receive a redirect to the OAuth2 login page
  instead of a 401 response, which may mask authentication failures or confuse tooling.
  **Fix**: add `.oauth2ResourceServer(rs -> rs.jwt(withDefaults()))` to the filter chain (or
  document that the generated bean is intended to be replaced by the application) and add a
  `@ConditionalOnMissingBean` note in the Javadoc so adopters know to override the default.

<!-- AC:END -->
