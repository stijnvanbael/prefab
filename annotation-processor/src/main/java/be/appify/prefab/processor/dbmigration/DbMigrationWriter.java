package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ListUtil;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.data.relational.core.mapping.Embedded;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
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

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;
import static java.util.stream.Collectors.groupingBy;

class DbMigrationWriter {

    void writeDbMigration(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> classManifests
    ) {
        var currentDatabaseState = currentDatabaseState(processingEnvironment);
        var desiredDatabaseState = desiredDatabaseState(classManifests);
        var changes = detectChanges(currentDatabaseState.tables(), desiredDatabaseState);
        if (!changes.isEmpty()) {
            writeChanges(processingEnvironment, classManifests, changes, currentDatabaseState.version());
        }
    }

    private void writeChanges(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> sortedManifests,
            List<DatabaseChange> changes,
            int version
    ) {
        try {
            var resource = createResource(processingEnvironment, sortedManifests, version + 1);
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
            } else if (!existing.equals(desired)) {
                changes.add(DatabaseChange.AlterTable.from(existing, desired));
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
            var tables = migrations.stream()
                    .flatMap(file -> parseSql(file, tablesByName))
                    .collect(groupingBy(Table::name))
                    .values()
                    .stream()
                    .map(List::getLast)
                    .toList();
            return new DatabaseState(tables, migrations.size());
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

    private static Stream<Table> parseSql(FileObject file, Map<String, Table> tables) {
        try (var input = file.openInputStream()) {
            var content = new String(input.readAllBytes());
            return new CCJSqlParser(content).Statements().stream()
                    .map(statement -> {
                        if (statement instanceof CreateTable createTable) {
                            var table = Table.fromCreateTable(createTable);
                            tables.put(table.name(), table);
                            return table;
                        } else if (statement instanceof Alter alter) {
                            var table = tables.get(alter.getTable().getName());
                            if (table == null) {
                                throw new IllegalStateException(
                                        "Found ALTER TABLE on table not previously created: " + alter.getTable()
                                                .getName());
                            }
                            return table.apply(alter);
                        }
                        return null;
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Table> desiredDatabaseState(List<ClassManifest> classManifests) {
        return sortByDependencies(classManifests.stream().flatMap(manifest -> {
            var tables = new ArrayList<Table>();
            var aggregateRootTable = toSnakeCase(manifest.simpleName());
            var columns = columnsOf(manifest, null, false);
            tables.add(new Table(aggregateRootTable, columns, List.of("id")));
            tables.addAll(childEntityTables(aggregateRootTable, manifest));
            return tables.stream();
        }).toList());
    }

    private List<Table> childEntityTables(String aggregateRootTable, ClassManifest manifest) {
        return manifest.fields().stream().filter(field -> field.type().is(List.class))
                .map(field -> field.type().parameters().getFirst())
                .flatMap(child -> {
                    if (child.isRecord()) {
                        var columns = ListUtil.concat(List.of(
                                new Column(aggregateRootTable, new DataType.Varchar(255), false,
                                        new ForeignKey(aggregateRootTable, "id"), null),
                                new Column(aggregateRootTable + "_key", DataType.Primitive.INTEGER, false, null, null)
                        ), columnsOf(child.asClassManifest(), null, false));
                        var table = new Table(toSnakeCase(child.simpleName()), columns, List.of(
                                aggregateRootTable,
                                aggregateRootTable + "_key"
                        ));
                        return Stream.of(table);
                    } else {
                        return Stream.empty();
                    }
                })
                .toList();
    }

    private List<Column> columnsOf(ClassManifest manifest, String prefix, boolean parentNullable) {
        return manifest.fields().stream()
                .filter(field -> !field.type().is(List.class)
                        || field.type().parameters().getFirst().isStandardType()
                        || field.type().parameters().getFirst().is(Reference.class))
                .peek(field -> {
                    if (field.type().isRecord() && !field.hasAnnotation(Embedded.Nullable.class)) {
                        throw new IllegalArgumentException(
                                "Value type field '%s' in '%s' must be annotated with @Embedded.Nullable"
                                        .formatted(field.name(), manifest.simpleName()));
                    }
                })
                .flatMap(field -> field.type().isRecord()
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
            int version
    ) throws IOException {
        return processingEnvironment.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "db/migration/V%d__generated.sql".formatted(version),
                classManifests.stream().map(manifest -> manifest.type().asElement()).toArray(Element[]::new)
        );
    }

    record DatabaseState(List<Table> tables, int version) {
    }
}
