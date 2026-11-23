package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.engine.InMemoryStreamStateRepository;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.streams.engine.StreamStateRepository;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SnsSqsStreamsConfigurationTest {
    private final FakeSnsSqs aws = new FakeSnsSqs();
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnsSqsStreamsConfiguration.class))
            .withPropertyValues("spring.application.name=shop");

    @Test
    void backsOffWithoutAStateRepository() {
        withAws(runner).run(context -> assertThat(context).doesNotHaveBean(PrefabStreams.class));
    }

    @Test
    void backsOffWithoutSnsSqs() {
        runner.withBean(StreamStateRepository.class, InMemoryStreamStateRepository::new)
                .run(context -> assertThat(context).doesNotHaveBean(PrefabStreams.class));
    }

    @Test
    void providesTheEngineWhenStateAndAwsArePresent() {
        withState(withAws(runner)).run(context -> {
            assertThat(context).hasSingleBean(PrefabStreams.class);
            assertThat(context.getBean(PrefabStreams.class)).isInstanceOf(InProcessPrefabStreams.class);
            assertThat(context.getBean(TracingConfig.class).enabled()).isFalse();
        });
    }

    @Test
    void startsTheDeclaredTopologyWithTheConfiguredVisibilityTimeoutAndStopsItWithTheContext() {
        var stopped = new AtomicBoolean();
        var sqsUtil = aws.sqsUtil();
        when(sqsUtil.subscribe(any())).thenReturn(() -> stopped.set(true));
        withState(withAws(runner))
                .withUserConfiguration(DeclaredTopology.class)
                .withPropertyValues("prefab.streams.sns-sqs.visibility-timeout=PT5M",
                        "prefab.streams.tracing.enabled=true")
                .run(context -> {
                    assertThat(context.getBean(TracingConfig.class).enabled()).isTrue();
                    var captor = ArgumentCaptor.forClass(SqsSubscriptionRequest.class);
                    verify(sqsUtil, atLeastOnce()).subscribe(captor.capture());
                    assertThat(captor.getValue().queueName()).startsWith("shop-streams-source-");
                    assertThat(captor.getValue().visibilityTimeout()).contains(Duration.ofMinutes(5));
                    assertThat(stopped).isFalse();
                });
        assertThat(stopped).isTrue();
    }

    @Test
    void rejectsAVisibilityTimeoutSqsDoesNotAccept() {
        withState(withAws(runner)).withPropertyValues("prefab.streams.sns-sqs.visibility-timeout=PT13H")
                .run(context -> assertThat(context).hasFailed());
    }

    private ApplicationContextRunner withAws(ApplicationContextRunner base) {
        return base.withBean(SqsUtil.class, aws::sqsUtil)
                .withBean(io.awspring.cloud.sns.core.SnsTemplate.class, aws::snsTemplate)
                .withBean(be.appify.prefab.core.sns.SnsSerializer.class, aws::serializer)
                .withBean(EventRegistry.class, DeclaredTopology::registry);
    }

    private static ApplicationContextRunner withState(ApplicationContextRunner base) {
        return base.withBean(StreamStateRepository.class, InMemoryStreamStateRepository::new);
    }

    @Configuration(proxyBeanMethods = false)
    static class DeclaredTopology {
        static EventRegistry registry() {
            var registry = new EventRegistry();
            registry.register("sales", Sale.class, Event.Serialization.JSON);
            registry.register("category-totals", CategoryTotal.class, Event.Serialization.JSON);
            return registry;
        }

        @Bean
        StreamDefinition salesTopology(PrefabStreams streams) {
            return streams.from(Sale.class)
                    .groupBy(String.class, Sale::category)
                    .aggregate(CategoryTotal.class, () -> new CategoryTotal(null, 0),
                            (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                    .to(CategoryTotal.class);
        }
    }
}
