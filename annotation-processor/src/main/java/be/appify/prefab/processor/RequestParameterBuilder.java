package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Example;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static be.appify.prefab.processor.rest.ControllerUtil.OPENAPI_INCLUDED;

/**
 * Builder class for creating request parameters using plugins and default rules.
 */
public class RequestParameterBuilder {
    private final List<PrefabPlugin> plugins;

    /**
     * Constructs a RequestParameterBuilder with the given plugins.
     *
     * @param plugins the list of Prefab plugins
     */
    public RequestParameterBuilder(List<PrefabPlugin> plugins) {
        this.plugins = plugins;
    }

    /**
     * Builds a body parameter for the given variable manifest.
     *
     * @param parameter the variable manifest
     * @return an optional ParameterSpec representing the body parameter
     */
    public Optional<ParameterSpec> buildBodyParameter(VariableManifest parameter) {
        return plugins.stream().flatMap(plugin -> plugin.requestBodyParameter(parameter).stream())
                .findFirst()
                .or(() -> defaultParameters(parameter));
    }

    /**
     * Builds a method parameter for the given variable manifest.
     *
     * @param parameter the variable manifest
     * @return an optional ParameterSpec representing the method parameter
     */
    public Optional<ParameterSpec> buildMethodParameter(VariableManifest parameter) {
        return plugins.stream().flatMap(plugin -> plugin.requestMethodParameter(parameter).stream())
                .findFirst();
    }

    private static Optional<ParameterSpec> defaultParameters(VariableManifest parameter) {
        var effectiveType = parameter.type().isSingleValueType()
                ? parameter.type().fields().getFirst().type().asBoxed()
                : parameter.type();
        var builder = ParameterSpec.builder(effectiveType.asTypeName(), parameter.name());
        var annotations = new ArrayList<>(
                parameter.annotations().stream().map(AnnotationManifest::asSpec).toList());
        if (!effectiveType.isStandardType() && !effectiveType.isEnum()) {
            annotations.add(AnnotationSpec.builder(Valid.class).build());
        } else if (effectiveType.is(String.class) && parameter.annotations().stream()
                .noneMatch(a -> a.type().is(Size.class))) {
            annotations.add(AnnotationSpec.builder(Size.class).addMember("max", "255").build());
        }
        if (OPENAPI_INCLUDED) {
            parameter.getAnnotation(Example.class).ifPresent(exampleManifest -> {
                var schemaClass = ClassName.get("io.swagger.v3.oas.annotations.media", "Schema");
                annotations.add(AnnotationSpec.builder(schemaClass)
                        .addMember("example", "$S", exampleManifest.value().value())
                        .build());
            });
        }
        return Optional.of(builder.addAnnotations(annotations).build());
    }
}
