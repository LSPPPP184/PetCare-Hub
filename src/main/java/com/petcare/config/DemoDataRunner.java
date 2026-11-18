package com.petcare.config;

import com.petcare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Khởi tạo demo theo thứ tự trong một giao dịch; database có dữ liệu được giữ nguyên hoàn toàn. */
@Component @Profile("demo") @RequiredArgsConstructor
public class DemoDataRunner implements CommandLineRunner {
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final AppointmentRepository appointments;
    private final OwnedPetRepository pets;
    private final LoyaltyAccountRepository accounts;
    private final SubscriptionRepository subscriptions;
    private final DataSeeder userSeeder;
    private final ProductDemoSeeder productSeeder;
    private final MatchingDemoSeeder matchingSeeder;
    private final AppointmentDemoSeeder appointmentSeeder;
    private final PetHealthDemoSeeder healthSeeder;
    private final LoyaltyDemoSeeder loyaltySeeder;
    private final SubscriptionDemoSeeder subscriptionSeeder;
    private final OrderDemoSeeder orderSeeder;

    @Override @Transactional
    public void run(String... args) {
        if (users.count() != 0 || products.count() != 0 || orders.count() != 0 || appointments.count() != 0
                || pets.count() != 0 || accounts.count() != 0 || subscriptions.count() != 0) return;
        userSeeder.run();
        productSeeder.run();
        matchingSeeder.seed();
        appointmentSeeder.seed();
        healthSeeder.seed();
        loyaltySeeder.seed();
        subscriptionSeeder.seed();
        orderSeeder.seed();
    }
}
