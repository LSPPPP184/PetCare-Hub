package com.petcare.domain.order;

import java.util.Map;
import java.util.Objects;

/** Factory dựng State từ enum lưu trong DB; các State bất biến có thể dùng chung. */
public final class OrderStateFactory {
    private OrderStateFactory() {}
    private static final Map<OrderStatus, OrderState> STATES = Map.of(
        OrderStatus.PENDING, new PendingState(),
        OrderStatus.CONFIRMED, new ConfirmedState(),
        OrderStatus.SHIPPING, new ShippingState(),
        OrderStatus.COMPLETED, new CompletedState(),
        OrderStatus.CANCELLED, new CancelledState());
    public static OrderState from(OrderStatus status) {
        return STATES.get(Objects.requireNonNull(status, "Trạng thái đơn hàng không được trống."));
    }
}
