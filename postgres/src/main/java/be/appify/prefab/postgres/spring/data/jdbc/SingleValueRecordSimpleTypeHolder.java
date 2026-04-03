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
                || (type.isRecord() && type.getRecordComponents().length == 1);
    }
}
