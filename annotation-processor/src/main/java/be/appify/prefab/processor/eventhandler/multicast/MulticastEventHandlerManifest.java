package be.appify.prefab.processor.eventhandler.multicast;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

public record MulticastEventHandlerManifest(
        ExecutableElement methodElement,
        TypeManifest eventType,
        PrefabContext context,
        String queryMethod,
        EventHandler.Param[] paramMapping
) {
    public String methodName() {
        return methodElement.getSimpleName().toString();
    }

    public TypeManifest returnType() {
        return methodElement.getReturnType().getKind() == TypeKind.VOID
                ? null
                : new TypeManifest(methodElement.getReturnType(), context.processingEnvironment());
    }
}
