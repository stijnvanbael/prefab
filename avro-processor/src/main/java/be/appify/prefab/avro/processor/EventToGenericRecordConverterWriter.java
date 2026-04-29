package be.appify.prefab.avro.processor;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import static be.appify.prefab.avro.processor.AvroPlugin.isLogicalType;
import static be.appify.prefab.avro.processor.AvroPlugin.isNestedRecord;
import static be.appify.prefab.avro.processor.AvroPlugin.nestedTypes;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class EventToGenericRecordConverterWriter {
    private final PrefabContext context;

    EventToGenericRecordConverterWriter(PrefabContext context) {
        this.context = context;
    }

    void writeConverter(TypeManifest event) {
        // @Avsc contract interfaces must not be mapped field-by-field — generate a delegating
        // converter that dispatches on the runtime type of the event to the concrete converter.
        if (event.asElement() != null && event.asElement().getAnnotation(Avsc.class) != null) {
            writeAvscInterfaceConverter(event);
            return;
        }

        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.avro");

        var name = "%sToGenericRecordConverter".formatted(event.simpleName().replace(".", ""));
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addSuperinterface(
                        ParameterizedTypeName.get(ClassName.get(Converter.class), event.asTypeName(), ClassName.get(GenericRecord.class)))
                .addField(FieldSpec.builder(Schema.class, "schema", Modifier.PRIVATE, Modifier.FINAL).build());
        type.addMethod(constructor(event, type))
                .addMethod(convertMethod(event));

        fileWriter.writeFile(event.packageName(), name, type.build());
    }

    /**
     * Generates a converter for an {@code @Avsc}-annotated contract interface.
     * The converter switches on the runtime type of the event and delegates to the appropriate
     * concrete record converter instead of trying to map the interface's fields directly.
     */
    private void writeAvscInterfaceConverter(TypeManifest contractInterface) {
        var implementations = context.eventElements()
                .filter(e -> e.getKind() == ElementKind.RECORD)
                .filter(e -> e.getInterfaces().stream()
                        .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                        .anyMatch(iface -> iface.equals(contractInterface.asElement())))
                .map(e -> TypeManifest.of(e.asType(), context.processingEnvironment()))
                .toList();

        // Skip round 1: concrete records are compiled in round 2 and not yet available.
        if (implementations.isEmpty()) {
            return;
        }

        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.avro");
        var name = "%sToGenericRecordConverter".formatted(contractInterface.simpleName().replace(".", ""));

        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(Converter.class), contractInterface.asTypeName(), ClassName.get(GenericRecord.class)));

        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        implementations.forEach(impl -> addConverter(type, impl, constructor));
        type.addMethod(constructor.build());

        var convertMethod = MethodSpec.methodBuilder("convert")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(GenericRecord.class)
                .addParameter(contractInterface.asTypeName(), "event")
                .addStatement(CodeBlock.of("""
                        return switch (event) {
                            $L
                            default -> throw new $T("Unknown event type: " + event.getClass());
                        }""",
                        implementations.stream()
                                .map(impl -> {
                                    var converterName = uncapitalize("%sToGenericRecordConverter"
                                            .formatted(impl.simpleName().replace(".", "")));
                                    return CodeBlock.of("case $T e -> $L.convert(e);",
                                            impl.asTypeName(), converterName);
                                })
                                .collect(CodeBlock.joining("\n    ")),
                        IllegalArgumentException.class));
        type.addMethod(convertMethod.build());

        fileWriter.writeFile(contractInterface.packageName(), name, type.build());
    }

    private static MethodSpec constructor(TypeManifest event, TypeSpec.Builder type) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(event.packageName() + ".infrastructure.avro",
                        "%sSchemaFactory".formatted(event.simpleName().replace(".", ""))), "schemaFactory")
                .addStatement("this.schema = schemaFactory.createSchema()");
        nestedTypes(List.of(event)).forEach(nestedType -> addConverter(type, nestedType, constructor));
        sealedSubtypes(event).forEach(subtype -> addConverter(type, subtype, constructor));
        return constructor.build();
    }

    private static void addConverter(TypeSpec.Builder type, TypeManifest subtype, MethodSpec.Builder constructor) {
        var converterName = uncapitalize("%sToGenericRecordConverter".formatted(subtype.simpleName().replace(".", "")));
        var converterType = ClassName.get(subtype.packageName() + ".infrastructure.avro",
                "%sToGenericRecordConverter".formatted(subtype.simpleName().replace(".", "")));
        type.addField(FieldSpec.builder(converterType, converterName, Modifier.PRIVATE, Modifier.FINAL).build());
        constructor.addParameter(converterType, converterName)
                .addStatement("this.$L = $L", converterName, converterName);
    }

    private static List<TypeManifest> sealedSubtypes(TypeManifest event) {
        return event.isSealed() ? event.permittedSubtypes() : Collections.emptyList();
    }

    private MethodSpec convertMethod(TypeManifest event) {
        var method = MethodSpec.methodBuilder("convert")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(GenericRecord.class)
                .addParameter(event.asTypeName(), "event");
        if (event.isSealed()) {
            method.addStatement("return $L", sealedType(CodeBlock.of("event"), event));
        } else {
            method.addStatement("var genericRecord = new $T(schema)", GenericData.Record.class)
                    .addCode(mapFields(event))
                    .addStatement("return genericRecord");
        }
        return method.build();
    }

    private CodeBlock mapFields(TypeManifest event) {
        var codeBlock = CodeBlock.builder();
        event.fields().forEach(field -> {
            if (field.type().isCustomType()) {
                var pluginValue = context.plugins().stream()
                        .map(plugin -> plugin.toAvroValueOf(field.type(), CodeBlock.of("event.$L()", field.name())))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .flatMap(opt -> opt);
                if (pluginValue.isPresent()) {
                    codeBlock.addStatement("genericRecord.put($S, $L)", field.name(), pluginValue.get());
                } else {
                    context.processingEnvironment().getMessager().printMessage(
                            Diagnostic.Kind.WARNING,
                            ("Field '%s' of @CustomType '%s' is omitted from Avro serialization: no PrefabPlugin " +
                            "provides a toAvroValueOf() implementation. Implement PrefabPlugin.toAvroValueOf() to " +
                            "include this field in the Avro record.")
                                    .formatted(field.name(), field.type()),
                            field.element());
                }
            } else {
                codeBlock.addStatement("genericRecord.put($S, $L)", field.name(), field(
                        CodeBlock.of("event.$L()", field.name()),
                        CodeBlock.of("schema.getField($S).schema()", field.name()),
                        field.type()));
            }
        });
        return codeBlock.build();
    }

    private CodeBlock field(CodeBlock value, CodeBlock schema, TypeManifest type) {
        if (isLogicalType(type)) {
            return maybeNull(value, logicalType(value, type));
        } else if (type.isEnum()) {
            return maybeNull(value, CodeBlock.of("new $T($T.enumSchemaOf($L), $L.name())", GenericData.EnumSymbol.class, SchemaSupport.class, schema, value));
        } else if (type.isSealed()) {
            return maybeNull(value, sealedType(value, type));
        } else if (isNestedRecord(type)) {
            var converterName = uncapitalize("%sToGenericRecordConverter".formatted(type.simpleName().replace(".", "")));
            return maybeNull(value, CodeBlock.of("$L.convert($L)", converterName, value));
        } else if (type.is(List.class)) {
            return maybeNull(value, listType(value, schema, type));
        } else if (type.isCustomType()) {
            return context.plugins().stream()
                    .map(plugin -> plugin.toAvroValueOf(type, value))
                    .filter(Optional::isPresent)
                    .findFirst()
                    .flatMap(opt -> opt)
                    .orElseGet(() -> {
                        context.processingEnvironment().getMessager().printMessage(
                                Diagnostic.Kind.WARNING,
                                ("@CustomType '%s' has no Avro serialization: no PrefabPlugin provides a " +
                                "toAvroValueOf() implementation. Implement PrefabPlugin.toAvroValueOf() to " +
                                "support Avro serialization for this type.")
                                        .formatted(type),
                                type.asElement());
                        return CodeBlock.of("null");
                    });
        } else if (type.isStandardType()) {
            return CodeBlock.of("$L", value);
        } else {
            throw new IllegalStateException("Unsupported field type: " + type);
        }
    }

    private static CodeBlock maybeNull(CodeBlock value, CodeBlock nonNullValue) {
        return CodeBlock.of("$L != null ? $L : null", value, nonNullValue);
    }

    private static CodeBlock logicalType(CodeBlock value, TypeManifest type) {
        if (type.is(Instant.class)) {
            return CodeBlock.of("$L.toEpochMilli()", value);
        } else if (type.is(LocalDate.class)) {
            return CodeBlock.of("(int) $L.toEpochDay()", value);
        } else if (type.is(Duration.class)) {
            return CodeBlock.of("$L.toMillis()", value);
        } else if (type.isSingleValueType()) {
            return CodeBlock.of("$L.$N()", value, type.singleValueAccessor());
        } else {
            throw new IllegalStateException("Unsupported logical type: " + type);
        }
    }

    private static CodeBlock sealedType(CodeBlock value, TypeManifest type) {
        return CodeBlock.of("""
                        switch($L) {
                            $L
                        }""",
                value,
                type.permittedSubtypes().stream()
                        .map(subtype -> {
                            var converterName = uncapitalize("%sToGenericRecordConverter".formatted(subtype.simpleName().replace(".", "")));
                            return CodeBlock.of("case $T v -> $L.convert(v);", subtype.asTypeName(), converterName);
                        }).collect(CodeBlock.joining("\n    ")));
    }

    private CodeBlock listType(CodeBlock value, CodeBlock schema, TypeManifest type) {
        var itemType = type.parameters().getFirst();
        var arraySchema = CodeBlock.of("$T.arraySchemaOf($L)", SchemaSupport.class, schema);
        return CodeBlock.of("""
                        new $T(
                            $L,
                            $L.stream()
                                .map(item -> $L)
                                .toList()
                        )""",
                GenericData.Array.class,
                arraySchema,
                value,
                field(CodeBlock.of("item"), CodeBlock.of("$T.arraySchemaOf($L).getElementType()", SchemaSupport.class, schema), itemType));
    }
}
