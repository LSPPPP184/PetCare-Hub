package com.petcare.domain.discount;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.MembershipTier;
import org.springframework.stereotype.Component;
import java.util.Map;

/** Factory tra cứu Strategy qua bảng đăng ký, không rẽ nhánh theo loại hạng. */
@Component
public class DiscountStrategyFactory {
    private final Map<MembershipTier, DiscountStrategy> strategies;
    public DiscountStrategyFactory(LoyaltyProperties properties) {
        strategies = Map.of(MembershipTier.BRONZE, new BronzeDiscount(properties), MembershipTier.SILVER, new SilverDiscount(properties),
            MembershipTier.GOLD, new GoldDiscount(properties), MembershipTier.PLATINUM, new PlatinumDiscount(properties));
    }
    public DiscountStrategy forTier(MembershipTier tier) { return strategies.get(tier); }
}
