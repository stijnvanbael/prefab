package be.appify.prefab.core.spring;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Custom implementation of Spring Data's Page interface.
 * @param content the content of the page
 * @param pageable the pagination information
 * @param total the total number of elements
 * @param <T> the type of the content
 */
public record Page<T>(
        List<T> content,
        Pageable pageable,
        long total
) implements org.springframework.data.domain.Page<T> {
    @Override
    public int getTotalPages() {
        return (int) Math.ceil((double) total / getSize());
    }

    @Override
    public long getTotalElements() {
        return total;
    }

    @Override
    public int getNumber() {
        return pageable.isPaged() ? pageable.getPageNumber() : 0;
    }

    @Override
    public int getSize() {
        return pageable.isPaged() ? pageable.getPageSize() : content.size();
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
        return pageable.getSort();
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
        return hasNext() ? pageable.next() : Pageable.unpaged();
    }

    @Override
    public Pageable previousPageable() {
        return hasPrevious() ? pageable.previousOrFirst() : Pageable.unpaged();
    }

    @Override
    public <U> Page<U> map(Function<? super T, ? extends U> converter) {
        return new Page<>(
                content.stream().map(e -> (U) converter.apply(e)).toList(),
                pageable,
                total
        );
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }
}
