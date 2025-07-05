package be.appify.prefab.processor.delete;

import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

public class DeleteServiceWriter {
    public MethodSpec deleteMethod(ClassManifest manifest) {
        return MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Deleting {} with id: {}", manifest.className())
                .addStatement("%sRepository.deleteById(id)".formatted(uncapitalize(manifest.simpleName())))
                .build();
    }
}
