package com.example.billing;

import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InvoiceIntegrationTest {
    @Autowired
    InvoiceClient invoices;
    @Autowired
    MockMvc mockMvc;

    @Test
    void createdInvoiceCanBeRead() throws Exception {
        var id = invoices.createInvoice("Ada", new BigDecimal("120.50"));

        var invoice = invoices.getInvoiceById(id);

        assertThat(invoice.customer()).isEqualTo("Ada");
        assertThat(invoice.amount()).isEqualByComparingTo("120.50");
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void payingMarksTheInvoicePaid() throws Exception {
        var id = invoices.createInvoice("Ada", new BigDecimal("10"));

        mockMvc.perform(post("/invoices/{id}/pay", id)).andExpect(status().isOk());

        assertThat(invoices.getInvoiceById(id).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void payingTwiceIsAConflict() throws Exception {
        var id = invoices.createInvoice("Ada", new BigDecimal("10"));
        mockMvc.perform(post("/invoices/{id}/pay", id)).andExpect(status().isOk());

        mockMvc.perform(post("/invoices/{id}/pay", id)).andExpect(status().isConflict());
    }
}
