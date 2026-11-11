package com.petcare.domain.order;

/** State đã hủy: chỉ chấp nhận các bước được ghi trong bảng OrderState. */
public final class CancelledState extends AbstractOrderState {
    @Override public String getName() { return "CANCELLED"; }
    @Override public String getLabel() { return "Đã hủy"; }

}
