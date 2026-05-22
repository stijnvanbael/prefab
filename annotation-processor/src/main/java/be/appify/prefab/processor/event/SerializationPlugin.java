package be.appify.prefab.processor.event;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates one {@code *EventTypeRegistrar} component per {@code @Event}-annotated type found in
 * the current compilation, regardless of broker platform. Dependency events are skipped — their
 * registrar was already written when that module was compiled.
 */
public class SerializationPlugin implements PrefabPlugin {
    private EventTypeRegistrarWriter eventTypeRegistrarWriter;
    private PrefabContext context;
    private final Set<String> writtenTypes = new HashSet<>();

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        eventTypeRegistrarWriter = new EventTypeRegistrarWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        context.eventElementsFromCurrentCompilation()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .map(EventPlatformPluginSupport::publisherEventType)
                .distinct()
                .filter(event -> writtenTypes.add(event.packageName() + "." + event.simpleName()))
                .forEach(eventTypeRegistrarWriter::writeRegistrar);
    }
}
