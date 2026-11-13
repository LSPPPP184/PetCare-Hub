package com.petcare.domain.vaccination;

import java.util.Map;
import java.util.Objects;

/** Factory dựng State từ tên lưu trong DB, dùng registry thay cho phân nhánh theo loại. */
public final class VaccinationStateFactory {
    public static final int DEFAULT_DUE_SOON_DAYS = 7;
    private final Map<VaccinationStatus, VaccinationState> states;

    public VaccinationStateFactory(int dueSoonDays) {
        states = Map.of(VaccinationStatus.SCHEDULED, new ScheduledState(dueSoonDays),
            VaccinationStatus.DUE_SOON, new DueSoonState(), VaccinationStatus.OVERDUE, new OverdueState(),
            VaccinationStatus.COMPLETED, new CompletedState());
    }
    public VaccinationState from(VaccinationStatus status) {
        return states.get(Objects.requireNonNull(status, "Trạng thái tiêm phòng không được trống."));
    }
}
