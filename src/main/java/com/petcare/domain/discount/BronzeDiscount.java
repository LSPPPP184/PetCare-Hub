package com.petcare.domain.discount;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.loyalty.MembershipTier;

/** Strategy giảm giá cho hạng Bronze, tỷ lệ lấy từ cấu hình. */
public class BronzeDiscount extends AbstractTierDiscount {
    public BronzeDiscount(LoyaltyProperties properties) { super(properties, MembershipTier.BRONZE); }
}
