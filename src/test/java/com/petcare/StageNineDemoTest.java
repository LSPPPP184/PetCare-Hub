package com.petcare;

import com.petcare.config.DemoDataRunner;
import com.petcare.domain.*;
import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.order.OrderStatus;
import com.petcare.domain.subscription.SubscriptionStatus;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.repository.*;
import com.petcare.service.DashboardService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:stage9_demo;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "demo"}) @AutoConfigureMockMvc
@Import({StageNineDemoTest.Time.class, StageNineDemoTest.ErrorProbe.class}) @Transactional
class StageNineDemoTest {
    @TestConfiguration static class Time {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
    @Controller static class ErrorProbe {
        @GetMapping("/_test/lock-spring") String springLock() { throw new org.springframework.dao.OptimisticLockingFailureException("secret"); }
        @GetMapping("/_test/lock-jpa") String jpaLock() { throw new jakarta.persistence.OptimisticLockException("secret"); }
        @GetMapping("/_test/missing") String missing() { throw new EntityNotFoundException("secret-database-password"); }
        @GetMapping("/_test/state") String state() { throw new InvalidStateTransitionException("secret-state"); }
        @GetMapping("/_test/validate") String validate(@RequestParam int number) { return "home"; }
        @GetMapping("/_test/failure") String failure() { throw new IllegalStateException("secret-database-password"); }
    }
    @Test void invalidPagesAndGlobalLockConflicts() throws Exception {
        for (var path : List.of("/products", "/staff/products", "/admin/members"))
            for (var page : List.of("-1", "2147483647", "abc"))
                mvc.perform(get(path).param("page", page).with(user("admin@petcare.vn").roles("ADMIN")))
                    .andExpect(status().isBadRequest());
        for (var path : List.of("/_test/lock-spring", "/_test/lock-jpa"))
            mvc.perform(get(path)).andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dữ liệu đã được thay đổi, vui lòng tải lại trang")));
    }
    @Test void dashboardAccessContentAndLegacyRedirectsByRole() throws Exception {
        for (var role : List.of("GUEST", "CUSTOMER", "STAFF", "ADMIN")) {
            for (var path : List.of("/admin", "/staff", "/admin/dashboard", "/staff/dashboard")) {
                var request = get(path);
                if (!role.equals("GUEST")) request.with(user(role.toLowerCase() + "@petcare.vn").roles(role));
                var result = mvc.perform(request);
                if (role.equals("GUEST")) result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
                else if (role.equals("CUSTOMER") || (role.equals("STAFF") && path.startsWith("/admin"))) result.andExpect(status().isForbidden());
                else if (path.endsWith("/dashboard")) result.andExpect(redirectedUrl(path.replace("/dashboard", "")));
                else {
                    var html = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                    if (path.equals("/staff")) assertThat(html).contains("Đơn chờ xử lý", "Lịch hẹn hôm nay", "Mũi tiêm sắp đến hạn", "Mũi tiêm quá hạn").doesNotContain("Doanh thu theo tháng", "Thành viên theo hạng");
                    else assertThat(html).contains("Doanh thu theo tháng", "Top 5 sản phẩm bán chạy", "Thành viên theo hạng", "Giả lập ngày");
                }
            }
        }
        for (var role : List.of("GUEST", "CUSTOMER", "STAFF", "ADMIN")) {
            for (var path : List.of("/staff/orders", "/staff/appointments", "/staff/health", "/staff/products", "/admin/members", "/admin/subscriptions", "/admin/demo/health", "/cart", "/orders", "/my-pets", "/membership", "/subscriptions")) {
                var request = get(path);
                if (!role.equals("GUEST")) request.with(user(role.toLowerCase() + "@petcare.vn").roles(role));
                var result = mvc.perform(request);
                boolean allowed = path.startsWith("/admin") ? role.equals("ADMIN") : path.startsWith("/staff") ? List.of("STAFF", "ADMIN").contains(role) : role.equals("CUSTOMER");
                if (allowed) result.andExpect(status().isOk());
                else if (role.equals("GUEST")) result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
                else result.andExpect(status().isForbidden());
            }
        }
        var work = dashboard.workOverview();
        assertThat(work.pendingOrders()).isEqualTo(orders.findAll().stream().filter(o -> o.getStateName() == OrderStatus.PENDING).count());
        assertThat(work.appointmentsToday()).isEqualTo(dashboard.overview().appointmentsToday());
        assertThat(work.dueSoon()).isPositive();
        assertThat(work.overdue()).isPositive();
    }
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired com.petcare.service.PetHealthService petHealth;
    @Autowired com.petcare.service.SubscriptionService subscriptionService;
    @Autowired com.petcare.service.MatchingService matchingService;
    @Autowired QuizResultRepository quizResults;

