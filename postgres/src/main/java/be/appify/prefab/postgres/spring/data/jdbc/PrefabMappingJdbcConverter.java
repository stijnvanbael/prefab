package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.DbDocument;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.postgresql.util.PGobject;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jdbc.core.convert.Identifier;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.relational.core.mapping.AggregatePath;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.SqlIdentifier;
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
 * <p>
 * For polymorphic aggregates (sealed interface + record subtypes), this converter is responsible for loading child
 * collections that are stored in separate tables. Spring Data JDBC's standard {@code readAggregate} path is bypassed
 * for sealed interface types because the registered reading converter short-circuits relation resolution.
 * After the flat row is converted to the concrete subtype via the reading converter, child collections are loaded
 * manually using the {@link RelationResolver} and the record is reconstructed with the resolved children.
 * </p>
 */
public class PrefabMappingJdbcConverter extends MappingJdbcConverter {

    private final JsonMapper jsonMapper;
    private final RelationResolver relationResolver;
    private final RelationalMappingContext mappingContext;

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
        this.relationResolver = relationResolver;
        this.mappingContext = context;
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
        Class<?> sealedInterface = PrefabPersistentEntity.findDirectSealedAggregateInterface(type);
        if (sealedInterface != null && getConversionService().canConvert(source.getClass(), sealedInterface)) {
            Object converted = getConversionService().convert(source, sealedInterface);
            if (converted != null) {
                return type.cast(converted);
            }
        }
        return super.read(type, source);
    }

    /**
     * Reads a polymorphic aggregate root from the given row document and loads child collections via the
     * {@link RelationResolver}.
     * <p>
     * Spring Data JDBC's standard {@code readAggregate} cannot be used for this because the presence of a registered
     * reading converter (e.g. {@code QuizReadingConverter}) causes {@code readAggregate} to short-circuit via
     * {@code hasCustomReadTarget}, bypassing the entity-based relation loading entirely. Furthermore, the supertype
     * matching in {@code CustomConversions} causes {@code hasCustomReadTarget(RowDocument, ConcreteSubtype)} to return
     * {@code true} even though no converter for the concrete subtype exists, leading to a
     * {@code ConverterNotFoundException}.
     * <p>
     * This implementation avoids both problems by:
     * <ol>
     *   <li>Using the registered reading converter to instantiate the correct concrete subtype from the flat row.</li>
     *   <li>Manually loading child collections using the {@link RelationResolver} and reconstructing the record
     *       via its canonical constructor.</li>
     * </ol>
     */
    @Override
    public <R> R readAndResolve(TypeInformation<R> type, RowDocument source, Identifier identifier) {
        Class<R> rawType = type.getType();
        if (isSealedInterfaceConvertible(rawType, source)) {
            R instance = requireConvert(source, rawType);
            return resolveChildren(instance, source);
        }
        Class<?> sealedInterface = PrefabPersistentEntity.findDirectSealedAggregateInterface(rawType);
        if (sealedInterface != null && getConversionService().canConvert(source.getClass(), sealedInterface)) {
            R instance = rawType.cast(getConversionService().convert(source, sealedInterface));
            return resolveChildren(instance, source);
        }
        return super.readAndResolve(type, source, identifier);
    }

    @SuppressWarnings("unchecked")
    private <R> R resolveChildren(R instance, RowDocument source) {
        Class<R> type = (Class<R>) instance.getClass();
        RelationalPersistentEntity<R> entity =
                (RelationalPersistentEntity<R>) mappingContext.getRequiredPersistentEntity(type);

        List<RelationalPersistentProperty> collectionProperties = findChildCollectionProperties(entity);
        if (collectionProperties.isEmpty()) {
            return instance;
        }

        if (!entity.hasIdProperty()) {
            return instance;
        }
        Object rawId = source.get(entity.getRequiredIdProperty().getColumnName().getReference());
        if (rawId == null) {
            return instance;
        }

        Map<String, Iterable<Object>> loadedChildren = loadChildCollections(entity, rawId, collectionProperties);
        return reconstructWithChildren(instance, type, loadedChildren);
    }

    private List<RelationalPersistentProperty> findChildCollectionProperties(RelationalPersistentEntity<?> entity) {
        List<RelationalPersistentProperty> result = new ArrayList<>();
        entity.doWithProperties(
                (PropertyHandler<RelationalPersistentProperty>) property -> {
                    if (property.isCollectionLike() && property.isEntity() && !property.isMap()) {
                        result.add(property);
                    }
                });
        return result;
    }

    private <R> Map<String, Iterable<Object>> loadChildCollections(
            RelationalPersistentEntity<R> entity,
            Object rawId,
            List<RelationalPersistentProperty> collectionProperties
    ) {
        Map<String, Iterable<Object>> result = new HashMap<>();
        AggregatePath entityPath = mappingContext.getAggregatePath(entity);

        for (RelationalPersistentProperty property : collectionProperties) {
            AggregatePath propertyPath = entityPath.append(property);
            AggregatePath.TableInfo tableInfo = propertyPath.getTableInfo();
            if (tableInfo.reverseColumnInfo() == null) {
                continue;
            }
            SqlIdentifier fkColumn = tableInfo.reverseColumnInfo().name();
            Identifier childIdentifier = Identifier.of(fkColumn, rawId, rawId.getClass());
            Iterable<Object> children = relationResolver.findAllByPath(
                    childIdentifier,
                    propertyPath.getRequiredPersistentPropertyPath());
            result.put(property.getName(), children);
        }

        return result;
    }

    private <R> R reconstructWithChildren(
            R instance,
            Class<R> type,
            Map<String, Iterable<Object>> loadedChildren
    ) {
        if (!type.isRecord()) {
            return instance;
        }
        RecordComponent[] components = type.getRecordComponents();
        Object[] values = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            if (loadedChildren.containsKey(component.getName())) {
                values[i] = materializeCollection(loadedChildren.get(component.getName()), component.getType());
            } else {
                values[i] = invokeAccessor(instance, component);
            }
        }

        Class<?>[] paramTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
        try {
            return type.getDeclaredConstructor(paramTypes).newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot reconstruct " + type.getName() + " with resolved children", e);
        }
    }

    private static Object invokeAccessor(Object instance, RecordComponent component) {
        try {
            return component.getAccessor().invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot read component " + component.getName() + " of " + instance.getClass().getName(), e);
        }
    }

    private static Object materializeCollection(Iterable<Object> items, Class<?> targetType) {
        List<Object> list = new ArrayList<>();
        items.forEach(list::add);
        if (Set.class.isAssignableFrom(targetType)) {
            return new LinkedHashSet<>(list);
        }
        return list;
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
        var componentType = type.getComponentType();
        if (componentType != null) {
            return jsonMapper.getTypeFactory()
                    .constructCollectionType(List.class, componentType.getType());
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
