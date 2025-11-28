package be.appify.prefab.core.pubsub;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface PubSubConnectionDetails extends ConnectionDetails {
    String getEmulatorHost();
}
