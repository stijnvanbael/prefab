package be.appify.prefab.processor.eventhandler;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;
import static org.apache.commons.text.WordUtils.uncapitalize;

import javax.lang.model.element.Modifier;
import java.util.Optional;

public class StaticEventHandlerWriter {
    public MethodSpec staticDomainHandlerMethod(ClassManifest manifest, StaticEventHandlerManifest eventHandler) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(event.asTypeName(), "event");
        if (event.inheritedAnnotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        return eventHandler.returnType().is(Optional.class)
                ? method.addStatement(CodeBlock.builder()
                        .add("$T.$L(event).ifPresent(aggregate -> $L.save(aggregate))",
                                manifest.type().asTypeName(),
                                eventHandler.methodName(),
                                uncapitalize(manifest.simpleName()) + "Repository")
                        .build())
                .build()
                : method.addStatement(CodeBlock.builder()
                                .add("$L.save($T.$L(event))",
                                        uncapitalize(manifest.simpleName()) + "Repository",
                                        manifest.type().asTypeName(),
                                        eventHandler.methodName())
                                .build())
                        .build();
    }
}
