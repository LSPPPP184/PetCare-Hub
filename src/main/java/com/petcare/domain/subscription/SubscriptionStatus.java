package com.petcare.domain.subscription;

/** Mã lưu trữ cho pattern State; hành vi được ủy quyền cho từng lớp trạng thái. */
public enum SubscriptionStatus {
    ACTIVE("Đang hoạt động"), EXPIRED("Đã hết hạn / hết lượt"), CANCELLED("Đã hủy");
    private final String label;
    SubscriptionStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
