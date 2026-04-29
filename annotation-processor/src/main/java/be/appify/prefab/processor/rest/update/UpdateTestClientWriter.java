package be.appify.prefab.processor.rest.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static be.appify.prefab.processor.TestClasses.MOCK_PART;
import static be.appify.prefab.processor.TestClasses.TEST_UTIL;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
class UpdateTestClientWriter {
    List<MethodSpec> updateMethods(
            ClassManifest manifest,
            UpdateManifest update,
            PrefabContext context
    ) {
        var pathVariables = manifest.parent().stream()
                .map(parent -> "%sId".formatted(uncapitalize(parent.name())))
                .collect(Collectors.joining(", "));
        var bodyType = ClassName.get(
                manifest.packageName() + ".application",
                "%s%sRequest".formatted(
                        manifest.simpleName(),
                        capitalize(update.operationName())));
        var individualParams = update.requestParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder().buildTestClientParameter(parameter).stream())
                .toList();
        var requestParts = Stream.concat(update.requestParameters().stream()
                        .flatMap(parameter -> context.requestParameterBuilder()
                                .buildMethodParameter(parameter)
                                .stream()),
                !update.requestParameters().isEmpty()
                        ? Stream.of(ParameterSpec.builder(bodyType, "request").build())
                        : Stream.empty()
        ).toList();

