# Prefab Cheat Sheet for AI Agents

Copy this file into your project's `AGENTS.md` / `CLAUDE.md`. Prefab is a Spring Boot code generator: you write
annotated Java records, the annotation processor writes the rest at compile time. Do not use plain Spring Boot
habits here.

## Mental model

**You write** (in `src/main/java`, `src/test/java`):

- Aggregate records: `@Aggregate` + `@Id Reference<Self> id` + `@Version long version` + fields.
- Operations as annotated constructors and methods on the record: `@Create`, `@Update`, `@Delete`.
- `@Event` records, `@EventHandler` methods, `@RepositoryMixin` interfaces, tests.

**Prefab generates** (in `target/generated-sources`, never edit): `{Aggregate}Controller`, `Service`,
`Repository`, `Create…Request`/`Update…Request`/`{Aggregate}Response` records, event producers and consumers,
Flyway migrations (`V…__*.sql`), and `{Aggregate}Client` test helpers (`target/prefab-test-sources`).

After a compile, read `target/prefab/agent-context.md` first: it lists this project's aggregates, endpoints,
events, tables and test helpers, so you need not browse generated sources.

Business rules live in the record's methods. Change behaviour by changing annotations or the record, then run
`mvn compile` and read the compiler output: Prefab errors say what to fix.

## Minimal aggregate

```java
@Aggregate
@GetById
@GetList
public record Product(
        @Id Reference<Product> id,        // org.springframework.data.annotation.Id
        @Version long version,            // org.springframework.data.annotation.Version
        @Filter String name,
        BigDecimal price
) {
    @Create
    public Product(String name, BigDecimal price) {
        this(Reference.create(), 0L, name, price);
    }

    @Update
    public Product rename(String name) {  // returns the new state
        return new Product(id, version, name, price);
    }

    @Delete
    public void delete() { }
}
```

Annotations: `be.appify.prefab.core.annotations` (`@Aggregate`, `@EventHandler`, `@Event`, …) and
`be.appify.prefab.core.annotations.rest` (`@Create`, `@Update`, `@Delete`, `@GetById`, `@GetList`, `@Filter`, …).
Setup: `@EnablePrefab` on the `@SpringBootApplication` class. Add only the modules you need
(`prefab-core`, `prefab-annotation-processor`, `prefab-postgres` or `prefab-mongodb`, `prefab-kafka`,
`prefab-security`, `prefab-test`).

## Intent → what to use

| I want to… | Use |
|---|---|
| Create / read one / list / update / delete over HTTP | `@Create`, `@GetById`, `@GetList`, `@Update`, `@Delete` |
| Filter a list by a field | `@Filter` on the field (`operator = EQUAL`, `STARTS_WITH`, range operators, `FULL_TEXT`) |
| Default sort order of a list | `@GetList(sorted = @SortOrder(property = "name"))` |
| Autocomplete / type-ahead | `@Autocomplete` |
| A read-only derived value in responses | `@Computed` on a public no-arg method |
| A custom action (e.g. `POST /invoices/{id}/pay`) | `@Update(path = "/pay", method = "POST")` on a method that returns the new state |
| Nested resource (`/orders/{id}/lines`) | `@Parent Reference<Order>` field on the child |
| Reject invalid input | Jakarta validation (`@NotNull`, `@Size`, …) on constructor/method parameters |
| Reference another aggregate | `Reference<Other>` field, never a JPA relation |
| Publish an event | `@Event(topic = "…")` record + `private static final EventPublisher<E> P = EventPublisher.bind(E.class)`, then `P.publish(event)` in a constructor or update method |
| React to an event: create an aggregate | `@EventHandler public static Agg on(Event e)` |
| React to an event: update one aggregate | `@EventHandler @ByReference(property = "ref")` instance method |
| React to an event: update many aggregates | `@EventHandler @Multicast(queryMethod = "findByX", parameters = "x")` + `@RepositoryMixin` with `findByX`; set `failWhenEmpty = false` when no matches is normal (default fails/retries) |
| A custom query | `@RepositoryMixin(Agg.class)` interface with a Spring Data derived method or `@Query` |
| Require login, role or authority | `security = @Security(role = "ADMIN")` on the operation annotation |
| Audit who/when | `AuditInfo audit` field (or `@CreatedAt`, `@CreatedBy`, `@LastModifiedAt`, `@LastModifiedBy`) |
| Multi-tenancy | `@TenantId` field |
| Store a file | `Binary` field, `@Download` for the endpoint |
| Large text, index, JSON column | `@Text`, `@Indexed`, `@DbDocument` |
| Rename a column, set a default | `@DbRename`, `@DbDefaultValue` |
| Hide an aggregate's HTTP endpoints | `@Aggregate(exposure = Exposure.INTERNAL)` |
| Test one aggregate's logic and events | Plain JUnit; `@ExtendWith(PublishedEventsExtension.class)` + `CapturingDomainEventPublisher` parameter |
| Test through HTTP | `@IntegrationTest` (prefab-test) and `@Autowired {Aggregate}Client`. Runs in memory, no Docker; add `containers = true` only for a test that needs real database or broker behaviour |

