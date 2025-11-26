package be.appify.prefab.processor.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static be.appify.prefab.processor.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;

public class GetByIdControllerWriter {
    public MethodSpec getByIdMethod(ClassManifest manifest, GetById getById) {
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

    private AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }

    private ClassName responseType(ClassManifest manifest) {
        return ClassName.get("%s.infrastructure.http".formatted(manifest.packageName()),
                "%sResponse".formatted(manifest.simpleName()));
    }
}
