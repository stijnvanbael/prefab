package be.appify.prefab.core.spring;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Custom implementation of Spring Data's Page interface.
 *
 * @param content
 *         the content of the page
 * @param page
 *         the page information
 * @param <T>
 *         the type of the content
 */
public record Page<T>(
        List<T> content,
        PageInfo page
) implements org.springframework.data.domain.Page<T> {
    @Override
    public int getTotalPages() {
        return (int) Math.ceil((double) page.totalElements / getSize());
    }

    @Override
    public long getTotalElements() {
        return page.totalElements;
    }

    @Override
    public int getNumber() {
        return page.number;
    }

    @Override
    public int getSize() {
        return page.size;
    }

    @Override
    public int getNumberOfElements() {
        return content.size();
    }

    @Override
    public List<T> getContent() {
        return content;
    }

    @Override
    public boolean hasContent() {
        return !content.isEmpty();
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public boolean isFirst() {
        return !hasPrevious();
    }

    @Override
    public boolean isLast() {
        return !hasNext();
    }

    @Override
    public boolean hasNext() {
        return getNumber() + 1 < getTotalPages();
    }

    @Override
    public boolean hasPrevious() {
        return getNumber() > 0;
    }

    @Override
    public Pageable nextPageable() {
        return Pageable.unpaged();
    }

    @Override
    public Pageable previousPageable() {
        return Pageable.unpaged();
    }

    @Override
    public <U> Page<U> map(Function<? super T, ? extends U> converter) {
        return new Page<>(
                content.stream().map(e -> (U) converter.apply(e)).toList(),
                page
        );
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    record PageInfo(int size, int number, long totalElements, int totalPages) {
    }
}
