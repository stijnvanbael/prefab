package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.repository.CrudRepository;

/** Repository whose identifier type is String while the aggregate's identifier is a Reference. */
public interface TicketRepository extends CrudRepository<Ticket, String> {
}
