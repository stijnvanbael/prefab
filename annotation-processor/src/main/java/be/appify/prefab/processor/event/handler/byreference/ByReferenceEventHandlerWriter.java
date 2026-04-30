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
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var mapBlock = Objects.equals(eventHandler.returnType(), manifest.type())
                ? CodeBlock.of("""
                        .map(aggregate -> {
                            var updated = aggregate.$L(event);
                            return $L.save(updated);
                        })
                        """, eventHandler.methodName(), repositoryName)
                : CodeBlock.of("""
                        .map(aggregate -> {
                            aggregate.$L(event);
                            return $L.save(aggregate);
                        })
                        """, eventHandler.methodName(), repositoryName);
        var notFoundClause = eventHandler.staticCompanionMethodName() != null
                ? CodeBlock.of(".orElseGet(() -> $L.save($T.$L(event)))",
                        repositoryName,
                        manifest.type().asTypeName(),
                        eventHandler.staticCompanionMethodName())
                : CodeBlock.of(".orElseThrow()");
        return method.addParameter(event.asTypeName(), "event").addStatement(CodeBlock.builder()
                        .add("$L.findById(event.$L()$L)",
                                repositoryName,
                                eventHandler.annotation().property(),
                                eventHandler.valueAccessor() != null ? "." + eventHandler.valueAccessor() : "")
                        .add(mapBlock)
                        .add(notFoundClause)
                        .build())
                .build();
    }
}
