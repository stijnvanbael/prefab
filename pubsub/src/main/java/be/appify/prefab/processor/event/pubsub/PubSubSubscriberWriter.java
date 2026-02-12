package be.appify.prefab.processor.event.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import be.appify.prefab.processor.CaseUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.ConsumerWriterSupport;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

import static be.appify.prefab.core.annotations.EventHandlerConfig.Util.hasCustomDeadLetterTopic;
import static be.appify.prefab.core.annotations.EventHandlerConfig.Util.hasCustomRetries;
import static be.appify.prefab.processor.event.ConsumerWriterSupport.concurrencyExpression;
import static java.util.stream.Collectors.groupingBy;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class PubSubSubscriberWriter {
    private static final ConsumerWriterSupport support = new ConsumerWriterSupport(Event.Platform.PUB_SUB);
    private final PrefabContext context;

    PubSubSubscriberWriter(PrefabContext context) {
        this.context = context;
    }

    void writePubSubSubscriber(
            TypeManifest owner,
            List<ExecutableElement> eventHandlers
    ) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.pubsub");

        var name = "%sPubSubSubscriber".formatted(owner.simpleName());
        var packageName = TypeManifest.of(eventHandlers.getFirst().getEnclosingElement().asType(),
                context.processingEnvironment()).packageName();
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Component.class)
                .addModifiers(PUBLIC)
                .addField(FieldSpec.builder(Logger.class, "log", PRIVATE, STATIC, FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(packageName + ".infrastructure.pubsub", name))
                        .build())
                .addField(FieldSpec.builder(Executor.class, "executor", PRIVATE, FINAL)
                        .build());

        var fields = support.addFields(eventHandlers, context, type);
        addEventHandlers(eventHandlers, type);
        var topics = eventHandlers.stream()
                .map(e -> support.rootEventType(e, context).annotationsOfType(Event.class).stream().findFirst()
                        .orElseThrow()
                        .topic())
                .collect(Collectors.toSet());
        type.addMethod(constructor(topics, owner, fields, eventHandlers));
        fileWriter.writeFile(packageName, name, type.build());
    }

    private void addEventHandlers(
            List<ExecutableElement> allEventHandlers,
            TypeSpec.Builder type
    ) {
        var eventHandlersByEventType = allEventHandlers.stream()
                .collect(groupingBy(e -> support.rootEventType(e, context)));
        for (Map.Entry<TypeManifest, List<ExecutableElement>> eventHandlersForEvent : eventHandlersByEventType.entrySet()) {
            var eventType = eventHandlersForEvent.getKey();
            var method = MethodSpec.methodBuilder("on%s".formatted(eventType.simpleName().replace(".", "")))
                    .addModifiers(PRIVATE)
                    .addParameter(eventType.asTypeName(), "event")
                    .addStatement("log.debug($S, event)", "Received event {}");
            support.writeEventHandler(context, type, eventHandlersForEvent, eventType, method);
        }
    }

    private MethodSpec constructor(
            Set<String> topics,
            TypeManifest owner,
            Set<FieldSpec> fields,
            List<ExecutableElement> eventHandlers
    ) {
        var constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        var config = owner.inheritedAnnotationsOfType(EventHandlerConfig.class).stream().findFirst().orElse(null);
        var concurrency = concurrencyExpression(owner);
        if (concurrency.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, "concurrency")
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", concurrency)
                            .build())
                    .build());
        }
        constructor.addStatement("executor = $T.newFixedThreadPool($L)", ClassName.get(Executors.class),
                concurrency.matches("\\$\\{.+}") ? "Integer.parseInt(concurrency)" : concurrency);
        fields.forEach(field -> constructor.addParameter(ParameterSpec.builder(field.type(), field.name()).build()));
        constructor.addParameter(PubSubUtil.class, "pubSub");
        if (hasCustomRetries(config)) {
            if (config.retryLimit().matches("\\$\\{.+}")) {
                constructor.addParameter(configParameter(Integer.class, "maxRetries", config.retryLimit()));
            }
            if (config.minimumBackoffMs().matches("\\$\\{.+}")) {
                constructor.addParameter(configParameter(Long.class, "initialRetryInterval", config.minimumBackoffMs()));
            }
            if (config.maximumBackoffMs().matches("\\$\\{.+}")) {
                constructor.addParameter(configParameter(Long.class, "maxRetryInterval", config.maximumBackoffMs()));
            }
            if (config.backoffMultiplier().matches("\\$\\{.+}")) {
                constructor.addParameter(configParameter(Double.class, "backoffMultiplier", config.backoffMultiplier()));
            }
        }
        topics.forEach(topic -> addTopic(owner, eventHandlers, topic, constructor));
        fields.forEach(field -> constructor.addStatement("this.$L = $L", field.name(), field.name()));
        return constructor.build();
    }

    private void addTopic(
            TypeManifest owner,
            List<ExecutableElement> eventHandlers,
            String topic,
            MethodSpec.Builder constructor
    ) {
        var eventType = support.eventTypeOf(eventHandlers, context, topic);
        var topicVariableName = uncapitalize(eventType.simpleName().replace(".", "")) + "Topic";
        var eventName = eventType.simpleName().replace(".", "");
        if (topic.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, topicVariableName)
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", topic)
                            .build())
                    .build());
        }
        var eventHandlerConfig = owner.inheritedAnnotationsOfType(EventHandlerConfig.class).stream().findFirst().orElse(null);
        if (hasCustomDeadLetterTopic(eventHandlerConfig) && eventHandlerConfig.deadLetterTopic().matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, "deadLetterTopic")
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", eventHandlerConfig.deadLetterTopic())
                            .build())
                    .build());
        }
        constructor.addStatement("""
                        pubSub.subscribe(new $T($L, $S, $T.class, this::on$L)
                        .withExecutor(executor)$L)""",
                ParameterizedTypeName.get(ClassName.get(SubscriptionRequest.class),
                        eventType.asTypeName()),
                topic.matches("\\$\\{.+}") ? topicVariableName : CodeBlock.of("$S", topic),
                CaseUtil.toKebabCase(owner.simpleName()) + "-on-" + CaseUtil.toKebabCase(
                        eventName),
                eventType.asTypeName(),
                eventName,
                deadLetterPolicy(eventHandlerConfig));
    }

    private static ParameterSpec configParameter(Class<?> type, String name, String value) {
        return ParameterSpec.builder(type, name)
                .addAnnotation(AnnotationSpec.builder(Value.class)
                        .addMember("value", "$S", value)
                        .build())
                .build();
    }

    private static CodeBlock deadLetterPolicy(EventHandlerConfig eventHandlerConfig) {
        if (eventHandlerConfig != null) {
            var codeBlock = CodeBlock.builder();
            if (hasCustomDeadLetterTopic(eventHandlerConfig)) {
                codeBlock.add(CodeBlock.of("""
                                
                                .withDeadLetterPolicy($T.newBuilder()
                                    .setDeadLetterTopic($L)
                                    .build())""",
                        DeadLetterPolicy.class,
                        eventHandlerConfig.deadLetterTopic().matches("\\$\\{.+}")
                                ? "deadLetterTopic"
                                : CodeBlock.of("$S", eventHandlerConfig.deadLetterTopic())
                ));
            } else if (!eventHandlerConfig.deadLetteringEnabled()) {
                codeBlock.add(CodeBlock.of("""
                        
                        .withDeadLetterPolicy(null)"""));
            }
            if (hasCustomRetries(eventHandlerConfig)) {
                codeBlock.add(CodeBlock.of("""
                                
                                .withRetryTemplate(new $T($T.builder()
                                        .maxRetries($L)
                                        .delay($T.ofMillis($L))
                                        .maxDelay($T.ofMillis($L))
                                        .multiplier($L)
                                        .build()))""",
                        RetryTemplate.class,
                        RetryPolicy.class,
                        eventHandlerConfig.retryLimit().matches("\\$\\{.+}") ? "maxRetries" : eventHandlerConfig.retryLimit(),
                        Duration.class,
                        eventHandlerConfig.minimumBackoffMs().matches("\\$\\{.+}")
                                ? "initialRetryInterval"
                                : eventHandlerConfig.minimumBackoffMs() + "L",
                        Duration.class,
                        eventHandlerConfig.maximumBackoffMs().matches("\\$\\{.+}")
                                ? "maxRetryInterval"
                                : eventHandlerConfig.maximumBackoffMs() + "L",
                        eventHandlerConfig.backoffMultiplier().matches("\\$\\{.+}")
                                ? "backoffMultiplier"
                                : eventHandlerConfig.backoffMultiplier()
                ));
            }
            return codeBlock.build();
        }
        return CodeBlock.of("");
    }
}
