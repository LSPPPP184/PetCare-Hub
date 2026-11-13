package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import java.time.LocalDate;

/** State quá hạn giữ nguyên tới khi hoàn tất hoặc chủ động đổi lịch. */
public final class OverdueState extends ActiveVaccinationState {
    public VaccinationStatus checkTime(VaccinationRecord record, LocalDate today) { return VaccinationStatus.OVERDUE; }
    public String getName() { return "OVERDUE"; }
    public String getLabel() { return "Quá hạn"; }
    public boolean notifiesStatusChange() { return true; }
}
