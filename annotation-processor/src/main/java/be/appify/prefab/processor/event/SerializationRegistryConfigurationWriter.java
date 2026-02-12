package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.util.SerializationRegistry;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static be.appify.prefab.processor.CaseUtil.toCamelCase;

class SerializationRegistryConfigurationWriter {
    private final PrefabContext context;

    SerializationRegistryConfigurationWriter(PrefabContext context) {
        this.context = context;
    }

    void writeConfiguration(List<TypeManifest> events) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.event");

        var type = TypeSpec.classBuilder("SerializationRegistryConfiguration")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Configuration.class)
                .addAnnotation(AnnotationSpec.builder(Order.class)
                        .addMember("value", "0")
                        .build())
                .addMethod(constructor(events));

        var rootPackage = findCommonRootPackage(events);
        fileWriter.writeFile(rootPackage, "SerializationRegistryConfiguration", type.build());
    }

    private static String findCommonRootPackage(List<TypeManifest> events) {
        return events.stream()
                .map(TypeManifest::packageName)
                .reduce((package1, package2) -> {
                    var parts1 = package1.split("\\.");
                    var parts2 = package2.split("\\.");
                    var minLength = Math.min(parts1.length, parts2.length);
                    var commonParts = new StringBuilder();
                    for (int i = 0; i < minLength; i++) {
                        if (parts1[i].equals(parts2[i])) {
                            if (!commonParts.isEmpty()) {
                                commonParts.append(".");
                            }
                            commonParts.append(parts1[i]);
                        } else {
                            break;
                        }
                    }
                    return commonParts.toString();
                }).orElseThrow();
    }

    private static MethodSpec constructor(List<TypeManifest> events) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(SerializationRegistry.class, "registry");
        events.stream()
                .map(event -> event.inheritedAnnotationsOfType(Event.class).stream().findFirst().orElseThrow())
                .map(event -> new EventConfig(event.topic(), event.serialization()))
                .distinct()
                .forEach(event -> {
                    if (event.topic().matches("\\$\\{.+}")) {
                        var topicName = toCamelCase(event.topic().replaceAll("[^\\w._]", ""));
                        constructor.addParameter(ParameterSpec.builder(String.class, topicName)
                                .addAnnotation(AnnotationSpec.builder(Value.class)
                                        .addMember("value", "$S", event.topic())
                                        .build())
                                .build());
                        constructor.addStatement("registry.register($L, $T.$L)",
                                topicName,
                                Event.Serialization.class,
                                event.serialization.toString());
                    } else {
                        constructor.addStatement("registry.register($S, $T.$L)",
                                event.topic(),
                                Event.Serialization.class,
                                event.serialization.toString());
                    }
                });
        return constructor.build();
    }

    private record EventConfig(String topic, Event.Serialization serialization) {
    }
}
