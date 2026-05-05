package be.appify.prefab.processor.event.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import static be.appify.prefab.processor.event.EventPlatformPluginSupport.derivedPlatform;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.filteredEventHandlersByOwner;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isAvscGeneratedRecord;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.isMultiplePlatformsDetected;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.publisherEventType;
import static be.appify.prefab.processor.event.EventPlatformPluginSupport.setDerivedPlatform;
import static java.util.Objects.requireNonNull;

/**
 * Prefab plugin to generate SNS publishers and SQS subscribers based on event annotations.
 */
public class SnsPlugin implements PrefabPlugin {
    private SnsPublisherWriter snsPublisherWriter;
    private SqsSubscriberWriter sqsSubscriberWriter;
    private PrefabContext context;

    /** Constructs a new SnsPlugin. */
    public SnsPlugin() {
        setDerivedPlatform(Event.Platform.SNS_SQS);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> aggregates) {
        writePublishers();
        writeConsumers();
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        snsPublisherWriter = new SnsPublisherWriter(context);
        sqsSubscriberWriter = new SqsSubscriberWriter(context);
    }

    private void writeConsumers() {
        filteredEventHandlersByOwner(context, this::isSnsSqsEvent)
                .forEach((owner, eventHandlers) ->
                        sqsSubscriberWriter.writeSqsSubscriber(owner, eventHandlers));
    }

    private boolean isSnsSqsEvent(ExecutableElement method) {
        return method.getParameters().stream()
                .anyMatch(parameter ->
                        TypeManifest.of(parameter.asType(), context.processingEnvironment())
                                .inheritedAnnotationsOfType(Event.class)
                                .stream()
                                .anyMatch(event -> platformIsSnsSqs(event, method, context)));
    }

    private void writePublishers() {
        var events = context.eventElements()
                .filter(e -> !isAvscGeneratedRecord(e))
                .filter(e -> platformIsSnsSqs(requireNonNull(e.getAnnotation(Event.class)), e, context))
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .map(EventPlatformPluginSupport::publisherEventType)
                .distinct()
                .toList();
        events.forEach(event -> snsPublisherWriter.writeSnsPublisher(event));
    }

    static boolean platformIsSnsSqs(Event event, Element element, PrefabContext context) {
        if (event.platform() == Event.Platform.DERIVED && isMultiplePlatformsDetected()) {
            context.logError(
                    "Cannot derive platform for event [%s] because multiple messaging platforms are configured. Please specify the platform explicitly."
                            .formatted(element.getSimpleName()), element);
        }
        return event.platform() == Event.Platform.SNS_SQS ||
                event.platform() == Event.Platform.DERIVED && derivedPlatform() == Event.Platform.SNS_SQS;
    }
}
