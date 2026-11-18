package be.appify.prefab.spike.usecase.importing;

import java.util.List;

/** Input batch for the product import workflow. */
public record ProductImportRequest(List<ProductImportRow> products) {
    public ProductImportRequest {
        products = List.copyOf(products);
    }
}
