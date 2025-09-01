package be.appify.prefab.processor.eventhandler.broadcast;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;
import static org.apache.commons.text.WordUtils.uncapitalize;

import javax.lang.model.element.Modifier;

public class BroadcastEventHandlerWriter {
    public MethodSpec broadcastEventHandlerMethod(ClassManifest manifest, BroadcastEventHandlerManifest eventHandler) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC);
        if (event.annotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        return method.addParameter(event.asTypeName(), "event").addStatement(CodeBlock.builder()
                        .add("$L.findAll().forEach(envelope -> envelope.map(aggregate -> {\n$L}).apply($L::save))",
                                uncapitalize(manifest.simpleName()) + "Repository",
                                eventHandler.returnType().equals(manifest.type())
                                        ? CodeBlock.of("""
                                            return aggregate.$L(event);
                                        """, eventHandler.methodName())
                                        : CodeBlock.of("""
                                                    aggregate.$L(event);
                                                    return aggregate;
                                                """, eventHandler.methodName()),
                                uncapitalize(manifest.simpleName()) + "Repository")
                        .build())
                .build();
    }
}
