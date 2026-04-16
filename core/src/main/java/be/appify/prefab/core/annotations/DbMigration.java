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
 * {@code mongo/migration/}. If both backends are present at compile time, both SQL and MongoDB
 * migrations are generated. If only one backend is present, only that backend's migration is
 * generated.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DbMigration {
}
