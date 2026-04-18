package be.appify.prefab.processor.rest.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
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
        return method.addStatement("""
                                mockMvc.perform($T.$N($L, id)$L
                                        .contentType($T.APPLICATION_JSON)
                                        $L
                                        .andExpect($T.status().isOk())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        update.method().toLowerCase(),
                        pathVariables(manifest, update, pathVariables),
                        ControllerUtil.withMockUser(update.security()),
                        MediaType.class,
                         update.requestParameters().isEmpty() ? ")" : ".content(jsonMapper.writeValueAsString(request)))",
                        MOCK_MVC_RESULT_MATCHERS)
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
        return method.addStatement("""
                                return mockMvc.perform($T.multipart($L, id)$L
                                        $L
                                        ).andExpect($T.status().isOk())""",
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
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    private static CodeBlock pathVariables(ClassManifest manifest, UpdateManifest update, String pathVariables) {
        return pathVariables.isBlank()
                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + "/{id}" + update.path())
                : CodeBlock.of("$S, $L",
                        "/" + ControllerUtil.pathOf(manifest) + "/{id}" + update.path(),
                        pathVariables);
    }
}
