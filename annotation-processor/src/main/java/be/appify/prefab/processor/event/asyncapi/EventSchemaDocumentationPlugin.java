package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.SourceContributions;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.EventBindings;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/** Collects source-owned event declarations and subscriptions for the module's AsyncAPI document. */
public class EventSchemaDocumentationPlugin implements PrefabPlugin {
    private final SourceContributions<Input> inputs = new SourceContributions<>(Input.class);
    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        context.eventElementsFromCurrentCompilation().forEach(event -> {
            if (event.getAnnotation(Avsc.class) != null) {
                return; // Document the concrete records contributed in the following round instead.
            }
            var owner = event.getInterfaces().stream().filter(DeclaredType.class::isInstance)
                    .map(type -> (TypeElement) ((DeclaredType) type).asElement())
                    .filter(type -> type.getAnnotation(Avsc.class) != null)
                    .findFirst().orElse(event);
            inputs.add(owner, new Input(event.getQualifiedName().toString(), ""));
        });
        context.roundEnvironment().getElementsAnnotatedWith(EventHandler.class).stream()
                .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
                .forEach(handler -> handler.getParameters().forEach(parameter -> {
                    var type = TypeManifest.of(parameter.asType(), context.processingEnvironment());
                    if (!type.inheritedAnnotationsOfType(Event.class).isEmpty()) {
                        var declaration = type.asElement();
                        var name = declaration.getAnnotationsByType(Event.class).length > 0
                                && declaration.getAnnotation(Avsc.class) == null
                                ? declaration.getQualifiedName().toString() : "";
                        inputs.add((TypeElement) handler.getEnclosingElement(),
                                new Input(name, EventBindings.identityOf(type.asTypeName())));
                    }
                }));
    }

    @Override
    public PrefabContext.EventScope additionalFileEventScope() {
        return PrefabContext.EventScope.CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES;
    }

    @Override
    public void finishProcessing() {
        var contributions = inputs.merge(context);
        var elements = context.processingEnvironment().getElementUtils();
        var declarations = new TreeSet<String>();
        var consumed = new HashSet<String>();
        contributions.values().forEach(values -> values.forEach(input -> {
            if (!input.declaration().isEmpty()) {
                declarations.add(input.declaration());
            }
            if (!input.consumed().isEmpty()) {
                consumed.add(input.consumed());
            }
        }));
        var events = declarations.stream().map(elements::getTypeElement).filter(Objects::nonNull)
                .flatMap(type -> EventBindings.typesOf(type, context))
                .sorted(Comparator.comparing(type -> EventBindings.identityOf(type.asTypeName())))
                .toList();
        if (!events.isEmpty()) {
            var owners = contributions.keySet().stream().map(elements::getTypeElement)
                    .filter(Objects::nonNull).toArray(TypeElement[]::new);
            new EventSchemaDocumentationWriter(context).writeDocumentation(events, consumed, owners);
        }
    }

    private record Input(String declaration, String consumed) {}
}
