package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.order.InvalidStateTransitionException;
import com.petcare.domain.vaccination.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class VaccinationStateTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    static OwnedPet pet(Species species, LocalDate birth) {
        return OwnedPet.create(new User("Khách", "customer@test.vn", "hash", "", Role.CUSTOMER),
            "Milo", species, "Giống mẫu", birth, "Đực", "", TODAY);
    }
    static VaccinationRecord scheduled(LocalDate date) {
        return VaccinationRecord.schedule(pet(Species.DOG, TODAY.minusYears(1)), "Vaccine mẫu", date,
            "Dữ liệu mẫu, không phải tư vấn thú y.", TODAY.minusMonths(2), 7);
    }
    static VaccinationRecord inState(VaccinationStatus state) {
        var record = scheduled(TODAY.plusDays(8));
        if (state == VaccinationStatus.DUE_SOON) record.checkTime(TODAY.plusDays(1), 7);
        if (state == VaccinationStatus.OVERDUE) record.checkTime(TODAY.plusDays(9), 7);
        if (state == VaccinationStatus.COMPLETED) record.complete(TODAY, TODAY);
        return record;
    }

    @ParameterizedTest
    @CsvSource({"8,SCHEDULED", "7,DUE_SOON", "1,DUE_SOON", "0,DUE_SOON", "-1,OVERDUE"})
    void scheduledChecksExactDateBoundaries(int days, VaccinationStatus expected) {
        var record = scheduled(TODAY.plusDays(days));
        record.checkTime(TODAY, 7);
        assertThat(record.getStateName()).isEqualTo(expected);
        assertThat(record.getAdministeredDate()).isNull();
    }

    @Test void dueSoonIncludesAppointmentDayAndThenBecomesOverdue() {
        var record = scheduled(TODAY.plusDays(7));
        assertThat(record.checkTime(TODAY, 7)).isTrue();
        assertThat(record.checkTime(TODAY.plusDays(7), 7)).isFalse();
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(record.checkTime(TODAY.plusDays(8), 7)).isTrue();
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(record.checkTime(TODAY.plusYears(1), 7)).isFalse();
    }

    @ParameterizedTest @EnumSource(VaccinationStatus.class)
    void completionAndCapabilitiesCoverEveryState(VaccinationStatus state) {
        var record = inState(state);
        assertThat(record.getState().getName()).isEqualTo(state.name());
        assertThat(record.getState().getLabel()).isNotBlank();
        boolean allowed = state != VaccinationStatus.COMPLETED;
        assertThat(record.getState().canComplete()).isEqualTo(allowed);
        assertThat(record.getState().canReschedule()).isEqualTo(allowed);
        if (allowed) {
            record.complete(TODAY.minusDays(1), TODAY);
            assertThat(record.getAdministeredDate()).isEqualTo(TODAY.minusDays(1));
        } else {
            assertThatThrownBy(() -> record.complete(TODAY.minusDays(1), TODAY))
                .isInstanceOf(InvalidStateTransitionException.class).hasMessageContaining("Đã tiêm");
            assertThat(record.getAdministeredDate()).isEqualTo(TODAY);
        }
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.COMPLETED);
    }

    static Stream<Arguments> reschedules() {
        return Arrays.stream(VaccinationStatus.values()).flatMap(state -> Stream.of(
            Arguments.of(state, 8, VaccinationStatus.SCHEDULED), Arguments.of(state, 7, VaccinationStatus.DUE_SOON),
            Arguments.of(state, 0, VaccinationStatus.DUE_SOON), Arguments.of(state, -1, VaccinationStatus.OVERDUE)));
    }
    @ParameterizedTest @MethodSource("reschedules")
    void reschedulingClassifiesNewDateFromEveryState(VaccinationStatus state, int days, VaccinationStatus expected) {
        var record = inState(state);
        var oldDate = record.getScheduledDate();
        if (state == VaccinationStatus.COMPLETED) {
            assertThatThrownBy(() -> record.reschedule(TODAY.plusDays(days), TODAY, 7))
                .isInstanceOf(InvalidStateTransitionException.class);
            assertThat(record.getScheduledDate()).isEqualTo(oldDate);
            assertThat(record.getStateName()).isEqualTo(state);
            assertThat(record.getAdministeredDate()).isEqualTo(TODAY);
        } else {
            record.reschedule(TODAY.plusDays(days), TODAY, 7);
            assertThat(record.getScheduledDate()).isEqualTo(TODAY.plusDays(days));
            assertThat(record.getStateName()).isEqualTo(expected);
            assertThat(record.getAdministeredDate()).isNull();
        }
    }

    @ParameterizedTest @EnumSource(VaccinationStatus.class)
    void checkingFarFuturePreservesTerminalStateAndExpiresActiveStates(VaccinationStatus state) {
        var record = inState(state);
        record.checkTime(TODAY.plusYears(3), 7);
        assertThat(record.getStateName()).isEqualTo(state == VaccinationStatus.COMPLETED
            ? VaccinationStatus.COMPLETED : VaccinationStatus.OVERDUE);
    }

    @Test void configurableThresholdAndZeroDaysWorkOnCreationAndReschedule() {
        var record = scheduled(TODAY.plusDays(10));
        record.checkTime(TODAY, 10);
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        record.reschedule(TODAY.plusDays(1), TODAY, 0);
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        record.checkTime(TODAY.plusDays(1), 0);
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        var created = VaccinationRecord.schedule(record.getOwnedPet(), "Vaccine", TODAY.plusDays(10), "", TODAY, 10);
        assertThat(created.getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
    }

    @Test void invalidDatesAndThresholdNeverPartiallyChangeRecord() {
        var record = scheduled(TODAY.plusDays(8));
        var date = record.getScheduledDate();
        for (LocalDate invalid : Arrays.asList(null, record.getOwnedPet().getBirthDate().minusDays(1))) {
            assertThatThrownBy(() -> record.complete(invalid, TODAY)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> record.reschedule(invalid, TODAY, 7)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> record.complete(TODAY.plusDays(1), TODAY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> record.reschedule(TODAY, TODAY, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> record.checkTime(TODAY, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> record.reschedule(TODAY, null, 7)).isInstanceOf(NullPointerException.class);
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        assertThat(record.getScheduledDate()).isEqualTo(date);
        assertThat(record.getAdministeredDate()).isNull();
    }

    @Test void factoryRebuildsAllConcreteStatesAndRejectsMissingState() {
        var factory = new VaccinationStateFactory(7);
        assertThat(factory.from(VaccinationStatus.SCHEDULED)).isInstanceOf(ScheduledState.class);
        assertThat(factory.from(VaccinationStatus.DUE_SOON)).isInstanceOf(DueSoonState.class);
        assertThat(factory.from(VaccinationStatus.OVERDUE)).isInstanceOf(OverdueState.class);
        assertThat(factory.from(VaccinationStatus.COMPLETED)).isInstanceOf(CompletedState.class);
        assertThatThrownBy(() -> factory.from(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new VaccinationStateFactory(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
