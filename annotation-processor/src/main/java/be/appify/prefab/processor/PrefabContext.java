package be.appify.prefab.processor;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Context class providing access to processing environment, plugins, and utilities during annotation processing.
 */
public class PrefabContext {
    private final ProcessingEnvironment processingEnvironment;
    private final RequestParameterBuilder requestParameterBuilder;
    private final List<PrefabPlugin> plugins;
    private final RequestParameterMapper requestParameterMapper;
    private final RoundEnvironment roundEnvironment;

    /**
     * Constructs a PrefabContext.
     *
     * @param processingEnvironment
     *         the processing environment
     * @param plugins
     *         the list of Prefab plugins
     * @param roundEnvironment
     *         the round environment
     */
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

    /**
     * Gets the RequestParameterBuilder.
     *
     * @return the RequestParameterBuilder
     */
    public RequestParameterBuilder requestParameterBuilder() {
        return requestParameterBuilder;
    }

    /**
     * Gets the ProcessingEnvironment.
     *
     * @return the ProcessingEnvironment
     */
    public ProcessingEnvironment processingEnvironment() {
        return processingEnvironment;
    }

    /**
     * Gets the RoundEnvironment.
     *
     * @return the RoundEnvironment
     */
    public RoundEnvironment roundEnvironment() {
        return roundEnvironment;
    }

    /**
     * Gets the list of Prefab plugins.
     *
     * @return the list of Prefab plugins
     */
    public List<PrefabPlugin> plugins() {
        return plugins;
    }

    /**
     * Gets the RequestParameterMapper.
     *
     * @return the RequestParameterMapper
     */
    public RequestParameterMapper requestParameterMapper() {
        return requestParameterMapper;
    }

    /**
     * Logs an error message associated with a specific element.
     *
     * @param message
     *         the error message
     * @param element
     *         the element associated with the error
     */
    public void logError(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Logs a note message associated with a specific element.
     *
     * @param message
     *         the note message
     * @param element
     *         the element associated with the note
     */
    public void logNote(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }
}
