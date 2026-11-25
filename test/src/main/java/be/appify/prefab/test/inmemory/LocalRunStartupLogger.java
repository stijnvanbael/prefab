package be.appify.prefab.test.inmemory;

import be.appify.prefab.test.inmemory.messaging.InMemoryEventBus;
import be.appify.prefab.test.inmemory.persistence.InMemoryStore;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

final class LocalRunStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRunStartupLogger.class);
    private static final String OPEN_API_CONFIGURATION = "be.appify.prefab.openapi.OpenApiAutoConfiguration";
    private static final String ASYNC_API_CONFIGURATION = "be.appify.prefab.asyncapi.AsyncApiConfiguration";

    private final Environment environment;

    LocalRunStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        var port = Optional.ofNullable(environment.getProperty("local.server.port", Integer.class));
        if (port.isEmpty()) {
            return;
        }

        var baseUrl = baseUrl(port.orElseThrow());
        LOGGER.info("Prefab local application is ready at {}", baseUrl);
        if (hasClass(OPEN_API_CONFIGURATION, context)) {
            LOGGER.info("OpenAPI: {}/v3/api-docs; Swagger UI: {}/swagger-ui/index.html", baseUrl, baseUrl);
        }
        if (hasClass(ASYNC_API_CONFIGURATION, context)) {
            LOGGER.info("AsyncAPI: {}/async-api/asyncapi.json; viewer: {}/async-api", baseUrl, baseUrl);
        }

        var persistence = context.getBeanNamesForType(InMemoryStore.class).length > 0
                ? "in-memory repositories" : "no in-memory repository backend";
        var messaging = context.getBeanNamesForType(InMemoryEventBus.class).length > 0
                ? "in-process event bus" : "no in-memory event bus";
        LOGGER.info("Prefab local backends: {}, {}", persistence, messaging);
    }

    private String baseUrl(int port) {
        var host = environment.getProperty("server.address", "localhost");
        if ("0.0.0.0".equals(host) || "::".equals(host) || "0:0:0:0:0:0:0:0".equals(host)) {
            host = "localhost";
        } else if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        var protocol = environment.getProperty("server.ssl.enabled", Boolean.class, false) ? "https" : "http";
        return "%s://%s:%d".formatted(protocol, host, port);
    }

    private static boolean hasClass(String name, ApplicationContext context) {
        return ClassUtils.isPresent(name, context.getClassLoader());
    }
}
