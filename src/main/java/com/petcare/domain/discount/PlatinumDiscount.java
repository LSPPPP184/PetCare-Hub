package com.petcare.domain.discount;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.MembershipTier;

/** Strategy giảm giá cho hạng Platinum, tỷ lệ lấy từ cấu hình. */
public class PlatinumDiscount extends AbstractTierDiscount {
    public PlatinumDiscount(LoyaltyProperties properties) { super(properties, MembershipTier.PLATINUM); }
}
