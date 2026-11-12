package com.petcare.domain.appointment;

import com.petcare.domain.Appointment;
import com.petcare.domain.order.InvalidStateTransitionException;

/** Mặc định từ chối; từng State chỉ mở đúng hành động hợp lệ. */
public abstract class AbstractAppointmentState implements AppointmentState {
    protected InvalidStateTransitionException invalid(String action) {
        return new InvalidStateTransitionException("Không thể " + action + " lịch hẹn ở trạng thái " + getLabel() + ".");
    }
    public AppointmentStatus book(Appointment a) { throw invalid("đặt lại"); }
    public AppointmentStatus start(Appointment a) { throw invalid("bắt đầu"); }
    public AppointmentStatus finish(Appointment a) { throw invalid("hoàn tất"); }
    public AppointmentStatus cancel(Appointment a) { throw invalid("hủy"); }
}
