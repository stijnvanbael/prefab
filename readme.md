# 📦 Prefab

The purpose of the software we build is to solve business problems. However, a lot of time is spent
on boilerplate code that doesn't add any business value. Prefab aims to reduce this boilerplate
by generating the necessary code for you based on your domain model.

With Prefab, you can focus on defining your domain classes with the business logic and let the
framework handle the application infrastructure. This leads to faster development cycles, fewer bugs,
and a more maintainable codebase. You are essentially coding at a higher level of abstraction,
focusing on the "what" instead of the "how".

There are several use cases where Prefab can help you:

- Rapid prototyping
- Iterate quickly on domain models
- Reducing boilerplate and maintenance overhead
- Learning and experimenting with Spring Boot and Domain-Driven Design
- Scaffolding for larger applications

While Prefab is not intended to build very complex applications out of the box, it can serve as a solid foundation
that you can extend and customize to fit your specific needs.

## ⚙️ How it works

Prefab is a Java annotation processor that generates an entire application from domain classes.

By adding annotations to your domain classes, Prefab will generate the following:

- REST controllers
- Request and response classes
- Services
- Repositories

Additionally, Prefab supports:

- Event producers and consumers
- Database migrations

Prefab is designed to work with Spring Boot and PostgreSQL. Other databases might be added in the future.

Prefab is an opinionated framework that follows the principles of Domain-Driven Design (DDD). Domain classes must
therefore be valid aggregate roots.

## 🏁 Getting started

To get started with Prefab, you need to add the following to your `pom.xml`:

```xml

<parent>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-parent</artifactId>
    <version>0.2.0</version>
</parent>

<dependencies>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-core</artifactId>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
    <scope>provided</scope>
</dependency>
<!-- Optional, only when using Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<!-- Optional, only when using Pub/Sub -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-pubsub</artifactId>
    <version>${google-cloud.version}</version>
</dependency>

<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-test</artifactId>
    <scope>test</scope>
</dependency>
</dependencies>
<build>
<plugins>
    <!-- Optional, to generate test clients -->
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>3.4.0</version>
        <executions>
            <execution>
                <id>add-main-as-test-source</id>
                <phase>generate-test-sources</phase>
                <goals>
                    <goal>add-test-source</goal>
                </goals>
                <configuration>
                    <sources>
                        <source>target/prefab-test-sources</source>
                    </sources>
                </configuration>
            </execution>
        </executions>
    </plugin>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
            <source>${maven.compiler.release}</source>
            <target>${maven.compiler.release}</target>
            <!-- Required to run the annotation processor -->
            <annotationProcessors>
                <annotationProcessor>be.appify.prefab.processor.PrefabProcessor</annotationProcessor>
            </annotationProcessors>
        </configuration>
    </plugin>
</plugins>
</build>
```

Create a Spring Boot application class and annotate it with `@EnablePrefab` so the Prefab framework dependencies are
wired:

```java

@SpringBootApplication
@EnablePrefab
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Prefab will generate code for any domain entity annotated with `@Aggregate`. This can be a concrete class or a record,
e.g.:

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
}
```

Prefab will generate a REST controller, a service, and a repository for the annotated class.

## 🛠️ IDE support

IDEs like IntelliJ IDEA and Eclipse support annotation processors out of the box. However, you might need to enable
annotation processing and switch to Maven for building as partial compilation by the IDE might not produce the desired
result.

### 💡 IntelliJ IDEA

1. Go to `Settings > Build, Execution, Deployment > Compiler > Annotation Processors` and make sure
   `Enable annotation processing` is checked.
2. It's also recommended to use Maven instead of IntelliJ IDEA's built-in compiler to avoid potential issues
   with annotation processing. You can do this by going to
   `Settings > Build, Execution, Deployment > Build Tools > Maven > Runner` and selecting
   `Delegate IDE build/run actions to Maven`. Tick `Skip tests` to avoid running tests on every build.

## ⭐️ Features

### 🐣 Create

Annotate any public constructor with `@Create` to expose it as a REST endpoint to create a new instance of the class.
For records, this can also be the canonical constructor. By default, the constructor will be exposed at
`POST /{plural-class-name}`, where `{plural-class-name}` is the plural of the class name in kebab case. Both
method and path can be customized with the `@Create` annotation. The endpoint will accept a JSON body that matches the
signature of the constructor.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
    @Create // This constructor will be exposed as a REST endpoint to create a new Sale instance
    public Sale(Instant start, Double amount) {
        this(UUID.randomUUID().toString(), 0, start, amount);
    }

    @PersistenceCreator // Prefab generates Spring Data JDBC code that requires a constructor with all properties
    public Sale {
    }
}
```

### 📘 Get by ID

Annotate an aggregate with `@GetById` to expose a REST endpoint to retrieve an instance of the class by its ID. By
default, the endpoint will be exposed at `GET /{plural-class-name}/{id}`, where `{plural-class-name}`
is the plural of the class name in kebab case. Both method and path can be customized with the`@GetById` annotation.

```java

