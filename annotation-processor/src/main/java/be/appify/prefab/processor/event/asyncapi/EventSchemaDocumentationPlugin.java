package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

/**
 * A plugin that generates AsyncAPI documentation for all consumed and published events.
 * The documentation is written as AsyncAPI 2.6.0 JSON to {@code META-INF/async-api/asyncapi.json}.
 */
public class EventSchemaDocumentationPlugin implements PrefabPlugin {
    private EventSchemaDocumentationWriter writer;
    private PrefabContext context;

    /** Constructs a new EventSchemaDocumentationPlugin. */
    public EventSchemaDocumentationPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        writer = new EventSchemaDocumentationWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        var events = context.eventElements()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        if (!events.isEmpty()) {
            Set<TypeManifest> consumedEventTypes = consumedEventTypes();
            writer.writeDocumentation(events, consumedEventTypes);
        }
    }

    private Set<TypeManifest> consumedEventTypes() {
        return context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class)
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(element -> (ExecutableElement) element)
                .flatMap(method -> method.getParameters().stream())
                .map(param -> TypeManifest.of(param.asType(), context.processingEnvironment()))
                .filter(type -> !type.inheritedAnnotationsOfType(Event.class).isEmpty())
                .collect(Collectors.toSet());
    }
}
