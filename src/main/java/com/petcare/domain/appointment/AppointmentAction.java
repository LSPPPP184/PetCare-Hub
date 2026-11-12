package com.petcare.domain.appointment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public enum AppointmentAction {
    START("start", "Bắt đầu"), FINISH("finish", "Hoàn tất"), CANCEL("cancel", "Hủy lịch");
    private final String path;
    private final String label;
}
