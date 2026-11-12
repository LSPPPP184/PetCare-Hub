package com.petcare.domain.order;

import com.petcare.domain.Order;

/** State chờ xác nhận: chỉ chấp nhận các bước được ghi trong bảng OrderState. */
public final class PendingState extends AbstractOrderState {
    @Override public java.util.Set<OrderAction> getAllowedActions() {
        return java.util.Set.of(OrderAction.CONFIRM, OrderAction.CANCEL);
    }
    @Override public String getName() { return "PENDING"; }
    @Override public String getLabel() { return "Chờ xác nhận"; }
    @Override public OrderStatus confirm(Order order) { return OrderStatus.CONFIRMED; }
    @Override public OrderStatus cancel(Order order) { return OrderStatus.CANCELLED; }
}
