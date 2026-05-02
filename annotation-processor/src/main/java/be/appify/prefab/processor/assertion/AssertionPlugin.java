package be.appify.prefab.processor.assertion;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.assertion.AssertionWriter.AssertionEntry;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * Plugin that generates AssertJ assertion classes for every response record, event type, and
 * nested record in the domain model, plus a per-package {@code Assertions} factory class.
 */
public class AssertionPlugin implements PrefabPlugin {

    private PrefabContext context;
    private final Set<String> writtenTypes = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, List<AssertionEntry>> entriesByPackage = new HashMap<>();

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(
            List<ClassManifest> manifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        var writer = new AssertionWriter(context, writtenTypes, entriesByPackage);

        manifests.forEach(writer::writeResponseAssert);

        context.eventElements().forEach(element -> {
            var preferredElement = avscContractInterface(element);
            writer.writeEventAssert(preferredElement);
        });
    }

    @Override
    public void writeGlobalFiles(
            List<ClassManifest> manifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        var writer = new AssertionWriter(context, writtenTypes, entriesByPackage);
        entriesByPackage.forEach(writer::writeAssertionsClass);
    }

    private static TypeElement avscContractInterface(TypeElement element) {
        if (!EventPlatformPluginSupport.isAvscGeneratedRecord(element)) {
            return element;
        }
        return element.getInterfaces().stream()
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Avsc.class) != null)
                .findFirst()
                .orElse(element);
    }
}
