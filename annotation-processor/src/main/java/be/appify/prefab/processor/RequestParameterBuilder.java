package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.Text;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
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

    /**
     * Builds a plain parameter for use in test client methods, without validation or documentation annotations.
     *
     * @param parameter the variable manifest
     * @return an optional ParameterSpec with no annotations
     */
    public Optional<ParameterSpec> buildTestClientParameter(VariableManifest parameter) {
        return plugins.stream().flatMap(plugin -> plugin.requestBodyParameter(parameter).stream())
                .findFirst()
                .map(spec -> ParameterSpec.builder(spec.type(), spec.name()).build())
                .or(() -> plainParameter(parameter));
    }

    private static Optional<ParameterSpec> plainParameter(VariableManifest parameter) {
        var effectiveType = parameter.type().isSingleValueType()
                ? parameter.type().fields().getFirst().type().asBoxed()
                : parameter.type();
        return Optional.of(ParameterSpec.builder(effectiveType.asTypeName(), parameter.name()).build());
    }

    /**
     * {@code @Text} cannot be placed on parameters, so a parameter inherits it from the field of the same name
     * in the declaring type; such a column is unbounded and must not get the default length limit.
     */
    private static boolean initialisesTextField(VariableManifest parameter) {
        return parameter.element().getEnclosingElement() instanceof ExecutableElement executable
                && executable.getEnclosingElement() instanceof TypeElement type
                && type.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.RECORD_COMPONENT)
                .filter(e -> e.getSimpleName().contentEquals(parameter.name()))
                .anyMatch(e -> e.getAnnotation(Text.class) != null);
    }

    private static Optional<ParameterSpec> defaultParameters(VariableManifest parameter) {
        var effectiveType = parameter.type().isSingleValueType()
                ? parameter.type().fields().getFirst().type().asBoxed()
                : parameter.type();
        var builder = ParameterSpec.builder(effectiveType.asTypeName(), parameter.name());
        var annotations = new ArrayList<>(
                parameter.annotations().stream().map(AnnotationManifest::asSpec).toList());
        var alreadyValidated = parameter.annotations().stream().anyMatch(a -> a.type().is(Valid.class));
        if (!effectiveType.isStandardType() && !effectiveType.isEnum() && !alreadyValidated) {
            annotations.add(AnnotationSpec.builder(Valid.class).build());
        } else if (effectiveType.is(String.class) && parameter.annotations().stream()
                .noneMatch(a -> a.type().is(Size.class)) && !initialisesTextField(parameter)) {
            annotations.add(AnnotationSpec.builder(Size.class).addMember("max", "255").build());
        }
        if (OPENAPI_INCLUDED) {
            var exampleAnnotation = parameter.getAnnotation(Example.class);
            var docAnnotation = parameter.getAnnotation(Doc.class);
            if (exampleAnnotation.isPresent() || docAnnotation.isPresent()) {
                var schemaClass = ClassName.get("io.swagger.v3.oas.annotations.media", "Schema");
                var schemaBuilder = AnnotationSpec.builder(schemaClass);
                exampleAnnotation.ifPresent(e -> schemaBuilder.addMember("example", "$S", e.value().value()));
                docAnnotation.ifPresent(d -> schemaBuilder.addMember("description", "$S", d.value().value()));
                annotations.add(schemaBuilder.build());
            }
        }
        return Optional.of(builder.addAnnotations(annotations).build());
    }
}
