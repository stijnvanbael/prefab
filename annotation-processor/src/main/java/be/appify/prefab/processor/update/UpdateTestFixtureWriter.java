package be.appify.prefab.processor.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import javax.lang.model.element.Modifier;
import java.util.stream.Collectors;

public class UpdateTestFixtureWriter {
    public MethodSpec updateMethods(ClassManifest manifest, UpdateManifest update) {
        var pathVariables = manifest.parent().stream()
                .map(parent -> "%sId".formatted(uncapitalize(parent.name())))
                .collect(Collectors.joining(", "));
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
        if (!update.parameters().isEmpty()) {
            method.addParameter(ClassName.get(
                            manifest.packageName() + ".application",
                            "%s%sRequest".formatted(
                                    manifest.simpleName(),
                                    capitalize(update.operationName()))),
                    "request");
        }
        return method.addException(Exception.class)
                .addStatement("""
                                mockMvc.perform($T.$N($L, id)
                                        .contentType($T.APPLICATION_JSON)
                                        $L
                                        .andExpect($T.status().isOk())""",
                        MockMvcRequestBuilders.class,
                        update.method().toLowerCase(),
                        pathVariables.isBlank()
                                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + "/{id}" + update.path())
                                : CodeBlock.of("$S, $L",
                                        "/" + ControllerUtil.pathOf(manifest) + "/{id}" + update.path(),
                                        pathVariables),
                        MediaType.class,
                        update.parameters().isEmpty() ? ")" : ".content(objectMapper.writeValueAsString(request)))",
                        MockMvcResultMatchers.class)
                .build();
    }
}
