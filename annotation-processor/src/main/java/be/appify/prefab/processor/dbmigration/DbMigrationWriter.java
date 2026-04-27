package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbDocument;
import be.appify.prefab.core.annotations.DbRename;
import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.TenantId;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ListUtil;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.springframework.data.annotation.Id;

import static be.appify.prefab.core.util.IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH;
import static be.appify.prefab.core.util.IdentifierShortener.columnName;
import static be.appify.prefab.core.util.IdentifierShortener.foreignKeyConstraintName;
import static be.appify.prefab.core.util.IdentifierShortener.shorten;
import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

class DbMigrationWriter {

    private final PrefabContext context;

    DbMigrationWriter(PrefabContext context) {
        this.context = context;
    }

    void writeDbMigration(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> classManifests
    ) {
        writeDbMigration(processingEnvironment, classManifests, List.of());
    }

    void writeDbMigration(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> classManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        warnUnconstrainedStringFields(classManifests);
        var currentDatabaseState = currentDatabaseState(processingEnvironment);
        var desiredDatabaseState = desiredDatabaseState(classManifests, polymorphicManifests);
        var changes = detectChanges(currentDatabaseState.tables(), desiredDatabaseState);
        if (!changes.isEmpty()) {
            writeChanges(processingEnvironment, classManifests, polymorphicManifests, changes,
                    currentDatabaseState.version());
        }
    }

    private void warnUnconstrainedStringFields(List<ClassManifest> classManifests) {
        classManifests.forEach(this::warnUnconstrainedStringFields);
    }

    private void warnUnconstrainedStringFields(ClassManifest manifest) {
        manifest.fields().stream()
                .filter(field -> !field.type().is(List.class))
                .filter(field -> !isDbDocumentField(field))
                .forEach(this::warnIfUnconstrainedString);
    }

