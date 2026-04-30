package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import org.springframework.stereotype.Component;

class AvroSupport {
    private AvroSupport() {
    }

    static AnnotationSpec componentAnnotation(TypeManifest event, String name) {
        return AnnotationSpec.builder(Component.class)
                .addMember("value", "$S", event.packageName().replace('.', '_') + "_" + name)
                .build();
    }
}
