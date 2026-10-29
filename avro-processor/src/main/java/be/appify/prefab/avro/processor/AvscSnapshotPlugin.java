package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.EventSnapshot;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import org.apache.avro.Schema;

/**
 * Generates a plain snapshot record of a historic Avro schema for every {@link EventSnapshot}. The record is the
 * {@code Old} type of a {@link be.appify.prefab.core.versioning.Migration}. Enums become strings and logical types are
 * represented by their underlying Avro type.
 */
public class AvscSnapshotPlugin implements PrefabPlugin {
    private PrefabContext context;
    private final Set<String> processed = new HashSet<>();

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeEventFiles() {
        ElementFilter.typesIn(context.roundEnvironment().getElementsAnnotatedWith(EventSnapshot.class)).stream()
                .filter(element -> processed.add(element.getQualifiedName().toString()))
                .forEach(this::generate);
    }

    private void generate(TypeElement element) {
        var annotation = element.getAnnotation(EventSnapshot.class);
        var schema = parse(annotation.avsc(), element);
        if (schema == null) {
            return;
        }
        if (schema.getType() != Schema.Type.RECORD) {
            error("The schema of @EventSnapshot '%s' must be a record.".formatted(annotation.avsc()), element);
            return;
        }
        var packageName = context.processingEnvironment().getElementUtils().getPackageOf(element)
                .getQualifiedName().toString();
        var records = new LinkedHashMap<String, TypeSpec>();
        var rootName = annotation.name().isBlank() ? schema.getName() : annotation.name();
        collect(schema, rootName, packageName, records);
        records.values().forEach(spec -> write(JavaFile.builder(packageName, spec).build(), element));
    }

    private void collect(Schema schema, String name, String packageName, Map<String, TypeSpec> records) {
        if (records.containsKey(name)) {
            return;
        }
        var constructor = MethodSpec.constructorBuilder();
        var builder = TypeSpec.recordBuilder(name).addModifiers(Modifier.PUBLIC);
        records.put(name, builder.build());
        for (var field : schema.getFields()) {
            constructor.addParameter(typeOf(field.schema(), packageName, records), field.name());
        }
        records.put(name, builder.recordConstructor(constructor.build()).build());
    }

    private TypeName typeOf(Schema schema, String packageName, Map<String, TypeSpec> records) {
        return switch (schema.getType()) {
            case STRING, ENUM -> ClassName.get(String.class);
            case INT -> TypeName.INT;
            case LONG -> TypeName.LONG;
            case FLOAT -> TypeName.FLOAT;
            case DOUBLE -> TypeName.DOUBLE;
            case BOOLEAN -> TypeName.BOOLEAN;
            case BYTES, FIXED -> ArrayTypeName.of(TypeName.BYTE);
            case ARRAY -> ParameterizedTypeName.get(ClassName.get(java.util.List.class),
                    typeOf(schema.getElementType(), packageName, records).box());
            case MAP -> ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                    typeOf(schema.getValueType(), packageName, records).box());
            case RECORD -> {
                collect(schema, schema.getName(), packageName, records);
                yield ClassName.get(packageName, schema.getName());
            }
            case UNION -> unionType(schema, packageName, records);
            default -> ClassName.get(Object.class);
        };
    }

    private TypeName unionType(Schema schema, String packageName, Map<String, TypeSpec> records) {
        var branches = schema.getTypes().stream().filter(type -> type.getType() != Schema.Type.NULL).toList();
        return branches.size() == 1
                ? typeOf(branches.getFirst(), packageName, records).box()
                : ClassName.get(Object.class);
    }

    private Schema parse(String path, TypeElement element) {
        try (var stream = open(path)) {
            if (stream == null) {
                error("AVSC file not found: '%s'. Ensure it is on the classpath.".formatted(path), element);
                return null;
            }
            return new Schema.Parser().parse(stream);
        } catch (IOException | RuntimeException e) {
            error("Failed to read or parse AVSC file '%s': %s".formatted(path, e.getMessage()), element);
            return null;
        }
    }

    private InputStream open(String path) throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream != null) {
            return stream;
        }
        var file = Path.of("src/main/resources", path);
        return Files.exists(file) ? Files.newInputStream(file) : null;
    }

    private void write(JavaFile file, TypeElement element) {
        try {
            file.writeTo(context.processingEnvironment().getFiler());
        } catch (IOException e) {
            error("Failed to write snapshot record: " + e.getMessage(), element);
        }
    }

    private void error(String message, TypeElement element) {
        context.processingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
