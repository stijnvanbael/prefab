package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition topology(PrefabStreams streams) {
        return streams.from(RawProductionData.class)
                .aggregate(
                        ProductionKey::of,
                        ProductionData::aggregate,
                        ProductionData::isComplete)
                .to(ProductionData.class);
    }
}
