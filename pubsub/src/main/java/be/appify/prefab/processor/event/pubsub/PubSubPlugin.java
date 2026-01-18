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
    private final PubSubPublisherWriter pubSubPublisherWriter = new PubSubPublisherWriter();
    private final PubSubSubscriberWriter pubSubSubscriberWriter = new PubSubSubscriberWriter();

    /** Constructs a new PubSubPlugin. */
    public PubSubPlugin() {
        setDerivedPlatform(Event.Platform.PUB_SUB);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates, PrefabContext context) {
        writePublishers(context);
        writeConsumers(context);
    }

    private void writeConsumers(PrefabContext context) {
        filteredEventHandlersByOwner(context, PubSubPlugin::isPubSubEvent)
                .forEach((owner, eventHandlers) ->
                        pubSubSubscriberWriter.writePubSubSubscriber(owner, eventHandlers, context));
    }

    private static boolean isPubSubEvent(ExecutableElement method, PrefabContext context) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class)
                                .stream()
                                .anyMatch(event -> platformIsPubSub(event, method, context)));
    }

    private void writePublishers(PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> platformIsPubSub(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .map(element -> new TypeManifest(element.asType(), context.processingEnvironment()))
                .toList();
        events.forEach(event -> pubSubPublisherWriter.writePubSubPublisher(event, context));
    }

    public static boolean platformIsPubSub(Event event, Element element, PrefabContext context) {
        if (event.platform() == Event.Platform.DERIVED && isMultiplePlatformsDetected()) {
            context.logError(
                    "Cannot derive platform for event [%s] because multiple messaging platforms are configured. Please specify the platform explicitly."
                            .formatted(element.getSimpleName()), element);
        }
        return event.platform() == Event.Platform.PUB_SUB ||
                event.platform() == Event.Platform.DERIVED && derivedPlatform() == Event.Platform.PUB_SUB;
    }
}
