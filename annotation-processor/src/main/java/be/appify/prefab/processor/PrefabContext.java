package be.appify.prefab.processor;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.List;

public class PrefabContext {
    private final ProcessingEnvironment processingEnvironment;
    private final RequestParameterBuilder requestParameterBuilder;
    private final List<PrefabPlugin> plugins;
    private final RequestParameterMapper requestParameterMapper;

    public PrefabContext(ProcessingEnvironment processingEnvironment, List<PrefabPlugin> plugins) {
        this.processingEnvironment = processingEnvironment;
        this.plugins = plugins;
        requestParameterBuilder = new RequestParameterBuilder(plugins);
        requestParameterMapper = new RequestParameterMapper(plugins);
    }

    public RequestParameterBuilder requestParameterBuilder() {
        return requestParameterBuilder;
    }

    public ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    public List<PrefabPlugin> plugins() {
        return plugins;
    }

    public RequestParameterMapper requestParameterMapper() {
        return requestParameterMapper;
    }
}
