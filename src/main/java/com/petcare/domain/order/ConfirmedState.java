package com.petcare.domain.order;

import com.petcare.domain.Order;

/** State đã xác nhận: chỉ chấp nhận các bước được ghi trong bảng OrderState. */
public final class ConfirmedState extends AbstractOrderState {
    @Override public String getName() { return "CONFIRMED"; }
    @Override public String getLabel() { return "Đã xác nhận"; }
    @Override public OrderStatus ship(Order order) { return OrderStatus.SHIPPING; }
    @Override public OrderStatus cancel(Order order) { return OrderStatus.CANCELLED; }
    @Override public boolean restoresStockOnCancel() { return true; }
}