    private void writeChanges(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> sortedManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests,
            List<DatabaseChange> changes,
            int version
    ) {
        try {
            var resource = createResource(processingEnvironment, sortedManifests, polymorphicManifests, version + 1);
            try (var writer = resource.openWriter()) {
                for (DatabaseChange change : changes) {
                    writer.write(change.toSql());
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<DatabaseChange> detectChanges(List<Table> currentDatabaseState, List<Table> desiredDatabaseState) {
        var changes = new ArrayList<DatabaseChange>();
        var renamedFromNames = new HashSet<String>();

        for (Table desired : desiredDatabaseState) {
            var existing = findTable(currentDatabaseState, desired.name());
            if (existing == null) {
                if (desired.oldName() != null) {
                    var oldTable = findTable(currentDatabaseState, desired.oldName());
                    if (oldTable != null) {
                        renamedFromNames.add(desired.oldName());
                        changes.add(new DatabaseChange.RenameTable(desired.oldName(), desired.name()));
                        // Check if column structure differs (ignoring table name difference)
                        if (!oldTable.columns().equals(desired.columns())
                                || !oldTable.primaryKey().equals(desired.primaryKey())) {
                            changes.add(DatabaseChange.AlterTable.from(oldTable, desired));
                        }
                        continue;
                    }
                }
                changes.add(new DatabaseChange.CreateTable(desired));
                desired.indexes().forEach(index -> changes.add(new DatabaseChange.CreateIndex(desired.name(), index)));
            } else {
                if (!existingColumnsMatch(existing, desired)) {
                    changes.add(DatabaseChange.AlterTable.from(existing, desired));
                }
                detectIndexChanges(existing, desired, changes);
            }
        }
        for (Table existing : currentDatabaseState) {
            var desired = findTable(desiredDatabaseState, existing.name());
            if (desired == null && !renamedFromNames.contains(existing.name())) {
                changes.add(new DatabaseChange.DropTable(existing.name()));
            }
        }
        return changes;
    }

    private static boolean existingColumnsMatch(Table existing, Table desired) {
        return existing.columns().equals(desired.columns()) && existing.primaryKey().equals(desired.primaryKey());
    }

    private static void detectIndexChanges(Table existing, Table desired, List<DatabaseChange> changes) {
        for (Index desiredIndex : desired.indexes()) {
            if (existing.getIndex(desiredIndex.name()).isEmpty()) {
                changes.add(new DatabaseChange.CreateIndex(desired.name(), desiredIndex));
            }
        }
        for (Index existingIndex : existing.indexes()) {
            if (desired.getIndex(existingIndex.name()).isEmpty()) {
                changes.add(new DatabaseChange.DropIndex(existingIndex));
            }
        }
    }

    private Table findTable(List<Table> tables, String name) {
        return tables.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private DatabaseState currentDatabaseState(ProcessingEnvironment processingEnvironment) {
        try {
            var tablesByName = new HashMap<String, Table>();
            var generatedMigrations = existingGeneratedMigrations(processingEnvironment);
            generatedMigrations.forEach(file -> parseSql(file, tablesByName));
            var latestVersion = latestMigrationVersion(processingEnvironment);
            return new DatabaseState(List.copyOf(tablesByName.values()), latestVersion);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<FileObject> existingGeneratedMigrations(ProcessingEnvironment processingEnvironment)
            throws IOException {
        var files = new ArrayList<FileObject>();
        for (int i = 1; i <= MAX_VERSION_PROBE; i++) {
            try {
                var file = processingEnvironment.getFiler().getResource(StandardLocation.CLASS_PATH, "db.migration",
                        "V%d__generated.sql".formatted(i));
                files.add(file);
            } catch (FileNotFoundException ignored) {
            }
        }
        return files;
    }

    private static int latestMigrationVersion(ProcessingEnvironment processingEnvironment) throws IOException {
        var migrationDir = migrationDirectory(processingEnvironment);
        if (migrationDir == null) {
            return 0;
        }
        try (var paths = java.nio.file.Files.list(migrationDir)) {
            return paths
                    .map(p -> p.getFileName().toString())
                    .map(DbMigrationWriter::extractVersion)
                    .filter(v -> v >= 0)
                    .max(Integer::compareTo)
                    .orElse(0);
        } catch (java.nio.file.NoSuchFileException e) {
            return 0;
        }
    }

    private static java.nio.file.Path migrationDirectory(ProcessingEnvironment processingEnvironment)
            throws IOException {
        var fromClassPath = migrationDirectoryFromClassPath(processingEnvironment);
        if (fromClassPath != null) {
            return fromClassPath;
        }
        return migrationDirectoryFromClassOutput(processingEnvironment);
    }

    private static java.nio.file.Path migrationDirectoryFromClassPath(ProcessingEnvironment processingEnvironment)
            throws IOException {
        for (int i = 1; i <= MAX_VERSION_PROBE; i++) {
            var dir = migrationDirectoryFromClassPathVersion(processingEnvironment, i);
            if (dir != null) {
                return dir;
            }
        }
        return null;
    }

    private static java.nio.file.Path migrationDirectoryFromClassPathVersion(
            ProcessingEnvironment processingEnvironment, int version) throws IOException {
        for (String suffix : CLASSPATH_PROBE_SUFFIXES) {
            try {
                var file = processingEnvironment.getFiler().getResource(
                        StandardLocation.CLASS_PATH, "db.migration",
                        "V%d__%s.sql".formatted(version, suffix));
                return java.nio.file.Path.of(file.toUri()).getParent();
            } catch (FileNotFoundException | FileSystemNotFoundException ignored) {
            }
        }
        return null;
    }

    private static final List<String> CLASSPATH_PROBE_SUFFIXES = List.of("generated", "manual", "init", "baseline");

    private static java.nio.file.Path migrationDirectoryFromClassOutput(ProcessingEnvironment processingEnvironment)
            throws IOException {
        try {
            var file = processingEnvironment.getFiler().getResource(
                    StandardLocation.CLASS_OUTPUT, "", "db/migration/.probe");
            var dir = java.nio.file.Path.of(file.toUri()).getParent();
            return java.nio.file.Files.isDirectory(dir) ? dir : null;
        } catch (FileNotFoundException | FileSystemNotFoundException ignored) {
            return null;
        }
    }

    private static final int MAX_VERSION_PROBE = 100;

    private static int extractVersion(String filename) {
        var matcher = java.util.regex.Pattern.compile("^V(\\d+)__.*\\.sql$").matcher(filename);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private static void parseSql(FileObject file, Map<String, Table> tables) {
        try (var input = file.openInputStream()) {
            var content = new String(input.readAllBytes());
            new SqlStatementParser().parse(content, tables);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Table> desiredDatabaseState(List<ClassManifest> classManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests) {
        var tables = new ArrayList<Table>();
        classManifests.forEach(manifest -> {
            var aggregateRootTable = tableNameOf(manifest.type());
            var oldTableName = manifest.annotationsOfType(DbRename.class).stream()
                    .findFirst()
                    .map(ann -> toSnakeCase(ann.value()).replace('.', '_'))
                    .orElse(null);
            var columns = columnsOf(manifest, null, false);
            var fkIndexes = fkIndexesOf(aggregateRootTable, columns);
            var fieldIndexes = fieldIndexesOf(aggregateRootTable, manifest, null);
            var idColumnName = idColumnNameOf(manifest);
            tables.add(new Table(aggregateRootTable, columns, List.of(idColumnName), oldTableName,
                    ListUtil.concat(fkIndexes, fieldIndexes)));
            tables.addAll(childEntityTables(aggregateRootTable, List.of(idColumnName), manifest));
        });
        polymorphicManifests.forEach(manifest -> {
            var rootTable = polymorphicTable(manifest);
            tables.add(rootTable);
            tables.addAll(childTablesForPolymorphicManifest(rootTable, manifest));
        });
        return sortByDependencies(tables);
    }

    private List<Table> childTablesForPolymorphicManifest(Table rootTable, PolymorphicAggregateManifest manifest) {
        return manifest.subtypes().stream()
                .flatMap(subtype -> childEntityTables(rootTable.name(), rootTable.primaryKey(), subtype).stream())
                .collect(Collectors.toMap(Table::name, t -> t, (a, b) -> a))
                .values()
                .stream()
                .toList();
    }

    private Table polymorphicTable(PolymorphicAggregateManifest manifest) {
        var tableName = tableNameOf(manifest.type());
        var commonFieldNames = manifest.commonFields().stream()
                .map(VariableManifest::name)
                .collect(Collectors.toSet());

        // Discriminator column at the top
        var discriminatorColumn = new Column("type", new DataType.Varchar(255), false, null, null);

        // Columns from all subtypes: common ones are NOT NULL, subtype-specific are nullable
        var allColumns = manifest.allFields().stream()
                .filter(field -> !field.type().is(List.class)
                        || field.type().parameters().getFirst().isStandardType()
                        || (field.type().parameters().getFirst().isSingleValueType()
                                && !isWrappedRecordType(field.type().parameters().getFirst()))
                        || isDbDocumentField(field))
                .flatMap(field -> {
                    boolean isSubtypeSpecific = !commonFieldNames.contains(field.name());
                    if (isDbDocumentField(field)) {
                        return Stream.of(Column.fromField(null, field, isSubtypeSpecific || field.nullable(),
                                DataType.Primitive.JSONB));
                    } else if (field.type().isCustomType()) {
                        return customTypeColumn(null, field, isSubtypeSpecific || field.nullable());
                    } else if (field.type().isRecord() && (!field.type().isSingleValueType() || isWrappedRecordType(field.type()))) {
                        return columnsOf(field.type().asClassManifest(), field.name(),
                                isSubtypeSpecific || field.nullable()).stream();
                    }
                    return Stream.of(Column.fromField(null, field, isSubtypeSpecific || field.nullable()));
                })
                .toList();

        // The id column must come first, then discriminator, then the rest (excluding id)
        var idColumnName = idColumnNameOf(manifest.subtypes().getFirst());
        var idColumns = allColumns.stream().filter(c -> c.name().equals(idColumnName)).toList();
        var nonIdColumns = allColumns.stream().filter(c -> !c.name().equals(idColumnName)).toList();

        var columns = new ArrayList<>(idColumns);
        columns.add(discriminatorColumn);
        columns.addAll(nonIdColumns);

        var fkIndexes = fkIndexesOf(tableName, columns);
        return new Table(tableName, columns, List.of(idColumnName), fkIndexes);
    }

    private static List<Index> fkIndexesOf(String tableName, List<Column> columns) {
        return columns.stream()
                .filter(c -> c.foreignKey() != null)
                .map(c -> Index.of(tableName, c.name(), false))
                .toList();
    }

    private List<Index> fieldIndexesOf(String tableName, ClassManifest manifest, String prefix) {
        if (isSingleValueWrapperManifest(manifest)) {
            var innerField = manifest.fields().getFirst();
            return fieldIndexesOf(tableName, innerField.type().asClassManifest(), prefix);
        }
        return manifest.fields().stream()
                .filter(field -> !field.type().is(List.class) || isDbDocumentField(field))
                .flatMap(field -> {
                    if (isDbDocumentField(field)) {
                        var columnName = columnNameOf(prefix, field.name());
                        return jsonbIndexesFor(tableName, columnName, field).stream();
                    } else if (field.type().isCustomType()) {
                        // Only generate an index if a plugin provides a DataType (i.e. the column exists)
                        if (pluginDataType(field.type()).isPresent()) {
                            var columnName = columnNameOf(prefix, field.name());
                            return indexFor(tableName, columnName, field).stream();
                        }
                        return Stream.empty();
                    } else if (field.type().isRecord() && (!field.type().isSingleValueType() || isWrappedRecordType(field.type()))) {
                        var newPrefix = prefix != null ? prefix + "_" + field.name() : field.name();
                        return fieldIndexesOf(tableName, field.type().asClassManifest(), newPrefix).stream();
                    }
                    var columnName = columnNameOf(prefix, field.name());
                    return indexFor(tableName, columnName, field).stream();
                })
                .toList();
    }

    private List<Index> jsonbIndexesFor(String tableName, String jsonbColumn, VariableManifest field) {
        var indexes = new ArrayList<Index>();
        if (field.hasAnnotation(Indexed.class)) {
            indexes.add(Index.gin(tableName, jsonbColumn));
        }
        var fieldType = resolveDbDocumentFieldType(field);
        if (fieldType != null && fieldType.isRecord()) {
            fieldType.asClassManifest().fields().stream()
                    .filter(innerField -> innerField.hasAnnotation(Indexed.class))
                    .forEach(innerField -> {
                        var isUnique = innerField.getAnnotation(Indexed.class)
                                .map(a -> a.value().unique()).orElse(false);
                        var innerFieldName = toSnakeCase(innerField.name());
                        indexes.add(Index.jsonbPath(tableName, jsonbColumn, innerFieldName, isUnique));
                    });
        }
        return List.copyOf(indexes);
    }

    private TypeManifest resolveDbDocumentFieldType(VariableManifest field) {
        if (field.type().is(List.class) && !field.type().parameters().isEmpty()) {
            return field.type().parameters().getFirst();
        }
        return field.type();
    }

    private boolean isDbDocumentField(VariableManifest field) {
        return field.hasAnnotation(DbDocument.class) || isDbDocumentType(field.type())
                || (field.type().is(List.class) && !field.type().parameters().isEmpty()
                && isDbDocumentType(field.type().parameters().getFirst()));
    }

    private static boolean isDbDocumentType(TypeManifest type) {
        return !type.annotationsOfType(DbDocument.class).isEmpty();
    }

    private static String columnNameOf(String prefix, String fieldName) {
        var rawName = prefix != null ? prefix + "_" + fieldName : fieldName;
        return columnName(rawName);
    }

    private static List<Index> indexFor(String tableName, String columnName, VariableManifest field) {
        if (field.hasAnnotation(Indexed.class)) {
            var isUnique = field.getAnnotation(Indexed.class)
                    .map(a -> a.value().unique())
                    .orElse(false);
            return List.of(Index.of(tableName, columnName, isUnique));
        } else if (field.hasAnnotation(Filter.class) || field.hasAnnotation(Filters.class)
                || field.hasAnnotation(TenantId.class)) {
            return List.of(Index.of(tableName, columnName, false));
        }
        return List.of();
    }

    private List<Table> childEntityTables(String parentTableName, List<String> parentPrimaryKeyColumns,
            ClassManifest manifest) {
        return childEntityTablesOf(parentTableName, parentPrimaryKeyColumns, manifest);
    }

    private List<Table> childEntityTablesOf(String parentTableName, List<String> parentPrimaryKeyColumns,
            ClassManifest manifest) {
        return manifest.fields().stream().filter(field -> field.type().is(List.class))
                .filter(field -> !isDbDocumentField(field))
                .map(field -> field.type().parameters().getFirst())
                .flatMap(child -> {
                    if (child.isCustomType()) {
                        // @CustomType elements in List are skipped — no child table generated
                        return Stream.empty();
                    } else if (child.isRecord() && (!child.isSingleValueType() || isWrappedRecordType(child))) {
                        if (isDbDocumentType(child)) {
                            return Stream.empty();
                        }
                        var childTable = buildChildTable(parentTableName, parentPrimaryKeyColumns, child);
                        var nestedTables = childEntityTablesOf(
                                childTable.name(),
                                childTable.primaryKey(),
                                child.asClassManifest()
                        );
                        return Stream.concat(Stream.of(childTable), nestedTables.stream());
                    } else {
                        return Stream.empty();
                    }
                })
                .toList();
    }

    private Table buildChildTable(String parentTableName, List<String> parentPrimaryKeyColumns, TypeManifest child) {
        var backReferenceColumnNames = backReferenceColumnNames(parentTableName, parentPrimaryKeyColumns);
        var columns = ListUtil.concat(
                parentBackReferenceColumns(backReferenceColumnNames, parentTableName, parentPrimaryKeyColumns),
                columnsOf(child.asClassManifest(), null, false));
        var childTableName = tableNameOf(child);
        var fkIndexes = fkIndexesOf(childTableName, columns);
        var fieldIndexes = fieldIndexesOf(childTableName, child.asClassManifest(), null);
        var tableForeignKeys = parentPrimaryKeyColumns.size() > 1
                ? List.of(new ForeignKeyConstraint(
                        foreignKeyConstraintName(
                                childTableName,
                                String.join("_", backReferenceColumnNames)
                        ),
                        backReferenceColumnNames,
                        new ForeignKeyReference(parentTableName, parentPrimaryKeyColumns)
                ))
                : List.<ForeignKeyConstraint>of();

        var parentOrderColumnName = shorten(parentTableName + "_key", POSTGRES_MAX_IDENTIFIER_LENGTH);
        columns = ListUtil.concat(columns, List.of(new Column(parentOrderColumnName, DataType.Primitive.INTEGER,
                false, null, null, null)));

        var primaryKey = ListUtil.concat(backReferenceColumnNames, List.of(parentOrderColumnName));
        return new Table(childTableName, columns, primaryKey, null, ListUtil.concat(fkIndexes, fieldIndexes),
                tableForeignKeys);
    }

    private static List<String> backReferenceColumnNames(String parentTableName, List<String> parentPrimaryKeyColumns) {
        if (parentPrimaryKeyColumns.size() == 1) {
            return List.of(shorten(parentTableName, POSTGRES_MAX_IDENTIFIER_LENGTH));
        }
        return parentPrimaryKeyColumns;
    }

    private static List<Column> parentBackReferenceColumns(
            List<String> localColumns,
            String parentTableName,
            List<String> parentPrimaryKeyColumns
    ) {
        var columns = new ArrayList<Column>();
        for (int i = 0; i < localColumns.size(); i++) {
            var dataType = i == 0 ? new DataType.Varchar(255) : DataType.Primitive.INTEGER;
            var foreignKey = parentPrimaryKeyColumns.size() == 1 && i == 0
                    ? new ForeignKeyReference(parentTableName, parentPrimaryKeyColumns)
                    : null;
            columns.add(new Column(localColumns.get(i), dataType, false, foreignKey, null, null));
        }
        return List.copyOf(columns);
    }

    private static String tableNameOf(TypeManifest manifest) {
        return toSnakeCase(manifest.simpleName()).replace('.', '_');
    }

    private static String idColumnNameOf(ClassManifest manifest) {
        return manifest.idField()
                .map(field -> toSnakeCase(field.name()))
                .orElse("id");
    }

    private List<Column> columnsOf(ClassManifest manifest, String prefix, boolean parentNullable) {
        if (isSingleValueWrapperManifest(manifest)) {
            var innerField = manifest.fields().getFirst();
            return columnsOf(innerField.type().asClassManifest(), prefix, parentNullable || innerField.nullable());
        }
        return manifest.fields().stream()
                .filter(field -> !field.type().is(List.class)
                        || field.type().parameters().getFirst().isStandardType()
                        || (field.type().parameters().getFirst().isSingleValueType()
                                && !isWrappedRecordType(field.type().parameters().getFirst()))
                        || isDbDocumentField(field))
                .flatMap(field -> {
                    if (isDbDocumentField(field)) {
                        return Stream.of(Column.fromField(prefix, field, parentNullable, DataType.Primitive.JSONB));
                    } else if (field.type().isCustomType()) {
                        return customTypeColumn(prefix, field, parentNullable);
                    } else if (field.type().isRecord() && (!field.type().isSingleValueType() || isWrappedRecordType(field.type()))) {
                        return columnsOf(field.type().asClassManifest(),
                                prefix != null ? prefix + "_" + field.name() : field.name(),
                                parentNullable || field.nullable()).stream();
                    } else {
                        return Stream.of(Column.fromField(prefix, field, parentNullable));
                    }
                })
                .toList();
    }

    private static boolean isSingleValueWrapperManifest(ClassManifest manifest) {
        if (manifest.fields().size() != 1) {
            return false;
        }
        var innerType = manifest.fields().getFirst().type();
        return (innerType.isRecord() && !innerType.isSingleValueType()) || isWrappedRecordType(innerType);
    }

    private static boolean isWrappedRecordType(TypeManifest type) {
        return wrappedRecordType(type).isPresent();
    }

    private static Optional<TypeManifest> wrappedRecordType(TypeManifest type) {
        if (!type.isSingleValueType()) {
            return Optional.empty();
        }

        var currentType = type;
        while (currentType.isSingleValueType()) {
            var innerType = currentType.fields().getFirst().type().asBoxed();
            if (innerType.isRecord() && !innerType.isSingleValueType()) {
                return Optional.of(innerType);
            }
            currentType = innerType;
        }

        return Optional.empty();
    }

    private void warnIfUnconstrainedString(VariableManifest field) {
        if (!field.type().asBoxed().is(String.class)) {
            return;
        }
        if (field.hasAnnotation(Id.class)) {
            return;
        }
        var annotations = field.annotations();
        if (!DataType.isTextAnnotated(annotations) && !DataType.hasSizeConstraint(annotations)) {
            context.processingEnvironment().getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    ("String field '%s' is mapped to VARCHAR(255) by default. " +
                    "Add @Size(max = N) to set a specific length, or @Text to use an unbounded TEXT column.")
                            .formatted(field.name()),
                    field.element()
            );
        }
    }

    /**
     * Resolves the DataType for a field whose type is annotated with {@code @CustomType} by asking each registered
     * plugin. Returns the first non-empty result, or empty if no plugin handles the type.
     */
    private Optional<DataType> pluginDataType(TypeManifest type) {
        return context.plugins().stream()
                .map(plugin -> plugin.dataTypeOf(type))
                .filter(Optional::isPresent)
                .findFirst()
                .flatMap(opt -> opt);
    }

    /**
     * Produces the column(s) for a {@code @CustomType} field. If a plugin supplies a {@link DataType} the field
     * becomes a regular column; otherwise the field is skipped and a diagnostic NOTE is emitted.
     */
    private Stream<Column> customTypeColumn(String prefix, VariableManifest field, boolean parentNullable) {
        return pluginDataType(field.type())
                .map(dataType -> Stream.of(Column.fromField(prefix, field, parentNullable, dataType)))
                .orElseGet(() -> {
                    context.processingEnvironment().getMessager().printMessage(
                            Diagnostic.Kind.NOTE,
                            ("Field '%s' of @CustomType '%s' has no database column: no PrefabPlugin provides a " +
                            "DataType mapping. Annotate the field with " +
                            "@org.springframework.data.annotation.Transient to suppress this message, or " +
                            "implement PrefabPlugin.dataTypeOf() to generate a column.")
                                    .formatted(field.name(), field.type()),
                            field.element()
                    );
                    return Stream.empty();
                });
    }

    private List<Table> sortByDependencies(List<Table> tables) {
        var tableMap = tables.stream().collect(Collectors.toMap(Table::name, table -> table));
        var dependencyTree = new HashMap<Table, Set<Table>>();
        for (Table table : tables) {
            var columnForeignKeyDependencies = table.columns().stream()
                    .map(Column::foreignKey)
                    .filter(fk -> fk != null && tableMap.containsKey(fk.referencedTable()))
                    .map(fk -> tableMap.get(fk.referencedTable()))
                    .collect(Collectors.toSet());
            var tableForeignKeyDependencies = table.foreignKeys().stream()
                    .map(ForeignKeyConstraint::reference)
                    .map(ForeignKeyReference::referencedTable)
                    .filter(tableMap::containsKey)
                    .map(tableMap::get)
                    .collect(Collectors.toSet());
            var dependencies = new HashSet<Table>();
            dependencies.addAll(columnForeignKeyDependencies);
            dependencies.addAll(tableForeignKeyDependencies);
            dependencyTree.put(table, dependencies);
        }
        var result = new ArrayList<Table>();
        while (!dependencyTree.isEmpty()) {
            var added = false;
            var entries = dependencyTree.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().name()))
                    .toList();
            for (var entry : entries) {
                if (entry.getValue().isEmpty() || result.containsAll(entry.getValue())) {
                    result.add(entry.getKey());
                    dependencyTree.remove(entry.getKey());
                    added = true;
                }
            }
            if (!added) {
                throw new IllegalArgumentException("Circular dependency detected between: " + dependencyTree.keySet());
            }
        }
        return result;
    }

    private FileObject createResource(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> classManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests,
            int version
    ) throws IOException {
        var elements = Stream.concat(
                classManifests.stream().map(manifest -> manifest.type().asElement()),
                polymorphicManifests.stream().map(manifest -> manifest.type().asElement())
        ).toArray(Element[]::new);
        return processingEnvironment.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "db/migration/V%d__generated.sql".formatted(version),
                elements
        );
    }

    record DatabaseState(List<Table> tables, int version) {
    }
}
