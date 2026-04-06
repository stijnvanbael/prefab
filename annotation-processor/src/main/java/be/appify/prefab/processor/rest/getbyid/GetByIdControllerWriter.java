package be.appify.prefab.processor.rest.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.pathParameterAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;

class GetByIdControllerWriter {
    MethodSpec getByIdMethod(ClassManifest manifest, GetById getById) {
        var method = MethodSpec.methodBuilder("getById")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getById.method(), getById.path()));
        operationAnnotation("Get " + manifest.simpleName() + " by ID").ifPresent(method::addAnnotation);
        securedAnnotation(getById.security()).ifPresent(method::addAnnotation);
        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + manifest.simpleName() + " ID").ifPresent(idParameter::addAnnotation);
        return method
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class),
                        responseType(manifest)))
                .addParameter(idParameter.build())
                .addStatement("return toResponse(service.getById(id))")
                .build();
    }

    MethodSpec getByIdMethod(PolymorphicAggregateManifest manifest, GetById getById) {
        var method = MethodSpec.methodBuilder("getById")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(getById.method(), getById.path()));
        operationAnnotation("Get " + manifest.simpleName() + " by ID").ifPresent(method::addAnnotation);
        securedAnnotation(getById.security()).ifPresent(method::addAnnotation);
        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + manifest.simpleName() + " ID").ifPresent(idParameter::addAnnotation);
        return method
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class),
                        responseType(manifest)))
                .addParameter(idParameter.build())
                .addStatement("return toResponse(service.getById(id))")
                .build();
    }
}
