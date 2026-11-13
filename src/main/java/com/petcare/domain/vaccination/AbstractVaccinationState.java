package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import com.petcare.domain.order.InvalidStateTransitionException;
import java.time.LocalDate;

/** Mặc định từ chối thay đổi; các State chưa hoàn tất mở lại các thao tác được phép. */
public abstract class AbstractVaccinationState implements VaccinationState {
    protected InvalidStateTransitionException invalid(String action) {
        return new InvalidStateTransitionException("Không thể " + action + " lịch tiêm ở trạng thái " + getLabel() + ".");
    }
    public VaccinationStatus complete(VaccinationRecord record, LocalDate date) { throw invalid("hoàn tất"); }
    public VaccinationStatus reschedule(VaccinationRecord record, LocalDate newDate) { throw invalid("đổi ngày"); }
    public boolean canComplete() { return false; }
    public boolean canReschedule() { return false; }
}
