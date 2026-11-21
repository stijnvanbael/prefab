package com.example.banking;

import be.appify.prefab.core.problem.ConflictException;
import be.appify.prefab.test.domain.CapturingDomainEventPublisher;
import be.appify.prefab.test.domain.PublishedEventsExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(PublishedEventsExtension.class)
class AccountTest {

    @Test
    void depositPublishesDeposited(CapturingDomainEventPublisher publisher) {
        var account = new Account("Ada");

        account.deposit(new BigDecimal("100"));

        assertThat(publisher.publishedEvents()).singleElement()
                .isInstanceOfSatisfying(AccountEvent.Deposited.class, event -> {
                    assertThat(event.account()).isEqualTo(account.id());
                    assertThat(event.amount()).isEqualByComparingTo("100");
                });
    }

    @Test
    void withdrawalPublishesWithdrawn(CapturingDomainEventPublisher publisher) {
        var account = new Account("Ada").deposit(new BigDecimal("100"));
        publisher.clear();

        account.withdraw(new BigDecimal("30"));

        assertThat(publisher.publishedEvents()).singleElement()
                .isInstanceOfSatisfying(AccountEvent.Withdrawn.class,
                        event -> assertThat(event.amount()).isEqualByComparingTo("30"));
    }

    @Test
    void overdraftIsRefusedWithoutEvents(CapturingDomainEventPublisher publisher) {
        var account = new Account("Ada").deposit(new BigDecimal("10"));
        publisher.clear();

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("11"))).isInstanceOf(ConflictException.class);
        assertThat(publisher.publishedEvents()).isEmpty();
    }
}
