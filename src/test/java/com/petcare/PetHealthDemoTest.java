package com.petcare;

import com.petcare.config.*;
import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.repository.*;
import com.petcare.service.AppointmentPolicy;
import com.petcare.domain.appointment.AppointmentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.List;
import java.util.Comparator;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:health_demo;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
    "petcare.vaccinations.scheduler.enabled=true", "petcare.vaccinations.scheduler.cron=-"})
@AutoConfigureMockMvc @ActiveProfiles({"test", "demo"}) @Import(PetHealthDemoTest.FixedTime.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PetHealthDemoTest {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final Instant START = Instant.parse("2026-11-13T03:00:00Z");
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(START, ZONE); }
    }
    @Autowired MockMvc mvc;
    @Autowired @Qualifier("healthClock") MutableClock clock;
    @Autowired Clock applicationClock;
    @Autowired AppointmentPolicy appointments;
    @Autowired PetHealthDemoSeeder seeder;
    @Autowired OwnedPetRepository pets;
    @Autowired UserRepository users;
    @MockitoSpyBean VaccinationRecordRepository vaccinations;
    @Autowired HealthRecordRepository health;
    @Autowired PlatformTransactionManager transactions;

    @Test void seededProfilesAndAdminDateSimulationPersistBoundaryTransitions() throws Exception {
        assertThat(applicationClock).isNotSameAs(clock);
        assertThat(applicationClock.instant()).isEqualTo(START);
        var owner = users.findByEmail("customer@petcare.vn").orElseThrow();
        var owned = pets.findByOwnerIdOrderByIdAsc(owner.getId());
        assertThat(owned).hasSize(2);
        var today = LocalDate.now(clock);
        assertThat(owned).anySatisfy(pet -> assertThat(pet.getBirthDate()).isAfter(today.minusMonths(2)));
        assertThat(owned).anySatisfy(pet -> assertThat(pet.getBirthDate()).isBefore(today.minusYears(4)));
        var records = vaccinations.findAll();
        assertThat(records).extracting(VaccinationRecord::getStateName).contains(VaccinationStatus.values());
        assertThat(health.count()).isEqualTo(2);
        long count = vaccinations.count(); seeder.seed();
        assertThat(pets.count()).isEqualTo(2); assertThat(vaccinations.count()).isEqualTo(count); assertThat(health.count()).isEqualTo(2);
        var scheduled = records.stream().filter(r -> r.getStateName() == VaccinationStatus.SCHEDULED)
            .min(java.util.Comparator.comparing(VaccinationRecord::getScheduledDate)).orElseThrow();
        var completed = records.stream().filter(r -> r.getStateName() == VaccinationStatus.COMPLETED).findFirst().orElseThrow();
        var boundary = scheduled.getScheduledDate().minusDays(7);
        simulate(boundary.minusDays(1));
        assertThat(vaccinations.findById(scheduled.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        simulate(boundary);
        assertThat(vaccinations.findById(scheduled.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        String html = mvc.perform(get("/staff/health?status=DUE_SOON").with(user("admin@petcare.vn").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("text-bg-warning", "/" + scheduled.getId() + "/complete");
        simulate(scheduled.getScheduledDate());
        assertThat(vaccinations.findById(scheduled.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        simulate(scheduled.getScheduledDate().plusDays(1));
        assertThat(vaccinations.findById(scheduled.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(vaccinations.findById(completed.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.COMPLETED);
        assertThat(vaccinations.findById(completed.getId()).orElseThrow().getAdministeredDate()).isEqualTo(completed.getAdministeredDate());
        var current = LocalDate.now(clock);
        mvc.perform(post("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()).param("date", today.toString()))
            .andExpect(flash().attributeExists("error"));
        assertThat(LocalDate.now(clock)).isEqualTo(current);
        // Bản ghi đã lưu vẫn giữ trạng thái khi đọc trong giao dịch mới.
        new TransactionTemplate(transactions).executeWithoutResult(s ->
            assertThat(vaccinations.findById(scheduled.getId()).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.OVERDUE));
    }

    @Test void selectingTodayKeepsRealTimeAndDoesNotChangeAppointmentRules() throws Exception {
        assertThat(clock.instant()).isEqualTo(START);
        assertAppointmentRulesUnchanged();
        simulate(LocalDate.of(2026, 11, 13));
        assertThat(clock.instant()).isEqualTo(START);
        assertAppointmentRulesUnchanged();
        simulate(LocalDate.of(2026, 12, 1));
        assertThat(clock.instant()).isEqualTo(LocalDate.of(2026, 12, 1).atStartOfDay(ZONE).toInstant());
        assertAppointmentRulesUnchanged();
        simulate(LocalDate.of(2026, 12, 1));
        assertAppointmentRulesUnchanged();
    }

    private void assertAppointmentRulesUnchanged() {
        assertThat(appointments.now()).isEqualTo(LocalDateTime.of(2026, 11, 13, 10, 0));
        assertThat(appointments.canCustomerCancel(LocalDateTime.of(2026, 11, 13, 11, 59))).isFalse();
        assertThat(appointments.canCustomerCancel(LocalDateTime.of(2026, 11, 13, 12, 0))).isTrue();
        assertThatThrownBy(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 13, 9, 0), 30))
            .isInstanceOf(AppointmentException.class).hasMessageContaining("quá khứ");
        assertThatCode(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 13, 10, 0), 30)).doesNotThrowAnyException();
        assertThatCode(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 14, 8, 0), 30)).doesNotThrowAnyException();
        assertThatCode(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 14, 17, 30), 30)).doesNotThrowAnyException();
        assertThatThrownBy(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 14, 7, 59), 30))
            .isInstanceOf(AppointmentException.class).hasMessageContaining("giờ làm việc");
        assertThatThrownBy(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 14, 17, 31), 30))
            .isInstanceOf(AppointmentException.class).hasMessageContaining("giờ làm việc");
        assertThatThrownBy(() -> appointments.validateBooking(LocalDateTime.of(2026, 11, 15, 10, 0), 30))
            .isInstanceOf(AppointmentException.class).hasMessageContaining("giờ làm việc");
    }

    @ParameterizedTest @ValueSource(strings = {"database", "version"})
    void schedulerFailureRestoresClockAndRollsBackAlreadyFlushedRecords(String failure) throws Exception {
        var before = snapshot();
        var previous = clock.instant();
        var target = LocalDate.of(2026, 12, 1);
        doAnswer(call -> {
            VaccinationRecord record = call.getArgument(0);
            assertThat(LocalDate.now(clock)).isEqualTo(target);
            vaccinations.saveAndFlush(record);
            if (failure.equals("version")) throw new OptimisticLockingFailureException("Xung đột version giả lập");
            throw new DataAccessResourceFailureException("Lỗi database giả lập");
        }).when(vaccinations).save(any(VaccinationRecord.class));

        mvc.perform(post("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf())
                .param("date", target.toString()))
            .andExpect(redirectedUrl("/admin/demo/health"))
            .andExpect(flash().attribute("error", "Không thể giả lập ngày. Đã giữ nguyên Clock và lịch tiêm; vui lòng thử lại."));

        verify(vaccinations).saveAndFlush(any(VaccinationRecord.class));
        assertThat(clock.instant()).isEqualTo(previous);
        assertThat(snapshot()).isEqualTo(before);
        assertAppointmentRulesUnchanged();
        // Thử lại cùng ngày sau lỗi phải thành công, không còn ngày tạm hoặc transaction hỏng.
        reset(vaccinations);
        simulate(target);
        assertThat(snapshot()).isNotEqualTo(before);
    }

    private record VaccinationSnapshot(Long id, long version, VaccinationStatus status,
                                       LocalDate scheduled, LocalDate administered) {}
    private List<VaccinationSnapshot> snapshot() {
        return new TransactionTemplate(transactions).execute(s -> vaccinations.findAll().stream()
            .sorted(Comparator.comparing(VaccinationRecord::getId))
            .map(r -> new VaccinationSnapshot(r.getId(), r.getVersion(), r.getStateName(), r.getScheduledDate(), r.getAdministeredDate()))
            .toList());
    }

    @Test void renamingSeededPetsDoesNotDuplicateAnyHealthData() throws Exception {
        var owner = users.findByEmail("customer@petcare.vn").orElseThrow();
        var before = pets.findByOwnerIdOrderByIdAsc(owner.getId());
        var petIds = before.stream().map(com.petcare.domain.OwnedPet::getId).toList();
        var keys = before.stream().map(com.petcare.domain.OwnedPet::getSeedKey).toList();
        var records = snapshot(); long historyCount = health.count();
        for (var pet : before) {
            mvc.perform(post("/my-pets/" + pet.getId() + "/edit").with(user("customer@petcare.vn").roles("CUSTOMER")).with(csrf())
                    .param("name", "Tên mới " + pet.getId()).param("species", pet.getSpecies().name())
                    .param("birthDate", pet.getBirthDate().toString()).param("version", Long.toString(pet.getVersion()))
                    .param("seedKey", "khóa giả mạo"))
                .andExpect(flash().attributeExists("success"));
        }
        seeder.seed();
        var after = pets.findByOwnerIdOrderByIdAsc(owner.getId());
        assertThat(after).extracting(com.petcare.domain.OwnedPet::getId).containsExactlyElementsOf(petIds);
        assertThat(after).extracting(com.petcare.domain.OwnedPet::getSeedKey).containsExactlyElementsOf(keys);
        assertThat(after).allSatisfy(p -> assertThat(p.getName()).isEqualTo("Tên mới " + p.getId()));
        assertThat(snapshot()).isEqualTo(records);
        assertThat(health.count()).isEqualTo(historyCount);
    }

    @Test void upgradesRenamedLegacyDemoPetsWithoutCreatingNewRecords() {
        var before = snapshot(); long petCount = pets.count(); long historyCount = health.count();
        new TransactionTemplate(transactions).executeWithoutResult(s -> {
            for (var pet : pets.findAll()) pet.updateDetails("Tên đã sửa", pet.getBreed(), pet.getGender(), pet.getNote());
        });
        // Mô phỏng dữ liệu trước khi có cột seed_key, kể cả tên đã đổi.
        new TransactionTemplate(transactions).executeWithoutResult(s ->
            entityManager.createNativeQuery("update owned_pets set seed_key = null").executeUpdate());
        seeder.seed();
        assertThat(pets.count()).isEqualTo(petCount);
        assertThat(pets.findAll()).extracting(com.petcare.domain.OwnedPet::getSeedKey)
            .containsExactlyInAnyOrder("health-demo-milo", "health-demo-lucky");
        assertThat(pets.findAll()).allSatisfy(p -> assertThat(p.getName()).isEqualTo("Tên đã sửa"));
        assertThat(snapshot()).isEqualTo(before);
        assertThat(health.count()).isEqualTo(historyCount);
    }
    @Autowired jakarta.persistence.EntityManager entityManager;
    private void simulate(LocalDate date) throws Exception {
        mvc.perform(post("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN")).with(csrf()).param("date", date.toString()))
            .andExpect(redirectedUrl("/admin/demo/health")).andExpect(flash().attributeExists("success"));
        assertThat(LocalDate.now(clock)).isEqualTo(date);
        String page = mvc.perform(get("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(page).contains(date.toString(), "name=\"_csrf\"", "href=\"/staff/health\"", "href=\"/staff/health/pets\"");
    }
    @ParameterizedTest @ValueSource(strings = {"GUEST", "CUSTOMER", "STAFF", "ADMIN"})
    void demoNavigationAndEndpointsOnlyForAdmin(String role) throws Exception {
        var request = get("/"); if (!role.equals("GUEST")) request.with(user(role.toLowerCase() + "@petcare.vn").roles(role));
        String html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String section : List.of(html.substring(html.indexOf("<header"), html.indexOf("</header>")), html.substring(html.indexOf("<main"), html.indexOf("</main>"))))
            assertThat(section.contains("href=\"/admin/demo/health\"")).isEqualTo(role.equals("ADMIN"));
        var read = get("/admin/demo/health"); var write = post("/admin/demo/health").with(csrf()).param("date", LocalDate.now(clock).toString());
        if (!role.equals("GUEST")) { read.with(user(role.toLowerCase() + "@petcare.vn").roles(role)); write.with(user(role.toLowerCase() + "@petcare.vn").roles(role)); }
        if (role.equals("GUEST")) {
            mvc.perform(read).andExpect(status().is3xxRedirection()); mvc.perform(write).andExpect(status().is3xxRedirection());
        } else if (!role.equals("ADMIN")) {
            mvc.perform(read).andExpect(status().isForbidden()); mvc.perform(write).andExpect(status().isForbidden());
        } else {
            mvc.perform(read).andExpect(status().isOk());
            mvc.perform(post("/admin/demo/health").with(user("admin@petcare.vn").roles("ADMIN")).param("date", LocalDate.now(clock).toString()))
                .andExpect(status().isForbidden());
        }
    }
}
