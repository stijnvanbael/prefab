package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeatable {@link AvscInterface} declarations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface AvscInterfaces {

    /**
     * Declared AVSC-to-interface mappings.
     *
     * @return the mappings declared on the contract
     */
    AvscInterface[] value();
}
