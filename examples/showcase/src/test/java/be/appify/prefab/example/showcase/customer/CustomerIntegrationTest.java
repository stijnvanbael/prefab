package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code @Create} and {@code @GetById} on the customer resource. Deactivating a customer is tested with its orders in
 * {@code OrderCancellationOnDeactivationIntegrationTest}. */
@IntegrationTest
class CustomerIntegrationTest {

    @Autowired
    CustomerClient customers;

    @Test
    void createCustomer_givenDetails_canBeRetrievedAsActive() throws Exception {
        var customerId = customers.createCustomer("Ada", "Lovelace", "ada@example.com");

        var customer = customers.getCustomerById(customerId);

        assertThat(customer.firstName()).isEqualTo("Ada");
        assertThat(customer.lastName()).isEqualTo("Lovelace");
        assertThat(customer.email()).isEqualTo("ada@example.com");
        assertThat(customer.active()).isTrue();
    }

    @Test
    void createCustomer_givenInvalidEmail_isRejected() {
        assertThatThrownBy(() -> customers.createCustomer("Ada", "Lovelace", "not-an-email"))
                .hasMessageContaining("was:<400>");
    }
}
