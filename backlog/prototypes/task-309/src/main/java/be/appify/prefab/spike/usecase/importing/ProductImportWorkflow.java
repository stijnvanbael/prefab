package be.appify.prefab.spike.usecase.importing;

import be.appify.prefab.spike.usecase.UseCase;
import java.util.ArrayList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Imports valid product rows in one local transaction and reports invalid rows. */
@Component
public final class ProductImportWorkflow {
    private final ProductCatalog catalog;

    /** Creates the workflow with its persistence port. */
    public ProductImportWorkflow(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    /** Imports non-blank rows and returns the invalid SKUs without hiding persistence failures. */
    @UseCase(method = "POST", path = "/products/import")
    @Transactional
    public ProductImportResult execute(ProductImportRequest request) {
        var rejectedSkus = new ArrayList<String>();
        var importedCount = 0;
        for (var row : request.products()) {
            if (row.sku().isBlank() || row.name().isBlank()) {
                rejectedSkus.add(row.sku());
            } else {
                catalog.add(row);
                importedCount++;
            }
        }
        return new ProductImportResult(importedCount, rejectedSkus);
    }
}
