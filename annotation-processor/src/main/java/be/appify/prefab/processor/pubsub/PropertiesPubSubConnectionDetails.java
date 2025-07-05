package be.appify.prefab.processor.pubsub;

import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;

public class PropertiesPubSubConnectionDetails implements PubSubConnectionDetails {
    private final GcpPubSubProperties properties;

    public PropertiesPubSubConnectionDetails(GcpPubSubProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getEmulatorHost() {
        return properties.getEmulatorHost();
    }
}
