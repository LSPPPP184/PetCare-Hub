package com.petcare;

import com.petcare.config.*;
import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.dto.ProductForm;
import com.petcare.repository.*;
import com.petcare.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:dashboard;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test") @Import(DashboardIntegrationTest.Time.class) @Transactional
class DashboardIntegrationTest {
    @TestConfiguration static class Time {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2025-12-31T17:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
    @Autowired DashboardService dashboard;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired AppointmentRepository appointments;
    @Autowired OwnedPetRepository pets;
    @Autowired VaccinationRecordRepository vaccinations;
    @Autowired LoyaltyAccountRepository accounts;
    @Autowired LoyaltyProperties loyalty;
    private final LocalDate today = LocalDate.of(2026, 1, 1);

    @Test void emptyDashboardIncludesAllMonthsAndTiers() {
        var view = dashboard.overview();
        assertThat(view.today()).isEqualTo(today);
        assertThat(view.revenue()).hasSize(12).allSatisfy(r -> assertThat(r.amount()).isZero());
        assertThat(view.revenue().getFirst().month()).isEqualTo(YearMonth.of(2025, 2));
        assertThat(view.revenue().getLast().month()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(view.members()).hasSize(4).allSatisfy(m -> assertThat(m.count()).isZero());
        assertThat(view.bestSellers()).isEmpty();
    }

    @Test void revenueUsesCompletedNetAmountsWindowAndDeterministicTopFive() {
        var customer = users.findByEmail("customer@petcare.vn").orElseThrow();
        for (int i = 0; i < 6; i++) {
            var f = new ProductForm(); f.setType(ProductType.ACCESSORY); f.setName("Phụ kiện " + i);
            f.setBasePrice(new BigDecimal("100.00")); f.setStockQuantity(100);
            var product = products.save(new ProductFactory().create(f));
            order(customer, product, 2, today.atStartOfDay(), true);
            order(customer, product, 1, LocalDateTime.of(2025, 2, 1, 0, 0), true);
            order(customer, product, 3, LocalDateTime.of(2025, 1, 31, 23, 59), true);
            order(customer, product, 4, LocalDateTime.of(2026, 2, 1, 0, 0), false);
        }
        var view = dashboard.overview();
        assertThat(view.revenue().getFirst().amount()).isEqualByComparingTo("540.00");
        assertThat(view.revenue().getLast().amount()).isEqualByComparingTo("1140.00");
        assertThat(view.revenue().subList(1, 11)).allSatisfy(r -> assertThat(r.amount()).isZero());
        assertThat(view.bestSellers()).hasSize(5).allSatisfy(p -> assertThat(p.quantity()).isEqualTo(6));
        assertThat(view.bestSellers()).extracting(p -> p.name()).containsExactly("Phụ kiện 0", "Phụ kiện 1", "Phụ kiện 2", "Phụ kiện 3", "Phụ kiện 4");
    }

    private void order(User customer, Product product, int quantity, LocalDateTime time, boolean completed) {
        var cart = new Cart(customer); cart.add(product, quantity);
        var order = Order.fromCart(cart, "Địa chỉ kiểm thử");
        order.snapshotDiscount(new BigDecimal("10.00")); order.recordCreatedAt(time);
        if (completed) { order.confirm(); order.ship(); order.complete(); }
        orders.save(order);
    }

    @Test void countsDistinctPetsDateBoundariesAppointmentsAndTierThresholds() {
        var customer = users.findByEmail("customer@petcare.vn").orElseThrow();
        var pet = pets.save(OwnedPet.create(customer, "Bông có dấu", Species.CAT, "", today.minusYears(2), "", "", today));
        for (int offset : new int[]{-1, 0, 7, 8})
            vaccinations.save(VaccinationRecord.schedule(pet, "Mũi " + offset, today.plusDays(offset), "", today.minusDays(20), 7));
        var completedPet = pets.save(OwnedPet.create(customer, "Đã tiêm", Species.DOG, "", today.minusYears(2), "", "", today));
        var completed = VaccinationRecord.schedule(completedPet, "Đã xong", today.minusDays(1), "", today, 7);
        completed.complete(today, today); vaccinations.save(completed);
        var f = new ProductForm(); f.setType(ProductType.GROOMING); f.setName("Tắm tỉa"); f.setDurationMinutes(30);
        var service = (ServiceItem) products.save(new ProductFactory().create(f));
        var staff = users.findByEmail("staff@petcare.vn").orElseThrow();
        for (var time : java.util.List.of(today.minusDays(1).atTime(23, 59), today.atStartOfDay(), today.atTime(23, 59), today.plusDays(1).atStartOfDay()))
            appointments.save(Appointment.create(customer, "Bông", Species.CAT, service, staff, time, ""));
        var cancelled = Appointment.create(customer, "Bông", Species.CAT, service, staff, today.atTime(12, 0), "");
        cancelled.cancel(); appointments.save(cancelled);
        for (int points : new int[]{0, 499, 500, 1999, 2000, 4999, 5000}) {
            var member = users.save(new User("Khách " + points, "tier" + points + "@test.vn", "test-hash", "", Role.CUSTOMER));
            var account = new LoyaltyAccount(member); account.credit(points, loyalty); accounts.save(account);
        }
        var view = dashboard.overview();
        assertThat(view.petsDueSoon()).isEqualTo(1);
        assertThat(view.petsOverdue()).isEqualTo(1);
        assertThat(view.appointmentsToday()).isEqualTo(2);
        assertThat(view.members()).extracting(m -> m.count()).containsExactly(2L, 2L, 2L, 1L);
    }
}
