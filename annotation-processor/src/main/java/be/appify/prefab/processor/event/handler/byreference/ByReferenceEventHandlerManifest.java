package be.appify.prefab.processor.event.handler.byreference;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

record ByReferenceEventHandlerManifest(
        ExecutableElement methodElement,
        EventHandler.ByReference annotation,
        TypeManifest eventType,
        PrefabContext context,
        TypeManifest referencedType
) {
    String methodName() {
        return methodElement.getSimpleName().toString();
    }

    TypeManifest returnType() {
        return methodElement.getReturnType().getKind() == TypeKind.VOID
                ? null
                : new TypeManifest(methodElement.getReturnType(), context.processingEnvironment());
    }
}
