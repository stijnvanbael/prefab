package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Search;
import be.appify.prefab.processor.problem.BadRequestProblem;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Aggregate
@Search(property = "code")
@GetById
@DbMigration(version = 3)
public class GiftVoucher {
    @NotNull
    private final String code;
    @NotNull
    private BigDecimal remainingValue;

    @Create
    public GiftVoucher(@NotNull String code, @NotNull BigDecimal remainingValue) {
        this.code = code;
        this.remainingValue = remainingValue;
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
