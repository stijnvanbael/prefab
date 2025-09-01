package be.appify.prefab.processor.create;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.atteo.evo.inflector.English.plural;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import javax.lang.model.element.ExecutableElement;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class CreateControllerWriter {
    public MethodSpec createMethod(ClassManifest manifest, ExecutableElement constructor, PrefabContext context) {
        var create = constructor.getAnnotation(Create.class);
        var requestParts = constructor.getParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder()
                        .buildMethodParameter(new VariableManifest(parameter, context.processingEnvironment()))
                        .stream()).toList();
        var method = MethodSpec.methodBuilder("create")
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(create.method(), create.path(), requestParts))
                .returns(ParameterizedTypeName.get(ResponseEntity.class, Void.class));
        if (constructor.getParameters().isEmpty()) {
            method.addStatement("var id = service.create()");
        } else {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(manifest.packageName()),
                                    "Create%sRequest".formatted(manifest.simpleName())),
                            "request")
                    .addAnnotation(Valid.class)
                    .addAnnotation(requestParts.isEmpty()
                            ? AnnotationSpec.builder(RequestBody.class).build()
                            : AnnotationSpec.builder(RequestPart.class)
                                    .addMember("name", "$S", "body")
                                    .build())
                    .build());
            requestParts.forEach(method::addParameter);
            method.addStatement("var id = service.create(request$L)", String.join(", ", requestParts.stream()
                    .map(param -> ".with%s(%s)".formatted(capitalize(param.name()), param.name()))
                    .collect(Collectors.joining(", "))));
        }
        return method
                .addStatement("return $T.created($T.create($S + id)).build()",
                        ResponseEntity.class, URI.class, toKebabCase("/" + plural(manifest.simpleName()) + "/"))
                .build();
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
