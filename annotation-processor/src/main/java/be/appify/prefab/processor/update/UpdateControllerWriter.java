package be.appify.prefab.processor.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.List;
import java.util.stream.Collectors;

import static be.appify.prefab.processor.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

class UpdateControllerWriter {
    MethodSpec updateMethod(ClassManifest manifest, UpdateManifest update, PrefabContext context) {
        var responseType = responseType(manifest);
        var requestParts = update.parameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder().buildMethodParameter(parameter).stream())
                .toList();
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(update.method(), "/{id}" + update.path(), requestParts))
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), responseType))
                .addParameter(ParameterSpec.builder(String.class, "id")
                        .addAnnotation(PathVariable.class)
                        .build());
        securedAnnotation(update.security()).ifPresent(method::addAnnotation);
        if (update.parameters().isEmpty()) {
            method.addStatement("return toResponse(service.$N(id))", update.operationName());
        } else {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()),
                                    "%s%sRequest".formatted(manifest.simpleName(), capitalize(update.operationName()))),
                            "request")
                    .addAnnotation(Valid.class)
                    .addAnnotation(requestParts.isEmpty()
                            ? AnnotationSpec.builder(RequestBody.class).build()
                            : AnnotationSpec.builder(RequestPart.class)
                                    .addMember("name", "$S", "body")
                                    .build())
                    .build());
            requestParts.forEach(method::addParameter);
            method.addStatement("return toResponse(service.$N(id, request$L))", update.operationName(),
                    requestParts.stream()
                            .map(param -> ".with%s(%s)".formatted(capitalize(param.name()), param.name()))
                            .collect(Collectors.joining(", ")));
        }
        return method.build();
    }

    private ClassName responseType(ClassManifest manifest) {
        return ClassName.get("%s.infrastructure.http".formatted(manifest.packageName()),
                "%sResponse".formatted(manifest.simpleName()));
    }

    private AnnotationSpec requestMapping(String method, String path, List<ParameterSpec> requestParts) {
        var requestMapping = AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path);
        if (!requestParts.isEmpty()) {
            requestMapping.addMember("consumes", "$S", MULTIPART_FORM_DATA_VALUE);
        }
        return requestMapping.build();
    }
}
