package com.example.library;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Member(
        @Id Reference<Member> id,
        @Version long version,
        @NotBlank String name,
        @NotBlank @Email String email,
        int activeLoans
) {
    @Create
    public Member(@NotBlank String name, @NotBlank @Email String email) {
        this(Reference.create(), 0L, name, email, 0);
    }

    @EventHandler
    @ByReference(property = "member")
    public Member onLoanStarted(LoanEvent.Started event) {
        return new Member(id, version, name, email, activeLoans + 1);
    }

    @EventHandler
    @ByReference(property = "member")
    public Member onLoanReturned(LoanEvent.Returned event) {
        return new Member(id, version, name, email, activeLoans - 1);
    }
}
