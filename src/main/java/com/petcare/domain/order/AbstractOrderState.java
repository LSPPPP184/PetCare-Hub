package com.petcare.domain.order;

import com.petcare.domain.Order;

/** State mặc định từ chối; từng trạng thái chỉ mở các hành động hợp lệ. */
public abstract class AbstractOrderState implements OrderState {
    private InvalidStateTransitionException invalid(String action) {
        return new InvalidStateTransitionException("Không thể " + action + " đơn hàng ở trạng thái " + getLabel() + ".");
    }
    @Override public OrderStatus confirm(Order order) { throw invalid("xác nhận"); }
    @Override public OrderStatus ship(Order order) { throw invalid("giao"); }
    @Override public OrderStatus complete(Order order) { throw invalid("hoàn thành"); }
    @Override public OrderStatus cancel(Order order) { throw invalid("hủy"); }
}
