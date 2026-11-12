package com.petcare.domain.appointment;

import java.util.Map;
import java.util.Objects;

/** Factory dựng State bất biến từ enum đã lưu, cùng cách làm của Order. */
public final class AppointmentStateFactory {
    private AppointmentStateFactory() {}
    private static final Map<AppointmentStatus, AppointmentState> STATES = Map.of(
        AppointmentStatus.BOOKED, new BookedState(), AppointmentStatus.IN_PROGRESS, new InProgressState(),
        AppointmentStatus.DONE, new DoneState(), AppointmentStatus.CANCELLED, new CancelledState());
    public static AppointmentState from(AppointmentStatus status) {
        return STATES.get(Objects.requireNonNull(status, "Trạng thái lịch hẹn không được trống."));
    }
}
