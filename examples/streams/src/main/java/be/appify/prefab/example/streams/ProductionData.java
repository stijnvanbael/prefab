package be.appify.prefab.example.streams;

import be.appify.prefab.core.domain.Keyed;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public record ProductionData(
        ProductionKey key,
        Map<EnergySource, List<Double>> aggregatedValues
) implements Keyed<ProductionKey> {
    public static ProductionData aggregate(List<RawProductionData> productionSet) {
        return new ProductionData(
                ProductionKey.of(productionSet.getFirst()),
                productionSet.stream()
                        .collect(groupingBy(
                                RawProductionData::source,
                                mapping(RawProductionData::values, flatMapping(List::stream, toList())
                                ))));
    }

    public boolean isComplete() {
        return false;
    }
}
