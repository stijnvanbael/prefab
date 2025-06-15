package be.appify.prefab.processor;

import be.appify.prefab.core.service.Reference;
import com.palantir.javapoet.CodeBlock;

import java.util.List;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class RequestParameterMapper {
    private final List<PrefabPlugin> plugins;

    public RequestParameterMapper(List<PrefabPlugin> plugins) {
        this.plugins = plugins;
    }

    public CodeBlock mapRequestParameter(VariableManifest parameter) {
        return plugins.stream()
                .flatMap(plugin -> plugin.mapRequestParameter(parameter).stream())
                .findFirst()
                .orElseGet(() -> defaultMapping(parameter));
    }

    private CodeBlock defaultMapping(VariableManifest parameter) {
        if (parameter.type().is(Reference.class)) {
            var type = uncapitalize(parameter.type().parameters().getFirst().simpleName());
            return CodeBlock.of("toReference($S, $N, request.$N())",
                    type, type + "Repository", parameter.name());
        }
        return CodeBlock.of("request.$N()", parameter.name());
    }
}
