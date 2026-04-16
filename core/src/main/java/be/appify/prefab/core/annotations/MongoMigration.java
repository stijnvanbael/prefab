package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate a MongoDB migration script for this aggregate root.
 *
 * @deprecated Use {@link DbMigration} instead. {@code @DbMigration} now works transparently for both
 *             SQL and MongoDB backends depending on what is present on the classpath.
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MongoMigration {
}
