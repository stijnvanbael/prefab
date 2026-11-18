package be.appify.prefab.spike.usecase.importing;

/** Persistence port used by the product import workflow. */
public interface ProductCatalog {
    /** Persists one validated product row. */
    void add(ProductImportRow row);
}
