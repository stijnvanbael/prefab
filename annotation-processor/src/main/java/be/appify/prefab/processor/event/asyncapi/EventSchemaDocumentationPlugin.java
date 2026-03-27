package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.List;

/**
 * Prefab plugin that generates an AsyncAPI 2.6.0 schema documentation file for all events
 * annotated with {@link Event}. The generated file describes event channels, message schemas,
 * and field types in AsyncAPI format and is written to
 * {@code META-INF/async-api/asyncapi.json} in the class output directory.
 */
public class EventSchemaDocumentationPlugin implements PrefabPlugin {
    private PrefabContext context;

    /** Creates a new instance of EventSchemaDocumentationPlugin. */
    public EventSchemaDocumentationPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        var events = context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        if (!events.isEmpty()) {
            new EventSchemaDocumentationWriter(context.processingEnvironment())
                    .writeDocumentation(events);
        }
    }
}
