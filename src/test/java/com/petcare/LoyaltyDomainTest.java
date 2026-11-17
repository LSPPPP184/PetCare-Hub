package com.petcare;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.*;
import com.petcare.domain.discount.*;
import com.petcare.domain.loyalty.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class LoyaltyDomainTest {
    private final LoyaltyProperties properties = new LoyaltyProperties();
    private LoyaltyAccount account() { return new LoyaltyAccount(new User("Khách", "test@example.vn", "hash", "", Role.CUSTOMER)); }
    @ParameterizedTest @CsvSource({"BRONZE,0,100000", "SILVER,0.03,97000", "GOLD,0.05,95000", "PLATINUM,0.10,90000"})
    void strategyUsesTierRate(MembershipTier tier, String rate, String amount) {
        var strategy = new DiscountStrategyFactory(properties).forTier(tier);
        assertThat(strategy.discountRate()).isEqualByComparingTo(rate);
        assertThat(strategy.apply(new BigDecimal("100000"))).isEqualByComparingTo(amount);
        assertThat(strategy.apply(BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThatThrownBy(() -> strategy.apply(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @CsvSource({"0,BRONZE", "499,BRONZE", "500,SILVER", "501,SILVER", "1999,SILVER", "2000,GOLD", "2001,GOLD", "4999,GOLD", "5000,PLATINUM", "5001,PLATINUM"})
    void exactThresholds(long points, MembershipTier tier) {
        var account = account(); account.credit(points, properties);
        assertThat(account.getTier()).isEqualTo(tier);
    }
    @Test void thresholdsAndRatesAreConfigurable() {
        properties.tier(MembershipTier.SILVER).setThreshold(100);
        properties.tier(MembershipTier.SILVER).setRate(new BigDecimal("0.07"));
        properties.validate();
        assertThat(properties.tierFor(99)).isEqualTo(MembershipTier.BRONZE);
        assertThat(properties.tierFor(100)).isEqualTo(MembershipTier.SILVER);
        assertThat(new SilverDiscount(properties).apply(new BigDecimal("100"))).isEqualByComparingTo("93");
        properties.tier(MembershipTier.GOLD).setThreshold(100);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
    }
    @Test void redeemAndNegativeAdjustmentNeverLowerLifetimePointsOrTier() {
        var a = account(); a.credit(500, properties); a.redeem();
        assertThat(a.getCurrentPoints()).isEqualTo(400);
        assertThat(a.getTotalPoints()).isEqualTo(500);
        assertThat(a.getTier()).isEqualTo(MembershipTier.SILVER);
        assertThatThrownBy(a::redeem).isInstanceOf(LoyaltyException.class);
        a.consumeVoucher(); a.adjust(-400, properties);
        assertThat(a.getCurrentPoints()).isZero();
        assertThat(a.getTier()).isEqualTo(MembershipTier.SILVER);
        assertThatThrownBy(a::redeem).isInstanceOf(LoyaltyException.class);
        assertThatThrownBy(() -> a.adjust(-1, properties)).isInstanceOf(LoyaltyException.class);
        assertThatThrownBy(() -> a.adjust(Long.MIN_VALUE, properties)).isInstanceOf(LoyaltyException.class);
        assertThat(a.getCurrentPoints()).isZero();
    }
    @Test void redeemExactBalanceAndRoundDiscount() {
        var a = account(); a.credit(100, properties); a.redeem();
        assertThat(a.getCurrentPoints()).isZero(); assertThat(a.isVoucherAvailable()).isTrue();
        assertThat(new SilverDiscount(properties).apply(new BigDecimal("100.50"))).isEqualByComparingTo("97.48");
    }
}
