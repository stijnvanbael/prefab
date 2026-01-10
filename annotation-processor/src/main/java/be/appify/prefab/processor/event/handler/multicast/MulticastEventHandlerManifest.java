package be.appify.prefab.processor.event.handler.multicast;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

record MulticastEventHandlerManifest(
        ExecutableElement methodElement,
        TypeManifest eventType,
        PrefabContext context,
        String queryMethod,
        EventHandler.Param[] paramMapping
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
