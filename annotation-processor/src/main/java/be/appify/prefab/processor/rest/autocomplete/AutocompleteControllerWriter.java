package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;

import javax.lang.model.element.Modifier;
import java.util.List;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static be.appify.prefab.processor.CaseUtil.toPascalCase;
import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;

class AutocompleteControllerWriter {

    List<MethodSpec> autocompleteMethods(ClassManifest manifest) {
        return manifest.fields().stream()
                .map(field -> field.getAnnotation(Autocomplete.class)
                        .map(annotation -> autocompleteMethod(
                                manifest.simpleName(),
                                field.name(),
                                endpointPath(field.name(), annotation.value().path()),
                                annotation.value().security())))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private MethodSpec autocompleteMethod(
            String aggregateName,
            String fieldName,
            String path,
            be.appify.prefab.core.annotations.rest.Security security
    ) {
        var methodName = "autocompleteBy" + toPascalCase(fieldName);
        var responseType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(requestMapping("GET", path))
                .returns(ParameterizedTypeName.get(ClassName.get("org.springframework.http", "ResponseEntity"), responseType))
                .addParameter(com.palantir.javapoet.ParameterSpec.builder(String.class, "query")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestParam"))
                        .build())
                .addStatement("return $T.ok(service.$N(query))",
                        ClassName.get("org.springframework.http", "ResponseEntity"), methodName);

        operationAnnotation("Autocomplete " + aggregateName + " " + fieldName).ifPresent(method::addAnnotation);
        securedAnnotation(security).ifPresent(method::addAnnotation);
        return method.build();
    }

    private String endpointPath(String fieldName, String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return "/" + toKebabCase(fieldName) + "/autocomplete";
        }
        return configuredPath.startsWith("/") ? configuredPath : "/" + configuredPath;
    }
}

