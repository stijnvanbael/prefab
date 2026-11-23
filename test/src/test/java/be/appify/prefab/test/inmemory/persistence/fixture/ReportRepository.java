package be.appify.prefab.test.inmemory.persistence.fixture;

import java.util.List;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

/** Repository with MongoDB specific queries. */
public interface ReportRepository extends CrudRepository<Report, String> {

    @Query("{ 'title': ?0 }")
    List<Report> findWithMongoQuery(String title);

    @Aggregation(pipeline = "{ '$match': { 'title': ?0 } }")
    List<Report> findWithPipeline(String title);
}
