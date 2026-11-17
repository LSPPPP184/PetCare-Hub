package com.petcare.domain.discount;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.MembershipTier;

/** Strategy giảm giá cho hạng Gold, tỷ lệ lấy từ cấu hình. */
public class GoldDiscount extends AbstractTierDiscount {
    public GoldDiscount(LoyaltyProperties properties) { super(properties, MembershipTier.GOLD); }
}
