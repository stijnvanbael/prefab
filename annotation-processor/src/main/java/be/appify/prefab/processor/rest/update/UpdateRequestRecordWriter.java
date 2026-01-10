package be.appify.prefab.processor.rest.update;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.RequestParameterBuilder;
import com.palantir.javapoet.ClassName;

import static be.appify.prefab.processor.rest.ControllerUtil.writeRecord;
import static org.apache.commons.text.WordUtils.capitalize;

class UpdateRequestRecordWriter {
    void writeUpdateRequestRecord(
            JavaFileWriter fileWriter,
            ClassManifest manifest,
            UpdateManifest update,
            RequestParameterBuilder parameterBuilder
    ) {
        var name = "%s%sRequest".formatted(manifest.simpleName(), capitalize(update.operationName()));
        var type = writeRecord(ClassName.get(manifest.packageName() + ".application", name), update.parameters(),
                parameterBuilder);
        fileWriter.writeFile(manifest.packageName(), name, type);
    }
}
