package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

/**
 * Generates JDBC reading converters for polymorphic aggregate roots.
 *
 * <p>For each polymorphic aggregate (sealed interface with {@code @Aggregate}), this writer generates a
 * {@code @ReadingConverter} that reads a row from the database (represented as a {@code Map<String, Object>}) and
 * creates the appropriate subtype based on the {@code type} discriminator column.</p>
 */
class PolymorphicJdbcConverterWriter {

    private final JavaFileWriter fileWriter;

    PolymorphicJdbcConverterWriter(PrefabContext context) {
        this.fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.persistence");
    }

    /**
     * Generates JDBC reading converters for the given polymorphic aggregate.
     *
     * @param manifest
     *         the polymorphic aggregate manifest
     */
    void writeConverters(PolymorphicAggregateManifest manifest) {
        writeReadingConverter(manifest);
    }

    private void writeReadingConverter(PolymorphicAggregateManifest manifest) {
        var converterName = "%sReadingConverter".formatted(manifest.simpleName());
        var mapType = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                ClassName.get(Object.class));
        var type = TypeSpec.classBuilder(converterName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addAnnotation(ReadingConverter.class)
                .addSuperinterface(
                        ParameterizedTypeName.get(ClassName.get(Converter.class), mapType,
                                manifest.type().asTypeName()))
                .addSuperinterface(ClassName.get("be.appify.prefab.core.spring.data.jdbc", "PolymorphicReadingConverter"))
                .addMethod(buildConvertMethod(manifest, mapType));
        fileWriter.writeFile(manifest.packageName(), converterName, type.build());
    }

    private MethodSpec buildConvertMethod(PolymorphicAggregateManifest manifest, ParameterizedTypeName mapType) {
        var switchCases = manifest.subtypes().stream()
                .map(subtype -> {
                    var subtypeName = lastSimpleName(subtype.simpleName());
                    return CodeBlock.of("case $S -> $L;", subtypeName, buildSubtypeConstruction(subtype));
                })
                .collect(CodeBlock.joining("\n    "));

        var defaultCase = CodeBlock.of("default -> throw new $T($S + type);",
                IllegalArgumentException.class,
                "Unknown " + manifest.simpleName() + " type: ");

        var switchBlock = CodeBlock.of("switch (type) {\n    $L\n    $L\n}", switchCases, defaultCase);

        return MethodSpec.methodBuilder("convert")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(manifest.type().asTypeName())
                .addParameter(mapType, "row")
                .addStatement("var type = ($T) row.get($S)", String.class, "type")
                .addStatement("return $L", switchBlock)
                .build();
    }

    private CodeBlock buildSubtypeConstruction(ClassManifest subtype) {
        var args = subtype.fields().stream()
                .map(this::readFieldFromRow)
                .toList();
        return CodeBlock.of("new $T($L)",
                subtype.type().asTypeName(),
                args.stream().collect(CodeBlock.joining(", ")));
    }

    private CodeBlock readFieldFromRow(VariableManifest field) {
        var columnName = toSnakeCase(field.name());
        var fieldType = field.type();
        var boxedType = fieldType.asBoxed();

        if (fieldType.isSingleValueType()) {
            // Reference<T> and other single-value records
            var innerType = fieldType.fields().getFirst().type();
            if (fieldType.is(Reference.class)) {
                return CodeBlock.of("$T.fromId(($T) row.get($S))", Reference.class, innerType.asTypeName(), columnName);
            }
            return CodeBlock.of("row.get($S) != null ? new $T(($T) row.get($S)) : null",
                    columnName, fieldType.asTypeName(), innerType.asTypeName());
        } else if (boxedType.is(String.class)) {
            return CodeBlock.of("($T) row.get($S)", String.class, columnName);
        } else if (boxedType.is(Long.class)) {
            if (field.isPrimitive()) {
                return CodeBlock.of("row.get($S) instanceof $T n ? n.longValue() : 0L", columnName, Number.class);
            }
            return CodeBlock.of("row.get($S) instanceof $T n ? n.longValue() : null", columnName, Number.class);
        } else if (boxedType.is(Integer.class)) {
            if (field.isPrimitive()) {
                return CodeBlock.of("row.get($S) instanceof $T n ? n.intValue() : 0", columnName, Number.class);
            }
            return CodeBlock.of("row.get($S) instanceof $T n ? n.intValue() : null", columnName, Number.class);
        } else if (boxedType.is(Double.class)) {
            if (field.isPrimitive()) {
                return CodeBlock.of("row.get($S) instanceof $T n ? n.doubleValue() : 0.0", columnName, Number.class);
            }
            return CodeBlock.of("row.get($S) instanceof $T n ? n.doubleValue() : null", columnName, Number.class);
        } else if (boxedType.is(Float.class)) {
            if (field.isPrimitive()) {
                return CodeBlock.of("row.get($S) instanceof $T n ? n.floatValue() : 0.0f", columnName, Number.class);
            }
            return CodeBlock.of("row.get($S) instanceof $T n ? n.floatValue() : null", columnName, Number.class);
        } else if (boxedType.is(Boolean.class)) {
            if (field.isPrimitive()) {
                return CodeBlock.of("row.get($S) instanceof $T b ? b : false", columnName, Boolean.class);
            }
            return CodeBlock.of("($T) row.get($S)", Boolean.class, columnName);
        } else if (boxedType.is(BigDecimal.class)) {
            return CodeBlock.of("($T) row.get($S)", BigDecimal.class, columnName);
        } else if (boxedType.is(Instant.class)) {
            return CodeBlock.of("row.get($S) instanceof $T ts ? ts.toInstant() : null",
                    columnName, java.sql.Timestamp.class);
        } else if (boxedType.is(LocalDate.class)) {
            return CodeBlock.of("row.get($S) instanceof $T d ? d.toLocalDate() : null",
                    columnName, java.sql.Date.class);
        } else if (boxedType.is(OffsetDateTime.class)) {
            return CodeBlock.of("row.get($S) instanceof $T ts ? ts.toInstant().atOffset($T.UTC) : null",
                    columnName, java.sql.Timestamp.class, java.time.ZoneOffset.class);
        } else if (boxedType.is(List.class)) {
            var elementType = fieldType.parameters().isEmpty() ? null : fieldType.parameters().getFirst();
            if (elementType != null && elementType.is(String.class)) {
                return CodeBlock.of("row.get($S) instanceof $T arr ? $T.of(arr) : $T.of()",
                        columnName, String[].class, List.class, List.class);
            }
            return CodeBlock.of("$T.of()", List.class);
        } else if (boxedType.isEnum()) {
            return CodeBlock.of("row.get($S) != null ? $T.valueOf(($T) row.get($S)) : null",
                    columnName, fieldType.asTypeName(), String.class);
        } else {
            // Fallback: cast to the boxed type
            return CodeBlock.of("($T) row.get($S)", boxedType.asTypeName(), columnName);
        }
    }

    private static String lastSimpleName(String simpleName) {
        int dot = simpleName.lastIndexOf('.');
        return dot < 0 ? simpleName : simpleName.substring(dot + 1);
    }
}
