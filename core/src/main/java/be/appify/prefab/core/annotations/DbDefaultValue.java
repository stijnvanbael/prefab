package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specifies a default value for a database column. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DbDefaultValue {
    /**
     * The default value as a plain (non-SQL) string.
     * The annotation processor will automatically apply the correct SQL quoting based on the field's data type.
     * For example, use {@code "active"} for a string column or {@code "0"} for a numeric column.
     * @return The plain default value.
     */
    String value();
}
