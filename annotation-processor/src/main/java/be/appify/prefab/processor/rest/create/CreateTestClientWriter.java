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
import java.util.Optional;
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

    List<MethodSpec> asyncCreateMethods(ClassManifest manifest, ExecutableElement factoryMethod, PrefabContext context) {
        var create = Objects.requireNonNull(factoryMethod.getAnnotation(Create.class));
        var methodName = factoryMethod.getSimpleName().toString();
        var allParams = factoryMethod.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var parentParam = parentName.flatMap(name ->
                allParams.stream().filter(p -> name.equals(p.name())).findFirst());
        var parentPathParam = parentParam.map(p -> ParameterSpec.builder(String.class, p.name() + "Id").build());
        var bodyParams = allParams.stream()
                .filter(p -> parentName.map(name -> !name.equals(p.name())).orElse(true))
                .toList();
        if (bodyParams.isEmpty()) {
            return List.of(buildAsyncNoBodyMethod(manifest, create, methodName, parentPathParam));
        }
        var individualBodyParams = bodyParams.stream()
                .flatMap(param -> context.requestParameterBuilder().buildTestClientParameter(param).stream())
                .toList();
        var allIndividualParams = parentPathParam
                .map(pp -> Stream.concat(Stream.of(pp), individualBodyParams.stream()).toList())
                .orElse(individualBodyParams);
        var bodyType = ClassName.get("%s.application".formatted(manifest.packageName()),
                "Create%sRequest".formatted(manifest.simpleName()));
        return List.of(
                buildAsyncIndividualParamsMethod(methodName, allIndividualParams, bodyType, individualBodyParams,
                        parentPathParam),
                buildAsyncRequestOverload(manifest, create, methodName, bodyType, parentPathParam));
    }

    private static MethodSpec buildAsyncNoBodyMethod(ClassManifest manifest, Create create, String methodName,
            Optional<ParameterSpec> parentPathParam) {
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        var pathVariables = parentPathParam.map(ParameterSpec::name).orElse("");
        return method.addStatement("""
                        mockMvc.perform($T.$N($L)$L)
                                .andExpect($T.status().isAccepted())""",
                MOCK_MVC_REQUEST_BUILDERS,
                create.method().toLowerCase(),
                asyncPathVariables(manifest, create, pathVariables),
                ControllerUtil.withMockUser(create.security()),
                MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    private static MethodSpec buildAsyncIndividualParamsMethod(
            String methodName,
            List<ParameterSpec> allIndividualParams,
            ClassName bodyType,
            List<ParameterSpec> individualBodyParams,
            Optional<ParameterSpec> parentPathParam
    ) {
        var bodyParamNames = individualBodyParams.stream()
                .map(ParameterSpec::name)
                .collect(Collectors.joining(", "));
        var serviceCallArgs = parentPathParam
                .map(pp -> pp.name() + ", new $T(" + bodyParamNames + ")")
                .orElse("new $T(" + bodyParamNames + ")");
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameters(allIndividualParams)
                .addException(Exception.class)
                .addStatement(methodName + "(" + serviceCallArgs + ")", bodyType)
                .build();
    }

    private static MethodSpec buildAsyncRequestOverload(
            ClassManifest manifest,
            Create create,
            String methodName,
            ClassName bodyType,
            Optional<ParameterSpec> parentPathParam
    ) {
        var createRequest = uncapitalize(manifest.simpleName());
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        method.addParameter(bodyType, createRequest);
        var pathVariables = parentPathParam.map(ParameterSpec::name).orElse("");
        return method.addStatement("""
                        mockMvc.perform($T.$N($L)$L
                        .contentType($T.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString($L)))
                        .andExpect($T.status().isAccepted())""",
                MOCK_MVC_REQUEST_BUILDERS,
                create.method().toLowerCase(),
                asyncPathVariables(manifest, create, pathVariables),
                ControllerUtil.withMockUser(create.security()),
                MediaType.class,
                createRequest,
                MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    private static CodeBlock asyncPathVariables(ClassManifest manifest, Create create, String pathVariables) {
        return pathVariables.isBlank()
                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + create.path())
                : CodeBlock.of("$S, $L", "/" + ControllerUtil.pathOf(manifest) + create.path(), pathVariables);
    }

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
        var parentName = polymorphic.parent().map(VariableManifest::name);
        var individualParams = individualParamsExcludingParent(entry.getValue(), context, parentName);
        return List.of(createUnionIndividualParamsMethod(polymorphic, leafName, individualParams));
    }

    private static MethodSpec createUnionIndividualParamsMethod(
            PolymorphicAggregateManifest polymorphic,
            String leafName,
            List<ParameterSpec> individualParams
    ) {
        var unionName = "Create%sRequest".formatted(polymorphic.simpleName());
        var nestedClass = ClassName.get(polymorphic.packageName() + ".application", unionName,
                "Create%sRequest".formatted(leafName));
        var parentParam = polymorphic.parent()
                .map(parent -> ParameterSpec.builder(String.class, parent.name()).build());
        var bodyParamNames = individualParams.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
        var createCallArgs = parentParam
                .map(pp -> pp.name() + ", new $T(" + bodyParamNames + ")")
                .orElse("new $T(" + bodyParamNames + ")");
        var allParams = parentParam
                .map(pp -> Stream.concat(Stream.of(pp), individualParams.stream()).toList())
                .orElse(individualParams);
        return MethodSpec.methodBuilder("create" + leafName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameters(allParams)
                .addException(Exception.class)
                .addStatement("return create(" + createCallArgs + ")", nestedClass)
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
        var parentName = polymorphic.parent().map(VariableManifest::name);
        var individualParams = individualParamsExcludingParent(constructor, context, parentName);
        var path = "/" + ControllerUtil.pathOf(polymorphic) + create.path();
        if (constructor.getParameters().isEmpty()) {
            return List.of(createNoBodyMethodForPolymorphic(polymorphic, leafName, create));
        }
        return List.of(createPolymorphicIndividualParamsMethod(polymorphic, leafName, individualParams, path, create));
    }

    private static MethodSpec createPolymorphicIndividualParamsMethod(
            PolymorphicAggregateManifest polymorphic,
            String leafName,
            List<ParameterSpec> individualParams,
            String path,
            Create create
    ) {
        var bodyType = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(leafName));
        var parentParam = polymorphic.parent()
                .map(parent -> ParameterSpec.builder(String.class, parent.name()).build());
        var bodyParamNames = individualParams.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
        var allParams = parentParam
                .map(pp -> Stream.concat(Stream.of(pp), individualParams.stream()).toList())
                .orElse(individualParams);
        var pathExpression = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", path, parent.name()))
                .orElseGet(() -> CodeBlock.of("$S", path));
        return MethodSpec.methodBuilder("create" + leafName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameters(allParams)
                .addException(Exception.class)
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L
                                .contentType($T.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(new $T($L))))
                                .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathExpression,
                        ControllerUtil.withMockUser(create.security()),
                        MediaType.class,
                        bodyType,
                        bodyParamNames,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private List<ParameterSpec> individualParamsExcludingParent(
            ExecutableElement constructor,
            PrefabContext context,
            Optional<String> parentName
    ) {
        return constructor.getParameters().stream()
                .map(param -> VariableManifest.of(param, context.processingEnvironment()))
                .filter(p -> parentName.map(n -> !n.equals(p.name())).orElse(true))
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
            Optional<ParameterSpec> parentPathParam
    ) {
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var createRequest = uncapitalize(manifest.simpleName());
        var bodyType = ClassName.get(manifest.packageName() + ".application",
                "Create%sRequest".formatted(manifest.simpleName()));
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var requestParts = Stream.concat(constructor.getParameters().stream()
                        .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                        .filter(p -> parentName.map(n -> !n.equals(p.name())).orElse(true))
                        .flatMap(parameter -> context.requestParameterBuilder()
                                .buildMethodParameter(parameter)
                                .stream()),
                Stream.of(ParameterSpec.builder(bodyType, createRequest).build())
        ).toList();
        var method = MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        method.addParameter(bodyType, createRequest);
        var pathVariables = parentPathParam.map(ParameterSpec::name).orElse("");
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


    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
