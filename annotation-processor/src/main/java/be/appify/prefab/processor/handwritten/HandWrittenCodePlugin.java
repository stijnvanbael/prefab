package be.appify.prefab.processor.handwritten;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * Reports hand-written code that duplicates what Prefab generates for an aggregate: controllers that map endpoints
 * Prefab already exposes, JPA entities that mirror an aggregate and Spring Data repositories for an aggregate.
 * (Hand-written database migrations are checked by the migration plugin, see
 * {@link be.appify.prefab.processor.dbmigration.ManualMigrationCheck}.)
 *
 * <p>Only the sources handed to the compiler are inspected; code that Prefab or another processor generates in a
 * later round is not. Findings are compiler errors by default; see {@link HandWrittenFindings} for the severity
 * option. Disable the checks with {@code -Aprefab.plugin.handwrittencode.enabled=false}.
 */
public class HandWrittenCodePlugin implements PrefabPlugin {
    private final Set<String> handWrittenTypeNames = new LinkedHashSet<>();
    private boolean sourcesCaptured;
    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        if (!sourcesCaptured) {
            // The first round holds exactly the sources of the project; later rounds only hold generated sources.
            sourcesCaptured = true;
            context.roundEnvironment().getRootElements().stream()
                    .filter(TypeElement.class::isInstance)
                    .forEach(root -> collect(root, handWrittenTypeNames));
        }
    }

    @Override
    public void writeGlobalFiles(
            List<ClassManifest> manifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        var aggregates = Stream.concat(
                manifests.stream().map(ManagedAggregate::of),
                polymorphicManifests.stream().map(ManagedAggregate::of)).toList();
        if (aggregates.isEmpty()) {
            return;
        }
        var findings = HandWrittenFindings.of(context, HandWrittenFindings.Severity.ERROR);
        var types = handWrittenTypes();
        new HandWrittenControllerCheck(findings).check(types, aggregates);
        new HandWrittenPersistenceCheck(findings, context.processingEnvironment()).check(types, aggregates);
    }

    private List<TypeElement> handWrittenTypes() {
        var elements = context.processingEnvironment().getElementUtils();
        var types = new ArrayList<TypeElement>();
        handWrittenTypeNames.forEach(name -> {
            var type = elements.getTypeElement(name);
            if (type != null) {
                types.add(type);
            }
        });
        return types;
    }

    private static void collect(Element element, Set<String> names) {
        if (element instanceof TypeElement type) {
            names.add(type.getQualifiedName().toString());
            type.getEnclosedElements().stream()
                    .filter(member -> member.getKind().isClass() || member.getKind().isInterface())
                    .filter(member -> member.getKind() != ElementKind.ENUM)
                    .forEach(member -> collect(member, names));
        }
    }
}