    private org.hibernate.stat.Statistics freshStatistics() {
        entityManager.flush(); entityManager.clear();
        var stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true); stats.clear(); return stats;
    }
    @Test @org.springframework.security.test.context.support.WithMockUser(username = "admin@petcare.vn", roles = "ADMIN")
    void staleVaccinationStatesStillMatchDashboardAndListDates() {
        entityManager.createQuery("update VaccinationRecord v set v.stateName = :scheduled where v.stateName <> :completed")
            .setParameter("scheduled", VaccinationStatus.SCHEDULED).setParameter("completed", VaccinationStatus.COMPLETED).executeUpdate();
        entityManager.clear();
        var due = petHealth.alerts(VaccinationStatus.DUE_SOON);
        var overdue = petHealth.alerts(VaccinationStatus.OVERDUE);
        assertThat(due).isNotEmpty().allSatisfy(v -> assertThat(v.status()).isEqualTo(VaccinationStatus.DUE_SOON));
        assertThat(overdue).isNotEmpty().allSatisfy(v -> assertThat(v.status()).isEqualTo(VaccinationStatus.OVERDUE));
        assertThat(dashboard.overview().petsDueSoon()).isEqualTo(due.stream().map(v -> v.petId()).distinct().count());
        assertThat(dashboard.overview().petsOverdue()).isEqualTo(overdue.stream().map(v -> v.petId()).distinct().count());
        assertThat(dashboard.workOverview().dueSoon()).isEqualTo(due.size());
        assertThat(dashboard.workOverview().overdue()).isEqualTo(overdue.size());
        assertThat(vaccinations.findById(overdue.getFirst().id()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
    }
    @Test @org.springframework.security.test.context.support.WithMockUser(username = "admin@petcare.vn", roles = "ADMIN")
    void listQueryCountsStayBoundedAfterClearingPersistenceContext() {
        var owner = users.findByEmail("customer@petcare.vn").orElseThrow();
        for (int i = 0; i < 25; i++) quizResults.save(new QuizResult(owner, new com.petcare.dto.QuizAnswers(), "test", "Kiểm thử", LocalDateTime.of(2026, 11, 18, 0, 0).plusMinutes(i), List.of()));
        for (var pet : pets.findAll()) {
            for (int i = 0; i < 4; i++) vaccinations.save(VaccinationRecord.schedule(pet, "Mũi kiểm thử " + i,
                LocalDate.of(2026, 11, 18), "", LocalDate.of(2026, 11, 18), 7));
        }
        var page = org.springframework.data.domain.PageRequest.of(0, 2);
        var stats = freshStatistics();
        try {
            var alerts = petHealth.alerts(null, page);
            assertThat(alerts.getContent()).hasSize(2);
            assertThat(alerts.getTotalElements()).isGreaterThan(2);
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
            stats = freshStatistics();
            var largerAlerts = petHealth.alerts(null, org.springframework.data.domain.PageRequest.of(0, 20));
            assertThat(largerAlerts.getNumberOfElements()).isGreaterThan(2);
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
            stats = freshStatistics();
            var subs = subscriptionService.active(page);
            assertThat(subs.getContent()).hasSize(2);
            assertThat(subs.getTotalElements()).isEqualTo(3);
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(4);
            stats = freshStatistics();
            var history = matchingService.history(owner.getEmail(), page);
            assertThat(history.getContent()).hasSize(2);
            assertThat(history.getTotalElements()).isEqualTo(25);
            assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
            assertThat(stats.getCollectionFetchCount()).isZero();
            assertThat(stats.getEntityLoadCount()).isZero();
            var second = matchingService.history(owner.getEmail(), org.springframework.data.domain.PageRequest.of(1, 2));
            assertThat(second.getContent()).extracting(v -> v.id()).doesNotContainAnyElementsOf(history.stream().map(v -> v.id()).toList());
        } finally { stats.setStatisticsEnabled(false); }
    }
    @Autowired MockMvc mvc;
    @Autowired DemoDataRunner runner;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired AppointmentRepository appointments;
    @Autowired OwnedPetRepository pets;
    @Autowired VaccinationRecordRepository vaccinations;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired DashboardService dashboard;

    @Test void emptyDatabaseDemoHasAllStatesAndRerunDoesNotChangeAnything() {
        assertThat(orders.findAll()).hasSize(10).extracting(Order::getStateName).contains(OrderStatus.values());
        assertThat(orders.findAll()).extracting(Order::getSeedKey).doesNotHaveDuplicates().doesNotContainNull();
        assertThat(vaccinations.findAll()).extracting(VaccinationRecord::getStateName).contains(VaccinationStatus.values());
        assertThat(subscriptions.findAll()).filteredOn(s -> s.getStateName() == SubscriptionStatus.ACTIVE).hasSize(3);
        assertThat(dashboard.overview().members()).allSatisfy(m -> assertThat(m.count()).isEqualTo(1));
        assertThat(dashboard.overview().revenue()).filteredOn(r -> r.amount().signum() > 0).hasSize(6);
        assertThat(dashboard.overview().appointmentsToday()).isEqualTo(1);
        var snapshot = snapshot(); runner.run(); runner.run();
        assertThat(snapshot()).isEqualTo(snapshot);
        assertThat(users.findByEmail("admin@petcare.vn").orElseThrow().getFullName()).isEqualTo("Quản trị viên");
        assertThat(products.findAll()).anySatisfy(p -> assertThat(p.getName()).isEqualTo("Bông – chó Poodle"));
    }
    private List<Long> snapshot() {
        return List.of(users.count(), products.count(), orders.count(), appointments.count(), pets.count(), vaccinations.count(), subscriptions.count());
    }

    @Test void partiallyPopulatedDatabaseIsNotBackfilled() {
        orders.deleteAll(); orders.flush();
        var before = snapshot(); runner.run();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(orders.count()).isZero();
    }

    @Test void globalErrorsRenderVietnameseWithoutInternalDetails() throws Exception {
        for (var path : List.of("/not-a-page", "/products/9223372036854775807", "/_test/missing"))
            mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(view().name("error/404"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy trang")));
        mvc.perform(get("/_test/state")).andExpect(status().isConflict()).andExpect(view().name("error/request"));
        mvc.perform(get("/_test/validate").param("number", "secret-invalid-value"))
            .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-invalid-value"))));
        mvc.perform(get("/_test/failure")).andExpect(status().isInternalServerError())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã xảy ra lỗi hệ thống")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-database-password"))));
        mvc.perform(get("/403")).andExpect(status().isForbidden()).andExpect(content().string(org.hamcrest.Matchers.containsString("Không có quyền truy cập")));
        mvc.perform(post("/admin").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")));
    }

    @Test void customersCannotReadOrChangeAnotherCustomersDataAndCsrfIsRequired() throws Exception {
        var owner = users.findByEmail("customer@petcare.vn").orElseThrow();
        var order = orders.findSummaries(owner.getId(), null).getFirst();
        var pet = pets.findByOwnerIdOrderByIdAsc(owner.getId()).getFirst();
        var appointment = appointments.findByCustomerIdOrderByStartTimeDescIdDesc(owner.getId()).getFirst();
        var subscription = subscriptions.findByCustomerIdOrderByEndDateAscIdAsc(owner.getId()).getFirst();
        for (String path : List.of("/orders/" + order.id(), "/my-pets/" + pet.getId(), "/my-pets/" + pet.getId() + "/edit", "/appointments/" + appointment.getId())) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("loyalty.gold@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        }
        for (String path : List.of("/orders/" + order.id() + "/cancel", "/appointments/" + appointment.getId() + "/cancel",
                "/subscriptions/" + subscription.getId() + "/cancel", "/subscriptions/" + subscription.getId() + "/renew")) {
            mvc.perform(post(path).with(user("loyalty.gold@petcare.vn").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post(path).with(user("customer@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        }
        for (var role : List.of("CUSTOMER", "STAFF"))
            mvc.perform(get("/admin").with(user("tester").roles(role))).andExpect(status().isForbidden());
        var html = mvc.perform(get("/admin").with(user("admin@petcare.vn").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Doanh thu theo tháng", "Top 5 sản phẩm bán chạy", "Thành viên theo hạng")
            .doesNotContain("Admin@123", "$2a$", "$2b$");
    }

    @Test void allFeatureGetRoutesAreReachableByFollowingLinksForTheirRoles() throws Exception {
        for (var role : List.of("GUEST", "CUSTOMER", "STAFF", "ADMIN")) {
            Set<String> reached = new HashSet<>();
            var queue = new ArrayDeque<String>(); queue.add("/");
            Set<String> visited = new HashSet<>();
            while (!queue.isEmpty()) {
                var url = queue.removeFirst();
                // Một đại diện cho mỗi trang: bỏ phân trang/lọc và các ID khác để không duyệt hàng trăm khung giờ.
                var key = url.split("\\?")[0].replaceAll("/\\d+", "/{id}");
                if (!visited.add(key)) continue;
                var request = get(java.net.URI.create(url.replace("&amp;", "&")));
                if (!role.equals("GUEST")) request.with(user(role.toLowerCase() + "@petcare.vn").roles(role));
                var response = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
                reached.add(key);
                var links = Pattern.compile("href=\"(/[^\"]*)\"").matcher(response.getContentAsString());
                while (links.find()) queue.add(links.group(1));
            }
        var allRoutes = List.of("/", "/login", "/register", "/products", "/products/{id}", "/staff/products",
            "/staff/products/new", "/staff/products/{id}/edit", "/cart", "/checkout", "/orders", "/orders/{id}",
            "/staff/orders", "/staff/orders/{id}", "/appointments/services", "/appointments/slots", "/appointments/confirm",
            "/appointments", "/appointments/{id}", "/staff/appointments", "/staff/appointments/{id}",
            "/my-pets", "/my-pets/new", "/my-pets/{id}", "/my-pets/{id}/edit", "/staff/health", "/staff/health/pets",
            "/staff/health/pets/{id}", "/staff/health/pets/{id}/records/new", "/admin/demo/health", "/quiz", "/quiz/results",
            "/quiz/history", "/membership", "/admin/members", "/admin/members/{id}", "/subscriptions", "/admin/subscriptions",
            "/admin", "/staff");
        var publicRoutes = Set.of("/", "/login", "/register", "/products", "/products/{id}", "/appointments/services", "/quiz", "/quiz/results");
        var expected = allRoutes.stream().filter(path -> (publicRoutes.contains(path) && (!List.of("/login", "/register").contains(path) || role.equals("GUEST")))
            || (path.startsWith("/admin") && role.equals("ADMIN"))
            || (path.startsWith("/staff") && List.of("STAFF", "ADMIN").contains(role))
            || (!publicRoutes.contains(path) && !path.startsWith("/admin") && !path.startsWith("/staff") && role.equals("CUSTOMER"))).toList();
        assertThat(reached).as("Liên kết cho vai trò %s", role).containsAll(expected);
        }
        // Lịch sử quiz trống ở database demo mới; luồng POST tạo kết quả và liên kết chi tiết được kiểm thử trong MatchingWebTest.
    }
}
