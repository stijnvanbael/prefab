package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RequestParameterBuilder {
    private final List<PrefabPlugin> plugins;

    public RequestParameterBuilder(List<PrefabPlugin> plugins) {
        this.plugins = plugins;
    }

    public Optional<ParameterSpec> buildBodyParameter(VariableManifest parameter) {
        return plugins.stream().flatMap(plugin -> plugin.requestBodyParameter(parameter).stream())
                .findFirst()
                .or(() -> defaultParameters(parameter));
    }

    public Optional<ParameterSpec> buildMethodParameter(VariableManifest parameter) {
        return plugins.stream().flatMap(plugin -> plugin.requestMethodParameter(parameter).stream())
                .findFirst();
    }

    private static Optional<ParameterSpec> defaultParameters(VariableManifest parameter) {
        var builder = parameter.type().is(Reference.class)
                ? ParameterSpec.builder(String.class, parameter.name())
                : ParameterSpec.builder(parameter.type().asTypeName(), parameter.name());
        var annotations = new ArrayList<>(
                parameter.annotations().stream().map(AnnotationManifest::asSpec).toList());
        if (!parameter.type().isStandardType() && !parameter.type().isEnum()) {
            annotations.add(AnnotationSpec.builder(Valid.class).build());
        } else if (parameter.type().is(String.class) && parameter.annotations().stream()
                .noneMatch(a -> a.type().is(Size.class))) {
            annotations.add(AnnotationSpec.builder(Size.class).addMember("max", "255").build());
        }
        return Optional.of(builder.addAnnotations(annotations).build());
    }
}
