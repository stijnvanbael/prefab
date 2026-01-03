package be.appify.prefab.core.pubsub;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/** Connection details for Pub/Sub services. */
public interface PubSubConnectionDetails extends ConnectionDetails {
    /**
     * Gets the emulator host for Pub/Sub.
     *
     * @return the emulator host
     */
    String getEmulatorHost();
}
