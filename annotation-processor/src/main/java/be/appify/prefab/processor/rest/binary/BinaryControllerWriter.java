package be.appify.prefab.processor.rest.binary;

import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.binary.BinaryControllerUtil;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.lang.model.element.Modifier;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.pathParameterAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static org.apache.commons.lang3.StringUtils.capitalize;

class BinaryControllerWriter {
    MethodSpec downloadMethod(ClassManifest manifest, VariableManifest field) {
        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + manifest.simpleName() + " ID").ifPresent(idParameter::addAnnotation);
        var method = MethodSpec.methodBuilder("download%s".formatted(capitalize(field.name())))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ResponseEntity.class, InputStreamResource.class))
                .addAnnotation(AnnotationSpec.builder(GetMapping.class)
                        .addMember("path", "$S", "/{id}/%s".formatted(field.name()))
                        .build())
                .addAnnotation(ResponseBody.class);
        operationAnnotation("Download " + capitalize(field.name()) + " for " + manifest.simpleName()).ifPresent(method::addAnnotation);
        var security = field.getAnnotation(Download.class).map(AnnotationManifest::value).orElseThrow().security();
        securedAnnotation(security).ifPresent(method::addAnnotation);
        return method
                .addParameter(idParameter.build())
                .addStatement("""
                                return service.getById(id)
                                .map($T::$N)
                                .map($T::toResponseEntity)
                                .orElse($T.notFound().build())""",
                        manifest.className(),
                        field.name(),
                        BinaryControllerUtil.class,
                        ResponseEntity.class)
                .build();
    }
}
