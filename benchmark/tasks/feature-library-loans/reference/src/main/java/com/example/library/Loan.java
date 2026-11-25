package com.example.library;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Loan(
        @Id Reference<Loan> id,
        @Version long version,
        @NotNull Reference<Book> book,
        @NotNull @Filter Reference<Member> member,
        @NotNull LoanStatus status
) {
    private static final EventPublisher<LoanEvent> EVENTS = EventPublisher.bind(LoanEvent.class);

    @Create
    public Loan(@NotNull Reference<Book> book, @NotNull Reference<Member> member) {
        this(Reference.create(), 0L, book, member, LoanStatus.ACTIVE);
        EVENTS.publish(new LoanEvent.Started(id, book, member));
    }

    @Update(path = "/return", method = "POST")
    public Loan returnBook() {
        if (status == LoanStatus.RETURNED) {
            return this;
        }
        EVENTS.publish(new LoanEvent.Returned(id, book, member));
        return new Loan(id, version, book, member, LoanStatus.RETURNED);
    }
}
