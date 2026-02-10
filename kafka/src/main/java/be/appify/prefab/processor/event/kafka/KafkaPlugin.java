package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import static be.appify.prefab.processor.event.EventPlatformPluginSupport.derivedPlatform;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.filteredEventHandlersByOwner;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isMultiplePlatformsDetected;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.setDerivedPlatform;
import static java.util.Objects.requireNonNull;

/**
 * Prefab plugin to generate Kafka producers and consumers based on event annotations.
 */
public class KafkaPlugin implements PrefabPlugin {
    private final KafkaProducerWriter kafkaProducerWriter = new KafkaProducerWriter();
    private final KafkaConsumerWriter kafkaConsumerWriter = new KafkaConsumerWriter();

    /** Constructs a new KafkaPlugin. */
    public KafkaPlugin() {
        setDerivedPlatform(Event.Platform.KAFKA);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates, PrefabContext context) {
        writePublishers(context);
        writeConsumerConfigs(context);
        writeConsumers(context);
    }

    private void writeConsumerConfigs(PrefabContext context) {
        filteredEventHandlersByOwner(context, KafkaPlugin::isKafkaEvent)
                .entrySet()
                .stream()
                .filter(KafkaPlugin::hasCustomConfig)
                .forEach((entry) ->
                        new KafkaConsumerConfigWriter()
                                .writeConsumerConfig(entry.getKey(), entry.getValue(), context));
    }

    private static boolean hasCustomConfig(Map.Entry<TypeManifest, List<ExecutableElement>> entry) {
        return entry.getKey().inheritedAnnotationsOfType(EventHandlerConfig.class).stream()
                .anyMatch(EventHandlerConfig.Util::hasCustomConfig);
    }

    private void writeConsumers(PrefabContext context) {
        filteredEventHandlersByOwner(context, KafkaPlugin::isKafkaEvent)
                .forEach((owner, eventHandlers) ->
                        kafkaConsumerWriter.writeKafkaConsumer(owner, eventHandlers, context));
    }

    private static boolean isKafkaEvent(ExecutableElement method, PrefabContext context) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        TypeManifest.of(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class)
                                .stream()
                                .anyMatch(event -> platformIsKafka(event, method, context)));
    }

    private void writePublishers(PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .filter(e -> platformIsKafka(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        events.forEach(event -> kafkaProducerWriter.writeKafkaProducer(event, context));
    }

    static boolean platformIsKafka(Event event, Element element, PrefabContext context) {
        if (event.platform() == Event.Platform.DERIVED && isMultiplePlatformsDetected()) {
            context.logError(
                    "Cannot derive platform for event [%s] because multiple messaging platforms are configured. Please specify the platform explicitly."
                            .formatted(element.getSimpleName()), element);
        }
        return event.platform() == Event.Platform.KAFKA ||
                event.platform() == Event.Platform.DERIVED && derivedPlatform() == Event.Platform.KAFKA;
    }
}
