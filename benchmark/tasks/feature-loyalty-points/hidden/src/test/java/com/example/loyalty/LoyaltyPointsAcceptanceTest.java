package com.example.loyalty;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LoyaltyPointsAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api cashier() {
        return new Api(mockMvc).with(user("carl").roles("CASHIER"));
    }

    private Api customer() {
        return new Api(mockMvc).with(user("ada").roles("CUSTOMER"));
    }

    private String account() throws Exception {
        return customer().create("/loyalty-accounts", "{\"holder\": \"Ada\"}");
    }

    private void purchase(String account, String amount) throws Exception {
        cashier().create("/purchases", "{\"account\": \"%s\", \"amount\": %s}".formatted(account, amount));
    }

    private String redeem(String account, int points) throws Exception {
        return cashier().create("/redemptions", "{\"account\": \"%s\", \"points\": %d}".formatted(account, points));
    }

    private void expectAccount(String account, int points, String tier) throws Exception {
        Api.eventually(Duration.ofSeconds(20), () -> customer().get("/loyalty-accounts/{id}", account)
                .andExpect(jsonPath("$.points").value(points))
                .andExpect(jsonPath("$.tier").value(tier)));
    }

    private void expectRedemption(String redemption, String status) throws Exception {
        Api.eventually(Duration.ofSeconds(20), () -> customer().get("/redemptions/{id}", redemption)
                .andExpect(jsonPath("$.status").value(status)));
    }

    @Test
    void customersEarnPointsReachGoldAndSpendOnlyWhatTheyHave() throws Exception {
        var account = account();
        customer().get("/loyalty-accounts/{id}", account).andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("Ada"))
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.tier").value("BASIC"));

        purchase(account, "600.75");
        expectAccount(account, 600, "BASIC");

        var tooMuch = redeem(account, 700);
        expectRedemption(tooMuch, "REJECTED");
        customer().get("/redemptions/{id}", tooMuch).andExpect(jsonPath("$.points").value(700));
        expectAccount(account, 600, "BASIC");

        purchase(account, "500");
        expectAccount(account, 1100, "GOLD");

        var coffee = redeem(account, 700);
        expectRedemption(coffee, "APPROVED");
        expectAccount(account, 400, "GOLD");
    }

    @Test
    void redeemingFromAnEmptyAccountIsRejected() throws Exception {
        var account = account();
        var redemption = redeem(account, 10);

        expectRedemption(redemption, "REJECTED");
        expectAccount(account, 0, "BASIC");
    }

    @Test
    void onlyCashiersRegisterPurchasesAndRedemptions() throws Exception {
        var account = account();

        customer().post("/purchases", "{\"account\": \"%s\", \"amount\": 10}".formatted(account))
                .andExpect(status().isForbidden());
        customer().post("/redemptions", "{\"account\": \"%s\", \"points\": 10}".formatted(account))
                .andExpect(status().isForbidden());
        new Api(mockMvc).post("/purchases", "{\"account\": \"%s\", \"amount\": 10}".formatted(account))
                .andExpect(status().isUnauthorized());
        cashier().post("/purchases", "{\"account\": \"%s\", \"amount\": 0}".formatted(account))
                .andExpect(status().isBadRequest());
        Thread.sleep(500);
        expectAccount(account, 0, "BASIC");
    }
}
