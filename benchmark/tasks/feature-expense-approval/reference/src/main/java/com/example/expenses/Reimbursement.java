package com.example.expenses;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById(security = @Security(role = "FINANCE"))
@GetList(security = @Security(role = "FINANCE"))
public record Reimbursement(
        @Id Reference<Reimbursement> id,
        @Version long version,
        @Filter Reference<ExpenseClaim> claim,
        String employee,
        BigDecimal amount,
        ReimbursementStatus status
) {
    private static final EventPublisher<ReimbursementPaid> PAID = EventPublisher.bind(ReimbursementPaid.class);

    @EventHandler
    public static Reimbursement onClaimApproved(ClaimApproved event) {
        return new Reimbursement(Reference.create(), 0L, event.claim(), event.employee(), event.amount(),
                ReimbursementStatus.PENDING);
    }

    @Update(path = "/payment", method = "POST", security = @Security(role = "FINANCE"))
    public Reimbursement pay() {
        if (status == ReimbursementStatus.PAID) {
            return this;
        }
        PAID.publish(new ReimbursementPaid(id, claim));
        return new Reimbursement(id, version, claim, employee, amount, ReimbursementStatus.PAID);
    }
}
