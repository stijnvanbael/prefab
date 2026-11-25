package com.example.expenses;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "reimbursements")
public record ReimbursementPaid(Reference<Reimbursement> reimbursement, @PartitioningKey Reference<ExpenseClaim> claim) {
}
