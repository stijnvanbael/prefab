package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a snapshot record of a historic shape of an Avro event from a separate AVSC file. The record can be used
 * as the {@code Old} type of a {@link be.appify.prefab.core.versioning.Migration}. Place the annotation on any type
 * of the application.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface EventSnapshot {
    /**
     * The classpath location of the AVSC file that describes the historic shape.
     *
     * @return the AVSC file location
     */
    String avsc();

    /**
     * The name of the generated record for the root schema. Defaults to the record name in the AVSC file, which clashes
     * with the current event when both live in the same package.
     *
     * @return the name of the generated record, empty to use the name in the AVSC file
     */
    String name() default "";
}
