package be.appify.prefab.processor.kafka;

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

public class KafkaPlugin implements PrefabPlugin {
    private final KafkaPublisherWriter kafkaPublisherWriter = new KafkaPublisherWriter();
    private final KafkaConsumerWriter kafkaConsumerWriter = new KafkaConsumerWriter();

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates, PrefabContext context) {
        writePublishers(context);
        writeConsumers(aggregates, context);
    }

    private void writeConsumers(List<ClassManifest> aggregates, PrefabContext context) {
        Stream.concat(
                        aggregates.stream().flatMap(aggregate -> kafkaEventHandlers(aggregate, context)),
                        componentHandlers(context)
                )
                .filter(method -> isKafkaEvent(context, method))
                .forEach(eventHandler -> kafkaConsumerWriter.writeKafkaConsumer(eventHandler, context));
    }

    private static boolean isKafkaEvent(PrefabContext context, ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment()).annotationsOfType(Event.class).stream()
                                .anyMatch(event -> event.platform() == Event.Platform.KAFKA));
    }

    private void writePublishers(PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> e.getAnnotation(Event.class).platform() == Event.Platform.KAFKA)
                .filter(element -> element.getKind().isClass() && !element.getModifiers().contains(Modifier.ABSTRACT))
                .map(element -> new ClassManifest((TypeElement) element, context.processingEnvironment()))
                .toList();
        events.forEach(event -> kafkaPublisherWriter.writeKafkaPublisher(event, context));
    }

    private static Stream<ExecutableElement> kafkaEventHandlers(ClassManifest aggregate, PrefabContext context) {
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
