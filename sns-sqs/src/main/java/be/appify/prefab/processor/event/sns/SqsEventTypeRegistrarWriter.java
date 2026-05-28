package be.appify.prefab.processor.event.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.sns.SnsPlugin.platformIsSnsSqs;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class SqsEventTypeRegistrarWriter {
    private final PrefabContext context;

    SqsEventTypeRegistrarWriter(PrefabContext context) {
        this.context = context;
    }

    void writeRegistrar(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.sns");
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> platformIsSnsSqs(e, event.asElement(), context))
                .findFirst()
                .orElseThrow();
        var simpleName = event.simpleName().replace(".", "");
        var name = "%sSqsEventTypeRegistrar".formatted(simpleName);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addMethod(constructor(annotation.topic(), annotation.publishTo(), simpleName, event))
                .build();
        fileWriter.writeFile(event.packageName(), name, type);
    }

    private MethodSpec constructor(String[] topics, PublishTo publishTo, String simpleName, TypeManifest event) {
        boolean useIndexedNames = topics.length > 1;
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(SqsUtil.class, "sqsUtil");
        constructor.addStatement("sqsUtil.registerType($T.class.getName(), $T.class)", event.asTypeName(), event.asTypeName());
        for (int i = 0; i < topics.length; i++) {
            var topic = topics[i];
            if (topic.matches("\\$\\{.+}")) {
                var topicFieldName = topicFieldName(simpleName, i, useIndexedNames);
                constructor.addParameter(ParameterSpec.builder(String.class, topicFieldName)
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", topic)
                                .build())
                        .build());
                constructor.addStatement("sqsUtil.registerEventTopic($L, $T.class)", topicFieldName, event.asTypeName());
            } else {
                constructor.addStatement("sqsUtil.registerEventTopic($S, $T.class)", topic, event.asTypeName());
            }
        }
        if (publishTo != PublishTo.FIRST) {
            constructor.addStatement("sqsUtil.registerPublishTo($T.class, $T.$L)",
                    event.asTypeName(), PublishTo.class, publishTo.name());
        }
        return constructor.build();
    }

    private static String topicFieldName(String simpleName, int index, boolean useIndexedNames) {
        var base = uncapitalize(simpleName) + "Topic";
        return useIndexedNames ? base + index : base;
    }
}
