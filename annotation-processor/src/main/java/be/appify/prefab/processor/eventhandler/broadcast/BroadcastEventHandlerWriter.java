package be.appify.prefab.processor.eventhandler.broadcast;

import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;

import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class BroadcastEventHandlerWriter {
    public MethodSpec broadcastEventHandlerMethod(ClassManifest manifest, BroadcastEventHandlerManifest eventHandler) {
        return MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(EventListener.class)
                .addParameter(eventHandler.eventType().asTypeName(), "event").addStatement(CodeBlock.builder()
                        .add("$L.findAll().forEach(envelope -> envelope.map(aggregate -> {\n$L}).apply($L::save));",
                                uncapitalize(manifest.simpleName()) + "Repository",
                                CodeBlock.of("""
                                            aggregate.$L(event);
                                            return aggregate;
                                        """, eventHandler.methodName()),
                                uncapitalize(manifest.simpleName()) + "Repository")
                        .build())
                .build();
    }
}