@Aggregate
@GetById // This will expose a GET endpoint for the Sale class
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
}
```

### 📋 Get list

Annotate an aggregate with `@GetList` to generate a list endpoint for the class. The list endpoint will be exposed
at `GET /{plural-class-name}`, where `{plural-class-name}` is the plural of the class name in kebab case.
Annotate one or more fields with `@Filter` to allow filtering on these fields. The filter will be
a case-insensitive like (contains) query on the specified field by default, but this behavior can be customized by
providing
with the `operator` and `ignoreCase` attributes of the `@Filter` annotation. The endpoint supports Spring Data REST
paging and sorting out of the box.

```java

@Aggregate
@GetList // This will generate a list endpoint for the Sale class
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount,
        @Filter // This property can be filtered on in the list endpoint
        String product
) {
}
```

### ✍️ Update

Annotate any public method with `@Update` to expose a REST endpoint to update an instance of the class. By default, the
endpoint will be exposed at `PUT /{plural-class-name}/{id}`, where `{plural-class-name}` is the plural
of the class name in kebab case. Both method and path can be customized with the `@Update` annotation. The endpoint
will accept a JSON body that matches the signature of the method. The method can either return the updated instance
or `void`. When it is `void`, Prefab assumes the method modifies the existing instance.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
    @Update // This will expose a PUT endpoint for the Sale class
    public Sale updateAmount(Double newAmount) {
        return new Sale(this.start, newAmount); // Return the updated instance
    }
}
```

### 🗑️ Delete

Annotate an aggregate with `@Delete` to expose a REST endpoint to delete an instance of the class. By default, the
endpoint will be exposed at `DELETE /{plural-class-name}/{id}`, where `{plural-class-name}` is the plural
of the class name in kebab case. Both method and path can be customized with the `@Delete` annotation.

```java

@Aggregate
@Delete // This will expose a DELETE endpoint for the Sale class
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
}
```

### 💿 Repository mixins

You can extend the generated repository by creating a mixin interface. Annotate the interface with `@RepositoryMixin`
and define custom query methods in it. The generated repository will extend the mixin interface, so your custom methods 
will be available on the repository. Spring Data JDBC query method conventions are supported.

```java
@RepositoryMixin(Sale.class)
public interface SaleRepositoryMixin {
    List<Sale> findByAmountGreaterThan(Double amount);
    
    @Query("SELECT * FROM sale WHERE start >= :start AND start <= :end")
    List<Sale> findByStartBetween(Instant start, Instant end);
}
```

### ➡️ Reference

Aggregates can reference other aggregates by using a `Reference<OtherType>` field. Prefab will map the reference to the
ID of the referenced aggregate. References can also be resolved to the actual instance by using
`Reference.resolveReadOnly()`. But aggregates cannot modify the referenced instance, as the method name suggests.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount,
        Reference<Customer> customer, // Reference to another aggregate
        String customerName // Not set through the constructor but resolved from the customer reference
) {
    @Create
    public Sale(Instant start, Double amount, Reference<Customer> customer) {
        this(
                UUID.randomUUID().toString(),
                start,
                amount,
                customer,
                customer.resolveReadOnly().name() // Resolve the customer reference to get the name
        );
    }
}
```

### 👦 Children

Any nested list of non-primitive classes or records within an aggregate will be treated as children of the aggregate.
These will be included in JSON requests and responses as nested objects. Make sure to limit the number of children to a
reasonable amount to avoid performance issues. These will be mapped in a separate database table with a foreign key to
the parent aggregate.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        List<SaleItem> items // List of SaleItem children
) {
}

public record SaleItem(
        String product,
        Double price
) {
}
```

### 💵 Value objects

Value objects can be embedded in aggregates. These will be included in JSON requests and responses as nested objects,
but not as separate database tables. This is useful for small, immutable objects that are part of the aggregate but have
no identity of their own. Examples are addresses, money, ...

Embedded objects are stored in the database as `<root field>_<child field>`. In the example below, the `amount` field
will be stored as `amount_value` and `amount_currency` in the database.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        @Embedded.Nullable(prefix = "amount_") Money amount
        // Money is a value object that will be embedded in the Sale aggregate
) {
}

