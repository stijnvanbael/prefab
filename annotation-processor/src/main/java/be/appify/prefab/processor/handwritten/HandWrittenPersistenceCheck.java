package be.appify.prefab.processor.handwritten;

import java.util.List;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

/**
 * Reports hand-written persistence types for aggregates that Prefab already persists: JPA (or Spring Data) entities
 * that mirror an aggregate, and Spring Data repositories whose domain type is an aggregate.
 *
 * <p>{@code @RepositoryMixin} interfaces and a repository that replaces the generated one through the source-file
 * override (same simple name as the generated repository) are legitimate and not reported.
 */
final class HandWrittenPersistenceCheck {
    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String REPOSITORY_MIXIN = "be.appify.prefab.core.annotations.RepositoryMixin";
    private static final List<String> ENTITY_ANNOTATIONS = List.of(
            "jakarta.persistence.Entity",
            "javax.persistence.Entity",
            "jakarta.persistence.Table",
            "javax.persistence.Table",
            "org.springframework.data.relational.core.mapping.Table",
            "org.springframework.data.mongodb.core.mapping.Document");
    private static final List<String> MAPPING_ATTRIBUTES = List.of("value", "name", "collection");

    private final HandWrittenFindings findings;
    private final Types types;

    HandWrittenPersistenceCheck(HandWrittenFindings findings, ProcessingEnvironment processingEnvironment) {
        this.findings = findings;
        this.types = processingEnvironment.getTypeUtils();
    }

    void check(List<TypeElement> handWrittenTypes, List<ManagedAggregate> aggregates) {
        handWrittenTypes.stream()
                .filter(type -> !Annotations.isSuppressed(type))
                .forEach(type -> {
                    checkEntity(type, aggregates);
                    checkRepository(type, aggregates);
                });
    }

    private void checkEntity(TypeElement type, List<ManagedAggregate> aggregates) {
        if (!Annotations.hasAny(type, ENTITY_ANNOTATIONS)) {
            return;
        }
        aggregates.stream()
                .filter(aggregate -> !aggregate.ownTypeNames().contains(type.getQualifiedName().toString()))
                .filter(aggregate -> mirrorsAggregate(type, aggregate))
                .findFirst()
                .ifPresent(aggregate -> findings.report(type,
                        "this hand-written entity duplicates aggregate %s, which Prefab already persists (table `%s`)"
                                .formatted(aggregate.simpleName(),
                                        toSnakeCase(aggregate.simpleName())),
                        ("delete the entity and persist the data through the @Aggregate type %s. Prefab generates the"
                                + " repository and the schema migration from it; add fields to the aggregate to change"
                                + " the schema, and use @RepositoryMixin for extra queries. To keep a legacy entity"
                                + " during a migration, add @SuppressWarnings(\"%s\")")
                                .formatted(aggregate.simpleName(), Annotations.SUPPRESSION)));
    }

    private static boolean mirrorsAggregate(TypeElement entity, ManagedAggregate aggregate) {
        return entity.getSimpleName().contentEquals(aggregate.simpleName())
                || mappedNames(entity).stream().anyMatch(aggregate::isMappedTo);
    }

    private static List<String> mappedNames(TypeElement entity) {
        return ENTITY_ANNOTATIONS.stream()
                .flatMap(name -> Annotations.find(entity, name).stream())
                .flatMap(mirror -> MAPPING_ATTRIBUTES.stream().flatMap(attribute ->
                        Annotations.values(mirror, attribute).stream()))
                .filter(name -> !name.isBlank())
                .toList();
    }

    private void checkRepository(TypeElement type, List<ManagedAggregate> aggregates) {
        if (Annotations.find(type, REPOSITORY_MIXIN).isPresent()) {
            return;
        }
        domainTypeOfRepository(type.asType())
                .flatMap(domainType -> aggregates.stream()
                        .filter(aggregate -> aggregate.ownTypeNames().contains(domainType))
                        .findFirst())
                .filter(aggregate -> !isOverride(type, aggregate))
                .ifPresent(aggregate -> findings.report(type,
                        "this hand-written repository duplicates the %sRepository that Prefab generates for aggregate %s"
                                .formatted(aggregate.simpleName(), aggregate.simpleName()),
                        ("delete it and inject the generated %sRepository. Put extra queries in an interface annotated"
                                + " with @RepositoryMixin(%s.class); to take over the whole repository, create %s.%sRepository"
                                + " yourself (the source-file override, see 'Overriding a Generated Class'). To keep a"
                                + " legacy repository during a migration, add @SuppressWarnings(\"%s\")")
                                .formatted(aggregate.simpleName(), aggregate.simpleName(), aggregate.packageName(),
                                        aggregate.simpleName(), Annotations.SUPPRESSION)));
    }

    private static boolean isOverride(TypeElement repository, ManagedAggregate aggregate) {
        return aggregate.isGeneratedName(repository.getSimpleName().toString(), "Repository")
                && repository.getQualifiedName().toString().startsWith(aggregate.packageName() + ".");
    }

    /** Walks the supertypes to find {@code Repository<T, ID>} and returns the qualified name of {@code T}. */
    private Optional<String> domainTypeOfRepository(TypeMirror type) {
        if (!(type instanceof DeclaredType declared)) {
            return Optional.empty();
        }
        if (declared.asElement() instanceof TypeElement element
                && element.getQualifiedName().contentEquals(SPRING_DATA_REPOSITORY)
                && !declared.getTypeArguments().isEmpty()
                && types.asElement(declared.getTypeArguments().getFirst()) instanceof TypeElement domain) {
            return Optional.of(domain.getQualifiedName().toString());
        }
        return types.directSupertypes(type).stream()
                .map(this::domainTypeOfRepository)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
