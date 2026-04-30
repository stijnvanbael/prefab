# 📦 Prefab

The purpose of the software we build is to solve business problems. However, a lot of time is spent
on boilerplate code that doesn't add any business value. Prefab aims to reduce this boilerplate
by generating the necessary code for you based on your domain model.

With Prefab, you can focus on defining your domain classes with the business logic and let the
framework handle the application infrastructure. This leads to faster development cycles, fewer bugs,
and a more maintainable codebase. You are essentially coding at a higher level of abstraction,
focusing on the "what" instead of the "how". Prefab severely reduces the context both for humans and LLMs,
allowing both to work more effectively with the domain model and business logic.

There are several use cases where Prefab can help you:

- Rapid prototyping
- Iterate quickly on domain models
- Reducing boilerplate and maintenance overhead
- Consume significantly fewer tokens when using LLMs to develop and maintain your application
- Learning and experimenting with Spring Boot and Domain-Driven Design
- Scaffolding for larger applications
- Easy pivoting and changing directions in the early stages of development

While Prefab is not intended to build very complex applications out of the box, it can serve as a solid foundation
that you can extend and customize to fit your specific needs.

## ⚙️ How it works

Prefab is a Java annotation processor that generates an entire application from domain classes.

By adding annotations to your domain classes, Prefab will generate REST controllers, request/response records,
services, repositories, event producers/consumers, and database migration scripts at compile time.

Prefab is an opinionated framework that follows the principles of Domain-Driven Design (DDD). Domain classes must
therefore be valid aggregate roots.

## 🏁 Getting started

Add the following to your `pom.xml` (replace `LATEST_VERSION` with the current release):

```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-core</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<!-- Choose your database backend: prefab-postgres for PostgreSQL or prefab-mongodb for MongoDB -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-postgres</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
    <version>LATEST_VERSION</version>
    <scope>provided</scope>
</dependency>
```

Annotate your Spring Boot application class with `@EnablePrefab`:

```java
@SpringBootApplication
@EnablePrefab
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Write your first aggregate:

```java
@Aggregate
@GetById
@GetList
public record Sale(
        @Id Reference<Sale> id,
        @Version long version,
        Instant start,
        Double amount
) {
    @Create
    public Sale(Instant start, Double amount) {
        this(Reference.create(), 0L, start, amount);
    }

    @Update
    public Sale updateAmount(Double newAmount) {
        return new Sale(id, version, start, newAmount);
    }

    @Delete
    public void delete() { }
}
```

Prefab generates a fully-wired REST controller, service, repository, request/response records, and a Flyway
migration script from this single class.

For a full reference of all annotations, built-in types, generated artefacts, event handling, extension points,
configuration, and troubleshooting, see the
**[Developer Guide](backlog/docs/developer-guide.md)**.

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

## 🧭 What's next?

Prefab is still in its early stages, and many more features are planned for the future.

See the [backlog](backlog/tasks) for a list of planned features and improvements.