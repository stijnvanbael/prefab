package be.appify.prefab.processor.event.kafka;

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
 * Prefab plugin to generate Kafka producers and consumers based on event annotations.
 */
public class KafkaPlugin implements PrefabPlugin {
    private final KafkaProducerWriter kafkaProducerWriter = new KafkaProducerWriter();
    private final KafkaConsumerWriter kafkaConsumerWriter = new KafkaConsumerWriter();

    /** Constructs a new KafkaPlugin. */
    public KafkaPlugin() {
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
                .filter(method -> isKafkaEvent(context, method))
                .collect(groupingBy(method -> ownerOf(context, method)))
                .forEach((owner, eventHandlers) ->
                        kafkaConsumerWriter.writeKafkaConsumer(owner, eventHandlers, context));
    }

    private static boolean isKafkaEvent(PrefabContext context, ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        new TypeManifest(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class)
                                .stream()
                                .anyMatch(event -> event.platform() == Event.Platform.KAFKA));
    }

    private void writePublishers(PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> requireNonNull(e.getAnnotation(Event.class)).platform() == Event.Platform.KAFKA)
                .map(element -> new TypeManifest(element.asType(), context.processingEnvironment()))
                .toList();
        events.forEach(event -> kafkaProducerWriter.writeKafkaProducer(event, context));
    }
}
