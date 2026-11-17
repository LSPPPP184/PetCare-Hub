package com.petcare.event;

/** Observer đồng bộ trong giao dịch hoàn tất; không xử lý sau commit để trạng thái và điểm có tính nguyên tử. */
public record AppointmentCompletedEvent(Long appointmentId) {}
