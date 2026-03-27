package be.appify.prefab.processor.rest.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static be.appify.prefab.processor.rest.OpenApiUtil.apiResponseAnnotation;
import static be.appify.prefab.processor.rest.OpenApiUtil.operationAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;

class DeleteControllerWriter {
    MethodSpec deleteMethod(ClassManifest manifest, Delete delete) {
        var method = MethodSpec.methodBuilder("delete")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(delete.method(), delete.path()));
        securedAnnotation(delete.security()).ifPresent(method::addAnnotation);
        operationAnnotation("Delete %s".formatted(manifest.simpleName())).ifPresent(method::addAnnotation);
        apiResponseAnnotation("204", "No content").ifPresent(method::addAnnotation);
        apiResponseAnnotation("404", "Not found").ifPresent(method::addAnnotation);
        return method
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class))
                .addParameter(ParameterSpec.builder(String.class, "id")
                        .addAnnotation(PathVariable.class)
                        .build())
                .addStatement("service.delete(id)")
                .addStatement("return $T.noContent().build()", ResponseEntity.class)
                .build();
    }
}
