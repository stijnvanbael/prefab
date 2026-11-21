package com.example.banking;

import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import benchmark.support.Api;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AccountEventsAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @TestEventConsumer(topic = "account-events")
    EventConsumer<AccountEvent> events;

    @Test
    void depositsAndWithdrawalsArePublishedInOrder() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/accounts", "{\"owner\": \"Ada\"}");
        api.post("/accounts/{id}/deposits", "{\"amount\": 100}", id).andExpect(status().isOk());
        api.post("/accounts/{id}/withdrawals", "{\"amount\": 30}", id).andExpect(status().isOk());

        Api.eventually(Duration.ofSeconds(15), () -> {
            var mine = events.messages().stream().filter(event -> event.account().id().equals(id)).toList();
            assertThat(mine).hasSize(2);
            assertThat(mine.get(0)).isInstanceOfSatisfying(AccountEvent.Deposited.class,
                    deposited -> assertThat(deposited.amount()).isEqualByComparingTo("100"));
            assertThat(mine.get(1)).isInstanceOfSatisfying(AccountEvent.Withdrawn.class,
                    withdrawn -> assertThat(withdrawn.amount()).isEqualByComparingTo("30"));
        });
    }
}
