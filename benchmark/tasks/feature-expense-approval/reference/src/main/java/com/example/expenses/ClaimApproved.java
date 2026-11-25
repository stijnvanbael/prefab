package com.example.expenses;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

@Event(topic = "expense-claims")
public record ClaimApproved(@PartitioningKey Reference<ExpenseClaim> claim, String employee, BigDecimal amount) {
}
