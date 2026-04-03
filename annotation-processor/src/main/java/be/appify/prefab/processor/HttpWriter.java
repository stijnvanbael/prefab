package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.pathParameterAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
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
        var polymorphicResponseType = polymorphicResponseType(manifest);
        var type = TypeSpec.classBuilder("%sController".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("path", "$S", polymorphicPathOf(manifest))
                        .build());
        ControllerUtil.tagAnnotation(manifest.simpleName()).ifPresent(type::addAnnotation);
        type.addField(serviceType, "service", PRIVATE, FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(serviceType, "service")
                        .addStatement("this.service = service")
                        .build())
                .addMethod(polymorphicToResponseMethod(manifest, polymorphicResponseType));

        manifest.annotationsOfType(GetById.class).stream().findFirst().ifPresent(getById ->
                type.addMethod(polymorphicGetByIdMethod(manifest, polymorphicResponseType, getById)));

        manifest.annotationsOfType(GetList.class).stream().findFirst().ifPresent(getList ->
                type.addMethod(polymorphicGetListMethod(manifest, polymorphicResponseType, getList)));

        fileWriter.writeFile(manifest.packageName(), "%sController".formatted(manifest.simpleName()), type.build());
    }

    private MethodSpec polymorphicGetByIdMethod(PolymorphicAggregateManifest manifest,
            ClassName responseType, GetById getById) {
        var method = MethodSpec.methodBuilder("getById")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getById.method(), getById.path()));
        operationAnnotation("Get " + manifest.simpleName() + " by ID").ifPresent(method::addAnnotation);
        securedAnnotation(getById.security()).ifPresent(method::addAnnotation);
        var idParameter = ParameterSpec.builder(String.class, "id").addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + manifest.simpleName() + " ID").ifPresent(idParameter::addAnnotation);
        return method
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), responseType))
                .addParameter(idParameter.build())
                .addStatement("return toResponse(service.getById(id))")
                .build();
    }

    private MethodSpec polymorphicGetListMethod(PolymorphicAggregateManifest manifest,
            ClassName responseType, GetList getList) {
        var method = MethodSpec.methodBuilder("getList")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getList.method(), getList.path()))
                .returns(ParameterizedTypeName.get(
                        ClassName.get(ResponseEntity.class),
                        ParameterizedTypeName.get(ClassName.get(PagedModel.class), responseType)));
        operationAnnotation("List " + plural(manifest.simpleName())).ifPresent(method::addAnnotation);
        securedAnnotation(getList.security()).ifPresent(method::addAnnotation);
        method.addParameter(Pageable.class, "pageable");
        return method
                .addStatement("return $T.ok(new $T(service.getList(pageable).map($T::from)))",
                        ResponseEntity.class, PagedModel.class, responseType)
                .build();
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
                            .orElse(ResponseEntity.notFound().build())""".stripIndent(),
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
        var type = TypeSpec.recordBuilder("%sResponse".formatted(manifest.simpleName()))
                .addModifiers(PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(manifest.fields().stream()
                                .map(field -> ParameterSpec.builder(
                                        field.type().asTypeName(),
                                        field.name()
                                ).build()).toList())
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

    /**
     * Generates a sealed {@code {Type}Response} interface with:
     * <ul>
     *   <li>Jackson {@code @JsonTypeInfo} and {@code @JsonSubTypes} for polymorphic serialisation.</li>
     *   <li>One nested response {@code record} per permitted subtype.</li>
     *   <li>A static {@code from(SealedInterface)} factory method using pattern-matching switch.</li>
     * </ul>
     */
    private void writePolymorphicResponseType(PolymorphicAggregateManifest manifest) {
        var responseName = "%sResponse".formatted(manifest.simpleName());
        var responseClassName = polymorphicResponseType(manifest);

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

        var jsonTypeInfo = AnnotationSpec.builder(JSON_TYPE_INFO)
                .addMember("use", "$T.Id.NAME", JSON_TYPE_INFO)
                .addMember("property", "$S", "type")
                .build();
        var jsonSubTypes = AnnotationSpec.builder(JSON_SUB_TYPES)
                .addMember("value", "{$L}",
                        subTypeAnnotations.stream()
                                .map(a -> CodeBlock.of("$L", a))
                                .collect(CodeBlock.joining(", ")))
                .build();

        var permittedNames = manifest.subtypes().stream()
                .map(subtype -> {
                    var subtypeName = lastSimpleName(subtype.simpleName());
                    return ClassName.get(
                            "%s.infrastructure.http".formatted(manifest.packageName()),
                            responseName, "%sResponse".formatted(subtypeName));
                })
                .toList();

        var outerInterface = TypeSpec.interfaceBuilder(responseName)
                .addModifiers(PUBLIC)
                .addModifiers(javax.lang.model.element.Modifier.SEALED)
                .addAnnotation(jsonTypeInfo)
                .addAnnotation(jsonSubTypes);
        permittedNames.forEach(outerInterface::addPermittedSubclass);

        manifest.subtypes().forEach(subtype -> {
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
            var nestedRecord = TypeSpec.recordBuilder(subtypeResponseName)
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
            outerInterface.addType(nestedRecord);
        });

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
        var factoryMethod = MethodSpec.methodBuilder("from")
                .addModifiers(PUBLIC, STATIC)
                .returns(responseClassName)
                .addParameter(manifest.type().asTypeName(), "aggregate")
                .addStatement("return switch (aggregate) {\n$L;\n}", switchCases)
                .build();
        outerInterface.addMethod(factoryMethod);

        fileWriter.writeFile(manifest.packageName(), responseName, outerInterface.build());
    }

    static ClassName polymorphicResponseType(PolymorphicAggregateManifest manifest) {
        return ClassName.get("%s.infrastructure.http".formatted(manifest.packageName()),
                "%sResponse".formatted(manifest.simpleName()));
    }

    static String polymorphicPathOf(PolymorphicAggregateManifest manifest) {
        return toKebabCase(plural(manifest.simpleName()));
    }

    private static String lastSimpleName(String simpleName) {
        int dot = simpleName.lastIndexOf('.');
        return dot < 0 ? simpleName : simpleName.substring(dot + 1);
    }
}
