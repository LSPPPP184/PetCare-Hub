package com.petcare.dto;

import com.petcare.domain.order.*;
import java.util.Map;

/** Màu trình bày tách khỏi quy tắc chuyển trạng thái trong domain. */
public record OrderStatusView(OrderStatus value, String label, String badgeClass) {
    private static final Map<OrderStatus, String> COLORS = Map.of(
        OrderStatus.PENDING, "text-bg-warning", OrderStatus.CONFIRMED, "text-bg-info",
        OrderStatus.SHIPPING, "text-bg-primary", OrderStatus.COMPLETED, "text-bg-success",
        OrderStatus.CANCELLED, "text-bg-secondary");
    public static OrderStatusView from(OrderStatus status) {
        return new OrderStatusView(status, OrderStateFactory.from(status).getLabel(), COLORS.get(status));
    }
}
