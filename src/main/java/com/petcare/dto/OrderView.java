package com.petcare.dto;

import com.petcare.domain.Order;
import com.petcare.domain.order.OrderAction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Dữ liệu hiển thị được chụp trong transaction, không đưa entity hay mật khẩu ra view. */
public record OrderView(Long id, String customerName, String customerEmail, String shippingAddress,
                        LocalDateTime createdAt, BigDecimal totalAmount, BigDecimal discountAmount,
                        BigDecimal finalAmount, OrderStatusView status, List<OrderAction> actions, List<Line> items) {
    public record Line(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal total, boolean active) {}
    public boolean isCancellable() { return actions.contains(OrderAction.CANCEL); }
    public static OrderView from(Order order) {
        return new OrderView(order.getId(), order.getCustomer().getFullName(), order.getCustomer().getEmail(),
            order.getShippingAddress(), order.getCreatedAt(), order.getTotalAmount(), order.getDiscountAmount(),
            order.getFinalAmount(), OrderStatusView.from(order.getStateName()),
            order.getState().getAllowedActions().stream().sorted().toList(),
            order.getItems().stream().map(item -> new Line(item.getProduct().getId(), item.getProduct().getName(),
                item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), item.getProduct().isActive())).toList());
    }
}
