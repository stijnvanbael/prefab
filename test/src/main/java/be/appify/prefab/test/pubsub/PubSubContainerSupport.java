package be.appify.prefab.test.pubsub;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PubSubEmulatorContainer;

/**
 * Support interface to provide a Pub/Sub emulator container for tests.
 * Implement this interface to have access to a shared Pub/Sub emulator container.
 */
public interface PubSubContainerSupport {

    /** The Pub/Sub emulator container. */
    @ServiceConnection
    PubSubEmulatorContainer gCloudContainer = new PubSubEmulatorContainer("gcr.io/google.com/cloudsdktool/cloud-sdk:529.0.0-emulators")
            .withReuse(true)
            .withExposedPorts(8085, 8086);

    /** Starts the Pub/Sub emulator container before all tests. */
    @BeforeAll
    static void beforeAll() {
        if (!gCloudContainer.isRunning()) {
            gCloudContainer.start();
        }
    }
}
