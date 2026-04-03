package be.appify.prefab.postgres.spring.data.jdbc;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jdbc.core.convert.Identifier;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.domain.RowDocument;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;

/**
 * Custom {@link MappingJdbcConverter} that provides generic read/write support for single-field Java records and
 * applies registered custom converters to individual elements when reading array/collection columns.
 * <p>
 * Any Java record with exactly one component is automatically handled without requiring an explicit
 * {@code @WritingConverter}/{@code @ReadingConverter} pair:
 * </p>
 * <ul>
 *   <li><b>Writing:</b> the single component value is extracted via reflection and written as a plain scalar.</li>
 *   <li><b>Reading:</b> the record is instantiated via its canonical constructor using the raw column value,
 *       with automatic type coercion via the Spring {@link org.springframework.core.convert.ConversionService}.</li>
 * </ul>
 * <p>
 * The converter also handles {@code List<SingleFieldRecord>} columns (e.g. arrays read from {@code VARCHAR[]}),
 * applying the same per-element logic in addition to any registered custom {@code @ReadingConverter}s.
 * </p>
 */
public class PrefabMappingJdbcConverter extends MappingJdbcConverter {

    /**
     * Constructs a new PrefabMappingJdbcConverter.
     *
     * @param context
     *         the JdbcMappingContext to use for mapping entities
     * @param relationResolver
     *         the RelationResolver to use for resolving entity relationships
     * @param conversions
     *         the JdbcCustomConversions to use for custom type conversions
     * @param typeFactory
     *         the JdbcTypeFactory to use for determining SQL types
     */
    public PrefabMappingJdbcConverter(
            JdbcMappingContext context,
            RelationResolver relationResolver,
            JdbcCustomConversions conversions,
            JdbcTypeFactory typeFactory
    ) {
        super(context, relationResolver, conversions, typeFactory);
    }

    @Override
    public Class<?> getColumnType(RelationalPersistentProperty property) {
        if (!property.isCollectionLike()) {
            Class<?> actualType = property.getActualType();
            if (isSingleFieldRecord(actualType)) {
                return actualType.getRecordComponents()[0].getType();
            }
        }
        return super.getColumnType(property);
    }

    @Override
    public SQLType getTargetSqlType(RelationalPersistentProperty property) {
        if (!property.isCollectionLike()) {
            Class<?> actualType = property.getActualType();
            if (isSingleFieldRecord(actualType)) {
                return sqlTypeFor(actualType.getRecordComponents()[0].getType());
            }
        }
        return super.getTargetSqlType(property);
    }

    @Override
    @Nullable
    public Object readValue(@Nullable Object value, TypeInformation<?> type) {
        // Handle scalar single-field record (e.g. a Reference<T> property)
        if (value != null && isSingleFieldRecord(type.getType())) {
            return constructSingleFieldRecord(type.getType(), value);
        }

        Object result = super.readValue(value, type);

        // Handle List<SingleFieldRecord> — e.g. reading from a VARCHAR[] array column
        if (result instanceof List<?> list && type.isCollectionLike() && !list.isEmpty()) {
            TypeInformation<?> componentType = type.getRequiredComponentType();
            Class<?> targetType = componentType.getType();
            Object firstElement = list.getFirst();

            if (firstElement != null && !targetType.isInstance(firstElement)) {
                if (isSingleFieldRecord(targetType)) {
                    List<Object> converted = new ArrayList<>(list.size());
                    for (Object element : list) {
                        converted.add(constructSingleFieldRecord(targetType, element));
                    }
                    return converted;
                }
                if (getConversions().hasCustomReadTarget(firstElement.getClass(), targetType)) {
                    List<Object> converted = new ArrayList<>(list.size());
                    for (Object element : list) {
                        converted.add(getConversionService().convert(element, targetType));
                    }
                    return converted;
                }
            }
        }

        return result;
    }

    @Override
    @Nullable
    public Object writeValue(@Nullable Object value, TypeInformation<?> type) {
        if (value != null && isSingleFieldRecord(value.getClass())) {
            try {
                var component = value.getClass().getRecordComponents()[0];
                Object fieldValue = component.getAccessor().invoke(value);
                return super.writeValue(fieldValue, TypeInformation.of(component.getType()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Cannot extract single-field value from record " + value.getClass() + " during write", e);
            }
        }
        return super.writeValue(value, type);
    }

    /**
     * Reads a polymorphic aggregate from a row document by delegating to the registered
     * {@link be.appify.prefab.core.spring.data.jdbc.PolymorphicReadingConverter} when the target type is a sealed interface.
     *
     * <p>For all other types the standard Spring Data JDBC reading path is used.</p>
     */
    @Override
    public <R> R read(Class<R> type, RowDocument source) {
        if (type.isSealed() && type.isInterface()
                && getConversionService().canConvert(source.getClass(), type)) {
            @SuppressWarnings("unchecked")
            R result = (R) getConversionService().convert(source, type);
            return result;
        }
        return super.read(type, source);
    }

    /**
     * Reads and resolves a polymorphic aggregate from a row document. Delegates to the registered
     * {@link be.appify.prefab.core.spring.data.jdbc.PolymorphicReadingConverter} when the target type is a sealed interface; otherwise uses the
     * standard Spring Data JDBC reading path.
     */
    @Override
    public <R> R readAndResolve(TypeInformation<R> type, RowDocument source, Identifier identifier) {
        Class<R> rawType = type.getType();
        if (rawType.isSealed() && rawType.isInterface()
                && getConversionService().canConvert(source.getClass(), rawType)) {
            @SuppressWarnings("unchecked")
            R result = (R) getConversionService().convert(source, rawType);
            return result;
        }
        return super.readAndResolve(type, source, identifier);
    }

    private static boolean isSingleFieldRecord(Class<?> type) {
        return type.isRecord() && type.getRecordComponents().length == 1;
    }

    /**
     * Maps a Java type to its corresponding JDBC {@link SQLType}. Uses Spring's {@link StatementCreatorUtils} for
     * standard type mappings, falling back to {@link JDBCType#OTHER} for unknown types.
     *
     * @param javaType
     *         the Java type to resolve
     * @return the SQL type for the given Java type
     */
    static SQLType sqlTypeFor(Class<?> javaType) {
        int code = StatementCreatorUtils.javaTypeToSqlParameterType(javaType);
        if (code != SqlTypeValue.TYPE_UNKNOWN) {
            try {
                return JDBCType.valueOf(code);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return JDBCType.OTHER;
    }

    private Object constructSingleFieldRecord(Class<?> targetType, Object rawValue) {
        var component = targetType.getRecordComponents()[0];
        Class<?> componentType = component.getType();
        Object value = rawValue;
        if (!componentType.isInstance(rawValue)) {
            try {
                value = getConversionService().convert(rawValue, componentType);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Cannot convert " + rawValue.getClass().getName() + " to " + componentType.getName()
                                + " for single-field record " + targetType, e);
            }
        }
        try {
            var constructor = targetType.getDeclaredConstructor(componentType);
            return constructor.newInstance(value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot construct single-field record " + targetType
                            + " from value of type " + rawValue.getClass().getName(), e);
        }
    }
}
