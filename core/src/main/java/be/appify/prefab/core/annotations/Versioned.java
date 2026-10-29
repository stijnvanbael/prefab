package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the version of a component type that is nested in events, such as an address. Migrations of the type
 * ({@link EventMigration} on a {@code Migration<AddressV1, Address>}) are applied wherever the type occurs in an event.
 * Producers stamp the version of every versioned component in the {@code prefab-component-versions} header.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Versioned {
    /**
     * The semantic version of the component type.
     *
     * @return the version, {@code major.minor.patch}
     */
    String version() default "1.0.0";
}
