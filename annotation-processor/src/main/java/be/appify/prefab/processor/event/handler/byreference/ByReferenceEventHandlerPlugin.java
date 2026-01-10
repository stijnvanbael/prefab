package be.appify.prefab.processor.event.handler.byreference;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.event.handler.EventHandlerPlugin;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Stream;

/** Plugin to handle ByReference event handlers in Prefab. */
public class ByReferenceEventHandlerPlugin implements EventHandlerPlugin {
    private final ByReferenceEventHandlerWriter byReferenceEventHandlerWriter = new ByReferenceEventHandlerWriter();

    /** Constructs a new ByReferenceEventHandlerPlugin. */
    public ByReferenceEventHandlerPlugin() {
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        byReferenceEventHandlers(manifest, context).forEach(handler ->
                builder.addMethod(byReferenceEventHandlerWriter.byReferenceEventHandlerMethod(manifest, handler)));
    }

    private Stream<ByReferenceEventHandlerManifest> byReferenceEventHandlers(
            ClassManifest manifest,
            PrefabContext context) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().contains(Modifier.PUBLIC))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(EventHandler.ByReference.class).length > 0)
                .map(element -> {
                    var annotation = element.getAnnotationsByType(EventHandler.ByReference.class)[0];
                    var eventType = getEventType(element, context);
                    var referenceField = getFields(eventType.asElement(), context.processingEnvironment()).stream()
                            .filter(field -> field.name().equals(annotation.value())
                                    && (field.type().is(Reference.class) || field.type().is(String.class)))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Event type %s does not have a field named %s, or the field is not of type Reference".formatted(
                                            element, annotation.value())));
                    return new ByReferenceEventHandlerManifest(
                            element,
                            annotation,
                            eventType,
                            context,
                            referenceField.type().parameters().stream().findFirst()
                                    .orElse(new TypeManifest(String.class, context.processingEnvironment())));
                });
    }

    private List<VariableManifest> getFields(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .map(element -> new VariableManifest(element, processingEnvironment))
                .toList();
    }

    @Override
    public Class<? extends Annotation> annotation() {
        return EventHandler.ByReference.class;
    }
}
