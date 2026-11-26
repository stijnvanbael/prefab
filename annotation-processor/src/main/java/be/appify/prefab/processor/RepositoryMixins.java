package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.RepositoryMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;

/** A repository can be regenerated without javac being given the source of its mixin. */
final class RepositoryMixins {
    private final SourceContributions<Binding> inputs = new SourceContributions<>(Binding.class);
    private final Map<String, List<TypeElement>> byAggregate = new TreeMap<>();

    void observe(PrefabContext context) {
        context.roundEnvironment().getElementsAnnotatedWith(RepositoryMixin.class).stream()
                .map(TypeElement.class::cast)
                .forEach(mixin -> inputs.add(mixin, new Binding(mixin.getQualifiedName().toString(), aggregateOf(mixin))));
        byAggregate.clear();
        var elements = context.processingEnvironment().getElementUtils();
        inputs.merge(context).values().forEach(bindings -> bindings.forEach(binding -> {
            var mixin = elements.getTypeElement(binding.mixin());
            if (mixin != null) {
                byAggregate.computeIfAbsent(binding.aggregate(), ignored -> new ArrayList<>()).add(mixin);
            }
        }));
    }

    private static String aggregateOf(TypeElement mixin) {
        try {
            return mixin.getAnnotation(RepositoryMixin.class).value().getCanonicalName();
        } catch (MirroredTypeException mirrored) {
            return mirrored.getTypeMirror().toString();
        }
    }

    List<TypeElement> forAggregate(TypeManifest aggregate) {
        return byAggregate.getOrDefault(aggregate.asElement().getQualifiedName().toString(), List.of());
    }

    private record Binding(String mixin, String aggregate) {}
}
