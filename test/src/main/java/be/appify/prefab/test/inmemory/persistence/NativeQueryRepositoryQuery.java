package be.appify.prefab.test.inmemory.persistence;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Stands in for a repository method with a store specific query, which cannot run in memory. The repository is
 * still created so that tests that do not call the method are unaffected; calling it fails with an actionable
 * message.
 */
final class NativeQueryRepositoryQuery implements RepositoryQuery {
    /** Annotations with a query in a store specific language. The store jars are optional, so match by name. */
    private static final Set<String> NATIVE_ANNOTATIONS = Set.of(
            "org.springframework.data.jdbc.repository.query.Query",
            "org.springframework.data.mongodb.repository.Query",
            "org.springframework.data.mongodb.repository.Aggregation");

    private final QueryMethod queryMethod;
    private final String description;

    NativeQueryRepositoryQuery(QueryMethod queryMethod, Class<?> repository, Method method) {
        this.queryMethod = queryMethod;
        this.description = "%s#%s".formatted(repository.getSimpleName(), method.getName());
    }

    static boolean isNative(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> NATIVE_ANNOTATIONS.contains(annotation.annotationType().getName()));
    }

    String description() {
        return description;
    }

    @Override
    public Object execute(Object[] parameters) {
        throw new UnsupportedOperationException(
                "%s has a native query that cannot run against the in-memory test backend. Use "
                        .formatted(description)
                        + "@IntegrationTest(containers = true) for tests that call it.");
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}
