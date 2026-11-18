package com.petcare.config;

import com.petcare.domain.*;
import com.petcare.domain.loyalty.*;
import com.petcare.repository.*;
import com.petcare.service.LoyaltyAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Locale;

/** Seed nhận diện bằng email và sourceKey cố định; không đặt lại số dư khi khởi động lại. */
@Component @Profile("demo") @RequiredArgsConstructor
public class LoyaltyDemoSeeder {
    private final UserRepository users;
    private final LoyaltyAccounts accounts;
    private final PointTransactionRepository transactions;
    private final LoyaltyProperties properties;
    private final PasswordEncoder encoder;
    private final Clock clock;
    @Transactional
    public void seed() {
        for (var tier : MembershipTier.values()) {
            var code = tier.name().toLowerCase(Locale.ROOT);
            var email = "loyalty." + code + "@petcare.vn";
            var customer = users.findByEmail(email).orElseGet(() -> users.saveAndFlush(new User(
                "Khách hạng " + properties.tier(tier).getName(), email, encoder.encode("Customer@123"), "", Role.CUSTOMER)));
            var account = accounts.locked(customer.getId());
            var key = "DEMO:LOYALTY:" + code;
            if (transactions.existsBySourceKey(key)) continue;
            long points = tier == MembershipTier.BRONZE ? Math.min(490, properties.tier(MembershipTier.SILVER).getThreshold() - 1)
                : properties.tier(tier).getThreshold();
            account.credit(points, properties);
            transactions.save(new PointTransaction(account, PointTransactionType.ADJUST, points,
                "Điểm khởi tạo thẻ thành viên " + properties.tier(tier).getName() + " — dữ liệu minh họa",
                null, null, LocalDateTime.now(clock), key));
        }
    }
}
