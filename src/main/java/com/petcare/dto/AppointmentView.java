package com.petcare.dto;

import com.petcare.domain.Appointment;
import com.petcare.domain.appointment.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** DTO tạo trong transaction, không để view truy cập entity hoặc thông tin đăng nhập. */
public record AppointmentView(Long id, String customerName, String petName, String petSpecies, String serviceName,
                              String staffName, LocalDateTime startTime, LocalDateTime endTime, String note,
                              BigDecimal price, String label, String badgeClass, List<AppointmentAction> actions,
                              boolean customerCanCancel) {
    private static final Map<AppointmentStatus, String> COLORS = Map.of(
        AppointmentStatus.BOOKED, "text-bg-warning", AppointmentStatus.IN_PROGRESS, "text-bg-primary",
        AppointmentStatus.DONE, "text-bg-success", AppointmentStatus.CANCELLED, "text-bg-secondary");
    public static AppointmentView from(Appointment a, boolean beforeCutoff) {
        var actions = a.getState().getAllowedActions().stream().sorted().toList();
        return new AppointmentView(a.getId(), a.getCustomer().getFullName(), a.getPetName(), a.getPetSpecies().getLabel(),
            a.getService().getName(), a.getStaff().getFullName(), a.getStartTime(), a.getEndTime(), a.getNote(), a.getPrice(),
            a.getState().getLabel(), COLORS.get(a.getStateName()), actions,
            beforeCutoff && actions.contains(AppointmentAction.CANCEL));
    }
}
