package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import static be.appify.prefab.processor.event.EventPlatformPluginSupport.derivedPlatform;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.filteredEventHandlersByOwner;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isMultiplePlatformsDetected;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.setDerivedPlatform;
import static java.util.Objects.requireNonNull;

/**
 * Prefab plugin to generate Pub/Sub publishers and subscribers based on event annotations.
 */
public class PubSubPlugin implements PrefabPlugin {
    private PubSubPublisherWriter pubSubPublisherWriter;
    private PubSubSubscriberWriter pubSubSubscriberWriter;
    private PrefabContext context;

    /** Constructs a new PubSubPlugin. */
    public PubSubPlugin() {
        setDerivedPlatform(Event.Platform.PUB_SUB);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates) {
        writePublishers();
        writeConsumers();
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        pubSubPublisherWriter = new PubSubPublisherWriter(context);
        pubSubSubscriberWriter = new PubSubSubscriberWriter(context);
    }

    private void writeConsumers() {
        filteredEventHandlersByOwner(context, this::isPubSubEvent)
                .forEach((owner, eventHandlers) ->
                        pubSubSubscriberWriter.writePubSubSubscriber(owner, eventHandlers));
    }

    private boolean isPubSubEvent(ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        TypeManifest.of(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class)
                                .stream()
                                .anyMatch(event -> platformIsPubSub(event, method, context)));
    }

    private void writePublishers() {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> platformIsPubSub(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        events.forEach(event -> pubSubPublisherWriter.writePubSubPublisher(event));
    }

    static boolean platformIsPubSub(Event event, Element element, PrefabContext context) {
        if (event.platform() == Event.Platform.DERIVED && isMultiplePlatformsDetected()) {
            context.logError(
                    "Cannot derive platform for event [%s] because multiple messaging platforms are configured. Please specify the platform explicitly."
                            .formatted(element.getSimpleName()), element);
        }
        return event.platform() == Event.Platform.PUB_SUB ||
                event.platform() == Event.Platform.DERIVED && derivedPlatform() == Event.Platform.PUB_SUB;
    }
}
