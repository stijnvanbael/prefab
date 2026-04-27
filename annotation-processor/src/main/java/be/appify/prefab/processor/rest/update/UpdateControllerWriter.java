package be.appify.prefab.processor.rest.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static be.appify.prefab.processor.rest.ControllerUtil.operationAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.pathParameterAnnotation;
import static be.appify.prefab.processor.rest.ControllerUtil.requestMapping;
import static be.appify.prefab.processor.rest.ControllerUtil.responseType;
import static be.appify.prefab.processor.rest.ControllerUtil.securedAnnotation;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.text.WordUtils.capitalize;

class UpdateControllerWriter {
    MethodSpec updateMethod(ClassManifest manifest, UpdateManifest update, PrefabContext context) {
        var responseType = responseType(manifest);
        var requestParts = update.requestParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder().buildMethodParameter(parameter).stream())
                .toList();
        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + manifest.simpleName() + " ID").ifPresent(idParameter::addAnnotation);
        var method = MethodSpec.methodBuilder(update.operationName())
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(update.method(), "/{id}" + update.path(), requestParts))
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), responseType))
                .addParameter(idParameter.build());
        operationAnnotation(capitalize(update.operationName()) + " " + manifest.simpleName()).ifPresent(method::addAnnotation);
        securedAnnotation(update.security()).ifPresent(method::addAnnotation);
        if (update.requestParameters().isEmpty()) {
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

    MethodSpec updateDispatchMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            List<Map.Entry<ClassManifest, UpdateManifest>> group,
            PrefabContext context
    ) {
        var update = group.getFirst().getValue();
        var operationName = capitalize(update.operationName());
        var unionName = "%s%sRequest".formatted(polymorphic.simpleName(), operationName);
        var unionClass = ClassName.get(polymorphic.packageName() + ".application", unionName);
        var responseType = responseType(polymorphic);

        var switchCases = group.stream()
                .map(e -> buildUpdateDispatchCase(polymorphic, unionName, operationName, e))
                .collect(CodeBlock.joining(";\n"));

        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + polymorphic.simpleName() + " ID").ifPresent(idParameter::addAnnotation);

        var method = MethodSpec.methodBuilder(uncapitalize(update.operationName()))
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(update.method(), "/{id}" + update.path(), List.of()))
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), responseType))
                .addParameter(idParameter.build());
        operationAnnotation(capitalize(update.operationName()) + " " + polymorphic.simpleName()).ifPresent(method::addAnnotation);
        securedAnnotation(update.security()).ifPresent(method::addAnnotation);
        method.addParameter(ParameterSpec.builder(unionClass, "request")
                .addAnnotation(Valid.class)
                .addAnnotation(AnnotationSpec.builder(RequestBody.class).build())
                .build());
        method.addStatement("return toResponse(switch (request) {\n$L;\n})", switchCases);
        return method.build();
    }

    private static CodeBlock buildUpdateDispatchCase(
            PolymorphicAggregateManifest polymorphic,
            String unionName,
            String operationName,
            Map.Entry<ClassManifest, UpdateManifest> entry
    ) {
        var leafName = leafName(entry.getKey().simpleName());
        var nestedClass = ClassName.get(polymorphic.packageName() + ".application", unionName,
                "%s%sRequest".formatted(leafName, operationName));
        var flatClass = ClassName.get(polymorphic.packageName() + ".application",
                "%s%sRequest".formatted(leafName, operationName));
        var paramNames = entry.getValue().requestParameters().stream()
                .map(p -> "r." + p.name() + "()")
                .collect(Collectors.joining(", "));
        var serviceMethod = uncapitalize(leafName + operationName);
        return CodeBlock.of("case $T r -> service.$L(id, new $T($L))", nestedClass, serviceMethod, flatClass, paramNames);
    }

    MethodSpec updateMethodForPolymorphic(
            PolymorphicAggregateManifest polymorphic,
            ClassManifest subtype,
            UpdateManifest update,
            PrefabContext context
    ) {
        var leafName = leafName(subtype.simpleName());
        var responseType = responseType(polymorphic);
        var requestParts = update.requestParameters().stream()
                .flatMap(parameter -> context.requestParameterBuilder().buildMethodParameter(parameter).stream())
                .toList();
        var idParameter = ParameterSpec.builder(String.class, "id")
                .addAnnotation(PathVariable.class);
        pathParameterAnnotation("The " + polymorphic.simpleName() + " ID").ifPresent(idParameter::addAnnotation);
        var operationName = leafName + capitalize(update.operationName());
        var method = MethodSpec.methodBuilder(uncapitalize(operationName))
                .addModifiers(PUBLIC)
                .addAnnotation(requestMapping(update.method(), "/{id}" + update.path(), requestParts))
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), responseType))
                .addParameter(idParameter.build());
        operationAnnotation(capitalize(update.operationName()) + " " + leafName).ifPresent(method::addAnnotation);
        securedAnnotation(update.security()).ifPresent(method::addAnnotation);
        if (update.requestParameters().isEmpty()) {
            method.addStatement("return toResponse(service.$N(id))", uncapitalize(operationName));
        } else {
            method.addParameter(ParameterSpec.builder(
                            ClassName.get("%s.application".formatted(polymorphic.packageName()),
                                    "%s%sRequest".formatted(leafName, capitalize(update.operationName()))),
                            "request")
                    .addAnnotation(Valid.class)
                    .addAnnotation(requestParts.isEmpty()
                            ? AnnotationSpec.builder(RequestBody.class).build()
                            : AnnotationSpec.builder(RequestPart.class)
                                    .addMember("name", "$S", "body")
                                    .build())
                    .build());
            requestParts.forEach(method::addParameter);
            method.addStatement("return toResponse(service.$N(id, request$L))", uncapitalize(operationName),
                    requestParts.stream()
                            .map(param -> ".with%s(%s)".formatted(capitalize(param.name()), param.name()))
                            .collect(Collectors.joining(", ")));
        }
        return method.build();
    }

    private static String leafName(String simpleName) {
        var dotIndex = simpleName.lastIndexOf('.');
        return dotIndex >= 0 ? simpleName.substring(dotIndex + 1) : simpleName;
    }

    private static String uncapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
