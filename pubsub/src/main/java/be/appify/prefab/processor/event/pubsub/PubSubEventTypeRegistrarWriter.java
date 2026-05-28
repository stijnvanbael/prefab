package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.ConsumerWriterSupport.keyField;
import static be.appify.prefab.processor.event.pubsub.PubSubPlugin.platformIsPubSub;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class PubSubEventTypeRegistrarWriter {
    private final PrefabContext context;

    PubSubEventTypeRegistrarWriter(PrefabContext context) {
        this.context = context;
    }

    void writeRegistrar(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.pubsub");
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> platformIsPubSub(e, event.asElement(), context))
                .findFirst()
                .orElseThrow();
        var simpleName = event.simpleName().replace(".", "");
        var name = "%sPubSubEventTypeRegistrar".formatted(simpleName);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addMethod(constructor(annotation.topic(), annotation.publishTo(), simpleName, event, keyField(event, context)))
                .build();
        fileWriter.writeFile(event.packageName(), name, type);
    }

    private MethodSpec constructor(String[] topics, PublishTo publishTo, String simpleName, TypeManifest event, Optional<CodeBlock> keyExtractor) {
        boolean useIndexedNames = topics.length > 1;
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(PubSubUtil.class, "pubSubUtil");
        constructor.addStatement("pubSubUtil.registerType($T.class.getName(), $T.class)", event.asTypeName(), event.asTypeName());
        for (int i = 0; i < topics.length; i++) {
            var topic = topics[i];
            if (topic.matches("\\$\\{.+}")) {
                var topicFieldName = topicFieldName(simpleName, i, useIndexedNames);
                constructor.addParameter(ParameterSpec.builder(String.class, topicFieldName)
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", topic)
                                .build())
                        .build());
                if (keyExtractor.isPresent()) {
                    constructor.addStatement("pubSubUtil.registerEventTopic($L, $T.class, event -> $L)",
                            topicFieldName, event.asTypeName(), keyExtractor.get());
                } else {
                    constructor.addStatement("pubSubUtil.registerEventTopic($L, $T.class)", topicFieldName, event.asTypeName());
                }
            } else {
                if (keyExtractor.isPresent()) {
                    constructor.addStatement("pubSubUtil.registerEventTopic($S, $T.class, event -> $L)",
                            topic, event.asTypeName(), keyExtractor.get());
                } else {
                    constructor.addStatement("pubSubUtil.registerEventTopic($S, $T.class)", topic, event.asTypeName());
                }
            }
        }
        if (publishTo != PublishTo.FIRST) {
            constructor.addStatement("pubSubUtil.registerPublishTo($T.class, $T.$L)",
                    event.asTypeName(), PublishTo.class, publishTo.name());
        }
        return constructor.build();
    }

    private static String topicFieldName(String simpleName, int index, boolean useIndexedNames) {
        var base = uncapitalize(simpleName) + "Topic";
        return useIndexedNames ? base + index : base;
    }
}
