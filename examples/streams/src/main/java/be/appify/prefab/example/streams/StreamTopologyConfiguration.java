package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition streamEventForwardTopology(PrefabStreams streams, @Value("${topics.streams.output}") String outputTopic) {
        return streams.from(StreamEvent.class).to(outputTopic);
    }
}

