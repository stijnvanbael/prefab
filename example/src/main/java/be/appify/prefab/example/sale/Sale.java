package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.problem.BadRequestProblem;
import be.appify.prefab.core.problem.ConflictProblem;
import be.appify.prefab.core.problem.RequiredProblem;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static be.appify.prefab.core.annotations.rest.HttpMethod.POST;

@Aggregate
@GetById(security = @Security(authority = "sale:view"))
@DbMigration
public class Sale implements PublishesEvents {
    @Id
    private String id;
    @Version
    private long version;
    @NotNull
    private final Instant start;
    private final List<SaleItem> items;
    private final List<Payment> payments;
    @NotNull
    private BigDecimal returned;
    @NotNull
    private State state;
    private Reference<Customer> customer;
    private SaleType type;

    @Create(security = @Security(authority = "sale:create"))
    public Sale(SaleType type) {
        this(
                UUID.randomUUID().toString(),
                0,
                Instant.now(),
                List.of(),
                List.of(),
                BigDecimal.ZERO,
                State.OPEN,
                null,
                type
        );
    }

    @PersistenceCreator
    public Sale(
            String id,
            long version,
            Instant start,
            List<SaleItem> items,
            List<Payment> payments,
            BigDecimal returned,
            State state,
            Reference<Customer> customer,
            SaleType type
    ) {
        this.id = id;
        this.version = version;
        this.start = start;
        this.items = items;
        this.payments = payments;
        this.returned = returned;
        this.state = state;
        this.customer = customer;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    public Instant start() {
        return start;
    }

    public List<SaleItem> items() {
        return items;
    }

    public List<Payment> payments() {
        return payments;
    }

    public BigDecimal returned() {
        return returned;
    }

    public State state() {
        return state;
    }

    public Reference<Customer> customer() {
        return customer;
    }

    public SaleType type() {
        return type;
    }

    public BigDecimal total() {
        return items.stream()
                .map(item -> item.price().multiply(item.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Update(method = POST, path = "/items", security = @Security(authority = "sale:add-item"))
    public void addItem(
            @NotNull String description,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal price
    ) {
        if (state != State.OPEN) {
            throw new ConflictProblem("Sale is not open");
        }
        items.add(new SaleItem(description, quantity, price));
    }

    @Update(method = POST, path = "/payments", security = @Security(authority = "sale:add-payment"))
    public void addPayment(
            @NotNull BigDecimal amount,
            @NotNull PaymentMethod method,
            Reference<GiftVoucher> giftVoucher
    ) {
        checkPaymentState();
        checkGiftVoucher(method, giftVoucher);
        payments.add(new Payment(amount, method, giftVoucher));
        var amountDue = calculateAmountDue();
        resolveGiftVoucher(amount, method, giftVoucher);
        if (amountDue.compareTo(BigDecimal.ZERO) > 0) {
            state = State.PAYMENT;
        } else {
            complete();
        }
        returned = amountDue.compareTo(BigDecimal.ZERO) < 0 ? amountDue.abs() : BigDecimal.ZERO;
        if (returned.compareTo(BigDecimal.ZERO) > 0 && method != PaymentMethod.CASH) {
            throw new BadRequestProblem("Can only return money with cash payment");
        }
    }

    @Update(method = POST, path = "/type", security = @Security(authority = "sale:set-type"))
    public void setType(@NotNull SaleType type) {
        this.type = type;
    }

    private void complete() {
        state = State.COMPLETED;
        publish(new SaleCompleted(
                id,
                start,
                items,
                returned,
                type
        ));
    }

    private void resolveGiftVoucher(BigDecimal amount, PaymentMethod method, Reference<GiftVoucher> giftVoucher) {
        if (method == PaymentMethod.GIFT_VOUCHER) {
            publish(new SalePaidWithGiftVoucher(giftVoucher, amount));
        }
    }

    private BigDecimal calculateAmountDue() {
        return total().subtract(payments.stream()
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void checkGiftVoucher(PaymentMethod method, Reference<GiftVoucher> giftVoucher) {
        if (method == PaymentMethod.GIFT_VOUCHER && giftVoucher == null) {
            throw new RequiredProblem("Gift voucher on a gift voucher payment");
        }
    }

    private void checkPaymentState() {
        if (state != State.OPEN && state != State.PAYMENT) {
            throw new ConflictProblem("Sale is not open or in payment state");
        }
    }

    @Update(method = POST, path = "/cancel", security = @Security(authority = "sale:cancel"))
    public void cancel() {
        if (state != State.OPEN) {
            throw new ConflictProblem("Sale is not open");
        }
        state = State.CANCELLED;
    }

    @Update(method = POST, path = "/customer", security = @Security(authority = "sale:add-customer"))
    public void addCustomer(Reference<Customer> customer) {
        this.customer = customer;
    }
}
