package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.gcloud.PubSubEmulatorContainer;

/**
 * Factory to create {@link PubSubConnectionDetails} for a {@link PubSubEmulatorContainer}.
 */
public class PubSubContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<PubSubEmulatorContainer, PubSubConnectionDetails> {

    /** Constructs a new {@link PubSubContainerConnectionDetailsFactory}. */
    public PubSubContainerConnectionDetailsFactory() {
    }

    @Override
    protected PubSubConnectionDetails getContainerConnectionDetails(
            ContainerConnectionSource<PubSubEmulatorContainer> source) {
        return new PubSubContainerConnectionDetails(source);
    }

    private static class PubSubContainerConnectionDetails
            extends ContainerConnectionDetails<PubSubEmulatorContainer>
            implements PubSubConnectionDetails {
        protected PubSubContainerConnectionDetails(ContainerConnectionSource<PubSubEmulatorContainer> source) {
            super(source);
        }

        @Override
        public String getEmulatorHost() {
            return getContainer().getEmulatorEndpoint();
        }
    }
}
