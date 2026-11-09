package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Container for concrete {@link Event} bindings on a generic event declaration. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Events {
    /** @return the event bindings */
    Event[] value();
}
