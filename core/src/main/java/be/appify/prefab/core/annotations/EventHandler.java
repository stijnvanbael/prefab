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

    /// Annotate the method of an aggregate root to have to process domain events. The method should have a single
    /// parameter of the desired event type. The aggregate root will be fetched using the specified property that is of
    /// type `Reference<A>` where `A` is the aggregate root type.
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface ByReference {
        String value();
    }

    /// Annotate the method of an aggregate root to have to process domain events. The method should have a single
    /// parameter of the desired event type. All aggregate roots will be fetched and the method will be called on each
    /// of them.
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Broadcast {
    }
}
