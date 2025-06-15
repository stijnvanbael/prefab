package be.appify.prefab.processor.delete;

import be.appify.prefab.core.annotations.rest.Delete;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static javax.lang.model.element.Modifier.PUBLIC;

public class DeleteControllerWriter {
    public MethodSpec deleteMethod(Delete delete) {
        return MethodSpec.methodBuilder("delete")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(delete.method(), delete.path()))
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class))
                .addParameter(ParameterSpec.builder(String.class, "id")
                        .addAnnotation(PathVariable.class)
                        .build())
                .addStatement("service.delete(id)")
                .addStatement("return $T.noContent().build()", ResponseEntity.class)
                .build();
    }

    private AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }
}
