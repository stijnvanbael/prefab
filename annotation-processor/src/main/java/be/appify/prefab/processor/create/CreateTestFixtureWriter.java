package be.appify.prefab.processor.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TestUtil;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.uncapitalize;

class CreateTestFixtureWriter {
    List<MethodSpec> createMethods(ClassManifest manifest, ExecutableElement constructor,
            PrefabContext context) {
        return List.of(createMethod(manifest, constructor, context), whenVariant(manifest), givenVariant(manifest));
    }

    private MethodSpec whenVariant(ClassManifest manifest) {
        return variant(manifest, "whenCreating" + manifest.simpleName());
    }

    private MethodSpec givenVariant(ClassManifest manifest) {
        return variant(manifest, "given" + manifest.simpleName() + "Created");
    }

    private static MethodSpec variant(ClassManifest manifest, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(
                        ClassName.get(manifest.packageName() + ".application",
                                "Create%sRequest".formatted(manifest.simpleName())),
                        uncapitalize(manifest.simpleName())
                )
                .addException(Exception.class)
                .addStatement("return create$L($L)", manifest.simpleName(), uncapitalize(manifest.simpleName()))
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
                                .buildMethodParameter(new VariableManifest(parameter, context.processingEnvironment()))
                                .stream()),
                Stream.of(ParameterSpec.builder(bodyType, createRequest).build())
        ).toList();
        var method = MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(bodyType, createRequest);
        method.addException(Exception.class);
        if (requestParts.size() == 1) {
            return withRequestBody(manifest, method, create, pathVariables, createRequest);
        } else {
            return withMultipart(manifest, method, create, pathVariables, createRequest, requestParts);
        }
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
                                .content(objectMapper.writeValueAsString($L)))
                                .andExpect($T.status().isCreated())""",
                        MockMvcRequestBuilders.class,
                        create.method().toLowerCase(),
                        pathVariables(manifest, create, pathVariables),
                        ControllerUtil.withMockUser(create.security()),
                        MediaType.class,
                        createRequest,
                        MockMvcResultMatchers.class)
                .addStatement("return $T.idOf(result)", TestUtil.class)
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
                        TestUtil.class,
                        createRequest,
                        part.name());
            } else {
                method.addStatement(
                        "var bodyPart = new $T($S, null, objectMapper.writeValueAsBytes($L), $T.APPLICATION_JSON)",
                        MockPart.class,
                        "body",
                        createRequest,
                        MediaType.class);
            }
        });
        return method.addStatement("""
                                var result = mockMvc.perform($T.multipart($L)
                                    $L
                                ).andExpect($T.status().isCreated())""",
                        MockMvcRequestBuilders.class,
                        pathVariables(manifest, create, pathVariables),
                        requestParts.stream().map(part -> {
                            if (part.type().equals(ClassName.get(MultipartFile.class))) {
                                return ".file(%s)".formatted(part.name());
                            } else {
                                return ".part(bodyPart)";
                            }
                        }).collect(Collectors.joining("\n")),
                        MockMvcResultMatchers.class)
                .addStatement("return $T.idOf(result)", TestUtil.class)
                .build();
    }

    private static CodeBlock pathVariables(ClassManifest manifest, Create create, String pathVariables) {
        return pathVariables.isBlank()
                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + create.path())
                : CodeBlock.of("$S, $L", "/" + ControllerUtil.pathOf(manifest) + create.path(),
                        pathVariables);
    }

}
