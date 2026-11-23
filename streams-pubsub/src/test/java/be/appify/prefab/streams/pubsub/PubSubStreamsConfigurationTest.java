package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubDeserializer;
import be.appify.prefab.core.pubsub.PubSubSerializer;
import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.engine.StreamStateRepository;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.spring.pubsub.core.publisher.PublisherCustomizer;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PubSubStreamsConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withConfiguration(AutoConfigurations.of(PubSubStreamsConfiguration.class))
            .withPropertyValues("spring.application.name=test");

    @Test
    void doesNotActivateWithoutTransportOrState() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PrefabStreams.class));
        runner.withBean(PubSubUtil.class, () -> mock(PubSubUtil.class))
                .run(context -> assertThat(context).doesNotHaveBean(PrefabStreams.class));
        runner.withBean(StreamStateRepository.class, () -> mock(StreamStateRepository.class))
                .run(context -> assertThat(context).doesNotHaveBean(PrefabStreams.class));
    }

    @Test
    void activatesWithTransportAndStateAndEnablesPublisherOrdering() {
        runner.withBean(PubSubUtil.class, () -> mock(PubSubUtil.class))
                .withBean(StreamStateRepository.class, () -> mock(StreamStateRepository.class))
                .withBean(EventRegistry.class, EventRegistry::new)
                .withBean(PubSubSerializer.class, () -> mock(PubSubSerializer.class))
                .withBean(PubSubDeserializer.class, () -> mock(PubSubDeserializer.class))
                .withBean(PublisherFactory.class, () -> mock(PublisherFactory.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(PrefabStreams.class);
                    var builder = mock(Publisher.Builder.class);
                    context.getBean(PublisherCustomizer.class).apply(builder, "topic");
                    verify(builder).setEnableMessageOrdering(true);
                });
    }
}
