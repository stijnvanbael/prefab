package be.appify.prefab.test.inmemory.persistence.fixture;

import be.appify.prefab.core.annotations.rest.AutocompleteQuery;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/** Autocomplete methods in every combination of scan mode and match strategy, as the processor generates them. */
public interface ProductRepository extends CrudRepository<Product, String> {

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.EXACT)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompletePrefixExact(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.IGNORE_CASE)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompletePrefixIgnoreCase(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.FUZZY)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompletePrefixFuzzy(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.FULL_TEXT)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompletePrefixFullText(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.FULL_TEXT_FUZZY)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompletePrefixFullTextFuzzy(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.EXACT)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompleteContainsExact(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.IGNORE_CASE)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompleteContainsIgnoreCase(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.FUZZY)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompleteContainsFuzzy(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.FULL_TEXT)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompleteContainsFullText(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.FULL_TEXT_FUZZY)
    @Query("SELECT DISTINCT name FROM product")
    List<String> autocompleteContainsFullTextFuzzy(@Param("query") String query, @Param("limit") int limit, @Param("offset") long offset);

    @AutocompleteQuery(property = "name", scanMode = ScanMode.PREFIX, matchStrategy = MatchStrategy.FULL_TEXT)
    List<String> autocompleteWithRegularExpression(String query, Pageable pageable);
}
