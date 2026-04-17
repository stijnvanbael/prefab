package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.ConsumerWriterSupport;
import java.util.ArrayList;
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
    private static final ConsumerWriterSupport CONSUMER_WRITER_SUPPORT = new ConsumerWriterSupport(Event.Platform.KAFKA);

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
        var rawEvents = context.eventElements()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        if (rawEvents.isEmpty()) {
            return;
        }
        var resolvedEvents = resolveAvscEvents(rawEvents);
        if (resolvedEvents == null) {
            return;
        }
        writer.writeDocumentation(resolvedEvents, consumedEventTypes());
    }

    /**
     * Expands {@code @Avsc}-annotated interface events into their generated concrete record types.
     * AVSC-generated records are included directly.
     * Returns {@code null} when any AVSC interface still lacks concrete records (round 1), signalling
     * that documentation generation should be deferred to the next processing round.
     */
    private List<TypeManifest> resolveAvscEvents(List<TypeManifest> rawEvents) {
        var resolved = new ArrayList<TypeManifest>();
        for (var event : rawEvents) {
            if (isAvscInterface(event)) {
                var concreteTypes = CONSUMER_WRITER_SUPPORT.concreteEventTypes(event, context);
                if (concreteTypes.size() == 1 && concreteTypes.getFirst() == event) {
                    return null;
                }
                concreteTypes.forEach(resolved::add);
            } else {
                resolved.add(event);
            }
        }
        return resolved;
    }

    private static boolean isAvscInterface(TypeManifest event) {
        return event.asElement() != null && event.asElement().getAnnotation(Avsc.class) != null;
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
