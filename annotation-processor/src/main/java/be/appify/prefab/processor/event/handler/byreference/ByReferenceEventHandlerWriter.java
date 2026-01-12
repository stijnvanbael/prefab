package be.appify.prefab.processor.event.handler.byreference;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;

import javax.lang.model.element.Modifier;
import java.util.Objects;

import static org.apache.commons.text.WordUtils.uncapitalize;

class ByReferenceEventHandlerWriter {
    MethodSpec byReferenceEventHandlerMethod(ClassManifest manifest, ByReferenceEventHandlerManifest eventHandler) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC);
        if (event.inheritedAnnotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        return method.addParameter(event.asTypeName(), "event").addStatement(CodeBlock.builder()
                        .add("$L.findById(event.$L().id())",
                                uncapitalize(manifest.simpleName()) + "Repository", eventHandler.annotation().property())
                        .add("""
                                        .map(aggregate -> {
                                            $L
                                            return $L.save(aggregate);
                                        })
                                        """,
                                Objects.equals(eventHandler.returnType(), manifest.type())
                                        ? CodeBlock.of("aggregate = aggregate.$L(event);", eventHandler.methodName())
                                        : CodeBlock.of("aggregate.$L(event);", eventHandler.methodName()),
                                uncapitalize(manifest.simpleName()) + "Repository")
                        .add(".orElseThrow()")
                        .build())
                .build();
    }
}
