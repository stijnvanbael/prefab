package com.example.tickets;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TicketSalesAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api organizer() {
        return new Api(mockMvc).with(user("olivia").roles("ORGANIZER"));
    }

    private Api fan() {
        return new Api(mockMvc).with(user("fred").roles("FAN"));
    }

    private Api anonymous() {
        return new Api(mockMvc);
    }

    private String concert(String city, int capacity) {
        return "{\"name\": \"Night Owls\", \"venue\": \"Arena\", \"city\": \"%s\", \"capacity\": %d}"
                .formatted(city, capacity);
    }

    private String order(String concert, int quantity) {
        return "{\"concert\": \"%s\", \"quantity\": %d}".formatted(concert, quantity);
    }

    @Test
    void onlyOrganizersPutConcertsOnSaleAndEveryoneCanFindThem() throws Exception {
        var city = "Ghent-" + System.nanoTime();
        var concert = organizer().create("/concerts", concert(city, 500));
        organizer().create("/concerts", concert("Bruges-" + System.nanoTime(), 100));
        fan().post("/concerts", concert(city, 10)).andExpect(status().isForbidden());
        anonymous().post("/concerts", concert(city, 10)).andExpect(status().isUnauthorized());

        anonymous().get("/concerts/{id}", concert).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Night Owls"))
                .andExpect(jsonPath("$.city").value(city))
                .andExpect(jsonPath("$.capacity").value(500))
                .andExpect(jsonPath("$.ticketsSold").value(0))
                .andExpect(jsonPath("$.remaining").value(500))
                .andExpect(jsonPath("$.soldOut").value(false));
        List<String> inCity = Api.read(anonymous().get("/concerts?city={city}", city).andExpect(status().isOk()),
                "$.content[*].id");
        assertThat(inCity).containsExactly(concert);
    }

    @Test
    void ordersSellTicketsAndCancellationsPutThemBackOnSale() throws Exception {
        var concert = organizer().create("/concerts", concert("Ghent", 5));

        var first = fan().create("/ticket-orders", order(concert, 3));
        fan().create("/ticket-orders", order(concert, 2));
        fan().get("/ticket-orders/{id}", first).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("PLACED"));
        Api.eventually(Duration.ofSeconds(20), () -> anonymous().get("/concerts/{id}", concert)
                .andExpect(jsonPath("$.ticketsSold").value(5))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.soldOut").value(true)));

        fan().postEmpty("/ticket-orders/{id}/cancellation", first).andExpect(status().isOk());
        fan().postEmpty("/ticket-orders/{id}/cancellation", first).andExpect(status().isOk());

        fan().get("/ticket-orders/{id}", first).andExpect(jsonPath("$.status").value("CANCELLED"));
        Api.eventually(Duration.ofSeconds(20), () -> anonymous().get("/concerts/{id}", concert)
                .andExpect(jsonPath("$.ticketsSold").value(2))
                .andExpect(jsonPath("$.remaining").value(3))
                .andExpect(jsonPath("$.soldOut").value(false)));
        Thread.sleep(500);
        anonymous().get("/concerts/{id}", concert).andExpect(jsonPath("$.ticketsSold").value(2));
    }

    @Test
    void ticketOrdersNeedASignedInUserAndASensibleQuantity() throws Exception {
        var concert = organizer().create("/concerts", concert("Ghent", 50));

        anonymous().post("/ticket-orders", order(concert, 1)).andExpect(status().isUnauthorized());
        fan().post("/ticket-orders", order(concert, 0)).andExpect(status().isBadRequest());
        fan().post("/ticket-orders", order(concert, 11)).andExpect(status().isBadRequest());
    }
}
