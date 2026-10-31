package be.appify.prefab.processor.event;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates one {@code *EventTypeRegistrar} component per {@code @Event}-annotated type found in
 * the current compilation, regardless of broker platform. Dependency events are skipped — their
 * registrar was already written when that module was compiled.
 */
public class SerializationPlugin implements PrefabPlugin {
    private EventTypeRegistrarWriter eventTypeRegistrarWriter;
    private EventEvolutionValidator evolutionValidator;
    private EventVersioningValidator versioningValidator;
    private PrefabContext context;
    private final Set<String> writtenTypes = new HashSet<>();

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        eventTypeRegistrarWriter = new EventTypeRegistrarWriter(context);
        evolutionValidator = new EventEvolutionValidator(context);
        versioningValidator = new EventVersioningValidator(context);
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        validateEvolution();
        versioningValidator.validate();
        context.eventElementsFromCurrentCompilation()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .map(EventPlatformPluginSupport::publisherEventType)
                .distinct()
                .filter(event -> EventBindings.validate(event.asElement(), context))
                .filter(event -> writtenTypes.add(event.packageName() + "." + event.simpleName()))
                .forEach(eventTypeRegistrarWriter::writeRegistrar);
    }

    private void validateEvolution() {
        var events = context.eventElementsFromCurrentCompilation()
                .map(element -> TypeManifest.of(element.asType(), context.processingEnvironment()))
                .toList();
        var currentNames = events.stream()
                .flatMap(SerializationPlugin::withSubtypes)
                .flatMap(type -> EventEvolutionValidator.namesOf(type).stream())
                .collect(Collectors.toSet());
        events.forEach(event -> evolutionValidator.validate(event, currentNames));
    }

    private static Stream<TypeManifest> withSubtypes(TypeManifest type) {
        return Stream.concat(Stream.of(type), type.permittedSubtypes().stream().flatMap(SerializationPlugin::withSubtypes));
    }
}