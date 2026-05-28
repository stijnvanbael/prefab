package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;

import javax.lang.model.element.Modifier;
import java.util.List;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static be.appify.prefab.processor.CaseUtil.toPascalCase;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;

class AutocompleteTestClientWriter {

    List<MethodSpec> autocompleteMethods(ClassManifest manifest) {
        return manifest.fields().stream()
                .map(field -> field.getAnnotation(Autocomplete.class)
                        .map(annotation -> autocompleteMethod(
                                manifest,
                                field.name(),
                                endpointPath(field.name(), annotation.value().path()),
                                annotation.value().security())))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private MethodSpec autocompleteMethod(
            ClassManifest manifest,
            String fieldName,
            String path,
            be.appify.prefab.core.annotations.rest.Security security
    ) {
        var method = MethodSpec.methodBuilder("autocompleteBy" + toPascalCase(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)))
                .addParameter(String.class, "query")
                .addException(Exception.class);

        manifest.parent().ifPresent(parent -> method.addParameter(String.class, parent.name()));
        manifest.parent().ifPresentOrElse(
                parent -> method.addStatement("var request = $T.get($S, $N)$L",
                        MOCK_MVC_REQUEST_BUILDERS,
                        "/" + ControllerUtil.pathOf(manifest) + path,
                        parent.name(),
                        ControllerUtil.withMockUser(security)),
                () -> method.addStatement("var request = $T.get($S)$L",
                        MOCK_MVC_REQUEST_BUILDERS,
                        "/" + ControllerUtil.pathOf(manifest) + path,
                        ControllerUtil.withMockUser(security)));
        method.addCode("""
                if (query != null) {
                    request.queryParam("query", query);
                }
                """);

        var returnType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        return method.addStatement("""
                        var json = mockMvc.perform(request.accept($T.APPLICATION_JSON))
                                .andExpect($T.status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString()""",
                        ClassName.get("org.springframework.http", "MediaType"),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return jsonMapper.readValue(json, new $T() {})",
                        ParameterizedTypeName.get(ClassName.get("tools.jackson.core.type", "TypeReference"), returnType))
                .build();
    }

    private String endpointPath(String fieldName, String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return "/" + toKebabCase(fieldName) + "/autocomplete";
        }
        return configuredPath.startsWith("/") ? configuredPath : "/" + configuredPath;
    }
}

