package com.example.auctions;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuctionAcceptanceTest {
    private static final String VASE = "{\"item\": \"Ming vase\", \"startingPrice\": 100}";

    @Autowired
    MockMvc mockMvc;

    private Api seller() {
        return new Api(mockMvc).with(user("sally").roles("SELLER"));
    }

    private Api bidder(String name) {
        return new Api(mockMvc).with(user(name).roles("BIDDER"));
    }

    private String bid(String bidder, String auction, int amount) throws Exception {
        return bidder(bidder).create("/bids", "{\"auction\": \"%s\", \"amount\": %d}".formatted(auction, amount));
    }

    private void expectHighest(String auction, double amount, String bid, int count) throws Exception {
        Api.eventually(Duration.ofSeconds(20), () -> {
            var result = seller().get("/auctions/{id}", auction)
                    .andExpect(jsonPath("$.winningBid").value(bid))
                    .andExpect(jsonPath("$.bidCount").value(count));
            assertThat(Api.number(result, "$.highestBid")).isCloseTo(amount, within(0.001));
        });
    }

    private List<String> salesOf(String auction) throws Exception {
        return Api.read(bidder("bob").get("/sales?auction={auction}", auction).andExpect(status().isOk()),
                "$.content[*].id");
    }

    @Test
    void theHighestValidBidWinsTheAuction() throws Exception {
        var auction = seller().create("/auctions", VASE);
        bidder("bob").get("/auctions/{id}", auction).andExpect(status().isOk())
                .andExpect(jsonPath("$.item").value("Ming vase"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.bidCount").value(0))
                .andExpect(jsonPath("$.highestBid").isEmpty());

        bid("bob", auction, 90);
        var alicesBid = bid("alice", auction, 120);
        expectHighest(auction, 120, alicesBid, 1);
        bidder("alice").get("/bids/{id}", alicesBid).andExpect(status().isOk())
                .andExpect(jsonPath("$.bidder").value("alice"));
        bid("bob", auction, 110);
        var bobsBid = bid("bob", auction, 150);
        expectHighest(auction, 150, bobsBid, 2);
        bidder("alice").get("/bids/{id}", bobsBid).andExpect(jsonPath("$.bidder").value("bob"));

        seller().postEmpty("/auctions/{id}/closing", auction).andExpect(status().isOk());
        seller().get("/auctions/{id}", auction).andExpect(jsonPath("$.status").value("CLOSED"));
        bid("alice", auction, 500);

        Api.eventually(Duration.ofSeconds(20), () -> assertThat(salesOf(auction)).hasSize(1));
        var sales = bidder("bob").get("/sales?auction={auction}", auction)
                .andExpect(jsonPath("$.content[0].bid").value(bobsBid))
                .andExpect(jsonPath("$.content[0].item").value("Ming vase"));
        assertThat(Api.number(sales, "$.content[0].amount")).isCloseTo(150, within(0.001));
        Thread.sleep(500);
        expectHighest(auction, 150, bobsBid, 2);
    }

    @Test
    void anAuctionWithoutValidBidsEndsWithoutSale() throws Exception {
        var auction = seller().create("/auctions", VASE);
        bid("bob", auction, 50);

        seller().postEmpty("/auctions/{id}/closing", auction).andExpect(status().isOk());

        seller().get("/auctions/{id}", auction).andExpect(jsonPath("$.status").value("CLOSED"));
        Thread.sleep(1000);
        assertThat(salesOf(auction)).isEmpty();
        seller().get("/auctions/{id}", auction).andExpect(jsonPath("$.bidCount").value(0));
    }

    @Test
    void onlySellersStartAndCloseAuctions() throws Exception {
        bidder("bob").post("/auctions", VASE).andExpect(status().isForbidden());
        new Api(mockMvc).post("/auctions", VASE).andExpect(status().isUnauthorized());
        var auction = seller().create("/auctions", VASE);

        bidder("bob").postEmpty("/auctions/{id}/closing", auction).andExpect(status().isForbidden());
        new Api(mockMvc).post("/bids", "{\"auction\": \"%s\", \"amount\": 200}".formatted(auction))
                .andExpect(status().isUnauthorized());
        seller().get("/auctions/{id}", auction).andExpect(jsonPath("$.status").value("OPEN"));
    }
}
