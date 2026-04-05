package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ListUtil;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.drop.Drop;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

class DbMigrationWriter {

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
        var currentDatabaseState = currentDatabaseState(processingEnvironment);
        var desiredDatabaseState = desiredDatabaseState(classManifests, polymorphicManifests);
        var changes = detectChanges(currentDatabaseState.tables(), desiredDatabaseState);
        if (!changes.isEmpty()) {
            writeChanges(processingEnvironment, classManifests, polymorphicManifests, changes,
                    currentDatabaseState.version());
        }
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

    private List<DatabaseChange> detectChanges(List<Table> currentDatabaseState, List<Table> desiredDatabaseState) {
        var changes = new ArrayList<DatabaseChange>();
        for (Table desired : desiredDatabaseState) {
            var existing = findTable(currentDatabaseState, desired.name());
            if (existing == null) {
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
            if (desired == null) {
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
            var migrations = existingMigrations(processingEnvironment);
            migrations.forEach(file -> parseSql(file, tablesByName));
            return new DatabaseState(List.copyOf(tablesByName.values()), migrations.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<FileObject> existingMigrations(ProcessingEnvironment processingEnvironment) throws IOException {
        var files = new ArrayList<FileObject>();
        for (int i = 1; ; i++) {
            try {
                var file = processingEnvironment.getFiler().getResource(StandardLocation.CLASS_PATH, "db.migration",
                        "V%d__generated.sql".formatted(i));
                files.add(file);
            } catch (FileNotFoundException e) {
                break;
            }
        }
        return files;
    }

    private static void parseSql(FileObject file, Map<String, Table> tables) {
        try (var input = file.openInputStream()) {
            var content = new String(input.readAllBytes());
            new CCJSqlParser(content).Statements().forEach(statement -> {
                if (statement instanceof CreateTable createTable) {
                    var table = Table.fromCreateTable(createTable);
                    tables.put(table.name(), table);
                } else if (statement instanceof Alter alter) {
                    var table = tables.get(alter.getTable().getName());
                    if (table == null) {
                        throw new IllegalStateException(
                                "Found ALTER TABLE on table not previously created: " + alter.getTable().getName());
                    }
                    tables.put(table.name(), table.apply(alter));
                } else if (statement instanceof CreateIndex createIndex) {
                    var tableName = createIndex.getTable().getName().replace("\"", "");
                    var table = tables.get(tableName);
                    if (table != null) {
                        tables.put(tableName, table.withAddedIndex(Index.fromCreateIndex(createIndex)));
                    }
                } else if (statement instanceof Drop drop && "INDEX".equalsIgnoreCase(drop.getType())) {
                    var indexName = drop.getName().getName().replace("\"", "");
                    tables.values().stream()
                            .filter(t -> t.getIndex(indexName).isPresent())
                            .findFirst()
                            .ifPresent(t -> tables.put(t.name(), t.withRemovedIndex(indexName)));
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Table> desiredDatabaseState(List<ClassManifest> classManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests) {
        var tables = new ArrayList<Table>();
        classManifests.forEach(manifest -> {
            var aggregateRootTable = tableNameOf(manifest.type());
            var columns = columnsOf(manifest, null, false);
            var fkIndexes = fkIndexesOf(aggregateRootTable, columns);
            var fieldIndexes = fieldIndexesOf(aggregateRootTable, manifest, null);
            tables.add(new Table(aggregateRootTable, columns, List.of("id"),
                    ListUtil.concat(fkIndexes, fieldIndexes)));
            tables.addAll(childEntityTables(aggregateRootTable, manifest));
        });
        polymorphicManifests.forEach(manifest -> tables.add(polymorphicTable(manifest)));
        return sortByDependencies(tables);
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
                        || field.type().parameters().getFirst().isSingleValueType())
                .flatMap(field -> {
                    boolean isSubtypeSpecific = !commonFieldNames.contains(field.name());
                    if (field.type().isRecord() && !field.type().isSingleValueType()) {
                        return columnsOf(field.type().asClassManifest(), field.name(),
                                isSubtypeSpecific || field.nullable()).stream();
                    }
                    return Stream.of(Column.fromField(null, field, isSubtypeSpecific || field.nullable()));
                })
                .toList();

        // The 'id' column must come first, then discriminator, then the rest (excluding id)
        var idColumns = allColumns.stream().filter(c -> c.name().equals("id")).toList();
        var nonIdColumns = allColumns.stream().filter(c -> !c.name().equals("id")).toList();

        var columns = new ArrayList<Column>();
        columns.addAll(idColumns);
        columns.add(discriminatorColumn);
        columns.addAll(nonIdColumns);

        var fkIndexes = fkIndexesOf(tableName, columns);
        return new Table(tableName, columns, List.of("id"), fkIndexes);
    }

    private static List<Index> fkIndexesOf(String tableName, List<Column> columns) {
        return columns.stream()
                .filter(c -> c.foreignKey() != null)
                .map(c -> Index.of(tableName, c.name(), false))
                .toList();
    }

    private List<Index> fieldIndexesOf(String tableName, ClassManifest manifest, String prefix) {
        return manifest.fields().stream()
                .filter(field -> !field.type().is(List.class))
                .flatMap(field -> {
                    if (field.type().isRecord() && !field.type().isSingleValueType()) {
                        var newPrefix = prefix != null ? prefix + "_" + field.name() : field.name();
                        return fieldIndexesOf(tableName, field.type().asClassManifest(), newPrefix).stream();
                    }
                    var columnName = toSnakeCase(prefix != null ? prefix + "_" + field.name() : field.name());
                    return indexFor(tableName, columnName, field).stream();
                })
                .toList();
    }

    private static List<Index> indexFor(String tableName, String columnName, VariableManifest field) {
        if (field.hasAnnotation(Indexed.class)) {
            var isUnique = field.getAnnotation(Indexed.class)
                    .map(a -> a.value().unique())
                    .orElse(false);
            return List.of(Index.of(tableName, columnName, isUnique));
        } else if (field.hasAnnotation(Filter.class) || field.hasAnnotation(Filters.class)) {
            return List.of(Index.of(tableName, columnName, false));
        }
        return List.of();
    }

    private List<Table> childEntityTables(String aggregateRootTable, ClassManifest manifest) {
        return manifest.fields().stream().filter(field -> field.type().is(List.class))
                .map(field -> field.type().parameters().getFirst())
                .flatMap(child -> {
                    if (child.isRecord() && !child.isSingleValueType()) {
                        var columns = ListUtil.concat(List.of(
                                new Column(aggregateRootTable, new DataType.Varchar(255), false,
                                        new ForeignKey(aggregateRootTable, "id"), null),
                                new Column(aggregateRootTable + "_key", DataType.Primitive.INTEGER, false, null, null)
                        ), columnsOf(child.asClassManifest(), null, false));
                        var childTableName = tableNameOf(child);
                        var fkIndexes = fkIndexesOf(childTableName, columns);
                        var fieldIndexes = fieldIndexesOf(childTableName, child.asClassManifest(), null);
                        var table = new Table(childTableName, columns, List.of(
                                aggregateRootTable,
                                aggregateRootTable + "_key"
                        ), ListUtil.concat(fkIndexes, fieldIndexes));
                        return Stream.of(table);
                    } else {
                        return Stream.empty();
                    }
                })
                .toList();
    }

    private static String tableNameOf(TypeManifest manifest) {
        return toSnakeCase(manifest.simpleName()).replace('.', '_');
    }

    private List<Column> columnsOf(ClassManifest manifest, String prefix, boolean parentNullable) {
        return manifest.fields().stream()
                .filter(field -> !field.type().is(List.class)
                        || field.type().parameters().getFirst().isStandardType()
                        || field.type().parameters().getFirst().isSingleValueType())
                .flatMap(field -> field.type().isRecord() && !field.type().isSingleValueType()
                        ? columnsOf(field.type().asClassManifest(),
                        prefix != null ? prefix + "_" + field.name() : field.name(),
                        parentNullable || field.nullable()).stream()
                        : Stream.of(Column.fromField(prefix, field, parentNullable)))
                .toList();
    }

    private List<Table> sortByDependencies(List<Table> tables) {
        var tableMap = tables.stream().collect(Collectors.toMap(Table::name, table -> table));
        var dependencyTree = new HashMap<Table, Set<Table>>();
        for (Table table : tables) {
            var dependencies = table.columns().stream()
                    .map(Column::foreignKey)
                    .filter(fk -> fk != null && tableMap.containsKey(fk.referencedTable()))
                    .map(fk -> tableMap.get(fk.referencedTable()))
                    .collect(Collectors.toSet());
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
