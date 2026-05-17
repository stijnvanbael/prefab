package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamDefinition;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

class StreamsConfigurationBootstrapTest {

    @Test
    void streamTopologyBootstrap_shouldBuildAllStreamDefinitions() {
        var builds = new AtomicInteger();
        var topology = new Topology();

        var firstDefinition = new StreamDefinition(() -> {
            builds.incrementAndGet();
            return topology;
        });
        var secondDefinition = new StreamDefinition(() -> {
            builds.incrementAndGet();
            return topology;
        });

        var beanFactory = new StaticListableBeanFactory(Map.of(
                "firstDefinition", firstDefinition,
                "secondDefinition", secondDefinition
        ));

        SmartInitializingSingleton bootstrap = new StreamsConfiguration().streamTopologyBootstrap(
                beanFactory.getBeanProvider(StreamDefinition.class)
        );

        bootstrap.afterSingletonsInstantiated();

        assertThat(builds.get()).isEqualTo(2);
        assertThat(firstDefinition.nativeTopology()).isSameAs(topology);
        assertThat(secondDefinition.nativeTopology()).isSameAs(topology);
    }
}

