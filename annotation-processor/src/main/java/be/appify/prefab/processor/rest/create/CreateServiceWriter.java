package be.appify.prefab.processor.rest.create;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

class CreateServiceWriter {
    MethodSpec createMethod(
            ClassManifest manifest,
            ExecutableElement controller,
            PrefabContext context
    ) {
        if (controller.getParameters().isEmpty()) {
            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className())
                    .addStatement("var aggregate = new $T()", manifest.type().asTypeName())
                    .addStatement("%sRepository.save(aggregate)".formatted(uncapitalize(manifest.simpleName())))
                    .addStatement("return aggregate.$N()", manifest.idField().map(VariableManifest::name).orElse("id"))
                    .build();
        } else {
            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(
                                    ClassName.get("%s.application".formatted(manifest.packageName()),
                                            "Create%sRequest".formatted(manifest.simpleName())), "request")
                            .addAnnotation(Valid.class)
                            .build())
                    .returns(String.class)
                    .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className())
                    .addStatement("var aggregate = new $T($L)", manifest.type().asTypeName(),
                            controller.getParameters().stream()
                                    .map(param -> new VariableManifest(param, context.processingEnvironment()))
                                    .map(context.requestParameterMapper()::mapRequestParameter)
                                    .collect(CodeBlock.joining(", ")))
                    .addStatement("%sRepository.save(aggregate)".formatted(uncapitalize(manifest.simpleName())))
                    .addStatement("return aggregate.$N()", manifest.idField().map(VariableManifest::name).orElse("id"))
                    .build();
        }
    }
}