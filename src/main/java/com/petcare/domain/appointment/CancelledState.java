package com.petcare.domain.appointment;

/** State đã hủy là trạng thái kết thúc; khung giờ được giải phóng. */
public final class CancelledState extends AbstractAppointmentState {
    public String getName() { return "CANCELLED"; }
    public String getLabel() { return "Đã hủy"; }
}