public record Money(
        Double value,
        String currency
) {
}
```

### ✅ Validation

Prefab supports validation of the fields in your aggregates. You can use standard Jakarta validation annotations such as
`@NotNull`, `@Size`, `@Min`, and `@Max` to validate the fields. Prefab will automatically validate the request bodies
of `@Create` and `@Update` endpoints and return a `400 Bad Request` response if the validation fails.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        @NotNull Instant start,
        @NotNull Double amount
) {
    @Create
    public Sale(
            @NotNull Instant start, // Must not be null
            @NotNull @Min(0) Double amount // Must be greater than or equal to 0
    ) {
        this(UUID.randomUUID().toString(), start, amount);
    }

    @PersistenceCreator
    public Sale {
    }

    @Update
    public Sale updateAmount(@NotNull @Min(0) Double newAmount) {
        return new Sale(this.start, newAmount); // Return the updated instance
    }
}
```

### 🛡️ Security

Add the `prefab-security` dependency to your `pom.xml` to enable security features in Prefab.

```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-security</artifactId>
</dependency>
```

Define an OAuth2 client registration in your `application.yml`:

```yaml
spring:
  security.oauth2.client.registration:
    google:
      client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
      client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}
```

By default, all generated REST endpoints are secured. They require the user to be authenticated. You can customize the
security requirements of any REST endpoint by setting the security attribute on it.

To make an endpoint publicly accessible, set enabled to false on the security attribute.

```java

@Create(security = @Security(enabled = false)) // Publicly accessible create endpoint
public Sale(Instant start, Double amount) {
    this(UUID.randomUUID().toString(), 0, start, amount);
}
```

To restrict access to users with a specific authority, set the authority attribute on the `@Security` annotation.

```java

@Delete(security = @Security(authority = "sale:delete")) // Only users with the sale:delete authority can delete
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
}
```

### 💾 Binary files

You can use the `Binary` type to store binary files in your aggregates. Any `Binary` field in the aggregate won't be
included in JSON request bodies, but will be a separate part of a multipart request. Prefab will store the binary file
in the database as a `bytea` field.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount,
        Binary receipt // Binary file field
) {
}
```

### 🔥 Events

Make sure you specify the spring application name in application.yml so the events are namespaced correctly.

```yaml
spring:
  application:
    name: my-application
```

Any record can be an event in Prefab. Events can be published by implementing `PublishesEvents` and calling `publish()`.
By default, events are published on the Spring application event bus.

```java

@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) implements PublishesEvents {
    void completeSale() {
        publish(new SaleCompletedEvent(id, Instant.now()));
    }
}
```

Alternatively, you can annotate the event with `@Event` to generate a producer for the event that publishes to a
message broker. Supported platforms right now are `KAFKA`, and `PUB_SUB`.

```java

@Event(topic = "${kafka.topics.sale.name}", platform = KAFKA)
public record SaleCompletedEvent(
        String saleId,
        Instant completedAt
) {
}
```

Events can be consumed by annotating a method that takes a single argument with `@EventHandler`. This can be either an
instance method on a Spring bean or a static method on an aggregate root. Make sure to annotate event handlers accessing the
database with `@Transactional` to ensure proper transaction management.

```java

@Component
@Transactional // When depending on the database
public class CreateInvoiceUseCase {
    @EventHandler
    public void handleSaleCompleted(SaleCompletedEvent event) {
        // Handle the event
    }
}
```

Event handler behaviour can be customized by adding an `@EventHandlerConfig` to the class containing `@EventHandler` methods.

```java
@EventHandlerConfig(
        concurrency = "5",
        retryLimit = "3",
        minimumBackoffMs = "2000",
        deadLetterTopic = "my.dlt"
)
public class CreateInvoiceUseCase {
    @EventHandler
    public void handleSaleCompleted(SaleCompletedEvent event) {
        // Handle the event
    }
}
```

You can have `@EventHandler` methods in aggregate roots instance methods as well, as long as you provide a strategy for 
loading the aggregate instance. This can be done by adding either a `@ByReference` or `@Mulicast` annotation to the 
event handler method.

`@ByReference` will load the aggregate instance by resolving a `Reference` field in the event containing the aggregate ID.

```java
@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Reference<Invoice> invoice
) {
    @EventHandler
    @ByReference("sale") // Load the Sale aggregate by resolving the sale reference in the event
    public void onInvoiceCreated(InvoiceCreated event) {
        this.invoice = Reference.fromId(event.invoiceId());
    }
}
```

`@Multicast` uses a query to load all aggregate instances using a custom query defined in a repository mixin. One or
more parameters can be extracted from the event to use in the query.

```java
@Aggregate
public record Sale(
        @Id String id,
        @Version long version,
        Reference<CashRegister> cashRegister,
        SaleStatus status
) {
   @EventHandler
   @Multicast(
           queryMethod = "findByCashRegister",
           parameters = { "cashRegister" }
   )
   public void onCashRegisterClosed(CashRegisterClosed event) {
      status = SaleStatus.CLOSED;
   }
}
```

### 🗃️ Alpha: Database migrations

Annotate an aggregate with `@DbMigration` to generate a Flyway database migration script for PostgreSQL.
A migration script will be generated in the target/classes/db/migration folder.
If you're satisfied with the generated script, you should copy it to the src/main/resources/db/migration folder so it
doesn't get overwritten the next time you compile your project.

```java

