package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
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

    private static final ClassName JDBC_CONVERTER =
            ClassName.get("org.springframework.data.jdbc.core.convert", "JdbcConverter");
    private static final ClassName TYPE_INFORMATION =
            ClassName.get("org.springframework.data.util", "TypeInformation");

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
                .addField(FieldSpec.builder(JDBC_CONVERTER, "converter", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(JDBC_CONVERTER, "converter")
                        .addStatement("this.converter = converter")
                        .build())
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

        if (fieldType.is(Reference.class)) {
            var innerType = fieldType.fields().getFirst().type();
            return CodeBlock.of("$T.fromId(($T) row.get($S))", Reference.class, innerType.asTypeName(), columnName);
        } else if (fieldType.isSingleValueType()) {
            var innerType = fieldType.fields().getFirst().type();
            return CodeBlock.of("row.get($S) != null ? new $T(($T) row.get($S)) : null",
                    columnName, fieldType.asTypeName(), innerType.asTypeName());
        }
        return CodeBlock.of("($T) converter.readValue(row.get($S), $T.of($T.class))",
                fieldType.asBoxed().asTypeName(), columnName, TYPE_INFORMATION,
                fieldType.asBoxed().asTypeName());
    }

    private static String lastSimpleName(String simpleName) {
        int dot = simpleName.lastIndexOf('.');
        return dot < 0 ? simpleName : simpleName.substring(dot + 1);
    }
}
