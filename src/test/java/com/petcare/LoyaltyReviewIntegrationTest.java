package com.petcare;

import com.petcare.config.LoyaltyProperties;
import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.loyalty.*;
import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.dto.AppointmentForm;
import com.petcare.dto.LoyaltyView;
import com.petcare.event.AppointmentCompletedEvent;
import com.petcare.repository.*;
import com.petcare.service.*;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Hồi quy bốn mục review GĐ7: cấu hình, truy vấn đọc, phân quyền và cạnh tranh sự kiện. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:loyalty_review;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test") @AutoConfigureMockMvc @Import(LoyaltyReviewIntegrationTest.TestConfig.class)
class LoyaltyReviewIntegrationTest {
    @TestConfiguration static class TestConfig {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
        @Bean SqlCapture sqlCapture() { return new SqlCapture(); }
        @Bean HibernatePropertiesCustomizer captureSql(SqlCapture capture) {
            return values -> values.put("hibernate.session_factory.statement_inspector", capture);
        }
    }
    static class SqlCapture implements StatementInspector {
        private final ThreadLocal<List<String>> statements = new ThreadLocal<>();
        public String inspect(String sql) {
            if (statements.get() != null) statements.get().add(sql.toLowerCase(Locale.ROOT));
            return sql;
        }
        void start() { statements.set(new ArrayList<>()); }
        List<String> stop() { var result = List.copyOf(statements.get()); statements.remove(); return result; }
    }
    @Autowired LoyaltyProperties properties;
    @Autowired LoyaltyService loyalty;
    @Autowired LoyaltyPricingService pricing;
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
    @Autowired SqlCapture sql;
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
    <T> T admin(Supplier<T> work) { return as("admin@petcare.vn", work); }
    <T> T staff(Supplier<T> work) { return as("staff@petcare.vn", work); }
    User createUser(Role role) { return users.saveAndFlush(new User("Người dùng kiểm thử", UUID.randomUUID() + "@petcare.vn", "test-hash", "", role)); }
    @BeforeEach void setup() { var customer = createUser(Role.CUSTOMER); customerId = customer.getId(); email = customer.getEmail(); }
    void adjust(long points) { admin(() -> { loyalty.adjust(customerId, points, "Điểm kiểm thử review"); return null; }); }
    Page<LoyaltyView> members(MembershipTier tier, int page) { return admin(() -> loyalty.members(tier, page)); }
    Long product(ProductType type, String price) {
        var form = ProductFactoryTest.form(type, "Sản phẩm kiểm thử review", price);
        form.setStockQuantity(100); form.setDurationMinutes(30); form.setPetSizeSurcharge(BigDecimal.ZERO);
        return products.saveAndFlush(new ProductFactory().create(form)).getId();
    }
    Long place(String price) {
        Long product = product(ProductType.ACCESSORY, price);
        return customer(() -> { carts.addItem(customerId, product, 1); return orderService.placeOrderFromCart(customerId, "Địa chỉ thử").getId(); });
    }
    void shipping(Long id) { staff(() -> { orderService.confirm(id); orderService.ship(id); return null; }); }
    record Booking(Long id, String employee) {}
    Booking book(String price) {
        var employee = createUser(Role.STAFF);
        var form = new AppointmentForm(); form.setServiceId(product(ProductType.GROOMING, price));
        form.setStaffId(employee.getId()); form.setStartTime(LocalDateTime.of(2026, 1, 6, 9, 0));
        form.setPetName("Bông"); form.setPetSpecies(Species.CAT);
        return new Booking(customer(() -> appointmentService.book(form)), employee.getEmail());
    }
    void start(Booking booking) { as(booking.employee(), () -> { appointmentService.start(booking.id()); return null; }); }
    void finish(Booking booking) { as(booking.employee(), () -> { appointmentService.finish(booking.id()); return null; }); }
    List<PointTransaction> ledger() {
        return transactions.findByAccountIdOrderByCreatedAtDescIdDesc(accounts.findByCustomerId(customerId).orElseThrow().getId());
    }
    static void await(CountDownLatch latch) {
        try { assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
    }
    static <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { await(gate); return first.call(); });
            var b = executor.submit(() -> { await(gate); return second.call(); });
            gate.countDown();
            return Arrays.asList(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }

