package com.petcare.domain.vaccination;

import com.petcare.domain.VaccinationRecord;
import java.time.LocalDate;

/**
 * State đóng gói quy tắc thời gian và quyền thao tác của từng trạng thái tiêm phòng.
 * State trả quyết định; VaccinationRecord áp dụng ngày, trạng thái và sự kiện cùng nhau.
 * reschedule trả SCHEDULED làm điểm bắt đầu; hồ sơ đánh giá ngay ngày mới bằng checkTime
 * với ngày hiện tại truyền vào, không phát sự kiện cho trạng thái trung gian.
 */
public interface VaccinationState {
    VaccinationStatus checkTime(VaccinationRecord record, LocalDate today);
    VaccinationStatus complete(VaccinationRecord record, LocalDate date);
    VaccinationStatus reschedule(VaccinationRecord record, LocalDate newDate);
    String getName();
    String getLabel();
    boolean canComplete();
    boolean canReschedule();
    default boolean notifiesStatusChange() { return false; }
}
