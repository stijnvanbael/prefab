package be.appify.prefab.example.avro.sale;

import be.appify.prefab.example.avro.cashregister.CashRegisterClient;
import be.appify.prefab.example.avro.cashregister.application.CashRegisterCashInRequest;
import be.appify.prefab.example.avro.cashregister.application.CreateCashRegisterRequest;
import be.appify.prefab.example.avro.customer.CustomerClient;
import be.appify.prefab.example.avro.customer.PersonName;
import be.appify.prefab.example.avro.customer.application.CreateCustomerRequest;
import be.appify.prefab.example.avro.sale.application.CreateSaleRequest;
import be.appify.prefab.example.avro.sale.application.SaleAddCustomerRequest;
import be.appify.prefab.example.avro.sale.application.SaleAddLineRequest;
import be.appify.prefab.example.avro.sale.application.SalePayRequest;
import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class SaleIntegrationTest {

    @Autowired
    CashRegisterClient cashRegisters;

    @Autowired
    CustomerClient customers;

    @Autowired
    SaleClient sales;

    @Test
    void sale() throws Exception {
        var cashRegisterId = cashRegisters.createCashRegister(new CreateCashRegisterRequest("Service 1"));
        cashRegisters.cashIn(cashRegisterId, new CashRegisterCashInRequest(100.0));
        var customerId = customers.createCustomer(new CreateCustomerRequest(new PersonName("John", "Doe"), "john.doe@gmail.com"));
        var saleId = sales.createSale(new CreateSaleRequest(cashRegisterId));
        sales.addCustomer(saleId, new SaleAddCustomerRequest(customerId));
        sales.addLine(saleId, new SaleAddLineRequest("Shampoo", 2, 3.5));
        sales.addLine(saleId, new SaleAddLineRequest("Toothpaste", 1, 4.0));
        sales.pay(saleId, new SalePayRequest(7.5, Sale.PaymentMethod.CASH));

        await().untilAsserted(() -> {
            var cashRegister = cashRegisters.getCashRegisterById(cashRegisterId);
            assertThat(cashRegister.cashInDrawer()).isEqualTo(107.5);
        });
    }
}
