package be.appify.prefab.processor.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.RequestParameterBuilder;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

import static org.apache.commons.text.WordUtils.capitalize;

public class UpdateRequestRecordWriter {
    public void writeUpdateRequestRecord(
            JavaFileWriter fileWriter,
            ClassManifest manifest,
            UpdateManifest update,
            RequestParameterBuilder parameterBuilder
    ) {
        var name = "%s%sRequest".formatted(manifest.simpleName(), capitalize(update.operationName()));
        var type = writeRecord(ClassName.get(manifest.packageName() + ".application", name), update.parameters(), parameterBuilder);
        fileWriter.writeFile(manifest.packageName(), name, type);
    }

    private TypeSpec writeRecord(ClassName name, List<VariableManifest> fields, RequestParameterBuilder parameterBuilder) {
        var type = TypeSpec.recordBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(fields.stream()
                                .flatMap(param -> parameterBuilder.buildMethodParameter(param)
                                        .or(() -> parameterBuilder.buildBodyParameter(param))
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
