package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate a database migration script for this aggregate root.
 *
 * <p>When {@code spring-boot-starter-data-jdbc} (or equivalent) is on the classpath a SQL script is
 * generated under {@code db/migration/}. When {@code spring-boot-starter-data-mongodb} (or equivalent)
 * is on the classpath a MongoDB JavaScript migration script is generated under
 * {@code mongo/migration/} instead. Both backends are handled transparently based on what is present
 * at compile time.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DbMigration {
}
