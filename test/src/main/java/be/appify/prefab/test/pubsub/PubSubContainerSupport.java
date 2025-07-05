package be.appify.prefab.test.pubsub;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PubSubEmulatorContainer;

public interface PubSubContainerSupport {
    @ServiceConnection
    PubSubEmulatorContainer gCloudContainer = new PubSubEmulatorContainer("gcr.io/google.com/cloudsdktool/cloud-sdk:529.0.0-emulators")
            .withReuse(true)
            .withExposedPorts(8085, 8086);

    @BeforeAll
    static void beforeAll() {
        if (!gCloudContainer.isRunning()) {
            gCloudContainer.start();
        }
    }
}
