package be.appify.prefab.postgres.spring.data.jdbc;

import java.util.Set;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * A {@link SimpleTypeHolder} that recognises single-field Java records as simple (scalar) types.
 * <p>
 * Any record with exactly one component (e.g. {@code record Reference<T>(String id)}) is treated as a scalar value
 * by Spring Data JDBC, so it is stored as a plain column rather than being embedded or mapped as a nested entity.
 * This removes the need for per-type {@code WritingConverter}/{@code ReadingConverter} registrations; instead,
 * {@link PrefabMappingJdbcConverter} handles the conversion generically via reflection.
 * </p>
 *
 * @see PrefabMappingJdbcConverter
 */
public class SingleValueRecordSimpleTypeHolder extends SimpleTypeHolder {

    /**
     * Constructs a new {@code SingleValueRecordSimpleTypeHolder} that delegates to the given {@code source} for all
     * existing simple-type checks, and additionally treats single-field records as simple types.
     *
     * @param source
     *         the base {@link SimpleTypeHolder} to delegate to
     */
    public SingleValueRecordSimpleTypeHolder(SimpleTypeHolder source) {
        super(Set.of(), source);
    }

    @Override
    public boolean isSimpleType(Class<?> type) {
        return super.isSimpleType(type)
                || (type.isRecord() && type.getRecordComponents().length == 1 && !wrapsMultiFieldRecord(type));
    }

    /**
     * Returns true if the given type is a single-field record that ultimately wraps a multi-field record through one
     * or more layers of single-field record wrappers. Such types must not be treated as scalars; they expand into
     * multiple columns via the embedded path.
     *
     * @param type
     *         the type to inspect; must be a single-field record
     * @return true if the innermost non-wrapper type is a record with more than one component
     */
    static boolean wrapsMultiFieldRecord(Class<?> type) {
        Class<?> current = type.getRecordComponents()[0].getType();
        while (current.isRecord() && current.getRecordComponents().length == 1) {
            current = current.getRecordComponents()[0].getType();
        }
        return current.isRecord();
    }
}
