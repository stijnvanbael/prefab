package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.derivedPlatform;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.filteredEventHandlersByOwner;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isAvscGeneratedRecord;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isMultiplePlatformsDetected;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.setDerivedPlatform;
import static java.util.Objects.requireNonNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * Prefab plugin to generate Pub/Sub publishers and subscribers based on event annotations.
 */
public class PubSubPlugin implements PrefabPlugin {
    private PubSubEventTypeRegistrarWriter pubSubEventTypeRegistrarWriter;
    private PubSubSubscriberWriter pubSubSubscriberWriter;
    private PrefabContext context;

    /** Constructs a new PubSubPlugin. */
    public PubSubPlugin() {
        setDerivedPlatform(Event.Platform.PUB_SUB);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates) {
        writeRegistrars();
        writeConsumers();
    }

    @Override
    public PrefabContext.EventScope additionalFileEventScope() {
        return PrefabContext.EventScope.CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES;
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        pubSubEventTypeRegistrarWriter = new PubSubEventTypeRegistrarWriter(context);
        pubSubSubscriberWriter = new PubSubSubscriberWriter(context);
    }

    private void writeConsumers() {
        filteredEventHandlersByOwner(context, this::isPubSubEvent)
                .forEach((owner, eventHandlers) ->
                        pubSubSubscriberWriter.writePubSubSubscriber(owner, eventHandlers));
    }

    private boolean isPubSubEvent(ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter -> isPubSubEventParameter(parameter, method));
    }

    private boolean isPubSubEventParameter(VariableElement parameter, ExecutableElement method) {
        if (TypeManifest.containsUnresolvedType(parameter.asType())) {
            return true;
        }
        return TypeManifest.of(parameter.asType(), context.processingEnvironment())
                .inheritedAnnotationsOfType(Event.class)
                .stream()
                .anyMatch(event -> platformIsPubSub(event, method, context));
    }

    private void writeRegistrars() {
        context.eventElementsIncludingConsumedDependencies()
                .filter(e -> !isAvscGeneratedRecord(e))
                .filter(e -> platformIsPubSub(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .map(EventPlatformPluginSupport::publisherEventType)
                .distinct()
                .forEach(event -> pubSubEventTypeRegistrarWriter.writeRegistrar(event));
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
