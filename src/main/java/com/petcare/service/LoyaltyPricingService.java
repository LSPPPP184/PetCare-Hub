package com.petcare.service;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.LoyaltyAccount;
import com.petcare.domain.discount.DiscountStrategyFactory;
import com.petcare.domain.loyalty.MembershipTier;
import com.petcare.dto.LoyaltyQuote;
import com.petcare.repository.LoyaltyAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;

/** Chốt quyền lợi một lần lúc tạo; voucher chỉ dành cho đơn hàng kế tiếp, không dành cho lịch hẹn. */
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class LoyaltyPricingService {
    private final LoyaltyAccounts accounts;
    private final LoyaltyAccountRepository repository;
    private final DiscountStrategyFactory strategies;
    private final LoyaltyProperties properties;
    private final CommerceAccess access;
    private LoyaltyQuote quote(LoyaltyAccount account, BigDecimal amount, boolean useVoucher) {
        var tier = account == null ? MembershipTier.BRONZE : account.currentTier(properties);
        var afterTier = strategies.forTier(tier).apply(amount);
        var voucher = useVoucher && account != null && account.isVoucherAvailable()
            ? afterTier.min(new BigDecimal("10000.00")) : BigDecimal.ZERO;
        return new LoyaltyQuote(properties.tier(tier).getName(), amount.subtract(afterTier), voucher, afterTier.subtract(voucher));
    }
    public LoyaltyQuote preview(BigDecimal amount, boolean order) {
        var account = repository.findByCustomerId(access.currentCustomer().getId()).orElse(null);
        return quote(account, amount, order);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public LoyaltyQuote snapshot(Long customerId, BigDecimal amount, boolean order) {
        var account = accounts.locked(customerId);
        var quote = quote(account, amount, order);
        if (order && quote.voucherDiscount().signum() > 0) account.consumeVoucher();
        return quote;
    }
}
