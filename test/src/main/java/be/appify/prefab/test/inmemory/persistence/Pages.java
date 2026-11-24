package be.appify.prefab.test.inmemory.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** Sorts and slices in-memory query results. */
final class Pages {

    private Pages() {
    }

    /**
     * Sorts the rows and returns the requested page, with the total of all rows.
     *
     * @param rows
     *         all matching rows
     * @param pageable
     *         the requested page, may be unpaged
     * @param order
     *         the order of the rows
     * @param <S>
     *         the row type
     * @return the page
     */
    static <S> Page<S> of(List<S> rows, Pageable pageable, Comparator<? super S> order) {
        var sorted = new ArrayList<>(rows);
        sorted.sort(order);
        if (pageable.isUnpaged()) {
            return new PageImpl<>(sorted);
        }
        var from = (int) Math.min(pageable.getOffset(), sorted.size());
        var to = Math.min(from + pageable.getPageSize(), sorted.size());
        return new PageImpl<>(List.copyOf(sorted.subList(from, to)), pageable, sorted.size());
    }
}
