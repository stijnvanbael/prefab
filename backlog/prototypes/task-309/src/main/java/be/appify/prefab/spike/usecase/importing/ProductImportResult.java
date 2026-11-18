package be.appify.prefab.spike.usecase.importing;

import java.util.List;

/** Result of importing a product batch. */
public record ProductImportResult(int importedCount, List<String> rejectedSkus) {
    public ProductImportResult {
        rejectedSkus = List.copyOf(rejectedSkus);
    }
}
