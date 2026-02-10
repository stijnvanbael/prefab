package be.appify.prefab.processor.rest.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
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
        return List.of(createMethod(manifest, constructor, context), whenVariant(manifest, constructor),
                givenVariant(manifest, constructor));
    }

    private MethodSpec whenVariant(ClassManifest manifest, ExecutableElement constructor) {
        return variant(manifest, constructor, "whenCreating" + manifest.simpleName());
    }

    private MethodSpec givenVariant(ClassManifest manifest, ExecutableElement constructor) {
        return variant(manifest, constructor, "given" + manifest.simpleName() + "Created");
    }

    private static MethodSpec variant(ClassManifest manifest, ExecutableElement constructor, String methodName) {
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);
        if (!constructor.getParameters().isEmpty()) {
            method.addParameter(
                    ClassName.get(manifest.packageName() + ".application",
                            "Create%sRequest".formatted(manifest.simpleName())),
                    uncapitalize(manifest.simpleName())
            );
        }
        return method

                .addException(Exception.class)
                .addStatement("return create$L($L)", manifest.simpleName(),
                        !constructor.getParameters().isEmpty() ? uncapitalize(manifest.simpleName()) : "")
                .build();
    }

    private static MethodSpec createMethod(
            ClassManifest manifest,
            ExecutableElement constructor,
            PrefabContext context
    ) {
        var create = constructor.getAnnotation(Create.class);
        var createRequest = uncapitalize(manifest.simpleName());
        var pathVariables = manifest.parent().stream()
                .map(parent -> "%s.%s()".formatted(createRequest, parent.name()))
                .collect(Collectors.joining(", "));
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
                .returns(String.class);
        if (!constructor.getParameters().isEmpty()) {
            method.addParameter(bodyType, createRequest);
        }
        method.addException(Exception.class);
        if (constructor.getParameters().isEmpty()) {
            return withoutRequestBody(manifest, method, create, pathVariables);
        } else if (requestParts.size() == 1) {
            return withRequestBody(manifest, method, create, pathVariables, createRequest);
        } else {
            return withMultipart(manifest, method, create, pathVariables, createRequest, requestParts);
        }
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
                method.addStatement("var $L = $T.mockMultipartFile($L.$L())",
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
                                return ".file(%s)".formatted(part.name());
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

}