@Aggregate
@DbMigration // This will include the Sale class in the generated Flyway migration script
public record Sale(
        @Id String id,
        @Version long version,
        Instant start,
        Double amount
) {
}
```

### 🧑‍🧑‍🧒‍ Alpha: Aggregate parents

Annotate a `Reference` field with `@Parent` to indicate that the reference is the parent of the aggregate. Any REST
requests to the aggregate will be prefixed with the path of the parent aggregate. Any `@Search` endpoints will also
be limited to the parent aggregate. This is useful for creating a hierarchy of aggregates with lots of children.

## 🦆 Supported data types

The types below are the only types you can currently use in Prefab aggregates or nested records.
Any type that is not listed is currently not supported.
Support may be added later on request.
Alternatively, you can write your own Prefab plugin to provide database and JSON mappings.

### 🐒 Primitives

Prefab supports the following Java primitives and their boxed variants:

- `boolean` / `Boolean`
- `int` / `Integer`
- `long` / `Long`
- `float` / `Float`
- `double` / `Double`

### ☕️ Standard Java types

In addition to the primitives above, Prefab supports the following standard Java types:

- `String`
- `BigDecimal`
- `Duration`
- `Instant`
- Any `enum`
- Any `record`
- Any `List` of supported types

### 🧩 Prefab built-in types

Prefab adds the following types for you to use as well:

- `Reference`
- `Binary`

## ⚠️ Known issues and limitations

### 🧑‍💼 Limited support for type hierarchies

Prefab does not fully support type hierarchies. While you can have type hierarchies in your domain model, the
`@Aggregate` annotation can only be applied to concrete classes or records. References to abstract classes or interfaces
will not work. Children must also be concrete classes or records. Prefab cannot generate code for abstract classes or
interfaces.

### ❌ Compiler errors in IntelliJ IDEA

Sometimes IntelliJ IDEA runs into an undefined compiler error when compiling the generated code.

```
java: Compilation failed: internal java compiler error
```

This likely happens due to the way IntelliJ handles annotation processors.

To fix this, you can try using Maven instead of IntelliJ IDEA's built-in compiler. You can do this by going to
`Settings > Build, Execution, Deployment > Build Tools > Maven > Runner` and selecting
`Delegate IDE build/run actions to Maven`.

### 🤔 Repository mixin interface missing on repository in IntelliJ IDEA

When running tests in IntelliJ IDEA, you might encounter an error like this:

```
  java: cannot find symbol
  symbol:   method someCustomMethod()
  location: interface package.SomeAggregateRepository
```

This happens because IntelliJ IDEA will only consider source files that have changed to run through the annotation
processor. Prefab, however, needs the full classpath to generate the repository mixin interfaces correctly.

To fix this, you can try using Maven instead of IntelliJ IDEA's built-in compiler.

### 💥 IllegalAccessException when saving an aggregate root

In certain cases, when saving a new aggregate root, you might encounter an `IllegalAccessException` with a message like this:

```
java.lang.IllegalAccessException: final field has no write access: ...
```

This is an issue related to Spring Data JDBC. You likely have defined a method on your aggregate root like this:
```java
@Update(path = "/name")
public User setName(String name) {
    return new User(this.id, this.version, name);
}
```

This will cause Spring Data JDBC to generate a proxy for the `User` class that doesn't have access to the private fields
of the record, which results in the `IllegalAccessException` when trying to save the new instance.

To fix this, name the method something other than `setX` or `withX`, where `X` is the name of a field in the record. For example:

```java
@Update(path = "/name")
public User updateName(String name) {
    return new User(this.id, this.version, name);
}
```

## 🧭 What's next?

Prefab is still in its early stages, and many more features are planned for the future.

See the [backlog](backlog/tasks) for a list of planned features and improvements.