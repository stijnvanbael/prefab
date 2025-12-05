package be.appify.prefab.processor.eventhandler.multicast;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;

public class MulticastEventHandlerWriter {
    public MethodSpec multicastEventHandlerMethod(ClassManifest manifest, MulticastEventHandlerManifest eventHandler,
            PrefabContext context) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC);
        if (event.annotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        method.addParameter(event.asTypeName(), "event");

        var repositoryName = manifest.simpleName() + "Repository";
        var repositoryTypeName = ClassName.get("%s.application".formatted(manifest.packageName()), repositoryName);

        var eventElement = eventHandler.eventType().asElement();
        var eventProperties = eventElement.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .toList();

        var arguments = Stream.of(eventHandler.paramMapping())
                .map(param -> {
                    var eventProperty = eventProperties.stream()
                            .filter(p -> p.getSimpleName().toString().equals(param.from()))
                            .findFirst()
                            .orElse(null);

                    if (eventProperty == null) {
                        context.logError("Cannot find property '%s' on event %s"
                                        .formatted(param.from(), eventHandler.eventType().simpleName()),
                                eventHandler.methodElement());
                        return CodeBlock.of("null");
                    }

                    var accessor = eventHandler.eventType().isRecord()
                            ? eventProperty.getSimpleName().toString()
                            : "get" + capitalize(eventProperty.getSimpleName().toString());
                    return CodeBlock.of("event.$L()", accessor);
                })
                .collect(Collectors.toList());

        return method.addStatement(CodeBlock.builder()
                        .add("""
                                        $N.$L($L)
                                        .forEach(aggregate -> {
                                            $L
                                            $N.save(aggregate);
                                        })""",
                                uncapitalize(repositoryName),
                                eventHandler.queryMethod(),
                                CodeBlock.join(arguments, ", "),
                                Objects.equals(eventHandler.returnType(), manifest.type())
                                        ? CodeBlock.of("aggregate = aggregate.$L(event);", eventHandler.methodName())
                                        : CodeBlock.of("aggregate.$L(event);", eventHandler.methodName()),
                                uncapitalize(repositoryName))
                        .build())
                .build();
    }
}
