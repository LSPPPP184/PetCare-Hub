package com.petcare;

import com.petcare.config.AppointmentHours;
import com.petcare.domain.appointment.AppointmentException;
import com.petcare.service.AppointmentPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class AppointmentPolicyTest {
    private final AppointmentPolicy policy = new AppointmentPolicy(
        Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneOffset.UTC), new AppointmentHours());
    @Test void clockUsesVietnamTimeAndCancellationIncludesExactlyTwoHours() {
        assertThat(policy.now()).isEqualTo(LocalDateTime.parse("2026-01-05T08:00"));
        assertThat(policy.canCustomerCancel(LocalDateTime.parse("2026-01-05T10:00"))).isTrue();
        assertThat(policy.canCustomerCancel(LocalDateTime.parse("2026-01-05T09:59:59"))).isFalse();
        assertThatThrownBy(() -> policy.validateCustomerCancellation(LocalDateTime.parse("2026-01-05T09:59:59")))
            .isInstanceOf(AppointmentException.class).hasMessageContaining("ít nhất 2 tiếng");
    }
    @ParameterizedTest @CsvSource({
        "2026-01-05T08:00,30,true", "2026-01-05T07:59,30,false", "2026-01-05T17:30,30,true",
        "2026-01-05T17:31,30,false", "2026-01-05T18:00,30,false", "2026-01-05T08:00,600,true",
        "2026-01-05T08:00,601,false", "2026-01-05T08:00,0,false", "2026-01-04T10:00,30,false",
        "2026-01-03T10:00,30,false",
        "2026-01-10T08:00,30,true", "2026-01-11T08:00,30,false", "2026-01-12T23:45,30,false"})
    void validatesWorkingHoursAndPast(String text, int minutes, boolean allowed) {
        var start = LocalDateTime.parse(text);
        if (allowed) assertThatCode(() -> policy.validateBooking(start, minutes)).doesNotThrowAnyException();
        else assertThatThrownBy(() -> policy.validateBooking(start, minutes)).isInstanceOf(AppointmentException.class);
    }
    @ParameterizedTest @CsvSource({"09:00,10:00,false", "10:00,10:30,true", "10:30,11:00,true",
        "11:00,12:00,false", "09:30,10:01,true", "10:59,11:30,true", "09:00,12:00,true", "10:15,10:45,true"})
    void overlapTreatsTouchingEndpointsAsFree(String from, String to, boolean overlap) {
        var day = LocalDate.of(2026, 1, 5);
        assertThat(AppointmentPolicy.overlaps(day.atTime(LocalTime.parse(from)), day.atTime(LocalTime.parse(to)),
            day.atTime(10, 0), day.atTime(11, 0))).isEqualTo(overlap);
    }
    @Test void respectsConfiguredWorkingDaysAndHours() {
        var hours = new AppointmentHours();
        hours.setOpen(LocalTime.of(9, 0)); hours.setClose(LocalTime.of(12, 0));
        hours.setWorkingDays(java.util.Set.of(DayOfWeek.TUESDAY));
        var custom = new AppointmentPolicy(Clock.fixed(Instant.parse("2026-01-05T01:00:00Z"), ZoneOffset.UTC), hours);
        assertThatThrownBy(() -> custom.validateBooking(LocalDateTime.parse("2026-01-05T10:00"), 30)).isInstanceOf(AppointmentException.class);
        assertThatThrownBy(() -> custom.validateBooking(LocalDateTime.parse("2026-01-06T08:30"), 30)).isInstanceOf(AppointmentException.class);
        assertThatCode(() -> custom.validateBooking(LocalDateTime.parse("2026-01-06T11:30"), 30)).doesNotThrowAnyException();
    }
}
