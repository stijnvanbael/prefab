package be.appify.prefab.processor.spring;

import be.appify.prefab.core.repository.Repository;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.problem.NotFoundProblem;

public interface SpringService {
    default <T> Reference<T> toReference(
            String aggregateRootName,
            Repository<T> repository,
            String id
    ) {
        if (id == null) {
            return null;
        }
        var reference = new SpringDataReference<>(id, repository);
        if (!reference.exists()) {
            throw new NotFoundProblem("%s with ID %s".formatted(aggregateRootName, id));
        }
        return reference;
    }
}
