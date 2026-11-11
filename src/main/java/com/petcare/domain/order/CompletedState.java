package com.petcare.domain.order;

/** State hoàn thành: chỉ chấp nhận các bước được ghi trong bảng OrderState. */
public final class CompletedState extends AbstractOrderState {
    @Override public String getName() { return "COMPLETED"; }
    @Override public String getLabel() { return "Hoàn thành"; }

}
