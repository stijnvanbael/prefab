package com.example.ordering;

import be.appify.prefab.core.annotations.RepositoryMixin;
import java.math.BigDecimal;
import org.springframework.data.jdbc.repository.query.Query;

@RepositoryMixin(Order.class)
public interface OrderRepositoryMixin {
    @Query("SELECT COALESCE(SUM(total), 0) FROM \"order\" WHERE status = :status")
    BigDecimal totalRevenue(String status);
}
