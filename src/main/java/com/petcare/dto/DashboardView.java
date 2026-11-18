package com.petcare.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Bản chụp thống kê quản trị, không mang entity hoặc thông tin đăng nhập ra giao diện. */
public record DashboardView(LocalDate today, LocalDate healthDate, long appointmentsToday,
                            long petsDueSoon, long petsOverdue, List<Revenue> revenue,
                            List<BestSeller> bestSellers, List<Members> members) {
    public record Revenue(YearMonth month, BigDecimal amount) {}
    public record BestSeller(Long productId, String name, long quantity) {}
    public record Members(String name, long count) {}
}
