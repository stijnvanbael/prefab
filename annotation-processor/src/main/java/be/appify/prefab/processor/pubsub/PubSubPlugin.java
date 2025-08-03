package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.StreamUtil;
import be.appify.prefab.processor.TypeManifest;
import org.springframework.stereotype.Component;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.stream.Stream;

public class PubSubPlugin implements PrefabPlugin {
    private final PubSubPublisherWriter pubSubPublisherWriter = new PubSubPublisherWriter();
    private final PubSubConsumerWriter pubSubConsumerWriter = new PubSubConsumerWriter();

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates, PrefabContext context) {
        writePublishers(context);
        writeConsumers(aggregates, context);
    }

    private void writeConsumers(List<ClassManifest> aggregates, PrefabContext context) {
        Stream.concat(
                        aggregates.stream().flatMap(aggregate -> pubSubEventHandlers(aggregate, context)),
                        componentHandlers(context)
                )
                .filter(method -> isPubSubEvent(context, method))
                .forEach(eventHandler -> pubSubConsumerWriter.writePubSubConsumer(eventHandler, context));
    }

    private static boolean isPubSubEvent(PrefabContext context, ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment()).annotationsOfType(Event.class).stream()
                                .anyMatch(event -> event.platform() == Event.Platform.PUB_SUB));
    }

    private void writePublishers(PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> e.getAnnotation(Event.class).platform() == Event.Platform.PUB_SUB)
                .filter(element -> element.getKind().isClass() && !element.getModifiers().contains(Modifier.ABSTRACT))
                .map(element -> new ClassManifest((TypeElement) element, context.processingEnvironment()))
                .toList();
        events.forEach(event -> pubSubPublisherWriter.writePubSubPublisher(event, context));
    }

    private Stream<ExecutableElement> pubSubEventHandlers(ClassManifest aggregate, PrefabContext context) {
        return StreamUtil.concat(
                aggregate.methodsWith(EventHandler.class).stream(),
                aggregate.methodsWith(EventHandler.ByReference.class).stream(),
                aggregate.methodsWith(EventHandler.Broadcast.class).stream()
        );
    }

    private static Stream<ExecutableElement> componentHandlers(PrefabContext context) {
        return context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getEnclosingElement().getAnnotation(Component.class) != null)
                .map(element -> (ExecutableElement) element);
    }
}
