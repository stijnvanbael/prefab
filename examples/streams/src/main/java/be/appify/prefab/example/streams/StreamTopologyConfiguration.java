package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StreamTopologyConfiguration {

    @Bean
    Topology streamEventForwardTopology(PrefabStreams streams, @Value("${topics.streams.output}") String outputTopic) {
        return streams.from(StreamEvent.class).to(outputTopic);
    }
}

