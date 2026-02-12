package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.util.Streams;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.WildcardTypeName;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.avro.AvroPlugin.isLogicalType;
import static be.appify.prefab.processor.event.avro.AvroPlugin.isNestedRecord;
import static be.appify.prefab.processor.event.avro.AvroPlugin.nestedTypes;
import static be.appify.prefab.processor.event.avro.AvroPlugin.sealedSubtypes;

class GenericRecordToEventConverterWriter {
    private final PrefabContext context;

    GenericRecordToEventConverterWriter(PrefabContext context) {
        this.context = context;
    }

    void writeConverter(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.avro");

        var name = "GenericRecordTo%sConverter".formatted(event.simpleName().replace(".", ""));
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addSuperinterface(
                        ParameterizedTypeName.get(ClassName.get(Converter.class), ClassName.get(GenericRecord.class), event.asTypeName()));
        type.addMethod(constructor(event, type))
                .addMethod(convertMethod(event));

        fileWriter.writeFile(event.packageName(), name, type.build());
    }

    private static MethodSpec constructor(TypeManifest event, TypeSpec.Builder type) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        nestedTypes(List.of(event)).forEach(nestedType -> addConverter(type, nestedType, constructor));
        sealedSubtypes(List.of(event)).forEach(subtype -> addConverter(type, subtype, constructor));
        return constructor.build();
    }

    private static void addConverter(TypeSpec.Builder type, TypeManifest subtype, MethodSpec.Builder constructor) {
        var converterName = "genericRecordTo%sConverter".formatted(subtype.simpleName().replace(".", ""));
        var converterType = ClassName.get(subtype.packageName() + ".infrastructure.avro",
                "GenericRecordTo%sConverter".formatted(subtype.simpleName().replace(".", "")));
        type.addField(converterType, converterName, Modifier.PRIVATE, Modifier.FINAL);
        constructor.addParameter(converterType, converterName)
                .addStatement("this.$L = $L", converterName, converterName);
    }

    private MethodSpec convertMethod(TypeManifest event) {
        var method = MethodSpec.methodBuilder("convert")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(GenericRecord.class, "genericRecord")
                .returns(event.asTypeName());
        if (event.isSealed()) {
            method.addStatement(CodeBlock.of("return $L", sealedType(CodeBlock.of("genericRecord"), event)));
        } else {
            method.addStatement(convertRecord(event));
        }
        return method.build();
    }

    private CodeBlock sealedType(CodeBlock value, TypeManifest type) {
        return CodeBlock.of("""
                switch($L.getSchema().getName()) {
                    $L
                    default -> throw new IllegalArgumentException("Unknown subtype: " + $L.getSchema().getName());
                }""",
                value,
                type.permittedSubtypes().stream()
                .map(subtype -> {
                   var converterName = "genericRecordTo%sConverter".formatted(subtype.simpleName().replace(".", ""));
                   return CodeBlock.of("case $S -> $L.convert($L);", subtype.simpleName(), converterName, value);
                }).collect(CodeBlock.joining("\n    ")),
                value
        );
    }

    private CodeBlock convertRecord(TypeManifest event) {
        return CodeBlock.of("""
                        return new $T(
                            $L
                        )""",
                event.asTypeName(),
                event.fields().stream()
                        .map(field -> field(CodeBlock.of("genericRecord.get($S)", field.name()), field.type()))
                        .collect(CodeBlock.joining(",\n    ")));
    }

    private CodeBlock field(CodeBlock value, TypeManifest type) {
        if (isLogicalType(type)) {
            return maybeNull(value, logicalType(value, type));
        } else if (type.isEnum()) {
            return maybeNull(value, CodeBlock.of("$T.valueOf($L.toString())", type.asTypeName(), value));
        } else if(type.isSealed()) {
            return maybeNull(value, sealedType(value, type));
        } else if (isNestedRecord(type)) {
            var converterName = "genericRecordTo%sConverter".formatted(type.simpleName().replace(".", ""));
            return maybeNull(value, CodeBlock.of("$L.convert((GenericRecord) $L)", converterName, value));
        } else if (type.is(List.class)) {
            return maybeNull(value, listType(value, type));
        } else if (type.is(String.class)) {
            return CodeBlock.of("$L.toString()", value);
        } else if (type.isStandardType()) {
            return CodeBlock.of("($T) $L", type.asBoxed().asTypeName(), value);
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + type);
        }
    }

    private CodeBlock maybeNull(CodeBlock value, CodeBlock nonNullValue) {
        return CodeBlock.of("$L != null ? $L : null", value, nonNullValue);
    }

    private CodeBlock logicalType(CodeBlock value, TypeManifest type) {
        if (type.is(Instant.class)) {
            return CodeBlock.of("$T.ofEpochMilli((Long) $L)", Instant.class, value);
        } else if (type.is(LocalDate.class)) {
            return CodeBlock.of("$T.ofEpochDay((Integer) $L)", LocalDate.class, value);
        } else if (type.is(Duration.class)) {
            return CodeBlock.of("$T.ofMillis((Long) $L)", Duration.class, value);
        } else if (type.is(Reference.class)) {
            return CodeBlock.of("$T.fromId($L.toString())", Reference.class, value);
        } else {
            throw new IllegalArgumentException("Unsupported logical type: " + type.asTypeName());
        }
    }

    private CodeBlock listType(CodeBlock value, TypeManifest type) {
        var elementType = type.parameters().getFirst();
        return CodeBlock.of("""
                        $T.stream((($T) $L).iterator())
                                .map(item -> $L)
                                .toList()""",
                Streams.class,
                ParameterizedTypeName.get(ClassName.get(GenericData.Array.class), WildcardTypeName.subtypeOf(Object.class)),
                value,
                field(CodeBlock.of("item"), elementType));
    }
}
