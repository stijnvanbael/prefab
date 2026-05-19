package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import com.palantir.javapoet.ClassName;
import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static be.appify.prefab.processor.event.EventPlatformPluginSupport.*;
import static java.util.Objects.requireNonNull;

/**
 * Prefab plugin to generate Kafka producers and consumers based on event annotations.
 */
public class KafkaPlugin implements PrefabPlugin {
    private KafkaConsumerWriter kafkaConsumerWriter;
    private KafkaEventTypeRegistrarWriter kafkaEventTypeRegistrarWriter;
    private PrefabContext context;

    /** Constructs a new KafkaPlugin. */
    public KafkaPlugin() {
        setDerivedPlatform(Event.Platform.KAFKA);
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        kafkaConsumerWriter = new KafkaConsumerWriter(context);
        kafkaEventTypeRegistrarWriter = new KafkaEventTypeRegistrarWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates) {
        writePublishers();
        writeConsumerConfigs();
        writeConsumers();
    }

    @Override
    public PrefabContext.EventScope additionalFileEventScope() {
        return PrefabContext.EventScope.CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES;
    }

    private void writeConsumerConfigs() {
        filteredEventHandlersByOwner(context, this::isKafkaEvent)
                .entrySet()
                .stream()
                .filter(KafkaPlugin::hasCustomConfig)
                .forEach(entry ->
                        new KafkaConsumerConfigWriter()
                                .writeConsumerConfig(entry.getKey(), entry.getValue(), context));
    }

    private static boolean hasCustomConfig(Map.Entry<TypeManifest, List<ExecutableElement>> entry) {
        return entry.getKey().inheritedAnnotationsOfType(EventHandlerConfig.class).stream()
                .anyMatch(EventHandlerConfig.Util::hasCustomConfig);
    }

    private void writeConsumers() {
        filteredEventHandlersByOwner(context, this::isKafkaEvent)
                .forEach((owner, eventHandlers) ->
                        kafkaConsumerWriter.writeKafkaConsumer(owner, eventHandlers));
    }

    private boolean isKafkaEvent(ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter -> isKafkaEventParameter(parameter, method));
    }

    private boolean isKafkaEventParameter(VariableElement parameter, ExecutableElement method) {
        if (TypeManifest.containsUnresolvedType(parameter.asType())) {
            return true;
        }
        return TypeManifest.of(parameter.asType(), context.processingEnvironment())
                .inheritedAnnotationsOfType(Event.class)
                .stream()
                .anyMatch(event -> platformIsKafka(event, method, context));
    }

    private void writePublishers() {
        writeRegularRegistrars();
        writeAvscRegistrars();
    }


    private void writeRegularRegistrars() {
        var events = context.eventElementsIncludingConsumedDependencies()
                .filter(e -> e.getAnnotation(Avsc.class) == null)
                .filter(e -> platformIsKafka(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .map(EventPlatformPluginSupport::publisherEventType)
                .distinct()
                .toList();
        events.forEach(event -> kafkaEventTypeRegistrarWriter.writeRegistrar(event));
    }

    private void writeAvscRegistrars() {
        context.avscElementsFromCurrentCompilation()
                .filter(e -> platformIsKafka(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .forEach(this::writeAvscRegistrarsForElement);
    }

    private void writeAvscRegistrarsForElement(TypeElement element) {
        var avsc = element.getAnnotation(Avsc.class);
        var event = requireNonNull(element.getAnnotation(Event.class));
        var packageName = context.processingEnvironment().getElementUtils()
                .getPackageOf(element).getQualifiedName().toString();
        for (var path : avsc.value()) {
            var schema = parseAvscSchema(path, element);
            if (schema == null) continue;
            var schemaPackage = schema.getNamespace() != null ? schema.getNamespace() : packageName;
            var eventType = ClassName.get(schemaPackage, schema.getName());
            kafkaEventTypeRegistrarWriter.writeAvscRegistrar(schemaPackage, eventType, event.topic());
        }
    }

    private Schema parseAvscSchema(String path, TypeElement originatingElement) {
        try (var stream = openResource(path)) {
            if (stream == null) {
                context.logError("AVSC file not found: '" + path + "'", originatingElement);
                return null;
            }
            return new Schema.Parser().parse(stream);
        } catch (IOException e) {
            context.logError("Failed to read AVSC file '" + path + "': " + e.getMessage(), originatingElement);
            return null;
        }
    }

    private InputStream openResource(String path) throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream != null) return stream;
        var file = Path.of("src/main/resources", path);
        return Files.exists(file) ? Files.newInputStream(file) : null;
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
