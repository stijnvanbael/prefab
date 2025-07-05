package be.appify.prefab.test.pubsub;

import be.appify.prefab.processor.pubsub.PubSubConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.PubSubEmulatorContainer;

public class PubSubContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<PubSubEmulatorContainer, PubSubConnectionDetails> {

    @Override
    protected PubSubConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<PubSubEmulatorContainer> source) {
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
