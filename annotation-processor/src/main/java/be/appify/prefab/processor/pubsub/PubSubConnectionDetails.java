package be.appify.prefab.processor.pubsub;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface PubSubConnectionDetails extends ConnectionDetails {
    String getEmulatorHost();
}
