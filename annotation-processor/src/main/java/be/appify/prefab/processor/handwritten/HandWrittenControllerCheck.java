package be.appify.prefab.processor.handwritten;

import be.appify.prefab.processor.rest.AggregateEndpoints;
import java.util.List;
import javax.lang.model.element.TypeElement;

/**
 * Reports hand-written Spring MVC handlers that map a method and path Prefab already exposes for an aggregate.
 *
 * <p>A controller that adds endpoints Prefab does not generate is fine, and so is a controller that takes over the
 * generated controller through the documented source-file override (same simple name as the generated one).
 */
final class HandWrittenControllerCheck {
    private final HandWrittenFindings findings;

    HandWrittenControllerCheck(HandWrittenFindings findings) {
        this.findings = findings;
    }

    void check(List<TypeElement> handWrittenTypes, List<ManagedAggregate> aggregates) {
        handWrittenTypes.stream()
                .filter(HttpMappings::isController)
                .filter(controller -> !Annotations.isSuppressed(controller))
                .forEach(controller -> check(controller, aggregates));
    }

    private void check(TypeElement controller, List<ManagedAggregate> aggregates) {
        var mappings = HttpMappings.of(controller);
        aggregates.stream()
                .filter(aggregate -> !isOverride(controller, aggregate))
                .forEach(aggregate -> aggregate.endpoints().forEach(endpoint -> mappings.stream()
                        .filter(mapping -> mapping.matches(endpoint.method(), endpoint.path()))
                        .filter(mapping -> !Annotations.isSuppressed(mapping.handler()))
                        .forEach(mapping -> report(mapping, aggregate, endpoint))));
    }

    private static boolean isOverride(TypeElement controller, ManagedAggregate aggregate) {
        return aggregate.isGeneratedName(controller.getSimpleName().toString(), "Controller")
                && controller.getQualifiedName().toString().startsWith(aggregate.packageName() + ".");
    }

    private void report(
            HttpMappings.Mapping mapping,
            ManagedAggregate aggregate,
            AggregateEndpoints.Endpoint endpoint
    ) {
        findings.report(mapping.handler(),
                "this hand-written handler maps %s %s, which Prefab already exposes for aggregate %s (%s)"
                        .formatted(endpoint.method(), endpoint.path(), aggregate.simpleName(), endpoint.description()),
                ("remove the handler and use the generated %sController. To change what the endpoint does, adapt the"
                        + " aggregate (its @Create/@Update/@Delete methods and fields); to take over the whole"
                        + " controller, create %s.infrastructure.http.%sController yourself (the source-file override,"
                        + " see 'Overriding a Generated Class'). To keep a legacy handler during a migration, add"
                        + " @SuppressWarnings(\"%s\")")
                        .formatted(aggregate.simpleName(), aggregate.packageName(), aggregate.simpleName(),
                                Annotations.SUPPRESSION));
    }
}
