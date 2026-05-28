package be.appify.prefab.test;

import java.util.Arrays;

import com.github.dockerjava.api.model.Container;
import org.springframework.core.env.PropertyResolver;
import org.testcontainers.DockerClientFactory;

/**
 * Utility for resolving test container names.
 *
 * <p>Provides a consistent mechanism for computing fixed, predictable Docker container names
 * across all test infrastructure. Names default to {@code prefab-<type>-<appName>} where
 * {@code appName} is derived from {@code spring.application.name}.
 */
public final class TestContainerNameResolver {

    private TestContainerNameResolver() {
    }

    /**
     * Resolves the container name for a given container type.
     *
     * <p>If a custom name is provided via the property, it is returned as-is. Otherwise, a default
     * name is generated based on the application name.
     *
     * @param propertyResolver the property resolver to fetch configuration values
     * @param containerType the type of container (e.g. "postgres", "kafka")
     * @param customNameProperty the property key to check for a custom name (e.g. "prefab.test.postgres.container-name")
     * @return the resolved container name
     */
    public static String resolveContainerName(
            PropertyResolver propertyResolver,
            String containerType,
            String customNameProperty) {
        var customName = propertyResolver.getProperty(customNameProperty);
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        return generateDefaultName(propertyResolver, containerType);
    }

    /**
     * Removes an existing Docker container that already uses the target name.
     *
     * <p>When fixed names are used, Testcontainers may try to create a new container whose configuration
     * hash differs from an already existing one (for example due to network differences), which leads to
     * a Docker 409 conflict. Removing the stale container avoids startup failures on repeated runs.</p>
     *
     * @param containerName the container name without leading slash
     */
    public static void removeConflictingContainer(String containerName) {
        var dockerClient = DockerClientFactory.instance().client();
        var dockerName = "/" + containerName;
        var existingContainerId = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container -> Arrays.asList(container.getNames() == null ? new String[0] : container.getNames()).contains(dockerName))
                .map(Container::getId)
                .findFirst();

        existingContainerId.ifPresent(containerId -> dockerClient.removeContainerCmd(containerId)
                .withForce(true)
                .exec());
    }

    /**
     * Generates a default container name from the application name.
     *
     * <p>The default name follows the pattern {@code <type>_<appName>}, where {@code appName}
     * is the value of {@code spring.application.name} with dots and dashes replaced by underscores.
     *
     * @param propertyResolver the property resolver to fetch the application name
     * @param containerType the type of container
     * @return the generated default name
     */
    private static String generateDefaultName(PropertyResolver propertyResolver, String containerType) {
        var appName = propertyResolver.getProperty("spring.application.name", "application");
        var sanitisedName = appName.toLowerCase().replaceAll("[.\\-]", "_");
        return "%s_%s".formatted(containerType, sanitisedName);
    }
}

