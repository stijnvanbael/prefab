package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static be.appify.prefab.processor.TestClasses.MOCK_PART;
import static be.appify.prefab.processor.TestClasses.TEST_UTIL;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class CreateTestClientWriter {
    List<MethodSpec> createMethods(ClassManifest manifest, ExecutableElement constructor, PrefabContext context) {
        if (constructor.getParameters().isEmpty()) {
            return List.of(createNoBodyMethod(manifest, constructor));
        }
        var allParams = constructor.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var parentParam = parentName.flatMap(name ->
                allParams.stream().filter(p -> name.equals(p.name())).findFirst());
        var parentPathParam = parentParam.map(p -> ParameterSpec.builder(String.class, p.name() + "Id").build());
        var bodyParams = allParams.stream()
                .filter(p -> parentName.map(name -> !name.equals(p.name())).orElse(true))
                .toList();
        var individualBodyParams = bodyParams.stream()
                .flatMap(param -> context.requestParameterBuilder().buildTestClientParameter(param).stream())
                .toList();
        var allIndividualParams = parentPathParam.map(pp ->
                java.util.stream.Stream.concat(java.util.stream.Stream.of(pp), individualBodyParams.stream()).toList())
                .orElse(individualBodyParams);
        return List.of(
                createIndividualParamsMethod(manifest, allIndividualParams, parentPathParam.isPresent(), bodyParams),
                createRequestOverload(manifest, constructor, context, parentPathParam));
    }

    MethodSpec baseCreateMethodForPolymorphic(PolymorphicAggregateManifest polymorphic, Create create) {
        var unionClass = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(polymorphic.simpleName()));
        var path = "/" + ControllerUtil.pathOf(polymorphic) + create.path();
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class);
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        method.addParameter(unionClass, "request");
        var pathExpression = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", path, parent.name()))
                .orElseGet(() -> CodeBlock.of("$S", path));
        return method.addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L
                                .contentType($T.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                                .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathExpression,
                        ControllerUtil.withMockUser(create.security()),
                        MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    List<MethodSpec> createMethodsForPolymorphicUnion(
            PolymorphicAggregateManifest polymorphic,
            Map.Entry<ClassManifest, ExecutableElement> entry,
            PrefabContext context
    ) {
        var leafName = leafName(entry.getKey().simpleName());
        var individualParams = getIndividualParams(entry.getValue(), context);
        return List.of(
                createIndividualParamsMethodForPolymorphic(polymorphic, leafName, individualParams),
                createUnionWrapperMethod(polymorphic, leafName, entry.getValue()));
    }

    private static MethodSpec createUnionWrapperMethod(
            PolymorphicAggregateManifest polymorphic,
            String leafName,
            ExecutableElement constructor
    ) {
        var unionName = "Create%sRequest".formatted(polymorphic.simpleName());
        var nestedClass = ClassName.get(polymorphic.packageName() + ".application", unionName,
                "Create%sRequest".formatted(leafName));
        var flatClass = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(leafName));
        var createRequest = org.apache.commons.lang3.StringUtils.uncapitalize(leafName);
        var paramAccess = constructor.getParameters().stream()
                .map(p -> createRequest + "." + p.getSimpleName() + "()")
                .collect(Collectors.joining(", "));
        var createCall = polymorphic.parent()
                .map(parent -> "return create(%s.%s(), new $T($L))".formatted(createRequest, parent.name()))
                .orElse("return create(new $T($L))");
        return MethodSpec.methodBuilder("create" + leafName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(flatClass, createRequest)
                .addException(Exception.class)
                .addStatement(createCall, nestedClass, paramAccess)
                .build();
    }

    List<MethodSpec> createMethodsForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        var leafName = leafName(subtype.simpleName());
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var individualParams = getIndividualParams(constructor, context);
        var path = "/" + ControllerUtil.pathOf(polymorphic) + create.path();
        if (constructor.getParameters().isEmpty()) {
            return List.of(createNoBodyMethodForPolymorphic(polymorphic, leafName, create));
        }
        return List.of(
                createIndividualParamsMethodForPolymorphic(polymorphic, leafName, individualParams),
                createRequestOverloadForPolymorphic(polymorphic, leafName, constructor, path));
    }

    private List<ParameterSpec> getIndividualParams(ExecutableElement constructor, PrefabContext context) {
        return constructor.getParameters().stream()
                .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                .flatMap(param -> context.requestParameterBuilder().buildTestClientParameter(param).stream())
                .toList();
    }

    private MethodSpec createIndividualParamsMethod(
            ClassManifest manifest,
            List<ParameterSpec> allIndividualParams,
            boolean hasParentPathParam,
            List<VariableManifest> bodyParams
    ) {
        var bodyType = ClassName.get(manifest.packageName() + ".application",
                "Create%sRequest".formatted(manifest.simpleName()));
        var bodyParamNames = bodyParams.stream()
                .flatMap(p -> {
                    var spec = allIndividualParams.stream()
                            .filter(ps -> ps.name().equals(p.name()) || ps.name().equals(p.name() + "Id"))
                            .findFirst();
                    return spec.map(ParameterSpec::name).stream();
                })
                .collect(Collectors.joining(", "));
        var serviceCallArgs = hasParentPathParam
                ? allIndividualParams.getFirst().name() + ", new $T(" + bodyParamNames + ")"
                : "new $T(" + bodyParamNames + ")";
        return MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameters(allIndividualParams)
                .addException(Exception.class)
                .addStatement("return create$L(" + serviceCallArgs + ")",
                        manifest.simpleName(),
                        bodyType)
                .build();
    }

    private MethodSpec createRequestOverload(
            ClassManifest manifest,
            ExecutableElement constructor,
            PrefabContext context,
            java.util.Optional<ParameterSpec> parentPathParam
    ) {
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var createRequest = uncapitalize(manifest.simpleName());
        var bodyType = ClassName.get(manifest.packageName() + ".application",
                "Create%sRequest".formatted(manifest.simpleName()));
        var requestParts = Stream.concat(constructor.getParameters().stream()
                        .flatMap(parameter -> context.requestParameterBuilder()
                                .buildMethodParameter(VariableManifest.of(parameter, context.processingEnvironment()))
                                .stream()),
                Stream.of(ParameterSpec.builder(bodyType, createRequest).build())
        ).toList();
        var method = MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        method.addParameter(bodyType, createRequest);
        var pathVariables = parentPathParam.map(pp -> pp.name()).orElse("");
        if (requestParts.size() == 1) {
            return withRequestBody(manifest, method, create, pathVariables, createRequest);
        } else {
            return withMultipart(manifest, method, create, pathVariables, createRequest, requestParts);
        }
    }

    private MethodSpec createNoBodyMethod(ClassManifest manifest, ExecutableElement constructor) {
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var method = MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class);
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        var pathVariables = manifest.parent()
                .map(VariableManifest::name)
                .orElse("");
        return withoutRequestBody(manifest, method, create, pathVariables);
    }

    private static MethodSpec withoutRequestBody(ClassManifest manifest, MethodSpec.Builder method, Create create, String pathVariables) {
        return method.addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L)
                                        .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathVariables(manifest, create, pathVariables),
                        ControllerUtil.withMockUser(create.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private static MethodSpec withRequestBody(
            ClassManifest manifest,
            MethodSpec.Builder method,
            Create create,
            String pathVariables,
            String createRequest
    ) {
        return method.addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L
                                .contentType($T.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString($L)))
                                .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathVariables(manifest, create, pathVariables),
                        ControllerUtil.withMockUser(create.security()),
                        MediaType.class,
                        createRequest,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private static MethodSpec withMultipart(
            ClassManifest manifest,
            MethodSpec.Builder method,
            Create create,
            String pathVariables,
            String createRequest,
            List<ParameterSpec> requestParts
    ) {
        requestParts.forEach(part -> {
            if (part.type().equals(ClassName.get(MultipartFile.class))) {
                method.addStatement("var $LMock = $T.mockMultipartFile($L.$L())",
                        part.name(),
                        TEST_UTIL,
                        createRequest,
                        part.name());
            } else {
                method.addStatement(
                        "var bodyPart = new $T($S, null, jsonMapper.writeValueAsBytes($L), $T.APPLICATION_JSON)",
                        MOCK_PART,
                        "body",
                        createRequest,
                        MediaType.class);
            }
        });
        return method.addStatement("""
                                var result = mockMvc.perform($T.multipart($L)
                                    $L
                                ).andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        pathVariables(manifest, create, pathVariables),
                        requestParts.stream().map(part -> {
                            if (part.type().equals(ClassName.get(MultipartFile.class))) {
                                return ".file(%sMock)".formatted(part.name());
                            } else {
                                return ".part(bodyPart)";
                            }
                        }).collect(Collectors.joining("\n")),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private static CodeBlock pathVariables(ClassManifest manifest, Create create, String pathVariables) {
        return pathVariables.isBlank()
                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + create.path())
                : CodeBlock.of("$S, $L", "/" + ControllerUtil.pathOf(manifest) + create.path(),
                        pathVariables);
    }


    private static MethodSpec createNoBodyMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            String leafName,
            Create create
    ) {
        var path = "/" + ControllerUtil.pathOf(polymorphic) + create.path();
        var method = MethodSpec.methodBuilder("create" + leafName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class);
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        var pathExpression = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", path, parent.name()))
                .orElseGet(() -> CodeBlock.of("$S", path));
        return method.addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L)
                                        .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathExpression,
                        ControllerUtil.withMockUser(create.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private static MethodSpec createIndividualParamsMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            String leafName,
            List<ParameterSpec> individualParams
    ) {
        var bodyType = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(leafName));
        return MethodSpec.methodBuilder("create" + leafName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameters(individualParams)
                .addException(Exception.class)
                .addStatement("return create$L(new $T($L))",
                        leafName,
                        bodyType,
                        individualParams.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")))
                .build();
    }

    private static MethodSpec createRequestOverloadForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            String leafName,
            ExecutableElement constructor,
            String path
    ) {
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var createRequest = uncapitalize(leafName);
        var bodyType = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(leafName));
        var pathExpression = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L.$N()", path, createRequest, parent.name()))
                .orElseGet(() -> CodeBlock.of("$S", path));
        return MethodSpec.methodBuilder("create" + leafName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(bodyType, createRequest)
                .addException(Exception.class)
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L
                                .contentType($T.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString($L)))
                                .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathExpression,
                        ControllerUtil.withMockUser(create.security()),
                        MediaType.class,
                        createRequest,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
