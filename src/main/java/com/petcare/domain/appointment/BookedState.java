package com.petcare.domain.appointment;

import com.petcare.domain.Appointment;
import java.util.Set;

/** State đã đặt: cho bắt đầu hoặc hủy, không cho đặt lại lịch cũ. */
public final class BookedState extends AbstractAppointmentState {
    public AppointmentStatus book(Appointment a) {
        if (a.getStateName() != null) throw invalid("đặt lại");
        return AppointmentStatus.BOOKED;
    }
    public AppointmentStatus start(Appointment a) { return AppointmentStatus.IN_PROGRESS; }
    public AppointmentStatus cancel(Appointment a) { return AppointmentStatus.CANCELLED; }
    public String getName() { return "BOOKED"; }
    public String getLabel() { return "Đã đặt"; }
    public Set<AppointmentAction> getAllowedActions() { return Set.of(AppointmentAction.START, AppointmentAction.CANCEL); }
}
