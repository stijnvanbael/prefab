package be.appify.prefab.test.inmemory.messaging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.util.ReflectionUtils;

/**
 * Connects {@link KafkaListener} methods to the {@link InMemoryEventBus}, replacing Spring Kafka's listener
 * containers, which are not available in in-memory mode.
 * <p>
 * Topics are resolved the same way Spring Kafka does: property placeholders first, then SpEL expressions. A listener
 * method receives the decoded event as its payload parameter and may declare {@link Header} parameters of type
 * {@code String}, such as the event type header of generic bindings. Each listener method is invoked once per
 * message. A message whose decoded event is not an instance of the payload parameter type is skipped.
 */
public class KafkaListenerSubscriber implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(KafkaListenerSubscriber.class);

    private final ObjectProvider<InMemoryEventBus> bus;
    private final List<Endpoint> endpoints = new ArrayList<>();
    private ConfigurableBeanFactory beanFactory;

    /**
     * Constructs a subscriber.
     *
     * @param bus the bus to subscribe on, resolved lazily so the post-processor does not trigger early
     *            initialization of the bus and its serializers
     */
    public KafkaListenerSubscriber(ObjectProvider<InMemoryEventBus> bus) {
        this.bus = bus;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        var targetClass = AopUtils.getTargetClass(bean);
        MethodIntrospector.selectMethods(targetClass,
                        (MethodIntrospector.MetadataLookup<KafkaListener>) method ->
                                AnnotatedElementUtils.findMergedAnnotation(method, KafkaListener.class))
                .forEach((method, listener) -> endpoints.add(new Endpoint(beanName, method, listener)));
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (endpoints.isEmpty()) {
            return;
        }
        var eventBus = bus.getObject();
        endpoints.forEach(endpoint -> subscribe(eventBus, endpoint));
    }

    private void subscribe(InMemoryEventBus eventBus, Endpoint endpoint) {
        var method = AopUtils.selectInvocableMethod(endpoint.method(), beanFactory.getType(endpoint.beanName()));
        var invoker = new ListenerInvoker(beanFactory.getBean(endpoint.beanName()), method);
        var name = "%s.%s".formatted(endpoint.beanName(), method.getName());
        topics(endpoint.listener()).forEach(topic -> {
            log.debug("Subscribing {} to topic {}", name, topic);
            eventBus.subscribe(topic, name, invoker::invoke);
        });
    }

    private Set<String> topics(KafkaListener listener) {
        var topics = new LinkedHashSet<String>();
        for (var topic : listener.topics()) {
            resolve(topic).forEach(resolved -> topics.add(resolved.toString()));
        }
        if (topics.isEmpty()) {
            throw new IllegalStateException("@KafkaListener without topics is not supported in in-memory mode");
        }
        return topics;
    }

    private Collection<?> resolve(String topic) {
        var placeholdersResolved = beanFactory.resolveEmbeddedValue(topic);
        var expressionResolver = beanFactory.getBeanExpressionResolver();
        var value = expressionResolver == null || placeholdersResolved == null
                ? placeholdersResolved
                : expressionResolver.evaluate(placeholdersResolved, new BeanExpressionContext(beanFactory, null));
        return switch (value) {
            case Collection<?> collection -> collection;
            case Object[] array -> List.of(array);
            case null -> throw new IllegalStateException("Topic [%s] resolved to null".formatted(topic));
            default -> List.of(value);
        };
    }

    private record Endpoint(String beanName, Method method, KafkaListener listener) {
    }

    /** Calls a listener method with the decoded event and the header parameters it declares. */
    private static final class ListenerInvoker {
        private final Object bean;
        private final Method method;

        private ListenerInvoker(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
            ReflectionUtils.makeAccessible(method);
            validate();
        }

        private void validate() {
            var payloadParameters = 0;
            for (var parameter : method.getParameters()) {
                if (isHeader(parameter)) {
                    if (parameter.getType() != String.class) {
                        throw unsupported(parameter);
                    }
                } else {
                    payloadParameters++;
                }
            }
            if (payloadParameters != 1) {
                throw new IllegalStateException(
                        "@KafkaListener method %s must have exactly one payload parameter in in-memory mode"
                                .formatted(method));
            }
        }

        private IllegalStateException unsupported(Parameter parameter) {
            return new IllegalStateException("Unsupported parameter %s of @KafkaListener method %s in in-memory mode"
                    .formatted(parameter, method));
        }

        void invoke(EncodedMessage message) throws Exception {
            var event = message.decode();
            var arguments = new Object[method.getParameterCount()];
            var parameters = method.getParameters();
            for (var i = 0; i < parameters.length; i++) {
                if (isHeader(parameters[i])) {
                    arguments[i] = headerValue(parameters[i], message);
                } else if (!parameters[i].getType().isInstance(event)) {
                    log.debug("Skipping {} for {}: not a {}", method.getName(), event, parameters[i].getType());
                    return;
                } else {
                    arguments[i] = event;
                }
            }
            try {
                method.invoke(bean, arguments);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception cause) {
                    throw cause;
                }
                throw e;
            }
        }

        private static boolean isHeader(Parameter parameter) {
            return parameter.isAnnotationPresent(Header.class) && !parameter.isAnnotationPresent(Payload.class);
        }

        private static String headerValue(Parameter parameter, EncodedMessage message) {
            var annotation = parameter.getAnnotation(Header.class);
            var name = annotation.value().isEmpty() ? annotation.name() : annotation.value();
            var hasDefault = !annotation.defaultValue().equals(ValueConstants.DEFAULT_NONE);
            var value = message.header(name);
            if (value.isEmpty() && hasDefault) {
                return annotation.defaultValue();
            }
            if (value.isEmpty() && annotation.required()) {
                throw new IllegalStateException("Missing header [%s]".formatted(name));
            }
            return value.orElse(null);
        }
    }
}
