package com.petcare.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record AppointmentSelection(Long serviceId, String serviceName, Long staffId, String staffName,
                                   LocalDateTime startTime, LocalDateTime endTime, BigDecimal price) {}
