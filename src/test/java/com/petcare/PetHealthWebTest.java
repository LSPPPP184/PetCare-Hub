package com.petcare;

import com.petcare.config.MutableClock;
import com.petcare.domain.*;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.List;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:health_web;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Import(PetHealthWebTest.FixedTime.class)
class PetHealthWebTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock testClock() { return Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
    @Autowired MockMvc mvc;
    @Autowired OwnedPetRepository pets;
    @Autowired VaccinationRecordRepository vaccinations;
    @Autowired HealthRecordRepository health;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired org.springframework.context.ApplicationContext context;
    private <T> T db(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }
    @BeforeEach void setup() {
        db(() -> { health.deleteAll(); vaccinations.deleteAll(); pets.deleteAll(); return null; });
        var owner = users.findByEmail("customer@petcare.vn").orElseThrow();
        if (users.findByEmail("other.health@petcare.vn").isEmpty())
            users.save(new User("Khách khác", "other.health@petcare.vn", owner.getPassword(), "", Role.CUSTOMER));
    }
    private String page(String path, String role) throws Exception {
        return mvc.perform(get(path).with(user(role.toLowerCase() + "@petcare.vn").roles(role)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private Long createPet() throws Exception {
        var response = mvc.perform(post("/my-pets").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())
            .param("name", "Milo").param("species", "CAT").param("birthDate", TODAY.minusWeeks(5).toString())
            .param("ownerId", users.findByEmail("other.health@petcare.vn").orElseThrow().getId().toString()))
            .andExpect(flash().attributeExists("success")).andReturn().getResponse();
        return Long.valueOf(response.getRedirectedUrl().substring("/my-pets/".length()));
    }
    private ResultActions edit(Long id, String email, String name, String birth, String version) throws Exception {
        return mvc.perform(post("/my-pets/" + id + "/edit").with(user(email).roles("CUSTOMER")).with(csrf())
            .param("name", name).param("species", "CAT").param("birthDate", birth).param("version", version)
            .param("ownerId", "9999").param("stateName", "COMPLETED"));
    }
    private ResultActions action(Long id, String action, String date, String version, String role) throws Exception {
        String prefix = role.equals("CUSTOMER") ? "/my-pets" : "/staff/health";
        return mvc.perform(post(prefix + "/vaccinations/" + id + "/" + action)
            .with(user(role.toLowerCase() + "@petcare.vn").roles(role)).with(csrf())
            .param("date", date).param("version", version));
    }
    @Test void createEditAndViewJourneyKeepsTemplateAndRejectsStaleForm() throws Exception {
        assertThat(page("/my-pets/new", "CUSTOMER")).contains("name=\"_csrf\"", "Lưu hồ sơ");
        Long id = createPet();
        var original = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(id);
        assertThat(original).hasSize(6);
        assertThat(original.getFirst().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(db(() -> pets.findById(id).orElseThrow().getOwner().getEmail())).isEqualTo("customer@petcare.vn");
        assertThat(page("/my-pets", "CUSTOMER")).contains("href=\"/my-pets/" + id + "\"", "href=\"/my-pets/" + id + "/edit\"");
        assertThat(page("/my-pets/" + id, "CUSTOMER")).contains("Timeline tiêm phòng", "Lịch sử khám", "text-bg-warning", "text-bg-secondary")
            .doesNotContain("/complete", "/records/new");
        assertThat(page("/my-pets/" + id + "/edit", "CUSTOMER")).contains("value=\"Milo\"");
        edit(id, "customer@petcare.vn", "Milo mới", TODAY.minusWeeks(5).toString(), "0")
            .andExpect(redirectedUrl("/my-pets/" + id));
        assertThat(pets.findById(id).orElseThrow().getName()).isEqualTo("Milo mới");
        edit(id, "customer@petcare.vn", "Ghi đè", TODAY.minusWeeks(5).toString(), "0").andExpect(flash().attributeExists("error"));
        edit(id, "customer@petcare.vn", "Đổi ngày sinh", TODAY.toString(), "1").andExpect(flash().attributeExists("error"));
        assertThat(pets.findById(id).orElseThrow().getName()).isEqualTo("Milo mới");
        assertThat(vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(id)).extracting(VaccinationRecord::getId)
            .containsExactlyElementsOf(original.stream().map(VaccinationRecord::getId).toList());
    }
    @Test void ownershipIsCheckedForReadEditAndReschedule() throws Exception {
        Long id = createPet(); Long recordId = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(id).getFirst().getId();
        for (String path : List.of("/my-pets/" + id, "/my-pets/" + id + "/edit"))
            mvc.perform(get(path).with(user("other.health@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        edit(id, "other.health@petcare.vn", "Chiếm hồ sơ", TODAY.minusWeeks(5).toString(), "0").andExpect(status().isForbidden());
        mvc.perform(post("/my-pets/vaccinations/" + recordId + "/reschedule").with(user("other.health@petcare.vn").roles("CUSTOMER"))
            .with(csrf()).param("date", TODAY.toString()).param("version", "0")).andExpect(status().isForbidden());
        String other = mvc.perform(get("/my-pets").with(user("other.health@petcare.vn").roles("CUSTOMER")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(other).doesNotContain("href=\"/my-pets/" + id + "\"");
        assertThat(pets.findById(id).orElseThrow().getName()).isEqualTo("Milo");
    }
    @Test void completeAndRescheduleUseStatePermissionsAndPersistDates() throws Exception {
        Long id = createPet(); var record = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(id).getFirst();
        assertThat(page("/staff/health/pets/" + id, "STAFF")).contains("/" + record.getId() + "/complete", "/" + record.getId() + "/reschedule");
        action(record.getId(), "reschedule", TODAY.plusDays(20).toString(), "0", "CUSTOMER").andExpect(flash().attributeExists("success"));
        action(record.getId(), "reschedule", TODAY.plusDays(3).toString(), "0", "STAFF").andExpect(flash().attributeExists("error"));
        assertThat(vaccinations.findById(record.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        action(record.getId(), "complete", TODAY.plusDays(1).toString(), "1", "STAFF").andExpect(flash().attributeExists("error"));
        action(record.getId(), "complete", TODAY.toString(), "1", "ADMIN").andExpect(redirectedUrl("/staff/health/pets/" + id));
        var completed = vaccinations.findById(record.getId()).orElseThrow();
        assertThat(completed.getAdministeredDate()).isEqualTo(TODAY);
        assertThat(completed.getStateName()).isEqualTo(VaccinationStatus.COMPLETED);
        assertThat(page("/staff/health/pets/" + id, "STAFF")).contains("text-bg-success")
            .doesNotContain("/" + record.getId() + "/complete", "/" + record.getId() + "/reschedule");
        assertThat(page("/my-pets/" + id, "CUSTOMER")).doesNotContain("/" + record.getId() + "/reschedule");
        action(record.getId(), "reschedule", TODAY.plusDays(1).toString(), Long.toString(completed.getVersion()), "CUSTOMER")
            .andExpect(flash().attributeExists("error"));
        action(record.getId(), "complete", TODAY.toString(), "2", "STAFF").andExpect(flash().attributeExists("error"));
    }
    @Test void staffAndAdminCanAddHistoryWithDateValidationAndCustomerCannot() throws Exception {
        Long id = createPet(); String path = "/staff/health/pets/" + id + "/records";
        assertThat(page("/staff/health/pets", "STAFF")).contains(path + "/new", "/staff/health/pets/" + id);
        assertThat(page(path + "/new", "STAFF")).contains("name=\"_csrf\"", "Lưu bản ghi khám");
        for (String date : List.of(TODAY.minusYears(1).toString(), TODAY.plusDays(1).toString()))
            mvc.perform(post(path).with(user("staff@petcare.vn").roles("STAFF")).with(csrf())
                .param("date", date).param("type", "Khám").param("description", "Khỏe"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post(path).with(user("staff@petcare.vn").roles("STAFF")).with(csrf())
            .param("date", TODAY.toString()).param("type", "").param("weight", "-1"))
            .andExpect(model().attributeHasFieldErrors("form", "type", "description", "weight"));
        for (String role : List.of("STAFF", "ADMIN"))
            mvc.perform(post(path).with(user(role.toLowerCase() + "@petcare.vn").roles(role)).with(csrf())
                .param("date", TODAY.toString()).param("type", "Khám tổng quát").param("description", "Kết quả an toàn <script>")
                .param("weight", "2.50").param("vetName", "Bác sĩ An"))
                .andExpect(redirectedUrl("/staff/health/pets/" + id));
        assertThat(health.count()).isEqualTo(2);
        assertThat(page("/my-pets/" + id, "CUSTOMER")).contains("Bác sĩ An", "Kết quả an toàn &lt;script&gt;", "2.50");
        mvc.perform(post(path).with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void filtersCoverAllStorePetsAndAllStatuses() throws Exception {
        Long id = createPet();
        db(() -> {
            var pet = pets.findById(id).orElseThrow();
            vaccinations.save(VaccinationRecord.schedule(pet, "Mũi quá hạn", TODAY.minusDays(1), "", TODAY, 7));
            var done = VaccinationRecord.schedule(pet, "Mũi hoàn tất", TODAY, "", TODAY, 7); done.complete(TODAY, TODAY); vaccinations.save(done);
            var other = pets.save(OwnedPet.create(users.findByEmail("other.health@petcare.vn").orElseThrow(), "Thú khách khác", Species.DOG, "", TODAY.minusYears(5), "", "", TODAY));
            vaccinations.save(VaccinationRecord.schedule(other, "Mũi khách khác", TODAY, "", TODAY, 7));
            return null;
        });
        assertThat(page("/staff/health", "STAFF")).contains("Mũi quá hạn", "text-bg-danger", "Mũi khách khác").doesNotContain("Mũi hoàn tất");
        assertThat(page("/staff/health?status=OVERDUE", "STAFF")).contains("Mũi quá hạn").doesNotContain("Mũi khách khác", "Mũi hoàn tất");
        assertThat(page("/staff/health?status=DUE_SOON", "ADMIN")).contains("Mũi khách khác").doesNotContain("Mũi quá hạn");
        assertThat(page("/staff/health?status=COMPLETED", "STAFF")).contains("Mũi hoàn tất").doesNotContain("Mũi quá hạn", "/complete", "/reschedule");
        assertThat(page("/staff/health?status=SCHEDULED", "STAFF")).contains("text-bg-secondary").doesNotContain("Mũi quá hạn", "Mũi khách khác");
    }
    @Test void invalidInputCsrfAndNonDemoIsolation() throws Exception {
        mvc.perform(post("/my-pets").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())
            .param("name", "").param("species", "INVALID").param("birthDate", "bad"))
            .andExpect(model().attributeHasFieldErrors("form", "name", "species", "birthDate"));
        mvc.perform(post("/my-pets").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())
            .param("name", "Milo").param("species", "CAT").param("birthDate", TODAY.plusDays(1).toString()))
            .andExpect(flash().attributeExists("error"));
        assertThat(pets.count()).isZero();
        Long id = createPet(); var record = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(id).getFirst();
        action(record.getId(), "reschedule", TODAY.minusDays(1).toString(), "0", "CUSTOMER").andExpect(flash().attributeExists("error"));
        action(record.getId(), "reschedule", "invalid", "0", "CUSTOMER").andExpect(flash().attributeExists("error"));
        mvc.perform(post("/my-pets").with(user("customer@petcare.vn").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/staff/health/vaccinations/" + record.getId() + "/complete")
            .with(user("admin@petcare.vn").roles("ADMIN")).param("date", TODAY.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/staff/health/vaccinations/" + record.getId() + "/complete")
            .with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf()).param("date", TODAY.toString())).andExpect(status().isForbidden());
        assertThat(context.getBeansOfType(MutableClock.class)).isEmpty();
        mvc.perform(get("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(post("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()).param("date", TODAY.toString())).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void headerHomeAndRouteAccessMatchEachRole(String role) throws Exception {
        var request = get("/"); if (!role.equals("GUEST")) request.with(user("viewer").roles(role));
        String html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String section : List.of(html.substring(html.indexOf("<header"), html.indexOf("</header>")), html.substring(html.indexOf("<main"), html.indexOf("</main>")))) {
            for (String path : List.of("/my-pets", "/my-pets/new"))
                assertThat(section.contains("href=\"" + path + "\"")).isEqualTo(role.equals("CUSTOMER"));
            for (String path : List.of("/staff/health", "/staff/health/pets", "/staff/health?status=DUE_SOON", "/staff/health?status=OVERDUE"))
                assertThat(section.contains("href=\"" + path + "\"")).isEqualTo(role.equals("STAFF") || role.equals("ADMIN"));
            assertThat(section).doesNotContain("href=\"/admin/demo/health\"");
        }
        if (role.equals("GUEST")) {
            for (String path : List.of("/my-pets", "/my-pets/new", "/staff/health", "/staff/health/pets", "/admin/demo/health"))
                mvc.perform(get(path)).andExpect(status().is3xxRedirection());
        } else if (role.equals("CUSTOMER")) {
            for (String path : List.of("/staff/health", "/staff/health/pets", "/admin/demo/health"))
                mvc.perform(get(path).with(user("customer@petcare.vn").roles(role))).andExpect(status().isForbidden());
        } else {
            for (String path : List.of("/my-pets", "/my-pets/new"))
                mvc.perform(get(path).with(user("viewer").roles(role))).andExpect(status().isForbidden());
        }
    }
}
