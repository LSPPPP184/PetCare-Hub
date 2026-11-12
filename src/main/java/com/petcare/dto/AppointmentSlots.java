package com.petcare.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record AppointmentSlots(Long serviceId, String serviceName, int durationMinutes, BigDecimal price,
                               LocalDate date, List<Slot> slots) {
    public record Slot(Long staffId, String staffName, LocalDateTime startTime, LocalDateTime endTime) {}
}
