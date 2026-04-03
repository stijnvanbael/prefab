package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.Aggregate;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Lazy;
import org.springframework.util.StringUtils;

/**
 * Custom PersistentEntity that automatically resolves the preferred constructor for entity instantiation:
 * <ul>
 *     <li>For records: uses the canonical constructor (matching all record components)</li>
 *     <li>For classes: uses the constructor whose parameters match all persistent properties by name</li>
 * </ul>
 * This removes the need for explicit {@code @PersistenceCreator} annotations in most cases.
 * An explicit {@code @PersistenceCreator} annotation is still respected and takes precedence.
 *
 * @param <T>
 *         the type of the entity
 */
public class PrefabPersistentEntity<T> extends BasicPersistentEntity<T, RelationalPersistentProperty> implements
        RelationalPersistentEntity<T> {

    private static final ValueExpressionParser PARSER = ValueExpressionParser.create();

    private final Lazy<InstanceCreatorMetadata<RelationalPersistentProperty>> preferredCreator =
            Lazy.of(this::resolvePreferredCreator);
    private final Lazy<SqlIdentifier> tableName;
    private final @Nullable ValueExpression tableNameExpression;
    private final Lazy<Optional<SqlIdentifier>> schemaName;
    private final @Nullable ValueExpression schemaNameExpression;
    private final SqlIdentifierExpressionEvaluator sqlIdentifierExpressionEvaluator;

    /**
     * Constructs a new PrefabPersistentEntity.
     *
     * @param information
     *         the type information for the entity
     * @param namingStrategy
     *         the naming strategy to use for deriving table and schema names
     * @param sqlIdentifierExpressionEvaluator
     *         the evaluator to use for resolving SQL identifier expressions in table and schema names
     */
    public PrefabPersistentEntity(
            TypeInformation<T> information,
            NamingStrategy namingStrategy,
            SqlIdentifierExpressionEvaluator sqlIdentifierExpressionEvaluator
    ) {
        super(information);
        this.sqlIdentifierExpressionEvaluator = sqlIdentifierExpressionEvaluator;

        Lazy<Optional<SqlIdentifier>> defaultSchema = Lazy.of(() -> StringUtils.hasText(namingStrategy.getSchema())
                ? Optional.of(createDerivedSqlIdentifier(namingStrategy.getSchema()))
                : Optional.empty());

        if (isAnnotationPresent(Table.class)) {

            Table table = getRequiredAnnotation(Table.class);

            this.tableName = Lazy.of(() -> StringUtils.hasText(table.value()) ? createSqlIdentifier(table.value())
                    : createDerivedSqlIdentifier(namingStrategy.getTableName(getType())));
            this.tableNameExpression = detectExpression(table.value());

            this.schemaName = StringUtils.hasText(table.schema())
                    ? Lazy.of(() -> Optional.of(createSqlIdentifier(table.schema())))
                    : defaultSchema;
            this.schemaNameExpression = detectExpression(table.schema());

        } else {

            this.tableName = Lazy.of(() -> {
                Class<?> sealedAggregateParent = findDirectSealedAggregateInterface(getType());
                String tableName = sealedAggregateParent != null
                        ? namingStrategy.getTableName(sealedAggregateParent)
                        : namingStrategy.getTableName(getType());
                return createDerivedSqlIdentifier(tableName);
            });
            this.tableNameExpression = null;
            this.schemaName = defaultSchema;
            this.schemaNameExpression = null;
        }
    }

    @Override
    public InstanceCreatorMetadata<RelationalPersistentProperty> getInstanceCreatorMetadata() {
        return preferredCreator.getNullable();
    }

    private InstanceCreatorMetadata<RelationalPersistentProperty> resolvePreferredCreator() {
        var defaultCreator = super.getInstanceCreatorMetadata();

        if (hasExplicitPersistenceCreator(defaultCreator)) {
            return defaultCreator;
        }

        Class<T> type = getType();

        if (type.isRecord()) {
            var canonical = findCanonicalConstructor(type);
            if (canonical != null) {
                return buildCreator(canonical);
            }
        }

        var allFieldsCtor = findAllFieldsConstructor(type);
        if (allFieldsCtor != null) {
            return buildCreator(allFieldsCtor);
        }

        return defaultCreator;
    }

    private boolean hasExplicitPersistenceCreator(InstanceCreatorMetadata<RelationalPersistentProperty> creator) {
        return creator instanceof PreferredConstructor<?, ?> pc
                && pc.getConstructor().isAnnotationPresent(PersistenceCreator.class);
    }

    private Constructor<T> findCanonicalConstructor(Class<T> type) {
        try {
            Class<?>[] paramTypes = Arrays.stream(type.getRecordComponents())
                    .map(java.lang.reflect.RecordComponent::getType)
                    .toArray(Class<?>[]::new);
            return type.getDeclaredConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Constructor<T> findAllFieldsConstructor(Class<T> type) {
        Set<String> propertyNames = new LinkedHashSet<>();
        doWithProperties((PropertyHandler<RelationalPersistentProperty>) property -> propertyNames.add(property.getName()));

        if (propertyNames.isEmpty()) {
            return null;
        }

        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == propertyNames.size()) {
                Set<String> paramNames = Arrays.stream(constructor.getParameters())
                        .map(Parameter::getName)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (paramNames.equals(propertyNames)) {
                    return (Constructor<T>) constructor;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private PreferredConstructor<T, RelationalPersistentProperty> buildCreator(Constructor<T> constructor) {
        var parameters = IntStream.range(0, constructor.getParameterCount())
                .mapToObj(index -> mapParameter(new MethodParameter(constructor, index)))
                .toArray(org.springframework.data.mapping.Parameter[]::new);
        return new PreferredConstructor<>(constructor, parameters);
    }

    private org.springframework.data.mapping.Parameter<T, RelationalPersistentProperty> mapParameter(MethodParameter parameter) {
        return new org.springframework.data.mapping.Parameter<>(
                parameter.getParameterName(),
                (TypeInformation<T>) TypeInformation.of(ResolvableType.forMethodParameter(parameter)),
                parameter.getParameterAnnotations(),
                this);
    }

    private static @Nullable ValueExpression detectExpression(@Nullable String potentialExpression) {

        if (!StringUtils.hasText(potentialExpression)) {
            return null;
        }

        ValueExpression expression = PARSER.parse(potentialExpression);
        return expression.isLiteral() ? null : expression;
    }

    private SqlIdentifier createSqlIdentifier(String name) {
        return isForceQuote() ? SqlIdentifier.quoted(name) : SqlIdentifier.unquoted(name);
    }

    private SqlIdentifier createDerivedSqlIdentifier(String name) {
        return new DerivedSqlIdentifier(name, isForceQuote());
    }

    public boolean isForceQuote() {
        return true;
    }

    @Override
    public SqlIdentifier getTableName() {

        if (tableNameExpression == null) {
            return tableName.get();
        }

        return sqlIdentifierExpressionEvaluator.evaluate(tableNameExpression, isForceQuote());
    }

    @Override
    public SqlIdentifier getQualifiedTableName() {

        SqlIdentifier schema;
        if (schemaNameExpression != null) {
            schema = sqlIdentifierExpressionEvaluator.evaluate(schemaNameExpression, isForceQuote());
        } else {
            schema = schemaName.get().orElse(null);
        }

        if (schema == null) {
            return getTableName();
        }

        if (schemaNameExpression != null) {
            schema = sqlIdentifierExpressionEvaluator.evaluate(schemaNameExpression, isForceQuote());
        }

        return SqlIdentifier.from(schema, getTableName());
    }

    @Override
    @Deprecated(forRemoval = true)
    public SqlIdentifier getIdColumn() {
        return getRequiredIdProperty().getColumnName();
    }

    /**
     * Returns the first directly-implemented sealed interface that is annotated with {@link Aggregate}, or
     * {@code null} if the given type is not a subtype of a polymorphic aggregate.
     *
     * @param type
     *         the type to inspect
     * @return the sealed {@link Aggregate} interface, or {@code null}
     */
    static @Nullable Class<?> findDirectSealedAggregateInterface(Class<?> type) {
        for (Class<?> iface : type.getInterfaces()) {
            if (iface.isSealed() && iface.isAnnotationPresent(Aggregate.class)) {
                return iface;
            }
        }
        return null;
    }

}
