package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import java.time.LocalDate;

/** State sắp đến hạn bao gồm chính ngày hẹn; chỉ quá hạn từ ngày kế tiếp. */
public final class DueSoonState extends ActiveVaccinationState {
    public VaccinationStatus checkTime(VaccinationRecord record, LocalDate today) {
        return today.isAfter(record.getScheduledDate()) ? VaccinationStatus.OVERDUE : VaccinationStatus.DUE_SOON;
    }
    public String getName() { return "DUE_SOON"; }
    public String getLabel() { return "Sắp đến hạn"; }
    public boolean notifiesStatusChange() { return true; }
}
