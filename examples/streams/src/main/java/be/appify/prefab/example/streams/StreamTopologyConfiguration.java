package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StreamTopologyConfiguration {

    @Bean
    Object streamEventForwardTopology(PrefabStreams streams, @Value("${topics.streams.output}") String outputTopic) {
        streams.from(StreamEvent.class).to(outputTopic);
        return new Object();
    }
}

