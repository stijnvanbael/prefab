package be.appify.prefab.processor.rest;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Exposure;
import javax.lang.model.element.TypeElement;

/** Resolves an operation's effective HTTP exposure. */
public final class ExposureResolver {
    private ExposureResolver() {
    }

    /**
     * Returns whether an operation resolves to public HTTP exposure.
     *
     * @param aggregateType aggregate declaring the operation
     * @param operationExposure operation-level override
     * @return {@code true} when the operation is public
     */
    public static boolean isPublic(TypeElement aggregateType, Exposure operationExposure) {
        var aggregate = aggregateType.getAnnotation(Aggregate.class);
        if (operationExposure == Exposure.INHERIT) {
            return aggregate == null || aggregate.exposure() == Exposure.PUBLIC;
        }
        return operationExposure == Exposure.PUBLIC;
    }
}
