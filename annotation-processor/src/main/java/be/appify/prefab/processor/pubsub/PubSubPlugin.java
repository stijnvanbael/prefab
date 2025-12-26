package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.StreamUtil;
import be.appify.prefab.processor.TypeManifest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;

public class PubSubPlugin implements PrefabPlugin {
    private final PubSubPublisherWriter pubSubPublisherWriter = new PubSubPublisherWriter();
    private final PubSubSubscriberWriter pubSubSubscriberWriter = new PubSubSubscriberWriter();

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates, PrefabContext context) {
        writePublishers(context);
        writeConsumers(aggregates, context);
    }

    private void writeConsumers(List<ClassManifest> aggregates, PrefabContext context) {
        Stream.concat(
                        aggregates.stream().flatMap(this::pubSubEventHandlers),
                        componentHandlers(context)
                )
                .filter(method -> isPubSubEvent(context, method))
                .collect(groupingBy(method -> topicAndOwnerOf(context, method)))
                .forEach((topicAndOwner, eventHandlers) ->
                        pubSubSubscriberWriter.writePubSubSubscriber(topicAndOwner.getLeft(), topicAndOwner.getRight(),
                                eventHandlers, context));
    }

    private Pair<String, TypeManifest> topicAndOwnerOf(PrefabContext context, ExecutableElement method) {
        return method.getParameters().stream()
                .flatMap(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class).stream()
                                .findFirst()
                                .map(event -> Pair.of(event.topic(),
                                        new TypeManifest(method.getEnclosingElement().asType(),
                                                context.processingEnvironment())))
                                .stream())
                .findFirst()
                .orElseThrow();
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

    private Stream<ExecutableElement> pubSubEventHandlers(ClassManifest aggregate) {
        return StreamUtil.concat(
                aggregate.methodsWith(EventHandler.class).stream(),
                aggregate.methodsWith(EventHandler.ByReference.class).stream(),
                aggregate.methodsWith(EventHandler.Broadcast.class).stream(),
                aggregate.methodsWith(EventHandler.Multicast.class).stream()
                // TODO: make this pluggable so other plugins can add their own handlers
        );
    }

    private static Stream<ExecutableElement> componentHandlers(PrefabContext context) {
        return context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getEnclosingElement().getAnnotation(Component.class) != null)
                .map(element -> (ExecutableElement) element);
    }
}
