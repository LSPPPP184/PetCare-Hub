package com.petcare.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Tên và nhãn thao tác để giao diện hiển thị các quyền do State cung cấp. */
@Getter @RequiredArgsConstructor
public enum OrderAction {
    CONFIRM("confirm", "Xác nhận"), SHIP("ship", "Giao hàng"),
    COMPLETE("complete", "Hoàn tất"), CANCEL("cancel", "Hủy đơn");

    private final String path;
    private final String label;
}
