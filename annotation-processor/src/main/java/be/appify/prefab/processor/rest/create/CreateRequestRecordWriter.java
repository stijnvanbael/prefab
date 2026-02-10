package be.appify.prefab.processor.rest.create;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import javax.lang.model.element.ExecutableElement;

import static be.appify.prefab.processor.rest.ControllerUtil.writeRecord;

class CreateRequestRecordWriter {
    void writeRequestRecord(
            JavaFileWriter fileWriter,
            ClassManifest manifest,
            ExecutableElement controller,
            PrefabContext context
    ) {
        var name = "Create%sRequest".formatted(manifest.simpleName());
        var type = writeRecord(ClassName.get(manifest.packageName() + ".application", name),
                controller.getParameters().stream()
                        .map(param ->
                                VariableManifest.of(param, context.processingEnvironment()))
                        .toList(),
                context.requestParameterBuilder());
        fileWriter.writeFile(manifest.packageName(), name, type);
    }
}