    @Test void changedThresholdImmediatelyAffectsQuotesCardsAndFilteringButNotExistingPrices() {
        adjust(600);
        Long oldOrder = place("100000"); var oldBooking = book("100000");
        long originalThreshold = properties.tier(MembershipTier.SILVER).getThreshold();
        try {
            properties.tier(MembershipTier.SILVER).setThreshold(1000); properties.validate();
            assertThat(accounts.findByCustomerId(customerId).orElseThrow().getTier()).isEqualTo(MembershipTier.SILVER);
            for (boolean order : List.of(true, false)) {
                var quote = customer(() -> pricing.preview(new BigDecimal("100000"), order));
                assertThat(quote.tierName()).isEqualTo("Đồng"); assertThat(quote.finalAmount()).isEqualByComparingTo("100000");
            }
            assertThat(customer(loyalty::mine).tier()).isEqualTo(MembershipTier.BRONZE);
            assertThat(admin(() -> loyalty.member(customerId)).tier()).isEqualTo(MembershipTier.BRONZE);
            assertThat(members(MembershipTier.BRONZE, 0).getContent()).extracting(LoyaltyView::customerId).contains(customerId);
            assertThat(members(MembershipTier.SILVER, 0)).allSatisfy(m -> {
                assertThat(m.customerId()).isNotEqualTo(customerId); assertThat(m.totalPoints()).isBetween(1000L, 1999L);
            });
            Long newOrder = place("100000"); var newBooking = book("100000");
            assertThat(accounts.findByCustomerId(customerId).orElseThrow().getTier()).isEqualTo(MembershipTier.BRONZE);
            properties.tier(MembershipTier.SILVER).setThreshold(600); properties.validate();
            assertThat(customer(loyalty::mine).tier()).isEqualTo(MembershipTier.SILVER);
            assertThat(admin(() -> loyalty.member(customerId)).tier()).isEqualTo(MembershipTier.SILVER);
            assertThat(customer(() -> pricing.preview(new BigDecimal("100000"), true)).finalAmount()).isEqualByComparingTo("97000");
            assertThat(members(MembershipTier.SILVER, 0).getContent()).extracting(LoyaltyView::customerId).contains(customerId);
            assertThat(members(MembershipTier.BRONZE, 0)).noneMatch(m -> m.customerId().equals(customerId));
            customer(() -> { loyalty.redeem(); return null; });
            var card = customer(loyalty::mine);
            assertThat(card.currentPoints()).isEqualTo(500); assertThat(card.totalPoints()).isEqualTo(600);
            assertThat(card.tier()).isEqualTo(MembershipTier.SILVER);
            assertThat(orders.findById(oldOrder).orElseThrow().getFinalAmount()).isEqualByComparingTo("97000");
            assertThat(appointments.findById(oldBooking.id()).orElseThrow().getFinalAmount()).isEqualByComparingTo("97000");
            assertThat(orders.findById(newOrder).orElseThrow().getFinalAmount()).isEqualByComparingTo("100000");
            assertThat(appointments.findById(newBooking.id()).orElseThrow().getFinalAmount()).isEqualByComparingTo("100000");
        } finally { properties.tier(MembershipTier.SILVER).setThreshold(originalThreshold); }
    }

