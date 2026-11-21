package com.example.watch;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import com.example.catalog.Product;
import java.util.List;

@RepositoryMixin(PriceWatch.class)
public interface PriceWatchRepositoryMixin {
    List<PriceWatch> findByProduct(Reference<Product> product);
}
