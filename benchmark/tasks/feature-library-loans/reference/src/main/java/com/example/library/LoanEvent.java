package com.example.library;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "loans")
public sealed interface LoanEvent permits LoanEvent.Started, LoanEvent.Returned {
    @PartitioningKey
    Reference<Loan> loan();

    Reference<Book> book();

    Reference<Member> member();

    record Started(Reference<Loan> loan, Reference<Book> book, Reference<Member> member) implements LoanEvent {
    }

    record Returned(Reference<Loan> loan, Reference<Book> book, Reference<Member> member) implements LoanEvent {
    }
}
