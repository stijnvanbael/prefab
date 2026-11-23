package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.service.Reference;
import java.util.Iterator;
import java.util.function.Predicate;
import org.springframework.data.keyvalue.repository.query.PredicateQueryCreator;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Creates the predicate of a derived query. Generated repositories filter {@link Reference} properties by the plain
 * identifier, for example {@code findByProduct(String productId, Pageable)}, which the real stores convert. The
 * identifier is wrapped in a {@link Reference} here, so that it compares equal to the stored value.
 */
public class ReferenceAwarePredicateQueryCreator extends PredicateQueryCreator {

    /**
     * Constructs a new ReferenceAwarePredicateQueryCreator.
     *
     * @param tree
     *         the parsed method name
     * @param parameters
     *         the arguments of the call
     */
    public ReferenceAwarePredicateQueryCreator(PartTree tree, ParameterAccessor parameters) {
        super(tree, parameters);
    }

    @Override
    protected Predicate<?> create(Part part, Iterator<Object> iterator) {
        return super.create(part, referencesOf(part, iterator));
    }

    private static Iterator<Object> referencesOf(Part part, Iterator<Object> values) {
        if (!Reference.class.equals(part.getProperty().getLeafType())) {
            return values;
        }
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return values.hasNext();
            }

            @Override
            public Object next() {
                var value = values.next();
                return value instanceof String id ? Reference.fromId(id) : value;
            }
        };
    }
}
