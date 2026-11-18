package com.petcare.config;

import com.petcare.domain.*;
import com.petcare.repository.*;
import com.petcare.service.CommerceLocks;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Comparator;

/** Khóa khách và sourceKey cố định; chạy lại không nhân bản hoặc đặt lại lượt đã sử dụng. */
@Component @Profile("demo") @RequiredArgsConstructor
public class SubscriptionDemoSeeder {
    private final UserRepository users;
    private final ProductRepository products;
    private final AppointmentServiceRepository services;
    private final SubscriptionRepository subscriptions;
    private final CommerceLocks locks;
    private final Clock clock;
    @Transactional
    public void seed() {
        var customer = users.findByEmail("customer@petcare.vn").orElse(null);
        if (customer == null) return;
        locks.lock(User.class, customer.getId());
        var pack = products.findAll().stream().sorted(Comparator.comparing(Product::getId))
            .flatMap(p -> p.subscriptionPackage().stream()).findFirst().orElse(null);
        if (pack == null) return;
        var today = LocalDate.now(clock);
        if (!subscriptions.existsBySourceKey("DEMO:SUBSCRIPTION:ACTIVE")) {
            if (pack.getCoveredServices().isEmpty()) pack.configureCoveredServices(services.findBookable());
            subscriptions.save(Subscription.create(customer, null, pack, today, 1, "DEMO:SUBSCRIPTION:ACTIVE"));
        }
        if (!subscriptions.existsBySourceKey("DEMO:SUBSCRIPTION:EXPIRED")) {
            var expired = Subscription.create(customer, null, pack, today.minusMonths(pack.getDurationMonths() + 1L), 1, "DEMO:SUBSCRIPTION:EXPIRED");
            expired.expire(today); subscriptions.save(expired);
        }
        if (!subscriptions.existsBySourceKey("DEMO:SUBSCRIPTION:CANCELLED")) {
            var cancelled = Subscription.create(customer, null, pack, today, 1, "DEMO:SUBSCRIPTION:CANCELLED");
            cancelled.cancel(); subscriptions.save(cancelled);
        }
        for (String code : java.util.List.of("silver", "gold")) {
            var member = users.findByEmail("loyalty." + code + "@petcare.vn").orElseThrow();
            var key = "DEMO:SUBSCRIPTION:MEMBER:" + code;
            if (!subscriptions.existsBySourceKey(key))
                subscriptions.save(Subscription.create(member, null, pack, today, 1, key));
        }
    }
}
