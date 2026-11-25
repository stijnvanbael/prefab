package com.example.library;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LibraryLoansAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api api() {
        return new Api(mockMvc);
    }

    private String book(int copies) throws Exception {
        return api().create("/books", "{\"title\": \"Dune\", \"author\": \"Frank Herbert\", \"copies\": %d}"
                .formatted(copies));
    }

    private String member(String name) throws Exception {
        return api().create("/members", "{\"name\": \"%s\", \"email\": \"%s@example.com\"}".formatted(name, name));
    }

    private String lend(String book, String member) throws Exception {
        return api().create("/loans", "{\"book\": \"%s\", \"member\": \"%s\"}".formatted(book, member));
    }

    private void expectCounters(String book, int available, String member, int activeLoans) throws Exception {
        Api.eventually(Duration.ofSeconds(20), () -> {
            api().get("/books/{id}", book).andExpect(jsonPath("$.availableCopies").value(available));
            api().get("/members/{id}", member).andExpect(jsonPath("$.activeLoans").value(activeLoans));
        });
    }

    @Test
    void newBooksAndMembersStartWithFullShelvesAndNoLoans() throws Exception {
        var book = book(3);
        var member = member("ada");

        api().get("/books/{id}", book).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dune"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"))
                .andExpect(jsonPath("$.copies").value(3))
                .andExpect(jsonPath("$.availableCopies").value(3));
        api().get("/members/{id}", member).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ada"))
                .andExpect(jsonPath("$.activeLoans").value(0));
    }

    @Test
    void lendingAndReturningUpdatesBookAndMember() throws Exception {
        var book = book(2);
        var ada = member("ada");
        var grace = member("grace");

        var first = lend(book, ada);
        lend(book, grace);
        api().get("/loans/{id}", first).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        expectCounters(book, 0, ada, 1);

        api().postEmpty("/loans/{id}/return", first).andExpect(status().isOk());
        api().get("/loans/{id}", first).andExpect(jsonPath("$.status").value("RETURNED"));
        expectCounters(book, 1, ada, 0);

        api().postEmpty("/loans/{id}/return", first).andExpect(status().isOk());
        Thread.sleep(500);
        expectCounters(book, 1, ada, 0);
        api().get("/members/{id}", grace).andExpect(jsonPath("$.activeLoans").value(1));
    }

    @Test
    void loansAreListedPerMember() throws Exception {
        var book = book(5);
        var ada = member("ada");
        var grace = member("grace");
        var mine = lend(book, ada);
        lend(book, grace);

        List<String> loans = Api.read(api().get("/loans?member={member}", ada).andExpect(status().isOk()),
                "$.content[*].id");

        assertThat(loans).containsExactly(mine);
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        api().post("/books", "{\"title\": \"\", \"author\": \"X\", \"copies\": 1}").andExpect(status().isBadRequest());
        api().post("/books", "{\"title\": \"Dune\", \"author\": \"X\", \"copies\": 0}").andExpect(status().isBadRequest());
        api().post("/members", "{\"name\": \"Ada\"}").andExpect(status().isBadRequest());
    }
}
