package be.appify.prefab.processor.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.ControllerUtil;
import be.appify.prefab.processor.TestUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class CreateTestFixtureWriter {
    public List<MethodSpec> createMethods(ClassManifest manifest, ExecutableElement constructor) {
        return List.of(createMethod(manifest, constructor), whenVariant(manifest), givenVariant(manifest));
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

    private static MethodSpec createMethod(ClassManifest manifest, ExecutableElement constructor) {
        var create = constructor.getAnnotation(Create.class);
        var createRequest = uncapitalize(manifest.simpleName());
        var pathVariables = manifest.parent().stream()
                .map(parent -> "%s.%s()".formatted(createRequest, parent.name()))
                .collect(Collectors.joining(", "));
        return MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(
                        ClassName.get(manifest.packageName() + ".application",
                                "Create%sRequest".formatted(manifest.simpleName())),
                        createRequest
                )
                .addException(Exception.class)
                .addStatement("""
                                var result = mockMvc.perform($T.$N($L)
                                .contentType($T.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString($L)))
                                .andExpect($T.status().isCreated())""",
                        MockMvcRequestBuilders.class,
                        create.method().toLowerCase(),
                        pathVariables.isBlank()
                                ? CodeBlock.of("$S", "/" + ControllerUtil.pathOf(manifest) + create.path())
                                : CodeBlock.of("$S, $L", "/" + ControllerUtil.pathOf(manifest) + create.path(),
                                        pathVariables),
                        MediaType.class,
                        createRequest,
                        MockMvcResultMatchers.class)
                .addStatement("return $T.idOf(result)", TestUtil.class)
                .build();
    }
}
