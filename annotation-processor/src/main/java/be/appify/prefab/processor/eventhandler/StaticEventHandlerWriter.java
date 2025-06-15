package be.appify.prefab.processor.eventhandler;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import org.springframework.context.event.EventListener;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class StaticEventHandlerWriter {
    public MethodSpec staticDomainHandlerMethod(ClassManifest manifest, StaticEventHandlerManifest eventHandler) {
        var method = MethodSpec.methodBuilder(eventHandler.methodName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(EventListener.class)
                .addParameter(eventHandler.eventType().asTypeName(), "event");
        return eventHandler.returnType().is(Optional.class)
                ? method.addStatement(CodeBlock.builder()
                        .add("$T.$L(event).ifPresent(aggregate -> $L.save($T.createNew(aggregate)))",
                                manifest.type().asTypeName(),
                                eventHandler.methodName(),
                                uncapitalize(manifest.simpleName()) + "Repository",
                                AggregateEnvelope.class)
                        .build())
                .build()
                : method.addStatement(CodeBlock.builder()
                        .add("$L.save($T.createNew($T.$L(event)))",
                                uncapitalize(manifest.simpleName()) + "Repository",
                                AggregateEnvelope.class,
                                manifest.type().asTypeName(),
                                eventHandler.methodName())
                        .build())
                .build();
    }
}
