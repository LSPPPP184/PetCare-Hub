package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.vaccination.*;
import com.petcare.event.VaccinationStatusChangedEvent;
import com.petcare.repository.*;
import com.petcare.scheduler.VaccinationScheduler;
import com.petcare.service.OwnedPetService;
import com.petcare.service.VaccinationService;
import com.petcare.domain.order.InvalidStateTransitionException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.petcare.VaccinationStateTest.TODAY;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pet_health_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
    "petcare.vaccinations.scheduler.enabled=true", "petcare.vaccinations.scheduler.cron=-",
    "petcare.vaccinations.annual-booster-years=2"})
@ActiveProfiles("test")
@Import(PetHealthPersistenceTest.TimeConfig.class)
@RecordApplicationEvents
@ExtendWith(OutputCaptureExtension.class)
class PetHealthPersistenceTest {
    @TestConfiguration
    static class TimeConfig {
        @Bean @Primary Clock testClock() {
            return Clock.fixed(Instant.parse("2026-11-12T18:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
    @Autowired OwnedPetService service;
    @Autowired VaccinationService vaccinationService;
    @Autowired Clock clock;
    @Autowired OwnedPetRepository pets;
    @MockitoSpyBean VaccinationRecordRepository vaccinations;
    @Autowired HealthRecordRepository health;
    @Autowired UserRepository users;
    @Autowired VaccinationScheduler scheduler;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;
    @Autowired ApplicationEvents events;

    private <T> T db(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
    private OwnedPet newPet(Species species, LocalDate birth) {
        return OwnedPet.create(users.findByEmail("customer@petcare.vn").orElseThrow(),
            "Milo", species, "Giống mẫu", birth, "Đực", "Ghi chú", TODAY);
    }
    private VaccinationRecord schedule(OwnedPet pet, int offset) {
        return VaccinationRecord.schedule(pet, "Vaccine kiểm thử", TODAY.plusDays(offset), "Ghi chú", TODAY.minusMonths(1), 7);
    }
    private void signIn(String email) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(email, "", List.of()));
        SecurityContextHolder.setContext(context);
    }
    @BeforeEach void setup() {
        db(() -> { vaccinations.deleteAll(); health.deleteAll(); pets.deleteAll(); return null; });
        signIn("customer@petcare.vn");
        events.clear();
    }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void registrationAutomaticallyPersistsTemplateAndHealthRecords() {
        var pet = service.register(newPet(Species.DOG, TODAY.minusWeeks(6).plusDays(7)));
        assertThat(pet.getId()).isNotNull();
        db(() -> {
            var records = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(pet.getId());
            assertThat(records).hasSize(5);
            assertThat(records.getFirst().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
            assertThat(records.getFirst().getScheduledDate()).isEqualTo(TODAY.plusDays(7));
            var saved = health.saveAndFlush(new HealthRecord(pets.findById(pet.getId()).orElseThrow(), TODAY,
                "Khám", "Khỏe mạnh", new BigDecimal("4.25"), "Bác sĩ A"));
            em.clear();
            var restored = health.findById(saved.getId()).orElseThrow();
            assertThat(restored.getOwnedPet().getOwner().getEmail()).isEqualTo("customer@petcare.vn");
            assertThat(restored.getDate()).isEqualTo(TODAY);
            assertThat(restored.getWeight()).isEqualByComparingTo("4.25");
            assertThat(restored.getVetName()).isEqualTo("Bác sĩ A");
            return null;
        });
        var event = events.stream(VaccinationStatusChangedEvent.class).toList();
        assertThat(event).hasSize(1);
        assertThat(event.getFirst().getRecordId()).isNotNull();
        assertThatThrownBy(() -> service.register(pet)).isInstanceOf(IllegalArgumentException.class);
        assertThat(vaccinations.count()).isEqualTo(5);
    }

    @Test void registrationRequiresAuthentication() {
        var pet = newPet(Species.CAT, TODAY);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.register(pet)).isInstanceOf(AccessDeniedException.class);
        assertThat(pets.count()).isZero();
        assertThat(vaccinations.count()).isZero();
    }

    @Test void customerCannotRegisterAnotherCustomersPet() {
        var owner = users.findByEmail("customer@petcare.vn").orElseThrow();
        var other = users.findByEmail("health.other@petcare.vn").orElseGet(() -> users.saveAndFlush(
            new User("Khách B", "health.other@petcare.vn", owner.getPassword(), "", Role.CUSTOMER)));
        var pet = OwnedPet.create(other, "Milo của B", Species.CAT, "", TODAY, "", "", LocalDate.now(clock));
        assertThatThrownBy(() -> service.register(pet)).isInstanceOf(AccessDeniedException.class);
        db(() -> {
            assertThat(pets.count()).isZero();
            assertThat(vaccinations.count()).isZero();
            return null;
        });
    }

    @Test void registrationRollsBackWhenSavingVaccinationsFailsWithoutOuterTransaction() {
        var partialWriteObserved = new AtomicBoolean();
        doAnswer(call -> {
            Iterable<VaccinationRecord> batch = call.getArgument(0);
            vaccinations.saveAndFlush(batch.iterator().next());
            assertThat(pets.count()).isEqualTo(1);
            assertThat(vaccinations.count()).isEqualTo(1);
            partialWriteObserved.set(true);
            throw new IllegalStateException("Lỗi lưu lịch tiêm giả lập.");
        }).when(vaccinations).saveAll(any());

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThatThrownBy(() -> service.register(newPet(Species.CAT, TODAY)))
            .isInstanceOf(IllegalStateException.class).hasMessage("Lỗi lưu lịch tiêm giả lập.");
        assertThat(partialWriteObserved).isTrue();
        db(() -> {
            assertThat(pets.count()).isZero();
            assertThat(vaccinations.count()).isZero();
            return null;
        });
    }

    @Test void schedulerPersistsStateNamesPublishesOnceAndLeavesCompletedUntouched(CapturedOutput output) {
        var ids = db(() -> {
            var pet = pets.save(newPet(Species.CAT, TODAY.minusYears(1)));
            var completed = schedule(pet, 7);
            completed.complete(TODAY, TODAY);
            return vaccinations.saveAll(List.of(schedule(pet, 7), schedule(pet, -1), schedule(pet, 8), completed))
                .stream().map(VaccinationRecord::getId).toList();
        });
        events.clear();
        scheduler.checkSchedules();
        var changes = events.stream(VaccinationStatusChangedEvent.class).toList();
        assertThat(changes).extracting(VaccinationStatusChangedEvent::getCurrentState)
            .containsExactlyInAnyOrder(VaccinationStatus.DUE_SOON, VaccinationStatus.OVERDUE);
        assertThat(changes).allSatisfy(event -> {
            assertThat(event.getPreviousState()).isEqualTo(VaccinationStatus.SCHEDULED);
            assertThat(event.getObservedOn()).isEqualTo(TODAY);
        });
        assertThat(output).contains("Sắp đến hạn", "Quá hạn", "Lịch tiêm #");
        scheduler.checkSchedules();
        assertThat(events.stream(VaccinationStatusChangedEvent.class)).hasSize(2);
        db(() -> {
            assertThat(vaccinations.findByStateNameNot(VaccinationStatus.COMPLETED)).hasSize(3);
            assertThat(vaccinations.findById(ids.get(0)).orElseThrow().getState()).isInstanceOf(DueSoonState.class);
            assertThat(vaccinations.findById(ids.get(1)).orElseThrow().getState()).isInstanceOf(OverdueState.class);
            var completed = vaccinations.findById(ids.get(3)).orElseThrow();
            assertThat(completed.getState()).isInstanceOf(CompletedState.class);
            assertThat(completed.getAdministeredDate()).isEqualTo(TODAY);
            assertThat(completed.getVersion()).isZero();
            assertThat(em.createNativeQuery("select state_name from vaccination_records where id = :id")
                .setParameter("id", ids.getFirst()).getSingleResult()).isEqualTo("DUE_SOON");
            return null;
        });
    }

    @Test void reschedulePublishesOnlyFinalChangedStatusAndCompletionStaysFinal() {
        Long id = db(() -> {
            var pet = pets.save(newPet(Species.CAT, TODAY.minusYears(1)));
            return vaccinations.save(schedule(pet, 8)).getId();
        });
        db(() -> {
            var record = vaccinations.findById(id).orElseThrow();
            record.reschedule(TODAY.plusDays(7), TODAY, 7);
            vaccinations.save(record);
            record.reschedule(TODAY.plusDays(6), TODAY, 7);
            vaccinations.save(record);
            record.reschedule(TODAY.minusDays(1), TODAY, 7);
            vaccinations.save(record);
            record.complete(TODAY, TODAY);
            vaccinations.save(record);
            return null;
        });
        var changes = events.stream(VaccinationStatusChangedEvent.class).toList();
        assertThat(changes).hasSize(2);
        assertThat(changes.getFirst().getCurrentState()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(changes.getFirst().getScheduledDate()).isEqualTo(TODAY.plusDays(7));
        assertThat(changes.getLast().getPreviousState()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(changes.getLast().getCurrentState()).isEqualTo(VaccinationStatus.OVERDUE);
        var restored = vaccinations.findById(id).orElseThrow();
        assertThat(restored.getStateName()).isEqualTo(VaccinationStatus.COMPLETED);
        assertThat(restored.getAdministeredDate()).isEqualTo(TODAY);
    }

    @Test void rollbackDoesNotLogUncommittedStatus(CapturedOutput output) {
        Long id = db(() -> {
            var pet = pets.save(newPet(Species.CAT, TODAY.minusYears(1)));
            return vaccinations.save(schedule(pet, 7)).getId();
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            scheduler.checkSchedules();
            em.flush();
            status.setRollbackOnly();
        });
        assertThat(vaccinations.findById(id).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        assertThat(output).doesNotContain("Lịch tiêm #");
    }

    @Test void registeringFiveYearOldPetPersistsLatestDueAndFutureBoosters() {
        LocalDate today = LocalDate.now(clock);
        var pet = service.register(newPet(Species.CAT, today.minusYears(5)));
        var records = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(pet.getId());
        assertThat(records).hasSize(3);
        assertThat(records).filteredOn(record -> record.getScheduledDate().isBefore(today)).hasSize(1);
        assertThat(records).filteredOn(record -> record.getScheduledDate().isAfter(today)).hasSize(2);
        assertThat(records).allSatisfy(record -> assertThat(record.getAnnualDoseYear()).isNotNull());
    }

    private Long saveAnnualBooster(LocalDate birth, int year) {
        return db(() -> {
            var pet = pets.save(newPet(Species.CAT, birth));
            return vaccinations.save(VaccinationRecord.annualBooster(pet, "Vaccine nhắc mẫu", year,
                "Dữ liệu mẫu, không phải tư vấn thú y.", LocalDate.now(clock), 7)).getId();
        });
    }

    @Test void completingAnnualBoosterCreatesNextYearExactlyOnce() {
        Long id = saveAnnualBooster(TODAY.minusYears(5), 4);
        signIn("staff@petcare.vn");
        var completed = vaccinationService.complete(id, LocalDate.now(clock));
        assertThat(completed.getStateName()).isEqualTo(VaccinationStatus.COMPLETED);
        assertThat(completed.getAdministeredDate()).isEqualTo(TODAY);
        db(() -> {
            var records = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(completed.getOwnedPet().getId());
            assertThat(records).hasSize(2);
            assertThat(records.getLast().getScheduledDate()).isEqualTo(LocalDate.of(2027, 2, 5));
            assertThat(records.getLast().getAnnualDoseYear()).isEqualTo(5);
            assertThat(records.getLast().getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
            return null;
        });
        assertThatThrownBy(() -> vaccinationService.complete(id, LocalDate.now(clock)))
            .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(vaccinations.count()).isEqualTo(2);
    }

    @Test void completionKeepsExistingRescheduledSuccessorWithoutDuplicatingIt() {
        var pet = service.register(newPet(Species.DOG, TODAY.minusYears(5)));
        var ids = db(() -> {
            var records = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(pet.getId());
            var next = records.get(1);
            next.reschedule(TODAY.plusDays(3), LocalDate.now(clock), 7);
            vaccinations.save(next);
            return List.of(records.getFirst().getId(), next.getId());
        });
        signIn("staff@petcare.vn");
        vaccinationService.complete(ids.getFirst(), LocalDate.now(clock));
        assertThat(vaccinations.count()).isEqualTo(3);
        var next = vaccinations.findById(ids.getLast()).orElseThrow();
        assertThat(next.getScheduledDate()).isEqualTo(TODAY.plusDays(3));
        assertThat(next.getAnnualDoseYear()).isEqualTo(5);
    }

    @Test void nextAnnualDoseUsesOriginalLeapDayEvenAfterRescheduling() {
        Long id = saveAnnualBooster(LocalDate.of(2023, 12, 7), 3);
        db(() -> {
            var record = vaccinations.findById(id).orElseThrow();
            record.reschedule(TODAY, LocalDate.now(clock), 7);
            vaccinations.save(record);
            return null;
        });
        signIn("staff@petcare.vn");
        var completed = vaccinationService.complete(id, LocalDate.now(clock));
        var records = vaccinations.findByOwnedPetIdOrderByScheduledDateAsc(completed.getOwnedPet().getId());
        assertThat(records).hasSize(2);
        assertThat(records.getLast().getScheduledDate()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(records.getLast().getAnnualDoseYear()).isEqualTo(4);
    }

    @Test void nonAnnualDoseDoesNotGenerateBoosterAndCustomerCannotComplete() {
        Long id = db(() -> {
            var pet = pets.save(newPet(Species.DOG, TODAY.minusYears(1)));
            return vaccinations.save(schedule(pet, 7)).getId();
        });
        assertThatThrownBy(() -> vaccinationService.complete(id, LocalDate.now(clock)))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(vaccinations.findById(id).orElseThrow().getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        signIn("staff@petcare.vn");
        vaccinationService.complete(id, LocalDate.now(clock));
        assertThat(vaccinations.count()).isEqualTo(1);
    }

    @Test void failedSuccessorSaveRollsBackCompletionWithoutOuterTransaction() {
        Long id = saveAnnualBooster(TODAY.minusYears(5), 4);
        signIn("staff@petcare.vn");
        doThrow(new IllegalStateException("Lỗi lưu mũi kế tiếp giả lập.")).when(vaccinations)
            .save(argThat(record -> record != null && Integer.valueOf(5).equals(record.getAnnualDoseYear())));
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThatThrownBy(() -> vaccinationService.complete(id, LocalDate.now(clock)))
            .isInstanceOf(IllegalStateException.class).hasMessage("Lỗi lưu mũi kế tiếp giả lập.");
        db(() -> {
            var record = vaccinations.findById(id).orElseThrow();
            assertThat(record.getStateName()).isEqualTo(VaccinationStatus.OVERDUE);
            assertThat(record.getAdministeredDate()).isNull();
            assertThat(vaccinations.count()).isEqualTo(1);
            return null;
        });
    }

    @Test void concurrentCompletionCreatesOneSuccessor() throws Exception {
        Long id = saveAnnualBooster(TODAY.minusYears(5), 4);
        var start = new CountDownLatch(1);
        Callable<Boolean> complete = () -> {
            signIn("staff@petcare.vn");
            try {
                start.await();
                vaccinationService.complete(id, LocalDate.now(clock));
                return true;
            } catch (InvalidStateTransitionException ex) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(complete);
            var second = executor.submit(complete);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        assertThat(vaccinations.count()).isEqualTo(2);
        assertThat(vaccinations.findAll()).extracting(VaccinationRecord::getAnnualDoseYear).containsExactlyInAnyOrder(4, 5);
    }
}
