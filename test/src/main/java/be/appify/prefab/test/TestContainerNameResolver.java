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
     * Resolves the name of a container that is shared by all modules and applications.
     *
     * <p>Unlike {@link #resolveContainerName}, the default name does not depend on the application, so that
     * every module reuses the same running container (provided Testcontainers reuse is enabled).
     *
     * @param propertyResolver the property resolver to fetch configuration values
     * @param containerType the type of container (e.g. "postgres", "kafka")
     * @param customNameProperty the property key to check for a custom name
     * @return the custom name if configured, otherwise {@code prefab_<containerType>}
     */
    public static String resolveSharedContainerName(
            PropertyResolver propertyResolver,
            String containerType,
            String customNameProperty) {
        var customName = propertyResolver.getProperty(customNameProperty);
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        return "prefab_" + containerType;
    }

    /**
     * Converts the application name into an identifier that is safe to use as a database name.
     *
     * @param propertyResolver the property resolver to fetch the application name
     * @return the sanitised application name
     */
    public static String resolveApplicationIdentifier(PropertyResolver propertyResolver) {
        var appName = propertyResolver.getProperty("spring.application.name", "application");
        return appName.toLowerCase().replaceAll("[.\\-]", "_");
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
     * Removes an existing Docker container with the target name unless it can be reused as-is.
     *
     * <p>A running container that was created from the expected image is kept so that it can be shared
     * across contexts and modules; anything else (stopped, or created from another image) is removed to
     * avoid a Docker 409 conflict.</p>
     *
     * @param containerName the container name without leading slash
     * @param image the image the container is expected to run
     */
    public static void removeStaleContainer(String containerName, String image) {
        var dockerClient = DockerClientFactory.instance().client();
        var dockerName = "/" + containerName;
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container -> container.getNames() != null
                        && Arrays.asList(container.getNames()).contains(dockerName))
                .filter(container -> !"running".equals(container.getState()) || !image.equals(container.getImage()))
                .forEach(container -> dockerClient.removeContainerCmd(container.getId()).withForce(true).exec());
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

