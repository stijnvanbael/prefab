package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.stream.Stream;

import static be.appify.prefab.processor.event.EventPlatformPluginSupport.componentHandlers;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.eventHandlers;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.ownerOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;

/**
 * Prefab plugin to generate Pub/Sub publishers and subscribers based on event annotations.
 */
public class PubSubPlugin implements PrefabPlugin {
    private final PubSubPublisherWriter pubSubPublisherWriter = new PubSubPublisherWriter();
    private final PubSubSubscriberWriter pubSubSubscriberWriter = new PubSubSubscriberWriter();

    /** Constructs a new PubSubPlugin. */
    public PubSubPlugin() {
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates, PrefabContext context) {
        writePublishers(context);
        writeConsumers(aggregates, context);
    }

    private void writeConsumers(List<ClassManifest> aggregates, PrefabContext context) {
        Stream.concat(
                        aggregates.stream().flatMap(aggregate -> eventHandlers(aggregate, context)),
                        componentHandlers(context)
                )
                .filter(method -> isPubSubEvent(context, method))
                .collect(groupingBy(method -> ownerOf(context, method)))
                .forEach((owner, eventHandlers) ->
                        pubSubSubscriberWriter.writePubSubSubscriber(owner, eventHandlers, context));
    }

    private static boolean isPubSubEvent(PrefabContext context, ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class)
                                .stream()
                                .anyMatch(event -> event.platform() == Event.Platform.PUB_SUB));
    }

    private void writePublishers(PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> requireNonNull(e.getAnnotation(Event.class)).platform() == Event.Platform.PUB_SUB)
                .map(element -> new TypeManifest(element.asType(), context.processingEnvironment()))
                .toList();
        events.forEach(event -> pubSubPublisherWriter.writePubSubPublisher(event, context));
    }
}
