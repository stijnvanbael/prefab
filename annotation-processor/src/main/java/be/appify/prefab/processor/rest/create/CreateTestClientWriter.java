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
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static be.appify.prefab.processor.TestClasses.MOCK_PART;
import static be.appify.prefab.processor.TestClasses.REST_RESPONSE_ASSERT;
import static be.appify.prefab.processor.TestClasses.TEST_UTIL;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class CreateTestClientWriter {

    private static TypeName voidResponseAssert() {
        return ParameterizedTypeName.get(REST_RESPONSE_ASSERT, TypeName.get(Void.class));
    }

    List<MethodSpec> asyncCreateMethods(ClassManifest manifest, ExecutableElement factoryMethod, PrefabContext context) {
        var create = Objects.requireNonNull(factoryMethod.getAnnotation(Create.class));
        var pathVarNames = be.appify.prefab.processor.rest.PathVariables.extractFrom(create.path());
        var methodName = factoryMethod.getSimpleName().toString();
        var createMethodInfo = getCreateMethodInfo(manifest, factoryMethod, context, pathVarNames);
        var pathVarNamesStr = createMethodInfo.pathVarParams().stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
        if (createMethodInfo.bodyParams().isEmpty()) {
            return List.of(buildAsyncNoBodyMethod(manifest, create, methodName, createMethodInfo.parentPathParam(), createMethodInfo.pathVarParams(),
                    pathVarNamesStr));
        }
        var individualBodyParams = createMethodInfo.bodyParams().stream()
                .flatMap(param -> context.requestParameterBuilder().buildTestClientParameter(param).stream())
                .toList();
        var allIndividualParams = Stream.concat(
                        Stream.concat(createMethodInfo.parentPathParam().stream(), createMethodInfo.pathVarParams().stream()),
                        individualBodyParams.stream())
                .toList();
        var bodyType = ClassName.get("%s.application".formatted(manifest.packageName()),
                capitalize(methodName) + "Request");
        return List.of(
                buildAsyncIndividualParamsMethod(methodName, allIndividualParams, bodyType, individualBodyParams,
                        createMethodInfo.parentPathParam(), pathVarNamesStr),
                buildAsyncRequestOverload(manifest, create, methodName, bodyType, createMethodInfo.parentPathParam(), createMethodInfo.pathVarParams(),
                        pathVarNamesStr));
    }

    private static CreateMethodInfo getCreateMethodInfo(
            ClassManifest manifest,
            ExecutableElement factoryMethod,
            PrefabContext context,
            Set<String> pathVarNames
    ) {
        var allParams = factoryMethod.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var parentParam = parentName.flatMap(name ->
                allParams.stream().filter(p -> name.equals(p.name())).findFirst());
        var parentPathParam = parentParam.map(p -> ParameterSpec.builder(String.class, p.name() + "Id").build());
        var pathVarParams = allParams.stream()
                .filter(p -> pathVarNames.contains(p.name()))
                .map(p -> ParameterSpec.builder(String.class, p.name()).build())
                .toList();
        var bodyParams = allParams.stream()
                .filter(p -> parentName.map(name -> !name.equals(p.name())).orElse(true))
                .filter(p -> !pathVarNames.contains(p.name()))
                .toList();
        return new CreateMethodInfo(parentPathParam, pathVarParams, bodyParams);
    }

    private record CreateMethodInfo(
            Optional<ParameterSpec> parentPathParam,
            List<ParameterSpec> pathVarParams,
            List<VariableManifest> bodyParams
    ) {
    }

    private static MethodSpec buildAsyncNoBodyMethod(
            ClassManifest manifest,
            Create create,
            String methodName,
            Optional<ParameterSpec> parentPathParam,
            List<ParameterSpec> pathVarParams,
            String pathVarNamesStr
    ) {
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(voidResponseAssert())
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        method.addParameters(pathVarParams);
        var pathVariables = Stream.of(parentPathParam.map(ParameterSpec::name).orElse(""), pathVarNamesStr)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        return method
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L)
                                        .andExpect($T.status().isAccepted())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        asyncPathVariables(manifest, create, pathVariables),
                        ControllerUtil.withMockUser(create.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return new $T<>(result, null, null)", REST_RESPONSE_ASSERT)
                .build();
    }

    private static MethodSpec buildAsyncIndividualParamsMethod(
            String methodName,
            List<ParameterSpec> allIndividualParams,
            ClassName bodyType,
            List<ParameterSpec> individualBodyParams,
            Optional<ParameterSpec> parentPathParam,
            String pathVarNamesStr
    ) {
        var bodyParamNames = individualBodyParams.stream()
                .map(ParameterSpec::name)
                .collect(Collectors.joining(", "));
        var hasBody = !individualBodyParams.isEmpty();
        var prefixArgs = Stream.of(
                        parentPathParam.map(ParameterSpec::name).orElse(""),
                        pathVarNamesStr)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        var builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(voidResponseAssert())
                .addParameters(allIndividualParams)
                .addException(Exception.class);
        if (hasBody) {
            var serviceCallArgs = prefixArgs.isBlank()
                    ? "new $T(" + bodyParamNames + ")"
                    : prefixArgs + ", new $T(" + bodyParamNames + ")";
            builder.addStatement("return " + methodName + "(" + serviceCallArgs + ")", bodyType);
        } else {
            builder.addStatement("return $L($L)", methodName, prefixArgs);
        }
        return builder.build();
    }

    private static MethodSpec buildAsyncRequestOverload(
            ClassManifest manifest,
            Create create,
            String methodName,
            ClassName bodyType,
            Optional<ParameterSpec> parentPathParam,
            List<ParameterSpec> pathVarParams,
            String pathVarNamesStr
    ) {
        var createRequest = uncapitalize(manifest.simpleName());
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(voidResponseAssert())
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        method.addParameters(pathVarParams);
        method.addParameter(bodyType, createRequest);
        var pathVariables = Stream.of(parentPathParam.map(ParameterSpec::name).orElse(""), pathVarNamesStr)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        return method
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L
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
                .addStatement("return new $T<>(result, null, null)", REST_RESPONSE_ASSERT)
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
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var pathVarNames = be.appify.prefab.processor.rest.PathVariables.extractFrom(create.path());
        var createMethodInfo = getCreateMethodInfo(manifest, constructor, context, pathVarNames);
        var individualBodyParams = createMethodInfo.bodyParams().stream()
                .flatMap(param -> context.requestParameterBuilder().buildTestClientParameter(param).stream())
                .toList();
        var allIndividualParams = Stream.concat(
                        Stream.concat(
                                createMethodInfo.parentPathParam().stream(),
                                createMethodInfo.pathVarParams().stream()),
                        individualBodyParams.stream())
                .toList();
        var pathVarNamesStr = createMethodInfo.pathVarParams().stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
        return List.of(
                createIndividualParamsMethod(manifest, allIndividualParams, createMethodInfo.parentPathParam().isPresent(),
                        pathVarNamesStr, createMethodInfo.bodyParams()),
                createRequestOverload(manifest, constructor, context, createMethodInfo.parentPathParam(), createMethodInfo.pathVarParams(), pathVarNamesStr));
    }

    MethodSpec baseCreateMethodForPolymorphic(PolymorphicAggregateManifest polymorphic, Create create) {
        var unionClass = ClassName.get(polymorphic.packageName() + ".application",
                "Create%sRequest".formatted(polymorphic.simpleName()));
        var path = "/" + ControllerUtil.pathOf(polymorphic) + create.path();
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(voidResponseAssert())
                .addException(Exception.class);
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        method.addParameter(unionClass, "request");
        var pathExpression = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", path, parent.name()))
                .orElseGet(() -> CodeBlock.of("$S", path));
        return method
                .addStatement("""
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
                .addStatement("return new $T<>(result, $T.idOf(result), null)", REST_RESPONSE_ASSERT, TEST_UTIL)
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
                .returns(voidResponseAssert())
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
                .returns(voidResponseAssert())
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
                .addStatement("return new $T<>(result, $T.idOf(result), null)", REST_RESPONSE_ASSERT, TEST_UTIL)
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
            String pathVarNamesStr,
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
        var hasBodyParams = !bodyParams.isEmpty();
        var prefixArgs = Stream.of(
                        hasParentPathParam ? allIndividualParams.getFirst().name() : null,
                        pathVarNamesStr.isBlank() ? null : pathVarNamesStr)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
        var builder = MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(voidResponseAssert())
                .addParameters(allIndividualParams)
                .addException(Exception.class);
        if (hasBodyParams) {
            var serviceCallArgs = prefixArgs.isBlank()
                    ? "new $T(" + bodyParamNames + ")"
                    : prefixArgs + ", new $T(" + bodyParamNames + ")";
            builder.addStatement("return create$L(" + serviceCallArgs + ")", manifest.simpleName(), bodyType);
        } else {
            var serviceCallArgs = prefixArgs.isBlank() ? "" : prefixArgs;
            builder.addStatement("return create$L($L)", manifest.simpleName(), serviceCallArgs);
        }
        return builder.build();
    }

    private MethodSpec createRequestOverload(
            ClassManifest manifest,
            ExecutableElement constructor,
            PrefabContext context,
            Optional<ParameterSpec> parentPathParam,
            List<ParameterSpec> pathVarParams,
            String pathVarNamesStr
    ) {
        var create = Objects.requireNonNull(constructor.getAnnotation(Create.class));
        var createRequest = uncapitalize(manifest.simpleName());
        var bodyType = ClassName.get(manifest.packageName() + ".application",
                "Create%sRequest".formatted(manifest.simpleName()));
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var requestParts = Stream.concat(constructor.getParameters().stream()
                        .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                        .filter(p -> parentName.map(n -> !n.equals(p.name())).orElse(true))
                        .filter(p -> !be.appify.prefab.processor.rest.PathVariables
                                .extractFrom(create.path()).contains(p.name()))
                        .flatMap(parameter -> context.requestParameterBuilder()
                                .buildMethodParameter(parameter)
                                .stream()),
                Stream.of(ParameterSpec.builder(bodyType, createRequest).build())
        ).toList();
        var method = MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(voidResponseAssert())
                .addException(Exception.class);
        parentPathParam.ifPresent(method::addParameter);
        method.addParameters(pathVarParams);
        method.addParameter(bodyType, createRequest);
        var pathVariables = Stream.of(
                        parentPathParam.map(ParameterSpec::name).orElse(""),
                        pathVarNamesStr)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
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
                .returns(voidResponseAssert())
                .addException(Exception.class);
        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        var pathVariables = manifest.parent()
                .map(VariableManifest::name)
                .orElse("");
        return withoutRequestBody(manifest, method, create, pathVariables);
    }

    private static MethodSpec withoutRequestBody(ClassManifest manifest, MethodSpec.Builder method, Create create, String pathVariables) {
        return method
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L)
                                        .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathVariables(manifest, create, pathVariables),
                        ControllerUtil.withMockUser(create.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return new $T<>(result, $T.idOf(result), null)", REST_RESPONSE_ASSERT, TEST_UTIL)
                .build();
    }

    private static MethodSpec withRequestBody(
            ClassManifest manifest,
            MethodSpec.Builder method,
            Create create,
            String pathVariables,
            String createRequest
    ) {
        return method
                .addStatement("""
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
                .addStatement("return new $T<>(result, $T.idOf(result), null)", REST_RESPONSE_ASSERT, TEST_UTIL)
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
        return method
                .addStatement("""
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
                .addStatement("return new $T<>(result, $T.idOf(result), null)", REST_RESPONSE_ASSERT, TEST_UTIL)
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
                .returns(voidResponseAssert())
                .addException(Exception.class);
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        var pathExpression = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", path, parent.name()))
                .orElseGet(() -> CodeBlock.of("$S", path));
        return method
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)$L)
                                        .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        pathExpression,
                        ControllerUtil.withMockUser(create.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return new $T<>(result, $T.idOf(result), null)", REST_RESPONSE_ASSERT, TEST_UTIL)
                .build();
    }


    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