        if (update.requestParameters().isEmpty()) {
            return List.of(buildNoBodyMethod(manifest, update, pathVariables));
        }
        return List.of(
                buildIndividualParamsMethod(manifest, update, pathVariables, bodyType, individualParams),
                buildRequestOverload(manifest, update, pathVariables, bodyType, requestParts));
    }

    private static MethodSpec buildNoBodyMethod(
            ClassManifest manifest,
            UpdateManifest update,
            String pathVariables
    ) {
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        if (!pathVariables.isBlank()) {
            method.addParameters(manifest.parent().stream()
                    .map(parent -> ParameterSpec.builder(String.class,
                            "%sId".formatted(uncapitalize(parent.name()))).build())
                    .collect(Collectors.toList()));
        }
        method.addException(Exception.class);
        return withRequestBody(manifest, update, method, pathVariables);
    }

    private static MethodSpec buildIndividualParamsMethod(
            ClassManifest manifest,
            UpdateManifest update,
            String pathVariables,
            ClassName bodyType,
            List<ParameterSpec> individualParams
    ) {
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        if (!pathVariables.isBlank()) {
            method.addParameters(manifest.parent().stream()
                    .map(parent -> ParameterSpec.builder(String.class,
                            "%sId".formatted(uncapitalize(parent.name()))).build())
                    .collect(Collectors.toList()));
        }
        method.addParameters(individualParams);
        method.addException(Exception.class);
        var idAndParentArgs = pathVariables.isBlank() ? "id" : "id, " + pathVariables;
        method.addStatement("$L($L, new $T($L))",
                update.operationName(),
                idAndParentArgs,
                bodyType,
                individualParams.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")));
        return method.build();
    }

    private static MethodSpec buildRequestOverload(
            ClassManifest manifest,
            UpdateManifest update,
            String pathVariables,
            ClassName bodyType,
            List<ParameterSpec> requestParts
    ) {
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        if (!pathVariables.isBlank()) {
            method.addParameters(manifest.parent().stream()
                    .map(parent -> ParameterSpec.builder(String.class,
                            "%sId".formatted(uncapitalize(parent.name()))).build())
                    .collect(Collectors.toList()));
        }
        method.addParameter(bodyType, "request");
        method.addException(Exception.class);
        if (requestParts.size() <= 1) {
            return withRequestBody(manifest, update, method, pathVariables);
        } else {
            return withMultipart(manifest, update, method, pathVariables, requestParts);
        }
    }

    private static MethodSpec withRequestBody(
            ClassManifest manifest,
            UpdateManifest update,
            MethodSpec.Builder method,
            String pathVariables
    ) {
        var statusMatcher = update.asyncCommit() ? "isAccepted()" : "isOk()";
        return method.addStatement("""
                                mockMvc.perform($T.$N($L, id)$L
                                        .contentType($T.APPLICATION_JSON)
                                        $L
                                        .andExpect($T.status().$L)""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        update.method().toLowerCase(),
                        pathVariables(manifest, update, pathVariables),
                        ControllerUtil.withMockUser(update.security()),
                        MediaType.class,
                         update.requestParameters().isEmpty() ? ")" : ".content(jsonMapper.writeValueAsString(request)))",
                        MOCK_MVC_RESULT_MATCHERS,
                        statusMatcher)
                .build();
    }

    private static MethodSpec withMultipart(
            ClassManifest manifest,
            UpdateManifest update,
            MethodSpec.Builder method,
            String pathVariables,
            List<ParameterSpec> requestParts
    ) {

        requestParts.forEach(part -> {
            if (part.type().equals(ClassName.get(MultipartFile.class))) {
                method.addStatement("var $LMock = $T.mockMultipartFile(request.$L())",
                        part.name(),
                        TEST_UTIL,
                        part.name());
            } else {
                method.addStatement(
                        "var bodyPart = new $T($S, null, jsonMapper.writeValueAsBytes(request), $T.APPLICATION_JSON)",
                        MOCK_PART,
                        "body",
                        MediaType.class);
            }
        });
        var statusMatcher = update.asyncCommit() ? "isAccepted()" : "isOk()";
        return method.addStatement("""
                                return mockMvc.perform($T.multipart($L, id)$L
                                        $L
                                        ).andExpect($T.status().$L)""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        pathVariables(manifest, update, pathVariables),
                        ControllerUtil.withMockUser(update.security()),
                        requestParts.stream()
                                .map(part -> {
                                    if (part.type().equals(ClassName.get(MultipartFile.class))) {
                                        return CodeBlock.of(".file($LMock)", part.name());
                                    } else {
                                        return CodeBlock.of(".part(bodyPart)");
                                    }
                                })
                                .reduce(CodeBlock.builder(), CodeBlock.Builder::add, (a, b) -> a)
                                .build(),
                        MOCK_MVC_RESULT_MATCHERS,
                        statusMatcher)
                .build();
    }

    private static CodeBlock pathVariables(ClassManifest manifest, UpdateManifest update, String pathVariables) {
        return pathVariables.isBlank()
                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + "/{id}" + update.path())
                : CodeBlock.of("$S, $L",
                        "/" + ControllerUtil.pathOf(manifest) + "/{id}" + update.path(),
                        pathVariables);
    }

    MethodSpec baseUpdateMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            UpdateManifest update
    ) {
        var operationName = capitalize(update.operationName());
        var unionClass = ClassName.get(polymorphic.packageName() + ".application",
                "%s%sRequest".formatted(polymorphic.simpleName(), operationName));
        var rawPath = "/" + ControllerUtil.pathOf(polymorphic) + "/{id}" + update.path();
        var path = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", rawPath, parent.name()))
                .orElse(CodeBlock.of("$S", rawPath));
        var method = MethodSpec.methodBuilder(uncapitalize(update.operationName()))
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addParameter(unionClass, "request")
                .addException(Exception.class)
                .addStatement("""
                                mockMvc.perform($T.$N($L, id)$L
                                        .contentType($T.APPLICATION_JSON)
                                        .content(jsonMapper.writeValueAsString(request)))
                                        .andExpect($T.status().isOk())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        update.method().toLowerCase(),
                        path,
                        ControllerUtil.withMockUser(update.security()),
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    List<MethodSpec> updateMethodsForPolymorphicUnion(
            PolymorphicAggregateManifest polymorphic,
            Map.Entry<ClassManifest, UpdateManifest> entry,
            PrefabContext context
    ) {
        var leafName = leafName(entry.getKey().simpleName());
        var update = entry.getValue();
        var opName = capitalize(update.operationName());
        var unionName = "%s%sRequest".formatted(polymorphic.simpleName(), opName);
        var nestedClass = ClassName.get(polymorphic.packageName() + ".application", unionName,
                "%s%sRequest".formatted(leafName, opName));
        var flatClass = ClassName.get(polymorphic.packageName() + ".application",
                "%s%sRequest".formatted(leafName, opName));
        var operationMethodName = uncapitalize(leafName + opName);
        var individualParams = update.requestParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder().buildTestClientParameter(parameter).stream())
                .toList();
        var paramAccess = update.requestParameters().stream()
                .map(p -> "request." + p.name() + "()")
                .collect(Collectors.joining(", "));

        var parentArg = polymorphic.parent().map(parent -> ", " + parent.name()).orElse("");

        var convenienceBuilder = MethodSpec.methodBuilder(operationMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        polymorphic.parent().ifPresent(parent -> convenienceBuilder.addParameter(String.class, parent.name()));
        var convenienceMethod = convenienceBuilder
                .addParameters(individualParams)
                .addException(Exception.class)
                .addStatement("$L(id$L, new $T($L))", operationMethodName, parentArg, flatClass,
                        individualParams.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")))
                .build();

        var wrapperBuilder = MethodSpec.methodBuilder(operationMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        polymorphic.parent().ifPresent(parent -> wrapperBuilder.addParameter(String.class, parent.name()));
        var wrapperMethod = wrapperBuilder
                .addParameter(flatClass, "request")
                .addException(Exception.class)
                .addStatement("$L(id$L, new $T($L))", uncapitalize(update.operationName()), parentArg, nestedClass,
                        paramAccess)
                .build();

        return List.of(convenienceMethod, wrapperMethod);
    }

    List<MethodSpec> updateMethodsForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            UpdateManifest update,
            PrefabContext context
    ) {
        var leafName = leafName(subtype.simpleName());
        var operationName = uncapitalize(leafName + capitalize(update.operationName()));
        var bodyType = ClassName.get(
                polymorphic.packageName() + ".application",
                "%s%sRequest".formatted(leafName, capitalize(update.operationName())));
        var individualParams = update.requestParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder().buildTestClientParameter(parameter).stream())
                .toList();
        var rawPath = "/" + ControllerUtil.pathOf(polymorphic) + "/{id}" + update.path();
        var path = polymorphic.parent()
                .map(parent -> CodeBlock.of("$S, $L", rawPath, parent.name()))
                .orElse(CodeBlock.of("$S", rawPath));

        if (update.requestParameters().isEmpty()) {
            return List.of(buildPolymorphicNoBodyMethod(polymorphic, operationName, update, path));
        }
        return List.of(
                buildPolymorphicIndividualParamsMethod(polymorphic, operationName, bodyType, individualParams),
                buildPolymorphicRequestOverload(polymorphic, operationName, bodyType, update, path));
    }

    private static MethodSpec buildPolymorphicNoBodyMethod(
            PolymorphicAggregateManifest polymorphic,
            String operationName,
            UpdateManifest update,
            CodeBlock path
    ) {
        var method = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addException(Exception.class)
                .addStatement("""
                                mockMvc.perform($T.$N($L, id)$L
                                        .contentType($T.APPLICATION_JSON)
                                        )
                                        .andExpect($T.status().isOk())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        update.method().toLowerCase(),
                        path,
                        ControllerUtil.withMockUser(update.security()),
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    private static MethodSpec buildPolymorphicIndividualParamsMethod(
            PolymorphicAggregateManifest polymorphic,
            String operationName,
            ClassName bodyType,
            List<ParameterSpec> individualParams
    ) {
        var parentArg = polymorphic.parent().map(parent -> ", " + parent.name()).orElse("");
        var method = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addParameters(individualParams)
                .addException(Exception.class)
                .addStatement("$L(id$L, new $T($L))",
                        operationName,
                        parentArg,
                        bodyType,
                        individualParams.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")))
                .build();
    }

    private static MethodSpec buildPolymorphicRequestOverload(
            PolymorphicAggregateManifest polymorphic,
            String operationName,
            ClassName bodyType,
            UpdateManifest update,
            CodeBlock path
    ) {
        var method = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        polymorphic.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        return method
                .addParameter(bodyType, "request")
                .addException(Exception.class)
                .addStatement("""
                                mockMvc.perform($T.$N($L, id)$L
                                        .contentType($T.APPLICATION_JSON)
                                        .content(jsonMapper.writeValueAsString(request)))
                                        .andExpect($T.status().isOk())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        update.method().toLowerCase(),
                        path,
                        ControllerUtil.withMockUser(update.security()),
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }
}
