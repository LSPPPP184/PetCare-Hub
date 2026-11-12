package com.petcare.service;

import com.petcare.config.AppointmentHours;
import com.petcare.domain.appointment.AppointmentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.*;

/** Quy tắc thời gian dùng Clock inject; các khoảng lịch là [start, end), cho phép nối tiếp. */
@Component @RequiredArgsConstructor
public class AppointmentPolicy {
    private final Clock clock;
    private final AppointmentHours hours;
    public LocalDateTime now() { return LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))); }
    public LocalDate today() { return now().toLocalDate(); }
    public boolean isWorkingDay(LocalDate date) { return hours.getWorkingDays().contains(date.getDayOfWeek()); }
    public LocalDateTime endTime(LocalDateTime start, Integer minutes) {
        if (start == null || minutes == null || minutes <= 0 || minutes > Duration.between(hours.getOpen(), hours.getClose()).toMinutes())
            throw new AppointmentException("Thời lượng dịch vụ không phù hợp với giờ làm việc.");
        return start.plusMinutes(minutes);
    }
    public void validateBooking(LocalDateTime start, Integer minutes) {
        var end = endTime(start, minutes);
        if (start.isBefore(now())) throw new AppointmentException("Không thể đặt lịch trong quá khứ.");
        if (!isWorkingDay(start.toLocalDate()) || !start.toLocalDate().equals(end.toLocalDate())
            || start.toLocalTime().isBefore(hours.getOpen()) || end.toLocalTime().isAfter(hours.getClose()))
            throw new AppointmentException("Lịch hẹn phải nằm hoàn toàn trong ngày và giờ làm việc của cửa hàng.");
    }
    public boolean canCustomerCancel(LocalDateTime start) { return !start.isBefore(now().plusHours(2)); }
    public void validateCustomerCancellation(LocalDateTime start) {
        if (!canCustomerCancel(start)) throw new AppointmentException("Chỉ được hủy lịch khi còn cách giờ hẹn ít nhất 2 tiếng.");
    }
    public static boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }
}
