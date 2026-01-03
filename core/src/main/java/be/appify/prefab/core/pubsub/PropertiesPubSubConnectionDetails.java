package be.appify.prefab.core.pubsub;

import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubProperties;

/** Pub/Sub connection details based on GCP Pub/Sub properties. */
public class PropertiesPubSubConnectionDetails implements PubSubConnectionDetails {
    private final GcpPubSubProperties properties;

    /**
     * Constructs a new PropertiesPubSubConnectionDetails with the given GCP Pub/Sub properties.
     *
     * @param properties the GCP Pub/Sub properties
     */
    public PropertiesPubSubConnectionDetails(GcpPubSubProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getEmulatorHost() {
        return properties.getEmulatorHost();
    }
}
