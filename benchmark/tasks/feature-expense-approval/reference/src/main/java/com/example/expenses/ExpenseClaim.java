package com.example.expenses;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record ExpenseClaim(
        @Id Reference<ExpenseClaim> id,
        @Version long version,
        @CreatedBy String submittedBy,
        @NotBlank String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull ClaimStatus status
) {
    private static final EventPublisher<ClaimApproved> APPROVED = EventPublisher.bind(ClaimApproved.class);

    @Create(security = @Security(role = "EMPLOYEE"))
    public ExpenseClaim(@NotBlank String description, @NotNull @Positive BigDecimal amount) {
        this(Reference.create(), 0L, null, description, amount, ClaimStatus.SUBMITTED);
    }

    @Update(path = "/approval", method = "POST", security = @Security(role = "MANAGER"))
    public ExpenseClaim approve() {
        if (status != ClaimStatus.SUBMITTED) {
            return this;
        }
        APPROVED.publish(new ClaimApproved(id, submittedBy, amount));
        return withStatus(ClaimStatus.APPROVED);
    }

    @Update(path = "/rejection", method = "POST", security = @Security(role = "MANAGER"))
    public ExpenseClaim reject() {
        return status == ClaimStatus.SUBMITTED ? withStatus(ClaimStatus.REJECTED) : this;
    }

    @EventHandler
    @ByReference(property = "claim")
    public ExpenseClaim onReimbursementPaid(ReimbursementPaid event) {
        return withStatus(ClaimStatus.REIMBURSED);
    }

    private ExpenseClaim withStatus(ClaimStatus newStatus) {
        return new ExpenseClaim(id, version, submittedBy, description, amount, newStatus);
    }
}
