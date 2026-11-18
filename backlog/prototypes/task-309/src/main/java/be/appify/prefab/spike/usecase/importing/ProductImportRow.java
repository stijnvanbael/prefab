package be.appify.prefab.spike.usecase.importing;

/** One product row supplied to the import workflow. */
public record ProductImportRow(String sku, String name) {}
