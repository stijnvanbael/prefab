package be.appify.prefab.processor.create;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.RequestParameterBuilder;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import static org.apache.commons.text.WordUtils.capitalize;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.List;

public class CreateRequestRecordWriter {
    public void writeRequestRecord(
            JavaFileWriter fileWriter,
            ClassManifest manifest,
            ExecutableElement controller,
            PrefabContext context
    ) {
        var name = "Create%sRequest".formatted(manifest.simpleName());
        var type = writeRecord(ClassName.get(manifest.packageName() + ".application", name),
                controller.getParameters().stream()
                        .map(param ->
                                new VariableManifest(param, context.processingEnvironment()))
                        .toList(),
                context.requestParameterBuilder());
        fileWriter.writeFile(manifest.packageName(), name, type);
    }

    private TypeSpec writeRecord(ClassName name, List<VariableManifest> fields,
            RequestParameterBuilder parameterBuilder) {
        var type = TypeSpec.recordBuilder(name.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(fields.stream()
                                .flatMap(param -> parameterBuilder.buildBodyParameter(param)
                                        .or(() -> parameterBuilder.buildMethodParameter(param))
                                        .stream())
                                .toList())
                        .build());
        type.addMethods(fields.stream()
                .flatMap(parameter -> parameterBuilder.buildMethodParameter(parameter).stream())
                .map(parameter -> withMethod(name, parameter, fields))
                .toList());
        return type.build();
    }

    private MethodSpec withMethod(TypeName type, ParameterSpec parameter, List<VariableManifest> fields) {
        return MethodSpec.methodBuilder("with%s".formatted(capitalize(parameter.name())))
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addParameter(parameter)
                .addStatement("return new $T($L)", type, fields.stream()
                        .map(VariableManifest::name)
                        .collect(java.util.stream.Collectors.joining(", ")))
                .build();
    }
}
