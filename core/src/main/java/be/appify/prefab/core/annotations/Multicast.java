package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate the method of an aggregate root to have it process domain events. The <code>queryMethod</code> should
 * reference a repository method that will be used to fetch the aggregate roots. The event will be delivered to all
 * fetched aggregate roots. The method should have a single parameter of the desired event type. The
 * <code>paramMapping</code> should specify how to map properties from the event to parameters of the query method.
 * Any parameter not specified in the mapping will be passed as-is if a property with the same name has been found on
 * the event. Failure to map all parameters or a reference to a non-existent query method will result in a compilation
 * error.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Multicast {
    /**
     * The name of the query method on the repository to fetch the aggregate roots.
     *
     * @return The name of the query method on the repository to fetch the aggregate roots.
     */
    String queryMethod();

    /**
     * The event properties to map on the query parameters, in the order of the query parameters.
     *
     * @return The names of the event properties to map on the query parameters, in the order of the query parameters.
     */
    String[] parameters() default {};
}
