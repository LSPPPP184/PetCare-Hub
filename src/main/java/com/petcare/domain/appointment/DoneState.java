package com.petcare.domain.appointment;

/** State hoàn tất là trạng thái kết thúc, không cho thay đổi tiếp. */
public final class DoneState extends AbstractAppointmentState {
    public String getName() { return "DONE"; }
    public String getLabel() { return "Hoàn tất"; }
}
