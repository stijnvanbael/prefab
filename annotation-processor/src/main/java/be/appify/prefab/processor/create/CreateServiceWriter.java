package be.appify.prefab.processor.create;

import be.appify.prefab.core.service.AggregateEnvelope;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.RequestParameterMapper;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import jakarta.validation.Valid;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class CreateServiceWriter {
    public MethodSpec createMethod(
            ClassManifest manifest,
            ExecutableElement controller,
            RequestParameterMapper parameterMapper
    ) {
        if (controller.getParameters().isEmpty()) {
            return MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("log.debug($S, $T.class.getSimpleName())", "Creating new {}", manifest.className())
                    .addStatement("var aggregate = new $T()", manifest.type().asTypeName())
                    .addStatement("var envelope = $T.createNew(aggregate)", AggregateEnvelope.class)
                    .addStatement("%sRepository.save(envelope)".formatted(uncapitalize(manifest.simpleName())))
                    .addStatement("return envelope.id()")
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
                                    .map(param -> new VariableManifest(param, manifest.processingEnvironment()))
                                    .map(parameterMapper::mapRequestParameter)
                                    .collect(CodeBlock.joining(", ")))
                    .addStatement("var envelope = $T.createNew(aggregate)", AggregateEnvelope.class)
                    .addStatement("%sRepository.save(envelope)".formatted(uncapitalize(manifest.simpleName())))
                    .addStatement("return envelope.id()")
                    .build();
        }
    }
}