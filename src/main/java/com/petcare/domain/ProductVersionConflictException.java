package com.petcare.domain;

/** Từ chối form cũ để không ghi đè thay đổi đã được lưu bởi giao dịch khác. */
public class ProductVersionConflictException extends RuntimeException {
    public ProductVersionConflictException() {
        super("Dữ liệu đã được thay đổi, vui lòng tải lại trang");
    }
}
