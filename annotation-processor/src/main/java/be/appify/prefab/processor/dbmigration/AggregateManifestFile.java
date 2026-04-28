package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystemNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads and writes a small text file ({@code db/migration/prefab-aggregates.txt}) that records the
 * fully-qualified names of all aggregate roots present during the last <em>complete</em> compilation.
 *
 * <p>On every successful migration generation the file is updated with the current set of aggregates.
 * Before generating a new migration the file is consulted to detect an <em>incomplete</em> incremental
 * build: if a previously-known aggregate is absent from the current compilation round but its type
 * element is still resolvable on the classpath, the build has only recompiled a subset of sources.
 * In that case migration generation is skipped to avoid producing scripts with missing tables.</p>
 */
class AggregateManifestFile {

    static final String MANIFEST_PACKAGE = "db.migration";
    static final String MANIFEST_NAME = "prefab-aggregates.txt";

    private final ProcessingEnvironment processingEnvironment;

    AggregateManifestFile(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * Returns {@code true} when the current set of aggregates appears to be a complete view of the
     * project — either because no previous manifest exists, or because every aggregate listed in the
     * manifest is also present in the current compilation set.
     *
     * <p>An aggregate is considered "missing" when it appears in the manifest but not in the current
     * set <em>and</em> its type can still be resolved on the classpath. If the type is gone from the
     * classpath the aggregate was intentionally deleted and is not counted as missing.</p>
     */
    boolean isComplete(
            List<ClassManifest> classManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        var knownAggregates = readKnownAggregates();
        if (knownAggregates.isEmpty()) {
            return true;
        }
        var currentAggregates = aggregateNames(classManifests, polymorphicManifests);
        var missingAggregates = knownAggregates.stream()
                .filter(name -> !currentAggregates.contains(name))
                .filter(this::existsOnClasspath)
                .toList();
        if (!missingAggregates.isEmpty()) {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "Skipping migration script generation: incremental build detected. "
                            + "Missing aggregates: " + missingAggregates
                            + ". Run a full build to regenerate the migration script.");
            return false;
        }
        return true;
    }

    /**
     * Persists the current set of aggregate names so that future incremental builds can compare
     * against them.
     */
    void save(List<ClassManifest> classManifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        var names = aggregateNames(classManifests, polymorphicManifests);
        try {
            var resource = processingEnvironment.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, MANIFEST_PACKAGE, MANIFEST_NAME);
            var writer = new StringWriter();
            names.forEach(name -> writer.write(name + "\n"));
            try (var out = resource.openWriter()) {
                out.write(writer.toString());
            }
        } catch (IOException e) {
            processingEnvironment.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Could not write aggregate manifest: " + e.getMessage());
        }
    }

    private Set<String> readKnownAggregates() {
        try {
            var file = processingEnvironment.getFiler().getResource(
                    StandardLocation.CLASS_PATH, MANIFEST_PACKAGE, MANIFEST_NAME);
            String content;
            try (var input = file.openInputStream()) {
                content = new String(input.readAllBytes());
            }
            return Arrays.stream(content.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } catch (FileSystemNotFoundException | IOException ignored) {
            return Set.of();
        }
    }

    private boolean existsOnClasspath(String fullyQualifiedName) {
        TypeElement element = processingEnvironment.getElementUtils().getTypeElement(fullyQualifiedName);
        return element != null;
    }

    private static Set<String> aggregateNames(
            List<ClassManifest> classManifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        return Stream.concat(
                classManifests.stream().map(ClassManifest::qualifiedName),
                polymorphicManifests.stream().map(m -> m.packageName() + "." + m.simpleName())
        ).collect(Collectors.toSet());
    }
}




