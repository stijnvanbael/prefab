package be.appify.prefab.processor.event.handler.createorupdate;

import be.appify.prefab.core.annotations.CreateOrUpdate;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import javax.lang.model.element.ExecutableElement;

record CreateOrUpdateEventHandlerManifest(
        ExecutableElement methodElement,
        CreateOrUpdate annotation,
        TypeManifest eventType,
        PrefabContext context,
        TypeManifest fieldType
) {
    String methodName() {
        return methodElement.getSimpleName().toString();
    }

    String valueAccessor() {
        if (fieldType.isSingleValueType()) {
            return fieldType.singleValueAccessor() + "()";
        }
        return null;
    }
}
