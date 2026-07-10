package be.appify.prefab.processor.event.handler.byreference;

import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.event.handler.EventHandlerPlugin;
import com.palantir.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

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
                // @Streaming push-model methods are handled by StreamPlugin to include SSE push logic
                .filter(element -> element.getAnnotation(be.appify.prefab.core.annotations.rest.Streaming.class) == null)
                .flatMap(element -> {
                    var annotation = element.getAnnotationsByType(ByReference.class)[0];
                    var eventType = getEventType(element, context);
                    return Stream.concat(
                                    getFields(eventType.asElement(), context.processingEnvironment()).stream(),
                                    getAccessors(eventType.asElement(), context.processingEnvironment()).stream())
                            .filter(member -> member.name().equals(annotation.property())
                                    && (member.type().isSingleValueType() || member.type().is(String.class)))
                            .findFirst()
                            .or(() -> {
                                context.logError(
                                        "Event type %s does not have a field or method named '%s', or it is not of a primitive or single-value type".formatted(
                                                eventType.asElement().getSimpleName(), annotation.property()), element);
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
                                    findStaticCompanion(typeElement, eventType, context).orElse(null)))
                            .stream();
                });
    }

    private List<VariableManifest> getFields(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .map(element -> VariableManifest.of(element, processingEnvironment))
                .toList();
    }

    private List<VariableManifest> getAccessors(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getParameters().isEmpty()
                        && !method.getModifiers().contains(Modifier.STATIC)
                        && method.getModifiers().contains(Modifier.PUBLIC)
                        && method.getReturnType().getKind() != TypeKind.VOID)
                .map(method -> VariableManifest.ofMethod(method, processingEnvironment))
                .toList();
    }

    @Override
    public Class<? extends Annotation> annotation() {
        return ByReference.class;
    }
}
