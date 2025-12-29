package be.appify.prefab.processor.eventhandler.byreference;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;
import static org.apache.commons.text.WordUtils.uncapitalize;

import javax.lang.model.element.Modifier;

public class ByReferenceEventHandlerWriter {
    public MethodSpec byReferenceEventHandlerMethod(ClassManifest manifest,
            ByReferenceEventHandlerManifest eventHandler) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC);
        if (event.inheritedAnnotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        return method.addParameter(event.asTypeName(), "event").addStatement(CodeBlock.builder()
                        .add("$L.findById(event.$L().id())",
                                uncapitalize(manifest.simpleName()) + "Repository", eventHandler.annotation().value())
                        .add("""
                                        .map(aggregate -> {
                                            $L
                                            return $L.save(aggregate);
                                        })
                                        """,
                                CodeBlock.of("aggregate.$L(event);", eventHandler.methodName()),
                                uncapitalize(manifest.simpleName()) + "Repository")
                        .add(".orElseThrow()")
                        .build())
                .build();
    }
}
