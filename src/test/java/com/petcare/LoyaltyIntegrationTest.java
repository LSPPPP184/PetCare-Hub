package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.loyalty.*;
import com.petcare.domain.order.*;
import com.petcare.domain.appointment.AppointmentStatus;
import com.petcare.dto.AppointmentForm;
import com.petcare.event.*;
import com.petcare.repository.*;
import com.petcare.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:loyalty_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test") @AutoConfigureMockMvc @Import(LoyaltyIntegrationTest.FixedTime.class)
class LoyaltyIntegrationTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
    @Autowired LoyaltyService loyalty;
    @Autowired CartService carts;
    @Autowired OrderService orderService;
    @Autowired AppointmentService appointmentService;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired AppointmentRepository appointments;
    @Autowired LoyaltyAccountRepository accounts;
    @Autowired PointTransactionRepository transactions;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    String email;
    Long customerId;
    <T> T db(Supplier<T> work) { return new TransactionTemplate(manager).execute(s -> work.get()); }
    <T> T as(String actor, Supplier<T> work) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(actor, "", List.of()));
        SecurityContextHolder.setContext(context);
        try { return work.get(); } finally { SecurityContextHolder.setContext(previous); }
    }
    <T> T customer(Supplier<T> work) { return as(email, work); }
    <T> T staff(Supplier<T> work) { return as("staff@petcare.vn", work); }
    void adjust(long points) { as("admin@petcare.vn", () -> { loyalty.adjust(customerId, points, "Điểm kiểm thử"); return null; }); }
    @BeforeEach void setup() {
        email = "loyalty-test-" + UUID.randomUUID() + "@petcare.vn";
        customerId = users.saveAndFlush(new User("Khách thử có dấu", email, "test-hash", "", Role.CUSTOMER)).getId();
    }
    Long product(ProductType type, String price) {
        var form = ProductFactoryTest.form(type, "Sản phẩm kiểm thử", price); form.setStockQuantity(100); form.setDurationMinutes(30);
        form.setPetSizeSurcharge(java.math.BigDecimal.ZERO);
        return products.saveAndFlush(new ProductFactory().create(form)).getId();
    }
    Long place(String price) {
        var product = product(ProductType.ACCESSORY, price);
        return customer(() -> { carts.addItem(customerId, product, 1); return orderService.placeOrderFromCart(customerId, "Địa chỉ thử").getId(); });
    }
    void shipping(Long id) { staff(() -> { orderService.confirm(id); orderService.ship(id); return null; }); }
    void complete(Long id) { staff(() -> orderService.complete(id)); }
    long countEarn(Long accountId) { return transactions.findByAccountIdOrderByCreatedAtDescIdDesc(accountId).stream().filter(t -> t.getType() == PointTransactionType.EARN).count(); }

    @Test void completionEarnsOncePromotesAndKeepsSnapshot() {
        adjust(490);
        Long first = place("109999.99"); shipping(first); complete(first);
        var card = customer(loyalty::mine);
        assertThat(card.totalPoints()).isEqualTo(500); assertThat(card.tier()).isEqualTo(MembershipTier.SILVER);
        assertThat(card.tierName()).isEqualTo("Bạc");
        assertThat(card.benefits()).extracting(b -> b.name()).containsExactly("Đồng", "Bạc", "Vàng", "Kim cương");
        assertThat(card.benefits().get(1).description()).isEqualTo("Giảm 3% khi mua hàng và đặt lịch.");
        assertThat(orders.findById(first).orElseThrow().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 5, 8, 0));
        db(() -> { events.publishEvent(new OrderCompletedEvent(first)); events.publishEvent(new OrderCompletedEvent(first)); return null; });
        assertThatThrownBy(() -> complete(first)).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(customer(loyalty::mine).totalPoints()).isEqualTo(500);
        var account = accounts.findByCustomerId(customerId).orElseThrow();
        assertThat(countEarn(account.getId())).isEqualTo(1);
        assertThat(transactions.findByAccountIdOrderByCreatedAtDescIdDesc(account.getId())).allSatisfy(t ->
            assertThat(t.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 5, 8, 0)));
        Long second = place("100000");
        assertThat(orders.findById(second).orElseThrow().getDiscountAmount()).isEqualByComparingTo("3000");
        adjust(4500);
        assertThat(orders.findById(first).orElseThrow().getFinalAmount()).isEqualByComparingTo("109999.99");
        assertThat(orders.findById(second).orElseThrow().getFinalAmount()).isEqualByComparingTo("97000");
    }
    @Test void rollbackCompletionAlsoRollsBackPointsAndLedger() {
        Long id = place("100000"); shipping(id);
        assertThatThrownBy(() -> staff(() -> db(() -> {
            orderService.complete(id); throw new IllegalStateException("Buộc rollback");
        }))).isInstanceOf(IllegalStateException.class);
        assertThat(orders.findById(id).orElseThrow().getStateName()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(customer(loyalty::mine).currentPoints()).isZero();
        assertThat(transactions.existsBySourceKey("EARN:ORDER:" + id)).isFalse();
        complete(id); assertThat(customer(loyalty::mine).currentPoints()).isEqualTo(10);
    }
    @Test void redeemIsAtomicVoucherIsConsumedOnceAndTierIsRetained() {
        adjust(500);
        customer(() -> { loyalty.redeem(); return null; });
        var card = customer(loyalty::mine);
        assertThat(card.currentPoints()).isEqualTo(400); assertThat(card.totalPoints()).isEqualTo(500);
        assertThat(card.tier()).isEqualTo(MembershipTier.SILVER);
        Long first = place("100000"); Long second = place("100000");
        assertThat(orders.findById(first).orElseThrow().getFinalAmount()).isEqualByComparingTo("87000");
        assertThat(orders.findById(second).orElseThrow().getFinalAmount()).isEqualByComparingTo("97000");
        shipping(first); complete(first);
        assertThat(customer(loyalty::mine).currentPoints()).isEqualTo(408);
        assertThat(customer(loyalty::mine).history()).anySatisfy(t -> { assertThat(t.type()).isEqualTo("Đổi điểm"); assertThat(t.points()).isEqualTo(-100); });
    }
    @Test void concurrentRedeemCannotOverspend() throws Exception {
        adjust(100);
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> redeem = () -> { gate.await(); try { customer(() -> { loyalty.redeem(); return null; }); return true; }
                catch (LoyaltyException ex) { return false; } };
            var first = executor.submit(redeem); var second = executor.submit(redeem); gate.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(customer(loyalty::mine).currentPoints()).isZero();
        assertThat(customer(loyalty::mine).history().stream().filter(t -> t.type().equals("Đổi điểm"))).hasSize(1);
    }
    @Test void failedCheckoutKeepsVoucherAndSmallOrderCannotBecomeNegative() {
        adjust(100);
        customer(() -> { loyalty.redeem(); return null; });
        var product = product(ProductType.ACCESSORY, "5000");
        customer(() -> { carts.addItem(customerId, product, 1); return null; });
        assertThatThrownBy(() -> customer(() -> db(() -> {
            orderService.placeOrderFromCart(customerId, "Địa chỉ thử");
            throw new IllegalStateException("Buộc rollback sau khi dùng voucher");
        }))).isInstanceOf(IllegalStateException.class);
        assertThat(customer(loyalty::mine).voucherAvailable()).isTrue();
        Long id = customer(() -> orderService.placeOrderFromCart(customerId, "Địa chỉ thử").getId());
        assertThat(orders.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("0");
        assertThat(customer(loyalty::mine).voucherAvailable()).isFalse();
        shipping(id); complete(id);
        assertThat(customer(loyalty::mine).totalPoints()).isEqualTo(100);
        assertThat(transactions.existsBySourceKey("EARN:ORDER:" + id)).isTrue();
    }
    @Test void cartAndBookingPreviewShowCurrentTierDiscount() throws Exception {
        adjust(500);
        var product = product(ProductType.ACCESSORY, "100000");
        customer(() -> { carts.addItem(customerId, product, 1); return null; });
        for (String path : List.of("/cart", "/checkout")) {
            mvc.perform(get(path).with(user(email).roles("CUSTOMER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hạng Bạc — giảm: 3000.00 VNĐ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thanh toán dự kiến: 97000.00 VNĐ")));
        }
        var serviceId = product(ProductType.GROOMING, "100000");
        var staffId = users.findByEmail("staff@petcare.vn").orElseThrow().getId();
        mvc.perform(get("/appointments/confirm").with(user(email).roles("CUSTOMER")).param("serviceId", serviceId.toString())
            .param("staffId", staffId.toString()).param("startTime", "2026-01-06T09:00:00"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Hạng Bạc — giảm: 3000.00 VNĐ")));
    }
    @Test void concurrentReplayCreditsOnlyOnce() throws Exception {
        Long id = place("100000"); shipping(id);
        // Dữ liệu hoàn tất chưa có sự kiện để kiểm tra hai lần phát đồng thời tranh cùng nguồn.
        db(() -> { orders.findById(id).orElseThrow().complete(); return null; });
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> replay = () -> { gate.await(); db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return null; }); return null; };
            var first = executor.submit(replay); var second = executor.submit(replay); gate.countDown();
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
        }
        assertThat(customer(loyalty::mine).currentPoints()).isEqualTo(10);
        assertThat(countEarn(accounts.findByCustomerId(customerId).orElseThrow().getId())).isEqualTo(1);
    }
    @Test void appointmentSnapshotsDiscountAndDonePublishesIdempotentEvent() {
        adjust(2000);
        var form = new AppointmentForm(); form.setServiceId(product(ProductType.GROOMING, "200000"));
        // Nhân viên riêng tránh trùng khung giờ giữa các ca kiểm thử.
        var employee = users.saveAndFlush(new User("Nhân viên thử", UUID.randomUUID() + "@petcare.vn", "hash", "", Role.STAFF));
        form.setStaffId(employee.getId()); form.setStartTime(LocalDateTime.of(2026, 1, 6, 9, 0));
        form.setPetName("Bông"); form.setPetSpecies(Species.CAT);
        Long id = customer(() -> appointmentService.book(form));
        assertThat(appointments.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("190000");
        adjust(3000);
        as(employee.getEmail(), () -> { appointmentService.start(id); appointmentService.finish(id); return null; });
        db(() -> { events.publishEvent(new AppointmentCompletedEvent(id)); events.publishEvent(new AppointmentCompletedEvent(id)); return null; });
        assertThat(customer(loyalty::mine).totalPoints()).isEqualTo(5019);
        assertThat(appointments.findById(id).orElseThrow().getStateName()).isEqualTo(AppointmentStatus.DONE);
        assertThat(appointments.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("190000");
        assertThat(countEarn(accounts.findByCustomerId(customerId).orElseThrow().getId())).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void navigationAndAuthorizationMatchEveryRole(String role) throws Exception {
        var request = get("/");
        if (!role.equals("GUEST")) request.with(user(role.equals("CUSTOMER") ? email : role.toLowerCase() + "@petcare.vn").roles(role));
        var html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String section : List.of("header", "main")) {
            String fragment = html.substring(html.indexOf("<" + section), html.indexOf("</" + section + ">"));
            assertThat(fragment.contains("href=\"/membership\"")).isEqualTo(role.equals("CUSTOMER"));
            assertThat(fragment.contains("href=\"/admin/members\"")).isEqualTo(role.equals("ADMIN"));
        }
        for (String path : List.of("/membership", "/admin/members", "/admin/members/" + customerId)) {
            var page = get(path);
            if (!role.equals("GUEST")) page.with(user(role.equals("CUSTOMER") ? email : role.toLowerCase() + "@petcare.vn").roles(role));
            boolean allowed = path.equals("/membership") ? role.equals("CUSTOMER") : role.equals("ADMIN");
            mvc.perform(page).andExpect(role.equals("GUEST") ? status().is3xxRedirection() : allowed ? status().isOk() : status().isForbidden());
        }
    }
    @Test void webAdjustRedeemFilterHistoryAndCsrf() throws Exception {
        mvc.perform(post("/admin/members/{id}/adjust", customerId).with(user("admin@petcare.vn").roles("ADMIN")).with(csrf())
            .param("points", "500").param("reason", "Tặng điểm tri ân có dấu")).andExpect(redirectedUrl("/admin/members/" + customerId));
        mvc.perform(get("/admin/members?tier=SILVER").with(user("admin@petcare.vn").roles("ADMIN"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/members/" + customerId + "\"")));
        mvc.perform(get("/membership").with(user(email).roles("CUSTOMER"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tặng điểm tri ân có dấu")));
        mvc.perform(post("/membership/redeem").with(user(email).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/membership/redeem").with(user(email).roles("CUSTOMER")).with(csrf())).andExpect(redirectedUrl("/membership"));
        mvc.perform(post("/admin/members/{id}/adjust", customerId).with(user(email).roles("CUSTOMER")).with(csrf())
            .param("points", "1000").param("reason", "Không có quyền")).andExpect(status().isForbidden());
        assertThat(customer(loyalty::mine).currentPoints()).isEqualTo(400);
        assertThatThrownBy(() -> as("admin@petcare.vn", () -> { loyalty.adjust(customerId, -401, "Thử trừ quá số dư"); return null; })).isInstanceOf(LoyaltyException.class);
        assertThatThrownBy(() -> as("admin@petcare.vn", () -> { loyalty.adjust(customerId, 10, " "); return null; })).isInstanceOf(LoyaltyException.class);
        assertThat(customer(loyalty::mine).currentPoints()).isEqualTo(400);
    }
}
