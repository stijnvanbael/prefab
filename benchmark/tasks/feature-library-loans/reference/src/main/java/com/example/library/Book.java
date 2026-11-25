package com.example.library;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Book(
        @Id Reference<Book> id,
        @Version long version,
        @NotBlank String title,
        @NotBlank String author,
        @Min(1) int copies,
        int availableCopies
) {
    @Create
    public Book(@NotBlank String title, @NotBlank String author, @Min(1) int copies) {
        this(Reference.create(), 0L, title, author, copies, copies);
    }

    @EventHandler
    @ByReference(property = "book")
    public Book onLoanStarted(LoanEvent.Started event) {
        return new Book(id, version, title, author, copies, availableCopies - 1);
    }

    @EventHandler
    @ByReference(property = "book")
    public Book onLoanReturned(LoanEvent.Returned event) {
        return new Book(id, version, title, author, copies, availableCopies + 1);
    }
}
