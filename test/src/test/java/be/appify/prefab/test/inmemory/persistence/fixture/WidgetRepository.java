package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.repository.CrudRepository;

/** Repository of an aggregate with a wrapper version. */
public interface WidgetRepository extends CrudRepository<Widget, String> {
}
