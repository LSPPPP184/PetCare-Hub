package com.petcare.domain.discount;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.MembershipTier;

/** Strategy giảm giá cho hạng Silver, tỷ lệ lấy từ cấu hình. */
public class SilverDiscount extends AbstractTierDiscount {
    public SilverDiscount(LoyaltyProperties properties) { super(properties, MembershipTier.SILVER); }
}
