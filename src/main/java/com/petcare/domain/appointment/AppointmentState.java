package com.petcare.domain.appointment;

import com.petcare.domain.Appointment;
import java.util.Set;

/**
 * State tách quy tắc chuyển trạng thái lịch hẹn; DB chỉ lưu enum, không lưu State object.
 * book chỉ áp dụng lúc khởi tạo, gọi lại trên lịch đã đặt là lỗi.
 * <pre>
 * | Trạng thái | book | start | finish | cancel |
 * |---|---|---|---|---|
 * | Chưa khởi tạo | BOOKED | Lỗi | Lỗi | Lỗi |
 * | BOOKED | Lỗi | IN_PROGRESS | Lỗi | CANCELLED |
 * | IN_PROGRESS | Lỗi | Lỗi | DONE | Lỗi |
 * | DONE | Lỗi | Lỗi | Lỗi | Lỗi |
 * | CANCELLED | Lỗi | Lỗi | Lỗi | Lỗi |
 * </pre>
 */
public interface AppointmentState {
    AppointmentStatus book(Appointment appointment);
    AppointmentStatus start(Appointment appointment);
    AppointmentStatus finish(Appointment appointment);
    AppointmentStatus cancel(Appointment appointment);
    String getName();
    String getLabel();
    default Set<AppointmentAction> getAllowedActions() { return Set.of(); }
}
