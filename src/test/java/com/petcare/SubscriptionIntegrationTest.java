package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.order.*;
import com.petcare.domain.subscription.*;
import com.petcare.dto.AppointmentForm;
import com.petcare.event.OrderCompletedEvent;
import com.petcare.repository.*;
import com.petcare.scheduler.SubscriptionScheduler;
import com.petcare.service.*;
import jakarta.persistence.EntityManager;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:subscription_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test") @AutoConfigureMockMvc @Import(SubscriptionIntegrationTest.FixedTime.class)
class SubscriptionIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 1, 5);
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
    @Autowired SubscriptionService service;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired SubscriptionGrantRepository grants;
    @Autowired CartService carts;
    @Autowired OrderService orderService;
    @Autowired AppointmentService appointmentService;
    @Autowired AppointmentRepository appointments;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired LoyaltyService loyalty;
    @Autowired PointTransactionRepository points;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    Long customerId, staffId, serviceId, packId;
    String email, staffEmail;
    <T> T db(Supplier<T> work) { return new TransactionTemplate(manager).execute(s -> work.get()); }
    <T> T as(String email, Supplier<T> work) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(email, "", List.of()));
        SecurityContextHolder.setContext(context);
        try { return work.get(); } finally { SecurityContextHolder.setContext(previous); }
    }
    @BeforeEach void setup() {
        email = "sub-" + UUID.randomUUID() + "@test.vn"; staffEmail = "staff-" + UUID.randomUUID() + "@test.vn";
        customerId = users.saveAndFlush(new User("Khách dùng gói", email, "hash", "", Role.CUSTOMER)).getId();
        staffId = users.saveAndFlush(new User("Nhân viên gói", staffEmail, "hash", "", Role.STAFF)).getId();
        var form = ProductFactoryTest.form(ProductType.GROOMING, "Tắm và chăm sóc có dấu", "100000"); form.setPetSizeSurcharge(BigDecimal.ZERO);
        serviceId = products.saveAndFlush(new ProductFactory().create(form)).getId();
        var pack = (MonthlyCarePackage) new ProductFactory().create(ProductFactoryTest.form(ProductType.MONTHLY, "Gói tháng chăm sóc", "200000"));
        pack.configureCoveredServices(List.of((ServiceItem) products.findById(serviceId).orElseThrow()));
        packId = products.saveAndFlush(pack).getId();
    }
    Long purchase() {
        return as(email, () -> { carts.addItem(customerId, packId, 1); return orderService.placeOrderFromCart(customerId, "Địa chỉ mô phỏng").getId(); });
    }
    void complete(Long orderId) { as(staffEmail, () -> { orderService.confirm(orderId); orderService.ship(orderId); return orderService.complete(orderId); }); }
    Long seed(LocalDate start, int uses) {
        return db(() -> {
            var pack = (MonthlyCarePackage) products.findById(packId).orElseThrow();
            var form = pack.toForm(); form.setIncludedUses(uses); pack.update(form);
            return subscriptions.saveAndFlush(Subscription.create(users.findById(customerId).orElseThrow(), null, pack, start, 1, "TEST:" + UUID.randomUUID())).getId();
        });
    }
    AppointmentForm form(int hour) {
        var f = new AppointmentForm(); f.setServiceId(serviceId); f.setStaffId(staffId);
        f.setStartTime(TODAY.atTime(hour, 0)); f.setPetName("Bông"); f.setPetSpecies(Species.CAT); return f;
    }
    Long book(int hour) { return as(email, () -> appointmentService.book(form(hour))); }
    Subscription read(Long id) { return subscriptions.findById(id).orElseThrow(); }
    <T> List<T> race(Supplier<T> work) throws Exception {
        var latch = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<T> task = () -> { latch.await(); return work.get(); };
            var a = executor.submit(task); var b = executor.submit(task); latch.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }
    @Test void purchaseDiscountCompletionPointsReplayAndSubscriptionAreAtomic() throws Exception {
        as("admin@petcare.vn", () -> { loyalty.adjust(customerId, 500, "Hạng Bạc kiểm thử"); return null; });
        var orderId = purchase();
        assertThat(orders.findById(orderId).orElseThrow().getFinalAmount()).isEqualByComparingTo("194000");
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).isEmpty();
        complete(orderId);
        var sub = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId).getFirst();
        assertThat(sub.getRemainingUses()).isEqualTo(4);
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(519);
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(orderId)); return true; }));
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(1);
        assertThat(grants.findById(orderId)).isPresent();
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(519);
        var appointmentId = book(12);
        assertThat(appointments.findById(appointmentId).orElseThrow().getPrice()).isEqualByComparingTo("0");
        assertThat(read(sub.getId()).getRemainingUses()).isEqualTo(3);
        as(staffEmail, () -> { appointmentService.start(appointmentId); appointmentService.finish(appointmentId); return null; });
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(519);
    }
    @Test void firstConcurrentDeliveryCreatesExactlyOneSubscription() throws Exception {
        var orderId = purchase();
        db(() -> { var o = orders.findById(orderId).orElseThrow(); o.confirm(); o.ship(); o.complete(); return null; });
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(orderId)); return true; }));
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(1);
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(20);
    }
    @Test void rollbackCompletionRollsBackGrantAndPoints() {
        var id = purchase();
        as(staffEmail, () -> { orderService.confirm(id); orderService.ship(id); return null; });
        assertThatThrownBy(() -> as(staffEmail, () -> db(() -> { orderService.complete(id); throw new IllegalStateException("rollback"); })))
            .isInstanceOf(IllegalStateException.class);
        assertThat(grants.existsById(id)).isFalse(); assertThat(points.existsBySourceKey("EARN:ORDER:" + id)).isFalse();
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).isEmpty();
        assertThat(orders.findById(id).orElseThrow().getStateName()).isEqualTo(OrderStatus.SHIPPING);
    }
    @Test void concurrentLastUseNeverNegativeAndDoubleCancellationRefundsOnce() throws Exception {
        var id = seed(TODAY, 1);
        var counter = new java.util.concurrent.atomic.AtomicInteger(11);
        var ids = race(() -> book(counter.incrementAndGet()));
        assertThat(ids.stream().map(a -> appointments.findById(a).orElseThrow().getPrice())).containsExactlyInAnyOrder(new BigDecimal("0.00"), new BigDecimal("100000.00"));
        assertThat(read(id).getRemainingUses()).isZero(); assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
        var freeId = ids.stream().filter(a -> appointments.findById(a).orElseThrow().getSubscription() != null).findFirst().orElseThrow();
        var results = race(() -> as(email, () -> {
            try { appointmentService.cancelMine(freeId); return true; } catch (InvalidStateTransitionException ex) { return false; }
        }));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(read(id).getRemainingUses()).isOne(); assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(appointments.findById(freeId).orElseThrow().isSubscriptionUseRefunded()).isTrue();
        var paidId = ids.stream().filter(a -> !a.equals(freeId)).findFirst().orElseThrow();
        as(staffEmail, () -> { appointmentService.cancelByStaff(paidId); return null; });
        assertThat(read(id).getRemainingUses()).isOne();
    }
    @Test void bookingAndCancellationRollbackTogetherWithUses() {
        var id = seed(TODAY, 1);
        assertThatThrownBy(() -> as(email, () -> db(() -> { appointmentService.book(form(12)); throw new IllegalStateException(); }))).isInstanceOf(IllegalStateException.class);
        assertThat(read(id).getRemainingUses()).isOne();
        var appointmentId = book(12);
        assertThatThrownBy(() -> as(email, () -> db(() -> { appointmentService.cancelMine(appointmentId); throw new IllegalStateException(); }))).isInstanceOf(IllegalStateException.class);
        assertThat(read(id).getRemainingUses()).isZero();
        assertThat(appointments.findById(appointmentId).orElseThrow().isSubscriptionUseRefunded()).isFalse();
        as(staffEmail, () -> { appointmentService.cancelByStaff(appointmentId); return null; });
        assertThat(read(id).getRemainingUses()).isOne();
    }
    @Test void selectionUsesEarliestExpiryThenIdAndOnlyCoveredServicesWithinDates() {
        var later = seed(TODAY, 2); var first = seed(TODAY.minusDays(5), 2); var tie = seed(TODAY.minusDays(5), 2);
        book(12); assertThat(read(first).getRemainingUses()).isOne(); assertThat(read(tie).getRemainingUses()).isEqualTo(2);
        assertThat(read(later).getRemainingUses()).isEqualTo(2);
        var other = products.saveAndFlush(new ProductFactory().create(ProductFactoryTest.form(ProductType.VET, "Khám ngoài gói", "90000"))).getId();
        var f = form(13); f.setServiceId(other);
        var paid = as(email, () -> appointmentService.book(f));
        assertThat(appointments.findById(paid).orElseThrow().getSubscription()).isNull();
        var future = form(14); future.setStartTime(TODAY.plusMonths(2).atTime(14, 0));
        var outOfRange = as(email, () -> appointmentService.book(future));
        assertThat(appointments.findById(outOfRange).orElseThrow().getSubscription()).isNull();
    }
    @Test void renewalCreatesDiscountedOrderAndAppliesExactlyOnceOnCompletion() {
        var id = seed(TODAY.minusMonths(2), 1);
        service.expireIfDue(id, TODAY);
        as("admin@petcare.vn", () -> { loyalty.adjust(customerId, 500, "Hạng Bạc"); return null; });
        var renewal = as(email, () -> service.renew(id));
        assertThat(orders.findById(renewal).orElseThrow().getFinalAmount()).isEqualByComparingTo("194000");
        assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThatThrownBy(() -> as(email, () -> service.renew(id))).isInstanceOf(InvalidStateTransitionException.class);
        complete(renewal);
        db(() -> { events.publishEvent(new OrderCompletedEvent(renewal)); return null; });
        assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(read(id).getRemainingUses()).isOne(); assertThat(read(id).getStartDate()).isEqualTo(TODAY);
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(1);
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(519);
    }
    @Test void schedulerWithFixedClockIncludesBoundaryAndZeroUses() {
        var boundary = seed(TODAY.minusMonths(1).plusDays(1), 2);
        var elapsed = seed(TODAY.minusMonths(1), 2);
        var empty = seed(TODAY, 1);
        db(() -> { org.springframework.test.util.ReflectionTestUtils.setField(subscriptions.findById(empty).orElseThrow(), "remainingUses", 0); return null; });
        var scheduler = new SubscriptionScheduler(subscriptions, service, Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh")));
        scheduler.expireSubscriptions(); scheduler.expireSubscriptions();
        assertThat(read(boundary).getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(read(elapsed).getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(read(empty).getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
        new SubscriptionScheduler(subscriptions, service, Clock.fixed(TODAY.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh"))).expireSubscriptions();
        assertThat(read(boundary).getStateName()).isEqualTo(SubscriptionStatus.EXPIRED);
    }
    @Test void cancellingRenewalOrderReleasesReservationAndRefundCannotStrandAcceptedRenewal() {
        var id = seed(TODAY, 1); var appointmentId = book(12);
        var first = as(email, () -> service.renew(id));
        assertThat(read(id).isRenewalPending()).isTrue();
        as(email, () -> orderService.cancel(first));
        assertThat(read(id).isRenewalPending()).isFalse();
        var second = as(email, () -> service.renew(id));
        as(email, () -> { appointmentService.cancelMine(appointmentId); return null; });
        assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
        complete(second);
        assertThat(read(id).getRemainingUses()).isEqualTo(2);
        assertThat(read(id).isRenewalPending()).isFalse();
    }
    @Test void serviceMembershipUsesIdsAndCatalogChangesDoNotRewritePurchasedRights() throws Exception {
        var orderId = purchase(); complete(orderId);
        var sub = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId).getFirst();
        var other = products.saveAndFlush(new ProductFactory().create(ProductFactoryTest.form(ProductType.VET, "Khám khác", "100000")));
        db(() -> {
            var pack = (MonthlyCarePackage) products.findById(packId).orElseThrow(); pack.configureCoveredServices(List.of((ServiceItem) other));
            var product = products.findById(serviceId).orElseThrow(); var f = product.toForm(); f.setName("Tên dịch vụ đã đổi"); product.updateCatalog(f);
            return null;
        });
        book(12); assertThat(read(sub.getId()).getRemainingUses()).isEqualTo(3);
        mvc.perform(get("/staff/products/{id}/edit", packId).with(user(staffEmail).roles("STAFF")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("coveredServiceIds")));
    }
    @Test void multipleUnitsReceiveSeparateSubscriptionsAndDistinctPackagesAreRejected() {
        var id = as(email, () -> { carts.addItem(customerId, packId, 2); return orderService.placeOrderFromCart(customerId, "Địa chỉ").getId(); });
        complete(id);
        var granted = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId);
        assertThat(granted).hasSize(2).allSatisfy(s -> assertThat(s.getRemainingUses()).isEqualTo(4));
        assertThat(granted).extracting(Subscription::getSourceKey).containsExactlyInAnyOrderElementsOf(expectedKeys(id));
        db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return null; });
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(2);
        var otherPack = new ProductFactory().create(ProductFactoryTest.form(ProductType.MONTHLY, "Gói khác", "100000"));
        otherPack.configureCoveredServices(List.of((ServiceItem) products.findById(serviceId).orElseThrow()));
        var other = products.saveAndFlush(otherPack).getId();
        as(email, () -> { carts.addItem(customerId, packId, 1); carts.addItem(customerId, other, 1); return null; });
        assertThatThrownBy(() -> as(email, () -> orderService.placeOrderFromCart(customerId, "Địa chỉ")))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("một loại");
    }
    /** Mô phỏng các hàng đã có trước GĐ8 bằng SQL: không đi vòng quy tắc của API tạo đơn mới. */
    Long legacyOrder(int firstQuantity, int secondQuantity) {
        var otherForm = ProductFactoryTest.form(ProductType.MONTHLY, "Gói cũ thứ hai", "300000");
        otherForm.setIncludedUses(6); otherForm.setDurationMonths(2);
        var other = (MonthlyCarePackage) new ProductFactory().create(otherForm);
        other.configureCoveredServices(List.of((ServiceItem) products.findById(serviceId).orElseThrow()));
        var otherId = products.saveAndFlush(other).getId();
        var id = as(email, () -> { carts.addItem(customerId, packId, firstQuantity); return orderService.placeOrderFromCart(customerId, "Đơn trước GĐ8").getId(); });
        db(() -> {
            entityManager.createNativeQuery("delete from order_item_snapshot_services where order_item_id in (select id from order_items where order_id = :id)")
                .setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("update order_items set snapshot_package_id = null, snapshot_included_uses = null, snapshot_duration_months = null where order_id = :id")
                .setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("insert into order_items (order_id, product_id, quantity, unit_price) values (:id, :product, :quantity, 300000)")
                .setParameter("id", id).setParameter("product", otherId).setParameter("quantity", secondQuantity).executeUpdate();
            var total = new BigDecimal("200000").multiply(BigDecimal.valueOf(firstQuantity))
                .add(new BigDecimal("300000").multiply(BigDecimal.valueOf(secondQuantity)));
            entityManager.createNativeQuery("update orders set total_amount = :total, final_amount = :total where id = :id")
                .setParameter("total", total).setParameter("id", id).executeUpdate();
            return null;
        });
        return id;
    }
    List<String> expectedKeys(Long id) {
        return db(() -> orders.findById(id).orElseThrow().getItems().stream().flatMap(item ->
            java.util.stream.IntStream.rangeClosed(1, item.getQuantity()).mapToObj(unit ->
                "ORDER:" + id + ":ITEM:" + item.getId() + (item.getQuantity() > 1 ? ":UNIT:" + unit : ""))).toList());
    }
    @Test void legacyOrderWithTwoPackagesCompletesAndReplayKeepsBothRights() throws Exception {
        var id = legacyOrder(1, 1);
        complete(id);
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return true; }));
        var granted = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId);
        assertThat(granted).hasSize(2).extracting(Subscription::getRemainingUses).containsExactlyInAnyOrder(4, 6);
        assertThat(granted).extracting(Subscription::getEndDate).containsExactlyInAnyOrder(TODAY.plusMonths(1).minusDays(1), TODAY.plusMonths(2).minusDays(1));
        assertThat(granted).extracting(Subscription::getSourceKey).containsExactlyInAnyOrderElementsOf(expectedKeys(id));
        assertThat(orders.findById(id).orElseThrow().getStateName()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(orders.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("500000");
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(50);
    }
    @Test void concurrentCompletionOfLegacyMultiQuantityOrderGrantsEveryUnitOnce() throws Exception {
        var id = legacyOrder(2, 1);
        as(staffEmail, () -> { orderService.confirm(id); orderService.ship(id); return null; });
        var results = race(() -> as(staffEmail, () -> {
            try { orderService.complete(id); return true; }
            catch (InvalidStateTransitionException ex) { return false; }
        }));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return true; }));
        var granted = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId);
        assertThat(granted).hasSize(3).extracting(Subscription::getRemainingUses).containsExactlyInAnyOrder(4, 4, 6);
        assertThat(granted).extracting(Subscription::getSourceKey).containsExactlyInAnyOrderElementsOf(expectedKeys(id));
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(70);
    }
    @Test void concurrentFirstEventsForLegacyOrderAndRollbackAreAtomic() throws Exception {
        var id = legacyOrder(2, 1);
        as(staffEmail, () -> { orderService.confirm(id); orderService.ship(id); return null; });
        assertThatThrownBy(() -> as(staffEmail, () -> db(() -> { orderService.complete(id); throw new IllegalStateException("Rollback cả lô"); })))
            .isInstanceOf(IllegalStateException.class);
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).isEmpty();
        assertThat(grants.existsById(id)).isFalse();
        assertThat(points.existsBySourceKey("EARN:ORDER:" + id)).isFalse();
        db(() -> { orders.findById(id).orElseThrow().complete(); return null; });
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return true; }));
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(3)
            .extracting(Subscription::getSourceKey).containsExactlyInAnyOrderElementsOf(expectedKeys(id));
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(70);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void legacyOrderLevelSourceKeysAndReceiptsRemainValid(boolean receiptExists) throws Exception {
        var id = as(email, () -> { carts.addItem(customerId, packId, 2); return orderService.placeOrderFromCart(customerId, "Đơn đã cấp theo cách cũ").getId(); });
        var originalId = db(() -> {
            var order = orders.findById(id).orElseThrow(); order.confirm(); order.ship(); order.complete();
            var sub = subscriptions.saveAndFlush(Subscription.create(order.getCustomer(), null,
                (MonthlyCarePackage) products.findById(packId).orElseThrow(), TODAY, 2, "ORDER:" + id));
            if (receiptExists) grants.saveAndFlush(new SubscriptionGrant(order, sub));
            return sub.getId();
        });
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return true; }));
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).singleElement().satisfies(s -> {
            assertThat(s.getId()).isEqualTo(originalId); assertThat(s.getSourceKey()).isEqualTo("ORDER:" + id);
            assertThat(s.getRemainingUses()).isEqualTo(8);
        });
        assertThat(as(email, loyalty::mine).totalPoints()).isEqualTo(40);
    }
    void editPackage(int uses, int months, List<Long> serviceIds) {
        db(() -> {
            var pack = (MonthlyCarePackage) products.findById(packId).orElseThrow();
            var form = pack.toForm(); form.setIncludedUses(uses); form.setDurationMonths(months); form.setBasePrice(new BigDecimal("900000"));
            pack.updateCatalog(form);
            pack.configureCoveredServices(serviceIds.stream().map(id -> (ServiceItem) products.findById(id).orElseThrow()).toList());
            return null;
        });
    }
    void assertPurchasedBenefits(Long id, int uses) {
        db(() -> {
            var s = subscriptions.findById(id).orElseThrow();
            assertThat(s.getCarePackage().getId()).isEqualTo(packId);
            assertThat(s.getRemainingUses()).isEqualTo(uses);
            assertThat(s.getEndDate()).isEqualTo(TODAY.plusMonths(1).minusDays(1));
            assertThat(s.getCoveredServices()).extracting(Product::getId).containsExactly(serviceId);
            return null;
        });
    }
    @Test void purchaseSnapshotsPackageUsesMonthsAndServicesBeforeCatalogChanges() {
        var id = purchase();
        db(() -> {
            var item = orders.findById(id).orElseThrow().getItems().getFirst();
            assertThat(item.getSnapshotPackage().getId()).isEqualTo(packId);
            assertThat(item.getSnapshotIncludedUses()).isEqualTo(4); assertThat(item.getSnapshotDurationMonths()).isOne();
            assertThat(item.getSnapshotServices()).extracting(Product::getId).containsExactly(serviceId);
            return null;
        });
        var other = products.saveAndFlush(new ProductFactory().create(ProductFactoryTest.form(ProductType.VET, "Dịch vụ mới", "100000"))).getId();
        editPackage(1, 3, List.of(other));
        complete(id);
        var subId = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId).getFirst().getId();
        assertPurchasedBenefits(subId, 4);
        assertThat(orders.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("200000");
        editPackage(9, 6, List.of(other));
        assertPurchasedBenefits(subId, 4);
    }
    @Test void renewalSnapshotsRightsAndReplayedEventsDoNotReadEditedCatalog() throws Exception {
        var subId = seed(TODAY.minusMonths(2), 4); service.expireIfDue(subId, TODAY);
        var id = as(email, () -> service.renew(subId));
        var other = products.saveAndFlush(new ProductFactory().create(ProductFactoryTest.form(ProductType.VET, "Dịch vụ đổi sau gia hạn", "100000"))).getId();
        editPackage(1, 3, List.of(other));
        complete(id);
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return true; }));
        assertPurchasedBenefits(subId, 4);
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(1);
        assertThat(orders.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("200000");
        editPackage(8, 6, List.of(other)); assertPurchasedBenefits(subId, 4);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void renewalReplacesOldServicesWithSnapshotForExpiredAndActivePackages(boolean elapsed) {
        var start = elapsed ? TODAY.minusMonths(1) : TODAY.minusMonths(1).plusDays(4);
        var subId = seed(start, 4);
        var oldEnd = read(subId).getEndDate();
        if (elapsed) service.expireIfDue(subId, TODAY);
        var snapshotServiceId = products.saveAndFlush(new ProductFactory().create(
            ProductFactoryTest.form(ProductType.VET, "Dịch vụ B trong bản chụp gia hạn", "100000"))).getId();
        var laterServiceId = products.saveAndFlush(new ProductFactory().create(
            ProductFactoryTest.form(ProductType.VET, "Dịch vụ C sau khi đặt đơn", "100000"))).getId();

        // Gói cũ chứa A; danh mục đổi sang B trước khi chốt đơn, rồi sang C trước khi hoàn tất.
        editPackage(2, 2, List.of(snapshotServiceId));
        var orderId = as(email, () -> service.renew(subId));
        db(() -> {
            var item = orders.findById(orderId).orElseThrow().getItems().getFirst();
            assertThat(item.getSnapshotServices()).extracting(Product::getId).containsExactly(snapshotServiceId);
            assertThat(read(subId).getCoveredServices()).extracting(Product::getId).containsExactly(serviceId);
            return null;
        });
        editPackage(9, 3, List.of(laterServiceId));
        complete(orderId);

        int expectedUses = elapsed ? 2 : 6;
        db(() -> {
            var renewed = read(subId);
            assertThat(renewed.getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(renewed.getStartDate()).isEqualTo(elapsed ? TODAY : start);
            assertThat(renewed.getEndDate()).isEqualTo(
                (elapsed ? TODAY : oldEnd.plusDays(1)).plusMonths(2).minusDays(1));
            assertThat(renewed.getRemainingUses()).isEqualTo(expectedUses);
            assertThat(renewed.getCoveredServices()).extracting(Product::getId).containsExactly(snapshotServiceId);
            assertThat(renewed.covers(serviceId)).isFalse();
            assertThat(renewed.covers(snapshotServiceId)).isTrue();
            assertThat(renewed.covers(laterServiceId)).isFalse();
            return null;
        });
        var oldServiceBooking = book(10);
        var snapshotForm = form(12); snapshotForm.setServiceId(snapshotServiceId);
        var snapshotBooking = as(email, () -> appointmentService.book(snapshotForm));
        var laterForm = form(14); laterForm.setServiceId(laterServiceId);
        var laterBooking = as(email, () -> appointmentService.book(laterForm));
        for (var paidId : List.of(oldServiceBooking, laterBooking)) {
            var paid = appointments.findById(paidId).orElseThrow();
            assertThat(paid.getSubscription()).isNull();
            assertThat(paid.getPrice()).isEqualByComparingTo("100000");
        }
        var covered = appointments.findById(snapshotBooking).orElseThrow();
        assertThat(covered.getSubscription().getId()).isEqualTo(subId);
        assertThat(covered.getPrice()).isEqualByComparingTo("0");
        assertThat(read(subId).getRemainingUses()).isEqualTo(expectedUses - 1);
    }
    @Test void emptyServiceSnapshotDoesNotFallBackToLaterCatalogConfiguration() {
        var id = purchase();
        as(staffEmail, () -> { orderService.confirm(id); orderService.ship(id); return null; });
        // Bản chụp rỗng từ phiên bản cũ phải bị từ chối, không lấy quyền lợi mới để che dữ liệu thiếu.
        db(() -> {
            entityManager.createNativeQuery("delete from order_item_snapshot_services where order_item_id in (select id from order_items where order_id = :id)")
                .setParameter("id", id).executeUpdate();
            return null;
        });
        editPackage(1, 3, List.of(serviceId));
        assertThatThrownBy(() -> as(staffEmail, () -> orderService.complete(id)))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("tạm ngừng bán");
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).isEmpty();
        assertThat(grants.existsById(id)).isFalse();
        assertThat(points.existsBySourceKey("EARN:ORDER:" + id)).isFalse();
        assertThat(orders.findById(id).orElseThrow().getStateName()).isEqualTo(OrderStatus.SHIPPING);
    }
    @Test void legacyMissingSnapshotFallsBackToCurrentBenefits() {
        var id = legacyOrder(1, 1);
        editPackage(1, 3, List.of(serviceId));
        complete(id);
        db(() -> {
            var first = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId).stream()
                .filter(s -> s.getCarePackage().getId().equals(packId)).findFirst().orElseThrow();
            assertThat(first.getRemainingUses()).isOne();
            assertThat(first.getEndDate()).isEqualTo(TODAY.plusMonths(3).minusDays(1));
            assertThat(first.getCoveredServices()).extracting(Product::getId).containsExactly(serviceId);
            return null;
        });
        assertThat(orders.findById(id).orElseThrow().getFinalAmount()).isEqualByComparingTo("500000");
    }
    @Test void existingUnitKeyIsPreservedWithoutBatchReceipt() throws Exception {
        var id = as(email, () -> { carts.addItem(customerId, packId, 2); return orderService.placeOrderFromCart(customerId, "Kiểm tra khóa đơn vị").getId(); });
        var firstId = db(() -> {
            var order = orders.findById(id).orElseThrow(); order.confirm(); order.ship(); order.complete();
            var item = order.getItems().getFirst();
            var first = Subscription.createFromBenefits(order.getCustomer(), null, item.subscriptionBenefits().orElseThrow(),
                TODAY, 1, "ORDER:" + id + ":ITEM:" + item.getId() + ":UNIT:1");
            first.useOnce(TODAY);
            return subscriptions.saveAndFlush(first).getId();
        });
        race(() -> db(() -> { events.publishEvent(new OrderCompletedEvent(id)); return true; }));
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(2)
            .extracting(Subscription::getSourceKey).containsExactlyInAnyOrderElementsOf(expectedKeys(id));
        assertThat(read(firstId).getRemainingUses()).isEqualTo(3);
        assertThat(grants.existsById(id)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void navigationAndAccessMatchEveryRole(String role) throws Exception {
        seed(TODAY, 1);
        var request = get("/"); if (!role.equals("GUEST")) request.with(user("viewer").roles(role));
        var html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var section : List.of(html.substring(html.indexOf("<header"), html.indexOf("</header>")), html.substring(html.indexOf("<main"), html.indexOf("</main>")))) {
            assertThat(section.contains("href=\"/subscriptions\"")).isEqualTo(role.equals("CUSTOMER"));
            assertThat(section.contains("href=\"/admin/subscriptions\"")).isEqualTo(role.equals("ADMIN"));
        }
        if (role.equals("GUEST")) {
            mvc.perform(get("/subscriptions")).andExpect(status().is3xxRedirection());
            mvc.perform(get("/admin/subscriptions")).andExpect(status().is3xxRedirection());
        } else {
            var actor = role.equals("CUSTOMER") ? email : role.equals("ADMIN") ? "admin@petcare.vn" : staffEmail;
            mvc.perform(get("/subscriptions").with(user(actor).roles(role))).andExpect(role.equals("CUSTOMER") ? status().isOk() : status().isForbidden());
            mvc.perform(get("/admin/subscriptions").with(user(actor).roles(role))).andExpect(role.equals("ADMIN") ? status().isOk() : status().isForbidden());
        }
    }
    @Test void ownerChecksCsrfAndVietnameseRendering() throws Exception {
        var id = seed(TODAY, 2);
        mvc.perform(get("/subscriptions").with(user(email).roles("CUSTOMER"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tắm và chăm sóc có dấu")));
        mvc.perform(post("/subscriptions/{id}/cancel", id).with(user(email).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/subscriptions/{id}/cancel", id).with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
        assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.ACTIVE);
        mvc.perform(post("/subscriptions/{id}/cancel", id).with(user(email).roles("CUSTOMER")).with(csrf())).andExpect(redirectedUrl("/subscriptions"));
        assertThat(read(id).getStateName()).isEqualTo(SubscriptionStatus.CANCELLED);
    }
    /** Mô phỏng gói cũ thiếu cấu hình, không dùng API mới vốn bắt buộc chọn dịch vụ. */
    void removeLegacyCoverage() {
        db(() -> { entityManager.createNativeQuery("delete from monthly_package_services where package_id = :id")
            .setParameter("id", packId).executeUpdate(); return null; });
    }
    @Test void unconfiguredPackageCannotBeAddedToCart() {
        removeLegacyCoverage();
        assertThatThrownBy(() -> as(email, () -> { carts.addItem(customerId, packId, 1); return null; }))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("tạm ngừng bán");
    }
    @Test void checkoutRechecksCoverageAndConfigurationRestoresPurchasing() {
        as(email, () -> { carts.addItem(customerId, packId, 1); return null; });
        removeLegacyCoverage(); var before = orders.count();
        assertThatThrownBy(() -> as(email, () -> orderService.placeOrderFromCart(customerId, "Địa chỉ")))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("tạm ngừng bán");
        assertThat(orders.count()).isEqualTo(before);
        editPackage(4, 1, List.of(serviceId));
        var id = as(email, () -> orderService.placeOrderFromCart(customerId, "Địa chỉ").getId());
        complete(id);
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).singleElement()
            .satisfies(s -> assertThat(s.getRemainingUses()).isEqualTo(4));
    }
    @Test void hiddenOnlyCoveredServiceAlsoStopsSales() {
        db(() -> { products.findById(serviceId).orElseThrow().hide(); return null; });
        assertThatThrownBy(() -> as(email, () -> { carts.addItem(customerId, packId, 1); return null; }))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("tạm ngừng bán");
    }
    @Test void renewalOfUnconfiguredPackageIsRejectedWithoutLeavingReservation() {
        var id = seed(TODAY.minusMonths(2), 4); service.expireIfDue(id, TODAY);
        removeLegacyCoverage(); var before = orders.count();
        assertThatThrownBy(() -> as(email, () -> service.renew(id)))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("tạm ngừng bán");
        assertThat(read(id).isRenewalPending()).isFalse();
        assertThat(orders.count()).isEqualTo(before);
    }
    @Test void legacyOrderWithoutSnapshotAndCoverageCannotGrantSubscription() {
        var id = legacyOrder(1, 1);
        as(staffEmail, () -> { orderService.confirm(id); orderService.ship(id); return null; });
        removeLegacyCoverage();
        assertThatThrownBy(() -> as(staffEmail, () -> orderService.complete(id)))
            .isInstanceOf(com.petcare.domain.purchase.PurchaseException.class).hasMessageContaining("tạm ngừng bán");
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).isEmpty();
        assertThat(grants.existsById(id)).isFalse();
        assertThat(points.existsBySourceKey("EARN:ORDER:" + id)).isFalse();
        editPackage(4, 1, List.of(serviceId));
        as(staffEmail, () -> orderService.complete(id));
        assertThat(subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(customerId)).hasSize(2);
    }
    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void legacyPackageShowsUnavailableAndStaffConfigurationWarning(String role) throws Exception {
        removeLegacyCoverage();
        var request = get(role.equals("STAFF") || role.equals("ADMIN") ? "/staff/products" : "/products")
            .param("q", "Gói tháng chăm sóc").param("descending", "true");
        if (!role.equals("GUEST")) request.with(user(email).roles(role));
        // Chi tiết luôn chọn đúng gói kiểm thử; danh sách quản lý phải có cảnh báo cho STAFF/ADMIN.
        if (role.equals("STAFF") || role.equals("ADMIN")) mvc.perform(request).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng sửa gói và chọn dịch vụ")));
        var detail = get("/products/" + packId);
        if (!role.equals("GUEST")) detail.with(user(email).roles(role));
        var html = mvc.perform(detail).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Gói tạm ngừng bán").doesNotContain("Thêm vào giỏ", "Đăng nhập để mua hàng");
    }
    String previewHtml() throws Exception {
        return mvc.perform(get("/appointments/confirm").param("serviceId", serviceId.toString())
            .param("staffId", staffId.toString()).param("startTime", TODAY.atTime(12, 0).toString())
            .with(user(email).roles("CUSTOMER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    @Test void previewShowsSelectedPackageAndZeroPriceWithoutMutatingAnything() throws Exception {
        seed(TODAY, 4);
        var earliest = seed(TODAY.minusDays(3), 2);
        seed(TODAY.minusDays(3), 3); // Cùng hạn: ID nhỏ hơn được chọn.
        var before = read(earliest); var appointmentCount = appointments.count();
        assertThat(previewHtml()).contains("Dự kiến dùng gói #" + earliest, "Lượt còn lại: 2", "<strong>0đ</strong>")
            .doesNotContain("Thanh toán dự kiến", "Giá: 100000");
        assertThat(previewHtml()).contains("Lượt còn lại: 2");
        var after = read(earliest);
        assertThat(after.getRemainingUses()).isEqualTo(before.getRemainingUses());
        assertThat(after.getVersion()).isEqualTo(before.getVersion());
        assertThat(after.getStateName()).isEqualTo(before.getStateName());
        assertThat(appointments.count()).isEqualTo(appointmentCount);
        var booked = book(12);
        db(() -> { assertThat(appointments.findById(booked).orElseThrow().getSubscription().getId()).isEqualTo(earliest); return null; });
        assertThat(read(earliest).getRemainingUses()).isOne();
    }
    @ParameterizedTest @ValueSource(strings = {"none", "empty", "elapsed", "uncovered", "cancelled"})
    void previewWithoutApplicablePackageShowsUsualPriceAndDoesNotMutate(String reason) throws Exception {
        Long id = null;
        if (!reason.equals("none")) {
            id = seed(reason.equals("elapsed") ? TODAY.minusMonths(2) : TODAY, 1);
            final var subId = id;
            db(() -> {
                var sub = subscriptions.findById(subId).orElseThrow();
                if (reason.equals("empty")) sub.useOnce(TODAY);
                if (reason.equals("cancelled")) sub.cancel();
                return null;
            });
            if (reason.equals("uncovered")) serviceId = products.saveAndFlush(new ProductFactory().create(
                ProductFactoryTest.form(ProductType.VET, "Khám ngoài gói", "100000"))).getId();
        }
        var before = id == null ? null : read(id);
        assertThat(previewHtml()).contains("Thanh toán dự kiến", "100000").doesNotContain("Dự kiến dùng gói #", "<strong>0đ</strong>");
        if (id != null) {
            assertThat(read(id).getRemainingUses()).isEqualTo(before.getRemainingUses());
            assertThat(read(id).getVersion()).isEqualTo(before.getVersion());
            assertThat(read(id).getStateName()).isEqualTo(before.getStateName());
        }
    }
    @Test void postRechecksPackageAfterPreviewAndDoesNotUseTheSameLastUseTwice() throws Exception {
        var id = seed(TODAY, 1);
        assertThat(previewHtml()).contains("Dự kiến dùng gói #" + id);
        book(13);
        mvc.perform(post("/appointments").with(user(email).roles("CUSTOMER")).with(csrf())
            .param("serviceId", serviceId.toString()).param("staffId", staffId.toString())
            .param("startTime", TODAY.atTime(12, 0).toString()).param("petName", "Bông").param("petSpecies", "CAT"))
            .andExpect(status().is3xxRedirection());
        assertThat(read(id).getRemainingUses()).isZero();
        db(() -> {
            var booked = appointments.findByCustomerIdOrderByStartTimeDescIdDesc(customerId);
            assertThat(booked).hasSize(2);
            assertThat(booked.stream().filter(a -> a.getSubscription() != null)).hasSize(1);
            assertThat(booked.stream().filter(a -> a.getSubscription() == null).findFirst().orElseThrow().getFinalAmount()).isEqualByComparingTo("100000");
            return null;
        });
    }
}
