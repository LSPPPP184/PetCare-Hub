package com.petcare.domain.appointment;

/** Lỗi nghiệp vụ lịch hẹn có thông báo tiếng Việt để hiển thị cho người dùng. */
public class AppointmentException extends RuntimeException {
    public AppointmentException(String message) { super(message); }
}
