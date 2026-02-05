package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;

public class SerializationPlugin implements PrefabPlugin {
    private final SerializationRegistryConfigurationWriter serializationRegistryConfigurationWriter = new SerializationRegistryConfigurationWriter();

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext context) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .map(element -> new TypeManifest(element.asType(), context.processingEnvironment()))
                .toList();
        if (!events.isEmpty()) {
            serializationRegistryConfigurationWriter.writeConfiguration(events, context);
        }
    }
}
