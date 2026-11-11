package com.petcare.domain.order;

import com.petcare.domain.Order;

/** State đang giao: chỉ chấp nhận các bước được ghi trong bảng OrderState. */
public final class ShippingState extends AbstractOrderState {
    @Override public String getName() { return "SHIPPING"; }
    @Override public String getLabel() { return "Đang giao"; }
    @Override public OrderStatus complete(Order order) { return OrderStatus.COMPLETED; }
}
