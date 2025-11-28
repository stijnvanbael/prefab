package be.appify.prefab.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.List;

public class PrefabContext {
    private final ProcessingEnvironment processingEnvironment;
    private final RequestParameterBuilder requestParameterBuilder;
    private final List<PrefabPlugin> plugins;
    private final RequestParameterMapper requestParameterMapper;
    private final RoundEnvironment roundEnvironment;

    public PrefabContext(
            ProcessingEnvironment processingEnvironment,
            List<PrefabPlugin> plugins,
            RoundEnvironment roundEnvironment
    ) {
        this.processingEnvironment = processingEnvironment;
        this.plugins = plugins;
        this.roundEnvironment = roundEnvironment;
        requestParameterBuilder = new RequestParameterBuilder(plugins);
        requestParameterMapper = new RequestParameterMapper(plugins);
    }

    public RequestParameterBuilder requestParameterBuilder() {
        return requestParameterBuilder;
    }

    public ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    public RoundEnvironment roundEnvironment() {
        return roundEnvironment;
    }

    public List<PrefabPlugin> plugins() {
        return plugins;
    }

    public RequestParameterMapper requestParameterMapper() {
        return requestParameterMapper;
    }

    public void logError(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
