package be.appify.prefab.processor.event.handler.byreference;

import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.event.handler.EventHandlerPlugin;
import com.palantir.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static javax.lang.model.type.TypeKind.VOID;

/** Plugin to handle ByReference event handlers in Prefab. */
public class ByReferenceEventHandlerPlugin implements EventHandlerPlugin {
    private final ByReferenceEventHandlerWriter byReferenceEventHandlerWriter = new ByReferenceEventHandlerWriter();
    private PrefabContext context;


    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        byReferenceEventHandlers(manifest).forEach(handler ->
                builder.addMethod(byReferenceEventHandlerWriter.byReferenceEventHandlerMethod(manifest, handler)));
    }

    private Stream<ByReferenceEventHandlerManifest> byReferenceEventHandlers(ClassManifest manifest) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC)
                        && !element.getModifiers().contains(Modifier.STATIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(ByReference.class).length > 0)
                .flatMap(element -> {
                    var annotation = element.getAnnotationsByType(ByReference.class)[0];
                    var eventType = getEventType(element, context);
                    return getFields(eventType.asElement(), context.processingEnvironment()).stream()
                            .filter(field -> field.name().equals(annotation.property())
                                    && (field.type().isSingleValueType() || field.type().is(String.class)))
                            .findFirst()
                            .or(() -> {
                                context.logError(
                                        "Event type %s does not have a field named %s, or the field is not of type Reference".formatted(
                                                element, annotation.property()), element);
                                return Optional.empty();
                            })
                            .map(referenceField -> new ByReferenceEventHandlerManifest(
                                    element,
                                    annotation,
                                    eventType,
                                    context,
                                    referenceField.type().parameters().stream().findFirst()
                                            .orElse(TypeManifest.of(String.class, context.processingEnvironment())),
                                    referenceField.type(),
                                    findStaticCompanion(typeElement, eventType)))
                            .stream();
                });
    }

    private Optional<String> findStaticCompanion(TypeElement typeElement, TypeManifest eventType) {
        var candidates = typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC)))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.class).length > 0)
                .filter(element -> element.getParameters().size() == 1)
                .filter(element -> {
                    var paramType = TypeManifest.of(element.getParameters().getFirst().asType(),
                            context.processingEnvironment());
                    return paramType.asElement() != null
                            && paramType.asElement().equals(eventType.asElement());
                })
                .filter(element -> {
                    var returnType = TypeManifest.of(element.getReturnType(), context.processingEnvironment());
                    return element.getReturnType().getKind() != VOID
                            && !returnType.is(Optional.class)
                            && Objects.equals(returnType.asElement(), typeElement);
                })
                .toList();
        if (candidates.size() > 1) {
            candidates.forEach(element -> context.logError(
                    "Multiple static @EventHandler methods found for event type %s; at most one static companion is allowed per event type".formatted(
                            eventType.asElement().getSimpleName()),
                    element));
            return Optional.empty();
        }
        return candidates.stream().map(element -> element.getSimpleName().toString()).findFirst();
    }

    private List<VariableManifest> getFields(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .map(element -> VariableManifest.of(element, processingEnvironment))
                .toList();
    }

    @Override
    public Class<? extends Annotation> annotation() {
        return ByReference.class;
    }
}
