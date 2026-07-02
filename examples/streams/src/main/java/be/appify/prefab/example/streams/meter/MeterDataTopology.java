package be.appify.prefab.example.streams.meter;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterDataTopology {
    @Bean
    public StreamDefinition topology(PrefabStreams streams) {
        return streams.from(RawMeterData.class)
                .process(new RawMeterDataProcessor())
                .to(MeterData.class);
    }
}
