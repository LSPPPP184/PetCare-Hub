package com.petcare.dto;

import com.petcare.domain.ServiceItem;
import java.math.BigDecimal;

public record AppointmentServiceView(Long id, String name, String description, int durationMinutes, BigDecimal price) {
    public static AppointmentServiceView from(ServiceItem service) {
        return new AppointmentServiceView(service.getId(), service.getName(), service.getDescription(),
            service.getDurationMinutes(), service.calculatePrice());
    }
}
