package com.petcare;

import com.petcare.config.MutableClock;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MutableClockTest {
    @Test void selectingRealTodayUsesCurrentRealInstantWithoutRewinding() {
        var source = mock(Clock.class);
        var zone = ZoneId.of("Asia/Ho_Chi_Minh");
        when(source.getZone()).thenReturn(zone);
        when(source.instant()).thenReturn(Instant.parse("2026-11-13T03:00:00Z"));
        var clock = new MutableClock(source);
        when(source.instant()).thenReturn(Instant.parse("2026-11-13T03:30:45Z"));
        clock.advanceTo(LocalDate.of(2026, 11, 13));
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-11-13T03:30:45Z"));
        // Nếu đồng hồ nguồn bị chỉnh lùi, không lùi thời điểm đã xác nhận.
        when(source.instant()).thenReturn(Instant.parse("2026-11-13T03:15:00Z"));
        clock.advanceTo(LocalDate.of(2026, 11, 13));
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-11-13T03:30:45Z"));
        clock.advanceTo(LocalDate.of(2026, 11, 14));
        var future = clock.instant();
        clock.advanceTo(LocalDate.of(2026, 11, 14));
        assertThat(clock.instant()).isEqualTo(future);
        assertThatThrownBy(() -> clock.advanceTo(LocalDate.of(2026, 11, 13))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void failedSimulationIsNotPublishedToOtherThreadsOrZoneViews() throws Exception {
        var start = Instant.parse("2026-11-13T03:00:00Z");
        var clock = new MutableClock(Clock.fixed(start, ZoneId.of("Asia/Ho_Chi_Minh")));
        var utc = clock.withZone(ZoneOffset.UTC);
        try (var executor = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> clock.advanceAndRun(LocalDate.of(2026, 11, 14), () -> {
                assertThat(clock.instant()).isEqualTo(Instant.parse("2026-11-13T17:00:00Z"));
                assertThat(utc.instant()).isEqualTo(clock.instant());
                try {
                    assertThat(executor.submit(utc::instant).get(5, TimeUnit.SECONDS)).isEqualTo(start);
                } catch (Exception ex) { throw new AssertionError(ex); }
                throw new IllegalStateException("Lỗi lưu giả lập");
            })).isInstanceOf(IllegalStateException.class).hasMessage("Lỗi lưu giả lập");
        }
        assertThat(clock.instant()).isEqualTo(start);
        assertThat(utc.instant()).isEqualTo(start);
        clock.advanceAndRun(LocalDate.of(2026, 11, 14), () -> {});
        assertThat(utc.instant()).isEqualTo(Instant.parse("2026-11-13T17:00:00Z"));
    }
    @Test void sharedZoneViewHandlesMidnightLeapDayAndRejectsBackwardDate() {
        var clock = new MutableClock(Clock.fixed(Instant.parse("2028-02-28T18:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        var utc = clock.withZone(ZoneOffset.UTC);
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(LocalDate.now(utc)).isEqualTo(LocalDate.of(2028, 2, 28));
        clock.advanceTo(LocalDate.of(2028, 3, 1));
        assertThat(clock.instant()).isEqualTo(Instant.parse("2028-02-29T17:00:00Z"));
        assertThat(utc.instant()).isEqualTo(clock.instant());
        assertThatThrownBy(() -> clock.advanceTo(LocalDate.of(2028, 2, 29))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> clock.advanceTo(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2028, 3, 1));
    }
}
