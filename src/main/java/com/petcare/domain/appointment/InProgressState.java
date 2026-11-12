package com.petcare.domain.appointment;

import com.petcare.domain.Appointment;
import java.util.Set;

/** State đang thực hiện: chỉ cho hoàn tất. */
public final class InProgressState extends AbstractAppointmentState {
    public AppointmentStatus finish(Appointment a) { return AppointmentStatus.DONE; }
    public String getName() { return "IN_PROGRESS"; }
    public String getLabel() { return "Đang thực hiện"; }
    public Set<AppointmentAction> getAllowedActions() { return Set.of(AppointmentAction.FINISH); }
}
