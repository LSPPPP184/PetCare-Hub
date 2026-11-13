package com.petcare;

import com.petcare.domain.Species;
import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.vaccination.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static com.petcare.VaccinationStateTest.*;

class VaccinationScheduleTemplateTest {
    private VaccinationScheduleTemplate template(int years, LocalDate today) {
        var zone = ZoneId.of("Asia/Ho_Chi_Minh");
        return new VaccinationScheduleTemplate(years, Clock.fixed(today.atStartOfDay(zone).toInstant(), zone));
    }
    @ParameterizedTest @EnumSource(value = Species.class, names = {"DOG", "CAT"})
    void generatesInitialWeeksAndAnnualDatesForBothSpecies(Species species) {
        LocalDate birth = LocalDate.of(2024, 1, 1);
        var pet = pet(species, birth);
        var records = template(3, birth).generate(pet, 7);
        assertThat(records).extracting(VaccinationRecord::getScheduledDate).containsExactly(
            LocalDate.of(2024, 2, 12), LocalDate.of(2024, 2, 26), LocalDate.of(2024, 3, 25),
            LocalDate.of(2025, 3, 25), LocalDate.of(2026, 3, 25), LocalDate.of(2027, 3, 25));
        assertThat(records).allSatisfy(record -> {
            assertThat(record.getOwnedPet()).isSameAs(pet);
            assertThat(record.getVaccineName()).contains(species.getLabel().toLowerCase());
            assertThat(record.getNote()).contains("không phải tư vấn thú y");
            assertThat(record.getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
            assertThat(record.getAdministeredDate()).isNull();
        });
    }

    @Test void annualDatesUseCalendarYearsIncludingLeapDay() {
        LocalDate birth = LocalDate.of(2023, 12, 7);
        var records = template(4, birth).generate(pet(Species.CAT, birth), 7);
        assertThat(records).extracting(VaccinationRecord::getScheduledDate).containsExactly(
            LocalDate.of(2024, 1, 18), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28), LocalDate.of(2027, 2, 28), LocalDate.of(2028, 2, 29));
    }

    @ParameterizedTest @EnumSource(value = Species.class, names = {"DOG", "CAT"})
    void fiveYearOldPetsGetOnlyLatestDueBoosterAndConfiguredFutureDoses(Species species) {
        var records = template(3, TODAY).generate(pet(species, TODAY.minusYears(5)), 7);
        assertThat(records).extracting(VaccinationRecord::getScheduledDate).containsExactly(
            LocalDate.of(2026, 2, 5), LocalDate.of(2027, 2, 5),
            LocalDate.of(2028, 2, 5), LocalDate.of(2029, 2, 5));
        assertThat(records).extracting(VaccinationRecord::getAnnualDoseYear).containsExactly(4, 5, 6, 7);
        assertThat(records.getFirst().getStateName()).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(records.subList(1, records.size())).allSatisfy(record -> {
            assertThat(record.getScheduledDate()).isAfter(TODAY);
            assertThat(record.getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        });
        assertThat(records).allSatisfy(record -> assertThat(record.getAdministeredDate()).isNull());
    }

    @Test void registrationOnAnniversaryDoesNotMarkTodaysDoseOverdue() {
        var pet = pet(Species.CAT, LocalDate.of(2021, 11, 13));
        LocalDate due = LocalDate.of(2026, 2, 5);
        var records = template(1, due).generate(pet, 7);
        assertThat(records).extracting(VaccinationRecord::getScheduledDate)
            .containsExactly(due, due.plusYears(1));
        assertThat(records.getFirst().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(template(1, due.plusDays(1)).generate(pet, 7).getFirst().getStateName())
            .isEqualTo(VaccinationStatus.OVERDUE);
        var before = template(1, due.minusDays(1)).generate(pet, 7);
        assertThat(before).extracting(VaccinationRecord::getScheduledDate).containsExactly(due.minusYears(1), due);
    }

    @Test void lateRegistrationBeforeFirstAnnualDoseDoesNotInventOverdueInitialDoses() {
        var records = template(2, TODAY).generate(pet(Species.DOG, TODAY.minusMonths(6)), 7);
        assertThat(records).hasSize(2).allSatisfy(record -> {
            assertThat(record.getScheduledDate()).isAfter(TODAY);
            assertThat(record.getAnnualDoseYear()).isNotNull();
        });
    }

    @Test void partiallyElapsedInitialScheduleRetainsOnlyTodayAndFutureInitialDoses() {
        var records = template(1, TODAY).generate(pet(Species.CAT, TODAY.minusWeeks(8)), 7);
        assertThat(records).hasSize(3);
        assertThat(records.getFirst().getScheduledDate()).isEqualTo(TODAY);
        assertThat(records.getFirst().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(records.get(1).getScheduledDate()).isEqualTo(TODAY.plusWeeks(4));
    }

    @Test void templateUsesConfiguredDueSoonBoundary() {
        LocalDate birth = TODAY.minusWeeks(6).plusDays(10);
        var records = template(1, TODAY).generate(pet(Species.DOG, birth), 10);
        assertThat(records.getFirst().getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(records.get(1).getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
    }

    @ParameterizedTest @EnumSource(value = Species.class, names = {"BIRD", "FISH", "OTHER"})
    void unsupportedSpeciesHaveNoInventedSchedule(Species species) {
        assertThat(template(3, TODAY).generate(pet(species, TODAY), 7)).isEmpty();
    }

    @Test void rejectsUnboundedOrMissingAnnualHorizon() {
        assertThatThrownBy(() -> template(0, TODAY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> template(21, TODAY)).isInstanceOf(IllegalArgumentException.class);
    }
}
