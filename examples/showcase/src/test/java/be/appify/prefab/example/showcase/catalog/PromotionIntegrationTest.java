package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.example.showcase.capability.RequiresCapability;
import be.appify.prefab.example.showcase.catalog.infrastructure.http.PromotionResponse;
import be.appify.prefab.example.showcase.catalog.infrastructure.http.PromotionResponse.AmountOffResponse;
import be.appify.prefab.example.showcase.catalog.infrastructure.http.PromotionResponse.PercentageOffResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static be.appify.prefab.example.showcase.capability.Capability.POLYMORPHIC_AGGREGATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A polymorphic aggregate: the kinds of {@link Promotion} share one resource and keep their own fields. */
@IntegrationTest
@RequiresCapability(POLYMORPHIC_AGGREGATES)
class PromotionIntegrationTest {

    @Autowired
    PromotionClient promotions;

    @Test
    void createPercentageOff_canBeRetrievedAsThatKind() throws Exception {
        var promotionId = promotions.createPercentageOff("SPRING10", 10);

        var promotion = promotions.getPromotionById(promotionId);

        assertThat(promotion).isInstanceOfSatisfying(PercentageOffResponse.class, percentageOff -> {
            assertThat(percentageOff.code()).isEqualTo("SPRING10");
            assertThat(percentageOff.percentage()).isEqualTo(10);
        });
    }

    @Test
    void createAmountOff_canBeRetrievedAsThatKind() throws Exception {
        var promotionId = promotions.createAmountOff("WELCOME5", new BigDecimal("5.00"));

        var promotion = promotions.getPromotionById(promotionId);

        assertThat(promotion).isInstanceOfSatisfying(AmountOffResponse.class, amountOff -> {
            assertThat(amountOff.code()).isEqualTo("WELCOME5");
            assertThat(amountOff.amount()).isEqualByComparingTo("5.00");
        });
    }

    @Test
    void findPromotions_listsAllKindsTogether() throws Exception {
        promotions.createPercentageOff("SPRING10", 10);
        promotions.createAmountOff("WELCOME5", new BigDecimal("5.00"));

        var all = promotions.findPromotions(Pageable.unpaged());

        assertThat(all.content()).extracting(PromotionResponse::getClass)
                .containsExactlyInAnyOrder(PercentageOffResponse.class, AmountOffResponse.class);
    }

    @Test
    void createPercentageOff_givenMoreThanHundredPercent_isRejected() {
        assertThatThrownBy(() -> promotions.createPercentageOff("FREE", 101))
                .hasMessageContaining("but was:<400>");
    }
}
