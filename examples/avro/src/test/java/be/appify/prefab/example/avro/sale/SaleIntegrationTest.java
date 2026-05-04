package be.appify.prefab.example.avro.sale;

import be.appify.prefab.core.outbox.OutboxRelayService;
import be.appify.prefab.example.avro.cashregister.CashRegisterClient;
import be.appify.prefab.example.avro.customer.CustomerClient;
import be.appify.prefab.example.avro.customer.PersonName;
import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class SaleIntegrationTest {

    @Autowired
    OutboxRelayService outboxRelayService;

    @Autowired
    CashRegisterClient cashRegisters;

    @Autowired
    CustomerClient customers;

    @Autowired
    SaleClient sales;

    @Test
    void sale() throws Exception {
        var cashRegisterId = cashRegisters.createCashRegister("Service 1");
        cashRegisters.cashIn(cashRegisterId, 100.0);
        var customerId = customers.createCustomer(new PersonName("John", "Doe"), "john.doe@gmail.com");
        var saleId = sales.createSale(cashRegisterId);
        sales.addCustomer(saleId, customerId);
        sales.addLine(saleId, "Shampoo", 2, 3.5);
        sales.addLine(saleId, "Toothpaste", 1, 4.0);
        sales.pay(saleId, 7.5, Sale.PaymentMethod.CASH);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var cashRegister = cashRegisters.getCashRegisterById(cashRegisterId);
            assertThat(cashRegister.cashInDrawer()).isEqualTo(107.5);
        });
    }
}
