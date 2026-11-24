package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.sns.SqsDeserializer;
import be.appify.prefab.core.sns.SqsMessageDecoder;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class InMemorySqsSubscriptionsTest {

    @Test
    @SuppressWarnings("unchecked")
    void rejectsMetadataAwareSubscriptionsBecausePrefabStreamsDoesNotRunInMemory() {
        ObjectProvider<InMemoryEventBus> bus = mock(ObjectProvider.class);
        var subscriptions = new InMemorySqsSubscriptions(bus, new SqsMessageDecoder(mock(SqsDeserializer.class)));
        var request = new SqsSubscriptionRequest<>("orders", "orders-queue", String.class, event -> { })
                .withMessageConsumer((event, metadata, acknowledge) -> { });

        assertThatThrownBy(() -> subscriptions.subscribe(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("orders")
                .hasMessageContaining("@IntegrationTest(containers = true)");
        verifyNoInteractions(bus);
    }
}
