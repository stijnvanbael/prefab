package com.example.ordering;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CustomerRevenueTopology {
    @Bean
    StreamDefinition customerRevenue(PrefabStreams streams) {
        return streams.from(OrderPlaced.class)
                .groupBy(String.class, OrderPlaced::customer)
                .aggregate(CustomerRevenue.class, CustomerRevenue::empty, (placed, revenue) -> revenue.add(placed))
                .to(CustomerRevenue.class);
    }
}
