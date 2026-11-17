package com.petcare.dto;

import java.math.BigDecimal;

public record LoyaltyQuote(String tierName, BigDecimal tierDiscount, BigDecimal voucherDiscount, BigDecimal finalAmount) {
    public BigDecimal discountAmount() { return tierDiscount.add(voucherDiscount); }
}
