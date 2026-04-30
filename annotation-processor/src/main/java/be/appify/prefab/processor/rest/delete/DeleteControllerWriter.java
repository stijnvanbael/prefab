package be.appify.prefab.processor.rest.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.pathParameterAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;

class DeleteControllerWriter {
    MethodSpec deleteMethod(ClassManifest manifest, Delete delete) {
        return buildDeleteMethod(manifest.simpleName(), delete);
    }

    MethodSpec deleteMethodForPolymorphic(PolymorphicAggregateManifest manifest, Delete delete) {
        return buildDeleteMethod(manifest.simpleName(), delete);
    }

    private MethodSpec buildDeleteMethod(String aggregateName, Delete delete) {
        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + aggregateName + " ID").ifPresent(idParameter::addAnnotation);
        var method = MethodSpec.methodBuilder("delete")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(delete.method(), delete.path()));
        operationAnnotation("Delete " + aggregateName).ifPresent(method::addAnnotation);
        securedAnnotation(delete.security()).ifPresent(method::addAnnotation);
        return method
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class))
                .addParameter(idParameter.build())
                .addStatement("service.delete(id)")
                .addStatement("return $T.noContent().build()", ResponseEntity.class)
                .build();
    }
}
