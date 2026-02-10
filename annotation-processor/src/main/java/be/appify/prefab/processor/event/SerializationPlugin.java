package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;

/**
 * A plugin that generates a configuration file for the serialization registry, containing all events.
 */
public class SerializationPlugin implements PrefabPlugin {
    private final SerializationRegistryConfigurationWriter serializationRegistryConfigurationWriter = new SerializationRegistryConfigurationWriter();

    /**
     * Writes a configuration file for the serialization registry, containing all events.
     *
     * @param manifests the class manifests of all classes that are processed by the annotation processor
     * @param context   the context of the annotation processor, containing information about the processing environment and the round environment
     */
    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        if (!events.isEmpty()) {
            serializationRegistryConfigurationWriter.writeConfiguration(events, context);
        }
    }
}
