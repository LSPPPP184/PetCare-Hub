package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import java.time.LocalDate;

/** Các State còn hiệu lực dùng chung quyền hoàn tất và đổi lịch, tránh lặp quy tắc. */
public abstract class ActiveVaccinationState extends AbstractVaccinationState {
    public VaccinationStatus complete(VaccinationRecord record, LocalDate date) {
        validateDate(record, date);
        return VaccinationStatus.COMPLETED;
    }
    public VaccinationStatus reschedule(VaccinationRecord record, LocalDate newDate) {
        validateDate(record, newDate);
        return VaccinationStatus.SCHEDULED;
    }
    private void validateDate(VaccinationRecord record, LocalDate date) {
        if (date == null || date.isBefore(record.getOwnedPet().getBirthDate()))
            throw new IllegalArgumentException("Ngày tiêm không được trống hoặc trước ngày sinh.");
    }
    public boolean canComplete() { return true; }
    public boolean canReschedule() { return true; }
}
