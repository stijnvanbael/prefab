package be.appify.prefab.processor.event;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A plugin that generates a configuration file for the serialization registry, containing all events.
 */
public class SerializationPlugin implements PrefabPlugin {
    private SerializationRegistryConfigurationWriter serializationRegistryConfigurationWriter;
    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        serializationRegistryConfigurationWriter = new SerializationRegistryConfigurationWriter(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        var eventsByPackage = context.eventElements()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .collect(Collectors.groupingBy(TypeManifest::packageName));

        eventsByPackage.forEach(
                serializationRegistryConfigurationWriter::writeConfigurationForPackage);
    }
}
