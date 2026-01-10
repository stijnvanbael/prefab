package be.appify.prefab.processor.rest.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;

class GetByIdControllerWriter {
    MethodSpec getByIdMethod(ClassManifest manifest, GetById getById) {
        var method = MethodSpec.methodBuilder("getById")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getById.method(), getById.path()));
        securedAnnotation(getById.security()).ifPresent(method::addAnnotation);
        return method
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class),
                        responseType(manifest)))
                .addParameter(ParameterSpec.builder(String.class, "id")
                        .addAnnotation(PathVariable.class)
                        .build())
                .addStatement("return toResponse(service.getById(id))")
                .build();
    }
}
