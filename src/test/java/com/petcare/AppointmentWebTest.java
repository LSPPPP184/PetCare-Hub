package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.appointment.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.dto.AppointmentForm;
import com.petcare.repository.*;
import com.petcare.service.AppointmentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
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

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:appointment_web;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Import(AppointmentWebTest.FixedTime.class)
class AppointmentWebTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
    @Autowired com.petcare.repository.PointTransactionRepository pointTransactions;
    @Autowired MockMvc mvc;
    @Autowired AppointmentService service;
    @Autowired AppointmentRepository appointments;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired EntityManager entityManager;
    Long staffId, otherStaffId, serviceId, vetId;
    private <T> T db(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }
    private <T> T as(String email, Supplier<T> work) {
        var previous = SecurityContextHolder.getContext(); var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(email, "", List.of()));
        SecurityContextHolder.setContext(context);
        try { return work.get(); } finally { SecurityContextHolder.setContext(previous); }
    }
    @BeforeEach void setup() {
        db(() -> { pointTransactions.deleteAll(); appointments.deleteAll(); products.deleteAll(); return null; });
        staffId = users.findByEmail("staff@petcare.vn").orElseThrow().getId();
        otherStaffId = users.findByEmail("other-staff@petcare.vn").orElseGet(() -> users.saveAndFlush(new User("Nhân viên khác",
            "other-staff@petcare.vn", encoder.encode("TestOnly@123"), "", Role.STAFF))).getId();
        users.findByEmail("other-appointment@petcare.vn").orElseGet(() -> users.saveAndFlush(new User("Khách khác",
            "other-appointment@petcare.vn", encoder.encode("TestOnly@123"), "", Role.CUSTOMER)));
        serviceId = product(ProductType.GROOMING); vetId = product(ProductType.VET);
    }
    private Long product(ProductType type) {
        var f = ProductFactoryTest.form(type, "Dịch vụ thử " + type, "100.00"); f.setDurationMinutes(30);
        return products.saveAndFlush(new ProductFactory().create(f)).getId();
    }
    private AppointmentForm form(Long staff, String start) {
        var f = new AppointmentForm(); f.setServiceId(serviceId); f.setStaffId(staff);
        f.setStartTime(LocalDateTime.parse(start)); f.setPetName("Milo"); f.setPetSpecies(Species.CAT); f.setNote("Ghi chú");
        return f;
    }
    private ResultActions book(Long employee, Long product, String start) throws Exception {
        return mvc.perform(post("/appointments").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())
            .param("serviceId", product.toString()).param("staffId", employee.toString()).param("startTime", start)
            .param("petName", "Milo").param("petSpecies", "CAT").param("note", "Ghi chú"));
    }
    private Long booked(Long employee, String start) throws Exception {
        var response = book(employee, serviceId, start).andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("success", "Đặt lịch thành công.")).andReturn().getResponse();
        return Long.valueOf(response.getRedirectedUrl().substring("/appointments/".length()));
    }
    private String getPage(String path, String email, String role) throws Exception {
        return mvc.perform(get(path).with(user(email).roles(role))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private ResultActions staffAction(Long id, String action) throws Exception {
        return mvc.perform(post("/staff/appointments/" + id + "/" + action).with(user("staff@petcare.vn").roles("STAFF")).with(csrf()));
    }
    @Test void completeBookingJourneyAndSnapshotsSurviveServiceEdits() throws Exception {
        String services = mvc.perform(get("/appointments/services")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(services).contains("Đăng nhập để đặt lịch");
        assertThat(getPage("/appointments/services", "customer@petcare.vn", "CUSTOMER")).contains("/appointments/slots?serviceId=");
        assertThat(getPage("/products/" + serviceId, "customer@petcare.vn", "CUSTOMER")).contains("/appointments/slots?serviceId=" + serviceId);
        assertThat(getPage("/appointments/slots?serviceId=" + serviceId + "&date=2026-01-05", "customer@petcare.vn", "CUSTOMER"))
            .contains("/appointments/confirm?", "Nhân viên khác");
        assertThat(getPage("/appointments/confirm?serviceId=" + serviceId + "&staffId=" + staffId + "&startTime=2026-01-05T10:00",
            "customer@petcare.vn", "CUSTOMER")).contains("Xác nhận đặt lịch", "name=\"_csrf\"");
        Long id = booked(staffId, "2026-01-05T10:00");
        var before = as("customer@petcare.vn", () -> service.myDetail(id));
        assertThat(before.endTime()).isEqualTo(LocalDateTime.parse("2026-01-05T10:30"));
        db(() -> { var p = products.findById(serviceId).orElseThrow(); var f = p.toForm();
            f.setBasePrice(new java.math.BigDecimal("999")); f.setDurationMinutes(90); p.updateCatalog(f); return null; });
        var after = as("customer@petcare.vn", () -> service.myDetail(id));
        assertThat(after.price()).isEqualByComparingTo(before.price()); assertThat(after.endTime()).isEqualTo(before.endTime());
        assertThat(getPage("/appointments", "customer@petcare.vn", "CUSTOMER")).contains("href=\"/appointments/" + id + "\"");
        assertThat(getPage("/staff/appointments?date=2026-01-05", "staff@petcare.vn", "STAFF"))
            .contains("/" + id + "/start", "/" + id + "/cancel").doesNotContain("/" + id + "/finish");
        staffAction(id, "finish").andExpect(flash().attributeExists("error"));
        staffAction(id, "start").andExpect(redirectedUrl("/staff/appointments/" + id));
        assertThat(getPage("/staff/appointments/" + id, "staff@petcare.vn", "STAFF")).contains("/" + id + "/finish")
            .doesNotContain("/" + id + "/start", "/" + id + "/cancel");
        staffAction(id, "finish").andExpect(redirectedUrl("/staff/appointments/" + id));
        assertThat(appointments.findById(id).orElseThrow().getStateName()).isEqualTo(AppointmentStatus.DONE);
        assertThat(getPage("/appointments/" + id, "customer@petcare.vn", "CUSTOMER")).contains("Hoàn tất").doesNotContain("/" + id + "/cancel");
        assertThat(db(() -> entityManager.createNativeQuery("select state_name from appointments where id=:id").setParameter("id", id).getSingleResult())).isEqualTo("DONE");
    }
    @Test void overlapBoundarySameStaffDifferentStaffAndCancellationReleaseSlot() throws Exception {
        Long id = booked(staffId, "2026-01-05T10:00");
        book(staffId, vetId, "2026-01-05T10:15").andExpect(flash().attributeExists("error"));
        booked(otherStaffId, "2026-01-05T10:00");
        booked(staffId, "2026-01-05T09:30"); booked(staffId, "2026-01-05T10:30");
        var available = as("customer@petcare.vn", () -> service.availableSlots(serviceId, LocalDate.of(2026, 1, 5)));
        assertThat(available.slots()).noneMatch(s -> s.staffId().equals(staffId) && s.startTime().equals(LocalDateTime.parse("2026-01-05T10:00")));
        mvc.perform(post("/appointments/" + id + "/cancel").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf()))
            .andExpect(redirectedUrl("/appointments/" + id));
        assertThat(appointments.findById(id).orElseThrow().getStateName()).isEqualTo(AppointmentStatus.CANCELLED);
        var freed = as("customer@petcare.vn", () -> service.availableSlots(serviceId, LocalDate.of(2026, 1, 5)));
        assertThat(freed.slots()).anyMatch(s -> s.staffId().equals(staffId) && s.startTime().equals(LocalDateTime.parse("2026-01-05T10:00")));
        booked(staffId, "2026-01-05T10:00");
    }
    @Test void cancellationCutoffAndOwnershipAreEnforcedAtPostBoundary() throws Exception {
        Long id = booked(staffId, "2026-01-05T09:59");
        assertThat(getPage("/appointments/" + id, "customer@petcare.vn", "CUSTOMER")).doesNotContain("/" + id + "/cancel");
        var result = mvc.perform(post("/appointments/" + id + "/cancel").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf()))
            .andExpect(redirectedUrl("/appointments")).andExpect(flash().attributeExists("error")).andReturn();
        String html = mvc.perform(get("/appointments").with(user("customer@petcare.vn").roles("CUSTOMER")).flashAttrs(result.getFlashMap()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("ít nhất 2 tiếng", "alert-danger");
        mvc.perform(get("/appointments/" + id).with(user("other-appointment@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/appointments/" + id + "/cancel").with(user("other-appointment@petcare.vn").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
        assertThat(getPage("/appointments", "other-appointment@petcare.vn", "CUSTOMER")).doesNotContain("href=\"/appointments/" + id + "\"");
        mvc.perform(get("/staff/appointments/" + id).with(user("other-staff@petcare.vn").roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(post("/staff/appointments/" + id + "/start").with(user("other-staff@petcare.vn").roles("STAFF")).with(csrf())).andExpect(status().isForbidden());
        assertThat(getPage("/staff/appointments?date=2026-01-05", "other-staff@petcare.vn", "STAFF")).doesNotContain("href=\"/staff/appointments/" + id + "\"");
        assertThat(getPage("/staff/appointments?date=2026-01-05", "admin@petcare.vn", "ADMIN")).contains("href=\"/staff/appointments/" + id + "\"");
        staffAction(id, "cancel").andExpect(redirectedUrl("/staff/appointments/" + id));
    }
    @ParameterizedTest @ValueSource(strings = {"2026-01-05T07:59", "2026-01-05T17:31", "2026-01-11T10:00", "2026-01-03T10:00"})
    void cannotPostInvalidWorkingTime(String start) throws Exception {
        book(staffId, serviceId, start).andExpect(redirectedUrl("/appointments/services")).andExpect(flash().attributeExists("error"));
        assertThat(appointments.count()).isZero();
    }
    @Test void adminCanManageVetAppointmentAssignedToAnotherStaffWithCsrf() throws Exception {
        var response = book(otherStaffId, vetId, "2026-01-05T14:00").andExpect(flash().attributeExists("success"))
            .andReturn().getResponse();
        Long id = Long.valueOf(response.getRedirectedUrl().substring("/appointments/".length()));
        mvc.perform(post("/staff/appointments/" + id + "/start").with(user("admin@petcare.vn").roles("ADMIN")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/staff/appointments/" + id + "/start").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/staff/appointments/" + id));
        mvc.perform(post("/staff/appointments/" + id + "/finish").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/staff/appointments/" + id));
        assertThat(appointments.findById(id).orElseThrow().getStateName()).isEqualTo(AppointmentStatus.DONE);
    }
    @Test void validationHiddenWrongServiceAndCsrf() throws Exception {
        mvc.perform(post("/appointments").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())
            .param("serviceId", serviceId.toString()).param("staffId", staffId.toString()).param("startTime", "2026-01-05T10:00")
            .param("petName", " ").param("petSpecies", "INVALID"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "petName", "petSpecies"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Loài thú cưng không hợp lệ.")));
        book(staffId, product(ProductType.MONTHLY), "2026-01-05T10:00").andExpect(flash().attributeExists("error"));
        db(() -> { products.findById(serviceId).orElseThrow().hide(); return null; });
        book(staffId, serviceId, "2026-01-05T10:00").andExpect(flash().attributeExists("error"));
        mvc.perform(post("/appointments").with(user("customer@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/appointments/slots").with(user("customer@petcare.vn").roles("CUSTOMER")))
            .andExpect(redirectedUrl("/appointments/services")).andExpect(flash().attributeExists("error"));
        mvc.perform(get("/staff/appointments").with(user("customer@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        assertThat(appointments.count()).isZero();
    }
    @Test void concurrentBookingAllowsOnlyOneCustomerForTheSameStaffAndTime() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of("customer@petcare.vn", "other-appointment@petcare.vn").stream().map(email -> executor.submit(() -> {
                start.await();
                try { as(email, () -> service.book(form(staffId, "2026-01-05T10:00"))); return true; }
                catch (AppointmentException ex) { return false; }
            })).toList();
            start.countDown();
            assertThat(List.of(futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        assertThat(appointments.count()).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void navigationAndRoutesMatchRoles(String role) throws Exception {
        var request = get("/"); if (!role.equals("GUEST")) request.with(user("viewer").roles(role));
        String html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String section : new String[]{html.substring(html.indexOf("<header"), html.indexOf("</header>")),
            html.substring(html.indexOf("<main"), html.indexOf("</main>"))}) {
            assertThat(section).contains("href=\"/appointments/services\"");
            assertThat(section.contains("href=\"/appointments\"")).isEqualTo(role.equals("CUSTOMER"));
            assertThat(section.contains("href=\"/staff/appointments\"")).isEqualTo(role.equals("STAFF") || role.equals("ADMIN"));
        }
        if (role.equals("GUEST")) mvc.perform(get("/appointments")).andExpect(status().is3xxRedirection());
        if (role.equals("STAFF") || role.equals("ADMIN")) mvc.perform(get("/appointments").with(user("viewer").roles(role))).andExpect(status().isForbidden());
    }
}
