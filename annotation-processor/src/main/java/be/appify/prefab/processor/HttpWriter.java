package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.atteo.evo.inflector.English.plural;

class HttpWriter {
    private static final ClassName JSON_TYPE_INFO =
            ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo");
    private static final ClassName JSON_SUB_TYPES =
            ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes");
    private static final ClassName JSON_SUB_TYPE =
            ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes", "Type");

    private final JavaFileWriter fileWriter;
    private final PrefabContext context;

    HttpWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.http");
    }

    void writeHttpLayer(ClassManifest manifest) {
        writeController(manifest);
        writeResponseRecord(manifest);
    }

    void writePolymorphicHttpLayer(PolymorphicAggregateManifest manifest) {
        writePolymorphicController(manifest);
        writePolymorphicResponseType(manifest);
    }

    private void writeController(ClassManifest manifest) {
        var serviceType = ClassName.get("%s.application".formatted(manifest.packageName()),
                "%sService".formatted(manifest.simpleName()));
        var type = TypeSpec.classBuilder("%sController".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", ControllerUtil.pathOf(manifest))
                        .build());
        ControllerUtil.tagAnnotation(manifest.simpleName()).ifPresent(type::addAnnotation);
        type.addField(serviceType, "service", PRIVATE, FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build())
                .addMethod(toResponseMethod(manifest));
        context.plugins().forEach(plugin -> plugin.writeController(manifest, type));
        fileWriter.writeFile(manifest.packageName(), "%sController".formatted(manifest.simpleName()), type.build());
    }

    private void writePolymorphicController(PolymorphicAggregateManifest manifest) {
        var serviceType = ClassName.get("%s.application".formatted(manifest.packageName()),
                "%sService".formatted(manifest.simpleName()));
        var polymorphicResponseType = ControllerUtil.responseType(manifest);
        var type = TypeSpec.classBuilder("%sController".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", ControllerUtil.pathOf(manifest))
                        .build());
        ControllerUtil.tagAnnotation(manifest.simpleName()).ifPresent(type::addAnnotation);
        type.addField(serviceType, "service", PRIVATE, FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build())
                .addMethod(polymorphicToResponseMethod(manifest, polymorphicResponseType));

        context.plugins().forEach(plugin -> plugin.writePolymorphicController(manifest, type));

        fileWriter.writeFile(manifest.packageName(), "%sController".formatted(manifest.simpleName()), type.build());
    }

    private MethodSpec polymorphicToResponseMethod(PolymorphicAggregateManifest manifest,
            ClassName polymorphicResponseType) {
        return MethodSpec.methodBuilder("toResponse")
                .addModifiers(PRIVATE, STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), polymorphicResponseType))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Optional.class),
                        manifest.type().asTypeName()), "aggregateRoot")
                .addStatement("""
                        return aggregateRoot
                            .map($T::from)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build())""",
                        polymorphicResponseType)
                .build();
    }

    private MethodSpec toResponseMethod(ClassManifest manifest) {
        return MethodSpec.methodBuilder("toResponse")
                .addModifiers(PRIVATE, STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class),
                        responseType(manifest)))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Optional.class),
                                ClassName.get(manifest.packageName(), manifest.simpleName())),
                        "aggregateRoot")
                .addStatement("""
                                return aggregateRoot
                                    .map($T::from)
                                    .map(ResponseEntity::ok)
                                    .orElse(ResponseEntity.notFound().build())""".stripIndent(),
                        responseType(manifest))
                .build();
    }

    private void writeResponseRecord(ClassManifest manifest) {
        var typeBuilder = TypeSpec.recordBuilder("%sResponse".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC);
        if (ControllerUtil.OPENAPI_INCLUDED) {
            var docAnnotation = manifest.annotationsOfType(Doc.class).stream().findFirst();
            docAnnotation.ifPresent(d -> typeBuilder.addAnnotation(
                    AnnotationSpec.builder(ClassName.get("io.swagger.v3.oas.annotations.media", "Schema"))
                            .addMember("description", "$S", d.value())
                            .build()));
        }
        var type = typeBuilder
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(manifest.fields().stream()
                                .map(field -> {
                                    var paramBuilder = ParameterSpec.builder(
                                            field.type().asTypeName(),
                                            field.name());
                                    if (ControllerUtil.OPENAPI_INCLUDED) {
                                        var exampleAnnotation = field.getAnnotation(Example.class);
                                        var docAnnotation = field.getAnnotation(Doc.class);
                                        if (exampleAnnotation.isPresent() || docAnnotation.isPresent()) {
                                            var schemaClass = ClassName.get("io.swagger.v3.oas.annotations.media", "Schema");
                                            var schemaBuilder = AnnotationSpec.builder(schemaClass);
                                            exampleAnnotation.ifPresent(e -> schemaBuilder.addMember("example", "$S", e.value().value()));
                                            docAnnotation.ifPresent(d -> schemaBuilder.addMember("description", "$S", d.value().value()));
                                            paramBuilder.addAnnotation(schemaBuilder.build());
                                        }
                                    }
                                    return paramBuilder.build();
                                }).toList())
                        .build())
                .addMethod(MethodSpec.methodBuilder("from")
                        .addModifiers(PUBLIC, STATIC)
                        .returns(responseType(manifest))
                        .addParameter(ParameterSpec.builder(
                                ClassName.get(manifest.packageName(), manifest.simpleName()),
                                "aggregateRoot"
                        ).build())
                        .addStatement("return new $T($L)",
                                responseType(manifest),
                                manifest.fields().stream()
                                        .map(field -> "aggregateRoot.%s()".formatted(field.name()))
                                        .collect(Collectors.joining(",\n"))
                        )
                        .build())
                .build();
        fileWriter.writeFile(manifest.packageName(), "%sResponse".formatted(manifest.simpleName()), type);
    }

    private void writePolymorphicResponseType(PolymorphicAggregateManifest manifest) {
        var responseName = "%sResponse".formatted(manifest.simpleName());
        var responseClassName = ControllerUtil.responseType(manifest);
        var outerInterface = TypeSpec.interfaceBuilder(responseName)
                .addModifiers(PUBLIC)
                .addModifiers(javax.lang.model.element.Modifier.SEALED)
                .addAnnotation(buildJsonTypeInfo())
                .addAnnotation(buildJsonSubTypes(manifest, responseName));
        buildPermittedNames(manifest, responseName).forEach(outerInterface::addPermittedSubclass);
        manifest.subtypes().forEach(subtype ->
                outerInterface.addType(buildSubtypeResponseRecord(manifest, subtype, responseName, responseClassName)));
        outerInterface.addMethod(buildPolymorphicFromMethod(manifest, responseClassName, responseName));
        fileWriter.writeFile(manifest.packageName(), responseName, outerInterface.build());
    }

    private AnnotationSpec buildJsonTypeInfo() {
        return AnnotationSpec.builder(JSON_TYPE_INFO)
                .addMember("use", "$T.Id.NAME", JSON_TYPE_INFO)
                .addMember("property", "$S", "type")
                .build();
    }

    private AnnotationSpec buildJsonSubTypes(PolymorphicAggregateManifest manifest, String responseName) {
        var subTypeAnnotations = manifest.subtypes().stream()
                .map(subtype -> {
                    var subtypeName = lastSimpleName(subtype.simpleName());
                    var subtypeResponseClass = ClassName.get(
                            "%s.infrastructure.http".formatted(manifest.packageName()),
                            responseName, "%sResponse".formatted(subtypeName));
                    return AnnotationSpec.builder(JSON_SUB_TYPE)
                            .addMember("value", "$T.class", subtypeResponseClass)
                            .addMember("name", "$S", subtypeName)
                            .build();
                })
                .toList();
        return AnnotationSpec.builder(JSON_SUB_TYPES)
                .addMember("value", "{$L}",
                        subTypeAnnotations.stream()
                                .map(a -> CodeBlock.of("$L", a))
                                .collect(CodeBlock.joining(", ")))
                .build();
    }

    private List<ClassName> buildPermittedNames(PolymorphicAggregateManifest manifest, String responseName) {
        return manifest.subtypes().stream()
                .map(subtype -> ClassName.get(
                        "%s.infrastructure.http".formatted(manifest.packageName()),
                        responseName, "%sResponse".formatted(lastSimpleName(subtype.simpleName()))))
                .toList();
    }

    private TypeSpec buildSubtypeResponseRecord(PolymorphicAggregateManifest manifest, ClassManifest subtype,
            String responseName, ClassName responseClassName) {
        var subtypeName = lastSimpleName(subtype.simpleName());
        var subtypeResponseName = "%sResponse".formatted(subtypeName);
        var subtypeResponseClassName = ClassName.get(
                "%s.infrastructure.http".formatted(manifest.packageName()),
                responseName, subtypeResponseName);
        var fromMethod = MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .returns(subtypeResponseClassName)
                .addParameter(subtype.type().asTypeName(), "subtype")
                .addStatement("return new $T($L)",
                        subtypeResponseClassName,
                        subtype.fields().stream()
                                .map(field -> "subtype.%s()".formatted(field.name()))
                                .collect(Collectors.joining(",\n")))
                .build();
        return TypeSpec.recordBuilder(subtypeResponseName)
                .addModifiers(PUBLIC, STATIC)
                .addSuperinterface(responseClassName)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(subtype.fields().stream()
                                .map(field -> ParameterSpec.builder(
                                        field.type().asTypeName(), field.name()).build())
                                .toList())
                        .build())
                .addMethod(fromMethod)
                .build();
    }

    private MethodSpec buildPolymorphicFromMethod(PolymorphicAggregateManifest manifest,
            ClassName responseClassName, String responseName) {
        var switchCases = manifest.subtypes().stream()
                .map(subtype -> {
                    var subtypeName = lastSimpleName(subtype.simpleName());
                    var subtypeResponseClass = ClassName.get(
                            "%s.infrastructure.http".formatted(manifest.packageName()),
                            responseName, "%sResponse".formatted(subtypeName));
                    return CodeBlock.of("case $T t -> $T.from(t)", subtype.type().asTypeName(),
                            subtypeResponseClass);
                })
                .collect(CodeBlock.joining(";\n"));
        return MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .returns(responseClassName)
                .addParameter(manifest.type().asTypeName(), "aggregate")
                .addStatement("return switch (aggregate) {\n$L;\n}", switchCases)
                .build();
    }

    static String polymorphicPathOf(PolymorphicAggregateManifest manifest) {
        return toKebabCase(plural(manifest.simpleName()));
    }

    private static String lastSimpleName(String simpleName) {
        int dot = simpleName.lastIndexOf('.');
        return dot < 0 ? simpleName : simpleName.substring(dot + 1);
    }
}
