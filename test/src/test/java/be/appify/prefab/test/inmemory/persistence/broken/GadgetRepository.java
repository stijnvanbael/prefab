package be.appify.prefab.test.inmemory.persistence.broken;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

/** Repository with a keyword that only a real store can evaluate. */
public interface GadgetRepository extends CrudRepository<Gadget, String> {

    List<Gadget> findByNameNear(String name);
}
