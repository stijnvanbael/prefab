package be.appify.prefab.test.inmemory.persistence.fixture;

import be.appify.prefab.core.annotations.rest.AutocompleteQuery;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
import java.util.List;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/** Repository in the shape the processor generates. */
public interface OrderRepository extends CrudRepository<Order, String>, PagingAndSortingRepository<Order, String>,
        OrderRepositoryMixin {

    Page<Order> findByCustomer(String customer, Pageable pageable);

    Page<Order> findAll(Example<Order> example, Pageable pageable);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.IGNORE_CASE)
    @Query("SELECT DISTINCT name FROM orders WHERE name LIKE :query LIMIT :limit OFFSET :offset")
    List<String> autocompleteByName(
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @AutocompleteQuery(property = "category", scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.EXACT)
    List<String> autocompleteByCategory(String query, Pageable pageable);
}
