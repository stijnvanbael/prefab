package be.appify.prefab.processor.event.handler.createorupdate;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;
import org.springframework.context.event.EventListener;

import static org.apache.commons.text.WordUtils.uncapitalize;

class CreateOrUpdateEventHandlerWriter {
    MethodSpec createOrUpdateEventHandlerMethod(ClassManifest manifest, CreateOrUpdateEventHandlerManifest eventHandler) {
        var event = eventHandler.eventType();
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC);
        if (event.inheritedAnnotationsOfType(Event.class).isEmpty()) {
            method.addAnnotation(EventListener.class);
        }
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        var valueAccessor = eventHandler.valueAccessor() != null ? "." + eventHandler.valueAccessor() : "";
        return method.addParameter(event.asTypeName(), "event")
                .addStatement(CodeBlock.builder()
                        .add("var existing = $L.findById(event.$L()$L)",
                                repositoryName,
                                eventHandler.annotation().property(),
                                valueAccessor)
                        .build())
                .addStatement(CodeBlock.builder()
                        .add("$L.save($T.$L(existing, event))",
                                repositoryName,
                                manifest.type().asTypeName(),
                                eventHandler.methodName())
                        .build())
                .build();
    }
}
