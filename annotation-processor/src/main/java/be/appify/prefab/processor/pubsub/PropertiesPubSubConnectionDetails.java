package be.appify.prefab.processor.pubsub;

import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;

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
