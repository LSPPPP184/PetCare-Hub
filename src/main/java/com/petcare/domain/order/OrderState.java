package com.petcare.domain.order;

import com.petcare.domain.Order;

/**
 * State tách quy tắc chuyển trạng thái khỏi Order, tránh chuỗi if/switch.
 * Các hành động trả tên trạng thái đích; Order tự lưu enum, không lưu object State.
 * Inventory do OrderService xử lý trong cùng transaction với chuyển trạng thái.
 *
 * <pre>
 * | Trạng thái | confirm | ship | complete | cancel |
 * |---|---|---|---|---|
 * | PENDING | CONFIRMED | Lỗi | Lỗi | CANCELLED (không hoàn kho) |
 * | CONFIRMED | Lỗi | SHIPPING | Lỗi | CANCELLED (hoàn kho) |
 * | SHIPPING | Lỗi | Lỗi | COMPLETED | Lỗi |
 * | COMPLETED | Lỗi | Lỗi | Lỗi | Lỗi |
 * | CANCELLED | Lỗi | Lỗi | Lỗi | Lỗi |
 * </pre>
 */
public interface OrderState {
    OrderStatus confirm(Order order);
    OrderStatus ship(Order order);
    OrderStatus complete(Order order);
    OrderStatus cancel(Order order);
    String getName();
    String getLabel();
    /** Chỉ mô tả thao tác cho giao diện; các phương thức chuyển trạng thái vẫn kiểm tra lại. */
    default java.util.Set<OrderAction> getAllowedActions() { return java.util.Set.of(); }
    default boolean restoresStockOnCancel() { return false; }
}
