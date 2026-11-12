package com.petcare.dto;

import com.petcare.domain.order.OrderAction;
import com.petcare.domain.order.OrderStateFactory;
import com.petcare.domain.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Projection cho danh sách: đếm dòng tại DB, không tải entity dòng hàng/sản phẩm.
 * Giữ tên khách và thành tiền để các cột hiện có không đổi; itemCount là số dòng,
 * không phải tổng số lượng. Nhãn, màu và thao tác vẫn lấy từ State như trang chi tiết.
 */
public record OrderSummaryView(Long id, LocalDateTime createdAt, OrderStatus stateName,
                               BigDecimal finalAmount, long itemCount, String customerName) {
    public OrderStatusView getStatus() { return OrderStatusView.from(stateName); }
    public List<OrderAction> getActions() {
        return OrderStateFactory.from(stateName).getAllowedActions().stream().sorted().toList();
    }
    public boolean isCancellable() { return getActions().contains(OrderAction.CANCEL); }
}
