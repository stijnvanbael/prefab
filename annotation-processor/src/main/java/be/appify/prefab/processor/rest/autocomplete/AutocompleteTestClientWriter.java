package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.data.domain.Pageable;

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
                        .map(annotation -> autocompleteMethods(
                                manifest,
                                field.name(),
                                endpointPath(field.name(), annotation.value().path()),
                                annotation.value().security())))
                .flatMap(java.util.Optional::stream)
                .flatMap(List::stream)
                .toList();
    }

    private List<MethodSpec> autocompleteMethods(
            ClassManifest manifest,
            String fieldName,
            String path,
            be.appify.prefab.core.annotations.rest.Security security
    ) {
        var methodName = "autocompleteBy" + toPascalCase(fieldName);
        var returnType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));

        var pagedMethod = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(String.class, "query")
                .addParameter(Pageable.class, "pageable")
                .addException(Exception.class);

        manifest.parent().ifPresent(parent -> pagedMethod.addParameter(String.class, parent.name()));
        manifest.parent().ifPresentOrElse(
                parent -> pagedMethod.addStatement("var request = $T.get($S, $N)$L",
                        MOCK_MVC_REQUEST_BUILDERS,
                        "/" + ControllerUtil.pathOf(manifest) + path,
                        parent.name(),
                        ControllerUtil.withMockUser(security)),
                () -> pagedMethod.addStatement("var request = $T.get($S)$L",
                        MOCK_MVC_REQUEST_BUILDERS,
                        "/" + ControllerUtil.pathOf(manifest) + path,
                        ControllerUtil.withMockUser(security)));
        pagedMethod.addCode("""
                if (query != null) {
                    request.queryParam("query", query);
                }
                if (pageable != null && pageable.isPaged()) {
                    request.queryParam("page", String.valueOf(pageable.getPageNumber()))
                           .queryParam("limit", String.valueOf(pageable.getPageSize()));
                }
                """);

        var unpagedMethod = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(String.class, "query")
                .addException(Exception.class);
        manifest.parent().ifPresent(parent -> unpagedMethod.addParameter(String.class, parent.name()));
        var unpagedArguments = manifest.parent()
                .map(parent -> "query, null, " + parent.name())
                .orElse("query, null");
        unpagedMethod.addStatement("return $N($L)", methodName, unpagedArguments);

        return List.of(
                unpagedMethod.build(),
                pagedMethod.addStatement("""
                                var json = mockMvc.perform(request.accept($T.APPLICATION_JSON))
                                        .andExpect($T.status().isOk())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString()""",
                                ClassName.get("org.springframework.http", "MediaType"),
                                MOCK_MVC_RESULT_MATCHERS)
                        .addStatement("return jsonMapper.readValue(json, new $T() {})",
                                ParameterizedTypeName.get(ClassName.get("tools.jackson.core.type", "TypeReference"), returnType))
                        .build());
    }

    private String endpointPath(String fieldName, String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return "/" + toKebabCase(fieldName) + "/autocomplete";
        }
        return configuredPath.startsWith("/") ? configuredPath : "/" + configuredPath;
    }
}
