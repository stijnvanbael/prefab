package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.CodeBlock;

import java.util.List;

/**
 * Mapper class for converting request parameters using plugins and default rules.
 */
public class RequestParameterMapper {
    private final List<PrefabPlugin> plugins;

    /**
     * Constructs a RequestParameterMapper with the given plugins.
     *
     * @param plugins the list of Prefab plugins
     */
    public RequestParameterMapper(List<PrefabPlugin> plugins) {
        this.plugins = plugins;
    }

    /**
     * Maps a request parameter for the given variable manifest.
     *
     * @param parameter the variable manifest
     * @return a CodeBlock representing the mapped request parameter
     */
    public CodeBlock mapRequestParameter(VariableManifest parameter) {
        return plugins.stream()
                .flatMap(plugin -> plugin.mapRequestParameter(parameter).stream())
                .findFirst()
                .orElseGet(() -> defaultMapping(parameter));
    }

    private CodeBlock defaultMapping(VariableManifest parameter) {
        if (parameter.type().is(Reference.class)) {
            var type = parameter.type().parameters().getFirst().asTypeName();
            return CodeBlock.of("referenceFactory.referenceTo($T.class, request.$N())", type, parameter.name());
        }
        return CodeBlock.of("request.$N()", parameter.name());
    }
}
