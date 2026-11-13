package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import java.time.LocalDate;

/** State kết thúc: thời gian không đổi hồ sơ và không thể hoàn tất lần hai hoặc đổi lịch. */
public final class CompletedState extends AbstractVaccinationState {
    public VaccinationStatus checkTime(VaccinationRecord record, LocalDate today) { return VaccinationStatus.COMPLETED; }
    public String getName() { return "COMPLETED"; }
    public String getLabel() { return "Đã tiêm"; }
}
