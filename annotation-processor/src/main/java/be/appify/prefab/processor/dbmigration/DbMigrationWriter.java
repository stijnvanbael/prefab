package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import static be.appify.prefab.processor.CaseUtil.toSnakeCase;
import static java.util.Collections.emptyList;

import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: prefix nested properties with the name of the parent property
public class DbMigrationWriter {

    public void writeDbMigration(int version, List<ClassManifest> classManifests) {
        try {
            var resource = createResource(classManifests, version);
            try (var writer = resource.openWriter()) {
                for (ClassManifest manifest : classManifests) {
                    writeAggregateRootTable(manifest, writer);
                    writeChildEntityTables(toSnakeCase(manifest.simpleName()), manifest, writer, emptyList());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAggregateRootTable(ClassManifest manifest, Writer writer) throws IOException {
        writer.write("CREATE TABLE %s (\n".formatted(toSnakeCase(manifest.simpleName())));
        writer.write("  id VARCHAR(255) PRIMARY KEY,\n");
        writer.write("  version INTEGER NOT NULL,\n");
        var fields = fieldsOf(manifest, null);
        for (Field field : fields) {
            var last = field == fields.getLast();
            writer.write("  %s %s%s%s\n".formatted(
                    toSnakeCase(field.name()),
                    sqlTypeOf(field.property().toBoxed().type(), field.property().annotations(), field.name()),
                    constraintOf(field.property()),
                    last ? "" : ","));
        }
        writer.write(");\n\n");
    }

    private void writeChildEntityTables(
            String aggregateRootTable,
            ClassManifest parent,
            Writer writer,
            List<String> parents
    ) {
        parent.fields().stream().filter(field -> field.type().is(List.class)).forEach(field -> {
            if (field.type().parameters().getFirst().isStandardType()) {
                return;
            }
            try {
                writeChildEntityTable(aggregateRootTable, parents,
                        field.type().parameters().getFirst().asClassManifest(), writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeChildEntityTable(
            String aggregateRootTable,
            List<String> parents,
            ClassManifest manifest,
            Writer writer
    ) throws IOException {
        var tableName = toSnakeCase(manifest.simpleName());
        writer.write("CREATE TABLE %s (\n".formatted(tableName));
        var keys = new ArrayList<>(List.of(aggregateRootTable, aggregateRootTable + "_key"));
        writer.write("  %s VARCHAR(255) NOT NULL,\n".formatted(aggregateRootTable));
        writer.write("  %s_key INTEGER NOT NULL,\n".formatted(aggregateRootTable));
        for (String parent : parents) {
            writer.write("  %s_key INTEGER NOT NULL,\n".formatted(parent));
            keys.add(parent + "_key");
        }
        var fields = fieldsOf(manifest, null);
        for (Field field : fields) {
            writer.write("  %s %s%s,\n".formatted(
                    toSnakeCase(field.name()),
                    sqlTypeOf(field),
                    constraintOf(field.property())));
        }
        writer.write("  PRIMARY KEY (%s)\n".formatted(String.join(", ", keys)));
        writer.write(");\n\n");
        var newParents = Stream.concat(parents.stream(), Stream.of(tableName)).toList();
        writeChildEntityTables(aggregateRootTable, manifest, writer, newParents);
    }

    private String constraintOf(VariableManifest field) {
        return (field.annotations().stream()
                .anyMatch(annotation -> annotation.type().is(NotNull.class))
                ? " NOT NULL" : "") + (
                       field.type().is(Reference.class)
                               ? " REFERENCES %s(id)".formatted(
                               toSnakeCase(field.type().parameters().getFirst().simpleName()))
                               : ""
               );
    }

    private List<Field> fieldsOf(ClassManifest manifest, String prefix) {
        return manifest.fields().stream()
                .filter(field -> !field.type().is(List.class) || field.type().parameters().getFirst().isStandardType())
                .flatMap(field -> field.type().isRecord()
                        ? fieldsOf(field.type().asClassManifest(),
                        prefix != null ? prefix + "_" + field.name() : field.name()).stream()
                        : Stream.of(new Field(prefix, field)))
                .toList();
    }

    private String sqlTypeOf(Field field) {
        return sqlTypeOf(
                field.property().toBoxed().type(),
                field.property().annotations(),
                field.name());
    }

    private static String sqlTypeOf(TypeManifest type, List<AnnotationManifest> annotations, String name) {
        if (type.is(String.class) || type.is(Reference.class) || type.isEnum() || type.is(Duration.class)) {
            var length = annotations.stream()
                    .filter(annotation -> annotation.type().is(Size.class))
                    .map(annotation -> (Integer) annotation.value("max"))
                    .findFirst().orElse(255);
            return "VARCHAR(%d)".formatted(length);
        } else if (type.is(Integer.class)) {
            return "INTEGER";
        } else if (type.is(Long.class)) {
            return "BIGINT";
        } else if (type.is(Boolean.class)) {
            return "BOOLEAN";
        } else if (type.is(BigDecimal.class) || type.is(Double.class) || type.is(Float.class)) {
            return "DECIMAL(19, 4)";
        } else if (type.is(Instant.class)) {
            return "TIMESTAMP";
        } else if (type.is(byte[].class) || type.is(File.class)) {
            return "BYTEA";
        } else if (type.is(List.class)) {
            return sqlTypeOf(type.parameters().getFirst(), annotations, name) + "[]";
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type [%s] for field %s".formatted(type, name));
        }
    }

    private FileObject createResource(List<ClassManifest> classManifests, int version) throws IOException {
        return classManifests.getFirst().processingEnvironment().getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "db/migration/V%d__%s.sql".formatted(version,
                        classManifests.stream().map(manifest -> toSnakeCase(manifest.simpleName())).collect(
                                Collectors.joining("_"))),
                classManifests.stream().map(manifest -> manifest.type().asElement()).toArray(Element[]::new)
        );
    }

    record Field(String prefix, VariableManifest property) {
        String name() {
            return prefix != null ? prefix + "_" + property.name() : property.name();
        }
    }
}
