package com.petcare;

import com.petcare.config.SubscriptionDemoSeeder;
import com.petcare.domain.subscription.SubscriptionStatus;
import com.petcare.repository.*;
import com.petcare.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:subscription_demo;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "demo"}) @Import(SubscriptionDemoTest.FixedTime.class)
class SubscriptionDemoTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
    @Autowired SubscriptionDemoSeeder seeder;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired SubscriptionService service;
    @Autowired UserRepository users;
    @Test @WithMockUser(username = "customer@petcare.vn", roles = "CUSTOMER")
    void stableKeysPreserveVietnameseAndDoNotResetUsedOrCancelledPackages() {
        var customerId = users.findByEmail("customer@petcare.vn").orElseThrow().getId();
        var before = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId);
        assertThat(before).extracting(s -> s.getStateName()).containsExactlyInAnyOrder(SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELLED);
        var views = service.mine();
        assertThat(views).allSatisfy(v -> assertThat(v.packageName()).isEqualTo("Gói tháng 1"));
        assertThat(views).anySatisfy(v -> assertThat(v.services()).contains("Tắm tỉa 1", "Khám thú y 1"));
        var active = before.stream().filter(s -> s.getStateName() == SubscriptionStatus.ACTIVE).findFirst().orElseThrow();
        assertThat(active.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(views).filteredOn(v -> v.id().equals(active.getId())).singleElement().satisfies(v -> {
            assertThat(v.stateLabel()).isEqualTo(SubscriptionStatus.ACTIVE.getLabel());
            assertThat(v.renewable()).isFalse();
        });
        service.cancel(active.getId());
        seeder.seed(); seeder.seed();
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).extracting(s -> s.getId())
            .containsExactlyElementsOf(before.stream().map(s -> s.getId()).toList());
        assertThat(subscriptions.findById(active.getId()).orElseThrow().getStateName()).isEqualTo(SubscriptionStatus.CANCELLED);
    }
}
