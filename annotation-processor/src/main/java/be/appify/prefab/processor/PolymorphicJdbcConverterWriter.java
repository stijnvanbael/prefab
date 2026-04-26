package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.util.IdentifierShortener;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.springframework.core.convert.ConversionService;
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
        var conversionServiceType = ClassName.get(ConversionService.class);
        var type = TypeSpec.classBuilder(converterName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addAnnotation(ReadingConverter.class)
                .addSuperinterface(
                        ParameterizedTypeName.get(ClassName.get(Converter.class), mapType,
                                manifest.type().asTypeName()))
                .addSuperinterface(ClassName.get("be.appify.prefab.core.spring.data.jdbc", "PolymorphicReadingConverter"))
                .addField(FieldSpec.builder(conversionServiceType, "conversionService", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(buildConstructor(conversionServiceType))
                .addMethod(buildConvertMethod(manifest, mapType));
        fileWriter.writeFile(manifest.packageName(), converterName, type.build());
    }

    private MethodSpec buildConstructor(ClassName conversionServiceType) {
        return MethodSpec.constructorBuilder()
                .addParameter(conversionServiceType, "conversionService")
                .addStatement("this.conversionService = conversionService")
                .build();
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
                .beginControlFlow("if (type == null)")
                .addStatement("throw new $T($S)", IllegalArgumentException.class,
                        "Missing type discriminator for " + manifest.simpleName())
                .endControlFlow()
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
            return CodeBlock.of("row.get($S) != null ? new $T(conversionService.convert(row.get($S), $T.class)) : null",
                    columnName, fieldType.asTypeName(), columnName, innerType.asBoxed().asTypeName());
        } else if (fieldType.isRecord()) {
            return buildEmbeddedRecordRead(fieldType, field.name(), field.nullable());
        } else if (fieldType.is(List.class)) {
            return CodeBlock.of("($T) row.get($S)", fieldType.asTypeName(), columnName);
        }
        return CodeBlock.of("conversionService.convert(row.get($S), $T.class)",
                columnName, fieldType.asBoxed().asTypeName());
    }

    private CodeBlock buildEmbeddedRecordRead(TypeManifest type, String rawPrefix, boolean nullable) {
        var subArgs = type.fields().stream()
                .map(subField -> buildSubFieldRead(subField, rawPrefix))
                .collect(CodeBlock.joining(", "));
        var construction = CodeBlock.of("new $T($L)", type.asTypeName(), subArgs);
        if (nullable) {
            var firstSubColumn = IdentifierShortener.columnName(rawPrefix + "_" + type.fields().getFirst().name());
            return CodeBlock.of("row.get($S) != null ? $L : null", firstSubColumn, construction);
        }
        return construction;
    }

    private CodeBlock buildSubFieldRead(VariableManifest subField, String parentRawPrefix) {
        var rawName = parentRawPrefix + "_" + subField.name();
        var subColumnName = IdentifierShortener.columnName(rawName);
        var subType = subField.type();

        if (subType.isSingleValueType()) {
            var innerType = subType.fields().getFirst().type();
            return CodeBlock.of("row.get($S) != null ? new $T(conversionService.convert(row.get($S), $T.class)) : null",
                    subColumnName, subType.asTypeName(), subColumnName, innerType.asBoxed().asTypeName());
        } else if (subType.isRecord()) {
            return buildEmbeddedRecordRead(subType, rawName, subField.nullable());
        } else if (subType.is(List.class)) {
            return CodeBlock.of("($T) row.get($S)", subType.asTypeName(), subColumnName);
        }
        return CodeBlock.of("conversionService.convert(row.get($S), $T.class)",
                subColumnName, subType.asBoxed().asTypeName());
    }

    private static String lastSimpleName(String simpleName) {
        int dot = simpleName.lastIndexOf('.');
        return dot < 0 ? simpleName : simpleName.substring(dot + 1);
    }
}
