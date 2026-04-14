package be.appify.prefab.processor.event.handler.createorupdate;

import be.appify.prefab.core.annotations.CreateOrUpdate;
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

/**
 * Prefab plugin to generate create-or-update event handlers based on CreateOrUpdate annotations.
 */
public class CreateOrUpdateEventHandlerPlugin implements EventHandlerPlugin {
    private final CreateOrUpdateEventHandlerWriter writer = new CreateOrUpdateEventHandlerWriter();
    private PrefabContext context;

    /** Constructs a new CreateOrUpdateEventHandlerPlugin. */
    public CreateOrUpdateEventHandlerPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        createOrUpdateEventHandlers(manifest)
                .forEach(handler -> builder.addMethod(writer.createOrUpdateEventHandlerMethod(manifest, handler)));
    }

    private Stream<CreateOrUpdateEventHandlerManifest> createOrUpdateEventHandlers(ClassManifest manifest) {
        var typeElement = manifest.type().asElement();
        return typeElement.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD
                        && element.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC)))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getAnnotationsByType(CreateOrUpdate.class).length > 0)
                .flatMap(element -> {
                    var annotation = element.getAnnotationsByType(CreateOrUpdate.class)[0];
                    var parameters = element.getParameters();
                    if (parameters.size() != 2) {
                        context.logError(
                                "Create-or-update event handler method %s must have exactly two parameters: Optional<%s> and an event type"
                                        .formatted(element, typeElement.getSimpleName()),
                                element);
                        return Stream.empty();
                    }
                    var eventType = TypeManifest.of(parameters.get(1).asType(), context.processingEnvironment());
                    if (eventType.asElement() == null) {
                        context.logError(
                                "Create-or-update event handler method %s second parameter must be a declared event type"
                                        .formatted(element),
                                element);
                        return Stream.empty();
                    }
                    return getFields(eventType.asElement(), context.processingEnvironment()).stream()
                            .filter(field -> field.name().equals(annotation.property())
                                    && (field.type().isSingleValueType() || field.type().is(String.class)))
                            .findFirst()
                            .or(() -> {
                                context.logError(
                                        "Event type %s does not have a field named '%s' of type Reference or String"
                                                .formatted(eventType.simpleName(), annotation.property()),
                                        element);
                                return Optional.empty();
                            })
                            .map(referenceField -> new CreateOrUpdateEventHandlerManifest(
                                    element,
                                    annotation,
                                    eventType,
                                    context,
                                    referenceField.type()))
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

    @Override
    public Class<? extends Annotation> annotation() {
        return CreateOrUpdate.class;
    }
}
