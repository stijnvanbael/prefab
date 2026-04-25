package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.DbDocument;
import java.lang.reflect.Array;
import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.postgresql.util.PGobject;
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
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Custom {@link MappingJdbcConverter} that provides generic read/write support for single-field Java records,
 * JSONB documents, and applies registered custom converters to individual elements when reading array/collection
 * columns.
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
 * Types annotated with {@link DbDocument} are serialized as PostgreSQL {@code JSONB} documents using Jackson.
 * Lists of {@link DbDocument}-annotated element types are also stored as JSONB arrays.
 * </p>
 * <p>
 * The converter also handles {@code List<SingleFieldRecord>} columns (e.g. arrays read from {@code VARCHAR[]}),
 * applying the same per-element logic in addition to any registered custom {@code @ReadingConverter}s.
 * </p>
 */
public class PrefabMappingJdbcConverter extends MappingJdbcConverter {

    private final JsonMapper jsonMapper;

    /**
     * Constructs a new PrefabMappingJdbcConverter.
     *
     * @param context          the JdbcMappingContext to use for mapping entities
     * @param relationResolver the RelationResolver to use for resolving entity relationships
     * @param conversions      the JdbcCustomConversions to use for custom type conversions
     * @param typeFactory      the JdbcTypeFactory to use for determining SQL types
     * @param jsonMapper       the JsonMapper to use for JSONB serialization and deserialization
     */
    public PrefabMappingJdbcConverter(
            JdbcMappingContext context,
            RelationResolver relationResolver,
            JdbcCustomConversions conversions,
            JdbcTypeFactory typeFactory,
            JsonMapper jsonMapper
    ) {
        super(context, relationResolver, conversions, typeFactory);
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Class<?> getColumnType(RelationalPersistentProperty property) {
        if (isJsonbProperty(property)) {
            return PGobject.class;
        }
        if (!property.isCollectionLike()) {
            Class<?> actualType = property.getActualType();
            if (isUnwrappableSingleFieldRecord(actualType)) {
                return actualType.getRecordComponents()[0].getType();
            }
        }
        return super.getColumnType(property);
    }

    @Override
    public SQLType getTargetSqlType(RelationalPersistentProperty property) {
        if (isJsonbProperty(property)) {
            return JDBCType.OTHER;
        }
        if (!property.isCollectionLike()) {
            Class<?> actualType = property.getActualType();
            if (isUnwrappableSingleFieldRecord(actualType)) {
                return sqlTypeFor(actualType.getRecordComponents()[0].getType());
            }
        }
        return super.getTargetSqlType(property);
    }

    @Override
    @Nullable
    public Object readValue(@Nullable Object value, TypeInformation<?> type) {
        if (value == null) {
            return super.readValue(null, type);
        }
        if (isJsonbValue(value)) {
            return readJsonbValue((PGobject) value, type);
        }
        if (isUnwrappableSingleFieldRecord(type.getType())) {
            return constructSingleFieldRecord(type.getType(), value);
        }
        if (value.getClass().isArray() && type.isCollectionLike()) {
            List<Object> converted = convertArrayToList(value, type);
            if (converted != null) {
                return converted;
            }
        }
        Object result = super.readValue(value, type);
        if (result instanceof List<?> list && type.isCollectionLike()) {
            List<Object> converted = convertListElements(list, type);
            if (converted != null) {
                return converted;
            }
        }
        return result;
    }

    @Override
    @Nullable
    public Object writeValue(@Nullable Object value, TypeInformation<?> type) {
        if (value == null) {
            return super.writeValue(null, type);
        }
        if (isDbDocumentValue(value)) {
            return serializeToJsonb(value);
        }
        if (value instanceof List<?> list && isJsonbListType(type)) {
            return serializeToJsonb(list);
        }
        if (isUnwrappableSingleFieldRecord(value.getClass())) {
            return writeUnwrappedRecord(value);
        }
        return super.writeValue(value, type);
    }

    @Override
    public <R> R read(Class<R> type, RowDocument source) {
        if (isSealedInterfaceConvertible(type, source)) {
            return requireConvert(source, type);
        }
        return super.read(type, source);
    }

    @Override
    public <R> R readAndResolve(TypeInformation<R> type, RowDocument source, Identifier identifier) {
        Class<R> rawType = type.getType();
        if (isSealedInterfaceConvertible(rawType, source)) {
            return requireConvert(source, rawType);
        }
        return super.readAndResolve(type, source, identifier);
    }

    static SQLType sqlTypeFor(Class<?> javaType) {
        int code = StatementCreatorUtils.javaTypeToSqlParameterType(javaType);
        if (code != SqlTypeValue.TYPE_UNKNOWN) {
            try {
                return JDBCType.valueOf(code);
            } catch (IllegalArgumentException ignored) {
                // fall through to OTHER
            }
        }
        return JDBCType.OTHER;
    }

    private boolean isJsonbProperty(RelationalPersistentProperty property) {
        if (property.findAnnotation(DbDocument.class) != null) {
            return true;
        }
        Class<?> actualType = property.getActualType();
        return actualType.isAnnotationPresent(DbDocument.class);
    }

    private boolean isDbDocumentValue(Object value) {
        return value.getClass().isAnnotationPresent(DbDocument.class);
    }

    private boolean isJsonbListType(TypeInformation<?> type) {
        if (!type.isCollectionLike()) {
            return false;
        }
        var componentType = type.getComponentType();
        return componentType != null && componentType.getType().isAnnotationPresent(DbDocument.class);
    }

    private static boolean isJsonbValue(Object value) {
        return value instanceof PGobject pgo && "jsonb".equalsIgnoreCase(pgo.getType());
    }

    @Nullable
    private Object readJsonbValue(PGobject pgObject, TypeInformation<?> type) {
        var json = pgObject.getValue();
        if (json == null) {
            return null;
        }
        try {
            JavaType javaType = resolveJavaType(type);
            return jsonMapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSONB value to " + type.getType(), e);
        }
    }

    private JavaType resolveJavaType(TypeInformation<?> type) {
        if (type.isCollectionLike()) {
            var componentType = type.getComponentType();
            if (componentType != null) {
                return jsonMapper.getTypeFactory()
                        .constructCollectionType(List.class, componentType.getType());
            }
        }
        return jsonMapper.getTypeFactory().constructType(type.getType());
    }

    private PGobject serializeToJsonb(Object value) {
        try {
            var pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(jsonMapper.writeValueAsString(value));
            return pgObject;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize value to JSONB: " + value.getClass(), e);
        }
    }

    @Nullable
    private List<Object> convertArrayToList(Object array, TypeInformation<?> type) {
        Class<?> elementTargetType = type.getRequiredComponentType().getType();
        if (!isSingleFieldRecord(elementTargetType)) {
            return null;
        }
        Object[] elements = toObjectArray(array);
        List<Object> converted = new ArrayList<>(elements.length);
        for (Object element : elements) {
            converted.add(constructSingleFieldRecord(elementTargetType, element));
        }
        return converted;
    }

    @Nullable
    private List<Object> convertListElements(List<?> list, TypeInformation<?> type) {
        if (list.isEmpty()) {
            return null;
        }
        Class<?> targetType = type.getRequiredComponentType().getType();
        Object firstElement = list.getFirst();
        if (firstElement == null || targetType.isInstance(firstElement)) {
            return null;
        }
        if (isSingleFieldRecord(targetType)) {
            return convertToSingleFieldRecords(list, targetType);
        }
        if (getConversions().hasCustomReadTarget(firstElement.getClass(), targetType)) {
            return convertWithConversionService(list, targetType);
        }
        return null;
    }

    private List<Object> convertToSingleFieldRecords(List<?> list, Class<?> targetType) {
        List<Object> converted = new ArrayList<>(list.size());
        for (Object element : list) {
            converted.add(constructSingleFieldRecord(targetType, element));
        }
        return converted;
    }

    private List<Object> convertWithConversionService(List<?> list, Class<?> targetType) {
        List<Object> converted = new ArrayList<>(list.size());
        for (Object element : list) {
            converted.add(getConversionService().convert(element, targetType));
        }
        return converted;
    }

    private Object writeUnwrappedRecord(Object value) {
        try {
            var component = value.getClass().getRecordComponents()[0];
            Object fieldValue = component.getAccessor().invoke(value);
            return super.writeValue(fieldValue, TypeInformation.of(component.getType()));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot extract single-field value from record " + value.getClass() + " during write", e);
        }
    }

    private boolean isSealedInterfaceConvertible(Class<?> type, RowDocument source) {
        return type.isSealed() && type.isInterface() && getConversionService().canConvert(source.getClass(), type);
    }

    private <R> R requireConvert(RowDocument source, Class<R> type) {
        R result = getConversionService().convert(source, type);
        if (result == null) {
            throw new IllegalStateException("ConversionService returned null converting RowDocument to " + type);
        }
        return result;
    }

    private Object constructSingleFieldRecord(Class<?> targetType, Object rawValue) {
        var component = targetType.getRecordComponents()[0];
        Class<?> componentType = component.getType();
        Object coerced = coerceToComponentType(rawValue, componentType, targetType);
        return instantiateRecord(targetType, componentType, coerced, rawValue);
    }

    private Object coerceToComponentType(Object rawValue, Class<?> componentType, Class<?> targetType) {
        if (componentType.isInstance(rawValue)) {
            return rawValue;
        }
        try {
            return getConversionService().convert(rawValue, componentType);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot convert " + rawValue.getClass().getName() + " to " + componentType.getName()
                            + " for single-field record " + targetType, e);
        }
    }

    private static Object instantiateRecord(Class<?> targetType, Class<?> componentType, Object value, Object rawValue) {
        try {
            return targetType.getDeclaredConstructor(componentType).newInstance(value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot construct single-field record " + targetType
                            + " from value of type " + rawValue.getClass().getName(), e);
        }
    }

    private static boolean isUnwrappableSingleFieldRecord(Class<?> type) {
        return isSingleFieldRecord(type) && !SingleValueRecordSimpleTypeHolder.wrapsMultiFieldRecord(type);
    }

    private static boolean isSingleFieldRecord(Class<?> type) {
        return type.isRecord() && type.getRecordComponents().length == 1;
    }

    private static Object[] toObjectArray(Object array) {
        if (array instanceof Object[] objects) {
            return objects;
        }
        int length = Array.getLength(array);
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = Array.get(array, i);
        }
        return result;
    }
}
