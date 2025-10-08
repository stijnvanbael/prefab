package be.appify.prefab.processor.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TestUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpdateTestFixtureWriter {
    public MethodSpec updateMethod(
            ClassManifest manifest,
            UpdateManifest update,
            PrefabContext context
    ) {
        var pathVariables = manifest.parent().stream()
                .map(parent -> "%sId".formatted(uncapitalize(parent.name())))
                .collect(Collectors.joining(", "));
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(String.class, "id");
        var bodyType = ClassName.get(
                manifest.packageName() + ".application",
                "%s%sRequest".formatted(
                        manifest.simpleName(),
                        capitalize(update.operationName())));
        var requestParts = Stream.concat(update.parameters().stream()
                        .flatMap(parameter -> context.requestParameterBuilder()
                                .buildMethodParameter(parameter)
                                .stream()),
                !update.parameters().isEmpty()
                        ? Stream.of(ParameterSpec.builder(bodyType, "request").build())
                        : Stream.empty()
        ).toList();
        if (!pathVariables.isBlank()) {
            method.addParameters(manifest.parent().stream()
                    .map(parent -> ParameterSpec.builder(String.class,
                            "%sId".formatted(uncapitalize(parent.name()))).build())
                    .collect(Collectors.toList()));
        }
        if (!update.parameters().isEmpty()) {
            method.addParameter(bodyType, "request");
        }
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
                                mockMvc.perform($T.$N($L, id)
                                        .contentType($T.APPLICATION_JSON)
                                        $L
                                        .andExpect($T.status().isOk())""",
                        MockMvcRequestBuilders.class,
                        update.method().toLowerCase(),
                        pathVariables(manifest, update, pathVariables),
                        MediaType.class,
                        update.parameters().isEmpty() ? ")" : ".content(objectMapper.writeValueAsString(request)))",
                        MockMvcResultMatchers.class)
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
                method.addStatement("var $L = $T.mockMultipartFile(request.$L())",
                        part.name(),
                        TestUtil.class,
                        part.name());
            } else {
                method.addStatement(
                        "var bodyPart = new $T($S, null, objectMapper.writeValueAsBytes(request), $T.APPLICATION_JSON)",
                        MockPart.class,
                        "body",
                        MediaType.class);
            }
        });
        return method.addStatement("""
                                return mockMvc.perform($T.multipart($L, id)
                                        $L
                                        ).andExpect($T.status().isOk())""",
                        MockMvcRequestBuilders.class,
                        pathVariables(manifest, update, pathVariables),
                        requestParts.stream()
                                .map(part -> {
                                    if (part.type().equals(ClassName.get(MultipartFile.class))) {
                                        return CodeBlock.of(".file($L)", part.name());
                                    } else {
                                        return CodeBlock.of(".part(bodyPart)");
                                    }
                                })
                                .reduce(CodeBlock.builder(), CodeBlock.Builder::add, (a, b) -> a)
                                .build(),
                        MockMvcResultMatchers.class)
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
