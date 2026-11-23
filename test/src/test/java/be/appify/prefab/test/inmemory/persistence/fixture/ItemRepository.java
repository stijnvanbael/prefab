package be.appify.prefab.test.inmemory.persistence.fixture;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import java.time.Instant;

/** One derived query per supported keyword and return type. */
public interface ItemRepository extends CrudRepository<Item, String> {

    List<Item> findByCategory(String category);

    List<Item> findByCategoryNot(String category);

    List<Item> findByCategoryIn(Collection<String> categories);

    List<Item> findByCategoryNotIn(Collection<String> categories);

    List<Item> findByNameLike(String pattern);

    List<Item> findByNameNotLike(String pattern);

    List<Item> findByNameContaining(String text);

    List<Item> findByNameNotContaining(String text);

    List<Item> findByNameStartingWith(String prefix);

    List<Item> findByNameEndingWith(String suffix);

    List<Item> findByStockGreaterThan(int stock);

    List<Item> findByStockGreaterThanEqual(int stock);

    List<Item> findByStockLessThan(int stock);

    List<Item> findByStockLessThanEqual(int stock);

    List<Item> findByCreatedAtBefore(Instant instant);

    List<Item> findByCreatedAtAfter(Instant instant);

    List<Item> findByStockBetween(int from, int to);

    List<Item> findByCategoryIsNull();

    List<Item> findByCategoryIsNotNull();

    List<Item> findByActiveTrue();

    List<Item> findByActiveFalse();

    List<Item> findByNameRegex(String regex);

    List<Item> findByNameIgnoreCase(String name);

    List<Item> findByTagsContaining(String tag);

    List<Item> findByCategoryAndActive(String category, boolean active);

    List<Item> findByCategoryOrName(String category, String name);

    List<Item> findDistinctByCategory(String category);

    List<Item> findByCategoryOrderByNameDesc(String category);

    List<Item> findByCategory(String category, Sort sort);

    List<Item> findTop2ByOrderByStockDesc();

    Optional<Item> findFirstByCategoryOrderByStockDesc(String category);

    Optional<Item> findByName(String name);

    Item findOneByName(String name);

    Stream<Item> streamByCategory(String category);

    Page<Item> findByActive(boolean active, Pageable pageable);

    Slice<Item> findSliceByActive(boolean active, Pageable pageable);

    long countByCategory(String category);

    int countByActive(boolean active);

    boolean existsByCategory(String category);

    long deleteByCategory(String category);

    void removeByName(String name);
}
