package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.repository.CrudRepository;

/** Repository of an aggregate without a version. */
public interface NoteRepository extends CrudRepository<Note, String> {
}
