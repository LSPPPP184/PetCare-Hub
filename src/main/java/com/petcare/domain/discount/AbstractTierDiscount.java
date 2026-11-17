package com.petcare.domain.discount;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.MembershipTier;
import java.math.*;

/** Trừu tượng hóa cách làm tròn; các Strategy cụ thể chọn cấu hình quyền lợi của hạng. */
public abstract class AbstractTierDiscount implements DiscountStrategy {
    private final LoyaltyProperties properties;
    private final MembershipTier tier;
    protected AbstractTierDiscount(LoyaltyProperties properties, MembershipTier tier) {
        this.properties = properties; this.tier = tier;
    }
    public BigDecimal discountRate() { return properties.tier(tier).getRate(); }
    public BigDecimal apply(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("Số tiền không hợp lệ.");
        return amount.subtract(amount.multiply(discountRate()).setScale(2, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
    }
}
