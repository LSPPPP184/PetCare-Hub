package com.petcare.domain.order;

/** Hành động không hợp lệ với trạng thái hiện tại của đơn hàng. */
public class InvalidStateTransitionException extends IllegalStateException {
    public InvalidStateTransitionException(String message) { super(message); }
}
