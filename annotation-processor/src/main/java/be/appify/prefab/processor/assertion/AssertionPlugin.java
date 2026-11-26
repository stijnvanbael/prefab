package be.appify.prefab.processor.assertion;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.SourceContributions;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.assertion.AssertionWriter.AssertionEntry;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * Plugin that generates AssertJ assertion classes for every response record, event type, and
 * nested record in the domain model, plus a per-package {@code Assertions} factory class.
 */
public class AssertionPlugin implements PrefabPlugin {

    private PrefabContext context;
    private final Set<String> writtenTypes = Collections.synchronizedSet(new HashSet<>());
    private final SourceContributions<AssertionEntry> entries = new SourceContributions<>(AssertionEntry.class);

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(
            List<ClassManifest> manifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        var writer = new AssertionWriter(context, writtenTypes, entries);

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
        writeFactories(OutputTarget.MAIN);
    }

    @Override
    public void finishProcessing() {
        writeFactories(OutputTarget.TEST);
    }

    private void writeFactories(OutputTarget target) {
        var byPackage = new TreeMap<String, Set<AssertionEntry>>();
        var owners = new TreeMap<String, Set<String>>();
        entries.merge(context).forEach((owner, values) -> values.stream().filter(entry -> entry.target() == target)
                .forEach(entry -> {
                    byPackage.computeIfAbsent(entry.packageName(), ignored ->
                            new TreeSet<>(Comparator.comparing(AssertionEntry::assertName))).add(entry);
                    owners.computeIfAbsent(entry.packageName(), ignored -> new TreeSet<>()).add(owner);
                }));
        var writer = new AssertionWriter(context, writtenTypes, entries);
        context.withOutputTarget(target, () -> byPackage.forEach((pkg, methods) ->
                writer.writeAssertionsClass(pkg, List.copyOf(methods), owners.get(pkg).stream()
                        .map(context.processingEnvironment().getElementUtils()::getTypeElement)
                        .filter(Objects::nonNull).toArray(TypeElement[]::new))));
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
