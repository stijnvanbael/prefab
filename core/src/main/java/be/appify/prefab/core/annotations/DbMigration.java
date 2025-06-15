package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Generate a SQL migration script for this aggregate root
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DbMigration {
    /// @return the version of the migration script or order to run, defaults to 1
    int version() default 1;
}
