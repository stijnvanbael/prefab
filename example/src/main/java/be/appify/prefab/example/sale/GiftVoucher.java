package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.problem.BadRequestProblem;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Aggregate
@GetById
@GetList
@DbMigration
public class GiftVoucher {
    @Id
    private String id;
    @Version
    private long version;
    @NotNull
    @Filter(operator = Filter.Operator.STARTS_WITH)
    private final String code;
    @NotNull
    private BigDecimal remainingValue;

    @PersistenceCreator
    public GiftVoucher(String id, long version, String code, BigDecimal remainingValue) {
        this.id = id;
        this.version = version;
        this.code = code;
        this.remainingValue = remainingValue;
    }

    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    @Create
    public GiftVoucher(@NotNull String code, @NotNull BigDecimal remainingValue) {
        this(UUID.randomUUID().toString(), 0, code, remainingValue);
    }

    public String code() {
        return code;
    }

    public BigDecimal remainingValue() {
        return remainingValue;
    }

    @EventHandler.ByReference("giftVoucher")
    public void onSalePaidWithGiftVoucher(SalePaidWithGiftVoucher event) {
        var amount = event.amount();
        if (amount.compareTo(remainingValue) > 0) {
            throw new BadRequestProblem("Cannot redeem more than the remaining value of the gift voucher");
        }
        remainingValue = remainingValue.subtract(amount);
    }
}



