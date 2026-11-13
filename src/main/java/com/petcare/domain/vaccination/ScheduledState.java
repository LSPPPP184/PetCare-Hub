package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** State còn xa hạn; cho phép đi thẳng tới quá hạn khi bộ lập lịch bỏ lỡ nhiều ngày. */
public final class ScheduledState extends ActiveVaccinationState {
    private final int dueSoonDays;
    public ScheduledState(int dueSoonDays) {
        if (dueSoonDays < 0) throw new IllegalArgumentException("Ngưỡng sắp đến hạn không được âm.");
        this.dueSoonDays = dueSoonDays;
    }
    public VaccinationStatus checkTime(VaccinationRecord record, LocalDate today) {
        long remaining = ChronoUnit.DAYS.between(today, record.getScheduledDate());
        if (remaining < 0) return VaccinationStatus.OVERDUE;
        if (remaining <= dueSoonDays) return VaccinationStatus.DUE_SOON;
        return VaccinationStatus.SCHEDULED;
    }
    public String getName() { return "SCHEDULED"; }
    public String getLabel() { return "Đã lên lịch"; }
}
