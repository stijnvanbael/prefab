package be.appify.prefab.processor.event.handler;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static org.apache.commons.text.WordUtils.uncapitalize;

class StaticEventHandlerWriter {
    MethodSpec staticDomainHandlerMethod(ClassManifest manifest, StaticEventHandlerManifest eventHandler) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(event.asTypeName(), "event");
        if (event.inheritedAnnotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        if (eventHandler.instanceMethod()) {
            var componentBeanName = uncapitalize(eventHandler.componentType().simpleName());
            return method.addStatement("$L.$L(event)", componentBeanName, eventHandler.methodName()).build();
        }
        var targetType = eventHandler.isMerged() ? eventHandler.componentType() : manifest.type();
        var repositoryName = uncapitalize(targetType.simpleName()) + "Repository";
        return eventHandler.returnType().is(Optional.class)
                ? method.addStatement(CodeBlock.builder()
                        .add("$T.$L(event).ifPresent(aggregate -> $L.save(aggregate))",
                                targetType.asTypeName(),
                                eventHandler.methodName(),
                                repositoryName)
                        .build())
                .build()
                : method.addStatement(CodeBlock.builder()
                                .add("$L.save($T.$L(event))",
                                        repositoryName,
                                        targetType.asTypeName(),
                                        eventHandler.methodName())
                                .build())
                        .build();
    }
}