    private void seedMembers(int count) {
        db(() -> {
            for (int i = 0; i < count; i++) {
                var account = new LoyaltyAccount(createUser(Role.CUSTOMER)); account.credit(500, properties); accounts.save(account);
            }
            return null;
        });
    }
    @Test void listingUsesBoundedReadQueriesAndDoesNotCreateAccounts() throws Exception {
        seedMembers(45);
        var unopened = createUser(Role.CUSTOMER);
        long initialCount = accounts.count();
        sql.start();
        var before = members(MembershipTier.SILVER, 1);
        var beforeSql = sql.stop();
        assertThat(before.getContent()).hasSize(20).allSatisfy(m -> assertThat(m.tier()).isEqualTo(MembershipTier.SILVER));
        assertThat(accounts.count()).isEqualTo(initialCount);
        assertThat(accounts.findByCustomerId(unopened.getId())).isEmpty();
        seedMembers(60);
        sql.start();
        var after = members(MembershipTier.SILVER, 1);
        var afterSql = sql.stop();
        assertThat(after.getTotalElements()).isEqualTo(before.getTotalElements() + 60);
        assertThat(afterSql).hasSameSizeAs(beforeSql).hasSize(3);
        assertThat(afterSql).allSatisfy(query -> {
            assertThat(query.stripLeading()).startsWith("select");
            assertThat(query).doesNotContain("for update", "updlock", "holdlock");
        });
        assertThat(afterSql).anySatisfy(query -> assertThat(query).contains("total_points>=", "total_points<", "offset", "fetch first"));
        assertThat(afterSql).anySatisfy(query -> assertThat(query).contains("count(", "total_points>="));
        var first = members(MembershipTier.SILVER, 0);
        assertThat(first.getContent()).extracting(LoyaltyView::customerId)
            .doesNotContainAnyElementsOf(after.stream().map(LoyaltyView::customerId).toList());
        mvc.perform(get("/admin/members").param("tier", "SILVER").with(user("admin@petcare.vn").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/members?tier=SILVER&amp;page=1")));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void openOrFilteredListDoesNotBlockCheckoutCompletionOrRedemption(boolean filtered) throws Exception {
        adjust(500);
        var product = product(ProductType.ACCESSORY, "100000");
        customer(() -> { carts.addItem(customerId, product, 1); return null; });
        var listed = new CountDownLatch(1); var releaseReader = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var reader = executor.submit(() -> admin(() -> {
                var transaction = new TransactionTemplate(manager); transaction.setReadOnly(true);
                return transaction.execute(s -> {
                    var result = loyalty.members(filtered ? MembershipTier.SILVER : null, 0);
                    assertThat(result.getContent()).extracting(LoyaltyView::customerId).contains(customerId);
                    listed.countDown(); await(releaseReader); return result.getTotalElements();
                });
            }));
            try {
                await(listed);
                var writer = executor.submit(() -> {
                    Long id = customer(() -> orderService.placeOrderFromCart(customerId, "Địa chỉ thử").getId());
                    shipping(id); staff(() -> orderService.complete(id));
                    customer(() -> { loyalty.redeem(); return null; });
                    return customer(loyalty::mine);
                });
                // Giao dịch đọc vẫn mở: mọi thao tác ghi phải hoàn tất trước khi thả reader.
                var card = writer.get(5, TimeUnit.SECONDS);
                assertThat(card.currentPoints()).isEqualTo(409); assertThat(card.totalPoints()).isEqualTo(509);
                assertThat(card.voucherAvailable()).isTrue(); assertThat(reader.isDone()).isFalse();
            } finally { releaseReader.countDown(); }
            reader.get(5, TimeUnit.SECONDS);
        }
    }

    @Test void invalidMemberIdsReturn404AndCustomerCannotReadAnotherCard() throws Exception {
        var staffId = users.findByEmail("staff@petcare.vn").orElseThrow().getId();
        var adminId = users.findByEmail("admin@petcare.vn").orElseThrow().getId();
        for (Long id : List.of(Long.MAX_VALUE, staffId, adminId)) {
            mvc.perform(get("/admin/members/{id}", id).with(user("admin@petcare.vn").roles("ADMIN"))).andExpect(status().isNotFound());
            mvc.perform(post("/admin/members/{id}/adjust", id).with(user("admin@petcare.vn").roles("ADMIN")).with(csrf())
                .param("points", "10").param("reason", "Thử mã không hợp lệ")).andExpect(status().isNotFound());
        }
        var other = createUser(Role.CUSTOMER);
        mvc.perform(get("/admin/members/{id}", other.getId()).with(user(email).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/membership/{id}", other.getId()).with(user(email).roles("CUSTOMER"))).andExpect(status().isNotFound());
        var response = mvc.perform(get("/membership").param("customerId", other.getId().toString()).with(user(email).roles("CUSTOMER")))
            .andExpect(status().isOk()).andReturn();
        var ownCard = (LoyaltyView) response.getModelAndView().getModel().get("member");
        assertThat(ownCard.email()).isEqualTo(email);
        assertThat(accounts.findByCustomerId(other.getId())).isEmpty();
        assertThat(accounts.findByCustomerId(customerId)).isPresent();
    }

    @Test void concurrentAppointmentCompletionCreditsOnce() throws Exception {
        var booking = book("100000"); start(booking);
        Callable<Boolean> complete = () -> {
            try { finish(booking); return true; }
            // Worker đã đọc phiên bản cũ có thể bị @Version từ chối trước khi kiểm tra State.
            catch (InvalidStateTransitionException | jakarta.persistence.OptimisticLockException ex) { return false; }
        };
        assertThat(concurrently(complete, complete)).containsExactlyInAnyOrder(true, false);
        var card = customer(loyalty::mine);
        assertThat(card.currentPoints()).isEqualTo(10); assertThat(card.totalPoints()).isEqualTo(10);
        assertThat(ledger()).singleElement().satisfies(t -> assertThat(t.getSourceKey()).isEqualTo("EARN:APPOINTMENT:" + booking.id()));
    }
    @Test void concurrentAppointmentReplayCreditsOnce() throws Exception {
        var booking = book("100000"); start(booking);
        // Cố ý hoàn tất domain chưa phát event để hai worker cùng tranh lần ghi điểm đầu tiên.
        db(() -> { appointments.findById(booking.id()).orElseThrow().finish(); return null; });
        Callable<Void> replay = () -> db(() -> { events.publishEvent(new AppointmentCompletedEvent(booking.id())); return null; });
        concurrently(replay, replay);
        var card = customer(loyalty::mine);
        assertThat(card.currentPoints()).isEqualTo(10); assertThat(card.totalPoints()).isEqualTo(10);
        assertThat(ledger()).singleElement().satisfies(t -> assertThat(t.getSourceKey()).isEqualTo("EARN:APPOINTMENT:" + booking.id()));
    }
    @Test void orderAndAppointmentCompleteConcurrentlyWithoutLostPoints() throws Exception {
        Long order = place("100000"); shipping(order);
        var booking = book("200000"); start(booking);
        concurrently(() -> { staff(() -> orderService.complete(order)); return null; }, () -> { finish(booking); return null; });
        var card = customer(loyalty::mine);
        assertThat(card.currentPoints()).isEqualTo(30); assertThat(card.totalPoints()).isEqualTo(30);
        assertThat(ledger()).hasSize(2).allSatisfy(t -> assertThat(t.getType()).isEqualTo(PointTransactionType.EARN));
        assertThat(ledger()).extracting(PointTransaction::getSourceKey)
            .containsExactlyInAnyOrder("EARN:ORDER:" + order, "EARN:APPOINTMENT:" + booking.id());
        assertThat(ledger()).extracting(PointTransaction::getPoints).containsExactlyInAnyOrder(10L, 20L);
    }
}
