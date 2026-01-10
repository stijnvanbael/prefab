package be.appify.prefab.processor.rest.delete;

import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.MethodSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static org.apache.commons.text.WordUtils.uncapitalize;

class DeleteServiceWriter {
    MethodSpec deleteMethod(ClassManifest manifest) {
        return MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Deleting {} with id: {}",
                        manifest.className())
                .addStatement("%sRepository.deleteById(id)".formatted(uncapitalize(manifest.simpleName())))
                .build();
    }

    public MethodSpec deleteMethod(ClassManifest manifest, ExecutableElement method) {
        return MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .addStatement("log.debug($S, $T.class.getSimpleName(), id)", "Deleting {} with id: {}",
                        manifest.className())
                .addStatement("%sRepository.findById(id).ifPresent(aggregate -> aggregate.%s())"
                        .formatted(uncapitalize(manifest.simpleName()), method.getSimpleName()))
                .addStatement("%sRepository.deleteById(id)".formatted(uncapitalize(manifest.simpleName())))
                .build();
    }
}
