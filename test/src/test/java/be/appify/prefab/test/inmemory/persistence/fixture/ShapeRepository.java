package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/** Repository of a sealed aggregate. */
public interface ShapeRepository extends CrudRepository<Shape, String>, PagingAndSortingRepository<Shape, String> {
}
