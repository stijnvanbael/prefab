package be.appify.prefab.processor.event.handler;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.audit.AuditFields;
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
            // Bean name uses Spring's default naming convention (uncapitalized simple class name).
            // Custom bean names via @Component("customName") are not supported.
            var componentBeanName = uncapitalize(eventHandler.componentType().simpleName());
            return method.addStatement("$L.$L(event)", componentBeanName, eventHandler.methodName()).build();
        }
        var targetType = eventHandler.isMerged() ? eventHandler.componentType() : manifest.type();
        var repositoryName = uncapitalize(targetType.simpleName()) + "Repository";
        var needsAudit = !eventHandler.isMerged() && AuditFields.hasAuditFields(manifest);
        if (eventHandler.returnType().is(Optional.class)) {
            return buildOptionalHandlerMethod(method, manifest, eventHandler, targetType, repositoryName, needsAudit);
        }
        return buildDirectHandlerMethod(method, manifest, eventHandler, targetType, repositoryName, needsAudit);
    }

    private MethodSpec buildOptionalHandlerMethod(
            MethodSpec.Builder method,
            ClassManifest manifest,
            StaticEventHandlerManifest eventHandler,
            be.appify.prefab.processor.TypeManifest targetType,
            String repositoryName,
            boolean needsAudit
    ) {
        if (needsAudit) {
            return method.addStatement(CodeBlock.builder()
                    .add("$T.$L(event).map(aggregate -> new $T($L)).ifPresent(aggregate -> $L.save(aggregate))",
                            targetType.asTypeName(),
                            eventHandler.methodName(),
                            targetType.asTypeName(),
                            AuditFields.createReconstructionArgs(manifest.fields()),
                            repositoryName)
                    .build())
                    .build();
        }
        return method.addStatement(CodeBlock.builder()
                .add("$T.$L(event).ifPresent(aggregate -> $L.save(aggregate))",
                        targetType.asTypeName(),
                        eventHandler.methodName(),
                        repositoryName)
                .build())
                .build();
    }

    private MethodSpec buildDirectHandlerMethod(
            MethodSpec.Builder method,
            ClassManifest manifest,
            StaticEventHandlerManifest eventHandler,
            be.appify.prefab.processor.TypeManifest targetType,
            String repositoryName,
            boolean needsAudit
    ) {
        if (needsAudit) {
            method.addStatement("var aggregate = $T.$L(event)", targetType.asTypeName(), eventHandler.methodName());
            method.addStatement("aggregate = new $T($L)", targetType.asTypeName(),
                    AuditFields.createReconstructionArgs(manifest.fields()));
            return method.addStatement("$L.save(aggregate)", repositoryName).build();
        }
        return method.addStatement(CodeBlock.builder()
                        .add("$L.save($T.$L(event))",
                                repositoryName,
                                targetType.asTypeName(),
                                eventHandler.methodName())
                        .build())
                .build();
    }
}
