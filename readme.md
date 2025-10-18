# Prefab

Prefab is a Java annotation processor that generates an entire application from domain classes.

By adding annotations to your domain classes, Prefab will generate the following:

- REST controllers
- Request and response classes
- Services
- Repositories
- Data classes

Prefab is designed to work with Spring Boot and PostgreSQL. Other databases might be added in the future.

Prefab is an opinionated framework that follows the principles of Domain-Driven Design (DDD). Domain classes must
therefore be valid aggregate roots.

## 🏁 Getting started

To get started with Prefab, you need to add the following to your `pom.xml`:

```xml

<parent>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>

<dependencies>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-core</artifactId>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
</dependency>
<!-- Required to support compilation in IntelliJ -->
<dependency>
    <groupId>com.palantir.javapoet</groupId>
    <artifactId>javapoet</artifactId>
    <version>${javapoet.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>problem-spring-web-starter</artifactId>
    <version>${problem-spring-web.version}</version>
</dependency>
</dependencies>
```

Prefab will generate code for any domain entity annotated with `@Aggregate`. This can be a concrete class or a record,
e.g.:

```java

@Aggregate
public record Sale(
        @Id String id,
        Instant start,
        Double amount
) {
}
```

Prefab will generate a REST controller, a service, and a repository for the annotated class.

## ⭐️ Features

### 🐣 Create

Annotate any public constructor with `@Create` to expose it as a REST endpoint to create a new instance of the class.
For a record, this can also be the canonical constructor. By default, the constructor will be exposed at the
`POST /{plural-class-name}` endpoint, where `{plural-class-name}` is the plural of the class name in kebab case. Both
method and path can be customized with the `@Create` annotation. The endpoint will accept a JSON body that matches the
signature of the constructor.

```java

@Aggregate
public record Sale(
        @Id String id,
        Instant start,
        Double amount
) {
    @Create // This constructor will be exposed as a REST endpoint to create a new Sale instance
    public Sale(Instant start, Double amount) {
        this(UUID.randomUUID().toString(), start, amount);
    }

    @PersistenceCreator // Prefab generates Spring Data JDBC code that requires a constructor with all properties
    public Sale {
    }
}
```

### 📘 Get by ID

Annotate an aggregate with `@GetById` to expose a REST endpoint to retrieve an instance of the class by its ID. By
default, the endpoint will be exposed at the `GET /{plural-class-name}/{id}` endpoint, where `{plural-class-name}`
is the plural of the class name in kebab case. Both method and path can be customized with the`@GetById` annotation.

```java

@Aggregate
@GetById // This will expose a GET endpoint for the Sale class
public record Sale(
        @Id String id,
        Instant start,
        Double amount
) {
}
```

### ✍️ Update

Annotate any public method with `@Update` to expose a REST endpoint to update an instance of the class. By default, the
endpoint will be exposed at the `PUT /{plural-class-name}/{id}` endpoint, where `{plural-class-name}` is the plural
of the class name in kebab case. Both method and path can be customized with the `@Update` annotation. The endpoint
will accept a JSON body that matches the signature of the method. The method can either return the updated instance
or `void`. When it is `void`, Prefab assumes the method modifies the existing instance.

```java

@Aggregate
public record Sale(
        @Id String id,
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
endpoint will be exposed at the `DELETE /{plural-class-name}/{id}` endpoint, where `{plural-class-name}` is the plural
of the class name in kebab case. Both method and path can be customized with the `@Delete` annotation.

```java

@Aggregate
@Delete // This will expose a DELETE endpoint for the Sale class
public record Sale(
        @Id String id,
        Instant start,
        Double amount
) {
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
        Instant start,
        Double amount,
        Reference<Customer> customer, // Reference to another aggregate
        String customerName // Not set through the constructor, but resolved from the customer reference
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

### 💾 Alpha: Binary files

You can use the `Binary` type to store binary files in your aggregates. Any `Binary` field in the aggregate won't be
included in JSON request bodies, but will be a separate part of a multipart request. Prefab will store the binary file
in the database as a `bytea` field.

```java

@Aggregate
public record Sale(
        @Id String id,
        Instant start,
        Double amount,
        Binary receipt // Binary file field
) {
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
        Instant start,
        Double amount
) {
}
```

### 🔍 Alpha: Search

Annotate an aggregate with `@Search` to generate a search endpoint for the class. The search endpoint will be exposed
at the `GET /{plural-class-name}` endpoint, where `{plural-class-name}` is the plural of the class name in kebab case.
Provide a `property` parameter to the annotation to specify which property to search on. The search will be
a case-insensitive like query on the specified property. The search will return a paged list of the instances that
match.

```java

@Aggregate
@Search(property = "product") // This will generate a search endpoint for the Sale class on the product property
public record Sale(
        @Id String id,
        Instant start,
        Double amount,
        String product
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
Support may be added later on popular request.
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

You can work around this issue by running:

```bash
mvn clean compile
```

### ❌ Required identifier property not found for class ...

This error occurs when annotation processors are not run in the correct order by your IDE.
You can work around this issue by running:

```bash
mvn clean compile
```

## 🧭 What's next?

Prefab is still in its early stages, and many features are planned for the future. Some of the upcoming features
include:

- Support for Kafka, PubSub, and SNS/SQS
- Support for more complex search queries
- Full-text search
- Support for Spring Security
- Projections
- Support for class hierarchies
- Support for generic types
- Simplify mixing in your own code with generated code
- Simplify Maven configuration
- Generate test fixtures
- Support for more databases
