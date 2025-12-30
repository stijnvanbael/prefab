package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate a static method to be an event handler. The method should have a single parameter of the desired event
/// type. The method should return the aggregate root that has been created, or an optional of the aggregate root.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface EventHandler {

    /// Annotate the method of an aggregate root to have it process domain events. The method should have a single
    /// parameter of the desired event type. The aggregate root will be fetched using the specified property of type
    /// `Reference<A>` where `A` is the aggregate root type.
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface ByReference {
        String value();
    }

    /// Annotate the method of an aggregate root to have it process domain events. The `queryMethod` should reference to
    /// a repository method that will be used to fetch the aggregate roots. The event will be delivered to all fetched
    /// aggregate roots. The method should have a single parameter of the desired event type. The `paramMapping` should
    /// specify how to map properties from the event to parameters of the query method. Any parameter not specified in
    /// the mapping will be passed as-is if a property with the same name has been found on the event. Failure to map
    /// all parameters or a reference to a non-existent query method will result in a compilation error.
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Multicast {
        String queryMethod();

        Param[] paramMapping() default {};
    }

    /// Mapping of a parameter from an event property to a query method parameter.
    @Retention(RetentionPolicy.SOURCE)
    @interface Param {
        String from();

        String to();
    }
}
