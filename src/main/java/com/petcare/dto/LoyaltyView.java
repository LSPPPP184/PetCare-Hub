package com.petcare.dto;

import com.petcare.domain.loyalty.MembershipTier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoyaltyView(Long customerId, String customerName, String email, MembershipTier tier, String tierName,
                          long totalPoints, long currentPoints, boolean voucherAvailable, String nextTierName,
                          long pointsToNext, int progress, List<Benefit> benefits, List<Transaction> history) {
    public record Benefit(String name, long threshold, BigDecimal percent, String description) {}
    public record Transaction(String type, long points, String description, LocalDateTime createdAt, Long orderId, Long appointmentId) {}
}
