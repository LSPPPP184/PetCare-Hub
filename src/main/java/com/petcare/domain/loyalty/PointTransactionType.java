package com.petcare.domain.loyalty;

public enum PointTransactionType {
    EARN("Tích điểm"), REDEEM("Đổi điểm"), ADJUST("Điều chỉnh");
    private final String label;
    PointTransactionType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
