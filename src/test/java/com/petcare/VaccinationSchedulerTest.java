package com.petcare;

import com.petcare.config.VaccinationProperties;
import com.petcare.domain.vaccination.VaccinationStatus;
import com.petcare.repository.VaccinationRecordRepository;
import com.petcare.scheduler.VaccinationScheduler;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static com.petcare.VaccinationStateTest.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VaccinationSchedulerTest {
    @Test void injectedClockUsesVietnamDateAndSchedulerIsIdempotent() {
        var records = mock(VaccinationRecordRepository.class);
        var properties = new VaccinationProperties();
        var dueSoon = scheduled(TODAY.plusDays(7));
        var overdue = scheduled(TODAY.minusDays(1));
        var farAway = scheduled(TODAY.plusDays(8));
        // UTC còn ngày 12, nhưng tại Việt Nam đã là ngày 13.
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T18:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        when(records.findByStateNameNot(VaccinationStatus.COMPLETED)).thenReturn(List.of(dueSoon, overdue, farAway));
        var scheduler = new VaccinationScheduler(records, properties, clock);
        scheduler.checkSchedules();
        assertThat(dueSoon.getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(overdue.getStateName()).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(farAway.getStateName()).isEqualTo(VaccinationStatus.SCHEDULED);
        scheduler.checkSchedules();
        verify(records, times(2)).findByStateNameNot(VaccinationStatus.COMPLETED);
        verify(records).save(dueSoon);
        verify(records).save(overdue);
        verifyNoMoreInteractions(records);
    }

    @Test void schedulerUsesConfiguredThresholdAndHandlesEmptyRepository() {
        var records = mock(VaccinationRecordRepository.class);
        var properties = new VaccinationProperties();
        properties.setDueSoonDays(10);
        var record = scheduled(TODAY.plusDays(10));
        when(records.findByStateNameNot(VaccinationStatus.COMPLETED)).thenReturn(List.of(record)).thenReturn(List.of());
        var scheduler = new VaccinationScheduler(records, properties, Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        scheduler.checkSchedules();
        scheduler.checkSchedules();
        assertThat(record.getStateName()).isEqualTo(VaccinationStatus.DUE_SOON);
        verify(records).save(record);
    }
}