## Do not

- Do **not** write `@RestController`, `@Service` or `@Repository` classes for an aggregate. Annotate the record.
- Do **not** write JPA `@Entity`, `@Table`, `@OneToMany` or Lombok. Aggregates are records; use `Reference<T>`.
- Do **not** write `@KafkaListener` or `KafkaTemplate` code. Use `@Event` and `@EventHandler`.
- Do **not** write Flyway migrations by hand. Prefab generates `V…__*.sql` from the record. Never edit an
  applied migration; to change the schema, change the record (use `@DbRename` for renames).
- Do **not** edit anything under `target/` (`target/generated-sources`, `target/prefab-test-sources`): it is
  overwritten on every build. Do not copy generated classes into `src/` to "fix" them.
- Do **not** hand-write request/response DTOs. Use the generated `Create…Request` and `…Response` records.
- Do **not** use deprecated `PublishesEvents.publish(...)` / `DomainEventPublisher.publish(...)`.
- Do **not** add setters, `getX()` accessors or mutable fields. Records are immutable; accessors are `name()`.
- Do **not** add `@SuppressWarnings("prefab.handwritten")` or `-- prefab:manual` to make a warning go away.
  Prefab reports hand-written controllers, entities, repositories and migrations that duplicate an aggregate at
  compile time; those opt-outs are for human decisions.
- Do **not** silence a Prefab compiler error by deleting the annotation. Read the message and fix the cause.

## When Prefab can't express it

Try these in order and stop at the first that works:

1. **Annotation attributes** on the existing annotation (`path`, `method`, `exposure`, `security`, `sorted`).
2. **`@RepositoryMixin`**: custom queries, SQL, joins, aggregations.
3. **Domain logic in the record's methods** and **events** for side effects. Put business rules there, not
   in a controller.
4. **A documented hook**: `AuditContextProvider`, `TenantContextProvider`, `HttpSecurityCustomizer` bean,
   `EventRegistryCustomizer`, a `@Component` with an `@EventHandler` method for side effects (email,
   external calls).
5. **Generated test client** missing a call: place your own `{Aggregate}Client` under `src/test/java` or use
   `MockMvc`.

**Stop and escalate** (say so in your answer, or hand the task to a stronger model or a human) when:

- the requirement needs a custom `PrefabPlugin`, or taking over a generated class by copying it into
  `src/main/java` (the whole-class override; you then maintain it by hand);
- you would have to change the shape of generated controllers or services, or the identity or version fields;
- the build fails with an internal Prefab error (not a clear diagnostic) or the same error survives two
  attempts at a fix;
- you are tempted to hand-write a controller, an entity or a migration.

A clearly reported limitation beats a workaround that breaks on the next build.

## Where to read more

Read only what the task needs. First choose one recipe from `docs/prefab/recipes/index.md` (or
`backlog/docs/recipes/index.md` in the Prefab repository); do not load the whole recipe set. Each recipe links to a
complete example source that is compiled or exercised by the Prefab build. For reference details, see `docs/prefab/`
or, in the Prefab repository, `backlog/docs/`:

- `recipes/index.md`: intent lookup; open only the matching task recipe
- `annotation-reference.md`: every annotation, its attributes and rules (look up one section, don't read it all)
- `feature-guides.md`: events, handlers, Avro, audit, tenancy, files, mixins, testing, security
- `generated-artefacts.md`: what gets generated, and the test client
- `database-migrations.md`: migration lifecycle and manual `V{n}__*.sql`
- `extension-points.md`: plugins, providers, whole-class override
- `configuration.md`, `modules.md`, `troubleshooting.md`: properties, which modules to add, common errors
