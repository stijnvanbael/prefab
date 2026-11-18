package be.appify.prefab.spike.usecase.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductImportWorkflowTest {
    @Test
    void execute_givenValidAndInvalidRows_importsValidRowsAndReportsInvalidSkus() {
        var savedRows = new ArrayList<ProductImportRow>();
        var workflow = new ProductImportWorkflow(savedRows::add);
        var request = new ProductImportRequest(List.of(
                new ProductImportRow("sku-1", "First"),
                new ProductImportRow("", "Missing SKU"),
                new ProductImportRow("sku-3", "Third")));

        var result = workflow.execute(request);

        assertEquals(2, result.importedCount());
        assertEquals(List.of(""), result.rejectedSkus());
        assertEquals(List.of(
                new ProductImportRow("sku-1", "First"),
                new ProductImportRow("sku-3", "Third")), savedRows);
    }
}
