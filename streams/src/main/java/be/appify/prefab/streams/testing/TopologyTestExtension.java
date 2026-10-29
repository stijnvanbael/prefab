package be.appify.prefab.streams.testing;

import be.appify.prefab.streams.PrefabStreams;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that sets up a fresh topology test environment for every test.
 *
 * <p>Fields and parameters annotated with {@link TestStreams} receive the {@link PrefabStreams} to declare the topology
 * on and the {@link TopologySession} to run it. The session starts on first use and is closed after the test.
 *
 * <pre>
 * &#64;ExtendWith(TopologyTestExtension.class)
 * class FilterTest {
 *     &#64;TestStreams PrefabStreams streams;
 *     &#64;TestStreams TopologySession topology;
 *
 *     &#64;Test
 *     void filter() {
 *         streams.from(Order.class).filter(Order::isValid).to(ValidOrder.class);
 *         topology.input(Order.class).send(new Order(...));
 *         assertThat(topology.output(ValidOrder.class).values()).hasSize(1);
 *     }
 * }
 * </pre>
 *
 * <p>Register the extension with {@code @RegisterExtension} and {@link #withAppId(String)} to use another application
 * id than the default.
 */
public final class TopologyTestExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final Namespace NAMESPACE = Namespace.create(TopologyTestExtension.class);
    private static final String DEFAULT_APP_ID = "test";

    private final String appId;

    /** Constructs an extension that uses the default application id. */
    public TopologyTestExtension() {
        this(DEFAULT_APP_ID);
    }

    private TopologyTestExtension(String appId) {
        this.appId = appId;
    }

    /**
     * Creates an extension that uses a specific application id.
     *
     * @param appId the application id, used to derive topic names
     * @return the extension
     */
    public static TopologyTestExtension withAppId(String appId) {
        return new TopologyTestExtension(appId);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws IllegalAccessException {
        var environment = environment(context);
        for (var instance : context.getRequiredTestInstances().getAllInstances()) {
            for (var field : annotatedFields(instance.getClass())) {
                field.setAccessible(true);
                field.set(instance, resolve(field.getType(), environment));
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var environment = context.getStore(NAMESPACE).remove(Environment.class, Environment.class);
        if (environment != null) {
            environment.session().close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(TestStreams.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return resolve(parameterContext.getParameter().getType(), environment(extensionContext));
    }

    private Environment environment(ExtensionContext context) {
        return context.getStore(NAMESPACE).computeIfAbsent(Environment.class, key -> createEnvironment(), Environment.class);
    }

    private Environment createEnvironment() {
        var harness = TopologyTestHarness.create(appId);
        return new Environment(harness.streams(), new LazyTopologySession(harness::start));
    }

    private static Object resolve(Class<?> type, Environment environment) {
        if (type.isAssignableFrom(PrefabStreams.class)) {
            return environment.streams();
        }
        if (type.isAssignableFrom(TopologySession.class)) {
            return environment.session();
        }
        throw new ParameterResolutionException(
                "@TestStreams supports PrefabStreams and TopologySession, but found " + type.getName());
    }

    private static Field[] annotatedFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(TestStreams.class))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);
    }

    private record Environment(PrefabStreams streams, TopologySession session) {
    }
}

